/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Checks one size-prefixed Block payload before any generated accessor reads it. */
public final class WireVerifier {
    static final int SCALAR = 0, BOOLEAN = 1, STRING = 2, TABLE = 3, UNION = 4,
            SCALAR_VECTOR = 5, BOOLEAN_VECTOR = 6, STRING_VECTOR = 7, TABLE_VECTOR = 8;
    private static final int MAX_DEPTH = 128;

    record Shape(String name, Field[] fields) {
    }

    record Field(String name, int offset, int kind, int width, boolean unsigned, int target,
            boolean required, long[] enumValues, int[] unionTypes, int discriminator) {
    }

    private final ByteBuffer bytes;
    private final int limit;
    private long remainingWork;

    private WireVerifier(ByteBuffer payload) {
        bytes = payload.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        limit = bytes.limit();
        remainingWork = Math.max(1024L, 8L * limit);
    }

    /** The buffer must start after the size prefix and end at this Block's last byte. */
    public static void verify(ByteBuffer payload, int rootOffset) throws IOException {
        var verifier = new WireVerifier(payload);
        verifier.range(0, 8);
        if (verifier.bytes.getInt(0) != rootOffset || rootOffset < 8
                || verifier.bytes.getInt(4) != 0x32564c43) {
            throw invalid("Invalid Block root or CLV2 identifier");
        }
        verifier.table(rootOffset, GeneratedWireVerifier.ROOT, 0);
    }

    private void table(int position, int shapeIndex, int depth) throws IOException {
        if (depth >= MAX_DEPTH) {
            throw invalid("Table nesting exceeds " + MAX_DEPTH);
        }
        spend(1);
        aligned(position, 4);
        range(position, 4);
        int displacement = bytes.getInt(position);
        if (displacement == 0) {
            throw invalid("Missing table vtable");
        }
        long vtableLong = (long) position - displacement;
        range(vtableLong, 4);
        int vtable = (int) vtableLong;
        aligned(vtable, 2);
        int vtableSize = Short.toUnsignedInt(bytes.getShort(vtable));
        int objectSize = Short.toUnsignedInt(bytes.getShort(vtable + 2));
        if (vtableSize < 4 || (vtableSize & 1) != 0 || objectSize < 4) {
            throw invalid("Invalid table or vtable size");
        }
        range(vtable, vtableSize);
        range(position, objectSize);
        Shape shape = GeneratedWireVerifier.SHAPES[shapeIndex];
        for (Field field : shape.fields()) {
            spend(1);
            int slot = slot(vtable, vtableSize, field.offset());
            if (slot == 0) {
                if (field.required()) {
                    throw invalid("Missing " + field.name());
                }
                if (field.kind() == UNION && discriminator(position, objectSize, vtable, vtableSize, field) != 0) {
                    throw invalid("Union tag without payload: " + field.name());
                }
                continue;
            }
            if (slot < 4 || (long) slot + (isVector(field.kind()) ? 4 : field.width()) > objectSize) {
                throw invalid("Field exceeds table: " + field.name());
            }
            int value = position + slot;
            aligned(value, isVector(field.kind()) ? 4 : field.width());
            switch (field.kind()) {
                case SCALAR, BOOLEAN -> scalar(value, field);
                case STRING -> string(indirect(value));
                case TABLE -> table(indirect(value), field.target(), depth + 1);
                case UNION -> {
                    int tag = discriminator(position, objectSize, vtable, vtableSize, field);
                    if (tag <= 0 || tag >= field.unionTypes().length || field.unionTypes()[tag] < 0) {
                        throw invalid("Unknown union tag: " + field.name());
                    }
                    table(indirect(value), field.unionTypes()[tag], depth + 1);
                }
                default -> vector(indirect(value), field, depth);
            }
        }
    }

    private static boolean isVector(int kind) {
        return kind >= SCALAR_VECTOR;
    }

    private int slot(int vtable, int size, int offset) {
        return offset < size ? Short.toUnsignedInt(bytes.getShort(vtable + offset)) : 0;
    }

    private int discriminator(int table, int objectSize, int vtable, int vtableSize, Field field)
            throws IOException {
        int tagSlot = slot(vtable, vtableSize, field.discriminator());
        if (tagSlot == 0) {
            return 0;
        }
        if (tagSlot < 4 || tagSlot >= objectSize) {
            throw invalid("Union discriminator exceeds table: " + field.name());
        }
        return Byte.toUnsignedInt(bytes.get(table + tagSlot));
    }

