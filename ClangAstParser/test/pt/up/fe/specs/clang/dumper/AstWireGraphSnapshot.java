package pt.up.fe.specs.clang.dumper;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.suikasoft.jOptions.Datakey.DataKey;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.SourceLocation;
import pt.up.fe.specs.clava.SourceRange;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** Canonical graph serializer used only by the isolated dual-wire fidelity test. */
final class AstWireGraphSnapshot {

    private static final Set<String> EXCLUDED_DATA_KEYS = Set.of(
            ClavaNode.ID.getName(),
            ClavaNode.PREVIOUS_ID.getName(),
            ClavaNode.CONTEXT.getName(),
            ClavaNode.ORIGIN.getName());

    private AstWireGraphSnapshot() {
    }

    static void write(ClavaNode root, Path fixture, Path output) throws Exception {
        var serializer = new Serializer(root, fixture.toRealPath().toString());
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("schema", "clava-ast-graph-v1");
        snapshot.addProperty("fixture", "$FIXTURE");
        snapshot.add("graph", serializer.graph());
        snapshot.add("excluded_fields", excludedFields());
        FilesSupport.write(output, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .toJson(snapshot));
    }

    private static JsonArray excludedFields() {
        JsonArray values = new JsonArray();
        values.add("ClavaNode.ID: wire-local identifier replaced by graph references");
        values.add("ClavaNode.PREVIOUS_ID: historical identifier");
        values.add("ClavaNode.CONTEXT: process-owned parser context");
        values.add("ClavaNode.ORIGIN: edit-provenance reference");
        values.add("object identity and JVM identity hash codes: never serialized");
        values.add("timings and transport metrics: not part of the AST graph");
        return values;
    }

    private static final class Serializer {
        private final List<ClavaNode> nodes = new ArrayList<>();
        private final IdentityHashMap<ClavaNode, Integer> indexes = new IdentityHashMap<>();
        private final Map<String, Integer> clavaIds = new TreeMap<>();
        private final String fixturePath;

        private Serializer(ClavaNode root, String fixturePath) {
            this.fixturePath = fixturePath;
            addNode(root);
            discoverGraph();
            for (int index = 0; index < nodes.size(); index++) {
                clavaIds.putIfAbsent(nodes.get(index).getId(), index);
            }
        }

        private JsonObject graph() {
            JsonObject graph = new JsonObject();
            graph.addProperty("root", 0);
            JsonArray serializedNodes = new JsonArray();
            for (int index = 0; index < nodes.size(); index++) {
                serializedNodes.add(serializeNode(index, nodes.get(index)));
            }
            graph.add("nodes", serializedNodes);
            return graph;
        }

        private void discoverGraph() {
            for (int index = 0; index < nodes.size(); index++) {
                ClavaNode node = nodes.get(index);
                node.getChildren().forEach(this::addNode);
                for (DataKey<?> key : orderedKeys(node)) {
                    if (isExcluded(key)) {
                        continue;
                    }
                    collectReferences(read(node, key), new IdentityHashMap<>());
                }
            }
        }

        private void collectReferences(Object value, IdentityHashMap<Object, Boolean> visited) {
            if (value == null || isScalar(value) || value instanceof File || value instanceof Path
                    || value instanceof SourceLocation || value instanceof SourceRange || value instanceof Include) {
                return;
            }
            if (value instanceof ClavaNode node) {
                addNode(node);
                return;
            }
            if (visited.put(value, Boolean.TRUE) != null) {
                return;
            }
            if (value instanceof Optional<?> optional) {
                optional.ifPresent(element -> collectReferences(element, visited));
                return;
            }
            if (value instanceof Map<?, ?> map) {
                map.entrySet().stream().sorted(Comparator.comparing(entry -> stableSortKey(entry.getKey())))
                        .forEach(entry -> {
                            collectReferences(entry.getKey(), visited);
                            collectReferences(entry.getValue(), visited);
                        });
                return;
            }
            if (value instanceof Set<?> set) {
                set.stream().sorted(Comparator.comparing(this::stableSortKey))
                        .forEach(element -> collectReferences(element, visited));
                return;
            }
            if (value instanceof Iterable<?> iterable) {
                iterable.forEach(element -> collectReferences(element, visited));
                return;
            }
            if (value.getClass().isArray()) {
                for (int index = 0; index < Array.getLength(value); index++) {
                    collectReferences(Array.get(value, index), visited);
                }
                return;
            }
            for (Field field : objectFields(value.getClass())) {
                try {
                    field.setAccessible(true);
                    collectReferences(field.get(value), visited);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Opaque JDK fields are serialized by class name and text below.
                }
            }
        }

