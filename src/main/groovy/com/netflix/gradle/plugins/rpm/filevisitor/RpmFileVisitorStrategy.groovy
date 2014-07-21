package com.netflix.gradle.plugins.rpm.filevisitor

import org.freecompany.redline.payload.Directive
import org.gradle.api.file.FileCopyDetails

interface RpmFileVisitorStrategy {
    void addFile(FileCopyDetails fileDetails, File source, int mode, int dirmode, Directive directive, String uname, String gname, boolean addParents)
    void addDirectory(FileCopyDetails dirDetails, int permissions, Directive directive, String uname, String gname, boolean addParents)
}
