/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.util.SpecsSystem;

class AClangAstTesterResourceTest {

    @TempDir
    Path tempFolder;

    @Test
    void parsesOnlySelectedFilesystemResourcesAndKeepsRoundTripOutputSeparate() throws Exception {
        Path selectedPath = TestResourceResolver.resolve("cxx/boolean.cpp").toPath();
        byte[] originalContents = Files.readAllBytes(selectedPath);

        CxxTester tester = new CxxTester("boolean.cpp");
        File cacheFolder = Files.createDirectory(tempFolder.resolve("cache")).toFile();
        tester.set(CodeParser.DUMPER_FOLDER, cacheFolder);
        tester.set(CodeParser.SHOW_EXEC_INFO, false);
        tester.set(ParallelCodeParser.PARALLEL_PARSING, false);

        String previousMetricsProperty = System.getProperty("clava.astWireMetrics");
        try {
            System.setProperty("clava.astWireMetrics", "true");
            tester.test();
        } finally {
            if (previousMetricsProperty == null) {
                System.clearProperty("clava.astWireMetrics");
            } else {
                System.setProperty("clava.astWireMetrics", previousMetricsProperty);
            }
        }

        File outputFolder = tester.getOutputFolder();
        assertEquals(Set.of(selectedPath.toFile()), Set.copyOf(tester.getInputFiles()));
        assertEquals(selectedPath.getParent().toFile(), tester.getInputRoot());
        assertFalse(tester.getInputRoot().toPath().startsWith(outputFolder.toPath()));
        assertFalse(outputFolder.toPath().startsWith(tester.getInputRoot().toPath()));
        assertArrayEquals(originalContents, Files.readAllBytes(selectedPath));
        if (!SpecsSystem.isDebug()) {
            assertFalse(outputFolder.exists());
        }

        IllegalArgumentException missingResource = assertThrows(IllegalArgumentException.class,
                () -> TestResourceResolver.resolve("parser-resource-that-does-not-exist.cpp"));
        assertTrue(missingResource.getMessage().contains("Could not find test resource"));

        URL packagedResource = new URL("jar:file:/parser-test-resources.jar!/cxx/boolean.cpp");
        IllegalArgumentException unsupportedResource = assertThrows(IllegalArgumentException.class,
                () -> TestResourceResolver.resolve("cxx/boolean.cpp", packagedResource));
        assertTrue(unsupportedResource.getMessage().contains("file URL"));

        CxxTester multiFileTester = new CxxTester(List.of("constructor.cpp", "constructor.h"));
        try {
            multiFileTester.setUp();

            List<File> selectedFiles = List.of(
                    TestResourceResolver.resolve("cxx/constructor.cpp"),
                    TestResourceResolver.resolve("cxx/constructor.h"));
            assertEquals(selectedFiles, multiFileTester.getInputFiles());
            assertEquals(selectedFiles.get(0).getParentFile(), multiFileTester.getInputRoot());
        } finally {
            multiFileTester.cleanupInstance();
        }
    }
}
