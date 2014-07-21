package com.netflix.gradle.plugins.deb.filevisitor

import com.netflix.gradle.plugins.deb.DataProducerDirectorySimple
import com.netflix.gradle.plugins.deb.DataProducerFileSimple
import com.netflix.gradle.plugins.deb.DebCopyAction
import com.netflix.gradle.plugins.utils.JavaNIOUtils
import org.gradle.api.file.FileCopyDetails
import org.vafer.jdeb.DataProducer
import org.vafer.jdeb.producers.DataProducerLink

import java.nio.file.Path

class Java7AndHigherDebFileVisitorStrategy implements DebFileVisitorStrategy {
    private final List<DataProducer> dataProducers
    private final List<DebCopyAction.InstallDir> installDirs

    Java7AndHigherDebFileVisitorStrategy(List<DataProducer> dataProducers, List<DebCopyAction.InstallDir> installDirs) {
        this.dataProducers = dataProducers
        this.installDirs = installDirs
    }

    @Override
    void addFile(FileCopyDetails fileDetails, File source, String user, int uid, String group, int gid, int mode) {
        try {
            if(!JavaNIOUtils.isSymbolicLink(fileDetails.file.parentFile)) {
                dataProducers << new DataProducerFileSimple("/" + fileDetails.path, source, user, uid, group, gid, mode)
            }
        }
        catch(UnsupportedOperationException e) {
            // For file details that have filters, accessing the file throws this exception
            dataProducers << new DataProducerFileSimple("/" + fileDetails.path, source, user, uid, group, gid, mode)
        }
    }

    @Override
    void addDirectory(FileCopyDetails dirDetails, String user, int uid, String group, int gid, int mode) {
        boolean symbolicLink = JavaNIOUtils.isSymbolicLink(dirDetails.file)

        if(symbolicLink) {
            Path path = JavaNIOUtils.createPath(dirDetails.file.path)
            Path target = JavaNIOUtils.readSymbolicLink(path)
            dataProducers << new DataProducerLink("/" + dirDetails.path, target.toFile().path, true, null, null, null)
        }
        else {
            String dirName =  "/" + dirDetails.path
            dataProducers << new DataProducerDirectorySimple(dirName, user, uid, group, gid, mode)

            // addParentDirs is implicit in jdeb, I think.
            installDirs << new DebCopyAction.InstallDir(
                    name: "/" + dirDetails.path,
                    user: user,
                    group: group,
            )
        }
    }
}
