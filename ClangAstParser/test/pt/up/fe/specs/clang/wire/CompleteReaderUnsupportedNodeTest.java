/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.flatbuffers.FlatBufferBuilder;

import astwire.v2.Block;
import astwire.v2.Children;
import astwire.v2.ClavaNodeData;
import astwire.v2.DeclData;
import astwire.v2.End;
import astwire.v2.File;
import astwire.v2.Header;
import astwire.v2.Node;
import astwire.v2.NodeClass;
import astwire.v2.NodePayload;
import astwire.v2.Record;
import astwire.v2.RecordPayload;
import astwire.v2.Range;
import astwire.v2.SourceInfo;
import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clava.context.ClavaContext;

class CompleteReaderUnsupportedNodeTest {
    @TempDir
    Path temporary;

    @Test
    void ignoresAnUnsupportedUnreachableNode() throws IOException {
        Path path = write(block(header(), file(), node(17, "LifetimeExtendedTemporaryDecl"),
                nodeClass(17, "LifetimeExtendedTemporaryDecl"), children(17), end(5, 1, 1, 17)));
        var result = assertDoesNotThrow(() -> CompleteReader.read(path, new ClavaContext(), null, "test"));
        assertTrue(result.data().getClavaNodes().getNodes().isEmpty());
        assertTrue(result.data().get(ClangAstData.VISITED_CHILDREN).isEmpty());
        CompleteReader.releaseLookup(result.data());
    }

    @Test
    void rejectsAnUnsupportedNodeReachableFromASupportedParent() throws IOException {
        Path path = write(block(header(), file(), children(1, 17), node(1, "StaticAssertDecl"),
                nodeClass(1, "StaticAssertDecl"), children(17), node(17, "LifetimeExtendedTemporaryDecl"),
                nodeClass(17, "LifetimeExtendedTemporaryDecl"), end(8, 2, 1, 17)));
        IOException failure = assertThrows(IOException.class,
                () -> CompleteReader.read(path, new ClavaContext(), null, "test"));
        assertTrue(failure.getMessage().contains("resolve references"));
    }

    @Test
    void rejectsAKnownClassWithAnIncompatiblePayloadFamily() throws IOException {
        Path path = write(block(header(), file(), node(1, "BuiltinType"), end(3, 1, 1, 1)));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> CompleteReader.read(path, new ClavaContext(), null, "test"));
        assertTrue(failure.getMessage().contains("payload mismatch"));
    }

    private Path write(byte[] block) throws IOException {
        Path path = Files.createTempFile(temporary, "wire-unsupported-", ".flat");
        Files.write(path, block);
        return path;
    }

    private static ToIntFunction<FlatBufferBuilder> header() {
        return builder -> record(builder, RecordPayload.Header,
                Header.createHeader(builder, builder.createString(GeneratedNodes.SCHEMA_HASH)));
    }

    private static ToIntFunction<FlatBufferBuilder> file() {
        return builder -> record(builder, RecordPayload.File,
                File.createFile(builder, 1, builder.createString("unsupported-node.cpp")));
    }

    private static ToIntFunction<FlatBufferBuilder> children(long nodeId, long... childIds) {
        return builder -> {
            int children = Children.createChildrenVector(builder, childIds);
            return record(builder, RecordPayload.Children,
                    Children.createChildren(builder, nodeId, children));
        };
    }

    private static ToIntFunction<FlatBufferBuilder> node(long id, String className) {
        return builder -> {
            int range = Range.createRange(builder, 1, 1, 1, 0, 0, 0);
            int source = SourceInfo.createSourceInfo(builder, range, false, range, false);
            int base = ClavaNodeData.createClavaNodeData(builder, source);
            int attributes = DeclData.createAttributesVector(builder, new long[0]);
            int payload = DeclData.createDeclData(builder, base, false, false, false, false, false, attributes);
            int name = builder.createString(className);
            return record(builder, RecordPayload.Node,
                    Node.createNode(builder, id, name, NodePayload.DeclData, payload));
        };
    }

    private static ToIntFunction<FlatBufferBuilder> nodeClass(long id, String className) {
        return builder -> record(builder, RecordPayload.NodeClass,
                NodeClass.createNodeClass(builder, id, builder.createString(className)));
    }

    private static ToIntFunction<FlatBufferBuilder> end(long records, long nodes, long files, long ids) {
        return builder -> record(builder, RecordPayload.End,
                End.createEnd(builder, records, nodes, (int) files, ids));
    }

    private static int record(FlatBufferBuilder builder, byte kind, int value) {
        return Record.createRecord(builder, kind, value);
    }

    @SafeVarargs
    private static byte[] block(ToIntFunction<FlatBufferBuilder>... payloads) {
        var builder = new FlatBufferBuilder(512);
        builder.forceDefaults(true);
        int[] records = new int[payloads.length];
        for (int index = 0; index < records.length; index++) {
            records[index] = payloads[index].applyAsInt(builder);
        }
        int vector = Block.createRecordsVector(builder, records);
        Block.finishSizePrefixedBlockBuffer(builder, Block.createBlock(builder, vector));
        ByteBuffer buffer = builder.dataBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
