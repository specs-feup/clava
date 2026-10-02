/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.google.flatbuffers.FlatBufferBuilder;

import astwire.v2.Block;
import astwire.v2.Header;
import astwire.v2.Language;
import astwire.v2.Node;
import astwire.v2.NodePayload;
import astwire.v2.Record;
import astwire.v2.RecordPayload;
import astwire.v2.TemplateArgument;
import astwire.v2.TemplateIntegral;
import astwire.v2.TemplatePack;
import astwire.v2.TemplateSpecializationTypeData;
import astwire.v2.TemplateValue;
import astwire.v2.TypeData;

class WireVerifierTest {
    @Test
    void acceptsAStandardSizePrefixedBlockWithUnicode() {
        ByteBuffer payload = block();
        assertDoesNotThrow(() -> verify(payload));
    }

    @Test
    void rejectsAnOversizedVectorBeforeWalkingElements() {
        ByteBuffer payload = block();
        payload.putInt(records(payload), Integer.MAX_VALUE);
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> rejects(payload));
    }

    @Test
    void rejectsOffsetsIntoTheNextFrameEvenWhenMappedBytesExist() {
        ByteBuffer payload = block();
        int end = payload.limit();
        ByteBuffer window = ByteBuffer.allocate(end + 1024).order(ByteOrder.LITTLE_ENDIAN);
        window.put(payload.duplicate()).position(0).limit(end);
        int field = field(window, window.getInt(0), 4);
        window.putInt(field, end + 8 - field);
        rejects(window);
    }

    @Test
    void rejectsAVtableOutsideThePayload() {
        ByteBuffer payload = block();
        payload.putInt(payload.getInt(0), Integer.MIN_VALUE);
        rejects(payload);
    }

    @Test
    void rejectsAFieldOutsideItsDeclaredTable() {
        ByteBuffer payload = block();
        int root = payload.getInt(0);
        int vtable = root - payload.getInt(root);
        int objectSize = Short.toUnsignedInt(payload.getShort(vtable + 2));
        payload.putShort(vtable + 4, (short) objectSize);
        rejects(payload);
    }

    @Test
    void rejectsUnknownUnionTags() {
        ByteBuffer payload = block();
        payload.put(field(payload, record(payload), 4), (byte) 255);
        rejects(payload);
    }

    @Test
    void rejectsAMissingRequiredUnionPayload() {
        ByteBuffer payload = block();
        int record = record(payload);
        int vtable = record - payload.getInt(record);
        payload.putShort(vtable + 6, (short) 0);
        rejects(payload);
    }

    @Test
    void rejectsAMissingRequiredRecordsVector() {
        ByteBuffer payload = block();
        int root = payload.getInt(0);
        int vtable = root - payload.getInt(root);
        payload.putShort(vtable + 4, (short) 0);
        rejects(payload);
    }

    @Test
    void rejectsUnterminatedStrings() {
        ByteBuffer payload = block();
        int string = headerString(payload);
        payload.put(string + 4 + payload.getInt(string), (byte) 1);
        rejects(payload);
    }

    @Test
    void rejectsOverlongUtf8InsteadOfReplacingIt() {
        ByteBuffer payload = block();
        payload.put(headerString(payload) + 4, (byte) 0xc0);
        rejects(payload);
    }

    @Test
    void rejectsAnOffsetThatWrapsASignedInteger() {
        ByteBuffer payload = block();
        payload.putInt(field(payload, payload.getInt(0), 4), 0xfffffff0);
        rejects(payload);
    }

    @Test
    void rejectsATruncatedPayload() {
        ByteBuffer payload = block();
        payload.limit(payload.limit() - 8);
        rejects(payload);
    }

    @Test
    void rejectsNonBooleanBytes() {
        var builder = new FlatBufferBuilder(256);
        int file = builder.createString("source.cpp");
        Language.startLanguage(builder);
        Language.addFile(builder, file);
        Language.addLineComment(builder, true);
        int language = Language.endLanguage(builder);
        ByteBuffer payload = finish(builder, RecordPayload.Language, language);
        int table = indirect(payload, field(payload, record(payload), 6));
        payload.put(field(payload, table, 6), (byte) 2);
        rejects(payload);
    }

    @Test
    void acceptsEmptyVectorsWithOnlyLengthAlignment() {
        assertDoesNotThrow(() -> verify(templateBlock(0)));
    }

    @Test
    void boundsRecursionBeforeAStackOverflow() {
        ByteBuffer payload = templateBlock(130);
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> rejects(payload));
    }

    private static ByteBuffer templateBlock(int levels) {
        var builder = new FlatBufferBuilder(4096);
        int name = builder.createString("pack");
        int argument = TemplateArgument.createTemplateArgument(builder, TemplateValue.TemplateIntegral,
                TemplateIntegral.createTemplateIntegral(builder, name));
        for (int i = 0; i < levels; i++) {
            int values = TemplatePack.createArgumentsVector(builder, new int[] { argument });
            int pack = TemplatePack.createTemplatePack(builder, values);
            argument = TemplateArgument.createTemplateArgument(builder, TemplateValue.TemplatePack, pack);
        }
        int arguments = TemplateSpecializationTypeData.createTemplateArgumentsVector(builder,
                levels == 0 ? new int[0] : new int[] { argument });
        TypeData.startTypeData(builder);
        TypeData.addTypeAsString(builder, name);
        int base = TypeData.endTypeData(builder);
        TemplateSpecializationTypeData.startTemplateSpecializationTypeData(builder);
        TemplateSpecializationTypeData.addBase(builder, base);
        TemplateSpecializationTypeData.addTemplateName(builder, name);
        TemplateSpecializationTypeData.addTemplateArguments(builder, arguments);
        int type = TemplateSpecializationTypeData.endTemplateSpecializationTypeData(builder);
        int node = Node.createNode(builder, 1, name, NodePayload.TemplateSpecializationTypeData, type);
        return finish(builder, RecordPayload.Node, node);
    }

    private static ByteBuffer block() {
        var builder = new FlatBufferBuilder(256);
        int hash = builder.createString("schema-\u03bb-\ud83d\ude80");
        int header = Header.createHeader(builder, hash);
        return finish(builder, RecordPayload.Header, header);
    }

    private static ByteBuffer finish(FlatBufferBuilder builder, byte kind, int value) {
        int record = Record.createRecord(builder, kind, value);
        int records = Block.createRecordsVector(builder, new int[] { record });
        Block.finishSizePrefixedBlockBuffer(builder, Block.createBlock(builder, records));
        ByteBuffer complete = builder.dataBuffer();
        byte[] copy = new byte[complete.remaining()];
        complete.get(copy);
        return ByteBuffer.wrap(copy, 4, copy.length - 4).slice().order(ByteOrder.LITTLE_ENDIAN);
    }

    private static int field(ByteBuffer payload, int table, int vtableOffset) {
        int vtable = table - payload.getInt(table);
        return table + Short.toUnsignedInt(payload.getShort(vtable + vtableOffset));
    }

    private static int indirect(ByteBuffer payload, int offset) {
        return offset + payload.getInt(offset);
    }

    private static int records(ByteBuffer payload) {
        return indirect(payload, field(payload, payload.getInt(0), 4));
    }

    private static int record(ByteBuffer payload) {
        return indirect(payload, records(payload) + 4);
    }

    private static int headerString(ByteBuffer payload) {
        int header = indirect(payload, field(payload, record(payload), 6));
        return indirect(payload, field(payload, header, 4));
    }

    private static void verify(ByteBuffer payload) throws IOException {
        WireVerifier.verify(payload, payload.getInt(0));
    }

    private static void rejects(ByteBuffer payload) {
        assertThrows(IOException.class, () -> verify(payload));
    }
}
