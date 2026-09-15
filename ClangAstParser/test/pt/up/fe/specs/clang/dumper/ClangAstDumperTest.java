/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.dumper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.LibcMode;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.Standard;
import org.suikasoft.jOptions.Interfaces.DataStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Verifies that syntax validation uses the protocol-free native mode. */
class ClangAstDumperTest {

    @TempDir
    Path tempFolder;

    @Test
    void validSyntaxDoesNotCreateAProtobufDump() throws Exception {
        File tool = nativeTool();
        assumeTrue(tool.isFile(), "clang-dumper build/tool is required for this integration test");
        Path source = Files.writeString(tempFolder.resolve("valid.cpp"), "int main() { return 0; }\n");

        var dumper = newDumper(tool);
        assertNull(dumper.validateSyntax(source.toFile(), "42", Standard.CXX17, config()));

        File workingFolder = dumper.getLastWorkingFolder();
        assertNotNull(workingFolder);
        assertFalse(new File(workingFolder, "clangDump.pb").exists());
        assertFalse(new File(workingFolder, "clangDump.pb.zst").exists());
    }

    @Test
    void invalidSyntaxReturnsStderrDiagnosticsWithoutProtocolBytes() throws Exception {
        File tool = nativeTool();
        assumeTrue(tool.isFile(), "clang-dumper build/tool is required for this integration test");
        Path source = Files.writeString(tempFolder.resolve("invalid.cpp"), "int main( {\n");

        var dumper = newDumper(tool);
        String error = dumper.validateSyntax(source.toFile(), "42", Standard.CXX17, config());

        assertNotNull(error);
        assertTrue(error.contains("Syntax validation failed"));
        assertTrue(error.contains("error:"));
        assertFalse(error.contains("CLAVAPB1"));
    }

    @Test
    void cxxMethodPreservesRecordReferenceAndLegacyRecordId() throws Exception {
        File tool = nativeTool();
        assumeTrue(tool.isFile(), "clang-dumper build/tool is required for this integration test");
        Path source = Files.writeString(tempFolder.resolve("record.cpp"),
                "struct Record { int declaration(); int definition() { return 0; } };\n");

        var data = newDumper(tool).parse(source.toFile(), "42", Standard.CXX17, config());
        var methods = data.get(ClangAstData.CLAVA_NODES).getNodes().values().stream()
                .filter(CXXMethodDecl.class::isInstance)
                .map(CXXMethodDecl.class::cast)
                .toList();

        assertFalse(methods.isEmpty());
        for (var method : methods) {
            var record = method.getRecordDecl().orElseThrow();
            assertEquals(record.getId(), method.get(CXXMethodDecl.RECORD_ID));
        }
    }

    private static ClangAstDumper newDumper(File tool) {
        return new ClangAstDumper(false, tool, List.of(), null, new ParallelCodeParser());
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
