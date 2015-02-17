package com.netflix.gradle.plugins.docker

import com.netflix.gradle.plugins.packaging.CommonPackagingPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class DockerPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.apply(CommonPackagingPlugin)
    }
}
