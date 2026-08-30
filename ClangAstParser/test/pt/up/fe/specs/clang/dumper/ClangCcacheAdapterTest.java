/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.dumper;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClangCcacheAdapterTest {

    @Test
    public void systemPropertyCanEnableTheAdapter() {
        var previousValue = System.getProperty("clava.ccache");
        try {
            System.setProperty("clava.ccache", "true");
            assertTrue(ClangCcacheAdapter.isEnabled());
        } finally {
            if (previousValue == null) {
                System.clearProperty("clava.ccache");
            } else {
                System.setProperty("clava.ccache", previousValue);
            }
        }
    }

    @Test
    public void commandUsesDumpAsThePrimaryOutput() {
        var invocation = new ClangCcacheAdapter.Invocation(
                new File("/adapter"), new File("/cache"), new File("/tool"), new File("/clang"));
        var dumperCommand = List.of(
                "/tool", "/source.cpp", "-id=7", "-system-header-threshold=1",
                "-ast-dump-output=/old.dump", "--", "-std=c++17");

        assertEquals(List.of(
                "ccache", "/adapter", "-c", "/source.cpp", "-o", "/output.dump",
                "-id=7", "-system-header-threshold=1", "--", "-std=c++17"),
                ClangCcacheAdapter.command(invocation, dumperCommand,
                        new File("/source.cpp"), new File("/output.dump")));
    }

    @Test
    public void environmentUsesTheGlobalCacheAndDisablesWorkingDirectoryHashing() {
        var invocation = new ClangCcacheAdapter.Invocation(
                new File("/adapter"), new File("/cache"), new File("/tool"), new File("/clang"));
        var environment = new HashMap<String, String>();

        invocation.configureEnvironment(environment, new File("/source.cpp"));

        assertEquals(new File("/cache").getAbsolutePath(), environment.get("CCACHE_DIR"));
        assertEquals("true", environment.get("CCACHE_NOHASHDIR"));
        assertEquals("clang", environment.get("CCACHE_COMPILERTYPE"));
        assertEquals("content", environment.get("CCACHE_COMPILERCHECK"));
    }
}
