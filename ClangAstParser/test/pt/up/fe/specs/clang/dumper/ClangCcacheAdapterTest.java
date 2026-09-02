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

public class ClangCcacheAdapterTest {

    @Test
    public void commandUsesDumpAsThePrimaryOutput() {
        var dumperCommand = List.of(
                "/tool", "-c", "/source.cpp", "-id=7", "-system-header-threshold=1",
                "-o", "/output.dump", "-ast-dump-compression=zstd", "--", "-std=c++17");

        assertEquals(List.of(
                "ccache", "/tool", "-c", "/source.cpp", "-id=7", "-system-header-threshold=1",
                "-o", "/output.dump", "-ast-dump-compression=zstd",
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
        assertEquals("true", environment.get("CCACHE_NOCOMPRESS"));
    }

    @Test
    public void environmentUsesGeneratedParseRootAsCcacheBaseDirectory() {
        var invocation = new ClangCcacheAdapter.Invocation(new File("/cache"), new File("/generated"));
        var environment = new HashMap<String, String>();

        invocation.configureEnvironment(environment);

        assertEquals(new File("/generated").getAbsolutePath(), environment.get("CCACHE_BASEDIR"));
    }
}
