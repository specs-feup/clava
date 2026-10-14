/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Benchmark-only source overlay for testing whether buffering the raw framed
 * Protobuf input changes reader performance. It mirrors the production reader;
 * the timed control changes only the opt-in 64 KiB wrapper selected by
 * {@code -Dclava.astWireBenchmarkBufferedReader=true}. The default path is
 * unchanged. A separate diagnostic-only property,
 * {@code -Dclava.astWireBenchmarkCountInputReads=true}, counts calls to the
 * wrapped input stream and must not be used in timed runs. Do not install this
 * class in production artifacts.
 */
public final class FramedProtobufReader {

    public static final int DEFAULT_MAX_FRAME_BYTES = 64 * 1024 * 1024;
    private static final int MAX_VARINT_BYTES = 5;
    private static final String BUFFER_PROPERTY = "clava.astWireBenchmarkBufferedReader";
    private static final String COUNT_READS_PROPERTY = "clava.astWireBenchmarkCountInputReads";

    private final InputStream input;
    private final CountingInputStream inputReadCounter;
    private final int maxFrameBytes;
    private long frameIndex;
    private long encodedBytes;

    public FramedProtobufReader(InputStream input) {
        this(input, DEFAULT_MAX_FRAME_BYTES);
    }

    public FramedProtobufReader(InputStream input, int maxFrameBytes) {
        InputStream source = Objects.requireNonNull(input, "input");
        inputReadCounter = Boolean.getBoolean(COUNT_READS_PROPERTY) ? new CountingInputStream(source) : null;
        if (inputReadCounter != null) {
            source = inputReadCounter;
        }
        this.input = Boolean.getBoolean(BUFFER_PROPERTY)
                ? new BufferedInputStream(source, 64 * 1024)
                : source;
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
    public <M> long read(MessageParser<M> parser, Consumer<? super M> consumer) throws IOException {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(consumer, "consumer");

        while (true) {
            int first = input.read();
            if (first < 0) {
                if (inputReadCounter != null) {
                    inputReadCounter.report(frameIndex, encodedBytes);
                }
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

            M message;
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

    /** Counts calls to the wrapped stream; enable only for a separate diagnostic run. */
    private static final class CountingInputStream extends FilterInputStream {
        private long singleByteCalls;
        private long bulkCalls;
        private long bytesRead;

        private CountingInputStream(InputStream input) {
            super(input);
        }

        @Override
        public int read() throws IOException {
            singleByteCalls++;
            int value = super.read();
            if (value >= 0) {
                bytesRead++;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            bulkCalls++;
            int count = super.read(buffer, offset, length);
            if (count > 0) {
                bytesRead += count;
            }
            return count;
        }

        private void report(long frames, long encodedBytes) {
            String json = "{\"frames\":" + frames
                    + ",\"encoded_bytes\":" + encodedBytes
                    + ",\"single_byte_calls\":" + singleByteCalls
                    + ",\"bulk_calls\":" + bulkCalls
                    + ",\"bytes_read\":" + bytesRead + "}";
            String outputPath = System.getProperty("clava.astWireBenchmarkInputReadCountsPath");
            if (outputPath == null || outputPath.isBlank()) {
                System.err.println("AST_WIRE_INPUT_READ_COUNTS " + json);
                return;
            }

            try {
                Files.writeString(Path.of(outputPath), json + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("AST_WIRE_INPUT_READ_COUNTS_ERROR " + e.getMessage());
            }
        }
    }

    @FunctionalInterface
    public interface MessageParser<M> {
        M parse(byte[] encoded) throws InvalidProtocolBufferException;
    }
}
