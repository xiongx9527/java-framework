package dev.langchain4j.agentic.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Immutable collection factories used internally by Agentic.
 *
 * <p>This class mirrors the null, duplicate, snapshot, order and mutability
 * contracts of the corresponding JDK collection factories.</p>
 */
public final class CollectionHelper {

    private CollectionHelper() {
    }

    @SafeVarargs
    public static <T> List<T> listOf(T... elements) {
        Objects.requireNonNull(elements, "elements");
        ArrayList<T> copy = new ArrayList<T>(elements.length);
        for (T element : elements) {
            copy.add(Objects.requireNonNull(element, "element"));
        }
        return Collections.unmodifiableList(copy);
    }

    public static <T> List<T> listCopyOf(Collection<? extends T> source) {
        Objects.requireNonNull(source, "source");
        ArrayList<T> copy = new ArrayList<T>(source.size());
        for (T element : source) {
            copy.add(Objects.requireNonNull(element, "element"));
        }
        return Collections.unmodifiableList(copy);
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... elements) {
        Objects.requireNonNull(elements, "elements");
        LinkedHashSet<T> copy = new LinkedHashSet<T>(capacityFor(elements.length));
        for (T element : elements) {
            T nonNullElement = Objects.requireNonNull(element, "element");
            if (!copy.add(nonNullElement)) {
                throw new IllegalArgumentException("duplicate element: " + nonNullElement);
            }
        }
        return Collections.unmodifiableSet(copy);
    }

    public static <T> Set<T> setCopyOf(Collection<? extends T> source) {
        Objects.requireNonNull(source, "source");
        LinkedHashSet<T> copy = new LinkedHashSet<T>(capacityFor(source.size()));
        for (T element : source) {
            copy.add(Objects.requireNonNull(element, "element"));
        }
        return Collections.unmodifiableSet(copy);
    }

    public static <K, V> Map<K, V> mapOf() {
        return Collections.emptyMap();
    }

    public static <K, V> Map<K, V> mapOf(K key, V value) {
        LinkedHashMap<K, V> copy = new LinkedHashMap<K, V>(1);
        putUnique(copy, key, value);
        return Collections.unmodifiableMap(copy);
    }

    public static <K, V> Map<K, V> mapOf(K key1, V value1, K key2, V value2) {
        LinkedHashMap<K, V> copy = new LinkedHashMap<K, V>(2);
        putUnique(copy, key1, value1);
        putUnique(copy, key2, value2);
        return Collections.unmodifiableMap(copy);
    }

    public static <K, V> Map<K, V> mapOf(
            K key1, V value1, K key2, V value2, K key3, V value3) {
        LinkedHashMap<K, V> copy = new LinkedHashMap<K, V>(3);
        putUnique(copy, key1, value1);
        putUnique(copy, key2, value2);
        putUnique(copy, key3, value3);
        return Collections.unmodifiableMap(copy);
    }

    public static <K, V> Map<K, V> mapOf(
            K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4) {
        LinkedHashMap<K, V> copy = new LinkedHashMap<K, V>(4);
        putUnique(copy, key1, value1);
        putUnique(copy, key2, value2);
        putUnique(copy, key3, value3);
        putUnique(copy, key4, value4);
        return Collections.unmodifiableMap(copy);
    }

    public static <K, V> Map<K, V> mapCopyOf(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        LinkedHashMap<K, V> copy = new LinkedHashMap<K, V>(capacityFor(source.size()));
        for (Map.Entry<? extends K, ? extends V> entry : source.entrySet()) {
            putUnique(copy, entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Collector that preserves encounter order and returns an immutable list.
     * Null elements are retained.
     */
    public static <T> Collector<T, List<T>, List<T>> toUnmodifiableList() {
        Supplier<List<T>> supplier = new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>();
            }
        };
        BiConsumer<List<T>, T> accumulator = new BiConsumer<List<T>, T>() {
            @Override
            public void accept(List<T> list, T value) {
                list.add(value);
            }
        };
        BinaryOperator<List<T>> combiner = new BinaryOperator<List<T>>() {
            @Override
            public List<T> apply(List<T> left, List<T> right) {
                left.addAll(right);
                return left;
            }
        };
        Function<List<T>, List<T>> finisher = new Function<List<T>, List<T>>() {
            @Override
            public List<T> apply(List<T> list) {
                return Collections.unmodifiableList(new ArrayList<T>(list));
            }
        };
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    private static <K, V> void putUnique(Map<K, V> map, K key, V value) {
        K nonNullKey = Objects.requireNonNull(key, "key");
        V nonNullValue = Objects.requireNonNull(value, "value");
        if (map.containsKey(nonNullKey)) {
            throw new IllegalArgumentException("duplicate key: " + nonNullKey);
        }
        map.put(nonNullKey, nonNullValue);
    }

    private static int capacityFor(int size) {
        return size < 3 ? size + 1 : size + (size / 3) + 1;
    }
}
