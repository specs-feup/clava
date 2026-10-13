/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;
import org.suikasoft.jOptions.storedefinition.StoreDefinitions;

import com.google.flatbuffers.reflection.BaseType;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.wire.WireReader.Tu;
import pt.up.fe.specs.clang.wire.WireSchema.FieldSpec;
import pt.up.fe.specs.clang.wire.WireSchema.TableSpec;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.data.GeneratedNodeFields;
import pt.up.fe.specs.clava.data.NodeDataStore;
import pt.up.fe.specs.clava.ast.attr.AlignedAttr;
import pt.up.fe.specs.clava.ast.attr.AlignedExprAttr;
import pt.up.fe.specs.clava.ast.attr.AlignedTypeAttr;
import pt.up.fe.specs.clava.ast.attr.enums.AlignedAttrKind;
import pt.up.fe.specs.clava.ast.decl.CXXConversionDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;

/**
 * Reads payloads using the schema and compile-time generated Java field descriptors. The descriptors preserve
 * the Java reference, optional and list types; conversions queue forward references into the node's shared
 * data view. Source information and aligned attributes have explicit representation adapters.
 */
final class NodeBindings {

    private static final Map<TableSpec, NodeBindings> BINDINGS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, StoreDefinition> STORE_DEFINITIONS = new ConcurrentHashMap<>();

    /** Writes one field into the node data, possibly by queueing a reference for later resolution. */
    private interface Setter {
        void set(WireTable table, DataStore store, Tu tu);
    }

    /** Reads one value from a table field. */
    private interface Reader {
        Object read(WireTable table, Tu tu);
    }

    /** Reads element {@code i} of a vector field. */
    private interface ElementReader {
        Object read(WireTable table, int i, Tu tu);
    }

    /** Setters for one table of the {@code base} chain, and the field leading to the next table. */
    private record Level(List<Setter> setters, FieldSpec base) {
    }

    /** The Clava class a payload table describes (possibly a superclass of the emitted node). */
    final Class<? extends ClavaNode> payloadClass;
    private final List<Level> levels = new ArrayList<>();

    static NodeBindings of(TableSpec payload) {
        return BINDINGS.computeIfAbsent(payload, NodeBindings::new);
    }

    private NodeBindings(TableSpec payload) {
        if (!payload.name.endsWith("Data")) {
            throw new IllegalArgumentException("Node payload table must be named <ClavaClass>Data: " + payload);
        }
        payloadClass = clavaClass(payload.name.substring(0, payload.name.length() - 4));
        var schema = WireSchema.get();
        for (TableSpec table = payload; table != null;) {
            var setters = new ArrayList<Setter>();
            FieldSpec base = null;
            for (var field : table.fields) {
                if (field.name().equals("base")) {
                    base = field;
                } else if (field.type() == BaseType.UType) {
                    // Type code of a union field, read together with the union.
                } else if (table.name.equals("ClavaNodeData") && field.name().equals("source")) {
                    setters.add(source(field));
                } else if (!table.name.equals("AlignedAttrData")) {
                    setters.add(setter(field));
                }
            }
            if (table.name.equals("AlignedAttrData")) {
                setters.add(NodeBindings::alignedAttr);
            } else if (table.name.equals("CXXConversionDeclData")) {
                setters.add((t, store, tu) -> tu.nodes().queueSetAction(store, NamedDecl.DECL_NAME,
                        CXXConversionDecl::buildDeclName));
            }
            levels.add(new Level(setters, base));
            table = base == null ? null : schema.table(base.index());
        }
    }

    /** Builds the data of node {@code id}, whose emitted class is {@code nodeClass}. */
    @SuppressWarnings("unchecked")
    DataStore read(WireTable payload, Class<? extends ClavaNode> nodeClass, String id, Tu tu) {
        var store = NodeDataStore.adapt(DataStore.newInstance(STORE_DEFINITIONS.computeIfAbsent(nodeClass,
                aClass -> StoreDefinitions.fromInterface((Class<ClavaNode>) aClass)), true));
        store.set(ClavaNode.CONTEXT, tu.data().get(ClangAstData.CONTEXT));
        store.set(ClavaNode.ID, id);
        store.set(ClavaNode.IS_MACRO, false);
        store.set(ClavaNode.IS_IN_SYSTEM_HEADER, false);
        WireTable table = payload;
        for (var level : levels) {
            for (var setter : level.setters) {
                setter.set(table, store, tu);
            }
            table = level.base == null ? null : table.table(level.base);
        }
        return store;
    }

    /** Payload tables may describe abstract classes, which the node-name mapping of ClassesService skips. */
    private static Class<? extends ClavaNode> clavaClass(String simpleName) {
        for (String folder : List.of("decl", "type", "expr", "stmt", "attr", "extra")) {
            try {
                return Class.forName("pt.up.fe.specs.clava.ast." + folder + "." + simpleName)
                        .asSubclass(ClavaNode.class);
            } catch (ClassNotFoundException e) {
                // Try the next AST folder.
            }
        }
        throw new IllegalStateException("No Clava node class named " + simpleName);
    }

    private static Setter source(FieldSpec field) {
        var info = WireSchema.get().table("SourceInfo");
        var expansion = info.field("expansion");
        var isMacro = info.field("is_macro");
        var inSystemHeader = info.field("is_in_system_header");
        return (table, store, tu) -> {
            var source = table.table(field);
            var range = source.table(expansion);
            if (range != null) {
                store.set(ClavaNode.LOCATION, tu.range(range));
            }
            store.set(ClavaNode.IS_MACRO, source.bool(isMacro));
            store.set(ClavaNode.IS_IN_SYSTEM_HEADER, source.bool(inSystemHeader));
        };
    }

