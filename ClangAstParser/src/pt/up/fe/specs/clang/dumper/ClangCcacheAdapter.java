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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import pt.up.fe.specs.util.SpecsLogs;

/** Configures ccache to invoke clang-dumper directly in depend mode. */
final class ClangCcacheAdapter {

    private static final String CACHE_FOLDER_NAME = "clang-dumper-ccache";
    private static final AtomicBoolean MISSING_CCACHE_REPORTED = new AtomicBoolean();
    private static final Map<Path, CachedDigest> EXECUTABLE_DIGESTS = new ConcurrentHashMap<>();

    private ClangCcacheAdapter() {
    }

    static boolean isDisabled(String value) {
        if (value == null) {
            return false;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "0", "false", "no" -> throw new IllegalArgumentException(
                    "CCACHE_DISABLE does not accept false values; unset it to enable ccache");
            default -> true;
        };
    }

    static boolean isAvailable() {
        if (isDisabled(System.getenv("CCACHE_DISABLE"))) {
            return false;
        }

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

    static Invocation prepare(File dumperFolder, File baseDir, File executable, String schemaHash) {
        if (!schemaHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid FlatBuffers schema hash: " + schemaHash);
        }

        String executableHash = sha256(executable);
        var cacheFolder = new File(new File(dumperFolder, CACHE_FOLDER_NAME),
                "flatbuffers-v" + pt.up.fe.specs.clang.wire.WireProtocol.SCHEMA_VERSION + "-"
                        + pt.up.fe.specs.clang.wire.WireProtocol.FLATBUFFERS_VERSION + "-"
                        + pt.up.fe.specs.clang.wire.WireProtocol.FLATBUFFERS_COMMIT + "-"
                        + schemaHash + "-" + executableHash);
        try {
            Files.createDirectories(cacheFolder.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Could not prepare clang-dumper ccache folder '" + cacheFolder + "'", e);
        }

        return new Invocation(cacheFolder, baseDir);
    }

    static String sha256(File file) {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("Cannot hash missing clang-dumper executable: " + file);
        }
        try {
            Path path = file.toPath().toRealPath();
            BasicFileAttributes before = Files.readAttributes(path, BasicFileAttributes.class);
            CachedDigest cached = EXECUTABLE_DIGESTS.get(path);
            if (cached != null && cached.matches(before)) {
                return cached.sha256();
            }

            var digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                var buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            BasicFileAttributes after = Files.readAttributes(path, BasicFileAttributes.class);
            if (!sameFileVersion(before, after)) {
                throw new IOException("clang-dumper executable changed while hashing: " + path);
            }
            String hash = HexFormat.of().formatHex(digest.digest());
            EXECUTABLE_DIGESTS.put(path, new CachedDigest(after.size(), after.lastModifiedTime(), after.fileKey(), hash));
            return hash;
        } catch (IOException e) {
            throw new RuntimeException("Could not hash clang-dumper executable '" + file + "'", e);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is required by the JRE", e);
        }
    }

    private static boolean sameFileVersion(BasicFileAttributes left, BasicFileAttributes right) {
        return left.size() == right.size() && left.lastModifiedTime().equals(right.lastModifiedTime())
                && java.util.Objects.equals(left.fileKey(), right.fileKey());
    }

    private record CachedDigest(long size, FileTime modifiedTime, Object fileKey, String sha256) {
        private boolean matches(BasicFileAttributes attributes) {
            return size == attributes.size() && modifiedTime.equals(attributes.lastModifiedTime())
                    && java.util.Objects.equals(fileKey, attributes.fileKey());
        }
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

    record Invocation(File cacheFolder, File baseDir) {

        Invocation(File cacheFolder) {
            this(cacheFolder, null);
        }

        void configureEnvironment(Map<String, String> environment) {
            environment.put("CCACHE_DIR", cacheFolder.getAbsolutePath());
            environment.put("CCACHE_COMPILERTYPE", "clang");
            environment.put("CCACHE_DEPEND", "true");
            environment.put("CCACHE_NOHASHDIR", "true");
            if (baseDir != null) {
                environment.put("CCACHE_BASEDIR", baseDir.getAbsolutePath());
            }
            environment.put("CCACHE_COMPRESS", "true");
            environment.remove("CCACHE_NOCOMPRESS");
        }
    }
}
