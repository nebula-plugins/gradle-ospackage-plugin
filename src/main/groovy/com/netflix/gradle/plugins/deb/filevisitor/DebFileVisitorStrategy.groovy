package com.netflix.gradle.plugins.deb.filevisitor

import org.gradle.api.file.FileCopyDetails

interface DebFileVisitorStrategy {
    void addFile(FileCopyDetails details, File source, String user, int uid, String group, int gid, int mode)
    void addDirectory(FileCopyDetails details, String user, int uid, String group, int gid, int mode)
}
