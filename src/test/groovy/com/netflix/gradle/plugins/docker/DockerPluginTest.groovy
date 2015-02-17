package com.netflix.gradle.plugins.docker

import nebula.test.ProjectSpec
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class DockerPluginTest extends ProjectSpec {
    Project project = ProjectBuilder.builder().build()

    def "creates a Dockerfile based on specifications"() {
        given:
        File destDir = project.file('build/tmp/DockerPluginTest')
        createDir(destDir)
        File srcDir = new File(projectDir, 'src')
        createDir(srcDir)
        FileUtils.writeStringToFile(new File(srcDir, 'apple.jar'), 'apple')
        FileUtils.writeStringToFile(new File(srcDir, 'banana.zip'), 'banana')

        when:
        project.apply plugin: 'docker'

        Docker docker = project.task('buildDocker', type: Docker) {
            destinationDir = destDir
            instruction "FROM 'ubuntu:14.04'"
            instruction "MAINTAINER John Doe 'john.doe@netflix.com'"

            from(srcDir) {
                into '/opt'
            }

            instruction 'WORKDIR /tmp'
        }

        docker.execute()

        then:
        docker.archivePath.exists()
        docker.archivePath.text ==
"""FROM 'ubuntu:14.04'
MAINTAINER John Doe 'john.doe@netflix.com'
WORKDIR /tmp
ADD apple.jar /opt/apple.jar
ADD banana.zip /opt/banana.zip
"""
    }

    private File createDir(File dir) {
        boolean success = dir.mkdirs()

        if(!success) {
            throw new IOException("Failed to create directory '$dir.canonicalPath'")
        }
    }
}
