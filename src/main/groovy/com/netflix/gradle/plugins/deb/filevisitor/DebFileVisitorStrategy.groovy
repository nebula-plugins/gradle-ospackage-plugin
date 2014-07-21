package com.netflix.gradle.plugins.deb.filevisitor

import org.gradle.api.internal.file.copy.FileCopyDetailsInternal

interface DebFileVisitorStrategy {
    void addFile(FileCopyDetailsInternal fileDetails, File source, String user, int uid, String group, int gid, int mode)
    void addDirectory(FileCopyDetailsInternal dirDetails, String user, int uid, String group, int gid, int mode)
}
