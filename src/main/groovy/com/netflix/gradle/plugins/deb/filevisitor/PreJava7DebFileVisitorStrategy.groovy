package com.netflix.gradle.plugins.deb.filevisitor

import com.netflix.gradle.plugins.deb.DataProducerDirectorySimple
import com.netflix.gradle.plugins.deb.DataProducerFileSimple
import com.netflix.gradle.plugins.deb.DebCopyAction
import org.gradle.api.file.FileCopyDetails
import org.vafer.jdeb.DataProducer

import static com.netflix.gradle.plugins.utils.FileCopyDetailsUtils.getRootPath

class PreJava7DebFileVisitorStrategy implements DebFileVisitorStrategy {
    private final List<DataProducer> dataProducers
    private final List<DebCopyAction.InstallDir> installDirs

    PreJava7DebFileVisitorStrategy(List<DataProducer> dataProducers, List<DebCopyAction.InstallDir> installDirs) {
        this.dataProducers = dataProducers
        this.installDirs = installDirs
    }

    @Override
    void addFile(FileCopyDetails fileDetails, File source, String user, int uid, String group, int gid, int mode) {
        dataProducers << new DataProducerFileSimple(getRootPath(fileDetails), source, user, uid, group, gid, mode)
    }

    @Override
    void addDirectory(FileCopyDetails dirDetails, String user, int uid, String group, int gid, int mode) {
        String rootPath = getRootPath(dirDetails)
        dataProducers << new DataProducerDirectorySimple(rootPath, user, uid, group, gid, mode)

        // addParentDirs is implicit in jdeb, I think.
        installDirs << new DebCopyAction.InstallDir(
                name: rootPath,
                user: user,
                group: group,
        )
    }
}
