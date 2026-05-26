package com.netflix.gradle.plugins.docker

import org.gradle.api.Plugin
import org.gradle.api.Project

class OsPackageDockerPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.apply("com.netflix.nebula.ospackage-base")
        project.plugins.apply("com.netflix.nebula.ospackage-docker-base")
        project.plugins.apply("com.bmuschko.docker-remote-api")
    }
}
