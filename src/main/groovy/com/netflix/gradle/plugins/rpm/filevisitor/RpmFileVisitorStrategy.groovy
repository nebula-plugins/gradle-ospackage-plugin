package com.netflix.gradle.plugins.rpm.filevisitor

import org.freecompany.redline.payload.Directive
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal

interface RpmFileVisitorStrategy {
    void addFile(FileCopyDetailsInternal fileDetails, File source, int mode, int dirmode, Directive directive, String uname, String gname, boolean addParents)
    void addDirectory(FileCopyDetailsInternal dirDetails, int permissions, Directive directive, String uname, String gname, boolean addParents)
}
