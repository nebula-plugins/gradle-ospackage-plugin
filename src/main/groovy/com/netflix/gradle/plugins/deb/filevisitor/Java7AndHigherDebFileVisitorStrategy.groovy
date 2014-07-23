package com.netflix.gradle.plugins.deb.filevisitor

import com.netflix.gradle.plugins.deb.DebCopyAction
import com.netflix.gradle.plugins.utils.JavaNIOUtils
import org.gradle.api.file.FileCopyDetails
import org.vafer.jdeb.DataProducer
import org.vafer.jdeb.producers.DataProducerLink

import java.nio.file.Path

import static com.netflix.gradle.plugins.utils.FileCopyDetailsUtils.getRootPath

class Java7AndHigherDebFileVisitorStrategy extends AbstractDebFileVisitorStrategy {
    Java7AndHigherDebFileVisitorStrategy(List<DataProducer> dataProducers, List<DebCopyAction.InstallDir> installDirs) {
        super(dataProducers, installDirs)
    }

    @Override
    void addFile(FileCopyDetails details, File source, String user, int uid, String group, int gid, int mode) {
        try {
            if(!JavaNIOUtils.isSymbolicLink(details.file.parentFile)) {
                addProducerFile(details, source, user, uid, group, gid, mode)
            }
        }
        catch(UnsupportedOperationException e) {
            // For file details that have filters, accessing the file throws this exception
            addProducerFile(details, source, user, uid, group, gid, mode)
        }
    }

    @Override
    void addDirectory(FileCopyDetails details, String user, int uid, String group, int gid, int mode) {
        boolean symbolicLink = JavaNIOUtils.isSymbolicLink(details.file)

        if(symbolicLink) {
            addProducerLink(details)
        }
        else {
            addProducerDirectoryAndInstallDir(details, user, uid, group, gid, mode)
        }
    }

    private void addProducerLink(FileCopyDetails details) {
        String rootPath = getRootPath(details)
        Path path = JavaNIOUtils.createPath(details.file.path)
        Path target = JavaNIOUtils.readSymbolicLink(path)
        dataProducers << new DataProducerLink(rootPath, target.toFile().path, true, null, null, null)
    }
}
