package com.netflix.gradle.plugins.rpm.filevisitor

import com.netflix.gradle.plugins.utils.JavaNIOUtils
import org.freecompany.redline.Builder
import org.freecompany.redline.payload.Directive
import org.gradle.api.file.FileCopyDetails

import java.nio.file.Path

class Java7AndHigherRpmFileVisitorStrategy implements RpmFileVisitorStrategy {
    private final Builder builder

    Java7AndHigherRpmFileVisitorStrategy(Builder builder) {
        this.builder = builder
    }

    @Override
    void addFile(FileCopyDetails fileDetails, File source, int mode, int dirmode, Directive directive, String uname, String gname, boolean addParents) {
        try {
            if(!JavaNIOUtils.isSymbolicLink(fileDetails.file.parentFile)) {
                builder.addFile("/" + fileDetails.path, source, mode, dirmode, directive, uname, gname, addParents)
            }
        }
        catch(UnsupportedOperationException e) {
            // For file details that have filters, accessing the file throws this exception
            builder.addFile("/" + fileDetails.path, source, mode, dirmode, directive, uname, gname, addParents)
        }
    }

    @Override
    void addDirectory(FileCopyDetails dirDetails, int permissions, Directive directive, String uname, String gname, boolean addParents) {
        boolean symbolicLink = JavaNIOUtils.isSymbolicLink(dirDetails.file)

        if(symbolicLink) {
            Path path = JavaNIOUtils.createPath(dirDetails.file.path)
            Path target = JavaNIOUtils.readSymbolicLink(path)
            builder.addLink("/" + dirDetails.path, "/" + target.toFile().path)
        }
        else {
            builder.addDirectory("/" + dirDetails.path, permissions, directive, uname, gname, addParents)
        }
    }
}
