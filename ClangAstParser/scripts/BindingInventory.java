/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import com.google.gson.GsonBuilder;

import org.suikasoft.jOptions.Datakey.DataKey;
import pt.up.fe.specs.clava.ClavaNode;

/** Build-time inventory of exact Java classes and their public inherited DataKeys. */
public final class BindingInventory {
    private BindingInventory() {
    }

    public static void main(String[] arguments) throws ReflectiveOperationException {
        if (arguments.length == 0) {
            throw new IllegalArgumentException("Supply fully qualified Java class names");
        }
        var classes = new ArrayList<Map<String, Object>>();
        for (String name : new TreeSet<>(List.of(arguments))) {
            Class<?> owner = Class.forName(name);
            var entry = new LinkedHashMap<String, Object>();
            entry.put("name", owner.getName());
            entry.put("node", ClavaNode.class.isAssignableFrom(owner));
            entry.put("keys", keys(owner));
            classes.add(entry);
        }
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("classes", classes)));
    }

    private static List<Map<String, Object>> keys(Class<?> owner) throws IllegalAccessException {
        Field[] fields = owner.getFields();
        Arrays.sort(fields, Comparator.comparingInt((Field field) -> distance(owner, field.getDeclaringClass()))
                .thenComparing(field -> field.getDeclaringClass().getName()).thenComparing(Field::getName));
        var result = new ArrayList<Map<String, Object>>();
        var byName = new LinkedHashMap<String, DataKey<?>>();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers()) || !DataKey.class.isAssignableFrom(field.getType())) {
                continue;
            }
            DataKey<?> key = (DataKey<?>) field.get(null);
            if (key == null) {
                throw new IllegalStateException("Null DataKey: " + field);
            }
            DataKey<?> previous = byName.putIfAbsent(key.getName(), key);
            if (previous != null) {
                // A subclass may shadow an inherited key. Prefer the nearest declaration,
                // just as source lookup does; unrelated declarations remain an error.
                Field nearer = result.stream().filter(entry -> key.getName().equals(entry.get("name")))
                        .map(entry -> (Field) entry.get("source_field")).findFirst().orElseThrow();
                if (previous == key || field.getDeclaringClass().isAssignableFrom(nearer.getDeclaringClass())) {
                    continue;
                }
                throw new IllegalStateException("Ambiguous DataKey name " + key.getName() + " in " + owner.getName());
            }
            Type generic = field.getGenericType();
            if (!(generic instanceof ParameterizedType parameterized)
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
            entry.put("nullable_reference", field.isAnnotationPresent(
                    pt.up.fe.specs.clava.NullableNodeReference.class));
            entry.put("source_field", field);
            result.add(entry);
        }
        result.forEach(entry -> entry.remove("source_field"));
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
            // Keep type variables explicit. The emitter must reject a field it cannot
            // bind; substituting Object would hide an unresolved generic declaration.
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
