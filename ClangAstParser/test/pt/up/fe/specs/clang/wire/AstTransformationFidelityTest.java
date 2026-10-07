/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionTemplateDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.CStyleCastExpr;
import pt.up.fe.specs.clava.ast.expr.CXXUnresolvedConstructExpr;
import pt.up.fe.specs.clava.ast.expr.LambdaExpr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.stmt.AsmStmt;
import pt.up.fe.specs.clava.ast.stmt.GCCAsmStmt;
import pt.up.fe.specs.clava.ast.stmt.data.AsmInput;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.MemberPointerType;
import pt.up.fe.specs.clava.ast.type.ParenType;
import pt.up.fe.specs.clava.ast.type.QualType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

class AstTransformationFidelityTest {
    @TempDir
    Path temporary;

    @Test
    void editedTemplateConstructionLambdaAndMemberPointerGenerateCompilableCurrentAst() throws Exception {
        SpecsSystem.programStandardInit();
        File source = SpecsIo.resourceCopy("cxx/wire_ast_fidelity.cpp", temporary.toFile(), false, true);
        App first = parse(source.toPath(), "source-parse", List.of("-std=c++17"));

        LambdaExpr lambda = first.getDescendants(LambdaExpr.class).stream()
                .filter(candidate -> candidate.getAncestorTry(VarDecl.class)
                        .map(variable -> variable.getDeclName().equals("init_captures")).orElse(false))
                .findFirst().orElseThrow();
        lambda.setChild(0, first.getFactory().integerLiteral(9));

        FunctionDecl makeFrom = first.getDescendants(FunctionDecl.class).stream()
                .filter(function -> function.getDeclName().equals("make_from"))
                .findFirst().orElseThrow();
        CXXUnresolvedConstructExpr dependentConstruction = makeFrom.getDescendants(CXXUnresolvedConstructExpr.class)
                .stream().findFirst().orElseThrow();
        Type valueType = first.getDescendants(RecordDecl.class).stream()
                .filter(record -> record.getDeclName().equals("Value"))
                .findFirst().orElseThrow().getType();
        dependentConstruction.setType(valueType);

        FunctionTemplateDecl makeFromTemplate = first.getDescendants(FunctionTemplateDecl.class).stream()
                .filter(template -> template.getTemplateDecl() == makeFrom)
                .findFirst().orElseThrow();
        makeFromTemplate.getTemplateParameters().get(0).setDeclName("Source");

        VarDecl memberPointerVariable = first.getDescendants(VarDecl.class).stream()
                .filter(variable -> variable.getDeclName().equals("member_function"))
                .findFirst().orElseThrow();
        MemberPointerType memberPointer = memberPointerVariable.getType().getTypeDescendantsAndSelfStream()
                .filter(MemberPointerType.class::isInstance)
                .map(MemberPointerType.class::cast)
                .filter(pointer -> findFunctionType(pointer.getPointeeType()) != null)
                .findFirst().orElseThrow();
        FunctionProtoType functionType = findFunctionType(memberPointer.getPointeeType());
        if (functionType == null) {
            throw new AssertionError("Member-function pointer has no structured FunctionProtoType pointee: "
                    + memberPointer.getPointeeType().getClass().getSimpleName());
        }
        functionType.setReturnType(first.getFactory().builtinType("long"));

        File generated = writeSource(first, source.getName(), "first-output");
        String generatedCode = Files.readString(generated.toPath());
        assertTrue(generatedCode.contains("copy = 9"), generatedCode);
        assertTrue(generatedCode.contains("template <typename Source>"), generatedCode);
        assertTrue(generatedCode.contains("Value{constructor()}"), generatedCode);
        assertMemberPointerReturnType(generatedCode);

        // Parsing the generated translation unit is an independent Clang syntax check
        // and also verifies that the edit survived consumer decoding.
        App second = parse(generated.toPath(), "generated-parse", List.of("-std=c++17"));
        String reparsedCode = second.getCode();
        assertTrue(reparsedCode.contains("copy = 9"), reparsedCode);
        assertTrue(reparsedCode.contains("template <typename Source>"), reparsedCode);
        assertTrue(reparsedCode.contains("Value{constructor()}"), reparsedCode);
        assertMemberPointerReturnType(reparsedCode);
    }

