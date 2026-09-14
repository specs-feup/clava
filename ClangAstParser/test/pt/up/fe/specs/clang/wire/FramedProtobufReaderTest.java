/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FramedProtobufReaderTest {

    @Test
    void consumesFramesIncrementally() throws IOException {
        Any first = Any.newBuilder().setTypeUrl("first").build();
        Any second = Any.newBuilder().setTypeUrl("second").build();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        writeFrame(bytes, first.toByteArray());
        writeFrame(bytes, second.toByteArray());

        List<String> values = new ArrayList<>();
        FramedProtobufReader reader = new FramedProtobufReader(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(2, reader.read(Any::parseFrom, value -> values.add(((Any) value).getTypeUrl())));
        assertEquals(List.of("first", "second"), values);
    }

    @Test
    void rejectsTruncatedLength() {
        assertThrows(EOFException.class, () -> new FramedProtobufReader(
                new ByteArrayInputStream(new byte[]{(byte) 0x80})).read(Any::parseFrom, ignored -> {
                }));
    }

    @Test
    void rejectsTruncatedPayload() {
        assertThrows(EOFException.class, () -> new FramedProtobufReader(
                new ByteArrayInputStream(new byte[]{3, 1, 2})).read(Any::parseFrom, ignored -> {
                }));
    }

    @Test
    void rejectsOversizedPayloadBeforeAllocation() {
        assertThrows(IOException.class, () -> new FramedProtobufReader(
                new ByteArrayInputStream(new byte[]{10}), 9).read(Any::parseFrom, ignored -> {
                }));
    }

    @Test
    void wrapsMalformedPayloadWithFrameNumber() {
        IOException error = assertThrows(IOException.class, () -> new FramedProtobufReader(
                new ByteArrayInputStream(new byte[]{1, 0x7f})).read(Any::parseFrom, ignored -> {
                }));
        assertEquals(true, error.getMessage().contains("frame 0"));
    }

    private static void writeFrame(ByteArrayOutputStream output, byte[] payload) {
        int value = payload.length;
        while ((value & ~0x7f) != 0) {
            output.write((value & 0x7f) | 0x80);
            value >>>= 7;
        }
        output.write(value);
        output.writeBytes(payload);
    }
}
