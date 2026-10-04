/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ast.extra.App;

class MSAsmStmtSourceFidelityTest {
    private static final String INSIDE_COMMENT = "// Keep this source comment inside the asm block.";
    private static final String BLOCK_COMMENT = "/* Keep this block comment inside the asm body.";
    private static final String OUTSIDE_COMMENT = "// Keep this comment outside the asm block.";

    @TempDir
    Path work;

    @Test
    void retainsMSAsmBodyWhitespaceAcrossGenerateAndReparse() throws Exception {
        Path source = work.resolve("ms_asm.cpp");
        Files.writeString(source, "void ms_asm_body() {\n"
                + "  __asm {\n"
                + "    " + INSIDE_COMMENT + "\n"
                + "    " + BLOCK_COMMENT + "\n"
                + "     * Keep its second line with the MS asm source.\n"
                + "     */\n"
                + "    mov eax, ebx\n"
                + "    nop\n"
                + "  } " + OUTSIDE_COMMENT + "\n"
                + "}\n");

        List<String> options = List.of("-std=c++11", "-fms-extensions", "-fasm-blocks",
                "-target", "i386-unknown-unknown");
        Path firstParseRoot = Files.createDirectory(work.resolve("first-parse"));
        App first = parse(source, firstParseRoot, options);
        List<File> firstFiles = first.write(Files.createDirectory(work.resolve("first-output")).toFile());
        Path firstOutput = matchingSource(firstFiles, source.getFileName().toString());

        Path secondParseRoot = Files.createDirectory(work.resolve("second-parse"));
        App second = parse(firstOutput, secondParseRoot, options);
        List<File> secondFiles = second.write(Files.createDirectory(work.resolve("second-output")).toFile());
        Path secondOutput = matchingSource(secondFiles, source.getFileName().toString());

        String firstCode = Files.readString(firstOutput);
        assertEquals(firstCode, Files.readString(secondOutput));
        assertEquals(1, count(firstCode, INSIDE_COMMENT));
        assertEquals(1, count(firstCode, BLOCK_COMMENT));
        assertEquals(1, count(firstCode, OUTSIDE_COMMENT));
    }

    private App parse(Path source, Path parseRoot, List<String> options) {
        CodeParser parser = CodeParser.newInstance();
        parser.set(CodeParser.GENERATED_PARSE_ROOT, parseRoot.toFile());
        parser.set(CodeParser.AST_DUMP_CACHE, false);
        parser.set(CodeParser.SHOW_EXEC_INFO, false);
        parser.set(ParallelCodeParser.PARALLEL_PARSING, false);
        return parser.parse(List.of(source.toFile()), options);
    }

    private Path matchingSource(List<File> files, String fileName) {
        return files.stream().map(File::toPath)
                .filter(path -> path.getFileName().toString().equals(fileName))
                .findFirst().orElseThrow();
    }

    private int count(String text, String substring) {
        int matches = 0;
        for (int offset = 0; (offset = text.indexOf(substring, offset)) >= 0; offset += substring.length()) {
            matches++;
        }
        return matches;
    }
}