    @Test
    void editedAssemblyInputExpressionIsPrintedAndReparsed() throws Exception {
        SpecsSystem.programStandardInit();
        Path source = Files.writeString(temporary.resolve("asm_edit.cpp"),
                "int asm_edit(int input, int replacement) {\n"
                        + "  int output;\n"
                        + "  __asm__ volatile (\"\" : \"=r\"(output) : \"r\"(input));\n"
                        + "  return output;\n"
                        + "}\n");

        App first = parse(source, "asm-source-parse", List.of("-std=c++17"));
        FunctionDecl function = first.getDescendants(FunctionDecl.class).stream()
                .filter(candidate -> candidate.getDeclName().equals("asm_edit"))
                .findFirst().orElseThrow();
        var replacement = function.getParameters().stream()
                .filter(parameter -> parameter.getDeclName().equals("replacement"))
                .findFirst().orElseThrow();
        GCCAsmStmt asm = first.getDescendants(GCCAsmStmt.class).stream().findFirst().orElseThrow();
        AsmInput input = asm.get(AsmStmt.INPUTS).get(0);
        input.set(AsmInput.EXPR, first.getFactory().declRefExpr(replacement));

        File generated = writeSource(first, source.getFileName().toString(), "asm-first-output");
        String code = Files.readString(generated.toPath());
        assertTrue(code.contains("\"r\" (replacement)"), code);

        App second = parse(generated.toPath(), "asm-generated-parse", List.of("-std=c++17"));
        assertTrue(second.getCode().contains("\"r\" (replacement)"), second.getCode());
    }

    @Test
    void castTypeSetterUpdatesTypeAsWrittenUsedByPrinter() throws Exception {
        SpecsSystem.programStandardInit();
        Path source = Files.writeString(temporary.resolve("cast_edit.cpp"),
                "long cast_edit(double value) { return (int)value; }\n");

        App first = parse(source, "cast-source-parse", List.of("-std=c++17"));
        CStyleCastExpr cast = first.getDescendants(CStyleCastExpr.class).stream().findFirst().orElseThrow();
        cast.setType(first.getFactory().builtinType("long"));

        File generated = writeSource(first, source.getFileName().toString(), "cast-first-output");
        String code = Files.readString(generated.toPath());
        assertTrue(code.contains("(long) value"), code);

        App second = parse(generated.toPath(), "cast-generated-parse", List.of("-std=c++17"));
        assertTrue(second.getCode().contains("(long) value"), second.getCode());
    }

    private App parse(Path source, String name, List<String> options) throws Exception {
        CodeParser parser = CodeParser.newInstance();
        parser.set(CodeParser.GENERATED_PARSE_ROOT, Files.createDirectories(temporary.resolve(name)).toFile());
        parser.set(CodeParser.AST_DUMP_CACHE, false);
        parser.set(CodeParser.SHOW_EXEC_INFO, false);
        parser.set(ParallelCodeParser.PARALLEL_PARSING, false);
        return parser.parse(List.of(source.toFile()), options);
    }

    private File writeSource(App app, String sourceName, String outputFolder) throws Exception {
        return app.write(Files.createDirectories(temporary.resolve(outputFolder)).toFile()).stream()
                .filter(file -> file.getName().equals(sourceName))
                .findFirst().orElseThrow();
    }

    private static FunctionProtoType findFunctionType(Type type) {
        if (type instanceof FunctionProtoType functionType) {
            return functionType;
        }
        if (type instanceof QualType qualified) {
            return findFunctionType(qualified.getUnqualifiedType());
        }
        if (type instanceof ParenType parenthesized) {
            return findFunctionType(parenthesized.getInnerType());
        }
        return null;
    }

    private static void assertMemberPointerReturnType(String code) {
        assertTrue(Pattern.compile("long\\s*\\(\\s*PartialMethod\\s*<\\s*int\\s*\\*\\s*,\\s*3\\s*>\\s*::\\s*\\*\\s*\\)\\s*\\(\\s*int\\s*\\)")
                .matcher(code).find(), code);
    }
}
