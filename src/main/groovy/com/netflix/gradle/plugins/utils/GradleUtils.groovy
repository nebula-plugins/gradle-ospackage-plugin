package com.netflix.gradle.plugins.utils

import org.gradle.api.file.FileCopyDetails

final class GradleUtils {
    private GradleUtils() {}

    static String getRootPath(FileCopyDetails details) {
        "/${details.path}".toString()
    }

    static <T> T lookup(def specToLookAt, String propertyName) {
        if (specToLookAt?.metaClass?.hasProperty(specToLookAt, propertyName) != null) {
            def prop = specToLookAt.metaClass.getProperty(specToLookAt, propertyName)
            if (prop instanceof MetaBeanProperty) {
                return prop?.getProperty(specToLookAt) as T
            } else {
                return prop as T
            }
        } else {
            return null
        }
    }
}
