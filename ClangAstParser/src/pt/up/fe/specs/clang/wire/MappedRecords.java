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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Sequential size-prefixed framing scan with bounded mapped windows. */
final class MappedRecords implements AutoCloseable {

    private static final long WINDOW = 64L * 1024 * 1024;
    private static final int MAX_RECORD = Integer.MAX_VALUE - Integer.BYTES;
    private static final Cleaner CLEANER = findCleaner();

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
        int payloadOffset = offset + Integer.BYTES;
        int root = mapped.getInt(payloadOffset);
        if(mapped.getInt(offset + 2 * Integer.BYTES)!=0x32564c43)
            throw new IOException("Invalid CLV2 block identifier at " + position);

        // The root offset is relative to the first byte after the size prefix.
        if (root < Integer.BYTES * 2 || root >= length) {
            throw new IOException("Invalid FlatBuffers root offset " + root + " at " + position);
        }

        ByteBuffer frameBuffer = mapped.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        frameBuffer.position(payloadOffset).limit(payloadOffset + length);
        frameBuffer = frameBuffer.slice().order(ByteOrder.LITTLE_ENDIAN);
        Frame frame = new Frame(frameBuffer, root, length);
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

        unmap(mapped);
        mapped = null;
        mappedStart = position;
        long bytes = Math.min(size - position, Math.max(WINDOW, requiredBytes));
        mapped = channel.map(FileChannel.MapMode.READ_ONLY, mappedStart, bytes)
                .order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        try {
            unmap(mapped);
            mapped = null;
        } catch (IOException e) {
            failure = e;
        }
        try {
            channel.close();
        } catch (IOException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static Cleaner findCleaner() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field singleton = unsafeClass.getDeclaredField("theUnsafe");
            singleton.setAccessible(true);
            Object unsafe = singleton.get(null);
            Method invokeCleaner = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
            return new Cleaner(unsafe, invokeCleaner);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static void unmap(ByteBuffer buffer) throws IOException {
        if (!(buffer instanceof MappedByteBuffer)) {
            return;
        }

        try {
            CLEANER.method().invoke(CLEANER.receiver(), buffer);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IOException("Could not release mapped AST buffer", e);
        }
    }

    private record Cleaner(Object receiver, Method method) {
    }
}