        private JsonObject serializeNode(int index, ClavaNode node) {
            JsonObject result = new JsonObject();
            result.addProperty("index", index);
            result.addProperty("class", node.getClass().getName());
            result.addProperty("data_class", node.getDataClassName());

            JsonArray children = new JsonArray();
            for (ClavaNode child : node.getChildren()) {
                children.add(new JsonPrimitive(indexes.get(child)));
            }
            result.add("children", children);

            JsonObject fields = new JsonObject();
            for (DataKey<?> key : orderedKeys(node)) {
                if (isExcluded(key)) {
                    continue;
                }
                fields.add(key.getName(), encode(read(node, key), new IdentityHashMap<>()));
            }
            result.add("fields", fields);
            return result;
        }

        private JsonElement encode(Object value, IdentityHashMap<Object, Boolean> active) {
            if (value == null) {
                return JsonNull.INSTANCE;
            }
            if (value instanceof ClavaNode node) {
                Integer index = indexes.get(node);
                if (index == null) {
                    return opaque(node);
                }
                JsonObject reference = new JsonObject();
                reference.addProperty("$ref", index);
                return reference;
            }
            if (isScalar(value)) {
                return scalar(value);
            }
            if (value instanceof File file) {
                return new JsonPrimitive(normalizePath(file.toPath()));
            }
            if (value instanceof Path path) {
                return new JsonPrimitive(normalizePath(path));
            }
            if (value instanceof SourceLocation location) {
                JsonObject result = new JsonObject();
                result.addProperty("file", normalizePath(location.getFilepath()));
                result.addProperty("line", location.getLine());
                result.addProperty("column", location.getColumn());
                result.addProperty("macro", location.isMacro());
                return result;
            }
            if (value instanceof SourceRange range) {
                JsonObject result = new JsonObject();
                result.add("start", encode(range.getStart(), active));
                result.add("end", encode(range.getEnd(), active));
                result.addProperty("text", range.toString());
                return result;
            }
            if (value instanceof Include include) {
                JsonObject result = new JsonObject();
                result.addProperty("source", include.getSourceFile() == null
                        ? null : normalizePath(include.getSourceFile().toPath()));
                result.addProperty("include", include.getInclude());
                result.addProperty("line", include.getLine());
                result.addProperty("angled", include.isAngled());
                return result;
            }
            if (value instanceof Optional<?> optional) {
                return optional.map(element -> encode(element, active)).orElse(JsonNull.INSTANCE);
            }
            if (active.put(value, Boolean.TRUE) != null) {
                JsonObject cycle = new JsonObject();
                cycle.addProperty("$cycle", value.getClass().getName());
                return cycle;
            }
            try {
                if (value instanceof Map<?, ?> map) {
                    List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
                    entries.sort(Comparator.comparing(entry -> stableSortKey(entry.getKey())));
                    JsonArray result = new JsonArray();
                    for (Map.Entry<?, ?> entry : entries) {
                        JsonArray pair = new JsonArray();
                        pair.add(encode(entry.getKey(), active));
                        pair.add(encode(entry.getValue(), active));
                        result.add(pair);
                    }
                    return result;
                }
                if (value instanceof Set<?> set) {
                    List<?> elements = set.stream().sorted(Comparator.comparing(this::stableSortKey)).toList();
                    return encodeIterable(elements, active);
                }
                if (value instanceof Iterable<?> iterable) {
                    List<Object> elements = new ArrayList<>();
                    iterable.forEach(elements::add);
                    return encodeIterable(elements, active);
                }
                if (value.getClass().isArray()) {
                    JsonArray result = new JsonArray();
                    for (int index = 0; index < Array.getLength(value); index++) {
                        result.add(encode(Array.get(value, index), active));
                    }
                    return result;
                }
                return encodeObject(value, active);
            } finally {
                active.remove(value);
            }
        }

