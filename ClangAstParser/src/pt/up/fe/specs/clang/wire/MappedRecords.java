/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Sequential size-prefixed framing scan with bounded mapped windows. */
final class MappedRecords implements AutoCloseable {

    private static final long WINDOW = 64L * 1024 * 1024;
    private static final int MAX_RECORD = Integer.MAX_VALUE - Integer.BYTES;

    private final FileChannel channel;
    private final long size;

    private long position;
    private long mappedStart = -1;
    private ByteBuffer mapped;

    record Frame(ByteBuffer buffer, int rootOffset, int bytes) {
    }

    MappedRecords(Path path) throws IOException {
        channel = FileChannel.open(path, StandardOpenOption.READ);
        size = channel.size();
    }

    Frame next() throws IOException {
        if (position == size) {
            return null;
        }

        if (size - position < Integer.BYTES) {
            throw new EOFException("Truncated size-prefixed record length");
        }

        ensureMapped(Integer.BYTES);
        int offset = Math.toIntExact(position - mappedStart);
        int length = mapped.getInt(offset);

        if (length < Integer.BYTES * 2 || length > MAX_RECORD || length > size - position - Integer.BYTES) {
            throw new IOException("Invalid record length " + length + " at " + position);
        }

        ensureMapped(Math.addExact(Integer.BYTES, length));
        offset = Math.toIntExact(position - mappedStart);
        int root = mapped.getInt(offset + Integer.BYTES);
        if(mapped.getInt(offset + 2 * Integer.BYTES)!=0x32564c43)
            throw new IOException("Invalid CLV2 block identifier at " + position);

        // The root offset is relative to the first byte after the size prefix.
        if (root < Integer.BYTES || root >= length) {
            throw new IOException("Invalid FlatBuffers root offset " + root + " at " + position);
        }

        Frame frame = new Frame(mapped, offset + Integer.BYTES + root, length);
        position += Integer.BYTES + (long) length;
        return frame;
    }

    private void ensureMapped(int requiredBytes) throws IOException {
        if (mapped != null) {
            long offset = position - mappedStart;
            if (offset >= 0 && offset + requiredBytes <= mapped.limit()) {
                return;
            }
        }

        mappedStart = position;
        long bytes = Math.min(size - position, Math.max(WINDOW, requiredBytes));
        mapped = channel.map(FileChannel.MapMode.READ_ONLY, mappedStart, bytes)
                .order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
