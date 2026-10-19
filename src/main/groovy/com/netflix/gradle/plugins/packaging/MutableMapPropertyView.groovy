package com.netflix.gradle.plugins.packaging

import org.gradle.api.provider.MapProperty

/** A live Map facade for the custom-fields API exposed before version 13. */
class MutableMapPropertyView<K, V> extends AbstractMap<K, V> {
    private final MapProperty<K, V> property

    MutableMapPropertyView(MapProperty<K, V> property) {
        this.property = property
    }

    private Map<K, V> snapshot() {
        property.getOrElse([:])
    }

    @Override
    V get(Object key) {
        property.getOrElse([:]).get(key)
    }

    @Override
    V put(K key, V value) {
        Map<K, V> values = new LinkedHashMap<>(property.getOrElse([:]))
        V previous = values.put(key, value)
        property.set(values)
        previous
    }

    @Override
    V remove(Object key) {
        Map<K, V> values = new LinkedHashMap<>(property.getOrElse([:]))
        V previous = values.remove(key)
        property.set(values)
        previous
    }

    @Override
    void clear() {
        property.set([:])
    }

    @Override
    Set<Map.Entry<K, V>> entrySet() {
        MutableMapPropertyView<K, V> owner = this
        new AbstractSet<Map.Entry<K, V>>() {
            @Override
            int size() { owner.snapshot().size() }

            @Override
            Iterator<Map.Entry<K, V>> iterator() {
                List<K> keys = new ArrayList<>(owner.snapshot().keySet())
                new Iterator<Map.Entry<K, V>>() {
                    int index = 0
                    K lastKey
                    boolean canRemove = false

                    @Override
                    boolean hasNext() { index < keys.size() }

                    @Override
                    Map.Entry<K, V> next() {
                        if (!hasNext()) throw new NoSuchElementException()
                        K key = keys.get(index++)
                        lastKey = key
                        canRemove = true
                        new AbstractMap.SimpleEntry<K, V>(key, owner.get(key)) {
                            @Override
                            V setValue(V value) {
                                V previous = owner.put(key, value)
                                super.setValue(value)
                                previous
                            }
                        }
                    }

                    @Override
                    void remove() {
                        if (!canRemove) throw new IllegalStateException()
                        owner.remove(lastKey)
                        canRemove = false
                    }
                }
            }
        }
    }
}
