package com.netflix.gradle.plugins.utils

import sun.misc.Unsafe

import java.lang.reflect.Field

class UnsafeHelper {
    private static final Unsafe getUnsafe() {
        Field field = Class.forName('sun.misc.Unsafe').getDeclaredField('theUnsafe')
        field.setAccessible(true)
        field.get(null) as Unsafe
    }

    public static void monkeyPatchField(Field field, Object newObject) {
        def base = unsafe.staticFieldBase(field)
        def offset = unsafe.staticFieldOffset(field)
        unsafe.putObject(base, offset, newObject)
    }
}
