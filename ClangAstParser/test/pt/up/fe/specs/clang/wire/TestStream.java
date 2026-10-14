/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.reflection.BaseType;

/**
 * Builds AST wire blocks for tests, naming fields as the schema does. Field values are Booleans, Longs, Strings,
 * {@code long[]} vectors, {@link Ref}s to tables built earlier in the same block, or {@link Union}s.
 */
final class TestStream {

    record Ref(int offset) {
    }

    record Union(String member, int offset) {
    }

    private final WireSchema schema = WireSchema.get();
    private FlatBufferBuilder builder = newBuilder();

    int table(String name, Object... fieldsAndValues) {
        var spec = schema.table(name);
        // Strings and vectors must be built before the table that refers to them.
        Object[] values = fieldsAndValues.clone();
        for (int i = 1; i < values.length; i += 2) {
            if (values[i] instanceof String text) {
                values[i] = new Ref(builder.createString(text));
            } else if (values[i] instanceof long[] longs) {
                builder.startVector(8, longs.length, 8);
                for (int j = longs.length - 1; j >= 0; j--) {
                    builder.addLong(longs[j]);
                }
                values[i] = new Ref(builder.endVector());
            }
        }
        builder.startTable(spec.fields.size());
        for (int i = 0; i < values.length; i += 2) {
            var field = spec.field((String) values[i]);
            int slot = (field.vtable() - 4) / 2;
            switch (values[i + 1]) {
                case Ref ref -> builder.addOffset(slot, ref.offset(), 0);
                case Union union -> {
                    builder.addOffset(slot, union.offset(), 0);
                    int typeSlot = (spec.field(field.name() + "_type").vtable() - 4) / 2;
                    builder.addByte(typeSlot, (byte) schema.unionCode(field, union.member()), 0);
                }
                case Boolean flag -> builder.addBoolean(slot, flag, false);
                case Long number -> {
                    switch (field.type()) {
                        case BaseType.Byte, BaseType.UByte -> builder.addByte(slot, number.byteValue(), 0);
                        case BaseType.Short, BaseType.UShort -> builder.addShort(slot, number.shortValue(), 0);
                        case BaseType.Int, BaseType.UInt -> builder.addInt(slot, number.intValue(), 0);
                        default -> builder.addLong(slot, number, 0);
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported test value " + values[i + 1]);
            }
        }
        return builder.endTable();
    }

    /** A {@code Record} holding a payload table built with {@link #table}. */
    int record(String payloadTable, int payload) {
        return table("Record", "payload", new Union(payloadTable, payload));
    }

    int header(String schemaHash) {
        return record("Header", table("Header", "schema_hash", schemaHash));
    }

    int header() {
        return header(schema.hash);
    }

    int end() {
        return record("End", table("End"));
    }

    /** Finishes a size-prefixed {@code Block} of records and starts a new one. */
    byte[] block(int... records) {
        builder.startVector(4, records.length, 4);
        for (int i = records.length - 1; i >= 0; i--) {
            builder.addOffset(records[i]);
        }
        int vector = builder.endVector();
        int block = table("Block", "records", new Ref(vector));
        builder.finishSizePrefixed(block, "CLV2");
        ByteBuffer buffer = builder.dataBuffer();
        byte[] bytes = Arrays.copyOfRange(buffer.array(), buffer.position(), buffer.limit());
        builder = newBuilder();
        return bytes;
    }

    private static FlatBufferBuilder newBuilder() {
        // Scalars are declared "= null", so values equal to a default must still be written.
        return new FlatBufferBuilder(1024).forceDefaults(true);
    }
}
