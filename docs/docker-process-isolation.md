# Docker image builds

This guide describes the unreleased replacement Docker integration. It changes
public task and extension types and is intended for a major release.

## Build an image

Apply the Docker plugin, provide a public repository for the Docker worker
dependencies, and configure the context and image tags:

```groovy
plugins {
    id 'com.netflix.nebula.ospackage-docker'
}

repositories {
    mavenCentral()
}

createDockerfile {
    destinationDirectory = layout.buildDirectory.dir('docker-context')
    instruction 'FROM scratch'
    from('src/main/distribution') {
        into '/app'
    }
}

buildImage {
    images.add('example/application:local')
}
```

Run `./gradlew buildImage` or `./gradlew buildDocker`. Image building needs a
reachable Docker daemon; it does not need the Docker CLI. `createDockerfile`
generates the Dockerfile and stages copied files under `.ospackage/` in the
context directory. Generated `ADD` instructions use JSON array syntax so paths
with spaces are preserved. That staging directory is owned by the task and is
replaced each time it executes. Do not put your own files there or exclude it
through `.dockerignore`.

Running packaging tasks, `help`, or `createDockerfile` does not contact Docker
or resolve Docker worker dependencies. The plugin does not add repositories.

## Connection and authentication

Use `ospackageDocker` for defaults shared by image tasks. Explicit task settings
override these defaults. Unset values use docker-java's environment and Docker
configuration defaults; Docker CLI context selection is not a separate ospackage
feature.

```groovy
ospackageDocker {
    url = providers.environmentVariable('DOCKER_HOST')
    // Set a version supported by your Docker daemon when an explicit version is needed.
    // apiVersion = '1.44'

    registryCredentials {
        url = 'registry.example.com'
        username = providers.environmentVariable('REGISTRY_USER')
        password = providers.environmentVariable('REGISTRY_PASSWORD')
    }
}
```

Local Unix sockets and TCP endpoints are supported. For a TLS-enabled TCP
endpoint, configure `tlsVerify = true` and `certPath` with a directory containing
`ca.pem`, `cert.pem`, and `key.pem`. Windows named-pipe operation has not been
verified. Endpoint URLs must not contain credentials.

The worker can also use authentication from the Docker configuration supported
by docker-java. Explicit registry credentials replace the entry for that
registry. Arbitrary Docker credential-helper integration is not implemented by
ospackage.

Configured registry credentials are omitted from task cache keys, redacted from
the worker's progress and error messages, and never written to the image-ID
result file. They still pass through Gradle's managed task and worker state.
Process isolation does not prevent Gradle's configuration cache from storing
configured values. Use Gradle's configuration-cache encryption and access
controls where appropriate; do not check local caches into source control.
Build arguments and arbitrary output from your Dockerfile are not a secret
storage mechanism.

## Build options and result

`buildImage` is a
`com.netflix.gradle.plugins.docker.DockerBuildImage` task. It exposes:

- `inputDir`, optional `dockerFile`, and `images`.
- `buildArgs`, `labels`, `extraHosts`, and `cacheFrom`.
- Optional `target`, `platform`, `network`, `memory`, and `shmSize`.
- Optional `noCache`, `pull`, `remove`, and `quiet`.
- Connection properties and `registryCredentials` for task-specific overrides.

Collections default to empty. Optional build flags are left unset and follow
docker-java/Docker daemon defaults. The default `inputDir` is the generated
Dockerfile's destination directory. A custom `dockerFile` must be usable with
the selected build context.

After success, `imageIdFile` contains the returned image ID and `imageId` is a
provider for its text. The default result path is
`build/docker/buildImage-image-id.txt`. Read the provider after the image task
has completed; it is not available during configuration. Before submitting
work, the task removes the previous invocation's result. Downstream tasks must
depend on the image task succeeding; a configuration or input-resolution failure
can leave files from a previous build on disk.

