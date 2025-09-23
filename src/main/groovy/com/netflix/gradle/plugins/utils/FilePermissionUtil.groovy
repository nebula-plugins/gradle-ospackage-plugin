package com.netflix.gradle.plugins.utils

import groovy.transform.CompileDynamic
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.SyncSpec

/**
 * Utility class for handling Unix file permissions in Gradle build scripts and OS packages.
 * 
 * <p>This class provides utilities to work with file permissions in the context of creating
 * DEB and RPM packages. It addresses two key challenges:</p>
 * 
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
     * Detects if file permissions were explicitly configured by the user in the build script.
     * 
     * <p>This method distinguishes between truly explicit permissions (set by user in filePermissions blocks)
     * and system default permissions (644) that may have the same numeric value. It uses heuristics to detect
     * explicit configuration:</p>
     * 
     * <ul>
     * <li><strong>Include/exclude patterns:</strong> Presence indicates explicit copySpec configuration</li>
     * <li><strong>Non-default values:</strong> Any permission other than 644 (420 decimal) is considered explicit</li>
     * <li><strong>Explicit configuration blocks:</strong> Detection of filePermissions { } usage</li>
     * </ul>
     * 
     * <p>Explicit permissions always take precedence over
     * filesystem-based workarounds, giving users "ultimate control over permissions in packages".</p>
     * 
     * @param copySpecInternal The copy specification to examine for explicit permission configuration
     * @return Integer permission value if explicit permissions detected, null if using system defaults
     */
    static Integer getFileMode(def copySpecInternal) {
        if(!copySpecInternal) {
            return null
        }

        try {
            if(copySpecInternal?.hasProperty('filePermissions') && copySpecInternal.filePermissions?.present) {
                def permissions = copySpecInternal.filePermissions.get()
                def numeric = permissions.toUnixNumeric()
                
                // Try to detect if this copySpec has explicit configuration (includes, excludes, etc.)
                def hasExplicitConfiguration = false
                try {
                    // Look for signs of explicit configuration
                    if (copySpecInternal.hasProperty('includes') && copySpecInternal.includes?.size() > 0) {
                        hasExplicitConfiguration = true
                    }
                    if (copySpecInternal.hasProperty('excludes') && copySpecInternal.excludes?.size() > 0) {
                        hasExplicitConfiguration = true
                    }
                } catch (Exception e) {
                    // Ignore - could not check explicit configuration
                }
                
                // If we have explicit configuration OR the permission is not default 644, treat as explicit
                if (hasExplicitConfiguration || numeric != 420) {
                    return numeric
                } else {
                    // Default 644 with no explicit configuration - treat as default
                    return null
                }
            }
        } catch (Exception e) {
            // Ignore - not a proper SyncSpec or permissions not set
        }
        return null
    }

    /**
     * Detects if directory permissions were explicitly configured by the user in the build script.
     * 
     * <p>Similar to {@link #getFileMode(def)} but specifically for directory permissions configured
     * via dirPermissions blocks. This method only returns non-null values when it detects the presence
     * of an explicit dirPermissions configuration block.</p>
     * 
     * @param copySpecInternal The copy specification to examine for explicit directory permission configuration  
     * @return Integer permission value if explicit directory permissions detected, null if using system defaults
     * @see #getFileMode(def)
     */
    static Integer getDirMode(def copySpecInternal) {
        if(!copySpecInternal) {
            return null
        }

        try {
            if(copySpecInternal?.hasProperty('dirPermissions') && copySpecInternal.dirPermissions?.present){
                return copySpecInternal.dirPermissions.get().toUnixNumeric()
            }
        } catch (Exception e) {
        }
        return null
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
