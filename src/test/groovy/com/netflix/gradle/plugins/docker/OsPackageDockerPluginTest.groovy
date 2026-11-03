package com.netflix.gradle.plugins.docker

import nebula.test.ProjectSpec
import org.vafer.jdeb.shaded.commons.io.FileUtils

class OsPackageDockerPluginTest extends ProjectSpec {

    def "creates required tasks on its own"() {
        when:
        project.apply plugin: 'com.netflix.nebula.ospackage-docker'

        then:
        project.tasks.findByName(OsPackageDockerBasePlugin.CREATE_DOCKERFILE_TASK_NAME)
        project.tasks.findByName(OsPackageDockerBasePlugin.BUILD_IMAGE_TASK_NAME)
        project.tasks.findByName(OsPackageDockerBasePlugin.AGGREGATION_TASK_NAME)
    }

    def "docker task inherits from extension"() {
        when:
        project.apply plugin: 'com.netflix.nebula.ospackage-docker'
        project.ospackage {
            user 'builder'
        }
        def ospackageTask = project.tasks.findByName('createDockerfile')

        then:
        ospackageTask.user == 'builder'
    }

    def "docker task as package name"() {
        when:
        project.apply plugin: 'com.netflix.nebula.ospackage-docker'
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
        project.apply plugin: 'com.netflix.nebula.ospackage-docker'

        SystemPackageDockerfile task = project.tasks.getByName(OsPackageDockerBasePlugin.CREATE_DOCKERFILE_TASK_NAME) {
            destinationDirectory.set(destDir)
            instruction "FROM ubuntu:14.04"
            instruction "MAINTAINER John Doe 'john.doe@netflix.com'"

            from(srcDir) {
                into '/opt'
            }

            instruction 'WORKDIR /tmp'
        }

        task.copy()

        then:
        task.archiveFile.get().asFile.exists()
        String dockerFileText = task.archiveFile.get().asFile.text
        dockerFileText.startsWith(
"""FROM ubuntu:14.04
MAINTAINER John Doe 'john.doe@netflix.com'
WORKDIR /tmp
""")
        dockerFileText.contains('ADD [".ospackage/opt/apple.jar","/opt/apple.jar"]')
        dockerFileText.contains('ADD [".ospackage/opt/banana.zip","/opt/banana.zip"]')
        new File(destDir, '.ospackage/opt/apple.jar').text == 'apple'
        new File(destDir, '.ospackage/opt/banana.zip').text == 'banana'
    }

    def "context staging preserves filters and distinct destinations and removes stale files"() {
        given:
        File first = new File(projectDir, 'first/same.txt')
        File second = new File(projectDir, 'second/same.txt')
        first.parentFile.mkdirs()
        second.parentFile.mkdirs()
        first.text = 'first token'
        second.text = 'second content'
        project.apply plugin: 'com.netflix.nebula.ospackage-docker'
        SystemPackageDockerfile task = project.tasks.getByName('createDockerfile')
        File context = project.file('build/context')
        task.destinationDirectory.set(context)
        task.instruction('FROM scratch')
        task.from(first) {
            into '/first folder'
            filter { String line -> line.replace('token', 'filtered') }
        }
        task.from(second) { into '/second' }

        when:
        task.copy()

        then:
        new File(context, '.ospackage/first folder/same.txt').text.trim() == 'first filtered'
        new File(context, '.ospackage/second/same.txt').text == 'second content'
        task.archiveFile.get().asFile.text.contains('ADD [".ospackage/first folder/same.txt","/first folder/same.txt"]')

        when:
        second.delete()
        task.copy()

        then:
        !new File(context, '.ospackage/second/same.txt').exists()
        !task.archiveFile.get().asFile.text.contains('/second/same.txt')
    }

    private File createDir(File dir) {
        boolean success = dir.mkdirs()

        if(!success) {
            throw new IOException("Failed to create directory '$dir.canonicalPath'")
        }
    }
}
