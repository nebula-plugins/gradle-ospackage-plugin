package com.netflix.gradle.plugins.docker

import nebula.test.ProjectSpec
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class OsPackageDockerPluginTest extends ProjectSpec {
    Project project = ProjectBuilder.builder().build()

    def "creates required tasks on its own"() {
        when:
        project.apply plugin: 'os-package-docker'

        then:
        project.tasks.findByName(OsPackageDockerBasePlugin.CREATE_DOCKERFILE_TASK_NAME)
        project.tasks.findByName(OsPackageDockerBasePlugin.BUILD_IMAGE_TASK_NAME)
        project.tasks.findByName(OsPackageDockerBasePlugin.AGGREGATION_TASK_NAME)
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
        project.apply plugin: 'os-package-docker'

        SystemPackageDockerfile task = project.tasks.getByName(OsPackageDockerBasePlugin.CREATE_DOCKERFILE_TASK_NAME) {
            destinationDir = destDir
            instruction "FROM ubuntu:14.04"
            instruction "MAINTAINER John Doe 'john.doe@netflix.com'"

            from(srcDir) {
                into '/opt'
            }

            instruction 'WORKDIR /tmp'
        }

        task.execute()

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
