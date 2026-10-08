/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import pt.up.fe.specs.clava.ast.decl.TypedefDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.type.LValueReferenceType;
import pt.up.fe.specs.clava.ast.type.RValueReferenceType;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

class ReferenceAndAutoFidelityTest {
    private static final List<String> OPTIONS = List.of("-std=c++11");
    private static final String FIXTURE = "cxx/wire_reference_auto.cpp";

    @TempDir
    Path temporary;

    @Test
    void referenceAliasesAndAutoBindingsSurviveProtobufRoundTrip() throws IOException {
        SpecsSystem.programStandardInit();

        Path sourceDirectory = Files.createDirectories(temporary.resolve("source"));
        File source = SpecsIo.resourceCopy(FIXTURE, sourceDirectory.toFile(), false, true);
        assertTrue(source.isFile());

        App parsed = parse(source, "native-first");
        assertReferenceCategories(parsed);

        Path firstOutputDirectory = Files.createDirectories(temporary.resolve("generated-first"));
        List<File> firstGeneratedFiles = parsed.write(firstOutputDirectory.toFile());
        assertEquals(1, firstGeneratedFiles.size());
        File firstGenerated = firstGeneratedFiles.get(0);
        byte[] firstGeneratedBytes = Files.readAllBytes(firstGenerated.toPath());
        String firstGeneratedCode = new String(firstGeneratedBytes, StandardCharsets.UTF_8);

        assertAliasRetained(firstGeneratedCode, "LRI", "r1");
        assertAliasRetained(firstGeneratedCode, "LRI", "r2");
        assertAliasRetained(firstGeneratedCode, "LRI", "r3");
        assertAliasRetained(firstGeneratedCode, "RRI", "r4");
        assertAliasRetained(firstGeneratedCode, "RRI", "r5");
        assertMatches(firstGeneratedCode, "auto\\s*&\\s*from_lvalue\\b");
        assertMatches(firstGeneratedCode, "auto\\s*&&\\s*from_xvalue\\b");
        assertMatches(firstGeneratedCode, "auto\\s*&\\s*from_member\\b");

        App reparsed = parse(firstGenerated, "native-reparse");
        assertReferenceCategories(reparsed);

        Path secondOutputDirectory = Files.createDirectories(temporary.resolve("generated-second"));
        List<File> secondGeneratedFiles = reparsed.write(secondOutputDirectory.toFile());
        assertEquals(1, secondGeneratedFiles.size());
        assertArrayEquals(firstGeneratedBytes, Files.readAllBytes(secondGeneratedFiles.get(0).toPath()),
                "Generating after reparsing must be byte-identical");
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

    private static void assertReferenceCategories(App app) {
        assertInstanceOf(LValueReferenceType.class, findTypedef(app, "r1").getUnderlyingType());
        assertInstanceOf(LValueReferenceType.class, findTypedef(app, "r2").getUnderlyingType());
        assertInstanceOf(LValueReferenceType.class, findTypedef(app, "r3").getUnderlyingType());
        assertInstanceOf(LValueReferenceType.class, findTypedef(app, "r4").getUnderlyingType());
        assertInstanceOf(RValueReferenceType.class, findTypedef(app, "r5").getUnderlyingType());

        assertInstanceOf(LValueReferenceType.class, findVariable(app, "from_lvalue").getType());
        assertInstanceOf(RValueReferenceType.class, findVariable(app, "from_xvalue").getType());
        assertInstanceOf(LValueReferenceType.class, findVariable(app, "from_member").getType());
    }

    private static TypedefDecl findTypedef(App app, String name) {
        return app.getDescendants(TypedefDecl.class).stream()
                .filter(declaration -> declaration.getDeclName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing typedef " + name));
    }

    private static VarDecl findVariable(App app, String name) {
        return app.getDescendants(VarDecl.class).stream()
                .filter(declaration -> declaration.getDeclName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing variable " + name));
    }

    private static void assertAliasRetained(String code, String typeAlias, String declaration) {
        String pattern = "(?m)^\\s*typedef\\s+(?:const\\s+)?" + Pattern.quote(typeAlias)
                + "\\s*&&?\\s*" + Pattern.quote(declaration) + "\\s*;\\s*$";
        assertMatches(code, pattern);
    }

    private static void assertMatches(String code, String regex) {
        assertTrue(Pattern.compile(regex).matcher(code).find(), "Expected generated code to match: " + regex
                + "\nGenerated code:\n" + code);
    }
}
