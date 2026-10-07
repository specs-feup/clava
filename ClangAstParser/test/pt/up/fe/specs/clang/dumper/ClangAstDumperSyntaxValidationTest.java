/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.dumper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClangAstDumperSyntaxValidationTest {

    @Test
    void syntaxCheckOptionAppearsOnceBeforeCompilerArguments() {
        var arguments = List.of(
                "/clang-dumper", "-c", "source.cpp", "--", "-std=c++17");

        assertEquals(List.of(
                "/clang-dumper", "-c", "source.cpp", "-syntax-check-only", "--", "-std=c++17"),
                ClangAstDumper.getSyntaxArguments(arguments));
    }
}
