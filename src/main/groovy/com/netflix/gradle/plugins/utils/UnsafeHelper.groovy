package com.netflix.gradle.plugins.utils

import sun.misc.Unsafe

import java.lang.reflect.Field

@Deprecated // This is not used anymore
class UnsafeHelper {
    private static final Unsafe getUnsafe() {
        Field field = Class.forName('sun.misc.Unsafe').getDeclaredField('theUnsafe')
        field.setAccessible(true)
        field.get(null) as Unsafe
    }

    static void monkeyPatchField(Field field, Object newObject) {
        def base = unsafe.staticFieldBase(field)
        def offset = unsafe.staticFieldOffset(field)
        unsafe.putObject(base, offset, newObject)
    }
}
