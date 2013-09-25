package com.trigonic.gradle.plugins.packaging

import org.freecompany.redline.payload.Directive
import org.gradle.api.internal.file.copy.CopySpecInternal

import java.util.logging.Logger

/**
 * Try to mark up CopySpec
 */
@Category(CopySpecInternal)
/**
 * CopySpec will nest in into() blocks, and Gradle will instantiate DefaultCopySpec itself, we have no ability to inject
 * our own. When appending another copy spec to the task, it'll be created a WrapperCopySpec. In 1.8, there's a slight
 * possibility that we override addChild so we can provide our own instances, or we mod the instantiator.
 *
 * Support some additional properties on CopySpecs. Using CopySpecInternal since its the parent of both DefaultCopySpec
 * and RelativizedCopySpec. The old way used the metaClass on the Class, which effected all CopySpecs in the build. This
 * limits the scope to just without our from/into/rename methods. This style also gives us type safety.
 *
 * It's implemented so that either format of setting is supported, e.g. the Gradle "name value" form or the typical
 * assignment form "name = value".
 *
 */
class CopySpecEnhancement {
    private static final Logger LOGGER = Logger.getLogger(CopySpecEnhancement.getName())

    static void user(CopySpecInternal spec, String user) {
        spec.metaClass.user = user
    }

    static void setUser(CopySpecInternal spec, String user) {
        spec.metaClass.user = user
    }

    static void permissionGroup(CopySpecInternal spec, String permissionGroup) {
        spec.metaClass.permissionGroup = permissionGroup
    }

    static void setPermissionGroup(CopySpecInternal spec, String permissionGroup) {
        spec.metaClass.permissionGroup = permissionGroup
    }

    /**
     * RPM Only
     */
    static void fileType(CopySpecInternal spec, Directive fileType) {
        spec.metaClass.fileType = fileType
    }

    /**
     * RPM Only
     */
    static void setfileType(CopySpecInternal spec, Directive fileType) {
        spec.metaClass.fileType = fileType
    }

    /**
     * RPM Only
     */
    static void addParentDirs(CopySpecInternal spec, boolean addParentDirs) {
        spec.metaClass.addParentDirs = addParentDirs
    }

    /**
     * RPM Only
     */
    static void setAddParentDirs(CopySpecInternal spec, boolean addParentDirs) {
        spec.metaClass.addParentDirs = addParentDirs
    }

    /**
     * RPM Only
     */
    static void createDirectoryEntry(CopySpecInternal spec, boolean createDirectoryEntry) {
        spec.metaClass.createDirectoryEntry = createDirectoryEntry
    }

    /**
     * RPM Only
     */
    static void setCreateDirectoryEntry(CopySpecInternal spec, boolean createDirectoryEntry) {
        spec.metaClass.createDirectoryEntry = createDirectoryEntry
    }

    /**
     * DEB Only
     */
    static void uid(CopySpecInternal spec, int uid) {
        spec.metaClass.uid = uid
    }

    /**
     * DEB Only
     */
    static void setUid(CopySpecInternal spec, int uid) {
        spec.metaClass.uid = uid
    }

    /**
     * DEB Only
     */
    static void gid(CopySpecInternal spec, int gid) {
        spec.metaClass.gid = gid
    }

    /**
     * DEB Only
     */
    static void setGid(CopySpecInternal spec, int gid) {
        spec.metaClass.gid = gid
    }
}