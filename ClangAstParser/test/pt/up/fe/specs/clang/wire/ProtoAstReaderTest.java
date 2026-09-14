/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clava.context.ClavaContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtoAstReaderTest {

    @Test
    void acceptsACompleteHeaderAndEndStream() throws IOException {
        var header = Envelope.newBuilder().setHeader(header()).build();
        var end = Envelope.newBuilder().setEnd(End.newBuilder()
                .setRecords(2).setNodes(0).setRawBytes(rawBytesBeforeEnd(header)).setFiles(0).setIds(0)).build();
        var bytes = stream(header, end);

        var result = ProtoAstReader.read(new ByteArrayInputStream(bytes), new ClavaContext(), null, "unit");
        assertEquals(2, result.metrics().frames());
        assertEquals(0, result.data().get(ClangAstData.NODE_DATA).size());
    }

    @Test
    void rejectsAnIncompatibleHeaderBeforeApplyingRecords() {
        var badHeader = header().toBuilder().setProtocolMajor(2).build();
        var bytes = stream(Envelope.newBuilder().setHeader(badHeader).build());
        IOException error = assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(bytes), new ClavaContext(), null, "unit"));
        assertEquals(true, error.getMessage().contains("version"));
    }

    @Test
    void rejectsMissingMagic() {
        var bytes = stream(Envelope.newBuilder().setHeader(header()).build());
        bytes[0] = 'X';
        assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(bytes), new ClavaContext(), null, "unit"));
    }

    @Test
    void rejectsMissingRequiredHeaderPresence() {
        var incomplete = header().toBuilder().clearSchemaSha256().build();
        var bytes = stream(Envelope.newBuilder().setHeader(incomplete).build());
        IOException error = assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(bytes), new ClavaContext(), null, "unit"));
        assertEquals(true, error.getMessage().contains("required field"));
    }

    @Test
    void rejectsAReferenceToAnUnknownTopLevelNode() {
        var header = Envelope.newBuilder().setHeader(header()).build();
        var topLevel = Envelope.newBuilder().setRecord(Record.newBuilder().setTopLevel(TopLevel.newBuilder()
                .setKind(TopLevelKind.TOPLEVELKIND_DECL).setNode(1))).build();
        var end = Envelope.newBuilder().setEnd(End.newBuilder()
                .setRecords(3).setNodes(0).setRawBytes(rawBytesBeforeEnd(header, topLevel)).setFiles(0).setIds(1)).build();
        IOException error = assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(stream(header, topLevel, end)),
                        new ClavaContext(), null, "unit"));
        assertEquals(true, error.getMessage().contains("Top-level reference"));
    }

    @Test
    void rejectsAnUnresolvableRequiredNodeReference() {
        var header = Envelope.newBuilder().setHeader(header()).build();
        var node = Envelope.newBuilder().setRecord(Record.newBuilder().setNode(Node.newBuilder()
                .setId(1)
                .setClassName("PointerType")
                .setPointerTypeData(PointerTypeData.newBuilder()
                        .setBase(TypeData.newBuilder()
                                .setTypeAsString("int *")
                                .setTypeDependency(TypeDependency.TYPEDEPENDENCY_NONE)
                                .setIsVariablyModified(false)
                                .setContainsUnexpandedParameterPack(false)
                                .setIsFromAst(false)
                                .setUnqualifiedDesugaredType(-1))
                        .setPointeeType(-1)))).build();
        var nodeClass = Envelope.newBuilder().setRecord(Record.newBuilder().setNodeClass(NodeClass.newBuilder()
                .setNode(1).setClassName("PointerType"))).build();
        var end = Envelope.newBuilder().setEnd(End.newBuilder()
                .setRecords(4).setNodes(1).setRawBytes(rawBytesBeforeEnd(header, node, nodeClass)).setFiles(0)
                .setIds(1)).build();

        IOException error = assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(stream(header, node, nodeClass, end)),
                        new ClavaContext(), null, "unit"));
        assertEquals(true, error.getMessage().contains("resolve protobuf AST references"));
    }

    @Test
    void rejectsAnEndCounterThatDoesNotMatchTheFramedStream() {
        var header = Envelope.newBuilder().setHeader(header()).build();
        var end = Envelope.newBuilder().setEnd(End.newBuilder()
                .setRecords(2).setNodes(0).setRawBytes(1).setFiles(0).setIds(0)).build();
        IOException error = assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(stream(header, end)),
                        new ClavaContext(), null, "unit"));
        assertEquals(true, error.getMessage().contains("raw_bytes"));
    }

    private static Header header() {
        return Header.newBuilder()
                .setProtocolMajor(1)
                .setProtocolMinor(0)
                .setSchemaId("clava-ast-wire")
                .setProducerVersion("test")
                .setLlvmMajor(18)
                .setSchemaSha256(com.google.protobuf.ByteString.copyFromUtf8(ProtoSchemaHash.VALUE))
                .build();
    }

    private static byte[] stream(Envelope... envelopes) {
        var bytes = new ByteArrayOutputStream();
        bytes.writeBytes(new byte[] { 'C', 'L', 'A', 'V', 'A', 'P', 'B', '1' });
        for (Envelope envelope : envelopes) {
            byte[] payload = envelope.toByteArray();
            int length = payload.length;
            while ((length & ~0x7f) != 0) {
                bytes.write((length & 0x7f) | 0x80);
                length >>>= 7;
            }
            bytes.write(length);
            bytes.writeBytes(payload);
        }
        return bytes.toByteArray();
    }

    private static long rawBytesBeforeEnd(Envelope... prior) {
        long bytes = 8;
        for (Envelope envelope : prior) {
            int payloadLength = envelope.toByteArray().length;
            bytes += varintSize(payloadLength) + payloadLength;
        }
        return bytes;
    }

    private static int varintSize(long value) {
        int size = 1;
        while ((value >>>= 7) != 0) {
            size++;
        }
        return size;
    }
}
