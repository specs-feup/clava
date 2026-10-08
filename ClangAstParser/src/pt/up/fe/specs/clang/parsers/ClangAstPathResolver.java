/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.parsers;

import java.io.File;
import java.nio.file.Path;

import pt.up.fe.specs.clang.dumper.ClangAstData;

/** Resolves paths in a dump without changing ordinary parser behaviour. */
public final class ClangAstPathResolver {

    private ClangAstPathResolver() {
    }

    /**
     * Resolves a path emitted by clang-dumper. Relative paths are rooted only
     * when the dump was made for generated sources. Clang's pseudo paths are
     * intentionally left untouched.
     */
    public static String resolve(String path, ClangAstData data) {
        if (path == null || data == null || !data.hasValue(ClangAstData.PARSE_ROOT)) {
            return path;
        }

        if (isPseudoPath(path) || new File(path).isAbsolute()) {
            return path;
        }

        File parseRoot = data.get(ClangAstData.PARSE_ROOT);
        if (parseRoot == null) {
            return path;
        }

        Path root = parseRoot.toPath();
        return root.resolve(path).normalize().toString();
    }

    public static File resolveFile(String path, ClangAstData data) {
        return new File(resolve(path, data));
    }

    private static boolean isPseudoPath(String path) {
        return path.startsWith("<") && path.endsWith(">");
    }
}
