package com.netflix.gradle.plugins.docker

import com.netflix.gradle.plugins.BaseIntegrationTestKitSpec
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Requires

class OsPackageDockerPluginIntegrationTest extends BaseIntegrationTestKitSpec {
    // Opt in on a host with Docker and its CLI; production image building needs no CLI.
    @Requires({ System.getenv('OSPACKAGE_DOCKER_INTEGRATION_TEST') == 'true' })
    def 'builds and tags a real image from the generated context'() {
        given:
        def tag = 'ospackage-isolation-test:' + UUID.randomUUID()
        new File(projectDir, 'src').mkdirs()
        new File(projectDir, 'src/payload.txt').text = 'image payload'
        buildFile << """
            plugins { id 'com.netflix.nebula.ospackage-docker' }
            repositories { mavenCentral() }
            ospackageDocker.url = providers.environmentVariable('DOCKER_HOST')
            createDockerfile {
                destinationDirectory = layout.buildDirectory.dir('docker-context')
                instruction 'FROM scratch'
                from('src') { into '/payload' }
            }
            buildImage { images.add('${tag}') }
        """

        when:
        def first = runTasks('buildImage', '--configuration-cache')
        def second = runTasks('buildImage', '--configuration-cache')
        def inspect = new ProcessBuilder('docker', 'image', 'inspect', tag, '--format', '{{.Id}}')
            .redirectErrorStream(true).start()
        def actualImageId = inspect.inputStream.text.trim()

        then:
        inspect.waitFor() == 0
        first.task(':buildImage').outcome == TaskOutcome.SUCCESS
        second.task(':buildImage').outcome == TaskOutcome.SUCCESS
        second.output.contains('Configuration cache entry reused')
        def resultId = new File(projectDir, 'build/docker/buildImage-image-id.txt').text.trim()
        actualImageId.replaceFirst('^sha256:', '').startsWith(resultId.replaceFirst('^sha256:', ''))

        cleanup:
        def removal = new ProcessBuilder('docker', 'image', 'rm', tag).redirectErrorStream(true).start()
        removal.inputStream.text
        removal.waitFor()
    }
}
