/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import java.nio.ByteBuffer;

import com.google.flatbuffers.Table;
import com.google.flatbuffers.reflection.BaseType;

import pt.up.fe.specs.clang.wire.WireSchema.FieldSpec;
import pt.up.fe.specs.clang.wire.WireSchema.TableSpec;

/**
 * A FlatBuffers table read through its {@link TableSpec}. Fields are addressed by spec (hot paths) or by schema
 * name. Reading an absent field that the schema requires fails with the field name.
 */
public final class WireTable extends Table {

    public final TableSpec spec;

    WireTable(ByteBuffer buffer, int position, TableSpec spec) {
        __reset(position, buffer);
        this.spec = spec;
    }

    public boolean has(FieldSpec field) {
        return __offset(field.vtable()) != 0;
    }

    public boolean has(String field) {
        return has(spec.field(field));
    }

    /** Any integer, enum or boolean scalar, widened to long. */
    public long integer(FieldSpec field) {
        int offset = present(field);
        return offset == 0 ? 0 : scalar(bb_pos + offset, field.type());
    }

    public long integer(String field) {
        return integer(spec.field(field));
    }

    public boolean bool(FieldSpec field) {
        return integer(field) != 0;
    }

    public boolean bool(String field) {
        return bool(spec.field(field));
    }

    public double real(FieldSpec field) {
        int offset = present(field);
        if (offset == 0) {
            return 0;
        }
        return field.type() == BaseType.Float ? bb.getFloat(bb_pos + offset) : bb.getDouble(bb_pos + offset);
    }

    public String string(FieldSpec field) {
        int offset = present(field);
        return offset == 0 ? null : __string(bb_pos + offset);
    }

    public String string(String field) {
        return string(spec.field(field));
    }

    /** A nested table, or null when the field is absent. */
    public WireTable table(FieldSpec field) {
        int offset = present(field);
        return offset == 0 ? null
                : new WireTable(bb, __indirect(bb_pos + offset), WireSchema.get().table(field.index()));
    }

    public WireTable table(String field) {
        return table(spec.field(field));
    }

    /** The member table of a union field. */
    public WireTable union(String field) {
        var union = spec.field(field);
        int offset = present(union);
        int code = (int) integer(spec.field(field + "_type"));
        return new WireTable(bb, __indirect(bb_pos + offset), WireSchema.get().unionMember(union, code));
    }

    public int length(FieldSpec field) {
        int offset = present(field);
        return offset == 0 ? 0 : __vector_len(offset);
    }

    public int length(String field) {
        return length(spec.field(field));
    }

    public long integerAt(FieldSpec field, int index) {
        int size = scalarSize(field.element());
        return scalar(__vector(__offset(field.vtable())) + index * size, field.element());
    }

    public long integerAt(String field, int index) {
        return integerAt(spec.field(field), index);
    }

    public String stringAt(FieldSpec field, int index) {
        return __string(__vector(__offset(field.vtable())) + index * 4);
    }

    public WireTable tableAt(FieldSpec field, int index) {
        int element = __vector(__offset(field.vtable())) + index * 4;
        return new WireTable(bb, __indirect(element), WireSchema.get().table(field.index()));
    }

    public WireTable tableAt(String field, int index) {
        return tableAt(spec.field(field), index);
    }

    private int present(FieldSpec field) {
        int offset = __offset(field.vtable());
        if (offset == 0 && field.required()) {
            throw new IllegalArgumentException("Missing wire field " + field);
        }
        return offset;
    }

    private long scalar(int position, int type) {
        return switch (type) {
            case BaseType.Bool, BaseType.UByte, BaseType.UType -> bb.get(position) & 0xFF;
            case BaseType.Byte -> bb.get(position);
            case BaseType.Short -> bb.getShort(position);
            case BaseType.UShort -> bb.getShort(position) & 0xFFFF;
            case BaseType.Int -> bb.getInt(position);
            case BaseType.UInt -> bb.getInt(position) & 0xFFFFFFFFL;
            case BaseType.Long, BaseType.ULong -> bb.getLong(position);
            default -> throw new IllegalArgumentException("Not an integer wire type: " + BaseType.name(type));
        };
    }

    private static int scalarSize(int type) {
        return switch (type) {
            case BaseType.Bool, BaseType.Byte, BaseType.UByte, BaseType.UType -> 1;
            case BaseType.Short, BaseType.UShort -> 2;
            case BaseType.Int, BaseType.UInt, BaseType.Float -> 4;
            default -> 8;
        };
    }
}
