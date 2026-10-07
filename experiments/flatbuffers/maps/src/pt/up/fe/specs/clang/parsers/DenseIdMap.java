package pt.up.fe.specs.clang.parsers;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/** Map optimized for canonical Clang node ids ({@code @1}, {@code @2}, ...). */
public final class DenseIdMap<V> extends AbstractMap<String, V> {
    /** Bounds sparse-id allocations; larger and non-canonical keys use fallback. */
    static final int MAX_DENSE_INDEX = 1_000_000;
    private static final Object EMPTY = new Object();
    private Object[] values = new Object[16];
    private final Map<String, V> fallback = new HashMap<>();
    private final Set<Entry<String, V>> entries = new Entries();
    private int size;
    private int modCount;

    public DenseIdMap() {
        Arrays.fill(values, EMPTY);
    }

    @Override
    public V get(Object key) {
        int index = denseIndex(key);
        return index >= 0 ? valueAt(index) : fallback.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        int index = denseIndex(key);
        return index >= 0 ? isPresent(index) : fallback.containsKey(key);
    }

    @Override
    public V put(String key, V value) {
        int index = denseIndex(key);
        if (index >= 0) {
            ensureCapacity(index);
            V previous = valueAt(index);
            if (!isPresent(index)) {
                size++;
                modCount++;
            }
            values[index] = value;
            return previous;
        }
        boolean present = fallback.containsKey(key);
        V previous = fallback.put(key, value);
        if (!present) {
            size++;
            modCount++;
        }
        return previous;
    }

    @Override
    public V remove(Object key) {
        int index = denseIndex(key);
        if (index >= 0) {
            if (!isPresent(index)) {
                return null;
            }
            V previous = valueAt(index);
            values[index] = EMPTY;
            size--;
            modCount++;
            return previous;
        }
        if (!fallback.containsKey(key)) {
            return null;
        }
        V previous = fallback.remove(key);
        size--;
        modCount++;
        return previous;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        Arrays.fill(values, EMPTY);
        fallback.clear();
        size = 0;
        modCount++;
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        return entries;
    }

    private static int denseIndex(Object key) {
        if (!(key instanceof String id) || id.length() < 2 || id.charAt(0) != '@'
                || id.charAt(1) < '1' || id.charAt(1) > '9') {
            return -1;
        }
        int index = 0;
        for (int i = 1; i < id.length(); i++) {
            char digit = id.charAt(i);
            if (digit < '0' || digit > '9') {
                return -1;
            }
            int next = index * 10 + digit - '0';
            if (next < index || next > MAX_DENSE_INDEX) {
                return -1;
            }
            index = next;
        }
        return index;
    }

    @SuppressWarnings("unchecked")
    private V valueAt(int index) {
        return isPresent(index) ? (V) values[index] : null;
    }

    private boolean isPresent(int index) {
        return index < values.length && values[index] != EMPTY;
    }

    private void ensureCapacity(int index) {
        if (index < values.length) {
            return;
        }
        int newLength = values.length;
        while (newLength <= index) {
            newLength = Math.min(MAX_DENSE_INDEX + 1, newLength * 2);
        }
        Object[] expanded = Arrays.copyOf(values, newLength);
        Arrays.fill(expanded, values.length, newLength, EMPTY);
        values = expanded;
    }

    private final class Entries extends AbstractSet<Entry<String, V>> {
        @Override
        public Iterator<Entry<String, V>> iterator() {
            ArrayList<String> keys = new ArrayList<>(size);
            for (int index = 1; index < values.length; index++) {
                if (isPresent(index)) {
                    keys.add("@" + index);
                }
            }
            keys.addAll(fallback.keySet());
            return new EntryIterator(keys);
        }

        @Override public int size() { return DenseIdMap.this.size; }
        @Override public void clear() { DenseIdMap.this.clear(); }

        @Override
        public boolean contains(Object object) {
            if (!(object instanceof Entry<?, ?> entry)) {
                return false;
            }
            return containsKey(entry.getKey()) && Objects.equals(get(entry.getKey()), entry.getValue());
        }

        @Override
        public boolean remove(Object object) {
            if (!contains(object)) {
                return false;
            }
            DenseIdMap.this.remove(((Entry<?, ?>) object).getKey());
            return true;
        }
    }

    private final class EntryIterator implements Iterator<Entry<String, V>> {
        private final ArrayList<String> keys;
        private int expectedModCount = modCount;
        private int cursor;
        private String current;
        private boolean removable;

        private EntryIterator(ArrayList<String> keys) {
            this.keys = keys;
        }

        @Override public boolean hasNext() { check(); return cursor < keys.size(); }

        @Override
        public Entry<String, V> next() {
            check();
            if (cursor == keys.size()) {
                throw new NoSuchElementException();
            }
            current = keys.get(cursor++);
            removable = true;
            return new EntryView(current);
        }

        @Override
        public void remove() {
            check();
            if (!removable) {
                throw new IllegalStateException();
            }
            DenseIdMap.this.remove(current);
            expectedModCount = modCount;
            removable = false;
        }

        private void check() {
            if (expectedModCount != modCount) {
                throw new java.util.ConcurrentModificationException();
            }
        }
    }

    private final class EntryView extends AbstractMap.SimpleEntry<String, V> {
        private EntryView(String key) { super(key, null); }
        @Override public V getValue() { return DenseIdMap.this.get(getKey()); }
        @Override public V setValue(V value) { return DenseIdMap.this.put(getKey(), value); }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Entry<?, ?> entry)) {
                return false;
            }
            return Objects.equals(getKey(), entry.getKey()) && Objects.equals(getValue(), entry.getValue());
        }

        @Override public int hashCode() { return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue()); }
    }
}
