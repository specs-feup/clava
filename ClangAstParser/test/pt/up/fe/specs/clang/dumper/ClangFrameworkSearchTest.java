/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.dumper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ClangFrameworkSearchTest {

    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(30);

    @TempDir
    Path tempFolder;

    @Test
    void bundledFrameworkAndOtherIncludeRootsKeepTheirOwnFlags() throws Exception {
        var fixture = createFixture();

        assertEquals(List.of(
                "-iframework", fixture.frameworkRoot().toString(),
                "-isystem", fixture.bundledIncludeRoot().toString(),
                "-isystem", fixture.customSystemRoot().toString()),
                ClangAstDumper.buildIncludeArguments(
                        List.of(fixture.frameworkRoot().toString(), fixture.bundledIncludeRoot().toString()),
                        List.of(fixture.customSystemRoot().toString())));
    }

    @Test
    void automaticClangSearchSkipsWrongVersionsAndKeepsExplicitOverride() throws Exception {
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        String executableName = windows ? "clang.exe" : "clang";
        var firstPath = Files.createDirectories(tempFolder.resolve("path-first"));
        var laterPath = Files.createDirectories(tempFolder.resolve("path-later"));
        var wrongVersion = Files.writeString(firstPath.resolve(executableName), "clang version 17.0.0");
        var clang18 = Files.writeString(laterPath.resolve(executableName), "clang version 18.1.0");
        String pathValue = firstPath + File.pathSeparator + laterPath;
        Predicate<Path> versionCheck = candidate -> {
            try {
                return isClang18Version(Files.readString(candidate));
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        };

        assertEquals(clang18, findClang18(null, pathValue, windows, versionCheck));
        assertNull(findClang18(null, firstPath.toString(), windows, versionCheck));
        assertEquals(wrongVersion,
                findClang18(wrongVersion.toString(), firstPath.toString(), windows, ignored -> false));
    }

    @Test
    void clang18FindsHeadersThroughGeneratedFrameworkArguments() throws Exception {
        var clang = findClang18();
        assumeTrue(clang != null, "Set CLANG_18 or install clang-18 to run the framework lookup smoke test");
        assertTrue(isClang18(clang), "CLANG_18 must identify Clang 18: " + clang);

        var fixture = createFixture();
        var arguments = new ArrayList<String>();
        arguments.add(clang.toString());
        arguments.addAll(List.of("-fsyntax-only", "-x", "c", "-std=c11"));
        arguments.addAll(ClangAstDumper.buildIncludeArguments(
                List.of(fixture.frameworkRoot().toString(), fixture.bundledIncludeRoot().toString()),
                List.of(fixture.customSystemRoot().toString())));
        arguments.add("-I");
        arguments.add(fixture.ordinaryIncludeRoot().toString());
        arguments.add(fixture.source().toString());

        var process = new ProcessBuilder(arguments).redirectErrorStream(true).start();
        if (!process.waitFor(PROCESS_TIMEOUT.toSeconds(), java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Clang 18 syntax check timed out");
        }

        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
    }

    private Fixture createFixture() throws Exception {
        var frameworkRoot = tempFolder.resolve("Frameworks");
        var frameworkHeaders = frameworkRoot.resolve("Foundation.framework/Headers");
        var bundledIncludeRoot = tempFolder.resolve("bundled-includes");
        var customSystemRoot = tempFolder.resolve("custom-system-includes");
        var ordinaryIncludeRoot = tempFolder.resolve("ordinary-includes");

        Files.createDirectories(frameworkHeaders);
        Files.createDirectories(bundledIncludeRoot);
        Files.createDirectories(customSystemRoot);
        Files.createDirectories(ordinaryIncludeRoot);
        Files.writeString(frameworkHeaders.resolve("tinyFoundation.h"), "#define FOUNDATION_VALUE 1\n");
        Files.writeString(bundledIncludeRoot.resolve("bundled.h"), "#define BUNDLED_VALUE 2\n");
        Files.writeString(customSystemRoot.resolve("custom.h"), "#define CUSTOM_VALUE 3\n");
        Files.writeString(ordinaryIncludeRoot.resolve("ordinary.h"), "#define ORDINARY_VALUE 4\n");
        var source = Files.writeString(tempFolder.resolve("framework-lookup.c"), """
                #include <Foundation/tinyFoundation.h>
                #include <bundled.h>
                #include <custom.h>
                #include <ordinary.h>
                int values = FOUNDATION_VALUE + BUNDLED_VALUE + CUSTOM_VALUE + ORDINARY_VALUE;
                """);

        return new Fixture(frameworkRoot, bundledIncludeRoot, customSystemRoot, ordinaryIncludeRoot, source);
    }

    private static Path findClang18() {
        String configured = System.getenv("CLANG_18");
        String path = System.getenv("PATH");
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        return findClang18(configured, path, windows, ClangFrameworkSearchTest::isClang18);
    }

    static Path findClang18(String configured, String path, boolean windows, Predicate<Path> isClang18) {
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }

        var candidates = new ArrayList<Path>();
        List<String> exactNames = windows
                ? List.of("clang-18.exe", "clang18.exe")
                : List.of("clang-18", "clang18");
        if (path != null) {
            var directories = List.of(path.split(java.util.regex.Pattern.quote(File.pathSeparator)));
            for (String name : exactNames) {
                for (String directory : directories) {
                    if (!directory.isBlank()) {
                        candidates.add(Path.of(directory).resolve(name));
                    }
                }
            }
            String genericName = windows ? "clang.exe" : "clang";
            for (String directory : directories) {
                if (!directory.isBlank()) {
                    candidates.add(Path.of(directory).resolve(genericName));
                }
            }
        }

        return candidates.stream()
                .filter(Files::isRegularFile)
                .filter(isClang18)
                .findFirst()
                .orElse(null);
    }

    private static boolean isClang18(Path clang) {
        try {
            var process = new ProcessBuilder(clang.toString(), "--version").redirectErrorStream(true).start();
            if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }

            var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return process.exitValue() == 0 && isClang18Version(output);
        } catch (java.io.IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean isClang18Version(String output) {
        return output.matches("(?s).*\\bversion 18(?:\\.|\\b).*");
    }

    private record Fixture(Path frameworkRoot, Path bundledIncludeRoot, Path customSystemRoot,
                           Path ordinaryIncludeRoot, Path source) {
    }
}
