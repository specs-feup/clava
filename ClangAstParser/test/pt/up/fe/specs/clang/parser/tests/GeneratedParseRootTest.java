/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.parser.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.lang.SpecsPlatforms;

class GeneratedParseRootTest {

    @TempDir
    Path tempFolder;

    @Test
    void syntaxValidationUsesTheGeneratedRootAndCleansWorkingFolders() throws Exception {
        Path root = createProject("syntax");
        Path source = root.resolve("src/foo.cpp");
        Files.writeString(source, "#include \"relative/header.h\"\nint value = header_value;\n");
        Set<Path> foldersBefore = syntaxWorkingFolders();
        CodeParser parser = CodeParser.newInstance();
        parser.set(CodeParser.GENERATED_PARSE_ROOT, root.toFile());
        parser.set(CodeParser.SHOW_EXEC_INFO, false);
        parser.set(ParallelCodeParser.PARALLEL_PARSING, false);
        parser.set(ParallelCodeParser.SYNTAX_ONLY, true);
        List<String> flags = List.of("-std=c++17", "-I" + root.resolve("include"));

        assertDoesNotThrow(() -> parser.parse(List.of(source.toFile()), flags));
        assertEquals(foldersBefore, syntaxWorkingFolders());
        Files.writeString(source, "int value = ;\n");
        assertThrows(RuntimeException.class, () -> parser.parse(List.of(source.toFile()), flags));
        assertEquals(foldersBefore, syntaxWorkingFolders());
    }

    private Set<Path> syntaxWorkingFolders() throws Exception {
        try (var paths = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return paths.filter(path -> path.getFileName().toString().startsWith("clava_ast_"))
                    .collect(Collectors.toSet());
        }
    }

    @Test
    void cachedGeneratedDumpIsResolvedAgainstTheCurrentRoot() throws Exception {
        Path rootA = createProject("A");
        Path rootB = createProject("B");
        Path externalFolder = Files.createDirectories(tempFolder.resolve("external"));
        Path externalHeader = Files.writeString(externalFolder.resolve("header.h"),
                "constexpr int external_value = 1;\n");
        File cacheFolder = Files.createDirectory(tempFolder.resolve("cache")).toFile();

        App first = parse(rootA, cacheFolder, externalFolder, externalHeader);
        App second = parse(rootB, cacheFolder, externalFolder, externalHeader);

        String firstRoot = rootA.toAbsolutePath().normalize().toString();
        String secondRoot = rootB.toAbsolutePath().normalize().toString();

        assertTrue(allFilepaths(first).stream().anyMatch(path -> path.startsWith(firstRoot)));
        assertTrue(allFilepaths(second).stream().anyMatch(path -> path.startsWith(secondRoot)));
        assertTrue(allFilepaths(second).stream().anyMatch(path -> path.startsWith(externalFolder.toString())));
        assertTrue(allIncludedFilepaths(second).stream().anyMatch(path -> path.startsWith(secondRoot)));
        assertFalse(allFilepaths(second).stream().anyMatch(path -> path.startsWith(firstRoot)));

        if (SpecsPlatforms.isLinux()) {
            Assumptions.assumeFalse(System.getenv().containsKey("CCACHE_DISABLE"),
                    "CCACHE_DISABLE is set, so this run deliberately bypasses cache hit counters");
            Assumptions.assumeTrue(hasCcache(), "ccache is required for the cache counter assertion");
            String stats = ccacheStats(cacheFolder);
            assertEquals(1L, statsFor("Hits", stats), stats);
            assertEquals(1L, statsFor("Misses", stats), stats);
        }
    }

    private Path createProject(String name) throws Exception {
        Path root = Files.createDirectory(tempFolder.resolve(name));
        Files.createDirectories(root.resolve("src"));
        Files.createDirectories(root.resolve("include/relative"));
        Files.writeString(root.resolve("src/foo.cpp"),
                "#include \"relative/header.h\"\n#include \"header.h\"\n"
                        + "int value = header_value + external_value;\n");
        Files.writeString(root.resolve("include/relative/header.h"), "constexpr int header_value = 41;\n");
        return root;
    }

    private App parse(Path root, File cacheFolder, Path externalFolder, Path externalHeader) {
        CodeParser parser = CodeParser.newInstance();
        parser.set(CodeParser.GENERATED_PARSE_ROOT, root.toFile());
        parser.set(CodeParser.DUMPER_FOLDER, cacheFolder);
        parser.set(CodeParser.AST_DUMP_CACHE, true);
        parser.set(CodeParser.SHOW_EXEC_INFO, false);
        parser.set(ParallelCodeParser.PARALLEL_PARSING, false);
        return parser.parse(List.of(root.resolve("src/foo.cpp").toFile(), externalHeader.toFile()),
                List.of("-std=c++17", "-I" + root, "-I" + root.resolve("include"), "-I" + externalFolder));
    }

    private List<String> allFilepaths(App app) {
        return app.getDescendantsAndSelfStream()
                .flatMap(node -> node.getLocationTry().stream())
                .map(SourceRange::getFilepath)
                .filter(filepath -> filepath != null)
                .toList();
    }

    private List<String> allIncludedFilepaths(App app) {
        return app.getTranslationUnits().stream()
                .flatMap(translationUnit -> translationUnit.getIncludes().getIncludes().stream())
                .map(includeDecl -> includeDecl.getInclude().getSourceFile().getAbsolutePath())
                .toList();
    }

    private boolean hasCcache() {
        try {
            Process process = new ProcessBuilder("ccache", "--version")
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String ccacheStats(File cacheFolder) throws Exception {
        Path ccacheRoot = cacheFolder.toPath().resolve("clang-dumper-ccache");
        List<Path> versionedCaches;
        try (var children = Files.list(ccacheRoot)) {
            versionedCaches = children
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("flatbuffers-v"))
                    .toList();
        }
        assertEquals(1, versionedCaches.size(), "Expected one schema/executable-specific cache namespace");

        ProcessBuilder processBuilder = new ProcessBuilder("ccache", "--show-stats");
        processBuilder.environment().put("CCACHE_DIR", versionedCaches.get(0).toAbsolutePath().toString());
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor() == 0, output);
        return output;
    }

    private long statsFor(String name, String stats) {
        Matcher matcher = Pattern.compile("^\\s*" + name + ":\\s+(\\d+)\\b", Pattern.MULTILINE).matcher(stats);
        assertTrue(matcher.find(), "Could not find '" + name + "' in ccache stats:\n" + stats);
        return Long.parseLong(matcher.group(1));
    }
}
