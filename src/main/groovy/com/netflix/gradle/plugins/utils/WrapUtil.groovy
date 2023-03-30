package com.netflix.gradle.plugins.utils

import org.gradle.api.DomainObjectSet
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.DefaultDomainObjectSet

/**
 * copied from org.gradle.util.WrapUtil
 * <a href="https://github.com/gradle/gradle/blob/v8.0.2/subprojects/core/src/main/java/org/gradle/util/WrapUtil.java">WrapUtil.java</a>
 */
class WrapUtil {
    /**
     * Wraps the given items in a mutable unordered set.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    static <T> Set<T> toSet(T... items) {
        Set<T> coll = new HashSet<T>()
        Collections.addAll(coll, items)
        return coll
    }

    /**
     * Wraps the given items in a mutable domain object set.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    static <T> DomainObjectSet<T> toDomainObjectSet(Class<T> type, T... items) {
        DefaultDomainObjectSet<T> set = new DefaultDomainObjectSet<T>(type, CollectionCallbackActionDecorator.NOOP)
        set.addAll(Arrays.asList(items))
        return set
    }

    /**
     * Wraps the given items in a mutable ordered set.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    static <T> Set<T> toLinkedSet(T... items) {
        Set<T> coll = new LinkedHashSet<T>()
        Collections.addAll(coll, items)
        return coll
    }

    /**
     * Wraps the given items in a mutable sorted set.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    static <T> SortedSet<T> toSortedSet(T... items) {
        SortedSet<T> coll = new TreeSet<T>()
        Collections.addAll(coll, items)
        return coll
    }

    /**
     * Wraps the given items in a mutable sorted set using the given comparator.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    static <T> SortedSet<T> toSortedSet(Comparator<T> comp, T... items) {
        SortedSet<T> coll = new TreeSet<T>(comp)
        Collections.addAll(coll, items)
        return coll
    }

    /**
     * Wraps the given items in a mutable list.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    static <T> List<T> toList(T... items) {
        ArrayList<T> coll = new ArrayList<T>()
        Collections.addAll(coll, items)
        return coll
    }

    /**
     * Wraps the given items in a mutable list.
     */
    static <T> List<T> toList(Iterable<? extends T> items) {
        ArrayList<T> coll = new ArrayList<T>()
        for (T item : items) {
            coll.add(item)
        }
        return coll
    }

    /**
     * Wraps the given key and value in a mutable unordered map.
     */
    static <K, V> Map<K, V> toMap(K key, V value) {
        Map<K, V> map = new HashMap<K, V>()
        map.put(key, value)
        return map
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    static <T> T[] toArray(T... items) {
        return items
    }

}