Image building runs whenever requested. Gradle build caching and up-to-date
skipping are disabled because the image and tag live in an external daemon.
Docker's own layer cache remains controlled by `noCache` and `cacheFrom`.
Gradle configuration cache is supported.

## Migrating from the bmuschko-backed integration

The plugin no longer applies `com.bmuschko.docker-remote-api` or exposes its
task and extension types.

| Existing usage | Replacement |
| --- | --- |
| `docker { ... }` connection and credentials settings for ospackage image tasks | Move to `ospackageDocker { ... }` |
| Imports or `withType` calls for bmuschko's `DockerBuildImage` | `com.netflix.gradle.plugins.docker.DockerBuildImage` |
| bmuschko extension type lookups | `com.netflix.gradle.plugins.docker.DockerExtension` |
| Applying bmuschko after the ospackage base plugin to create image tasks | Apply `com.netflix.nebula.ospackage-docker` |
| Direct `dockerClient` access, model callbacks or task subclassing | Task dependencies, build progress and the image-ID result |

The task names `createDockerfile`, `buildImage` and `buildDocker` remain.
Other bmuschko tasks and callbacks are not supplied. Both integrations can share
a project in either application order: bmuschko owns `docker`, and ospackage owns
`ospackageDocker`. Their connection and credential settings remain independent.
Task names are still project-wide; choose distinct names for custom bmuschko tasks.

Ordinary ospackage RPM/DEB packaging can still share a project with bmuschko.
If a build previously obtained bmuschko transitively through ospackage, declare
its dependency explicitly and continue applying its plugin. That path preserves
bmuschko's own APIs but also retains its bundled-class exposure. It does not
require migrating independent bmuschko tasks to the ospackage image-building API.

For example, in a build that only needs ospackage for packaging:

```groovy
buildscript {
    repositories { gradlePluginPortal() }
    dependencies { classpath 'com.bmuschko:gradle-docker-plugin:10.0.0' }
}
plugins { id 'com.netflix.nebula.ospackage' }
apply plugin: 'com.bmuschko.docker-remote-api'
```

Choose and test a bmuschko version compatible with the existing build; 10.0.0
above is an example, not an automatic upgrade recommendation.

The Docker client now runs in a process-isolated Gradle worker. Its dependencies
are absent from the plugin's runtime graph, so they do not replace another
buildscript library's Docker classes. Independently loading bmuschko through
another plugin can still expose its bundled classes.

## Worker dependency overrides

The default client and HTTP transport are docker-java 3.7.1. To supply a tested
alternative, declare the complete compatible client/transport set on
`ospackageDockerWorkerDependencies`. Any explicit dependency on this
configuration replaces the default set. `ospackageDockerWorkerClasspath` is
resolvable only; dependencies cannot be declared directly on it.

Dependency locking and resolution policies still apply to this configuration.
It is separate from the plugin's, application's and buildscript's dependency
graphs. Overridden versions and transports require their own compatibility
testing.

## Additional image tasks

Apply the explicit ospackage Docker plugin before registering additional tasks.
All tasks of the owned type inherit the worker classpath, connection and registry
defaults. Supply the context and its task dependency for each custom image:

```groovy
import com.netflix.gradle.plugins.docker.DockerBuildImage

tasks.register('buildAnotherImage', DockerBuildImage) {
    dependsOn tasks.named('createDockerfile')
    inputDir = tasks.named('createDockerfile').flatMap { it.destinationDirectory }
    images.add('example/another:local')
}
```

## Live integration testing

The automated HTTP and TLS fixtures do not need Docker. To run the live
scratch-image test as well, set `OSPACKAGE_DOCKER_INTEGRATION_TEST=true` when
running the test suite. That test uses the Docker CLI to inspect and remove its
test image tag; the production plugin does not invoke the CLI. Set
`DOCKER_HOST` when the daemon is not discoverable through the default client
configuration.
