package com.netflix.gradle.plugins.docker

import nebula.test.ProjectSpec
import org.apache.commons.io.FileUtils

class OsPackageDockerPluginTest extends ProjectSpec {

    def "creates required tasks on its own"() {
        when:
        project.apply plugin: 'nebula.ospackage-docker'

        then:
        project.tasks.findByName(OsPackageDockerBasePlugin.CREATE_DOCKERFILE_TASK_NAME)
        project.tasks.findByName(OsPackageDockerBasePlugin.BUILD_IMAGE_TASK_NAME)
        project.tasks.findByName(OsPackageDockerBasePlugin.AGGREGATION_TASK_NAME)
    }

    def "docker task inherits from extension"() {
        when:
        project.apply plugin: 'nebula.ospackage-docker'
        project.ospackage {
            user 'builder'
        }
        def ospackageTask = project.tasks.findByName('createDockerfile')

        then:
        ospackageTask.user == 'builder'
    }

    def "docker task as package name"() {
        when:
        project.apply plugin: 'nebula.ospackage-docker'
        def ospackageTask = project.tasks.findByName('createDockerfile')

        then:
        ospackageTask.packageName == 'docker-task-as-package-name'
    }

    def "creates a Dockerfile based on specifications"() {
        given:
        File destDir = project.file('build/tmp/DockerPluginTest')
        createDir(destDir)
        File srcDir = new File(projectDir, 'src')
        createDir(srcDir)
        FileUtils.writeStringToFile(new File(srcDir, 'apple.jar'), 'apple')
        FileUtils.writeStringToFile(new File(srcDir, 'banana.zip'), 'banana')

        when:
        project.apply plugin: 'nebula.ospackage-docker'

        SystemPackageDockerfile task = project.tasks.getByName(OsPackageDockerBasePlugin.CREATE_DOCKERFILE_TASK_NAME) {
            destinationDir = destDir
            instruction "FROM ubuntu:14.04"
            instruction "MAINTAINER John Doe 'john.doe@netflix.com'"

            from(srcDir) {
                into '/opt'
            }

            instruction 'WORKDIR /tmp'
        }

        task.copy()

        then:
        task.archivePath.exists()
        String dockerFileText = task.archivePath.text
        dockerFileText.startsWith(
"""FROM ubuntu:14.04
MAINTAINER John Doe 'john.doe@netflix.com'
WORKDIR /tmp
""")
        dockerFileText.contains('ADD apple.jar /opt/apple.jar')
        dockerFileText.contains('ADD banana.zip /opt/banana.zip')
    }

    private File createDir(File dir) {
        boolean success = dir.mkdirs()

        if(!success) {
            throw new IOException("Failed to create directory '$dir.canonicalPath'")
        }
    }
}
