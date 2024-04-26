package com.netflix.gradle.plugins.utils

import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.SyncSpec
import org.gradle.util.GradleVersion

/**
 * Utility class to get the unix permission of a file.
 */
class FilePermissionUtil {

    /**
     * Get the unix permission of a file.
     * Gradle 8.3 introduced a new way to get the unix permission of a file.
     * Gradle 8.8 deprecated the old way. @see https://docs.gradle.org/8.8-rc-1/userguide/upgrading_version_8.html#unix_file_permissions_deprecated
     * @param details
     * @return
     */
    static int getUnixPermission(FileCopyDetails details) {
        return isOlderThanGradle8_3() ? details.mode : details.permissions.toUnixNumeric()
    }

    /**
     * Get the unix permission of a file.
     * Gradle 8.3 introduced a new way to get the unix permission of a file.
     * Gradle 8.8 deprecated the old way. @see https://docs.gradle.org/8.8-rc-1/userguide/upgrading_version_8.html#unix_file_permissions_deprecated
     * @param details
     * @return
     */
    static Integer getFileMode(SyncSpec copySpecInternal) {
        if(!copySpecInternal) {
            return null
        }

        if(isOlderThanGradle8_3()) {
            return copySpecInternal.fileMode
        } else if(copySpecInternal.filePermissions.present){
            copySpecInternal.filePermissions.get().toUnixNumeric()
        } else {
            return null
        }
    }

    /**
     * Get the unix permission of a directory.
     * Gradle 8.3 introduced a new way to get the unix permission of a file.
     * Gradle 8.8 deprecated the old way. @see https://docs.gradle.org/8.8-rc-1/userguide/upgrading_version_8.html#unix_file_permissions_deprecated
     * @param copySpecInternal
     * @return
     */
    static Integer getDirMode(SyncSpec copySpecInternal) {
        if(!copySpecInternal) {
            return null
        }

        if(isOlderThanGradle8_3()) {
            return copySpecInternal.dirMode
        } else if(copySpecInternal.dirPermissions.present){
            copySpecInternal.dirPermissions.get().toUnixNumeric()
        } else {
            return null
        }
    }

    /**
     * Set the unix permission of a file.
     * Gradle 8.3 introduced a new way to set the unix permission of a file.
     * @param copySpec
     * @param permission
     */
    static void setFilePermission(SyncSpec copySpec, int permission) {
        isOlderThanGradle8_3() ?
                copySpec.setFileMode(permission) : copySpec.filePermissions {
            it.unix(permission)
        }
    }

    /**
     * Set the unix permission of a directory.
     * Gradle 8.3 introduced a new way to set the unix permission of a file.
     * @param copySpec
     * @param permission
     */
    static void setDirPermission(SyncSpec copySpec, int permission) {
        isOlderThanGradle8_3() ?
                copySpec.setDirMode(permission) : copySpec.dirPermissions {
            it.unix(permission)
        }
    }

    private static final isOlderThanGradle8_3() {
        return GradleVersion.current().baseVersion < GradleVersion.version('8.3').baseVersion
    }
}
