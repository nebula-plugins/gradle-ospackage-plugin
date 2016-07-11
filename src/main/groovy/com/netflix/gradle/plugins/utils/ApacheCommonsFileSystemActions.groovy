package com.netflix.gradle.plugins.utils

import org.apache.commons.io.FileUtils

class ApacheCommonsFileSystemActions implements FileSystemActions {
    @Override
    void copy(File from, File to) {
        FileUtils.copyFile(from, to)
    }
}
