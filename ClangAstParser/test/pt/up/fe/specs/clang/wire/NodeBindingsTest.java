/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class NodeBindingsTest {

    /** Every node payload table must match its Clava class's DataKeys; this replaces build-time generation. */
    @Test
    void bindsEveryNodePayloadTable() {
        var schema = WireSchema.get();
        for (var payload : schema.unionMembers(schema.table("Node").field("payload"))) {
            assertDoesNotThrow(() -> NodeBindings.of(payload), payload.name);
        }
    }
}
