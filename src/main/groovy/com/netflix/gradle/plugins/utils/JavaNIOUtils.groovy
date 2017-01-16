package com.netflix.gradle.plugins.utils

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

final class JavaNIOUtils {
    private JavaNIOUtils() {}

    static Path createPath(String filePath) {
        FileSystems.getDefault().getPath(filePath)
    }

    static boolean isSymbolicLink(File file) {
        Path path = createPath(file.path)
        Files.isSymbolicLink(path)
    }

    static Path readSymbolicLink(Path path) {
        Files.readSymbolicLink(path)
    }

    static void createSymblicLink(File source, File target) {
        Path newLink = createPath(source.path)
        Path targetDir = createPath(target.path)
        Files.createSymbolicLink(newLink, targetDir)
    }

    static Path createTempFile(String prefix, String suffix) {
        return Files.createTempFile(prefix, suffix)
    }
}
