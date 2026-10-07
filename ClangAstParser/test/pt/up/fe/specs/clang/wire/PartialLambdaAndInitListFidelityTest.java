/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ast.decl.ClassTemplatePartialSpecializationDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.TemplateTypeParmDecl;
import pt.up.fe.specs.clava.ast.decl.TypeDecl;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentType;
import pt.up.fe.specs.clava.ast.expr.CStyleCastExpr;
import pt.up.fe.specs.clava.ast.expr.CXXFunctionalCastExpr;
import pt.up.fe.specs.clava.ast.expr.LambdaExpr;
import pt.up.fe.specs.clava.ast.expr.enums.LambdaCaptureKind;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.type.MemberPointerType;
import pt.up.fe.specs.clava.ast.type.PointerType;
import pt.up.fe.specs.clava.ast.type.QualType;
import pt.up.fe.specs.clava.ast.type.TemplateTypeParmType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

class PartialLambdaAndInitListFidelityTest {
    private static final List<String> OPTIONS = List.of("-std=c++20");
    private static final String FIXTURE = "cxx/wire_ast_fidelity.cpp";

    @TempDir
    Path temporary;

    @Test
    void retainsPartialSpecializationLambdaCapturesAndDirectListSyntaxAcrossRoundTrip() throws IOException {
        SpecsSystem.programStandardInit();

        Path sourceDirectory = Files.createDirectories(temporary.resolve("source"));
        File source = SpecsIo.resourceCopy(FIXTURE, sourceDirectory.toFile(), false, true);
        assertTrue(source.isFile());

        App parsed = parse(source, "parse-first");
        assertPartialSpecialization(parsed);
        assertTemplateParameterContext(parsed);
        assertInitCaptures(parsed);
        assertFunctionalCasts(parsed);
        assertReferenceCastType(parsed);

        List<File> generated = parsed.write(Files.createDirectories(temporary.resolve("generated-first")).toFile());
        assertEquals(1, generated.size());
        byte[] firstGeneratedBytes = Files.readAllBytes(generated.get(0).toPath());
        String firstGeneratedCode = new String(firstGeneratedBytes, StandardCharsets.UTF_8);
        assertGeneratedSyntax(firstGeneratedCode);

        App reparsed = parse(generated.get(0), "parse-second");
        assertPartialSpecialization(reparsed);
        assertTemplateParameterContext(reparsed);
        assertInitCaptures(reparsed);
        assertFunctionalCasts(reparsed);
        assertReferenceCastType(reparsed);

        List<File> regenerated = reparsed.write(Files.createDirectories(temporary.resolve("generated-second")).toFile());
        assertEquals(1, regenerated.size());
        assertEquals(firstGeneratedCode, Files.readString(regenerated.get(0).toPath()),
                "Generated source must remain stable after reparsing");
    }

    private App parse(File source, String parseRootName) throws IOException {
        Path parseRoot = Files.createDirectories(temporary.resolve(parseRootName));

        CodeParser parser = CodeParser.newInstance();
        parser.set(CodeParser.GENERATED_PARSE_ROOT, parseRoot.toFile());
        parser.set(CodeParser.AST_DUMP_CACHE, false);
        parser.set(CodeParser.SHOW_EXEC_INFO, false);
        parser.set(ParallelCodeParser.PARALLEL_PARSING, false);

        return parser.parse(List.of(source), OPTIONS);
    }

