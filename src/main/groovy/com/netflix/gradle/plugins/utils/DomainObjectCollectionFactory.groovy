package com.netflix.gradle.plugins.utils

import org.gradle.api.DomainObjectCollection
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.DefaultDomainObjectCollection
import org.gradle.api.internal.collections.ListElementSource

class DomainObjectCollectionFactory<T> {
    private final CollectionCallbackActionDecorator callbackActionDecorator

    DomainObjectCollectionFactory(CollectionCallbackActionDecorator collectionCallbackActionDecorator) {
        this.callbackActionDecorator = collectionCallbackActionDecorator
    }

    DomainObjectCollection<T> create(Class<T> klass) {
        return new DefaultDomainObjectCollection<T>(klass, new ListElementSource<T>(), callbackActionDecorator)
    }

    DomainObjectCollection<T> create(Class<T> klass, Iterable<? super T> backing) {
        ListElementSource elements = new ListElementSource<T>()
        backing.each {
            elements.add(it)
        }
        return new DefaultDomainObjectCollection<T>(klass, elements, callbackActionDecorator)
    }
}
