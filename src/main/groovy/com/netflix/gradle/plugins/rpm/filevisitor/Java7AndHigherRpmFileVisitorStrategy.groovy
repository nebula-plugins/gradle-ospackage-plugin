package com.netflix.gradle.plugins.rpm.filevisitor

import com.netflix.gradle.plugins.utils.JavaNIOUtils
import org.redline_rpm.Builder
import org.redline_rpm.payload.Directive
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
        try {
            if(JavaNIOUtils.isSymbolicLink(details.file)) {
                addLinkToBuilder(details)
            }
            else {
                addDirectoryToBuilder(details, permissions, directive, uname, gname, addParents)
            }
        }
        catch(UnsupportedOperationException e) {
            // For file details that have filters, accessing the directory throws this exception
            addDirectoryToBuilder(details, permissions, directive, uname, gname, addParents)
        }
    }

    private void addLinkToBuilder(FileCopyDetails details) {
        Path path = JavaNIOUtils.createPath(details.file.path)
        Path target = JavaNIOUtils.readSymbolicLink(path)
        builder.addLink(getRootPath(details), target.toFile().path)
    }
}
