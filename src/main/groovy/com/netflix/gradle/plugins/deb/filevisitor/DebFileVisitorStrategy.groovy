package com.netflix.gradle.plugins.deb.filevisitor

import org.gradle.api.file.FileCopyDetails

interface DebFileVisitorStrategy {
    void addFile(FileCopyDetails fileDetails, File source, String user, int uid, String group, int gid, int mode)
    void addDirectory(FileCopyDetails dirDetails, String user, int uid, String group, int gid, int mode)
}
