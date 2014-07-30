package com.netflix.gradle.plugins.utils

import org.gradle.api.file.FileCopyDetails

final class FileCopyDetailsUtils {
    private FileCopyDetailsUtils() {}

    static String getRootPath(FileCopyDetails details) {
        "/${details.path}".toString()
    }
}
