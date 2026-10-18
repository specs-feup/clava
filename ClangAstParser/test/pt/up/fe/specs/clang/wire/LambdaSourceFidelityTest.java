/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.expr.LambdaExpr;
import pt.up.fe.specs.clava.ast.expr.enums.LambdaCaptureKind;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

class LambdaSourceFidelityTest {
    private static final List<String> OPTIONS = List.of("-std=c++20");
    private static final String FIXTURE = "cxx/lambda_source_fidelity.cpp";

    @TempDir
    Path temporary;

    @Test
    void preservesInitStylesAndPackExpansionThroughParseGenerateReparse() throws IOException {
        SpecsSystem.programStandardInit();

        Path sourceDirectory = Files.createDirectories(temporary.resolve("source"));
        File source = SpecsIo.resourceCopy(FIXTURE, sourceDirectory.toFile(), false, true);
        assertTrue(source.isFile());

        App parsed = parse(source, "parse-first");
        assertCapture(parsed, "direct_list", List.of("item"), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.LIST_INIT), List.of(false), "item\\s*\\{\\s*value\\s*}");
        assertCapture(parsed, "copy_init", List.of("item"), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.CINIT), List.of(false), "item\\s*=\\s*value");
        assertCapture(parsed, "call_init", List.of("item"), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.CALL_INIT), List.of(false), "item\\(\\s*value\\s*\\)");
        assertCapture(parsed, "regular_copy_pack", List.of(""), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.CINIT), List.of(true), "values\\.\\.\\.");
        assertCapture(parsed, "regular_ref_pack", List.of(""), List.of(LambdaCaptureKind.ByRef),
                List.of(InitializationStyle.CINIT), List.of(true), "&\\s*values\\.\\.\\.");
        assertCapture(parsed, "init_capture_pack_call", List.of("items"), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.CALL_INIT), List.of(false), "items\\(\\s*values\\.\\.\\.\\s*\\)");
        assertCapture(parsed, "init_capture_pack_list", List.of("items"), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.LIST_INIT), List.of(false), "items\\{\\s*values\\.\\.\\.\\s*}");
        assertCapture(parsed, "init_copy_pack", List.of("items"), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.CINIT), List.of(true), "\\.\\.\\.items\\s*=\\s*values");
        assertCapture(parsed, "init_ref_pack", List.of("items"), List.of(LambdaCaptureKind.ByRef),
                List.of(InitializationStyle.CINIT), List.of(true), "&\\s*\\.\\.\\.items\\s*=\\s*values");
        assertDefaultCapture(parsed, "copy_default", List.of(true), "\\[=\\]");
        assertDefaultCapture(parsed, "ref_default", List.of(true), "\\[&\\]");
        assertMixedDefaultCapture(parsed, "mixed_default");

        List<File> generated = parsed.write(Files.createDirectories(temporary.resolve("generated-first")).toFile());
        assertEquals(1, generated.size());
        String generatedCode = Files.readString(generated.get(0).toPath(), StandardCharsets.UTF_8);
        assertGeneratedCaptures(generatedCode);

        App reparsed = parse(generated.get(0), "parse-second");
        assertCapture(reparsed, "direct_list", List.of("item"), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.LIST_INIT), List.of(false), "item\\s*\\{\\s*value\\s*}");
        assertCapture(reparsed, "copy_init", List.of("item"), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.CINIT), List.of(false), "item\\s*=\\s*value");
        assertCapture(reparsed, "call_init", List.of("item"), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.CALL_INIT), List.of(false), "item\\(\\s*value\\s*\\)");
        assertCapture(reparsed, "regular_copy_pack", List.of(""), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.CINIT), List.of(true), "values\\.\\.\\.");
        assertCapture(reparsed, "regular_ref_pack", List.of(""), List.of(LambdaCaptureKind.ByRef),
                List.of(InitializationStyle.CINIT), List.of(true), "&\\s*values\\.\\.\\.");
        assertCapture(reparsed, "init_capture_pack_call", List.of("items"), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.CALL_INIT), List.of(false), "items\\(\\s*values\\.\\.\\.\\s*\\)");
        assertCapture(reparsed, "init_capture_pack_list", List.of("items"), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.LIST_INIT), List.of(false), "items\\{\\s*values\\.\\.\\.\\s*}");
        assertCapture(reparsed, "init_copy_pack", List.of("items"), List.of(LambdaCaptureKind.ByCopy),
                List.of(InitializationStyle.CINIT), List.of(true), "\\.\\.\\.items\\s*=\\s*values");
        assertCapture(reparsed, "init_ref_pack", List.of("items"), List.of(LambdaCaptureKind.ByRef),
                List.of(InitializationStyle.CINIT), List.of(true), "&\\s*\\.\\.\\.items\\s*=\\s*values");
        assertDefaultCapture(reparsed, "copy_default", List.of(true), "\\[=\\]");
        assertDefaultCapture(reparsed, "ref_default", List.of(true), "\\[&\\]");
        assertMixedDefaultCapture(reparsed, "mixed_default");

        Path secondGeneratedDirectory = Files.createDirectories(temporary.resolve("generated-second"));
        List<File> regenerated = reparsed.write(secondGeneratedDirectory.toFile());
        assertEquals(1, regenerated.size());
        assertEquals(generatedCode, Files.readString(regenerated.get(0).toPath(), StandardCharsets.UTF_8),
                "Generated lambda captures must remain stable after reparsing");
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

    private static void assertCapture(App app, String variableName, List<String> names,
            List<LambdaCaptureKind> kinds, List<InitializationStyle> styles, List<Boolean> packExpansions,
            String captureRegex) {
        LambdaExpr lambda = app.getDescendants(LambdaExpr.class).stream()
                .filter(candidate -> candidate.getAncestorTry(VarDecl.class)
                        .map(variable -> variable.getDeclName().equals(variableName)).orElse(false))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing lambda variable " + variableName));

        assertEquals(names, lambda.get(LambdaExpr.INIT_CAPTURE_NAMES), variableName + " capture names");
        assertEquals(kinds, lambda.get(LambdaExpr.CAPTURE_KINDS), variableName + " capture kinds");
        assertEquals(styles, lambda.get(LambdaExpr.CAPTURE_INIT_STYLES), variableName + " capture init styles");
        assertEquals(packExpansions, lambda.get(LambdaExpr.CAPTURE_PACK_EXPANSIONS), variableName + " pack flags");
        assertEquals(java.util.Collections.nCopies(kinds.size(), false),
                lambda.get(LambdaExpr.CAPTURE_IS_IMPLICIT), variableName + " implicit-capture flags");
        assertTrue(Pattern.compile(captureRegex).matcher(getCaptureList(lambda)).find(),
                variableName + " should emit capture syntax matching " + captureRegex + " but got " + lambda.getCode());
    }

    private static void assertDefaultCapture(App app, String variableName, List<Boolean> expectedImplicitFlags,
            String captureRegex) {
        LambdaExpr lambda = findLambda(app, variableName);
        assertEquals(expectedImplicitFlags, lambda.get(LambdaExpr.CAPTURE_IS_IMPLICIT), variableName);
        assertAlignedCaptureData(lambda, variableName);
        assertTrue(Pattern.compile(captureRegex).matcher(getCaptureList(lambda)).find(),
                variableName + " should emit capture syntax matching " + captureRegex + " but got " + lambda.getCode());
    }

    private static void assertMixedDefaultCapture(App app, String variableName) {
        LambdaExpr lambda = findLambda(app, variableName);
        List<Boolean> implicitFlags = lambda.get(LambdaExpr.CAPTURE_IS_IMPLICIT);
        assertEquals(2, implicitFlags.size(), variableName + " capture count");
        assertTrue(implicitFlags.contains(true) && implicitFlags.contains(false),
                variableName + " must preserve both explicit and implicit capture metadata: " + implicitFlags);
        assertAlignedCaptureData(lambda, variableName);
        assertTrue(Pattern.compile("\\[=\\s*,\\s*&other\\]").matcher(getCaptureList(lambda)).find(),
                variableName + " should print only its explicit capture after the default: " + lambda.getCode());
    }

    private static void assertAlignedCaptureData(LambdaExpr lambda, String variableName) {
        int count = lambda.getCaptureArguments().size();
        assertEquals(count, lambda.get(LambdaExpr.CAPTURE_KINDS).size(), variableName + " capture kinds");
        assertEquals(count, lambda.get(LambdaExpr.INIT_CAPTURE_NAMES).size(), variableName + " init-capture names");
        assertEquals(count, lambda.get(LambdaExpr.CAPTURE_INIT_STYLES).size(), variableName + " init styles");
        assertEquals(count, lambda.get(LambdaExpr.CAPTURE_PACK_EXPANSIONS).size(), variableName + " pack flags");
        assertEquals(count, lambda.get(LambdaExpr.CAPTURE_IS_IMPLICIT).size(), variableName + " implicit flags");
    }

    private static LambdaExpr findLambda(App app, String variableName) {
        return app.getDescendants(LambdaExpr.class).stream()
                .filter(candidate -> candidate.getAncestorTry(VarDecl.class)
                        .map(variable -> variable.getDeclName().equals(variableName)).orElse(false))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing lambda variable " + variableName));
    }

    private static String getCaptureList(LambdaExpr lambda) {
        String code = lambda.getCode();
        int close = code.indexOf(']');
        assertTrue(close >= 0, "Lambda code has no capture-list terminator: " + code);
        return code.substring(0, close + 1);
    }

    private static void assertGeneratedCaptures(String code) {
        assertTrue(Pattern.compile("\\[item\\s*\\{\\s*value\\s*}\\]").matcher(code).find(), code);
        assertTrue(Pattern.compile("\\[item\\s*=\\s*value\\]").matcher(code).find(), code);
        assertTrue(Pattern.compile("\\[item\\(\\s*value\\s*\\)\\]").matcher(code).find(), code);
        assertTrue(Pattern.compile("\\[values\\.\\.\\.\\]").matcher(code).find(), code);
        assertTrue(Pattern.compile("\\[&\\s*values\\.\\.\\.\\]").matcher(code).find(), code);
        assertTrue(Pattern.compile("\\[items\\(\\s*values\\.\\.\\.\\s*\\)\\]").matcher(code).find(), code);
        assertTrue(Pattern.compile("\\[items\\{\\s*values\\.\\.\\.\\s*}\\]").matcher(code).find(), code);
        assertTrue(Pattern.compile("\\[\\.\\.\\.items\\s*=\\s*values\\]").matcher(code).find(), code);
        assertTrue(Pattern.compile("\\[&\\s*\\.\\.\\.items\\s*=\\s*values\\]").matcher(code).find(), code);
        assertTrue(Pattern.compile("\\[=\\]").matcher(code).find(), code);
        assertTrue(Pattern.compile("\\[&\\]").matcher(code).find(), code);
        assertTrue(Pattern.compile("\\[=\\s*,\\s*&other\\]").matcher(code).find(), code);
    }
}
