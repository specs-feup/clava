/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.gson.GsonBuilder;
import org.suikasoft.jOptions.Datakey.DataKey;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.NullableNodeReference;

/** Build-time inventory of compiled Clava node classes and their inherited DataKeys. */
public final class BindingInventory {
    private static final String NODE_PACKAGE = "pt.up.fe.specs.clava";
    private static final String NODE_PATH = NODE_PACKAGE.replace('.', '/');

    private BindingInventory() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2 || !arguments[0].equals("--output")) {
            throw new IllegalArgumentException("Usage: BindingInventory --output <inventory.json>");
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        var classNames = discoverClasses();
        var classes = new ArrayList<Map<String, Object>>();
        for (String name : classNames) {
            Class<?> owner;
            try {
                owner = Class.forName(name, false, loader);
            } catch (LinkageError | ClassNotFoundException error) {
                throw new IllegalStateException("Could not load compiled Clava class " + name, error);
            }
            if (!ClavaNode.class.isAssignableFrom(owner)) {
                continue;
            }

            var entry = new LinkedHashMap<String, Object>();
            entry.put("name", owner.getName());
            entry.put("simple_name", owner.getSimpleName());
            entry.put("node", true);
            entry.put("abstract", Modifier.isAbstract(owner.getModifiers()));
            entry.put("parents", parents(owner));
            entry.put("keys", keys(owner));
            classes.add(entry);
        }

        classes.sort(Comparator.comparing(entry -> (String) entry.get("name")));
        var output = new LinkedHashMap<String, Object>();
        output.put("classes", classes);
        Path destination = Path.of(arguments[1]);
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, new GsonBuilder().setPrettyPrinting().create().toJson(output) + "\n");
    }

    private static Set<String> discoverClasses() throws Exception {
        var result = new TreeSet<String>();
        String classPath = System.getProperty("java.class.path");
        for (String entry : classPath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            Path path = Path.of(entry);
            if (Files.isDirectory(path)) {
                Path packageRoot = path.resolve(NODE_PATH);
                if (Files.isDirectory(packageRoot)) {
                    try (var paths = Files.walk(packageRoot)) {
                        paths.filter(file -> file.toString().endsWith(".class"))
                                .map(file -> packageName(packageRoot, file))
                                .filter(name -> !name.endsWith("module-info") && !name.endsWith("package-info"))
                                .forEach(result::add);
                    }
                }
            } else if (Files.isRegularFile(path) && entry.endsWith(".jar")) {
                try (var jar = new JarFile(path.toFile())) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith(NODE_PATH + "/") && name.endsWith(".class")
                                && !name.endsWith("module-info.class") && !name.endsWith("package-info.class")) {
                            result.add(name.substring(0, name.length() - 6).replace('/', '.'));
                        }
                    }
                }
            }
        }
        return result;
    }

    private static String packageName(Path root, Path file) {
        String relative = root.relativize(file).toString();
        return NODE_PACKAGE + "." + relative.substring(0, relative.length() - 6)
                .replace(File.separatorChar, '.');
    }

    private static List<String> parents(Class<?> owner) {
        var result = new ArrayList<String>();
        for (Class<?> current = owner.getSuperclass(); current != null && ClavaNode.class.isAssignableFrom(current);
                current = current.getSuperclass()) {
            result.add(current.getName());
        }
        return result;
    }

    private static List<Map<String, Object>> keys(Class<?> owner) throws IllegalAccessException {
        Field[] fields = owner.getFields();
        Arrays.sort(fields, Comparator.comparingInt((Field field) -> distance(owner, field.getDeclaringClass()))
                .thenComparing(field -> field.getDeclaringClass().getName()).thenComparing(Field::getName));
        var result = new ArrayList<Map<String, Object>>();
        var byName = new LinkedHashMap<String, Field>();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers()) || !DataKey.class.isAssignableFrom(field.getType())) {
                continue;
            }
            DataKey<?> key = (DataKey<?>) field.get(null);
            if (key == null) {
                throw new IllegalStateException("Null DataKey: " + field);
            }

            Field previous = byName.get(key.getName());
            if (previous != null) {
                if (previous.getDeclaringClass().isAssignableFrom(field.getDeclaringClass())) {
                    result.removeIf(entry -> key.getName().equals(entry.get("name")));
                    byName.put(key.getName(), field);
                } else if (field.getDeclaringClass().isAssignableFrom(previous.getDeclaringClass())) {
                    continue;
                } else {
                    throw new IllegalStateException("Ambiguous DataKey name " + key.getName()
                            + " in " + owner.getName() + ": " + previous + " and " + field);
                }
            } else {
                byName.put(key.getName(), field);
            }

            Type declared = field.getGenericType();
            if (!(declared instanceof ParameterizedType parameterized)
                    || parameterized.getActualTypeArguments().length != 1) {
                throw new IllegalStateException("DataKey lacks its declared value type: " + field);
            }
            Type value = parameterized.getActualTypeArguments()[0];
            var entry = new LinkedHashMap<String, Object>();
            entry.put("field", field.getName());
            entry.put("declaring_class", field.getDeclaringClass().getName());
            entry.put("name", key.getName());
            entry.put("generic_type", value.getTypeName());
            entry.put("value_class", key.getValueClass().getName());
            entry.put("type", describe(value));
            entry.put("nullable_reference", field.isAnnotationPresent(NullableNodeReference.class));
            result.add(entry);
        }
        result.sort(Comparator.comparing(entry -> (String) entry.get("name")));
        return result;
    }

    private static int distance(Class<?> owner, Class<?> declaring) {
        int distance = 0;
        for (Class<?> current = owner; current != null; current = current.getSuperclass(), distance++) {
            if (current == declaring) {
                return distance;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static Map<String, Object> describe(Type type) {
        var result = new LinkedHashMap<String, Object>();
        result.put("name", type.getTypeName());
        if (type instanceof ParameterizedType parameterized) {
            Class<?> raw = (Class<?>) parameterized.getRawType();
            result.put("kind", kind(raw));
            result.put("raw_class", raw.getName());
            result.put("arguments", Arrays.stream(parameterized.getActualTypeArguments())
                    .map(BindingInventory::describe).toList());
        } else if (type instanceof Class<?> value) {
            result.put("kind", kind(value));
            result.put("raw_class", value.getName());
            if (value.isEnum()) {
                result.put("enum_constants", Arrays.stream(value.getEnumConstants())
                        .map(constant -> ((Enum<?>) constant).name()).toList());
            }
            if (value.isArray()) {
                result.put("arguments", List.of(describe(value.getComponentType())));
            }
        } else if (type instanceof WildcardType wildcard) {
            result.put("kind", "wildcard");
            result.put("upper_bounds", Arrays.stream(wildcard.getUpperBounds()).map(BindingInventory::describe).toList());
            result.put("lower_bounds", Arrays.stream(wildcard.getLowerBounds()).map(BindingInventory::describe).toList());
        } else if (type instanceof GenericArrayType array) {
            result.put("kind", "array");
            result.put("arguments", List.of(describe(array.getGenericComponentType())));
        } else if (type instanceof TypeVariable<?>) {
            result.put("kind", "type_variable");
        } else {
            throw new IllegalStateException("Unsupported Java type: " + type);
        }
        return result;
    }

    private static String kind(Class<?> value) {
        if (ClavaNode.class.isAssignableFrom(value)) return "node";
        if (value.isEnum()) return "enum";
        if (value.isArray()) return "array";
        if (Optional.class.isAssignableFrom(value)) return "optional";
        if (List.class.isAssignableFrom(value)) return "list";
        if (Collection.class.isAssignableFrom(value)) return "collection";
        if (value.isPrimitive() || value == String.class || value == Boolean.class
                || value == Character.class || Number.class.isAssignableFrom(value)) return "scalar";
        return "object";
    }
}
