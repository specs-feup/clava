/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.dumper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClangCcacheAdapterTest {

    @Test
    public void anyPresentCcacheDisableValueDisablesCaching() {
        assertTrue(ClangCcacheAdapter.isDisabled("1"));
        assertTrue(ClangCcacheAdapter.isDisabled(" true "));
        assertTrue(ClangCcacheAdapter.isDisabled("YES"));
        assertTrue(ClangCcacheAdapter.isDisabled("on"));
        assertTrue(ClangCcacheAdapter.isDisabled(""));
        assertTrue(ClangCcacheAdapter.isDisabled("arbitrary"));
        assertFalse(ClangCcacheAdapter.isDisabled(null));
    }

    @Test
    public void falseCcacheDisableValuesFailInsteadOfEnablingCaching() {
        for (String value : List.of("0", "false", "no", " FALSE ")) {
            assertThrows(IllegalArgumentException.class, () -> ClangCcacheAdapter.isDisabled(value));
        }
    }

    @Test
    public void schemaVersionsHaveSeparateCaches(@TempDir Path folder) throws Exception {
        var executable = folder.resolve("tool");
        Files.writeString(executable, "producer revision one");
        var first = ClangCcacheAdapter.prepare(folder.toFile(), null, executable.toFile(), "a".repeat(64));
        var second = ClangCcacheAdapter.prepare(folder.toFile(), null, executable.toFile(), "b".repeat(64));
        assertNotEquals(first.cacheFolder(), second.cacheFolder());
        assertTrue(Files.isDirectory(first.cacheFolder().toPath()));
        assertTrue(Files.isDirectory(second.cacheFolder().toPath()));
        assertTrue(first.cacheFolder().getName().startsWith("flatbuffers-"));
        Files.writeString(first.cacheFolder().toPath().resolve("cached.dump"), "old schema");
        assertFalse(Files.exists(second.cacheFolder().toPath().resolve("cached.dump")));
        Files.writeString(executable, "producer revision two with different bytes");
        var rebuilt = ClangCcacheAdapter.prepare(folder.toFile(), null, executable.toFile(), "a".repeat(64));
        assertNotEquals(first.cacheFolder(), rebuilt.cacheFolder());
    }

    @Test
    public void invalidSchemaCannotChooseACacheDirectory(@TempDir Path folder) {
        for (String hash : List.of("../text", "", "A".repeat(64), "a".repeat(63))) {
            assertThrows(IllegalArgumentException.class,
                    () -> ClangCcacheAdapter.prepare(folder.toFile(), null, folder.resolve("tool").toFile(), hash));
        }
    }

    @Test
    public void commandUsesDumpAsThePrimaryOutput() {
        var dumperCommand = List.of(
                "/tool", "-c", "/source.cpp", "-id=7", "-system-header-threshold=1",
                "-o", "/output.dump", "--", "-std=c++17");

        assertEquals(List.of(
                "ccache", "/tool", "-c", "/source.cpp", "-id=7", "-system-header-threshold=1",
                "-o", "/output.dump",
                "-MD", "-MF", "/output.d", "--", "-std=c++17"),
                ClangCcacheAdapter.command(dumperCommand, new File("/output.d")));
    }

    @Test
    public void environmentCompressesTheSchemaCacheAndDisablesWorkingDirectoryHashing() {
        var invocation = new ClangCcacheAdapter.Invocation(new File("/cache"));
        var environment = new HashMap<String, String>();
        environment.put("CCACHE_NOCOMPRESS", "true");

        invocation.configureEnvironment(environment);

        assertEquals(new File("/cache").getAbsolutePath(), environment.get("CCACHE_DIR"));
        assertEquals("true", environment.get("CCACHE_NOHASHDIR"));
        assertEquals("clang", environment.get("CCACHE_COMPILERTYPE"));
        assertEquals("true", environment.get("CCACHE_DEPEND"));
        assertEquals("true", environment.get("CCACHE_COMPRESS"));
        assertFalse(environment.containsKey("CCACHE_NOCOMPRESS"));
    }

    @Test
    public void environmentUsesGeneratedParseRootAsCcacheBaseDirectory() {
        var invocation = new ClangCcacheAdapter.Invocation(new File("/cache"), new File("/generated"));
        var environment = new HashMap<String, String>();

        invocation.configureEnvironment(environment);

        assertEquals(new File("/generated").getAbsolutePath(), environment.get("CCACHE_BASEDIR"));
    }
}
