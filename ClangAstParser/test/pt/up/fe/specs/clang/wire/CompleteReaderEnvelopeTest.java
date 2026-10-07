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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.flatbuffers.FlatBufferBuilder;

import astwire.v2.Block;
import astwire.v2.Counter;
import astwire.v2.End;
import astwire.v2.Header;
import astwire.v2.ClavaNodeData;
import astwire.v2.ExprData;
import astwire.v2.LambdaExprData;
import astwire.v2.Node;
import astwire.v2.NodePayload;
import astwire.v2.SourceInfo;
import astwire.v2.StmtData;
import astwire.v2.Record;
import astwire.v2.RecordPayload;
import pt.up.fe.specs.clava.context.ClavaContext;

class CompleteReaderEnvelopeTest {
    @TempDir
    Path temporary;

    @Test
    void acceptsACompleteEmptyAstAndReleasesItsFile() throws IOException {
        Path path = write(block(header(), end(1)));
        var result = assertDoesNotThrow(() -> read(path));
        CompleteReader.releaseLookup(result.data());
        Files.delete(path);
        assertTrue(Files.notExists(path));
    }

    @Test
    void rejectsASchemaMismatchAndReleasesItsFile() throws IOException {
        Path path = write(block(b -> record(b, RecordPayload.Header,
                Header.createHeader(b, b.createString("0".repeat(64))))));
        IOException failure = assertThrows(IOException.class, () -> read(path));
        assertTrue(failure.getMessage().contains("schema hash"));
        Files.delete(path);
        assertTrue(Files.notExists(path));
    }

    @Test
    void rejectsARecordBeforeTheHeader() throws IOException {
        Path path = write(block(counter(), header(), end(2)));
        assertThrows(IOException.class, () -> read(path));
    }

    @Test
    void rejectsADuplicateHeader() throws IOException {
        Path path = write(block(header(), header(), end(2)));
        assertThrows(IOException.class, () -> read(path));
    }

    @Test
    void rejectsAMissingEnd() throws IOException {
        Path path = write(block(header()));
        assertThrows(IOException.class, () -> read(path));
    }

    @Test
    void rejectsAnEmptyBlock() throws IOException {
        Path path = write(block());
        assertThrows(IOException.class, () -> read(path));
    }

    @Test
    void rejectsTrailingRecordsInTheSameBlock() throws IOException {
        Path path = write(block(header(), end(1), counter()));
        assertThrows(IOException.class, () -> read(path));
    }

    @Test
    void rejectsTrailingRecordsInAnotherBlock() throws IOException {
        Path path = write(block(header(), end(1)), block(counter()));
        assertThrows(IOException.class, () -> read(path));
    }

    @Test
    void rejectsIncorrectEndCounts() throws IOException {
        Path path = write(block(header(), end(9)));
        assertThrows(IllegalArgumentException.class, () -> read(path));
    }

    @Test
    void rejectsATruncatedSizePrefix() throws IOException {
        Path path = write(block(header(), end(1)), new byte[] { 1, 0, 0 });
        assertThrows(IOException.class, () -> read(path));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    void rejectsMisalignedLambdaCaptureMetadataAndReleasesItsFile(int shortenedVector) throws IOException {
        Path path = write(block(header(), builder -> malformedLambda(builder, shortenedVector), end(2)));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> read(path));
        assertTrue(failure.getMessage().contains("capture vector length mismatch"), failure.getMessage());
        Files.delete(path);
        assertTrue(Files.notExists(path));
    }

    private static int malformedLambda(FlatBufferBuilder builder, int shortenedVector) {
        SourceInfo.startSourceInfo(builder);
        SourceInfo.addIsMacro(builder, false);
        SourceInfo.addIsInSystemHeader(builder, false);
        int source = SourceInfo.endSourceInfo(builder);
        int nodeBase = ClavaNodeData.createClavaNodeData(builder, source);
        int statement = StmtData.createStmtData(builder, nodeBase);
        int expression = ExprData.createExprData(builder, statement, -1, 0, 0, false);
        int kinds = LambdaExprData.createCaptureKindsVector(builder, new int[] { 0 });
        int name = builder.createString("value");
        int names = LambdaExprData.createInitCaptureNamesVector(builder,
                shortenedVector == 0 ? new int[0] : new int[] { name });
        int styles = LambdaExprData.createCaptureInitStylesVector(builder,
                shortenedVector == 1 ? new int[0] : new int[] { 0 });
        int packs = LambdaExprData.createCapturePackExpansionsVector(builder,
                shortenedVector == 2 ? new boolean[0] : new boolean[] { false });
        int implicit = LambdaExprData.createCaptureIsImplicitVector(builder,
                shortenedVector == 3 ? new boolean[0] : new boolean[] { false });
        int payload = LambdaExprData.createLambdaExprData(builder, expression, false, false, false, false,
                0, -2, kinds, names, styles, packs, implicit);
        int node = Node.createNode(builder, 1, builder.createString("LambdaExpr"), NodePayload.LambdaExprData,
                payload);
        return record(builder, RecordPayload.Node, node);
    }

    private CompleteReader.Result read(Path path) throws IOException {
        return CompleteReader.read(path, new ClavaContext(), null, "test");
    }

    private Path write(byte[]... blocks) throws IOException {
        Path path = Files.createTempFile(temporary, "wire-", ".flat");
        try (var output = Files.newOutputStream(path)) {
            for (byte[] block : blocks) {
                output.write(block);
            }
        }
        return path;
    }

    private static ToIntFunction<FlatBufferBuilder> header() {
        return builder -> record(builder, RecordPayload.Header,
                Header.createHeader(builder, builder.createString(GeneratedNodes.SCHEMA_HASH)));
    }

    private static ToIntFunction<FlatBufferBuilder> end(long records) {
        return builder -> record(builder, RecordPayload.End, End.createEnd(builder, records, 0, 0, 0));
    }

    private static ToIntFunction<FlatBufferBuilder> counter() {
        return builder -> record(builder, RecordPayload.Counter, Counter.createCounter(builder, 1));
    }

    private static int record(FlatBufferBuilder builder, byte kind, int value) {
        return Record.createRecord(builder, kind, value);
    }

    @SafeVarargs
    private static byte[] block(ToIntFunction<FlatBufferBuilder>... payloads) {
        var builder = new FlatBufferBuilder(512);
        builder.forceDefaults(true);
        int[] records = new int[payloads.length];
        for (int i = 0; i < records.length; i++) {
            records[i] = payloads[i].applyAsInt(builder);
        }
        int vector = Block.createRecordsVector(builder, records);
        Block.finishSizePrefixedBlockBuffer(builder, Block.createBlock(builder, vector));
        ByteBuffer buffer = builder.dataBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
