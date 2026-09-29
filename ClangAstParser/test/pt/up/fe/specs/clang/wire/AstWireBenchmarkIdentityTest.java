/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.dumper.AstWireBenchmarkIdentity;

class AstWireBenchmarkIdentityTest {

    @TempDir
    Path temp;

    @Test
    void identityRegistrationIsOptInScopedAndPathNormalized() {
        String previousProperty = System.getProperty("clava.astWireMetrics");
        try {
            File source = temp.resolve("nested/../fixture.c").toFile();

            System.setProperty("clava.astWireMetrics", "false");
            try (var ignored = AstWireBenchmarkIdentity.register(
                    Map.of(source, "c/fixtures/fixture.c"), "ExampleTest#parses", 2, "original")) {
                assertNull(AstWireBenchmarkIdentity.lookup(temp.resolve("fixture.c").toFile(), "1"));
            }

            System.setProperty("clava.astWireMetrics", "true");
            AstWireBenchmarkIdentity.Identity identity;
            try (var ignored = AstWireBenchmarkIdentity.register(
                    Map.of(source, "c/fixtures/fixture.c"), "ExampleTest#parses", 2, "roundtrip")) {
                identity = AstWireBenchmarkIdentity.lookup(temp.resolve("fixture.c").toFile(), "3");
                assertNotNull(identity);
                assertEquals("ExampleTest#parses", identity.testId());
                assertEquals(2, identity.testerInvocation());
                assertEquals("roundtrip", identity.parsePass());
                assertEquals("c/fixtures/fixture.c", identity.resourceKey());
                assertEquals("3", identity.parseId());
            }

            assertNull(AstWireBenchmarkIdentity.lookup(temp.resolve("fixture.c").toFile(), "3"));
        } finally {
            if (previousProperty == null) {
                System.clearProperty("clava.astWireMetrics");
            } else {
                System.setProperty("clava.astWireMetrics", previousProperty);
            }
        }
    }
}
