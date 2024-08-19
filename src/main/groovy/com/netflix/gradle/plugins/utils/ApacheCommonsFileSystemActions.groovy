package com.netflix.gradle.plugins.utils

import org.vafer.jdeb.shaded.commons.io.FileUtils

class ApacheCommonsFileSystemActions implements FileSystemActions {
    @Override
    void copy(File from, File to) {
        FileUtils.copyFile(from, to)
    }
}
