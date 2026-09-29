/*
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.dumper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ClangAstDumperArgumentsTest {

    @Test
    void normalizesOnlyTheExactJavaTempRootAndKeepsArgumentOrder() {
        String previousTemp = System.getProperty("java.io.tmpdir");
        try {
            List<String> textArguments = List.of(
                    "-isystem", "/runs/text/include", "-I/runs/text/include2",
                    "-I/runs/textual/include", "-c",
                    "/runs/text/generated.c", "-id=4");
            List<String> protobufArguments = List.of(
                    "-isystem", "/runs/protobuf/include", "-I/runs/protobuf/include2",
                    "-I/runs/textual/include", "-c",
                    "/runs/protobuf/generated.c", "-id=9");

            System.setProperty("java.io.tmpdir", "/runs/text");
            List<String> normalizedText = ClangAstDumper.stableParseArguments(
                    textArguments, "/runs/text/generated.c", true);
            List<String> rawText = ClangAstDumper.stableParseArguments(
                    textArguments, "/runs/text/generated.c", false);
            System.setProperty("java.io.tmpdir", "/runs/protobuf");
            List<String> normalizedProtobuf = ClangAstDumper.stableParseArguments(
                    protobufArguments, "/runs/protobuf/generated.c", true);
            List<String> rawProtobuf = ClangAstDumper.stableParseArguments(
                    protobufArguments, "/runs/protobuf/generated.c", false);

            assertEquals(List.of("-isystem", "<java.io.tmpdir>/include", "-I<java.io.tmpdir>/include2",
                    "-I/runs/textual/include", "-c", "<source>", "-id=<id>"), normalizedText);
            assertEquals(normalizedText, normalizedProtobuf);
            assertEquals(ClangAstDumper.parseArgumentsSha256(normalizedText),
                    ClangAstDumper.parseArgumentsSha256(normalizedProtobuf));
            assertNotEquals(ClangAstDumper.parseArgumentsSha256(rawText),
                    ClangAstDumper.parseArgumentsSha256(rawProtobuf));
        } finally {
            if (previousTemp == null) {
                System.clearProperty("java.io.tmpdir");
            } else {
                System.setProperty("java.io.tmpdir", previousTemp);
            }
        }
    }
}
