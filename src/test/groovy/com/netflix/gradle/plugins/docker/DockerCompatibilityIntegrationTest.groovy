package com.netflix.gradle.plugins.docker

import com.netflix.gradle.plugins.BaseIntegrationTestKitSpec

class DockerCompatibilityIntegrationTest extends BaseIntegrationTestKitSpec {
    def 'the Docker DSL uses owned types and preserves basic configuration and task wiring'() {
        given:
        buildFile << '''
            import com.netflix.gradle.plugins.docker.DockerExtension
            import com.netflix.gradle.plugins.docker.DockerBuildImage

            plugins { id 'com.netflix.nebula.ospackage-docker' }

            ospackageDocker {
                url = 'tcp://127.0.0.1:2375'
                apiVersion = '1.44'
                registryCredentials {
                    url = 'registry.example.com'
                    username = 'example'
                    password = 'fixture-only'
                }
            }
            def extension = extensions.getByType(DockerExtension)
            assert extension.is(extensions.getByName('ospackageDocker'))
            assert extensions.findByName('docker') == null
            assert !plugins.hasPlugin('com.bmuschko.docker-remote-api')
            tasks.withType(DockerBuildImage).configureEach {
                labels.put('api', 'owned')
            }
            def image = tasks.named('buildImage', DockerBuildImage).get()
            image.images.add('example/application:local')
            assert image.url.get() == extension.url.get()
            assert image.apiVersion.get() == '1.44'
            assert image.registryCredentials.username.get() == 'example'
            assert image.labels.get().api == 'owned'
            assert image.inputDir.get().asFile == tasks.createDockerfile.destinationDirectory.get().asFile
            assert image.taskDependencies.getDependencies(image).contains(tasks.createDockerfile)
            assert tasks.buildDocker.taskDependencies.getDependencies(tasks.buildDocker).contains(image)
            assert !image.hasProperty('dockerClient')
        '''

        when:
        def result = runTasks('help', '--offline')

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def 'ordinary packaging supports an independently declared bmuschko in either order'() {
        given:
        buildFile << '''
            buildscript {
                repositories { gradlePluginPortal() }
                dependencies { classpath 'com.bmuschko:gradle-docker-plugin:10.0.0' }
            }
            plugins { id 'com.netflix.nebula.ospackage' apply false }
        '''
        buildFile << """
            apply plugin: '${first}'
            apply plugin: '${second}'
        """
        buildFile << '''
            assert extensions.getByName('docker') instanceof com.bmuschko.gradle.docker.DockerExtension
            assert tasks.findByName('buildImage') == null
            assert tasks.findByName('createDockerfile') == null
            assert tasks.findByName('buildDeb') != null
            assert tasks.findByName('buildRpm') != null
            tasks.register('consumerImage', com.bmuschko.gradle.docker.tasks.image.DockerBuildImage)
        '''

        when:
        def result = runTasks('help')

        then:
        result.output.contains('BUILD SUCCESSFUL')

        where:
        first                            | second
        'com.netflix.nebula.ospackage'    | 'com.bmuschko.docker-remote-api'
        'com.bmuschko.docker-remote-api'   | 'com.netflix.nebula.ospackage'
    }
}
