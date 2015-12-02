package com.netflix.gradle.plugins.deb

import com.netflix.gradle.plugins.utils.JavaNIOUtils
import org.gradle.api.file.FileCopyDetails
import org.vafer.jdeb.DataProducer
import org.vafer.jdeb.producers.DataProducerLink

import java.nio.file.Path

import static com.netflix.gradle.plugins.utils.GradleUtils.getRootPath

class DebFileVisitorStrategy {
    protected final List<DataProducer> dataProducers
    protected final List<DebCopyAction.InstallDir> installDirs

    DebFileVisitorStrategy(List<DataProducer> dataProducers, List<DebCopyAction.InstallDir> installDirs) {
        this.dataProducers = dataProducers
        this.installDirs = installDirs
    }

    protected void addProducerFile(FileCopyDetails fileDetails, File source, String user, int uid, String group, int gid, int mode) {
        dataProducers << new DataProducerFileSimple(getRootPath(fileDetails), source, user, uid, group, gid, mode)
    }

    protected void addProducerDirectoryAndInstallDir(FileCopyDetails dirDetails, String user, int uid, String group, int gid, int mode) {
        String rootPath = getRootPath(dirDetails)
        dataProducers << new DataProducerDirectorySimple(rootPath, user, uid, group, gid, mode)

        // addParentDirs is implicit in jdeb, I think.
        installDirs << new DebCopyAction.InstallDir(
                name: rootPath,
                user: user,
                group: group,
        )
    }

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

    void addDirectory(FileCopyDetails details, String user, int uid, String group, int gid, int mode) {
        try {
            if(JavaNIOUtils.isSymbolicLink(details.file)) {
                addProducerLink(details)
            }
            else {
                addProducerDirectoryAndInstallDir(details, user, uid, group, gid, mode)
            }
        }
        catch(UnsupportedOperationException e) {
            // For file details that have filters, accessing the directory throws this exception
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
