package com.netflix.gradle.plugins.utils

import org.gradle.api.DomainObjectCollection
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.DefaultCollectionCallbackActionDecorator
import org.gradle.api.internal.DefaultDomainObjectCollection
import org.gradle.api.internal.collections.ListElementSource
import org.gradle.configuration.internal.UserCodeApplicationContext
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.util.GradleVersion

class BackwardsCompatibleDomainObjectCollectionFactory<T> {
    private static final GradleVersion FOUR_SEVEN = GradleVersion.version("4.7")
    private static final GradleVersion FIVE_ZERO = GradleVersion.version("5.0")
    private final GradleVersion gradleVersion
    private final CollectionCallbackActionDecorator callbackActionDecorator

    BackwardsCompatibleDomainObjectCollectionFactory(String gradleVersion, BuildOperationExecutor buildOperationExecutor, UserCodeApplicationContext userCodeApplicationContext) {
        this.gradleVersion = GradleVersion.version(gradleVersion)
        this.callbackActionDecorator = new DefaultCollectionCallbackActionDecorator(buildOperationExecutor, userCodeApplicationContext)
    }

    DomainObjectCollection<T> create(Class<T> klass) {
        if (gradleVersion > FIVE_ZERO) {
            return new DefaultDomainObjectCollection<T>(klass, new ListElementSource<T>(), callbackActionDecorator)
        } else  if (gradleVersion > FOUR_SEVEN) {
            return new DefaultDomainObjectCollection<T>(klass, new ListElementSource<T>())
        } else {
            return new DefaultDomainObjectCollection<T>(klass, [])
        }
    }

    DomainObjectCollection<T> create(Class<T> klass, Iterable<? super T> backing) {
        if (gradleVersion > FIVE_ZERO) {
            ListElementSource elements = new ListElementSource<T>()
            backing.each {
                elements.add(it)
            }
            return new DefaultDomainObjectCollection<T>(klass, elements, callbackActionDecorator)
        } else if (gradleVersion > FOUR_SEVEN) {
            ListElementSource elements = new ListElementSource<T>()
            backing.each {
                elements.add(it)
            }
            return new DefaultDomainObjectCollection<T>(klass, elements)
        } else {
            return new DefaultDomainObjectCollection<T>(klass, backing)

        }
    }
}
