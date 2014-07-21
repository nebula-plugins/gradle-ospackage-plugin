package com.netflix.gradle.plugins.deb.filevisitor

import com.netflix.gradle.plugins.deb.DataProducerDirectorySimple
import com.netflix.gradle.plugins.deb.DataProducerFileSimple
import com.netflix.gradle.plugins.deb.DebCopyAction
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.vafer.jdeb.DataProducer

class PreJava7DebFileVisitorStrategy implements DebFileVisitorStrategy {
    private final List<DataProducer> dataProducers
    private final List<DebCopyAction.InstallDir> installDirs

    PreJava7DebFileVisitorStrategy(List<DataProducer> dataProducers, List<DebCopyAction.InstallDir> installDirs) {
        this.dataProducers = dataProducers
        this.installDirs = installDirs
    }

    @Override
    void addFile(FileCopyDetailsInternal fileDetails, File source, String user, int uid, String group, int gid, int mode) {
        dataProducers << new DataProducerFileSimple("/" + fileDetails.relativePath.pathString, source, user, uid, group, gid, mode)
    }

    @Override
    void addDirectory(FileCopyDetailsInternal dirDetails, String user, int uid, String group, int gid, int mode) {
        String dirName =  "/" + dirDetails.relativePath.pathString
        dataProducers << new DataProducerDirectorySimple(dirName, user, uid, group, gid, mode)

        // addParentDirs is implicit in jdeb, I think.
        installDirs << new DebCopyAction.InstallDir(
                name: "/" + dirDetails.relativePath.pathString,
                user: user,
                group: group,
        )
    }
}
