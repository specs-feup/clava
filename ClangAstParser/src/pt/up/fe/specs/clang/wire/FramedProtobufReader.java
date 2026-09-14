/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Reads a stream of protobuf messages framed by an unsigned varint byte
 * length. A message is handed to the callback exactly once and is not kept by
 * this reader. The callback must therefore copy only the existing Clava data
 * it needs; retaining generated messages would turn this into a second AST.
 */
public final class FramedProtobufReader {

    public static final int DEFAULT_MAX_FRAME_BYTES = 64 * 1024 * 1024;
    private static final int MAX_VARINT_BYTES = 5;

    private final InputStream input;
    private final int maxFrameBytes;
    private long frameIndex;
    private long encodedBytes;

    public FramedProtobufReader(InputStream input) {
        this(input, DEFAULT_MAX_FRAME_BYTES);
    }

    public FramedProtobufReader(InputStream input, int maxFrameBytes) {
        this.input = Objects.requireNonNull(input, "input");
        if (maxFrameBytes <= 0) {
            throw new IllegalArgumentException("maxFrameBytes must be positive");
        }
        this.maxFrameBytes = maxFrameBytes;
    }

    /**
     * Consumes all frames until a clean EOF. Generated protobuf objects are
     * eligible for collection immediately after each callback returns.
     *
     * @return number of frames consumed
     */
    public long read(MessageParser parser, Consumer<Object> consumer) throws IOException {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(consumer, "consumer");

        while (true) {
            int first = input.read();
            if (first < 0) {
                return frameIndex;
            }

            long length = readLength(first);
            if (length > maxFrameBytes) {
                throw protocolError("frame " + frameIndex + " exceeds the configured limit of "
                        + maxFrameBytes + " bytes: " + length);
            }

            byte[] encoded = input.readNBytes((int) length);
            if (encoded.length != length) {
                throw new EOFException("Truncated protobuf frame " + frameIndex + ": expected " + length
                        + " bytes, received " + encoded.length);
            }
            encodedBytes += encoded.length;

            Object message;
            try {
                message = parser.parse(encoded);
            } catch (InvalidProtocolBufferException e) {
                throw protocolError("Malformed protobuf frame " + frameIndex + ": " + e.getMessage(), e);
            }

            frameIndex++;
            consumer.accept(message);
        }
    }

    public long frameIndex() {
        return frameIndex;
    }

    public long encodedBytes() {
        return encodedBytes;
    }

    private long readLength(int first) throws IOException {
        encodedBytes++;
        long value = first & 0x7fL;
        int shift = 7;
        if ((first & 0x80) == 0) {
            return value;
        }

        for (int byteIndex = 1; byteIndex < MAX_VARINT_BYTES; byteIndex++) {
            int next = input.read();
            if (next < 0) {
                throw new EOFException("Truncated protobuf frame length at frame " + frameIndex);
            }

            encodedBytes++;
            value |= (long) (next & 0x7f) << shift;
            if ((next & 0x80) == 0) {
                return value;
            }
            shift += 7;
        }

        throw protocolError("Invalid protobuf frame length at frame " + frameIndex
                + ": varint exceeds " + MAX_VARINT_BYTES + " bytes");
    }

    private static IOException protocolError(String message) {
        return new IOException(message);
    }

    private static IOException protocolError(String message, Throwable cause) {
        return new IOException(message, cause);
    }

    @FunctionalInterface
    public interface MessageParser {
        Object parse(byte[] encoded) throws InvalidProtocolBufferException;
    }
}
