/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.data;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.suikasoft.jOptions.Datakey.DataKey;
import pt.up.fe.specs.clava.ClavaNode;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;
import org.suikasoft.jOptions.storedefinition.StoreDefinitions;

/**
 * Generic access to generated node fields. Field layouts are immutable and shared between nodes. Each node
 * keeps only its typed state objects; Clava-only properties remain in the supplied store. Parser fixups use
 * this same view. Explicit shared views share states, and DataStore.copy retains the existing copy functions.
 */
public final class NodeDataStore implements DataStore {
    /** Implemented by each generated base's typed state with small integer switches. */
    public static class Fields {
        private long present;
        private BitSet extendedPresence;

        public Object read(int index) { throw new IllegalArgumentException("Unknown field index " + index); }
        public void write(int index, Object value) { throw new IllegalArgumentException("Unknown field index " + index); }

        public final boolean has(int index) {
            return index < Long.SIZE ? (present & (1L << index)) != 0
                    : extendedPresence != null && extendedPresence.get(index - Long.SIZE);
        }

        public final void mark(int index, Object value) {
            if (index < Long.SIZE) {
                long bit = 1L << index;
                present = value == null ? present & ~bit : present | bit;
            } else if (value != null || extendedPresence != null) {
                if (extendedPresence == null) extendedPresence = new BitSet();
                extendedPresence.set(index - Long.SIZE, value != null);
            }
        }
    }

    public record Layout<T extends Fields>(Class<T> type, Supplier<T> create, List<DataKey<?>> keys, int offset, int tag) {
        public Layout { keys = List.copyOf(keys); }
    }

    private record Slot(int group, int field, int tag, DataKey<?> key) {}
    private static final Map<Class<?>, Shape> SHAPES = new ConcurrentHashMap<>();
    private record Construction(Shape shape, StoreDefinition full, StoreDefinition extra) {}
    private static final Map<Class<?>, Construction> CONSTRUCTIONS = new ConcurrentHashMap<>();
    private static final Map<List<Layout<?>>, Shape> SHARED_SHAPES = new ConcurrentHashMap<>();

    private static final class Shape {
        final List<Layout<?>> layouts;
        final Map<Class<?>, Integer> groups = new HashMap<>();
        final Map<String, Slot> slots = new LinkedHashMap<>();
        final boolean singleState;

        Shape(List<Layout<?>> layouts) {
            this.layouts = List.copyOf(layouts);
            var leaf = layouts.isEmpty() ? null : layouts.getLast().type();
            singleState = leaf != null && layouts.stream().allMatch(layout -> layout.type().isAssignableFrom(leaf));
            for (int group = 0; group < layouts.size(); group++) {
                var layout = layouts.get(group);
                groups.put(layout.type(), group);
                for (int field = 0; field < layout.keys().size(); field++) {
                    var key = layout.keys().get(field);
                    // Parent declarations own an already-bound name, matching shared DataStore semantics.
                    slots.putIfAbsent(key.getName(), new Slot(group, layout.offset() + field, layout.tag(), key));
                }
            }
        }
    }

    private final DataStore extra;
    private StoreDefinition definition;
    private Shape shape;
    private Fields primary;
    private Fields[] states;
    private boolean strict;

    private NodeDataStore(DataStore extra) {
        this.extra = extra;
        definition = extra.getStoreDefinitionTry().orElse(null);
    }

    public static NodeDataStore adapt(DataStore store) {
        return store instanceof NodeDataStore node ? node : new NodeDataStore(store);
    }

    public static NodeDataStore adapt(DataStore store, Class<?> nodeClass) {
        var node = adapt(store);
        node.attach(nodeClass);
        return node;
    }

    /** Parser construction starts with empty storage, so no per-field migration is needed. */
    public static NodeDataStore create(Class<?> nodeClass) {
        var construction = CONSTRUCTIONS.computeIfAbsent(nodeClass, NodeDataStore::construction);
        var node = new NodeDataStore(DataStore.newInstance(construction.extra(), true));
        node.definition = construction.full();
        node.attach(construction.shape(), false);
        return node;
    }

