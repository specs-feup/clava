/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

class GnuAttributesAndStandaloneOmpFidelityTest {
    @TempDir
    Path temporary;

    @Test
    void preservesGnuAttributeSpellingsAndStandaloneOpenMpRoundTrip() throws IOException {
        SpecsSystem.programStandardInit();
        File source = SpecsIo.resourceCopy("c/roundtrip_fixes.c",
                Files.createDirectories(temporary.resolve("source")).toFile(), false, true);

        App first = parse(source, "first-parse");
        File generated = first.write(Files.createDirectories(temporary.resolve("first-code")).toFile()).get(0);
        String code = Files.readString(generated.toPath());

        assertTrue(code.contains("__attribute__((malloc))"), code);
        assertFalse(code.contains("__attribute__((restrict))"), code);
        assertTrue(code.contains("__attribute__((nothrow))"), code);
        assertFalse(code.contains("__attribute__((no_throw))"), code);
        assertTrue(code.indexOf("__attribute__((const))") < code.indexOf("__attribute__((nothrow))"), code);
        assertTrue(code.contains("++ *x;") || code.contains("++*x;"), code);
        int macroPragma = code.lastIndexOf("#pragma omp parallel");
        int macroAssignment = code.indexOf("*x = 7;", macroPragma);
        int macroBodyStart = code.indexOf('{', macroPragma);
        int macroBodyEnd = code.indexOf('}', macroBodyStart);
        assertTrue(macroPragma >= 0 && macroBodyStart > macroPragma && macroBodyEnd < macroAssignment
                && macroAssignment > macroBodyEnd, code);

        App second = parse(generated, "second-parse");
        File regenerated = second.write(Files.createDirectories(temporary.resolve("second-code")).toFile()).get(0);
        assertEquals(code, Files.readString(regenerated.toPath()));
    }

    private App parse(File source, String name) throws IOException {
        CodeParser parser = CodeParser.newInstance();
        parser.set(CodeParser.GENERATED_PARSE_ROOT, Files.createDirectories(temporary.resolve(name)).toFile());
        parser.set(CodeParser.AST_DUMP_CACHE, false);
        parser.set(CodeParser.SHOW_EXEC_INFO, false);
        parser.set(ParallelCodeParser.PARALLEL_PARSING, false);
        return parser.parse(List.of(source), List.of("-fopenmp", "-std=gnu11"));
    }
}
