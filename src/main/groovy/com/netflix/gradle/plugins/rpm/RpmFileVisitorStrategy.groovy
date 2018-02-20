package com.netflix.gradle.plugins.rpm

import com.netflix.gradle.plugins.utils.JavaNIOUtils
import org.gradle.api.file.FileCopyDetails
import org.redline_rpm.Builder
import org.redline_rpm.payload.Directive
import org.vafer.jdeb.producers.DataProducerLink

import java.nio.file.Path

import static com.netflix.gradle.plugins.utils.GradleUtils.getRootPath
import static com.netflix.gradle.plugins.utils.GradleUtils.relativizeSymlink

class RpmFileVisitorStrategy {
    protected final Builder builder

    RpmFileVisitorStrategy(Builder builder) {
        this.builder = builder
    }

    void addFile(FileCopyDetails details, File source, int mode, int dirmode, Directive directive, String uname, String gname, boolean addParents) {
        try {
            File file = details.file
            File parentLink = JavaNIOUtils.parentSymbolicLink(file)
            if (JavaNIOUtils.isSymbolicLink(details.file)) {
                addLinkToBuilder(details, file)
            } else if (parentLink != null) {
                addLinkToBuilder(details, parentLink)
            } else {
                addFileToBuilder(details, source, mode, dirmode, directive, uname, gname, addParents)
            }
        }
        catch (UnsupportedOperationException ignored) {
            // For file details that have filters, accessing the file throws this exception
            addFileToBuilder(details, source, mode, dirmode, directive, uname, gname, addParents)
        }
    }

    void addDirectory(FileCopyDetails details, int permissions, Directive directive, String uname, String gname, boolean addParents) {
        try {
            File file = details.file
            if (JavaNIOUtils.isSymbolicLink(details.file)) {
                addLinkToBuilder(details, file)
            } else {
                addDirectoryToBuilder(details, permissions, directive, uname, gname, addParents)
            }
        }
        catch (UnsupportedOperationException ignore) {
            // For file details that have filters, accessing the directory throws this exception
            addDirectoryToBuilder(details, permissions, directive, uname, gname, addParents)
        }
    }

    protected void addFileToBuilder(FileCopyDetails details, File source, int mode, int dirmode, Directive directive, String uname, String gname, boolean addParents) {
        builder.addFile(getRootPath(details), source, mode, dirmode, directive, uname, gname, addParents)
    }

    protected void addDirectoryToBuilder(FileCopyDetails details, int permissions, Directive directive, String uname, String gname, boolean addParents) {
        builder.addDirectory(getRootPath(details), permissions, directive, uname, gname, addParents)
    }

    private void addLinkToBuilder(FileCopyDetails details, File target) {
        def link = relativizeSymlink(details, target)
        builder.addLink(link.first, link.second)
    }
}
