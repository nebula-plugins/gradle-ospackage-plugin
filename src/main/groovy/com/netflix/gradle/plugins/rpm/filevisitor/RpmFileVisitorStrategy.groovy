package com.netflix.gradle.plugins.rpm.filevisitor

import org.redline_rpm.payload.Directive
import org.gradle.api.file.FileCopyDetails

interface RpmFileVisitorStrategy {
    void addFile(FileCopyDetails details, File source, int mode, int dirmode, Directive directive, String uname, String gname, boolean addParents)
    void addDirectory(FileCopyDetails details, int permissions, Directive directive, String uname, String gname, boolean addParents)
}
