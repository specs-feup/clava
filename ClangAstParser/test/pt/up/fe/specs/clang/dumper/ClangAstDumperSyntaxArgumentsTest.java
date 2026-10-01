/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.dumper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClangAstDumperSyntaxArgumentsTest {

    @Test
    public void addsSyntaxCheckOptionBeforeCompilerArgumentsWithoutChangingInput() {
        var arguments = List.of("/tool", "-c", "/source.cpp", "--", "-std=c++17");

        assertEquals(List.of(
                "/tool", "-c", "/source.cpp", "-syntax-check-only", "--", "-std=c++17"),
                ClangAstDumper.withSyntaxCheckOnlyArgument(arguments));
        assertEquals(List.of("/tool", "-c", "/source.cpp", "--", "-std=c++17"), arguments);
    }

    @Test
    public void appendsSyntaxCheckOptionWhenThereIsNoCompilerArgumentSeparator() {
        var arguments = List.of("/tool", "-c", "/source.cpp", "-std=c++17");

        assertEquals(List.of(
                "/tool", "-c", "/source.cpp", "-std=c++17", "-syntax-check-only"),
                ClangAstDumper.withSyntaxCheckOnlyArgument(arguments));
    }
}
