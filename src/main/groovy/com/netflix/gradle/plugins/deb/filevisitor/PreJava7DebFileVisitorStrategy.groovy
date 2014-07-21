package com.netflix.gradle.plugins.deb.filevisitor

import com.netflix.gradle.plugins.deb.DebCopyAction
import org.gradle.api.file.FileCopyDetails
import org.vafer.jdeb.DataProducer

class PreJava7DebFileVisitorStrategy extends AbstractDebFileVisitorStrategy {
    PreJava7DebFileVisitorStrategy(List<DataProducer> dataProducers, List<DebCopyAction.InstallDir> installDirs) {
        super(dataProducers, installDirs)
    }

    @Override
    void addFile(FileCopyDetails fileDetails, File source, String user, int uid, String group, int gid, int mode) {
        addProducerFile(fileDetails, source, user, uid, group, gid, mode)
    }

    @Override
    void addDirectory(FileCopyDetails dirDetails, String user, int uid, String group, int gid, int mode) {
        addProducerDirectoryAndInstallDir(dirDetails, user, uid, group, gid, mode)
    }
}
