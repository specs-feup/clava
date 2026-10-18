/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.wire.TestStream.Ref;
import pt.up.fe.specs.clang.wire.TestStream.Union;
import pt.up.fe.specs.clava.context.ClavaContext;

class WireReaderTest {

    @TempDir
    Path temporary;

    private final TestStream stream = new TestStream();

    @Test
    void readsAStreamWithOnlyHeaderAndEnd() {
        var data = assertDoesNotThrow(() -> read(stream.block(stream.header(), stream.end())));
        assertTrue(data.get(ClangAstData.NODE_DATA).isEmpty());
    }

    @Test
    void rejectsAnotherSchema() {
        var failure = assertThrows(IOException.class,
                () -> read(stream.block(stream.header("0".repeat(64)), stream.end())));
        assertTrue(failure.getMessage().contains("differs from Clava's"), failure.getMessage());
    }

    @Test
    void rejectsIncompleteOrMisorderedStreams() {
        assertThrows(IOException.class, () -> read(stream.block(stream.end())));
        assertThrows(IOException.class, () -> read(stream.block(stream.header())));
        assertThrows(IOException.class, () -> read(stream.block(stream.header(), stream.header(), stream.end())));
        assertThrows(IOException.class,
                () -> read(stream.block(stream.header(), stream.end()), stream.block(stream.end())));
        assertThrows(IOException.class, () -> read(stream.block(stream.header(), stream.end()), new byte[] { 1, 0 }));
    }

    @Test
    void skipsAnUnsupportedNodeThatNothingReferences() throws IOException {
        var data = read(stream.block(stream.header(), file(), node(17, "LifetimeExtendedTemporaryDecl"),
                stream.end()));
        assertTrue(data.getClavaNodes().getNodes().isEmpty());
        assertTrue(data.get(ClangAstData.VISITED_CHILDREN).isEmpty());
    }

    @Test
    void rejectsAReferenceToAnUnsupportedNode() {
        var failure = assertThrows(IOException.class, () -> read(stream.block(stream.header(), file(),
                node(17, "LifetimeExtendedTemporaryDecl"), node(1, "StaticAssertDecl", 17), stream.end())));
        assertTrue(failure.getMessage().contains("resolve references"), failure.getMessage());
    }

    @Test
    void rejectsAPayloadThatDoesNotDescribeTheNodeClass() {
        var failure = assertThrows(IOException.class,
                () -> read(stream.block(stream.header(), file(), node(1, "BuiltinType"), stream.end())));
        assertTrue(failure.getMessage().contains("has a DeclData payload"), failure.getMessage());
    }

    @Test
    void mapsWireReferencesToScopedNodeIds() {
        var first = new WireReader.Tu(null, null, "first", new ArrayList<>());
        var second = new WireReader.Tu(null, null, "second", new ArrayList<>());
        assertNotEquals(first.id(1), second.id(1));
        assertNotEquals(first.id(-6), second.id(-6));
        assertEquals("nullptr_type", first.id(-1));
        assertEquals("nullptr_attr", first.id(-5));
        for (long invalid : new long[] { 0, -7, Long.MIN_VALUE, -4294967297L }) {
            assertThrows(IllegalArgumentException.class, () -> first.id(invalid));
        }
    }

    private int file() {
        return stream.record("File", stream.table("File", "id", 1L, "path", "unsupported-node.cpp"));
    }

    private int node(long id, String className, long... children) {
        int range = stream.table("Range", "file", 1L, "line", 1L, "column", 1L, "end_file", 0L, "end_line", 0L,
                "end_column", 0L);
        int source = stream.table("SourceInfo", "expansion", new Ref(range), "is_macro", false,
                "is_in_system_header", false);
        int base = stream.table("ClavaNodeData", "source", new Ref(source));
        int payload = stream.table("DeclData", "base", new Ref(base), "is_implicit", false, "is_used", false,
                "is_referenced", false, "is_invalid_decl", false, "is_module_private", false,
                "attributes", new long[0]);
        return stream.record("Node", stream.table("Node", "id", id, "class_name", className, "children", children,
                "payload", new Union("DeclData", payload)));
    }

    private ClangAstData read(byte[]... blocks) throws IOException {
        Path path = Files.createTempFile(temporary, "ast-", ".clv2");
        try (var output = Files.newOutputStream(path)) {
            for (byte[] block : blocks) {
                output.write(block);
            }
        }
        return WireReader.read(path, new ClavaContext(), null, "test");
    }
}
