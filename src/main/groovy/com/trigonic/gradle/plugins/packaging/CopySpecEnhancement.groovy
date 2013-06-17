package com.trigonic.gradle.plugins.packaging

import org.gradle.api.file.CopySpec

import java.util.logging.Logger

/**
 * Try to mark up CopySpec
 */
@Category(CopySpec)
class CopySpecEnhancement {
    private static final Logger LOGGER = Logger.getLogger(CopySpecEnhancement.getName())

    CopySpec user(CopySpec spec, String username) {
        spec.metaClass.user = username
    }

    CopySpec createDirectoryEntry(CopySpec spec, boolean createDirectoryEntry) {
        spec.metaClass.createDirectoryEntry = createDirectoryEntry
    }
}