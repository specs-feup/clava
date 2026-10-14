/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.dumper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.LibcMode;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXConversionDecl;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.Standard;

class CXXConversionDeclTest {

    @TempDir
    Path tempFolder;

    @Test
    void preservesLegacySystemHeaderConversionOperatorName() throws Exception {
        File tool = nativeTool();
        assumeTrue(tool.isFile(), "clang-dumper build/tool is required for this integration test");

        Path source = tempFolder.resolve("comment.cpp");
        try (InputStream input = getClass().getResourceAsStream("/cxx/comment.cpp")) {
            assertTrue(input != null, "comment.cpp test resource is required");
            Files.copy(input, source);
        }

        ClangAstDumper dumper = new ClangAstDumper(false, tool, List.of(), null, new ParallelCodeParser())
                .setSystemIncludesThreshold(1);
        var data = dumper.parse(source.toFile(), "42", Standard.CXX17, config());
        var conversions = data.get(ClangAstData.CLAVA_NODES).getNodes().values().stream()
                .filter(CXXConversionDecl.class::isInstance)
                .map(CXXConversionDecl.class::cast)
                .filter(decl -> decl.get(ClavaNode.IS_IN_SYSTEM_HEADER))
                .toList();

        assertFalse(conversions.isEmpty(), "<string> must expose a system-header conversion declaration");
        var conversion = conversions.stream()
                .filter(decl -> decl.get(CXXConversionDecl.CONVERSION_TYPE).getCode().equals("__sv_type"))
                .findFirst()
                .orElseThrow();
        assertEquals("operator __sv_type", conversion.getDeclName());
    }

    private static DataStore config() {
        var config = ClangAstKeys.toDataStore(List.of());
        config.set(ClangAstKeys.USES_CILK, false);
        config.set(ClangAstKeys.LIBC_CXX_MODE, LibcMode.SYSTEM);
        config.add(ClavaNode.CONTEXT, new ClavaContext());
        return config;
    }

    private static File nativeTool() {
        String configured = System.getenv("CLANG_DUMPER_TOOL");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toFile();
        }

        Path workingDirectory = Path.of(System.getProperty("user.dir"));
        var candidates = new ArrayList<Path>();
        candidates.add(workingDirectory.resolve("../../clang-dumper/build/tool"));
        candidates.add(workingDirectory.resolve("../clang-dumper/build/tool"));
        candidates.add(workingDirectory.resolve("clang-dumper/build/tool"));
        return candidates.stream()
                .map(Path::toAbsolutePath)
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(candidates.get(0))
                .toFile();
    }
}
