package com.netflix.gradle.plugins.utils

import org.gradle.api.DomainObjectCollection
import org.gradle.api.internal.DefaultDomainObjectCollection
import org.gradle.api.internal.collections.ListElementSource
import org.gradle.util.GradleVersion

class BackwardsCompatibleDomainObjectCollectionFactory<T> {
    private static final GradleVersion FOUR_SEVEN = GradleVersion.version("4.7")
    private final GradleVersion gradleVersion

    BackwardsCompatibleDomainObjectCollectionFactory(String gradleVersion) {
        this.gradleVersion = GradleVersion.version(gradleVersion)
    }

    DomainObjectCollection<T> create(Class<T> klass) {
        if (gradleVersion > FOUR_SEVEN) {
            return new DefaultDomainObjectCollection<T>(klass, new ListElementSource<T>())
        }
        return new DefaultDomainObjectCollection<T>(klass, [])
    }

    DomainObjectCollection<T> create(Class<T> klass, Iterable<? super T> backing) {
        if (gradleVersion > FOUR_SEVEN) {
            ListElementSource elements = new ListElementSource<T>()
            backing.each {
                elements.add(it)
            }
            return new DefaultDomainObjectCollection<T>(klass, elements)
        }
        return new DefaultDomainObjectCollection<T>(klass, backing)
    }
}
