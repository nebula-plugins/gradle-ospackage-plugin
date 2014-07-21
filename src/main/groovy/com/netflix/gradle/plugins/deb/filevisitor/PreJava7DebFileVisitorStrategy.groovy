package com.netflix.gradle.plugins.deb.filevisitor

import com.netflix.gradle.plugins.deb.DebCopyAction
import org.gradle.api.file.FileCopyDetails
import org.vafer.jdeb.DataProducer

class PreJava7DebFileVisitorStrategy extends AbstractDebFileVisitorStrategy {
    PreJava7DebFileVisitorStrategy(List<DataProducer> dataProducers, List<DebCopyAction.InstallDir> installDirs) {
        super(dataProducers, installDirs)
    }

    @Override
    void addFile(FileCopyDetails details, File source, String user, int uid, String group, int gid, int mode) {
        addProducerFile(details, source, user, uid, group, gid, mode)
    }

    @Override
    void addDirectory(FileCopyDetails details, String user, int uid, String group, int gid, int mode) {
        addProducerDirectoryAndInstallDir(details, user, uid, group, gid, mode)
    }
}
