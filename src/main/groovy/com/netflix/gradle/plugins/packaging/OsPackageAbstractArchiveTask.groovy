package com.netflix.gradle.plugins.packaging

import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.work.DisableCachingByDefault

import javax.annotation.Nullable

@DisableCachingByDefault
abstract class OsPackageAbstractArchiveTask extends AbstractArchiveTask {

    OsPackageAbstractArchiveTask() {
        super()
        notCompatibleWithConfigurationCache("nebula.ospackage does not support configuration cache")
    }

    void setVersion(@Nullable String version) {
        archiveVersion.convention(version)
        archiveVersion.set(version)
    }

    @Nullable
    @Internal("Represented as part of archiveFile")
    String getVersion() {
        return archiveVersion.getOrNull()
    }
}
