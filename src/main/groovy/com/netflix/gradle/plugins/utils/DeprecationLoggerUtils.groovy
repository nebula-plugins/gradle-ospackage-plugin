package com.netflix.gradle.plugins.utils

import groovy.transform.CompileDynamic
import org.gradle.api.GradleException
import org.gradle.util.GradleVersion

import java.lang.reflect.InvocationTargetException

@CompileDynamic
class DeprecationLoggerUtils {

    private static final String LEGACY_DEPRECATION_LOGGER_CLASS = 'org.gradle.util.DeprecationLogger'

    private static final String DEPRECATION_LOGGER_CLASS = 'org.gradle.internal.deprecation.DeprecationLogger'

    static void whileDisabled(Runnable action) {
        String gradleDeprecationLoggerClassName = (GradleVersion.current() >= GradleVersion.version('6.2') || GradleVersion.current().version.startsWith('6.2')) ? DEPRECATION_LOGGER_CLASS : LEGACY_DEPRECATION_LOGGER_CLASS
        try {
            Class clazz = Class.forName(gradleDeprecationLoggerClassName)
            clazz.getMethod("whileDisabled", Runnable).invoke(this, action)
        } catch(ClassNotFoundException e) {
            throw new GradleException("Could not execute whileDisabled runnable action for $gradleDeprecationLoggerClassName | $e.message", e)
        } catch(InvocationTargetException e) {
            throw e.targetException
        }
    }
}
