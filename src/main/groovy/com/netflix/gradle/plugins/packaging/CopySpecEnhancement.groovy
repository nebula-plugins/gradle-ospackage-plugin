package com.netflix.gradle.plugins.packaging

import groovy.transform.CompileDynamic
import org.apache.commons.lang3.reflect.FieldUtils
import org.gradle.api.file.DuplicatesStrategy
import org.redline_rpm.payload.Directive
import org.gradle.api.file.CopySpec
import org.gradle.api.internal.file.copy.CopySpecWrapper

import java.util.logging.Logger

/**
 * Try to mark up CopySpec
 */
@Category(CopySpec)
/**
 * CopySpec will nest in into(CopySpec spec, ) blocks, and Gradle will instantiate DefaultCopySpec itself, we have no ability to inject
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
@CompileDynamic
class CopySpecEnhancement {
    private static final Logger LOGGER = Logger.getLogger(CopySpecEnhancement.getName())

    static void appendFieldToCopySpec(CopySpec spec, String fieldName, Object value) {
        def directSpec = spec
        if (spec instanceof CopySpecWrapper) {
            def delegateField = FieldUtils.getField(CopySpecWrapper, 'delegate', true)
            directSpec = delegateField.get(spec)
        }

        LOGGER.info("Adding $fieldName to ${directSpec}")
        directSpec.metaClass["get${fieldName.capitalize()}"] = { value }
        spec.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
    }
    static void user(CopySpec spec, String user) {
        appendFieldToCopySpec(spec, 'user', user)
    }

    static void setUser(CopySpec spec, String userArg) {
        user(spec, userArg)
    }

    static void setgid(CopySpec spec, boolean setgid) {
        appendFieldToCopySpec(spec, 'setgid', setgid)
    }

    static void setSetgid(CopySpec spec, boolean setgid) {
        setgid(spec, setgid)
    }

    static void setuid(CopySpec spec, boolean setuid) {
        appendFieldToCopySpec(spec, 'setuid', setuid)
    }

    static void setSetuid(CopySpec spec, boolean setuid) {
        setuid(spec, setuid)
    }

    static void permissionGroup(CopySpec spec, String permissionGroup) {
        appendFieldToCopySpec(spec, 'permissionGroup', permissionGroup)
    }

    static void setPermissionGroup(CopySpec spec, String permissionGroupArg) {
        permissionGroup(spec, permissionGroupArg)
    }

    /**
     * RPM Only
     */
    static void fileType(CopySpec spec, Directive fileType) {
        appendFieldToCopySpec(spec, 'fileType', fileType)
    }

    /**
     * RPM Only
     */
    static void setFileType(CopySpec spec, Directive fileTypeArg) {
        fileType(spec, fileTypeArg)
    }

    /**
     * RPM Only
     */
    static void addParentDirs(CopySpec spec, boolean addParentDirs) {
        appendFieldToCopySpec(spec, 'addParentDirs', addParentDirs)
    }

    /**
     * RPM Only
     */
    static void setAddParentDirs(CopySpec spec, boolean addParentDirsArg) {
        addParentDirs(spec, addParentDirsArg)
    }

    /**
     * RPM Only
     */
    static void createDirectoryEntry(CopySpec spec, boolean createDirectoryEntry) {
        appendFieldToCopySpec(spec, 'createDirectoryEntry', createDirectoryEntry)
    }

    /**
     * RPM Only
     */
    static void setCreateDirectoryEntry(CopySpec spec, boolean createDirectoryEntryArg) {
        createDirectoryEntry(spec, createDirectoryEntryArg)
    }

    /**
     * DEB Only
     */
    static void uid(CopySpec spec, int uid) {
        appendFieldToCopySpec(spec, 'uid', uid)
    }

    /**
     * DEB Only
     */
    static void setUid(CopySpec spec, int uidArg) {
        uid(spec, uidArg)
    }

    /**
     * DEB Only
     */
    static void gid(CopySpec spec, int gid) {
        appendFieldToCopySpec(spec, 'gid', gid)
    }

    /**
     * DEB Only
     */
    static void setGid(CopySpec spec, int gidArg) {
        gid(spec, gidArg)
    }
}