package com.netflix.gradle.plugins.utils

import groovy.transform.CompileDynamic
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.SyncSpec

/**
 * Utility class to get the unix permission of a file.
 */
@CompileDynamic
class FilePermissionUtil {

    /**
     * Get the unix permission of a file.
     * 
     * <p>In Gradle 9.0, the new permissions API (details.permissions.toUnixNumeric()) may not correctly 
     * detect executable bits for files, often returning 0644 instead of 0755 for executable files. 
     * This method implements a fallback mechanism that detects when the API fails to identify executable 
     * files and uses filesystem-level checks (File.canExecute()) to determine the correct permissions.</p>
     * 
     * <p>The fallback logic is triggered when:
     * <ul>
     * <li>The file exists and is marked as executable at the filesystem level</li>
     * <li>But the Gradle permissions API reports no executable bits (mode & 0111 == 0)</li>
     * </ul>
     * In such cases, this method uses direct filesystem permission checks to return appropriate 
     * Unix permission values (0755 for rwx, 0555 for r-x, etc.).</p>
     * 
     * @param details FileCopyDetails containing file information and permissions
     * @return Unix permission as integer (e.g., 0755, 0644)
     */
    static int getUnixPermission(FileCopyDetails details) {

        int newApiMode = details.permissions.toUnixNumeric()
        
        // Gradle 9.0 permissions API may not detect executable bits correctly
        // Fallback: check the actual file permissions if the API seems wrong
        try {
            if (details.file?.canExecute() && (newApiMode & 0111) == 0) {
                // File is executable but new API didn't detect it - use filesystem check
                boolean readable = details.file.canRead()
                boolean writable = details.file.canWrite()
                boolean executable = details.file.canExecute()

                if (readable && writable && executable) {
                    return 0755 // rwxr-xr-x
                } else if (readable && executable) {
                    return 0555 // r-xr-xr-x
                } else {
                    return 0644 // rw-r--r--
                }
            }
        } catch (Exception e) {
            return newApiMode
        }

        return newApiMode
    }

    /**
     * Get the unix permission of a file.
     * @param details
     * @return
     */
    static Integer getFileMode(SyncSpec copySpecInternal) {
        if(!copySpecInternal) {
            return null
        }

        if(copySpecInternal.filePermissions.present){
            copySpecInternal.filePermissions.get().toUnixNumeric()
        } else {
            return null
        }
    }

    /**
     * Get the unix permission of a directory.
     * @param copySpecInternal
     * @return
     */
    static Integer getDirMode(SyncSpec copySpecInternal) {
        if(!copySpecInternal) {
            return null
        }

        if(copySpecInternal.dirPermissions.present){
            copySpecInternal.dirPermissions.get().toUnixNumeric()
        } else {
            return null
        }
    }

    /**
     * Set the unix permission of a file.
     * @param copySpec
     * @param permission
     */
    static void setFilePermission(SyncSpec copySpec, int permission) {
        copySpec.filePermissions {
            it.unix(permission)
        }
    }

    /**
     * Set the unix permission of a directory.
     * @param copySpec
     * @param permission
     */
    static void setDirPermission(SyncSpec copySpec, int permission) {
        copySpec.dirPermissions {
            it.unix(permission)
        }
    }

}
