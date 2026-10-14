/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.codeparser;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.dumper.ClangAstDumper;
import pt.up.fe.specs.clang.wire.ProtobufAstParseException;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.util.SpecsSystem;

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

    @Test
    void rejectedWireParsingIsFatalAndCleansParallelRepeatedParses() throws IOException {
        SpecsSystem.programStandardInit();
        Path rejected = Files.writeString(tempFolder.resolve("unsupported_friend.cpp"),
                "template<class T> struct Owner;\n"
                        + "class Target { template<class T> friend void Owner<T>::get(); };\n"
                        + "template<class T> struct Owner { void get() {} };\n");
        Path accepted = Files.writeString(tempFolder.resolve("accepted.cpp"), "int accepted() { return 1; }\n");
        Set<Path> initialFolders = parserFolders();

        for (int repeat = 0; repeat < 3; repeat++) {
            var parser = CodeParser.newInstance();
            parser.set(CodeParser.AST_DUMP_CACHE, false);
            parser.set(CodeParser.SHOW_EXEC_INFO, false);
            parser.set(ParallelCodeParser.PARSING_NUM_THREADS, 2);
            // Compiler-error tolerance must never turn invalid wire data into
            // a successfully returned App with a missing translation unit.
            parser.set(ParallelCodeParser.CONTINUE_ON_PARSING_ERRORS, true);
            assertThrows(ProtobufAstParseException.class,
                    () -> parser.parse(List.of(rejected.toFile(), accepted.toFile()), List.of("-std=c++17")));
            assertEquals(initialFolders, parserFolders());
        }
    }

    @Test
    void compilerErrorsPreserveDiagnosticsWithAndWithoutContinue() throws IOException {
        SpecsSystem.programStandardInit();
        Path source = Files.writeString(tempFolder.resolve("continue_on_error.cpp"),
                "int continue_after_error() { return missing_protobuf_error_symbol; }\n");
        Set<Path> initialFolders = parserFolders();

        var strictParser = parser(false);
        ClavaParserException exception = assertThrows(ClavaParserException.class,
                () -> strictParser.parse(List.of(source.toFile()), List.of("-std=c++17")));
        assertTrue(exception.getErrors().stream()
                .anyMatch(error -> error.contains("missing_protobuf_error_symbol")));
        assertEquals(initialFolders, parserFolders());

        App app = parser(true).parse(List.of(source.toFile()), List.of("-std=c++17"));

        assertEquals(1, app.getTranslationUnits().size());
        TranslationUnit translationUnit = app.getTranslationUnits().get(0);
        assertTrue(translationUnit.get(TranslationUnit.HAS_PARSING_ERRORS));
        assertTrue(translationUnit.get(TranslationUnit.ERROR_OUTPUT)
                .contains("missing_protobuf_error_symbol"));
        assertEquals(initialFolders, parserFolders());
    }

    private static CodeParser parser(boolean continueOnParsingErrors) {
        var parser = CodeParser.newInstance();
        parser.set(CodeParser.AST_DUMP_CACHE, false);
        parser.set(CodeParser.SHOW_EXEC_INFO, false);
        parser.set(ParallelCodeParser.PARSING_NUM_THREADS, 1);
        parser.set(ParallelCodeParser.CONTINUE_ON_PARSING_ERRORS, continueOnParsingErrors);
        return parser;
    }

    private static Set<Path> parserFolders() throws IOException {
        try (var paths = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return paths.filter(path -> path.getFileName().toString().startsWith("clava_ast_"))
                    .collect(Collectors.toSet());
        }
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
