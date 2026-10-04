/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WireIdTest {
    @Test
    void rejectsNegativeIdsThatWouldNarrowToValidNullSentinels() {
        var files = new SchemaRuntime.Files("tu");
        for (long id : new long[] { 0, -7, Long.MIN_VALUE, -4294967297L, -4294967302L }) {
            assertThrows(IllegalArgumentException.class, () -> files.id(id));
        }
    }

    @Test
    void keepsPositiveIdsDistinctAcrossTranslationUnits() {
        var first = new SchemaRuntime.Files("first");
        var second = new SchemaRuntime.Files("second");
        assertNotEquals(first.id(1), second.id(1));
        assertEquals("nullptr_type", first.id(-1));
        assertEquals("nullptr_attr", first.id(-5));
        assertNotEquals(first.id(-6), second.id(-6));
    }
}
