package com.netflix.gradle.plugins.packaging

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty

/** Mutable 12.x collection views backed by the 13.x Gradle properties. */
final class PropertyBackedList<T> extends AbstractList<T> {
    private final ListProperty<T> property
    private final Runnable onChange

    PropertyBackedList(ListProperty<T> property, Runnable onChange = null) {
        this.property = property
        this.onChange = onChange
    }

    @Override
    T get(int index) { property.getOrElse([]).get(index) }

    @Override
    int size() { property.getOrElse([]).size() }

    // Without a bean getter, Groovy treats list.size as a spread over the elements.
    int getSize() { size() }

    @Override
    void add(int index, T value) {
        def values = new ArrayList<T>(property.getOrElse([]))
        values.add(index, value)
        replace(values)
        modCount++
    }

    @Override
    T set(int index, T value) {
        def values = new ArrayList<T>(property.getOrElse([]))
        T previous = values.set(index, value)
        replace(values)
        previous
    }

    @Override
    T remove(int index) {
        def values = new ArrayList<T>(property.getOrElse([]))
        T previous = values.remove(index)
        replace(values)
        modCount++
        previous
    }

    @Override
    void clear() {
        replace([])
        modCount++
    }

    @Override
    public void removeRange(int fromIndex, int toIndex) {
        def values = new ArrayList<T>(property.getOrElse([]))
        values.subList(fromIndex, toIndex).clear()
        replace(values)
        modCount++
    }

    private void replace(List<T> values) {
        property.set(values)
        onChange?.run()
    }
}

/** A live map view; copy-on-write keeps Gradle's MapProperty as the source of truth. */
final class PropertyBackedMap<K, V> extends AbstractMap<K, V> {
    private final MapProperty<K, V> property

    PropertyBackedMap(MapProperty<K, V> property) { this.property = property }

    @Override
    V get(Object key) { property.getOrElse([:]).get(key) }

    @Override
    int size() { property.getOrElse([:]).size() }

    @Override
    V put(K key, V value) {
        def values = new LinkedHashMap<K, V>(property.getOrElse([:]))
        V previous = values.put(key, value)
        property.set(values)
        previous
    }

    @Override
    V remove(Object key) {
        def values = new LinkedHashMap<K, V>(property.getOrElse([:]))
        V previous = values.remove(key)
        property.set(values)
        previous
    }

    @Override
    void clear() { property.set([:]) }

    @Override
    Set<Map.Entry<K, V>> entrySet() {
        new AbstractSet<Map.Entry<K, V>>() {
            @Override
            int size() { PropertyBackedMap.this.size() }

            @Override
            Iterator<Map.Entry<K, V>> iterator() {
                def keys = new ArrayList<K>(property.getOrElse([:]).keySet()).iterator()
                new Iterator<Map.Entry<K, V>>() {
                    K lastKey
                    boolean mayRemove

                    @Override
                    boolean hasNext() { keys.hasNext() }

                    @Override
                    Map.Entry<K, V> next() {
                        K key = keys.next()
                        lastKey = key
                        mayRemove = true
                        new AbstractMap.SimpleEntry<K, V>(key, PropertyBackedMap.this.get(key)) {
                            @Override
                            V setValue(V value) {
                                V previous = PropertyBackedMap.this.put(key, value)
                                super.setValue(value)
                                previous
                            }
                        }
                    }

                    @Override
                    void remove() {
                        if (!mayRemove) throw new IllegalStateException()
                        PropertyBackedMap.this.remove(lastKey)
                        mayRemove = false
                    }
                }
            }
        }
    }
}
