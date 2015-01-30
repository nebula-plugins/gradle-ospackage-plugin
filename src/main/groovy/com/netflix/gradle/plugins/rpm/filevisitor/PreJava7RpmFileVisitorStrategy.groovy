package com.netflix.gradle.plugins.rpm.filevisitor

import org.redline_rpm.Builder
import org.redline_rpm.payload.Directive
import org.gradle.api.file.FileCopyDetails

class PreJava7RpmFileVisitorStrategy extends AbstractRpmFileVisitorStrategy {
    PreJava7RpmFileVisitorStrategy(Builder builder) {
        super(builder)
    }

    @Override
    void addFile(FileCopyDetails details, File source, int mode, int dirmode, Directive directive, String uname, String gname, boolean addParents) {
        addFileToBuilder(details, source, mode, dirmode, directive, uname, gname, addParents)
    }

    @Override
    void addDirectory(FileCopyDetails details, int permissions, Directive directive, String uname, String gname, boolean addParents) {
        addDirectoryToBuilder(details, permissions, directive, uname, gname, addParents)
    }
}
