/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.codeparser;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.dumper.ClangAstDumper;

class ParallelCodeParserCleanupTest {

    @TempDir
    Path tempFolder;

    @Test
    void syntaxOnlySuccessCleansItsWorkingFolder() throws IOException {
        Path workingFolder = Files.createDirectory(tempFolder.resolve("syntax-only"));

        assertNull(ParallelCodeParser.runWithCleanup(new TestDumper(workingFolder), true, () -> null));

        assertFalse(Files.exists(workingFolder));
    }

    @Test
    void rejectedParseCleansItsWorkingFolder() throws IOException {
        Path workingFolder = Files.createDirectory(tempFolder.resolve("rejected"));

        assertThrows(IllegalStateException.class, () -> ParallelCodeParser.runWithCleanup(
                new TestDumper(workingFolder), true, () -> {
                    throw new IllegalStateException("parse rejected");
                }));

        assertFalse(Files.exists(workingFolder));
    }

    @Test
    void repeatedParsesCleanEachWorkingFolder() throws IOException {
        Path first = Files.createDirectory(tempFolder.resolve("first"));
        Path second = Files.createDirectory(tempFolder.resolve("second"));
        TestDumper dumper = new TestDumper(first);

        assertNull(ParallelCodeParser.runWithCleanup(dumper, true, () -> null));
        dumper.setWorkingFolder(second);
        assertNull(ParallelCodeParser.runWithCleanup(dumper, true, () -> null));

        assertFalse(Files.exists(first));
        assertFalse(Files.exists(second));
    }

    private static final class TestDumper extends ClangAstDumper {
        private File currentFolder;

        private TestDumper(Path folder) {
            super(false, null, List.of(), null, new ParallelCodeParser());
            this.currentFolder = folder.toFile();
        }

        @Override
        public File getLastWorkingFolder() {
            return currentFolder;
        }

        private void setWorkingFolder(Path folder) {
            currentFolder = folder.toFile();
        }
    }
}
