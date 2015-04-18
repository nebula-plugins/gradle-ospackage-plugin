package com.netflix.gradle.plugins.docker

import com.bmuschko.gradle.docker.DockerRemoteApiPlugin
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.netflix.gradle.plugins.packaging.CommonPackagingPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class OsPackageDockerPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.apply(OsPackageDockerBasePlugin)
        project.plugins.apply(DockerRemoteApiPlugin)
    }
}
