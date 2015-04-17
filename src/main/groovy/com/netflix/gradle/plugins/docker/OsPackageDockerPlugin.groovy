package com.netflix.gradle.plugins.docker

import com.bmuschko.gradle.docker.DockerRemoteApiPlugin
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.netflix.gradle.plugins.packaging.CommonPackagingPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class OsPackageDockerPlugin implements Plugin<Project> {
    static final String CREATE_DOCKERFILE_TASK_NAME = 'createDockerfile'
    static final String BUILD_IMAGE_TASK_NAME = 'buildImage'
    static final String AGGREGATION_TASK_NAME = 'buildDocker'

    @Override
    void apply(Project project) {
        project.plugins.withType(DockerRemoteApiPlugin) {
            project.plugins.apply(CommonPackagingPlugin)

            createTasks(project)
        }
    }

    private void createTasks(Project project) {
        SystemPackageDockerfile createDockerfileTask = project.task(CREATE_DOCKERFILE_TASK_NAME, type: SystemPackageDockerfile)

        DockerBuildImage buildImageTask = project.task(BUILD_IMAGE_TASK_NAME, type: DockerBuildImage) {
            dependsOn createDockerfileTask
            conventionMapping.inputDir = { createDockerfileTask.destinationDir }
        }

        project.task(AGGREGATION_TASK_NAME) {
            dependsOn buildImageTask
        }
    }
}
