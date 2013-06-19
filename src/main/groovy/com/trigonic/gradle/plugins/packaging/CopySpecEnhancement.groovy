package com.trigonic.gradle.plugins.packaging

import org.freecompany.redline.payload.Directive
import org.gradle.api.internal.file.copy.ReadableCopySpec

import java.util.logging.Logger

/**
 * Try to mark up CopySpec
 */
@Category(ReadableCopySpec)
/**
 * CopySpec will nest in into() blocks, and Gradle will instantiate CopySpecImpl itself, we have no ability to inject
 * our own. When appending another copy spec to the task, it'll be created a WrapperCopySpec

 * Support some additional properties on CopySpecs. Using ReadableCopySpec since its the parent of both CopySpecImpl
 * and WrapperCopySpec. The old way used the metaClass on the Class, which effected all CopySpecs in the build. This
 * limits the scope to just without our from/into/rename methods. This style also gives us type safety.
 *
 * It's implemented so that either format of setting is supported, e.g. the Gradle "name value" form or the typical
 * assignment form "name = value".
 *
 */
class CopySpecEnhancement {
    private static final Logger LOGGER = Logger.getLogger(CopySpecEnhancement.getName())

    static void user(ReadableCopySpec spec, String user) {
        spec.metaClass.user = user
    }

    static void setUser(ReadableCopySpec spec, String user) {
        spec.metaClass.user = user
    }

    static void permissionGroup(ReadableCopySpec spec, String permissionGroup) {
        spec.metaClass.permissionGroup = permissionGroup
    }

    static void setPermissionGroup(ReadableCopySpec spec, String permissionGroup) {
        spec.metaClass.permissionGroup = permissionGroup
    }

    /**
     * RPM Only
     */
    static void fileType(ReadableCopySpec spec, Directive fileType) {
        spec.metaClass.fileType = fileType
    }

    /**
     * RPM Only
     */
    static void setfileType(ReadableCopySpec spec, Directive fileType) {
        spec.metaClass.fileType = fileType
    }

    /**
     * RPM Only
     */
    static void addParentDirs(ReadableCopySpec spec, boolean addParentDirs) {
        spec.metaClass.addParentDirs = addParentDirs
    }

    /**
     * RPM Only
     */
    static void setAddParentDirs(ReadableCopySpec spec, boolean addParentDirs) {
        spec.metaClass.addParentDirs = addParentDirs
    }

    /**
     * RPM Only
     */
    static void createDirectoryEntry(ReadableCopySpec spec, boolean createDirectoryEntry) {
        spec.metaClass.createDirectoryEntry = createDirectoryEntry
    }

    /**
     * RPM Only
     */
    static void setCreateDirectoryEntry(ReadableCopySpec spec, boolean createDirectoryEntry) {
        spec.metaClass.createDirectoryEntry = createDirectoryEntry
    }

    /**
     * DEB Only
     */
    static void uid(ReadableCopySpec spec, int uid) {
        spec.metaClass.uid = uid
    }

    /**
     * DEB Only
     */
    static void setUid(ReadableCopySpec spec, int uid) {
        spec.metaClass.uid = uid
    }

    /**
     * DEB Only
     */
    static void gid(ReadableCopySpec spec, int gid) {
        spec.metaClass.gid = gid
    }

    /**
     * DEB Only
     */
    static void setGid(ReadableCopySpec spec, int gid) {
        spec.metaClass.gid = gid
    }
}