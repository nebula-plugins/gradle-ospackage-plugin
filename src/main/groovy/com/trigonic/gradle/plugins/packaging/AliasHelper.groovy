package com.trigonic.gradle.plugins.packaging

import org.gradle.api.internal.DynamicObject
import org.gradle.api.internal.DynamicObjectAware
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension

import java.lang.reflect.Field
import java.lang.reflect.Modifier

class AliasHelper {
    static <T extends Enum<T>> void aliasEnumValues(T[] values, dynAware) {
        //ExtraPropertiesExtension ext = dynAware.getExtensions()
        for (T value : values) {
            assert !dynAware.ext.hasProperty(value.name())
            dynAware.ext.set value.name(), value
        }
    }

    static <T> void aliasStaticInstances(Class<T> forClass, ExtensionAware dynAware) {
        aliasStaticInstances(forClass, forClass, dynAware)
    }

    static boolean hasModifier(Field field, int modifier) {
        (field.modifiers & modifier) == modifier
    }

    static <T, U> void aliasStaticInstances(Class<T> forClass, Class<U> ofClass, dynAware) {
        for (Field field : forClass.fields) {
            if (field.type == ofClass && hasModifier(field, Modifier.STATIC)) {
                assert !dynAware.ext.hasProperty(field.name)
                dynAware.ext.set field.name, field.get(null)
            }
        }
    }

}
