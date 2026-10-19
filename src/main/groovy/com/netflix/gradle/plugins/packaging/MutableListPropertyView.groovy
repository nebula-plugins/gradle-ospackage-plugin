package com.netflix.gradle.plugins.packaging

import org.gradle.api.provider.ListProperty

/** A live List facade for the collection API exposed before version 13. */
class MutableListPropertyView<T> extends AbstractList<T> {
    private final ListProperty<T> property

    MutableListPropertyView(ListProperty<T> property) {
        this.property = property
    }

    @Override
    T get(int index) {
        property.getOrElse([]).get(index)
    }

    @Override
    int size() {
        property.getOrElse([]).size()
    }

    @Override
    void add(int index, T element) {
        List<T> values = new ArrayList<>(property.getOrElse([]))
        values.add(index, element)
        property.set(values)
        modCount++
    }

    @Override
    T set(int index, T element) {
        List<T> values = new ArrayList<>(property.getOrElse([]))
        T previous = values.set(index, element)
        property.set(values)
        previous
    }

    @Override
    T remove(int index) {
        List<T> values = new ArrayList<>(property.getOrElse([]))
        T previous = values.remove(index)
        property.set(values)
        modCount++
        previous
    }
}
