/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.dumper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import pt.up.fe.specs.util.SpecsLogs;

/** Configures ccache to invoke clang-dumper directly in depend mode. */
final class ClangCcacheAdapter {

    private static final String CACHE_FOLDER_NAME = "clang-dumper-ccache";
    private static final AtomicBoolean MISSING_CCACHE_REPORTED = new AtomicBoolean();

    private ClangCcacheAdapter() {
    }

    static boolean isAvailable() {
        var path = System.getenv("PATH");
        if (path != null) {
            for (var folder : path.split(File.pathSeparator)) {
                if (Files.isExecutable(Path.of(folder.isEmpty() ? "." : folder, "ccache"))) {
                    return true;
                }
            }
        }

        if (MISSING_CCACHE_REPORTED.compareAndSet(false, true)) {
            SpecsLogs.warn("ccache is not available on PATH; AST dump caching is disabled");
        }
        return false;
    }

    static Invocation prepare(File dumperFolder) {
        var cacheFolder = new File(dumperFolder, CACHE_FOLDER_NAME);
        try {
            Files.createDirectories(cacheFolder.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Could not prepare clang-dumper ccache folder '" + cacheFolder + "'", e);
        }

        return new Invocation(cacheFolder);
    }

    static List<String> command(List<String> dumperCommand, File dependencyFile) {
        var separatorIndex = dumperCommand.indexOf("--");
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Expected clang-dumper command to contain '--': " + dumperCommand);
        }

        var command = new ArrayList<String>();
        command.add("ccache");
        command.addAll(dumperCommand.subList(0, separatorIndex));
        command.add("-MD");
        command.add("-MF");
        command.add(dependencyFile.getAbsolutePath());
        command.addAll(dumperCommand.subList(separatorIndex, dumperCommand.size()));
        return command;
    }

    record Invocation(File cacheFolder) {

        void configureEnvironment(Map<String, String> environment) {
            environment.put("CCACHE_DIR", cacheFolder.getAbsolutePath());
            environment.put("CCACHE_COMPILERTYPE", "clang");
            environment.put("CCACHE_DEPEND", "true");
            environment.put("CCACHE_NOHASHDIR", "true");
            // clang-dumper already streams a compressed Zstandard frame. Recompressing
            // it inside ccache roughly doubles miss latency without changing semantics.
            environment.put("CCACHE_NOCOMPRESS", "true");
        }
    }
}