    private int indirect(int position) throws IOException {
        range(position, 4);
        long offset = Integer.toUnsignedLong(bytes.getInt(position));
        if (offset < 4) {
            throw invalid("Invalid forward offset");
        }
        long target = position + offset;
        range(target, 4);
        return (int) target;
    }

    private void vector(int position, Field field, int depth) throws IOException {
        aligned(position, 4);
        range(position, 4);
        long count = Integer.toUnsignedLong(bytes.getInt(position));
        range((long) position + 4, count * field.width());
        if (count != 0) {
            aligned(position + 4, field.width());
        }
        spend(count);
        for (int i = 0; i < count; i++) {
            int element = position + 4 + i * field.width();
            switch (field.kind()) {
                case TABLE_VECTOR -> table(indirect(element), field.target(), depth + 1);
                case STRING_VECTOR -> string(indirect(element));
                case SCALAR_VECTOR, BOOLEAN_VECTOR -> scalar(element, field);
                default -> throw invalid("Unsupported generated vector descriptor");
            }
        }
    }

    private void scalar(int position, Field field) throws IOException {
        if (field.kind() == BOOLEAN || field.kind() == BOOLEAN_VECTOR) {
            int value = Byte.toUnsignedInt(bytes.get(position));
            if (value > 1) {
                throw invalid("Invalid boolean: " + field.name());
            }
        }
        if (field.enumValues() != null) {
            long value = switch (field.width()) {
                case 1 -> field.unsigned() ? Byte.toUnsignedInt(bytes.get(position)) : bytes.get(position);
                case 2 -> field.unsigned() ? Short.toUnsignedInt(bytes.getShort(position)) : bytes.getShort(position);
                case 4 -> field.unsigned() ? Integer.toUnsignedLong(bytes.getInt(position)) : bytes.getInt(position);
                case 8 -> bytes.getLong(position);
                default -> throw invalid("Unsupported enum width");
            };
            // UType is checked with its paired union payload, including tags above 127.
            for (long allowed : field.enumValues()) {
                if (value == allowed) {
                    return;
                }
            }
            throw invalid("Invalid enum: " + field.name());
        }
    }

    private void string(int position) throws IOException {
        aligned(position, 4);
        range(position, 4);
        long length = Integer.toUnsignedLong(bytes.getInt(position));
        range((long) position + 4, length + 1);
        int start = position + 4;
        int end = start + (int) length;
        if (bytes.get(end) != 0) {
            throw invalid("Unterminated string");
        }
        spend(length);
        for (int i = start; i < end;) {
            int first = Byte.toUnsignedInt(bytes.get(i++));
            if (first < 0x80) {
                continue;
            }
            int continuation;
            if (first >= 0xc2 && first <= 0xdf) {
                continuation = 1;
            } else if (first >= 0xe0 && first <= 0xef) {
                continuation = 2;
            } else if (first >= 0xf0 && first <= 0xf4) {
                continuation = 3;
            } else {
                throw invalid("Invalid UTF-8 leading byte");
            }
            if (end - i < continuation) {
                throw invalid("Truncated UTF-8 string");
            }
            int second = Byte.toUnsignedInt(bytes.get(i));
            if ((first == 0xe0 && second < 0xa0) || (first == 0xed && second >= 0xa0)
                    || (first == 0xf0 && second < 0x90) || (first == 0xf4 && second >= 0x90)) {
                throw invalid("Invalid UTF-8 code point");
            }
            for (int j = 0; j < continuation; j++) {
                if ((bytes.get(i++) & 0xc0) != 0x80) {
                    throw invalid("Invalid UTF-8 continuation byte");
                }
            }
        }
    }

    private void aligned(int position, int alignment) throws IOException {
        // The omitted size prefix contributes four bytes to FlatBuffers alignment.
        if (((long) position + Integer.BYTES) % alignment != 0) {
            throw invalid("Misaligned wire value at " + position + " with alignment " + alignment);
        }
    }

    private void range(long position, long size) throws IOException {
        if (position < 0 || size < 0 || position > limit || size > limit - position) {
            throw invalid("Wire offset or length exceeds this Block");
        }
    }

    private void spend(long work) throws IOException {
        remainingWork -= work;
        if (remainingWork < 0) {
            throw invalid("Wire verification work limit exceeded");
        }
    }

    private static IOException invalid(String reason) {
        return new IOException("Malformed FlatBuffers AST: " + reason);
    }
}
