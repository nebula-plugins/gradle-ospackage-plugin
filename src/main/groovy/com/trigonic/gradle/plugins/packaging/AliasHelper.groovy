package com.trigonic.gradle.plugins.packaging

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.lang.reflect.Field
import java.lang.reflect.Modifier

class AliasHelper {
    private static Logger logger = Logging.getLogger(AliasHelper);

    static <T extends Enum<T>> void aliasEnumValues(T[] values, dynAware) {
        for (T value : values) {
            assert !dynAware.hasProperty(value.name())
            logger.info("Setting ${value.name()} onto ${dynAware}")
            dynAware.metaClass."${value.name()}" = value
        }
    }

    static <T> void aliasStaticInstances(Class<T> forClass, dynAware) {
        aliasStaticInstances(forClass, forClass, dynAware)
    }

    static boolean hasModifier(Field field, int modifier) {
        (field.modifiers & modifier) == modifier
    }

    static <T, U> void aliasStaticInstances(Class<T> forClass, Class<U> ofClass, dynAware) {
        for (Field field : forClass.fields) {
            if (field.type == ofClass && hasModifier(field, Modifier.STATIC)) {
                assert !dynAware.hasProperty(field.name)
                logger.info("Setting ${field.name} to ${field.get(null)} onto ${dynAware}")
                dynAware.metaClass."${field.name}" = field.get(null)
            }
        }
    }

}
