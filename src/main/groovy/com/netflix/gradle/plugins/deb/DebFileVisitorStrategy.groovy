package com.netflix.gradle.plugins.deb

import com.netflix.gradle.plugins.utils.JavaNIOUtils
import org.gradle.api.file.FileCopyDetails
import org.vafer.jdeb.DataProducer
import org.vafer.jdeb.producers.DataProducerLink

import java.nio.file.Path

import static com.netflix.gradle.plugins.utils.GradleUtils.getRootPath
import static com.netflix.gradle.plugins.utils.GradleUtils.relativizeSymlink

class DebFileVisitorStrategy {
    protected final List<DataProducer> dataProducers
    protected final List<DebCopyAction.InstallDir> installDirs
    private final Set<Tuple2<String, String>> links = new LinkedHashSet<>()

    DebFileVisitorStrategy(List<DataProducer> dataProducers, List<DebCopyAction.InstallDir> installDirs) {
        this.dataProducers = dataProducers
        this.installDirs = installDirs
    }

    void addFile(FileCopyDetails details, File source, String user, int uid, String group, int gid, int mode) {
        try {
            File file = details.file
            File parentLink = JavaNIOUtils.parentSymbolicLink(file)
            if (parentLink != null) {
                def link = relativizeSymlink(details, parentLink)
                if (link != null) {
                    addProducerLink(link)
                    return
                }
            } else if (JavaNIOUtils.isSymbolicLink(file)) {
                def link = relativizeSymlink(details, file)
                if (link != null) {
                    addProducerLink(link)
                    return
                }
            }
            addProducerFile(details, source, user, uid, group, gid, mode)
        }
        catch (UnsupportedOperationException ignored) {
            // For file details that have filters, accessing the file throws this exception
            addProducerFile(details, source, user, uid, group, gid, mode)
        }
    }

    void addDirectory(FileCopyDetails details, String user, int uid, String group, int gid, int mode) {
        try {
            File file = details.file
            if (JavaNIOUtils.isSymbolicLink(file)) {
                def link = relativizeSymlink(details, file)
                if (link != null) {
                    addProducerLink(link)
                    return
                }
            }
            if (JavaNIOUtils.parentSymbolicLink(file) == null) {
                addProducerDirectoryAndInstallDir(details, user, uid, group, gid, mode)
            }
        }
        catch (UnsupportedOperationException ignored) {
            // For file details that have filters, accessing the directory throws this exception
            addProducerDirectoryAndInstallDir(details, user, uid, group, gid, mode)
        }
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

    private void addProducerLink(Tuple2<String, String> link) {
        if (links.add(link)) {
            dataProducers << new DataProducerLink(link.first, link.second, true, null, null, null)
        }
    }
}
