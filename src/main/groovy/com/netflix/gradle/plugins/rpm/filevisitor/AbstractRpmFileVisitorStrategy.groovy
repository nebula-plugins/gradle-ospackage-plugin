package com.netflix.gradle.plugins.rpm.filevisitor

import org.freecompany.redline.Builder
import org.freecompany.redline.payload.Directive
import org.gradle.api.file.FileCopyDetails

import static com.netflix.gradle.plugins.utils.FileCopyDetailsUtils.getRootPath

abstract class AbstractRpmFileVisitorStrategy implements RpmFileVisitorStrategy {
    protected final Builder builder

    AbstractRpmFileVisitorStrategy(Builder builder) {
        this.builder = builder
    }

    protected void addFileToBuilder(FileCopyDetails details, File source, int mode, int dirmode, Directive directive, String uname, String gname, boolean addParents) {
        builder.addFile(getRootPath(details), source, mode, dirmode, directive, uname, gname, addParents)
    }

    protected void addDirectoryToBuilder(FileCopyDetails details, int permissions, Directive directive, String uname, String gname, boolean addParents) {
        builder.addDirectory(getRootPath(details), permissions, directive, uname, gname, addParents)
    }
}
