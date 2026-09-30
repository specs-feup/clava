/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package pt.up.fe.specs.clang.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        URL selectedResource = AClangAstTester.class.getClassLoader().getResource("cxx/boolean.cpp");
        assertNotNull(selectedResource);
        Path selectedPath = Paths.get(selectedResource.toURI());
        byte[] originalContents = Files.readAllBytes(selectedPath);

        CxxTester tester = new CxxTester("boolean.cpp");
        File cacheFolder = Files.createDirectory(tempFolder.resolve("cache")).toFile();
        tester.set(CodeParser.DUMPER_FOLDER, cacheFolder);
        tester.set(CodeParser.SHOW_EXEC_INFO, false);
        tester.set(ParallelCodeParser.PARALLEL_PARSING, false);

        File outputFolder = null;
        try {
            tester.setUp();
            outputFolder = tester.getOutputFolder();

            assertEquals(Set.of(selectedPath.toFile()), Set.copyOf(tester.getInputFiles()));
            assertEquals(selectedPath.getParent().toFile(), tester.getInputRoot());
            assertFalse(tester.getInputRoot().toPath().startsWith(outputFolder.toPath()));
            assertFalse(outputFolder.toPath().startsWith(tester.getInputRoot().toPath()));

            tester.testProper();

            Set<String> firstOutputNames;
            try (Stream<Path> outputFiles = Files.walk(outputFolder.toPath().resolve("outputFirst"))) {
                firstOutputNames = outputFiles
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toSet());
            }

            assertEquals(Set.of("boolean.cpp"), firstOutputNames);
            assertFalse(firstOutputNames.contains("constructor.cpp"));
            assertArrayEquals(originalContents, Files.readAllBytes(selectedPath));
        } finally {
            tester.cleanupInstance();
        }

        if (!SpecsSystem.isDebug()) {
            assertFalse(outputFolder.exists());
        }
    }
}
