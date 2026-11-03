package com.netflix.gradle.plugins.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment

class OsPackageDockerPlugin implements Plugin<Project> {
    static final String WORKER_DEPENDENCIES = 'ospackageDockerWorkerDependencies'
    static final String WORKER_CLASSPATH = 'ospackageDockerWorkerClasspath'

    @Override
    void apply(Project project) {
        project.plugins.apply('com.netflix.nebula.ospackage-base')
        project.plugins.apply('com.netflix.nebula.ospackage-docker-base')

        def extension = project.extensions.create('ospackageDocker', DockerExtension)
        def versions = new Properties()
        OsPackageDockerPlugin.getResourceAsStream('docker-worker.properties').withCloseable {
            versions.load(it)
        }
        def declarations = project.configurations.dependencyScope(WORKER_DEPENDENCIES) { configuration ->
            configuration.description = 'Dependencies used only by the process-isolated Docker worker'
            configuration.defaultDependencies { dependencies ->
                dependencies.add(project.dependencies.create("com.github.docker-java:docker-java-core:${versions.getProperty('version')}"))
                dependencies.add(project.dependencies.create("com.github.docker-java:docker-java-transport-httpclient5:${versions.getProperty('version')}"))
            }
        }
        project.configurations.resolvable(WORKER_CLASSPATH) { configuration ->
            configuration.extendsFrom(declarations.get())
            configuration.attributes { attributes ->
                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage, Usage.JAVA_RUNTIME))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category, Category.LIBRARY))
                attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements, LibraryElements.JAR))
                attributes.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                    project.objects.named(TargetJvmEnvironment, TargetJvmEnvironment.STANDARD_JVM))
            }
        }

        def dockerfile = project.tasks.register(OsPackageDockerBasePlugin.CREATE_DOCKERFILE_TASK_NAME, SystemPackageDockerfile)
        project.tasks.withType(DockerBuildImage).configureEach { task ->
            task.group = 'Docker'
            task.description = 'Builds a Docker image in a process-isolated worker.'
            task.workerClasspath.from(project.configurations.named(WORKER_CLASSPATH))
            task.url.convention(extension.url)
            task.apiVersion.convention(extension.apiVersion)
            task.certPath.convention(extension.certPath)
            task.tlsVerify.convention(extension.tlsVerify)
            task.registryCredentials.url.convention(extension.registryCredentials.url)
            task.registryCredentials.username.convention(extension.registryCredentials.username)
            task.registryCredentials.password.convention(extension.registryCredentials.password)
            task.registryCredentials.email.convention(extension.registryCredentials.email)
        }
        def image = project.tasks.register(OsPackageDockerBasePlugin.BUILD_IMAGE_TASK_NAME, DockerBuildImage) { task ->
            task.dependsOn(dockerfile)
            task.inputDir.convention(dockerfile.flatMap { it.destinationDirectory })
        }
        project.tasks.register(OsPackageDockerBasePlugin.AGGREGATION_TASK_NAME) {
            it.group = 'Docker'
            it.dependsOn(image)
        }
    }
}
