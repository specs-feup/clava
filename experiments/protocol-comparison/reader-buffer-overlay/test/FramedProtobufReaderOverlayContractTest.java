/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import com.google.protobuf.Any;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Small standalone contract check for the benchmark overlay.
 * Compile beside FramedProtobufReader.java and run with protobuf-java on the
 * classpath. It exercises the same framing/error cases with buffering absent
 * and enabled, without running the benchmark workload.
 */
public final class FramedProtobufReaderOverlayContractTest {

    private static final String BUFFER_PROPERTY = "clava.astWireBenchmarkBufferedReader";
    private static final String COUNT_PROPERTY = "clava.astWireBenchmarkCountInputReads";
    private static final String COUNTS_PATH_PROPERTY = "clava.astWireBenchmarkInputReadCountsPath";

    private FramedProtobufReaderOverlayContractTest() {
    }

    public static void main(String[] args) throws Exception {
        System.clearProperty(COUNT_PROPERTY);
        System.clearProperty(COUNTS_PATH_PROPERTY);
        for (boolean buffered : List.of(false, true)) {
            if (buffered) {
                System.setProperty(BUFFER_PROPERTY, "true");
            } else {
                System.clearProperty(BUFFER_PROPERTY);
            }

            consumesFramesAndTracksBytes();
            rejectsTruncatedLength();
            rejectsTruncatedPayload();
            rejectsPayloadOverConfiguredLimit();
            rejectsNonPositiveLimit();
            wrapsMalformedPayloadWithFrameNumber();
        }
    }

    private static void consumesFramesAndTracksBytes() throws IOException {
        Any first = Any.newBuilder().setTypeUrl("first").build();
        Any second = Any.newBuilder().setTypeUrl("second").build();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        writeFrame(bytes, first.toByteArray());
        writeFrame(bytes, second.toByteArray());

        List<String> values = new ArrayList<>();
        FramedProtobufReader reader = new FramedProtobufReader(
                new ByteArrayInputStream(bytes.toByteArray()));
        long frames = reader.read(Any::parseFrom, value -> values.add(value.getTypeUrl()));

        require(frames == 2, "expected two frames");
        require(reader.frameIndex() == 2, "frame index did not reach two");
        require(reader.encodedBytes() == bytes.size(), "encoded byte count changed");
        require(values.equals(List.of("first", "second")), "parsed frame values changed");
    }

    private static void rejectsTruncatedLength() throws Exception {
        IOException error = expect(IOException.class, () -> new FramedProtobufReader(
                new ByteArrayInputStream(new byte[]{(byte) 0x80}))
                .read(Any::parseFrom, ignored -> { }));
        require(error instanceof EOFException, "truncated length must remain EOFException");
    }

    private static void rejectsTruncatedPayload() throws Exception {
        IOException error = expect(IOException.class, () -> new FramedProtobufReader(
                new ByteArrayInputStream(new byte[]{3, 1, 2}))
                .read(Any::parseFrom, ignored -> { }));
        require(error instanceof EOFException, "truncated payload must remain EOFException");
    }

    private static void rejectsPayloadOverConfiguredLimit() throws Exception {
        IOException error = expect(IOException.class, () -> new FramedProtobufReader(
                new ByteArrayInputStream(new byte[]{10}), 9)
                .read(Any::parseFrom, ignored -> { }));
        require(error.getMessage().contains("exceeds the configured limit"),
                "oversized payload must fail before reading its body");
    }

    private static void rejectsNonPositiveLimit() throws Exception {
        expect(IllegalArgumentException.class, () -> new FramedProtobufReader(
                new ByteArrayInputStream(new byte[0]), 0));
    }

    private static void wrapsMalformedPayloadWithFrameNumber() throws Exception {
        IOException error = expect(IOException.class, () -> new FramedProtobufReader(
                new ByteArrayInputStream(new byte[]{1, 0x7f}))
                .read(Any::parseFrom, ignored -> { }));
        require(error.getMessage().contains("Malformed protobuf frame 0"),
                "malformed payload must identify its frame");
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

    private static <T extends Throwable> T expect(Class<T> expected, ThrowingRunnable action)
            throws Exception {
        try {
            action.run();
        } catch (Throwable error) {
            if (expected.isInstance(error)) {
                return expected.cast(error);
            }
            throw new AssertionError("expected " + expected.getSimpleName() + " but got " + error, error);
        }
        throw new AssertionError("expected " + expected.getSimpleName() + " but no exception was thrown");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
