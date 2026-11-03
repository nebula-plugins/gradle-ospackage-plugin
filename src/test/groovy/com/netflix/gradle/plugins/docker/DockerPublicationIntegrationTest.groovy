package com.netflix.gradle.plugins.docker

import com.netflix.gradle.plugins.BaseIntegrationTestKitSpec
import org.gradle.testkit.runner.GradleRunner

import java.util.zip.ZipFile

class DockerPublicationIntegrationTest extends BaseIntegrationTestKitSpec {
    def 'published #pluginId keeps consumer Docker models separate (legacy client: #legacyClient)'() {
        given:
        def version = System.getProperty('dockerTest.version')
        def repository = prepareRepository(version)
        new File(projectDir, 'settings.gradle').text = """
            pluginManagement {
                repositories {
                    maven { url = uri('${repository.toURI()}') }
                    gradlePluginPortal()
                }
            }
            rootProject.name = 'docker-classpath-consumer'
            include 'consumer'
        """
        if (legacyClient) {
            buildFile << '''
                buildscript {
                    repositories { gradlePluginPortal() }
                    dependencies { classpath 'com.bmuschko:gradle-docker-plugin:10.0.0' }
                }
            '''
        }
        buildFile << "plugins { id '${pluginId}' version '${version}' }\n"
        if (!legacyClient) {
            buildFile << '''
                // No Docker API classes may be exported from the root plugin classpath.
                try {
                    Class.forName('com.github.dockerjava.api.model.Info', false, buildscript.classLoader)
                    throw new GradleException('Docker models leaked into the root buildscript')
                } catch (ClassNotFoundException expected) { }
            '''
        }
        new File(projectDir, 'consumer').mkdirs()
        new File(projectDir, 'consumer/build.gradle').text = '''
            buildscript {
                repositories { mavenCentral() }
                dependencies { classpath 'org.testcontainers:testcontainers:2.0.4' }
            }
            tasks.register('parseDockerInfo') {
                doLast {
                    def model = com.github.dockerjava.api.model.Info
                    println "Docker model origin: ${model.protectionDomain.codeSource.location}"
                    def annotation = model.getDeclaredField('osType').annotations.find {
                        it.annotationType().simpleName == 'JsonProperty'
                    }.annotationType()
                    println "Docker annotation origin: ${annotation.protectionDomain.codeSource.location}"
                    def mapper = new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper()
                    mapper.configure(org.testcontainers.shaded.com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    def info = mapper.readValue('{"OSType":"linux","MemTotal":123456789}', model)
                    assert info.osType == 'linux'
                    assert info.memTotal == 123456789L
                    assert annotation.name == 'com.fasterxml.jackson.annotation.JsonProperty'
                    assert model.protectionDomain.codeSource.location.toString().contains('docker-java-api-')
                    println 'Consumer Docker info parsed correctly'
                }
            }
        '''

        when:
        // Deliberately omit withPluginClasspath(): resolve the actual plugin marker and publication.
        def runner = GradleRunner.create().withProjectDir(projectDir)
            .withArguments(':consumer:parseDockerInfo', '--stacktrace', '--configuration-cache')
        def result = legacyClient ? runner.buildAndFail() : runner.build()

        then:
        if (legacyClient) {
            // Prove the fixture detects the old parent-classloader/annotation mismatch.
            assert result.output.contains("info.osType == 'linux'")
            assert result.output.contains('gradle-docker-plugin-10.0.0.jar')
        } else {
            assert result.output.contains('Consumer Docker info parsed correctly')
        }

        where:
        pluginId                              | legacyClient
        'com.netflix.nebula.ospackage'         | false
        'com.netflix.nebula.ospackage-docker'  | false
        'com.netflix.nebula.ospackage'         | true
    }

    private File prepareRepository(String version) {
        def publications = new File(System.getProperty('dockerTest.publications'))
        def jar = new File(System.getProperty('dockerTest.pluginJar'))
        new ZipFile(jar).withCloseable { archive ->
            assert !archive.entries().toList().any {
                it.name.startsWith('com/github/dockerjava/') || it.name.startsWith('com/bmuschko/')
            }
        }
        for (def name : ['nebula', 'pluginMaven']) {
            for (def file : ['pom-default.xml', 'module.json']) {
                def metadata = new File(publications, "${name}/${file}").text
                assert !metadata.contains('com.bmuschko')
                assert !metadata.contains('com.github.docker-java')
            }
        }
        def repository = new File(projectDir, 'repository')
        def module = new File(repository, "com/netflix/nebula/gradle-ospackage-plugin/${version}")
        module.mkdirs()
        new File(module, "gradle-ospackage-plugin-${version}.jar").bytes = jar.bytes
        new File(module, "gradle-ospackage-plugin-${version}.pom").bytes =
            new File(publications, 'nebula/pom-default.xml').bytes
        new File(module, "gradle-ospackage-plugin-${version}.module").bytes =
            new File(publications, 'nebula/module.json').bytes
        ['ospackage', 'ospackageDocker'].each { name ->
            def id = name == 'ospackage' ? 'com.netflix.nebula.ospackage' : 'com.netflix.nebula.ospackage-docker'
            def marker = new File(repository, "${id.replace('.', '/')}/${id}.gradle.plugin/${version}")
            marker.mkdirs()
            new File(marker, "${id}.gradle.plugin-${version}.pom").bytes =
                new File(publications, "${name}PluginMarkerMaven/pom-default.xml").bytes
        }
        repository
    }
}
