/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.flatbuffers.reflection.BaseType;
import com.google.flatbuffers.reflection.Schema;

/**
 * The clang-dumper AST wire schema, read from FlatBuffers binary reflection ({@code ast-wire.bfbs}) rather than
 * generated Java classes. Tables, fields and enums are looked up by their schema names.
 *
 * <p>
 * The {@code wireSchema} Gradle task fetches both resources for the dumper selected in {@code clang-dumper-release.tag}.
 */
public final class WireSchema {

    private static final WireSchema INSTANCE = load();

    /** Canonical schema hash that the producer writes in every stream header. */
    public final String hash;
    private final List<TableSpec> tables = new ArrayList<>();
    private final Map<String, TableSpec> tablesByName = new HashMap<>();
    private final List<EnumSpec> enums = new ArrayList<>();

    /** A table and its fields in declaration order, by schema name (without the namespace). */
    public static final class TableSpec {
        public final String name;
        public final List<FieldSpec> fields = new ArrayList<>();
        private final Map<String, FieldSpec> fieldsByName = new HashMap<>();

        private TableSpec(String name) {
            this.name = name;
        }

        public FieldSpec field(String fieldName) {
            var field = fieldsByName.get(fieldName);
            if (field == null) {
                throw new IllegalArgumentException("Wire table " + name + " has no field " + fieldName);
            }
            return field;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * One table field. {@code type} and {@code element} are {@link BaseType} codes; {@code index} is the enum,
     * union or table of the field or of its vector elements, or -1.
     */
    public record FieldSpec(TableSpec owner, String name, int vtable, int type, int element, int index,
            boolean required) {
        @Override
        public String toString() {
            return owner.name + "." + name;
        }
    }

    /** An enum or union. For unions, {@code members[code]} is the table index stored under that type code. */
    private record EnumSpec(List<String> values, int[] members) {
    }

    public static WireSchema get() {
        return INSTANCE;
    }

    public TableSpec table(String name) {
        var table = tablesByName.get(name);
        if (table == null) {
            throw new IllegalArgumentException("Wire schema has no table " + name);
        }
        return table;
    }

    TableSpec table(int index) {
        return tables.get(index);
    }

    /** Value names of the enum a field refers to, in ordinal order. */
    public List<String> enumValues(FieldSpec field) {
        return enums.get(field.index()).values();
    }

    /** The table that union {@code field} holds for a stored type code. */
    TableSpec unionMember(FieldSpec field, int code) {
        int[] members = enums.get(field.index()).members();
        if (code <= 0 || code >= members.length) {
            throw new IllegalArgumentException("Invalid type code " + code + " for wire union " + field);
        }
        return tables.get(members[code]);
    }

    /** The type code under which union {@code field} stores one of its member tables. */
    public int unionCode(FieldSpec field, String member) {
        int[] members = enums.get(field.index()).members();
        for (int code = 1; code < members.length; code++) {
            if (tables.get(members[code]).name.equals(member)) {
                return code;
            }
        }
        throw new IllegalArgumentException("Wire union " + field + " has no member " + member);
    }

    /** All tables a union field can hold, e.g. every node payload table. */
    public List<TableSpec> unionMembers(FieldSpec field) {
        int[] members = enums.get(field.index()).members();
        var result = new ArrayList<TableSpec>();
        for (int code = 1; code < members.length; code++) {
            result.add(tables.get(members[code]));
        }
        return result;
    }

    private WireSchema(ByteBuffer bytes, String hash) {
        this.hash = hash;
        var schema = Schema.getRootAsSchema(bytes);
        for (int i = 0; i < schema.objectsLength(); i++) {
            var table = new TableSpec(shortName(schema.objects(i).name()));
            tables.add(table);
            tablesByName.put(table.name, table);
        }
        for (int i = 0; i < schema.objectsLength(); i++) {
            var object = schema.objects(i);
            var table = tables.get(i);
            for (int f = 0; f < object.fieldsLength(); f++) {
                var field = object.fields(f);
                boolean wireOptional = false;
                for (int a = 0; a < field.attributesLength(); a++) {
                    wireOptional |= "wire_optional".equals(field.attributes(a).key());
                }
                boolean scalar = field.type().baseType() <= BaseType.Double;
                table.fields.add(new FieldSpec(table, field.name(), field.offset(), field.type().baseType(),
                        field.type().element(), field.type().index(),
                        scalar ? field.optional() && !wireOptional : field.required()));
            }
            // Reflection sorts fields by name; bindings follow declaration order.
            table.fields.sort(Comparator.comparingInt(FieldSpec::vtable));
            table.fields.forEach(field -> table.fieldsByName.put(field.name(), field));
        }
        for (int i = 0; i < schema.enumsLength(); i++) {
            var anEnum = schema.enums(i);
            var values = new ArrayList<String>();
            int[] members = new int[anEnum.valuesLength()];
            for (int v = 0; v < anEnum.valuesLength(); v++) {
                values.add(anEnum.values(v).name());
                members[v] = anEnum.isUnion() ? anEnum.values(v).unionType().index() : -1;
            }
            enums.add(new EnumSpec(values, members));
        }
    }

    private static String shortName(String qualified) {
        return qualified.substring(qualified.lastIndexOf('.') + 1);
    }

    private static WireSchema load() {
        try (InputStream schema = WireSchema.class.getResourceAsStream("ast-wire.bfbs");
                InputStream hash = WireSchema.class.getResourceAsStream("ast-wire.sha256")) {
            if (schema == null || hash == null) {
                throw new IllegalStateException("Missing AST wire schema resources");
            }
            return new WireSchema(ByteBuffer.wrap(schema.readAllBytes()),
                    new String(hash.readAllBytes(), StandardCharsets.US_ASCII).trim());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the AST wire schema", e);
        }
    }
}