        private JsonArray encodeIterable(Iterable<?> iterable, IdentityHashMap<Object, Boolean> active) {
            JsonArray result = new JsonArray();
            for (Object element : iterable) {
                result.add(encode(element, active));
            }
            return result;
        }

        private JsonElement encodeObject(Object value, IdentityHashMap<Object, Boolean> active) {
            List<Field> fields = objectFields(value.getClass());
            if (fields.isEmpty()) {
                return opaque(value);
            }
            JsonObject result = new JsonObject();
            result.addProperty("$class", value.getClass().getName());
            JsonObject fieldValues = new JsonObject();
            boolean serialized = false;
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    fieldValues.add(field.getName(), encode(field.get(value), active));
                    serialized = true;
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Preserve accessible fields without making module internals fatal.
                }
            }
            if (!serialized) {
                return opaque(value);
            }
            result.add("fields", fieldValues);
            return result;
        }

        private JsonElement opaque(Object value) {
            JsonObject result = new JsonObject();
            result.addProperty("$opaque_class", value.getClass().getName());
            String text = String.valueOf(value);
            if (!text.matches(".*@[0-9a-fA-F]+$")) {
                result.addProperty("text", text);
            }
            return result;
        }

        private boolean isScalar(Object value) {
            return value instanceof String || value instanceof Number || value instanceof Boolean
                    || value instanceof Character || value instanceof Enum<?>;
        }

        private JsonElement scalar(Object value) {
            if (value instanceof Enum<?> enumeration) {
                return new JsonPrimitive(enumeration.getDeclaringClass().getName() + ":" + enumeration.name());
            }
            if (value instanceof Character character) {
                return new JsonPrimitive(character.toString());
            }
            if (value instanceof Number number) {
                return new JsonPrimitive(number);
            }
            if (value instanceof Boolean bool) {
                return new JsonPrimitive(bool);
            }
            String text = String.valueOf(value);
            Integer referenced = clavaIds.get(text);
            if (referenced != null) {
                JsonObject reference = new JsonObject();
                reference.addProperty("$ref", referenced);
                return reference;
            }
            return new JsonPrimitive(text);
        }

        private String normalizePath(Path path) {
            return normalizePath(path.toAbsolutePath().normalize().toString());
        }

        private String normalizePath(String path) {
            if (path == null) {
                return null;
            }
            try {
                if (Path.of(path).toAbsolutePath().normalize().toString().equals(fixturePath)) {
                    return "$FIXTURE";
                }
            } catch (RuntimeException ignored) {
                // Source ranges can use non-filesystem names such as <built-in>.
            }
            return path.replace('\\', '/');
        }

        private String stableSortKey(Object value) {
            if (value == null) {
                return "0:null";
            }
            if (value instanceof ClavaNode node) {
                return "node:" + node.getClass().getName() + ":" + node.getDataClassName()
                        + ":" + node.get(ClavaNode.LOCATION);
            }
            return value.getClass().getName() + ":" + String.valueOf(value);
        }

        private List<DataKey<?>> orderedKeys(ClavaNode node) {
            return node.getDataKeysWithValues().stream()
                    .sorted(Comparator.comparing(DataKey::getName))
                    .toList();
        }

        private boolean isExcluded(DataKey<?> key) {
            return EXCLUDED_DATA_KEYS.contains(key.getName());
        }

        private void addNode(ClavaNode node) {
            if (indexes.containsKey(node)) {
                return;
            }
            indexes.put(node, nodes.size());
            nodes.add(node);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private Object read(ClavaNode node, DataKey<?> key) {
            return node.get((DataKey) key);
        }

        private List<Field> objectFields(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                        continue;
                    }
                    fields.add(field);
                }
            }
            fields.sort(Comparator.comparing(field -> field.getDeclaringClass().getName() + "." + field.getName()));
            return fields;
        }
    }

    /** Keep file output handling separate from snapshot semantics. */
    private static final class FilesSupport {
        private static void write(Path output, String content) throws Exception {
            java.nio.file.Files.createDirectories(output.toAbsolutePath().getParent());
            java.nio.file.Files.writeString(output, content + System.lineSeparator());
        }
    }
}
