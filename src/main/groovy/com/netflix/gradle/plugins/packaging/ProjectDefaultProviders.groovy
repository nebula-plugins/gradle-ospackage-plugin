package com.netflix.gradle.plugins.packaging

import org.gradle.api.Project
import org.gradle.api.provider.Provider

/** Lazily reads project defaults while the build is being configured. */
class ProjectDefaultProviders {
    static Provider<String> version(Project project) {
        project.providers.provider { project.version.toString() }
    }

    static Provider<String> description(Project project) {
        project.providers.provider { project.description ?: '' }
    }
}
