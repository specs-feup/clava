/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.dumper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClangCcacheAdapterTest {
    @TempDir
    Path tempFolder;

    @Test
    public void namespacesIsolateSchemaAndToolchain() {
        String schema = "a".repeat(64);
        var first = ClangCcacheAdapter.prepare(tempFolder.toFile(), null, schema, "28.3-4.28.3");
        assertEquals(first.cacheFolder(),
                ClangCcacheAdapter.prepare(tempFolder.toFile(), null, schema, "28.3-4.28.3").cacheFolder());
        assertNotEquals(first.cacheFolder(),
                ClangCcacheAdapter.prepare(tempFolder.toFile(), null, "b".repeat(64), "28.3-4.28.3").cacheFolder());
        assertNotEquals(first.cacheFolder(),
                ClangCcacheAdapter.prepare(tempFolder.toFile(), null, schema, "28.3-4.29.0").cacheFolder());
        assertThrows(IllegalArgumentException.class,
                () -> ClangCcacheAdapter.prepare(tempFolder.toFile(), null, "bad", "28.3"));
    }

    @Test
    public void recognizesTruthyCcacheDisableValues() {
        assertTrue(ClangCcacheAdapter.isDisabled("1"));
        assertTrue(ClangCcacheAdapter.isDisabled(" true "));
        assertTrue(ClangCcacheAdapter.isDisabled("YES"));
        assertTrue(ClangCcacheAdapter.isDisabled("on"));
        assertFalse(ClangCcacheAdapter.isDisabled(null));
        assertFalse(ClangCcacheAdapter.isDisabled("0"));
        assertFalse(ClangCcacheAdapter.isDisabled("false"));
        assertFalse(ClangCcacheAdapter.isDisabled("off"));
        assertFalse(ClangCcacheAdapter.isDisabled("unknown"));
        assertFalse(ClangCcacheAdapter.isDisabled(""));
    }

    @Test
    public void enabledValuesAreNormalizedForTheCcacheSubprocess() {
        var invocation = new ClangCcacheAdapter.Invocation(new File("/cache"));
        for (String value : List.of("0", "false", "no", "off", "unknown", "")) {
            var environment = new HashMap<String, String>();
            environment.put("CCACHE_DISABLE", value);
            invocation.configureEnvironment(environment);
            assertFalse(environment.containsKey("CCACHE_DISABLE"), value);
        }
        var disabled = new HashMap<String, String>();
        disabled.put("CCACHE_DISABLE", "true");
        disabled.put("CCACHE_NODISABLE", "true");
        invocation.configureEnvironment(disabled);
        assertTrue(ClangCcacheAdapter.isDisabled(disabled.get("CCACHE_DISABLE")),
                "An explicit disable value wins over conflicting enable flags");
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
    public void environmentUsesTheGlobalCacheAndDisablesWorkingDirectoryHashing() {
        var invocation = new ClangCcacheAdapter.Invocation(new File("/cache"));
        var environment = new HashMap<String, String>();

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
