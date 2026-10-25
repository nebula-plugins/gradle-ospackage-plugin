package com.netflix.gradle.plugins.packaging

import groovy.transform.CompileDynamic
import org.apache.commons.lang3.reflect.FieldUtils
import org.gradle.api.Action
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCopyDetails
import org.redline_rpm.payload.Directive
import org.gradle.api.file.CopySpec
import org.gradle.api.internal.file.copy.CopySpecWrapper
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.internal.file.copy.DefaultCopySpec

import java.util.logging.Logger

/**
 * Adds packaging settings to Gradle-created copy specs. Settings travel with the spec's
 * copy actions, which Gradle preserves when storing and loading the configuration cache.
 */
@Category(CopySpec)
@CompileDynamic
class CopySpecEnhancement {
    private static final Logger LOGGER = Logger.getLogger(CopySpecEnhancement.getName())

    static void appendFieldToCopySpec(CopySpec spec, String fieldName, Object value) {
        CopySpec directSpec = unwrap(spec)
        SpecSettingsAction settings = findSettings(directSpec)
        if (settings == null) {
            settings = new SpecSettingsAction()
            // The task declares these values as inputs; the action itself does no work.
            ((CopySpecInternal) directSpec).appendCachingSafeCopyAction(settings)
        }
        settings.values.put(fieldName, value)

        LOGGER.info("Adding $fieldName to ${directSpec}")
        // Retain configuration-time property reads for existing Groovy consumers.
        directSpec.metaClass["get${fieldName.capitalize()}"] = { value }
        spec.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
    }

    static Map<String, Object> getSettings(CopySpec spec) {
        findSettings(unwrap(spec))?.values ?: Collections.emptyMap()
    }

    static Map<String, Map<String, Object>> snapshotSettings(CopySpecInternal root) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>()
        collectSettings(root, 'root', result)
        result
    }

    private static void collectSettings(CopySpecInternal spec, String path,
                                        Map<String, Map<String, Object>> result) {
        Map<String, Object> settings = getSettings(spec)
        if (!settings.isEmpty()) {
            result.put(path, settings.collectEntries { String name, Object value ->
                [(name): value instanceof Directive ? value.flag() : value]
            })
        }
        int index = 0
        spec.children.each { CopySpecInternal child ->
            collectSettings(child, "${path}/${index++}".toString(), result)
        }
    }

    private static CopySpec unwrap(CopySpec spec) {
        while (spec instanceof CopySpecWrapper) {
            def delegateField = FieldUtils.getField(CopySpecWrapper, 'delegate', true)
            spec = (CopySpec) delegateField.get(spec)
        }
        spec
    }

    private static SpecSettingsAction findSettings(CopySpec spec) {
        spec instanceof DefaultCopySpec ? spec.copyActions.find { it instanceof SpecSettingsAction } : null
    }

    private static class SpecSettingsAction implements Action<FileCopyDetails>, Serializable {
        final Map<String, Object> values = new LinkedHashMap<>()

        @Override
        void execute(FileCopyDetails details) {
            // A serializable carrier for per-spec metadata, including directory settings.
        }
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
