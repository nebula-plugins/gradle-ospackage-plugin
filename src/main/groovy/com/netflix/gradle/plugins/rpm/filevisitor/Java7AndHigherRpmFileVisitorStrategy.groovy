package com.netflix.gradle.plugins.rpm.filevisitor

import com.netflix.gradle.plugins.utils.JavaNIOUtils
import org.freecompany.redline.Builder
import org.freecompany.redline.payload.Directive
import org.gradle.api.file.FileCopyDetails

import java.nio.file.Path

import static com.netflix.gradle.plugins.utils.FileCopyDetailsUtils.getRootPath

class Java7AndHigherRpmFileVisitorStrategy extends AbstractRpmFileVisitorStrategy {
    Java7AndHigherRpmFileVisitorStrategy(Builder builder) {
        super(builder)
    }

    @Override
    void addFile(FileCopyDetails details, File source, int mode, int dirmode, Directive directive, String uname, String gname, boolean addParents) {
        try {
            if(!JavaNIOUtils.isSymbolicLink(details.file.parentFile)) {
                addFileToBuilder(details, source, mode, dirmode, directive, uname, gname, addParents)
            }
        }
        catch(UnsupportedOperationException e) {
            // For file details that have filters, accessing the file throws this exception
            addFileToBuilder(details, source, mode, dirmode, directive, uname, gname, addParents)
        }
    }

    @Override
    void addDirectory(FileCopyDetails details, int permissions, Directive directive, String uname, String gname, boolean addParents) {
        boolean symbolicLink = JavaNIOUtils.isSymbolicLink(details.file)

        if(symbolicLink) {
            addLinkToBuilder(details)
        }
        else {
            addDirectoryToBuilder(details, permissions, directive, uname, gname, addParents)
        }
    }

    private void addLinkToBuilder(FileCopyDetails details) {
        Path path = JavaNIOUtils.createPath(details.file.path)
        Path target = JavaNIOUtils.readSymbolicLink(path)
        builder.addLink(getRootPath(details), target.toFile().path)
    }
}
