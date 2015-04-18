package com.netflix.gradle.plugins.docker

import nebula.test.ProjectSpec
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class OsPackageDockerBasePluginTest extends ProjectSpec {
    def "on its own, creates no tasks"() {
        when:
        project.apply plugin: 'os-package-docker-base'

        then:
        !project.tasks.findByName(OsPackageDockerBasePlugin.CREATE_DOCKERFILE_TASK_NAME)
        !project.tasks.findByName(OsPackageDockerBasePlugin.BUILD_IMAGE_TASK_NAME)
        !project.tasks.findByName(OsPackageDockerBasePlugin.AGGREGATION_TASK_NAME)
    }

    def "with docker plugin, tasks created"() {
        when:
        project.apply plugin: 'os-package-docker-base'
        project.apply plugin: 'com.bmuschko.docker-remote-api'

        then:
        project.tasks.findByName(OsPackageDockerBasePlugin.CREATE_DOCKERFILE_TASK_NAME)
        project.tasks.findByName(OsPackageDockerBasePlugin.BUILD_IMAGE_TASK_NAME)
        project.tasks.findByName(OsPackageDockerBasePlugin.AGGREGATION_TASK_NAME)
    }

}
