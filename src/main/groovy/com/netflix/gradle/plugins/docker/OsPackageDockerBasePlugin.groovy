package com.netflix.gradle.plugins.docker

import com.bmuschko.gradle.docker.DockerRemoteApiPlugin
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.netflix.gradle.plugins.packaging.CommonPackagingPlugin
import com.netflix.gradle.plugins.rpm.Rpm
import com.netflix.gradle.plugins.rpm.RpmPlugin
import groovy.transform.CompileDynamic
import org.gradle.api.Action
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
        project.tasks.withType(SystemPackageDockerfile).configureEach(new Action<SystemPackageDockerfile>() {
            @Override
            void execute(SystemPackageDockerfile systemPackageDockerfile) {
                systemPackageDockerfile.applyConventions()
            }
        })

        project.plugins.withType(DockerRemoteApiPlugin).configureEach(new Action<DockerRemoteApiPlugin>() {
            @Override
            void execute(DockerRemoteApiPlugin dockerRemoteApiPlugin) {
                createTasks(project)
            }
        })
    }

    private void createTasks(Project project) {
        def createDockerfileTaskProvider = project.tasks.register(CREATE_DOCKERFILE_TASK_NAME, SystemPackageDockerfile)

        def buildImageTaskProvider = project.tasks.register(BUILD_IMAGE_TASK_NAME, DockerBuildImage) {
            dependsOn createDockerfileTaskProvider
            conventionMapping.inputDir = { createDockerfileTaskProvider.get().destinationDir }
        }

        project.tasks.register(AGGREGATION_TASK_NAME) {
            dependsOn buildImageTaskProvider
        }
    }
}
