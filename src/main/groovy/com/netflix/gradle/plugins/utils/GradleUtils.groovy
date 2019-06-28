package com.netflix.gradle.plugins.utils

import org.gradle.api.file.FileCopyDetails

import java.nio.file.Path

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

    static Tuple2<String, String> relativizeSymlink(FileCopyDetails details, File target) {
        String sourcePath = details.file.path
        String sourceBasePath = sourcePath.substring(0, sourcePath.length() - details.relativeSourcePath.pathString.length())
        if (!target.path.startsWith(sourceBasePath)) {
            // target is below sourceBasePath
            return null
        }
        String sourceRelative = target.path.substring(sourceBasePath.length())
        String sourceBase = details.path.substring(0, details.path.indexOf(sourceRelative))

        File targetFile = JavaNIOUtils.readSymbolicLink(target.toPath()).toFile()
        String targetPath = targetFile.isAbsolute() ? targetFile.path : new File(target.parentFile, targetFile.path).getCanonicalPath()

        if (targetPath.startsWith(sourceBasePath)) {
            File sourceRoot = new File("/$sourceBase", sourceRelative)
            File targetRoot = new File("/$sourceBase", targetPath.substring(sourceBasePath.length()))
            Path relativeTarget = sourceRoot.isDirectory() ? sourceRoot.toPath().relativize(targetRoot.toPath()) : sourceRoot.parentFile.toPath().relativize(targetRoot.toPath())
            return new Tuple2(sourceRoot.path, relativeTarget.toString())
        } else {
            return null
        }
    }

    static String quotedIfPresent(String input) {
        input == null ? null : "\'${input}\'"
    }
}
