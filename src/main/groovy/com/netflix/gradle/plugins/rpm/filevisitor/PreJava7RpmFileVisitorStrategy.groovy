package com.netflix.gradle.plugins.rpm.filevisitor

import org.freecompany.redline.Builder
import org.freecompany.redline.payload.Directive
import org.gradle.api.file.FileCopyDetails

import static com.netflix.gradle.plugins.utils.FileCopyDetailsUtils.getRootPath

class PreJava7RpmFileVisitorStrategy implements RpmFileVisitorStrategy {
    private final Builder builder

    PreJava7RpmFileVisitorStrategy(Builder builder) {
        this.builder = builder
    }

    @Override
    void addFile(FileCopyDetails fileDetails, File source, int mode, int dirmode, Directive directive, String uname, String gname, boolean addParents) {
        builder.addFile(getRootPath(fileDetails), source, mode, dirmode, directive, uname, gname, addParents)
    }

    @Override
    void addDirectory(FileCopyDetails dirDetails, int permissions, Directive directive, String uname, String gname, boolean addParents) {
        builder.addDirectory(getRootPath(dirDetails), permissions, directive, uname, gname, addParents)
    }
}
