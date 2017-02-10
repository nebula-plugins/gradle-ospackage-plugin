package com.netflix.gradle.plugins.utils

import org.gradle.api.file.FileCopyDetails
import org.gradle.api.tasks.AbstractCopyTask

import java.nio.file.Files

class FromConfigurationFactory {

    static Closure<AbstractCopyTask> preserveSymlinks(delegate) {
        return {
            delegate.eachFile { FileCopyDetails details ->
                if (Files.isSymbolicLink(details.file.toPath())) {
                    details.exclude()
                    def toFile = Files.readSymbolicLink(details.file.toPath()).toFile()
                    delegate.link(details.relativePath.toString(), toFile.toString())
                }
            }
        }
    }

}
