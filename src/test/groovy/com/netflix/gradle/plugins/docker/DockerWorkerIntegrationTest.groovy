package com.netflix.gradle.plugins.docker

import com.netflix.gradle.plugins.BaseIntegrationTestKitSpec
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import groovy.json.JsonSlurper
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.api.JavaVersion

import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.GZIPInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class DockerWorkerIntegrationTest extends BaseIntegrationTestKitSpec {
    HttpServer server
    List<Map> requests = new CopyOnWriteArrayList<>()
    volatile String response = '{"stream":"Building fixture\\n"}\n{"aux":{"ID":"sha256:0123456789abcdef"}}\n'

    def setup() {
        server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
        startServer()
    }

    private void startServer() {
        server.createContext('/') { exchange ->
            requests.add([
                path: exchange.requestURI.path,
                query: exchange.requestURI.rawQuery,
                auth: exchange.requestHeaders.getFirst('X-Registry-Config'),
                body: exchange.requestBody.bytes
            ])
            byte[] body = response.getBytes('UTF-8')
            exchange.responseHeaders.set('Content-Type', 'application/json')
            exchange.sendResponseHeaders(200, body.length)
            exchange.responseBody.withCloseable { it.write(body) }
            exchange.close()
        }
        server.start()
    }

    def cleanup() {
        server?.stop(0)
    }

    def 'builds in a separate JVM with its own client, options and credentials, and reuses configuration'() {
        given:
        buildFile << """
            buildscript {
                repositories { mavenCentral() }
                dependencies {
                    classpath 'com.github.docker-java:docker-java-core:3.2.14'
                    classpath 'com.fasterxml.jackson.core:jackson-databind:2.10.5.1'
                }
            }
        """
        configureBuild()
        buildFile << """
            ospackageDocker {
                registryCredentials {
                    url = 'registry.example.test'
                    username = 'fixture-user'
                    password = 'fixture-secret'
                }
            }
            buildImage {
                images.add('fixture/image:latest')
                buildArgs.put('MESSAGE', 'hello')
                labels.put('purpose', 'test')
                target = 'final'
                platform = 'linux/amd64'
                network = 'none'
                extraHosts.add('example.test:127.0.0.1')
                cacheFrom.add('fixture/base:latest')
                noCache = true
                pull = true
                remove = false
                quiet = false
                memory = 123456L
                shmSize = 654321L
                doLast { println('GRADLE_PID=' + ProcessHandle.current().pid()) }
            }
        """

        when:
        def first = runTasks('buildImage', '--configuration-cache', '--info')
        def second = runTasks('buildImage', '--configuration-cache', '--info')

        then:
        first.task(':buildImage').outcome == TaskOutcome.SUCCESS
        second.task(':buildImage').outcome == TaskOutcome.SUCCESS
        second.output.contains('Configuration cache entry reused')
        new File(projectDir, 'build/docker/buildImage-image-id.txt').text.trim() == 'sha256:0123456789abcdef'
        first.output.contains('docker-java-core-3.7.1.jar')
        def worker = (first.output =~ /Docker worker JVM (\d+)/)[0][1]
        def daemon = (first.output =~ /GRADLE_PID=(\d+)/)[0][1]
        worker != daemon
        def builds = requests.findAll { it.path.endsWith('/build') }
        builds.size() == 2
        def query = queryParameters(builds[0].query)
        query.t == 'fixture/image:latest'
        new JsonSlurper().parseText(query.buildargs) == [MESSAGE: 'hello']
        new JsonSlurper().parseText(query.labels) == [purpose: 'test']
        query.target == 'final'
        query.platform == 'linux/amd64'
        query.networkmode == 'none'
        query.nocache == 'true'
        query.pull == 'true'
        query.rm == 'false'
        query.memory == '123456'
        query.shmsize == '654321'
        query.extrahosts == 'example.test:127.0.0.1'
        new JsonSlurper().parseText(query.cachefrom) == ['fixture/base:latest']
        def auth = new JsonSlurper().parse(builds[0].auth.decodeBase64())
        auth['registry.example.test'].username == 'fixture-user'
        auth['registry.example.test'].password == 'fixture-secret'
        contextFiles(builds[0].body)['Dockerfile'].contains('FROM scratch')
        contextFiles(builds[0].body)['.ospackage/app.txt'] == 'application content'
    }

    def 'a custom owned image task inherits worker and connection defaults'() {
        given:
        configureBuild()
        buildFile << '''
            import com.netflix.gradle.plugins.docker.DockerBuildImage
            ospackageDocker.registryCredentials {
                url = 'registry.example.test'
                username = 'custom-user'
                password = 'custom-secret'
            }
            tasks.register('customImage', DockerBuildImage) {
                dependsOn tasks.named('createDockerfile')
                inputDir = tasks.named('createDockerfile').flatMap { it.destinationDirectory }
                images.add('fixture/custom:latest')
            }
        '''

        when:
        def result = runTasks('customImage', '--configuration-cache')

        then:
        result.task(':customImage').outcome == TaskOutcome.SUCCESS
        new File(projectDir, 'build/docker/customImage-image-id.txt').text.trim() == 'sha256:0123456789abcdef'
        def build = requests.find { it.path.endsWith('/build') }
        queryParameters(build.query).t == 'fixture/custom:latest'
        def auth = new JsonSlurper().parse(build.auth.decodeBase64())
        auth['registry.example.test'].username == 'custom-user'
    }

    def 'owned image builds coexist with bmuschko in either application order'() {
        given:
        buildFile << '''
            buildscript {
                repositories { gradlePluginPortal() }
                dependencies { classpath 'com.bmuschko:gradle-docker-plugin:10.0.0' }
            }
            plugins { id 'com.netflix.nebula.ospackage-docker' apply false }
        '''
        buildFile << """
            apply plugin: '${first}'
            apply plugin: '${second}'
        """
        configureBuild(false)
        buildFile << '''
            docker {
                url = 'tcp://127.0.0.1:1'
                apiVersion = '1.40'
                registryCredentials {
                    username = 'bmuschko-user'
                    password = 'bmuschko-secret'
                }
            }
            ospackageDocker.registryCredentials {
                url = 'registry.example.test'
                username = 'owned-user'
                password = 'owned-secret'
            }
            assert extensions.getByName('docker') instanceof com.bmuschko.gradle.docker.DockerExtension
            assert extensions.getByName('ospackageDocker') instanceof com.netflix.gradle.plugins.docker.DockerExtension
            assert tasks.buildImage.url.get() != docker.url.get()
            assert tasks.buildImage.apiVersion.get() == '1.44'
            tasks.register('consumerImage', com.bmuschko.gradle.docker.tasks.image.DockerBuildImage)
            assert tasks.consumerImage instanceof com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
            assert tasks.buildImage instanceof com.netflix.gradle.plugins.docker.DockerBuildImage
        '''

        when:
        def firstBuild = runTasks('buildImage', '--configuration-cache', '--info')
        def secondBuild = runTasks('buildImage', '--configuration-cache')

        then:
        firstBuild.task(':buildImage').outcome == TaskOutcome.SUCCESS
        firstBuild.output.contains('docker-java-core-3.7.1.jar')
        secondBuild.task(':buildImage').outcome == TaskOutcome.SUCCESS
        secondBuild.output.contains('Configuration cache entry reused')
        def builds = requests.findAll { it.path.endsWith('/build') }
        builds.size() == 2
        def auth = new JsonSlurper().parse(builds[0].auth.decodeBase64())
        auth['registry.example.test'].username == 'owned-user'
        auth['registry.example.test'].password == 'owned-secret'

        where:
        first                                | second
        'com.netflix.nebula.ospackage-docker' | 'com.bmuschko.docker-remote-api'
        'com.bmuschko.docker-remote-api'       | 'com.netflix.nebula.ospackage-docker'
    }

    def 'a daemon build failure is redacted and removes a stale image ID'() {
        given:
        configureBuild()
        buildFile << """
            ospackageDocker.registryCredentials.password = 'fixture-secret'
        """
        response = '{"stream":"echo fixture-secret\\n"}\n' +
            '{"error":"denied fixture-secret","errorDetail":{"message":"denied fixture-secret"}}\n'
        def imageId = new File(projectDir, 'build/docker/buildImage-image-id.txt')
        imageId.parentFile.mkdirs()
        imageId.text = 'stale-id'

        when:
        def result = runTasksAndFail('buildImage', '--stacktrace')

        then:
        result.output.contains('Docker image build failed: denied [redacted]')
        !result.output.contains('fixture-secret')
        !imageId.exists()
    }

    def 'Dockerfile and package tasks do not resolve the Docker worker configuration'() {
        given:
        configureBuild()
        buildFile << """
            configurations.ospackageDockerWorkerClasspath.incoming.beforeResolve {
                throw new GradleException('Worker dependencies must not resolve')
            }
            tasks.register('fixtureDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'fixture'
                version = '1.0'
                from('src')
            }
            tasks.register('fixtureRpm', com.netflix.gradle.plugins.rpm.Rpm) {
                packageName = 'fixture'
                version = '1.0'
                from('src')
            }
        """

        when:
        def result = runTasks('help', 'createDockerfile', 'fixtureDeb', 'fixtureRpm', '--offline')

        then:
        result.task(':createDockerfile').outcome == TaskOutcome.SUCCESS
        result.task(':fixtureDeb').outcome == TaskOutcome.SUCCESS
        result.task(':fixtureRpm').outcome == TaskOutcome.SUCCESS
        requests.empty
    }

    def 'builds through a TLS endpoint using the configured certificate directory'() {
        given:
        def tls = createTlsContext()
        server.stop(0)
        server = HttpsServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
        ((HttpsServer) server).httpsConfigurator = new HttpsConfigurator(tls)
        startServer()
        configureBuild()
        buildFile << """
            ospackageDocker {
                tlsVerify = true
                certPath = layout.projectDirectory.dir('tls')
            }
        """

        when:
        def result = runTasks('buildImage')

        then:
        result.task(':buildImage').outcome == TaskOutcome.SUCCESS
        requests.count { it.path.endsWith('/build') } == 1
        new File(projectDir, 'build/docker/buildImage-image-id.txt').text.trim() == 'sha256:0123456789abcdef'
    }

    def 'worker runs and reuses configuration on Gradle #testedGradleVersion'() {
        given:
        gradleVersion = testedGradleVersion
        configureBuild()

        when:
        def first = runTasks('buildImage', '--configuration-cache')
        def second = runTasks('buildImage', '--configuration-cache')

        then:
        first.task(':buildImage').outcome == TaskOutcome.SUCCESS
        second.task(':buildImage').outcome == TaskOutcome.SUCCESS
        second.output.contains('Configuration cache entry reused')
        requests.count { it.path.endsWith('/build') } == 2

        where:
        testedGradleVersion << ['8.6', '9.8.2'].findAll {
            it != '8.6' || JavaVersion.current() <= JavaVersion.VERSION_21
        }
    }

    private SSLContext createTlsContext() {
        def directory = new File(projectDir, 'tls')
        directory.mkdirs()
        def store = new File(directory, 'fixture.p12')
        def keytool = new File(System.getProperty('java.home'), 'bin/keytool').absolutePath
        def process = new ProcessBuilder(keytool, '-genkeypair', '-alias', 'fixture', '-keyalg', 'RSA',
            '-keystore', store.absolutePath, '-storetype', 'PKCS12', '-storepass', 'fixture-password',
            '-dname', 'CN=localhost', '-ext', 'SAN=IP:127.0.0.1', '-validity', '1', '-noprompt')
            .redirectErrorStream(true).start()
        def output = process.inputStream.text
        assert process.waitFor() == 0: output
        def keys = KeyStore.getInstance('PKCS12')
        store.withInputStream { keys.load(it, 'fixture-password'.toCharArray()) }
        def certificate = pem('CERTIFICATE', keys.getCertificate('fixture').encoded)
        new File(directory, 'ca.pem').text = certificate
        new File(directory, 'cert.pem').text = certificate
        new File(directory, 'key.pem').text = pem('PRIVATE KEY', keys.getKey('fixture', 'fixture-password'.toCharArray()).encoded)
        def managers = KeyManagerFactory.getInstance(KeyManagerFactory.defaultAlgorithm)
        managers.init(keys, 'fixture-password'.toCharArray())
        def trust = TrustManagerFactory.getInstance(TrustManagerFactory.defaultAlgorithm)
        trust.init(keys)
        def context = SSLContext.getInstance('TLS')
        context.init(managers.keyManagers, trust.trustManagers, null)
        context
    }

    private static String pem(String kind, byte[] bytes) {
        '-----BEGIN ' + kind + '-----\n' +
            Base64.getMimeEncoder(64, '\n'.getBytes('UTF-8')).encodeToString(bytes) +
            '\n-----END ' + kind + '-----\n'
    }

    private void configureBuild(boolean applyPlugin = true) {
        new File(projectDir, 'src').mkdirs()
        new File(projectDir, 'src/app.txt').text = 'application content'
        buildFile << """
            ${applyPlugin ? "plugins { id 'com.netflix.nebula.ospackage-docker' }" : ''}
            repositories { mavenCentral() }
            ospackageDocker {
                url = 'tcp://127.0.0.1:${server.address.port}'
                apiVersion = '1.44'
                tlsVerify = false
            }
            createDockerfile {
                destinationDirectory = layout.buildDirectory.dir('docker-context')
                instruction 'FROM scratch'
                from('src')
            }
        """
    }

    private static Map<String, String> queryParameters(String query) {
        query.split('&').collectEntries { part ->
            def pair = part.split('=', 2)
            [(URLDecoder.decode(pair[0], 'UTF-8')): URLDecoder.decode(pair.length > 1 ? pair[1] : '', 'UTF-8')]
        }
    }

    private static Map<String, String> contextFiles(byte[] body) {
        def source = new ByteArrayInputStream(body)
        InputStream input = body[0] == (byte) 0x1f && body[1] == (byte) 0x8b ? new GZIPInputStream(source) : source
        Map<String, String> files = [:]
        new TarArchiveInputStream(input).withCloseable { tar ->
            def entry
            while ((entry = tar.nextTarEntry) != null) {
                if (!entry.directory) files[entry.name.replaceFirst('^\\./', '')] = new String(tar.readAllBytes(), 'UTF-8')
            }
        }
        files
    }
}
