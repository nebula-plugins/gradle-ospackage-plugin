package com.netflix.gradle.plugins.docker

import com.netflix.gradle.plugins.packaging.CommonPackagingPlugin
import groovy.transform.CompileDynamic
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileDynamic
class OsPackageDockerBasePlugin implements Plugin<Project> {
    static final String CREATE_DOCKERFILE_TASK_NAME = 'createDockerfile'
    static final String BUILD_IMAGE_TASK_NAME = 'buildImage'
    static final String AGGREGATION_TASK_NAME = 'buildDocker'

    @Override
    void apply(Project project) {
        project.plugins.apply(CommonPackagingPlugin)
        // Some defaults, if not set by the user
        project.tasks.withType(SystemPackageDockerfile).configureEach { SystemPackageDockerfile systemPackageDockerfile ->
            systemPackageDockerfile.applyConventions(project)
        }

    }
}
