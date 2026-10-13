/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;

/**
 * A compatibility view over generated node fields. Unbound, Clava-only properties stay in the supplied store.
 * Parser fixups, generic visitors and factories use this same view, so there is only one value per property.
 * Sharing a view shares its generated state; copying uses DataStore's normal per-key copy functions.
 */
public final class NodeDataStore implements DataStore {
    private record Field(Supplier<?> read, Consumer<Object> write) {}
    private final DataStore extra;
    private final Map<String, Field> fields = new HashMap<>();
    private final Map<Class<?>, Object> states = new HashMap<>();
    private boolean strict;

    private NodeDataStore(DataStore extra) { this.extra = extra; }

    public static NodeDataStore adapt(DataStore store) {
        return store instanceof NodeDataStore node ? node : new NodeDataStore(store);
    }

    /** Each generated base owns a typed state, shared only when callers explicitly share node data. */
    public <T> T state(Class<T> type, Supplier<T> create) {
        return type.cast(states.computeIfAbsent(type, ignored -> create.get()));
    }

    @SuppressWarnings("unchecked")
    public <T> void bind(DataKey<T> key, Supplier<T> read, Consumer<T> write) {
        if (fields.containsKey(key.getName())) {
            throw new IllegalStateException("Duplicate generated field " + key.getName());
        }
        T value = extra.getTry(key).orElse(null);
        write.accept(value);
        extra.remove(key);
        fields.put(key.getName(), new Field(read, raw -> write.accept((T) raw)));
    }

    public boolean isBound(DataKey<?> key) { return fields.containsKey(key.getName()); }

    @Override
    public <T> T get(DataKey<T> key) {
        var field = fields.get(key.getName());
        if (field == null) return extra.get(key);
        T value = key.getValueClass().cast(field.read().get());
        if (value == null) {
            if (strict) throw new IllegalStateException("Missing node field " + key.getName());
            if (!key.hasDefaultValue()) throw new IllegalStateException("No default for " + key.getName());
            value = key.getDefault().orElse(null);
            field.write().accept(value);
        }
        T result = value;
        return key.getCustomGetter().map(getter -> getter.get(result, this)).orElse(result);
    }

    @Override
    public <T, E extends T> DataStore set(DataKey<T> key, E value) {
        Objects.requireNonNull(value, "Use remove() to clear a node field");
        if (key.verifyValueClass() && !key.getValueClass().isInstance(value)) {
            throw new IllegalArgumentException("Wrong value type for " + key.getName());
        }
        setRaw(key.getName(), value);
        return this;
    }

    @Override
    public Optional<Object> setRaw(String key, Object value) {
        var field = fields.get(key);
        if (field == null) return extra.setRaw(key, value);
        var previous = Optional.ofNullable(field.read().get());
        field.write().accept(value);
        return previous;
    }

    @Override
    public Object get(String key) {
        var field = fields.get(key);
        return field == null ? extra.get(key) : field.read().get();
    }

    @Override
    public <T> Optional<T> remove(DataKey<T> key) {
        if (!isBound(key)) return extra.remove(key);
        var previous = getTry(key);
        setRaw(key.getName(), null);
        return previous;
    }

    @Override
    public <T> boolean hasValue(DataKey<T> key) {
        return isBound(key) ? fields.get(key.getName()).read().get() != null : extra.hasValue(key);
    }

    @Override
    public Collection<String> getKeysWithValues() {
        var keys = new LinkedHashSet<>(extra.getKeysWithValues());
        fields.forEach((name, field) -> { if (field.read().get() != null) keys.add(name); });
        return keys;
    }

    @Override
    public void setStrict(boolean value) { strict = value; extra.setStrict(value); }
    @Override
    public String getName() { return extra.getName(); }
    @Override
    public Optional<StoreDefinition> getStoreDefinitionTry() { return extra.getStoreDefinitionTry(); }
    @Override
    public void setStoreDefinition(StoreDefinition definition) { extra.setStoreDefinition(definition); }
    @Override
    public boolean isClosed() { return extra.isClosed(); }
    @Override
    public String toString() { return toInlinedString(); }
}
