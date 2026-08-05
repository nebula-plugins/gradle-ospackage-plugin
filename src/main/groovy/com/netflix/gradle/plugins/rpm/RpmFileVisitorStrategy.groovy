package com.netflix.gradle.plugins.rpm

import com.netflix.gradle.plugins.utils.JavaNIOUtils
import org.gradle.api.file.FileCopyDetails
import org.redline_rpm.Builder
import org.redline_rpm.payload.Directive

import static com.netflix.gradle.plugins.utils.GradleUtils.getRootPath
import static com.netflix.gradle.plugins.utils.GradleUtils.relativizeSymlink

class RpmFileVisitorStrategy {
    protected final Builder builder
    private final Set<Tuple2<String, String>> links = new LinkedHashSet<>()

    RpmFileVisitorStrategy(Builder builder) {
        this.builder = builder
    }

    void addFile(FileCopyDetails details, File source, int mode, int dirmode, Directive directive, String uname, String gname, boolean addParents) {
        try {
            File file = details.file
            File parentLink = JavaNIOUtils.parentSymbolicLink(file)
            if (parentLink != null) {
                def link = relativizeSymlink(details, parentLink)
                if (link != null) {
                    addLinkToBuilder(link, mode)
                    return
                }
            } else if (JavaNIOUtils.isSymbolicLink(details.file)) {
                def link = relativizeSymlink(details, file)
                if (link != null) {
                    addLinkToBuilder(link, mode)
                    return
                }
            }
            addFileToBuilder(details, source, mode, dirmode, directive, uname, gname, addParents)
        }
        catch (UnsupportedOperationException ignored) {
            // For file details that have filters, accessing the file throws this exception
            addFileToBuilder(details, source, mode, dirmode, directive, uname, gname, addParents)
        }
    }

    void addDirectory(FileCopyDetails details, int permissions, Directive directive, String uname, String gname, boolean addParents) {
        try {
            File file = details.file
            if (JavaNIOUtils.isSymbolicLink(details.file)) {
                def link = relativizeSymlink(details, file)
                if (link != null) {
                    addLinkToBuilder(link, permissions)
                    return
                }
            }
            if (JavaNIOUtils.parentSymbolicLink(file) == null) {
                addDirectoryToBuilder(details, permissions, directive, uname, gname, addParents)
            }
        }
        catch (UnsupportedOperationException ignore) {
            // For file details that have filters, accessing the directory throws this exception
            addDirectoryToBuilder(details, permissions, directive, uname, gname, addParents)
        }
    }

    protected void addFileToBuilder(FileCopyDetails details, File source, int mode, int dirmode, Directive directive, String uname, String gname, boolean addParents) {
        builder.addFile(getRootPath(details), source, mode, dirmode, directive, uname, gname, addParents)
    }

    protected void addDirectoryToBuilder(FileCopyDetails details, int permissions, Directive directive, String uname, String gname, boolean addParents) {
        builder.addDirectory(getRootPath(details), permissions, directive, uname, gname, addParents)
    }

    private void addLinkToBuilder(Tuple2<String, String> link, int permissions) {
        if (links.add(link)) {
            builder.addLink(link.first, link.second, permissions)
        }
    }
}
