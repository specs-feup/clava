/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
import com.google.flatbuffers.reflection.BaseType;
import com.google.flatbuffers.reflection.Schema;
import com.google.gson.Gson;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Generates abstract Clava data bases using upstream FlatBuffers reflection, without flatc or source parsing. */
public final class GenerateNodeData {
    record Key(String name, String type, String initializer, boolean nullable, String description, String wireField) {}
    record Node(String table, String className, String packageName, String parent, List<String> imports,
                List<Key> keys) {
        String qualified() { return packageName + "." + className; }
    }
    private final Schema schema;
    private final List<Node> nodes;
    private final Map<String, Node> byTable = new HashMap<>();
    private final Map<String, Node> byClass = new HashMap<>();

    private GenerateNodeData(Path schemaFile, Path contract) throws Exception {
        schema = Schema.getRootAsSchema(ByteBuffer.wrap(Files.readAllBytes(schemaFile)));
        nodes = Arrays.asList(new Gson().fromJson(Files.readString(contract), Node[].class));
        for (var node : nodes) {
            if (byTable.put(node.table(), node) != null || byClass.put(node.className(), node) != null) {
                throw new IllegalArgumentException("Duplicate data contract for " + node.className());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        var generator = new GenerateNodeData(Path.of(args[1]), Path.of(args[2]));
        switch (args[0]) {
            case "generate" -> generator.generate(Path.of(args[3]));
            case "update" -> Files.writeString(Path.of(args[3]), generator.fieldInventory());
            case "verify", "warn" -> generator.verify(Path.of(args[3]), args[0].equals("verify"));
            default -> throw new IllegalArgumentException("Unknown mode " + args[0]);
        }
    }

    /** Full field signatures and enum values make the lock catch semantic changes, not only renames. */
    private String fieldInventory() {
        var lines = new TreeSet<String>();
        for (int i = 0; i < schema.objectsLength(); i++) {
            var object = schema.objects(i);
            lines.add("table " + object.name() + " struct=" + object.isStruct());
            for (int j = 0; j < object.fieldsLength(); j++) {
                var field = object.fields(j);
                var attributes = new TreeSet<String>();
                for (int a = 0; a < field.attributesLength(); a++) {
                    attributes.add(field.attributes(a).key() + "=" + field.attributes(a).value());
                }
                lines.add(object.name() + "." + field.name() + " " + type(field.type())
                        + " slot=" + field.id() + " offset=" + field.offset()
                        + " optional=" + field.optional() + " required=" + field.required()
                        + " deprecated=" + field.deprecated() + " default=" + field.defaultInteger()
                        + "/" + field.defaultReal() + " attributes=" + attributes);
            }
        }
        for (int i = 0; i < schema.enumsLength(); i++) {
            var anEnum = schema.enums(i);
            lines.add("enum " + anEnum.name() + " " + type(anEnum.underlyingType()) + " union=" + anEnum.isUnion());
            for (int j = 0; j < anEnum.valuesLength(); j++) {
                var value = anEnum.values(j);
                lines.add(anEnum.name() + "." + value.name() + "=" + value.value()
                        + (anEnum.isUnion() && value.unionType().index() >= 0
                                ? " " + schema.objects(value.unionType().index()).name() : ""));
            }
        }
        return String.join("\n", lines) + "\n";
    }

    private String type(com.google.flatbuffers.reflection.Type type) {
        int base = type.baseType();
        int element = type.element();
        var name = BaseType.name(base);
        if (base == BaseType.Vector || base == BaseType.Array) name += "<" + BaseType.name(element) + ">";
        if (type.index() >= 0) {
            name += ":" + (base == BaseType.Obj || element == BaseType.Obj
                    ? schema.objects(type.index()).name() : schema.enums(type.index()).name());
        }
        return name;
    }

    private void verify(Path lock, boolean fail) throws Exception {
        var actual = new TreeSet<>(fieldInventory().lines().toList());
        var reviewed = Files.exists(lock) ? new TreeSet<>(Files.readAllLines(lock)) : new TreeSet<String>();
        if (actual.equals(reviewed)) return;
        var added = new TreeSet<>(actual); added.removeAll(reviewed);
        var removed = new TreeSet<>(reviewed); removed.removeAll(actual);
        var message = "Unreviewed AST wire schema changes in " + lock + "\n"
                + String.join("\n", added.stream().map(line -> "+ " + line).toList()) + "\n"
                + String.join("\n", removed.stream().map(line -> "- " + line).toList())
                + "\nReview the consumer behavior, then run updateWireFields and commit the lock.";
        if (fail) throw new IllegalStateException(message);
        System.err.println("WARNING: " + message);
    }

    private void generate(Path output) throws Exception {
        // A successful regeneration removes obsolete bases, including renamed schema tables.
        if (Files.exists(output)) {
            try (var files = Files.walk(output)) {
                for (var file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
        var binding = new StringBuilder("/* Generated. Do not edit. */\npackage pt.up.fe.specs.clava.data;\n");
        // Different AST families use the same short names. Emit each binding in its own package instead.
        binding.append("public final class GeneratedNodeFields {\n")
                .append(" public record Binding(org.suikasoft.jOptions.Datakey.DataKey<?> key, Class<?> valueType, Class<?> elementType, boolean nullable) {}\n")
                .append(" public static Binding binding(String table, String field) { return switch(table) {\n");
        for (var node : nodes) {
            var object = schema.objectsByKey("astwire.v2." + node.table());
            if (object == null) throw new IllegalStateException("Data contract has no schema table " + node.table());
            for (var key : node.keys()) {
                if (key.wireField() != null && object.fieldsByKey(key.wireField()) == null) {
                    throw new IllegalStateException("Obsolete Java field policy for " + node.table() + "." + key.wireField());
                }
            }
            var source = new StringBuilder("/* Generated from ast-wire.bfbs and node-data.json. Do not edit. */\n")
                    .append("package ").append(node.packageName()).append(";\n");
            var nodeImports = new TreeSet<>(node.imports());
            nodeImports.add("import pt.up.fe.specs.clava.data.NodeDataStore;");
            nodeImports.add("import pt.up.fe.specs.clava.data.GeneratedNodeFields.Binding;");
            source.append(String.join("\n", nodeImports)).append("\n\n")
                    .append("public abstract class ").append(node.className()).append("Base extends ")
                    .append(node.parent()).append(" {\n");
            var wireBase = object.fieldsByKey("base");
            if (wireBase != null) {
                var baseTable = schema.objects(wireBase.type().index());
                // Empty structural tables add no data. Clava represents expressions outside the Stmt hierarchy.
                while (baseTable.fieldsLength() == 1 && baseTable.fieldsByKey("base") != null) {
                    baseTable = schema.objects(baseTable.fieldsByKey("base").type().index());
                }
                var baseName = shortName(baseTable.name()).replaceFirst("Data$", "");
                var base = byClass.get(baseName);
                String qualifiedBase = baseName.equals("ClavaNode") ? "pt.up.fe.specs.clava.ClavaNode"
                        : base == null ? node.packageName() + "." + baseName : base.qualified();
                source.append(" private static final Class<? super ").append(node.className())
                        .append("> WIRE_PARENT = ").append(qualifiedBase).append(".class;\n");
            }
            for (var key : node.keys()) {
                if (key.description() != null) source.append(" /** ").append(key.description().replace("*/", "* /").replace("\n", "\n  * ")).append(" */\n");
                if (key.description() != null && key.description().contains("@deprecated")) source.append(" @Deprecated\n");
                if (key.nullable()) source.append(" @pt.up.fe.specs.clava.NullableNodeReference\n");
                source.append(" public static final DataKey<").append(key.type()).append("> ")
                        .append(key.name()).append(" = ").append(key.initializer()).append(";\n");
            }
            source.append(" public record Values(")
                    .append(String.join(", ", node.keys().stream().map(key -> key.type() + " " + member(key)).toList()))
                    .append(") {}\n");
            source.append(" public static class WireFields extends ").append(node.parent()).append(".WireFields {\n")
                    .append("  public static final int FIELD_COUNT = ").append(node.parent()).append(".WireFields.FIELD_COUNT + ").append(node.keys().size()).append(";\n");
            for (var key : node.keys()) source.append("  ").append(key.type()).append(" ").append(member(key)).append(";\n");
            source.append("  public Object read(int index) { return switch(index) {\n");
            for (int i = 0; i < node.keys().size(); i++) source.append("   case ").append(node.parent()).append(".WireFields.FIELD_COUNT + ").append(i)
                    .append(" -> this.").append(member(node.keys().get(i))).append(";\n");
            source.append("   default -> super.read(index);\n  }; }\n")
                    .append("  @SuppressWarnings(\"unchecked\") public void write(int index, Object value) { switch(index) {\n");
            for (int i = 0; i < node.keys().size(); i++) source.append("   case ").append(node.parent()).append(".WireFields.FIELD_COUNT + ").append(i).append(" -> this.")
                    .append(member(node.keys().get(i))).append(" = (").append(node.keys().get(i).type()).append(") value;\n");
            source.append("   default -> { super.write(index, value); return; }\n  }\n  mark(index, value);\n }\n")
                    .append(" }\n public static final NodeDataStore.Layout<WireFields> WIRE_LAYOUT = new NodeDataStore.Layout<>(WireFields.class, WireFields::new, java.util.List.of(")
                    .append(String.join(", ", node.keys().stream().map(Key::name).toList())).append("), ").append(node.parent()).append(".WireFields.FIELD_COUNT);\n")
                    .append(" private final WireFields fields;\n")
                    .append(" protected ").append(node.className()).append("Base(DataStore data, java.util.Collection<? extends pt.up.fe.specs.clava.ClavaNode> children) {\n")
                    .append("  super(data, children);\n  var store = NodeDataStore.adapt(getData());\n")
                    .append("  fields = store.state(WireFields.class);\n");
            source.append(" }\n")
                    .append(" protected ").append(node.className()).append("Base(DataStore data, java.util.Collection<? extends pt.up.fe.specs.clava.ClavaNode> children, Values values) {\n")
                    .append("  this(data, children);\n  setWireValues(values);\n }\n")
                    .append(" public final ").append(node.className()).append(" setWireValues(Values values) {\n")
                    .append("  java.util.Objects.requireNonNull(values);\n");
            for (var key : node.keys()) source.append("  setWire").append(accessor(key)).append("(values.")
                    .append(member(key)).append("());\n");
            source.append("  return (").append(node.className()).append(") this;\n }\n");
            for (var key : node.keys()) {
                var method = accessor(key);
                source.append(" public final ").append(key.type()).append(" getWire").append(method)
                        .append("() { return getNodeStorage().readGenerated(").append(key.name()).append(", fields.").append(member(key)).append("); }\n")
                        .append(" public final ").append(node.className()).append(" setWire").append(method)
                        .append("(").append(key.type()).append(" value) { return (").append(node.className())
                        .append(") set(").append(key.name()).append(", value); }\n");
            }
            source.append(" public static Binding wireBinding(String field) { return switch(field) {\n");
            for (int i = 0; i < object.fieldsLength(); i++) {
                var field = object.fields(i);
                if (field.name().equals("base") || field.type().baseType() == BaseType.UType) continue;
                // AlignedAttr splits one wire reference into a type or expression and synthesizes its kind.
                if (node.table().equals("AlignedAttrData") && !field.name().equals("spelling")) continue;
                var key = node.keys().stream().filter(k -> field.name().equals(k.wireField())).findFirst()
                        .orElseThrow(() -> new IllegalStateException("No Java field policy for " + node.table() + "." + field.name()));
                var raw = key.type().split("<", 2)[0].trim();
                String element = key.type().contains("<") ? key.type().substring(key.type().indexOf('<') + 1, key.type().lastIndexOf('>')).trim() : null;
                if (element != null && element.contains("<")) throw new IllegalStateException("Unsupported nested field type " + key.type());
                source.append("  case \"").append(field.name()).append("\" -> new Binding(").append(key.name())
                        .append(", ").append(raw).append(".class, ").append(element == null ? "null" : element + ".class")
                        .append(", ").append(key.nullable()).append(");\n");
            }
            source.append("  default -> throw new IllegalArgumentException(\"Unknown wire field ").append(node.table())
                    .append(".\" + field);\n }; }\n}\n");
            write(output, node.packageName() + "." + node.className() + "Base", source.toString());
            binding.append("  case \"").append(node.table()).append("\" -> ").append(node.qualified()).append("Base.wireBinding(field);\n");
        }
        binding.append("  default -> throw new IllegalArgumentException(\"No generated data binding for \" + table);\n }; }\n}\n");
        write(output, "pt.up.fe.specs.clava.data.GeneratedNodeFields", binding.toString());
    }

    private static String accessor(Key key) {
        var method = camel(key.name().toLowerCase());
        return Character.toUpperCase(method.charAt(0)) + method.substring(1);
    }

    private static void write(Path output, String qualified, String source) throws Exception {
        var path = output.resolve(qualified.replace('.', '/') + ".java");
        Files.createDirectories(path.getParent());
        Files.writeString(path, source);
    }
    private static String shortName(String name) { return name.substring(name.lastIndexOf('.') + 1); }
    private static String member(Key key) { return camel(key.name().toLowerCase()); }
    private static String camel(String value) {
        var out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            out.append(c == '_' ? Character.toUpperCase(value.charAt(++i)) : c);
        }
        return out.toString();
    }
}
