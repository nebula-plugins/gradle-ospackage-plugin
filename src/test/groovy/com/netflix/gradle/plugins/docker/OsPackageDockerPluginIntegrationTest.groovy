package com.netflix.gradle.plugins.docker

import nebula.test.IntegrationSpec
import spock.lang.Ignore

class OsPackageDockerPluginIntegrationTest extends IntegrationSpec {
    static final String SERVER_URL = 'http://localhost:2375'

    def setup() {
        fork = true
    }

    @Ignore
    //@IgnoreIf({ !OsPackageDockerPluginIntegrationTest.isDockerServerInfoUrlReachable() })
    def "Can create Dockerfile and build image from it"() {
        given:
        buildFile << """
apply plugin: 'com.netflix.gradle.plugins.docker.OsPackageDockerPlugin'

repositories {
    mavenCentral()
}

createDockerfile {
    destinationDir = file('build/tmp/DockerPluginTest')
    instruction "FROM ubuntu:14.04"
    instruction "MAINTAINER John Doe 'john.doe@netflix.com'"
}
"""
        when:
        runTasksSuccessfully('buildImage')

        then:
        noExceptionThrown()
    }

    static boolean isDockerServerInfoUrlReachable() {
        URL url = new URL("$SERVER_URL/info")
        isUrlReachable(url)
    }

    static boolean isUrlReachable(URL url) {
        try {
            HttpURLConnection connection = url.openConnection()
            connection.requestMethod = 'GET'
            connection.connectTimeout = 3000
            return connection.responseCode == HttpURLConnection.HTTP_OK
        }
        catch(IOException e) {
            return false
        }
    }
}