    private static Construction construction(Class<?> nodeClass) {
        var full = StoreDefinitions.fromInterface(nodeClass);
        var shape = SHAPES.computeIfAbsent(nodeClass, NodeDataStore::discover);
        var unbound = StoreDefinition.newInstance(full.getName(),
                full.getKeys().stream().filter(key -> !shape.slots.containsKey(key.getName())).toList());
        return new Construction(shape, full, unbound);
    }

    /** Discovers generated layouts once per behavior class, never once per node or per field access. */
    private static Shape discover(Class<?> nodeClass) {
        var layouts = new ArrayList<Layout<?>>();
        for (Class<?> type = nodeClass; type != null; type = type.getSuperclass()) {
            try {
                layouts.add((Layout<?>) type.getDeclaredField("WIRE_LAYOUT").get(null));
            } catch (NoSuchFieldException ignored) {
                // Hand-written behavior and Clava-only node classes declare no layout.
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot read generated layout for " + type, e);
            }
        }
        Collections.reverse(layouts);
        return new Shape(layouts);
    }

    private void attach(Class<?> nodeClass) { attach(nodeClass, true); }

    private void attach(Class<?> nodeClass, boolean transferValues) {
        var target = SHAPES.computeIfAbsent(nodeClass, NodeDataStore::discover);
        attach(target, transferValues);
    }

    private void attach(Shape target, boolean transferValues) {
        if (shape == target || shape != null && shape.layouts.containsAll(target.layouts)) return;
        if (shape == null && target.singleState) {
            primary = createState(target.layouts.getLast());
            shape = target;
            if (transferValues) migrate(null);
            return;
        }
        var previousShape = shape;
        var next = target;
        if (shape != null) {
            var layouts = new ArrayList<>(shape.layouts);
            for (var layout : target.layouts) if (!layouts.contains(layout)) layouts.add(layout);
            next = SHARED_SHAPES.computeIfAbsent(List.copyOf(layouts), Shape::new);
        }
        var nextStates = new Fields[next.layouts.size()];
        // A normal node needs only its most derived state. Parent layouts refer to that same object.
        Fields fresh = null;
        for (int group = 0; group < nextStates.length; group++) {
            var layout = next.layouts.get(group);
            Integer previous = shape == null ? null : shape.groups.get(layout.type());
            if (previous != null) {
                nextStates[group] = state(previous);
                continue;
            }
            if (fresh == null || !layout.type().isInstance(fresh)) {
                // Shared views can add another branch. Reuse old states, and allocate only the new branch.
                for (int candidate = next.layouts.size() - 1; candidate >= 0; candidate--) {
                    var leaf = next.layouts.get(candidate);
                    if (layout.type().isAssignableFrom(leaf.type())) {
                        fresh = createState(leaf);
                        break;
                    }
                }
            }
            nextStates[group] = fresh;
        }
        shape = next;
        states = nextStates;
        primary = nextStates.length == 0 ? null : nextStates[0];
        if (transferValues) migrate(previousShape);
    }

    private static Fields createState(Layout<?> layout) {
        return layout.tag() < 0 ? layout.create().get() : GeneratedNodeFields.create(layout.tag());
    }

    private void migrate(Shape previousShape) {
        for (var entry : shape.slots.entrySet()) {
            if (previousShape != null && previousShape.slots.containsKey(entry.getKey())) continue;
            var slot = entry.getValue();
            if (extra.hasValue(slot.key())) {
                write(slot, extra.get(entry.getKey()));
                extra.remove(slot.key());
            }
        }
    }

    /** Used by generated constructors to retain their own typed state, including explicit shared views. */
    public <T extends Fields> T state(Class<T> type) {
        return type.cast(states == null ? primary : states[shape.groups.get(type)]);
    }

    private Fields state(int group) { return states == null ? primary : states[group]; }

    /** Direct generated access avoids generic field lookup while retaining default and custom-getter rules. */
    public <T> T readGenerated(DataKey<T> key, T value) {
        if (value == null) return get(key);
        return applyGeneratedGetter(key, value);
    }