    private static void assertPartialSpecialization(App app) {
        List<ClassTemplatePartialSpecializationDecl> partials = app
                .getDescendants(ClassTemplatePartialSpecializationDecl.class);
        ClassTemplatePartialSpecializationDecl partial = partials.stream()
                .filter(candidate -> candidate.getDeclName().equals("Box"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing Box<T *> partial specialization"));

        assertEquals(1, partial.get(ClassTemplatePartialSpecializationDecl.TEMPLATE_PARAMETERS).size());
        NamedDecl parameter = partial.get(ClassTemplatePartialSpecializationDecl.TEMPLATE_PARAMETERS).get(0);
        assertEquals("T", parameter.getDeclName());
        TemplateTypeParmDecl templateParameter = assertInstanceOf(TemplateTypeParmDecl.class, parameter);
        TemplateTypeParmType declarationType = assertInstanceOf(TemplateTypeParmType.class,
                templateParameter.get(TypeDecl.TYPE_FOR_DECL).orElseThrow(
                        () -> new AssertionError("Template parameter has no declared type metadata")));

        String code = partial.getCode();
        assertTrue(code.startsWith("template<typename T>"), "Partial template parameters were lost:\n" + code);
        assertMatches(code, "Box\\s*<\\s*T\\s*\\*\\s*>");

        TemplateArgumentType typeArgument = partial.get(ClassTemplatePartialSpecializationDecl.TEMPLATE_ARGUMENTS).stream()
                .filter(TemplateArgumentType.class::isInstance)
                .map(TemplateArgumentType.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing type argument for Box<T *>"));
        Type argumentType = typeArgument.get(TemplateArgumentType.TYPE);
        assertInstanceOf(PointerType.class, argumentType);
        Type pointee = ((PointerType) argumentType).getPointeeType();
        if (pointee instanceof QualType qualified) {
            pointee = qualified.getUnqualifiedType();
        }
        TemplateTypeParmType templateType = assertInstanceOf(TemplateTypeParmType.class, pointee);
        assertEquals(declarationType.get(TemplateTypeParmType.DEPTH), templateType.get(TemplateTypeParmType.DEPTH));
        assertEquals(declarationType.get(TemplateTypeParmType.INDEX), templateType.get(TemplateTypeParmType.INDEX));
        templateType.get(TemplateTypeParmType.DECL).ifPresent(linkedDecl -> {
            NamedDecl linkedParameter = assertInstanceOf(NamedDecl.class, linkedDecl);
            assertEquals("T", linkedParameter.getDeclName());
        });
        assertEquals("T", templateType.getCode(partial),
                "A null Clang type-parameter declaration must use the partial specialization's named parameter");

        ClassTemplatePartialSpecializationDecl nested = partials.stream()
                .filter(candidate -> candidate.getCode().contains("Inner"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing nested Inner<I *> partial specialization"));
        assertEquals("I", nested.get(ClassTemplatePartialSpecializationDecl.TEMPLATE_PARAMETERS).get(0).getDeclName());
        assertMatches(nested.getCode(), "template\\s*<\\s*class I\\s*>\\s*struct\\s+Inner\\s*<\\s*I\\s*\\*\\s*>");
    }

    private static void assertInitCaptures(App app) {
        LambdaExpr lambda = app.getDescendants(LambdaExpr.class).stream()
                .filter(candidate -> candidate.get(LambdaExpr.INIT_CAPTURE_NAMES).contains("copy"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing lambda init-capture metadata"));

        assertEquals(List.of("copy"), lambda.get(LambdaExpr.INIT_CAPTURE_NAMES));
        assertEquals(List.of(LambdaCaptureKind.ByCopy), lambda.get(LambdaExpr.CAPTURE_KINDS));
        assertEquals(1, lambda.getCaptureArguments().size());
        assertMatches(lambda.getCode(), "\\[copy\\s*=\\s*1\\]");
    }

    private static void assertTemplateParameterContext(App app) {
        ClassTemplatePartialSpecializationDecl partialMethod = app
                .getDescendants(ClassTemplatePartialSpecializationDecl.class).stream()
                .filter(candidate -> candidate.getDeclName().equals("PartialMethod"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing PartialMethod<T *, N> specialization"));
        assertMatches(partialMethod.getCode(), "PartialMethod\\s*<\\s*T\\s*\\*\\s*,\\s*N\\s*>");
        assertMatches(partialMethod.getCode(), "\\bT\\s+value\\s*\\(");

        ClassTemplatePartialSpecializationDecl memberPointer = app
                .getDescendants(ClassTemplatePartialSpecializationDecl.class).stream()
                .filter(candidate -> candidate.getDeclName().equals("MemberPointerKind"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing member-function pointer specialization"));
        TemplateArgumentType memberArgument = assertInstanceOf(TemplateArgumentType.class,
                memberPointer.get(ClassTemplatePartialSpecializationDecl.TEMPLATE_ARGUMENTS).get(0));
        MemberPointerType memberType = assertInstanceOf(MemberPointerType.class,
                memberArgument.get(TemplateArgumentType.TYPE));
        assertTrue(memberType.hasValue(MemberPointerType.CLASS_TYPE)
                && memberType.hasValue(MemberPointerType.POINTEE_TYPE),
                "Member pointer type components must be read from structured Protobuf metadata");
        assertMatches(memberPointer.getCode(),
                "MemberPointerKind\\s*<\\s*R\\s*\\(\\s*C\\s*::\\s*\\*\\s*\\)\\s*\\(\\s*Args\\s*\\.\\.\\.\\s*\\)\\s*>");

        ClassTemplatePartialSpecializationDecl qualifiedMemberPointer = app
                .getDescendants(ClassTemplatePartialSpecializationDecl.class).stream()
                .filter(candidate -> candidate.getDeclName().equals("QualifiedMemberPointerKind"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing qualified member-function pointer specialization"));
        assertMatches(qualifiedMemberPointer.getCode(),
                "QualifiedMemberPointerKind\\s*<\\s*R\\s*\\(\\s*C\\s*::\\s*\\*\\s*\\)\\s*\\(\\s*Args\\s*\\.\\.\\.\\s*\\)\\s*const\\s*&\\s*noexcept\\s*>");

        ClassTemplatePartialSpecializationDecl memberArrayPointer = app
                .getDescendants(ClassTemplatePartialSpecializationDecl.class).stream()
                .filter(candidate -> candidate.getDeclName().equals("MemberArrayPointerKind"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing member-array pointer specialization"));
        assertMatches(memberArrayPointer.getCode(),
                "MemberArrayPointerKind\\s*<\\s*T\\s*\\(\\s*C\\s*::\\s*\\*\\s*\\)\\s*\\[\\s*N\\s*\\]\\s*>");

        ClassTemplatePartialSpecializationDecl functionSignature = app
                .getDescendants(ClassTemplatePartialSpecializationDecl.class).stream()
                .filter(candidate -> candidate.getDeclName().equals("FunctionSignature"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing variadic function signature specialization"));
        assertMatches(functionSignature.getCode(),
                "FunctionSignature\\s*<\\s*R\\s*\\(\\s*Args\\s*\\.\\.\\.\\s*\\)\\s*>");

        ClassTemplatePartialSpecializationDecl nested = app
                .getDescendants(ClassTemplatePartialSpecializationDecl.class).stream()
                .filter(candidate -> candidate.getCode().contains("Inner"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing nested Inner<I *> partial specialization"));
        assertMatches(nested.getCode(), "\\bO\\s*\\*\\s*outer");
        assertMatches(nested.getCode(), "\\bI\\s*\\*\\s*inner");
    }

    private static void assertFunctionalCasts(App app) {
        List<CXXFunctionalCastExpr> casts = app.getDescendants(CXXFunctionalCastExpr.class);
        assertTrue(casts.stream().anyMatch(cast -> cast.getCode().startsWith("ValueArray{")),
                "An array braced cast must use braces: " + casts.stream().map(CXXFunctionalCastExpr::getCode).toList());
        assertTrue(casts.stream().anyMatch(cast -> cast.getCode().startsWith("Point{")),
                "An aggregate braced functional cast must use braces: "
                        + casts.stream().map(CXXFunctionalCastExpr::getCode).toList());
        assertTrue(casts.stream().anyMatch(cast -> cast.getCode().startsWith("Value(")),
                "A parenthesized functional cast must keep parentheses: "
                        + casts.stream().map(CXXFunctionalCastExpr::getCode).toList());
    }

    private static void assertReferenceCastType(App app) {
        CStyleCastExpr cast = app.getDescendants(CStyleCastExpr.class).stream()
                .filter(candidate -> candidate.getSubExpr().getCode().equals("holder"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing cast from a Holder lvalue to const Holder&"));
        assertMatches(cast.getCode(), "\\(\\s*Holder\\s+const\\s*&\\s*\\)");

        CStyleCastExpr syntheticCast = cast.getFactory().cStyleCastExpr(
                cast.getFactory().builtinType("int"), cast.getSubExpr());
        assertTrue(syntheticCast.hasValue(CStyleCastExpr.TYPE_AS_WRITTEN),
                "Factory-created casts retain the type they were built with");
        assertTrue(syntheticCast.getCode().startsWith("(int) "));
    }

    private static void assertGeneratedSyntax(String code) {
        assertFalse(code.contains("type-parameter-"),
                "Generated declarations must use named template parameters rather than Clang's canonical placeholders:\n"
                        + code);
        assertMatches(code, "Box\\s*<\\s*T\\s*\\*\\s*>");
        assertMatches(code, "Inner\\s*<\\s*I\\s*\\*\\s*>");
        assertMatches(code, "PartialMethod\\s*<\\s*T\\s*\\*\\s*,\\s*N\\s*>");
        assertMatches(code, "PartialMethod\\s*<\\s*X\\s*\\*\\s*,\\s*N\\s*>\\s*::\\s*value");
        assertMatches(code,
                "MemberPointerKind\\s*<\\s*R\\s*\\(\\s*C\\s*::\\s*\\*\\s*\\)\\s*\\(\\s*Args\\s*\\.\\.\\.\\s*\\)\\s*>");
        assertMatches(code,
                "QualifiedMemberPointerKind\\s*<\\s*R\\s*\\(\\s*C\\s*::\\s*\\*\\s*\\)\\s*\\(\\s*Args\\s*\\.\\.\\.\\s*\\)\\s*const\\s*&\\s*noexcept\\s*>");
        assertMatches(code,
                "MemberArrayPointerKind\\s*<\\s*T\\s*\\(\\s*C\\s*::\\s*\\*\\s*\\)\\s*\\[\\s*N\\s*\\]\\s*>");
        assertMatches(code, "FunctionSignature\\s*<\\s*R\\s*\\(\\s*Args\\s*\\.\\.\\.\\s*\\)\\s*>");
        assertMatches(code, "\\bint\\s+values\\s*\\[2\\]\\s*(?:=\\s*)?\\{\\s*1\\s*,\\s*2\\s*}");
        assertMatches(code, "\\bPoint\\s+aggregate\\s*(?:=\\s*)?\\{[^}]+}");
        assertMatches(code, "Point\\s*\\{[^}]+}");
        assertMatches(code, "Value\\s*\\{[^}]+}");
        assertMatches(code, "ValueArray\\s*\\{[^}]+}");
        assertMatches(code, "Value\\s*\\([^)]*\\)");
        assertMatches(code,
                "PointerHolder\\s*\\(\\s*\\)\\s*:\\s*p\\s*\\(\\s*nullptr\\s*\\),\\s*q\\s*\\(\\s*\\(\\s*nullptr\\s*\\)\\s*\\)");
        assertMatches(code, "\\(\\s*Holder\\s+const\\s*&\\s*\\)");
        assertMatches(code, "\\[copy\\s*=\\s*1\\]");
    }

    private static void assertMatches(String code, String regex) {
        assertTrue(Pattern.compile(regex).matcher(code).find(), "Expected generated code to match: " + regex
                + "\nGenerated code:\n" + code);
    }
}
