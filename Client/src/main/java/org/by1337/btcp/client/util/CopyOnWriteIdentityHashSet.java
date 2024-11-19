package org.by1337.btcp.client.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CopyOnWriteIdentityHashSet<E> implements Set<E> {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private IdentityHashMap<E, Object> map;
    private static final Object PRESENT = new Object();

    public CopyOnWriteIdentityHashSet() {
        map = new IdentityHashMap<>();
    }

    public CopyOnWriteIdentityHashSet(Collection<? extends E> c) {
        map = new IdentityHashMap<>(Math.max(c.size(), 12));
        addAll(c);
    }

    public CopyOnWriteIdentityHashSet(int initialCapacity) {
        map = new IdentityHashMap<>(initialCapacity);
    }

    @Override
    public Spliterator<E> spliterator() {
        try (var ignore = readLock()) {
            return Set.super.spliterator();
        }
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        try (var ignore = readLock()) {
            return Set.super.toArray(generator);
        }
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        try (var ignore = writeLock()) {
            map = new IdentityHashMap<>(map);
            return map.keySet().removeIf(filter);
        }
    }

    @Override
    public Stream<E> stream() {
        try (var ignore = readLock()) {
            return Set.super.stream();
        }
    }

    @Override
    public Stream<E> parallelStream() {
        try (var ignore = readLock()) {
            return Set.super.parallelStream();
        }
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        try (var ignore = readLock()) {
            map.keySet().forEach(action);
        }
    }

    @Override
    public int size() {
        try (var ignore = readLock()) {
            return map.size();
        }
    }

    @Override
    public boolean isEmpty() {
        try (var ignore = readLock()) {
            return map.isEmpty();
        }
    }

    @Override
    public boolean contains(Object o) {
        try (var ignore = readLock()) {
            return map.containsKey(o);
        }
    }

    @Override
    public @NotNull Iterator<E> iterator() {
        try (var ignore = readLock()) {
            return map.keySet().iterator();
        }
    }

    @Override
    public @NotNull Object[] toArray() {
        try (var ignore = readLock()) {
            return map.keySet().toArray();
        }
    }

    @Override
    public @NotNull <T1> T1[] toArray(@NotNull T1[] a) {
        try (var ignore = readLock()) {
            return map.keySet().toArray(a);
        }
    }

    @Override
    public boolean add(E e) {
        if (contains(e)) return false;
        try (var ignore = writeLock()) {
            map = new IdentityHashMap<>(map);
            return map.put(e, PRESENT) == null;
        }
    }

    @Override
    public boolean remove(Object o) {
        if (!contains(o)) return false;
        try (var ignore = writeLock()) {
            map = new IdentityHashMap<>(map);
            return map.remove(o) == null;
        }
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        try (var ignore = readLock()) {
            return map.keySet().containsAll(c);
        }
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        try (var ignore = writeLock()) {
            map = new IdentityHashMap<>(map);
            boolean modified = false;
            for (E element : c) {
                if (map.put(element, PRESENT) == null) {
                    modified = true;
                }
            }
            return modified;
        }
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        try (var ignore = readLock()) {
            return map.keySet().retainAll(c);
        }
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        try (var ignore = writeLock()) {
            map = new IdentityHashMap<>(map);
            return map.keySet().removeAll(c);
        }
    }

    @Override
    public void clear() {
        try (var ignore = writeLock()) {
            map = new IdentityHashMap<>();
        }
    }

    private AutoCloseableLock readLock() {
        lock.readLock().lock();
        return () -> lock.readLock().unlock();
    }

    private AutoCloseableLock writeLock() {
        lock.writeLock().lock();
        return () -> lock.writeLock().unlock();
    }

    @FunctionalInterface
    private interface AutoCloseableLock extends AutoCloseable {
        @Override
        void close();
    }
}