    /** Generated callers initialize the actual field directly, without generic lookup or dispatch. */
    public <T> T defaultGenerated(DataKey<T> key) {
        if (strict) throw new IllegalStateException("Missing node field " + key.getName());
        if (!key.hasDefaultValue()) throw new IllegalStateException("No default for " + key.getName());
        return key.getDefault().orElse(null);
    }

    public <T> T applyGeneratedGetter(DataKey<T> key, T value) {
        var getter = key.getCustomGetter();
        return getter.isPresent() ? getter.get().get(value, this) : value;
    }

    private Slot slot(String name) { return shape == null ? null : shape.slots.get(name); }
    private Object read(Slot slot) {
        return slot.group() == 0 ? ((ClavaNode.WireFields) primary).readRoot(slot.field())
                : GeneratedNodeFields.read(slot.tag(), state(slot.group()), slot.field());
    }
    private void write(Slot slot, Object value) {
        if (slot.group() == 0) ((ClavaNode.WireFields) primary).writeRoot(slot.field(), value);
        else GeneratedNodeFields.write(slot.tag(), state(slot.group()), slot.field(), value);
    }

    @Override
    public <T> T get(DataKey<T> key) {
        var slot = slot(key.getName());
        if (slot == null) return extra.get(key);
        T value = key.getValueClass().cast(read(slot));
        if (value == null) {
            value = defaultGenerated(key);
            write(slot, value);
        }
        var getter = key.getCustomGetter();
        return getter.isPresent() ? getter.get().get(value, this) : value;
    }

    @Override
    public <T, E extends T> DataStore set(DataKey<T> key, E value) {
        Objects.requireNonNull(value, "Use remove() to clear a node field");
        if (key.verifyValueClass() && !key.getValueClass().isInstance(value)) {
            throw new IllegalArgumentException("Wrong value type for " + key.getName());
        }
        var slot = slot(key.getName());
        if (slot == null) extra.set(key, value);
        else write(slot, value);
        return this;
    }

    @Override
    public Optional<Object> setRaw(String key, Object value) {
        var slot = slot(key);
        if (slot == null) return extra.setRaw(key, value);
        var previous = Optional.ofNullable(read(slot));
        write(slot, value);
        return previous;
    }

    @Override
    public Object get(String key) {
        var slot = slot(key);
        return slot == null ? extra.get(key) : read(slot);
    }

    @Override
    public <T> Optional<T> remove(DataKey<T> key) {
        var slot = slot(key.getName());
        if (slot == null) return extra.remove(key);
        var previous = getTry(key);
        write(slot, null);
        return previous;
    }

    @Override
    public <T> boolean hasValue(DataKey<T> key) {
        var slot = slot(key.getName());
        return slot == null ? extra.hasValue(key) : state(slot.group()).has(slot.field());
    }

    @Override
    public Collection<String> getKeysWithValues() {
        var keys = new LinkedHashSet<>(extra.getKeysWithValues());
        if (shape != null) shape.slots.forEach((name, slot) -> { if (state(slot.group()).has(slot.field())) keys.add(name); });
        return keys;
    }

    @Override
    public DataStore copy() {
        if (shape == null || definition == null) return DataStore.super.copy();
        var copy = new NodeDataStore(DataStore.newInstance(extra.getStoreDefinition(), extra.isClosed()));
        copy.definition = definition;
        copy.attach(shape, false);
        for (var key : definition.getKeys()) {
            if (hasValue(key)) copy.setRaw(key.getName(), key.copyRaw(get(key.getName())));
        }
        return copy;
    }

    @Override public void setStrict(boolean value) { strict = value; extra.setStrict(value); }
    @Override public String getName() { return extra.getName(); }
    @Override public Optional<StoreDefinition> getStoreDefinitionTry() { return Optional.ofNullable(definition); }
    @Override public void setStoreDefinition(StoreDefinition definition) {
        extra.setStoreDefinition(definition);
        this.definition = definition;
    }
    @Override public boolean isClosed() { return extra.isClosed(); }
    @Override public String toString() { return toInlinedString(); }
}