    /** {@code alignment} is a type or an expression reference, depending on {@code is_expression}. */
    private static void alignedAttr(WireTable table, DataStore store, Tu tu) {
        String alignment = tu.id(table.integer("alignment"));
        if (table.bool("is_expression")) {
            store.set(AlignedAttr.ALIGNED_ATTR_KIND, AlignedAttrKind.EXPR);
            tu.nodes().queueSetOptionalNode(store, AlignedExprAttr.EXPR, alignment);
        } else {
            store.set(AlignedAttr.ALIGNED_ATTR_KIND, AlignedAttrKind.TYPE);
            tu.nodes().queueSetNode(store, AlignedTypeAttr.TYPE, alignment);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Setter setter(FieldSpec field) {
        var binding = GeneratedNodeFields.binding(field.owner().name, field.name());
        DataKey key = binding.key();
        Class<?> raw = binding.valueType();
        if (ClavaNode.class.isAssignableFrom(raw)) {
            requireType(field, BaseType.Long);
            if (binding.nullable()) {
                return (t, store, tu) -> tu.nodes().queueSetNullableNode(store, key, tu.id(t.integer(field)));
            }
            return (t, store, tu) -> tu.nodes().queueSetNode(store, key, tu.id(t.integer(field)));
        }
        if (raw == Optional.class) {
            Class<?> element = binding.elementType();
            if (ClavaNode.class.isAssignableFrom(element)) {
                requireType(field, BaseType.Long);
                return (t, store, tu) -> tu.nodes().queueSetOptionalNode(store, key, tu.id(t.integer(field)));
            }
            var reader = reader(field, element);
            if (field.type() == BaseType.String) {
                // Required strings are always present; an empty spelling means "no value".
                return (t, store, tu) -> store.set(key,
                        Optional.ofNullable((String) reader.read(t, tu)).filter(value -> !value.isEmpty()));
            }
            return (t, store, tu) -> store.set(key,
                    t.has(field) ? Optional.of(reader.read(t, tu)) : Optional.empty());
        }
        if (List.class.isAssignableFrom(raw)) {
            requireType(field, BaseType.Vector);
            Class<?> element = binding.elementType();
            if (ClavaNode.class.isAssignableFrom(element)) {
                return (t, store, tu) -> {
                    var ids = new ArrayList<String>(t.length(field));
                    for (int i = 0; i < t.length(field); i++) {
                        ids.add(tu.id(t.integerAt(field, i)));
                    }
                    tu.nodes().queueSetNodeList(store, key, ids);
                };
            }
            var reader = elementReader(field, element);
            return (t, store, tu) -> {
                int length = t.length(field);
                var values = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    values.add(reader.read(t, i, tu));
                }
                store.set(key, values);
            };
        }
        var reader = reader(field, raw);
        return (t, store, tu) -> store.set(key, reader.read(t, tu));
    }

    private static Reader reader(FieldSpec field, Class<?> type) {
        if (field.type() == BaseType.Obj) {
            return (t, tu) -> Compounds.read(t.table(field), tu);
        }
        if (type.isEnum()) {
            var constants = enumConstants(field, type);
            return (t, tu) -> constants[(int) t.integer(field)];
        }
        return switch (type.getSimpleName()) {
            case "Boolean" -> (t, tu) -> t.bool(field);
            case "String" -> (t, tu) -> t.string(field);
            case "BigInteger" -> (t, tu) -> new BigInteger(t.string(field));
            case "Byte" -> (t, tu) -> (byte) t.integer(field);
            case "Short" -> (t, tu) -> (short) t.integer(field);
            case "Integer" -> (t, tu) -> Math.toIntExact(t.integer(field));
            case "Long" -> (t, tu) -> t.integer(field);
            case "Float" -> (t, tu) -> (float) t.real(field);
            case "Double" -> (t, tu) -> t.real(field);
            default -> throw bindError(field, "cannot fill a DataKey of type " + type.getName());
        };
    }

    private static ElementReader elementReader(FieldSpec field, Class<?> type) {
        if (field.element() == BaseType.Obj) {
            return (t, i, tu) -> Compounds.read(t.tableAt(field, i), tu);
        }
        if (type.isEnum()) {
            var constants = enumConstants(field, type);
            return (t, i, tu) -> constants[(int) t.integerAt(field, i)];
        }
        return switch (type.getSimpleName()) {
            case "Boolean" -> (t, i, tu) -> t.integerAt(field, i) != 0;
            case "String" -> (t, i, tu) -> t.stringAt(field, i);
            case "Byte" -> (t, i, tu) -> (byte) t.integerAt(field, i);
            case "Integer" -> (t, i, tu) -> Math.toIntExact(t.integerAt(field, i));
            case "Long" -> (t, i, tu) -> t.integerAt(field, i);
            default -> throw bindError(field, "cannot fill a list of " + type.getName());
        };
    }

    /** Wire enums are stored by ordinal, so their value names must match the Java constants exactly. */
    private static Object[] enumConstants(FieldSpec field, Class<?> type) {
        var constants = type.getEnumConstants();
        var names = new ArrayList<String>();
        for (var constant : constants) {
            names.add(((Enum<?>) constant).name());
        }
        if (!names.equals(WireSchema.get().enumValues(field))) {
            throw bindError(field, "enum values differ from " + type.getName() + ": "
                    + WireSchema.get().enumValues(field) + " vs " + names);
        }
        return constants;
    }

    private static void requireType(FieldSpec field, int type) {
        if (field.type() != type) {
            throw bindError(field, "must be a " + BaseType.name(type) + " field");
        }
    }

    private static IllegalStateException bindError(FieldSpec field, String message) {
        return new IllegalStateException("Wire field " + field + " " + message);
    }
}
