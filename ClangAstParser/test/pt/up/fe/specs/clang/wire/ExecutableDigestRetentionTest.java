/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.ExecutableDigest;

class ExecutableDigestRetentionTest {
    @TempDir
    Path directory;

    @Test
    void concurrentDistinctResourceFoldersKeepBoundedMetadataAndCorrectDigests() throws Exception {
        var calls = new ArrayList<Callable<Boolean>>();
        Path first = null;
        for (int index = 0; index < 256; index++) {
            byte[] bytes = ("executable-" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Path executable = directory.resolve("tool-" + index);
            Files.write(executable, bytes);
            if (first == null) {
                first = executable;
            }
            String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            calls.add(() -> expected.equals(ExecutableDigest.sha256(executable.toFile())));
        }

        var workers = Executors.newFixedThreadPool(8);
        try {
            for (var result : workers.invokeAll(calls)) {
                assertTrue(result.get());
            }
        } finally {
            workers.shutdownNow();
        }

        var cacheField = ExecutableDigest.class.getDeclaredField("DIGESTS");
        cacheField.setAccessible(true);
        var cache = (Map<?, ?>) cacheField.get(null);
        synchronized (cache) {
            assertTrue(cache.size() <= 128, "temporary executable paths must not accumulate indefinitely");
        }
        // A previously visited path remains correct whether cached or evicted.
        byte[] replacement = "changed executable with a different size".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(first, replacement);
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(replacement)),
                ExecutableDigest.sha256(first.toFile()));
    }
}
