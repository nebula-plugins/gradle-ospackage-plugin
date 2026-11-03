/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.gradle.plugins.docker;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;

/** Builds an image in a separate JVM without adding Docker clients to the buildscript classpath. */
@DisableCachingByDefault(because = "Docker images and tags are external daemon state")
public abstract class DockerBuildImage extends DefaultTask implements DockerBuildOptions {
    /** @return Gradle's isolated worker executor */
    @Inject public abstract WorkerExecutor getWorkerExecutor();
    /** @return the isolated client's complete runtime classpath */
    @Classpath public abstract ConfigurableFileCollection getWorkerClasspath();
    /** @return registry authentication overrides for this task */
    @Nested public abstract DockerRegistryCredentials getRegistryCredentials();
    /** @return the image ID written only after a successful worker build */
    @OutputFile public abstract RegularFileProperty getImageIdFile();

    /** Sets collection and result-file conventions and disables up-to-date skipping. */
    public DockerBuildImage() {
        getImages().convention(Set.of());
        getBuildArgs().convention(Map.of());
        getLabels().convention(Map.of());
        getExtraHosts().convention(Set.of());
        getCacheFrom().convention(Set.of());
        getImageIdFile().convention(getProject().getLayout().getBuildDirectory()
                .file("docker/" + getName() + "-image-id.txt"));
        // DefaultTask narrows getOutputs() to an internal type; call the public API.
        ((Task) this).getOutputs().upToDateWhen(task -> false);
    }

    /** @return the result text, to be read after this task succeeds */
    @Internal
    public Provider<String> getImageId() {
        return getProviders().fileContents(getImageIdFile()).getAsText().map(String::trim);
    }

    /** @return Gradle's provider factory */
    @Inject
    protected abstract org.gradle.api.provider.ProviderFactory getProviders();

    /**
     * Configures registry authentication overrides.
     * @param action configuration for this task's registry credentials
     */
    public void registryCredentials(Action<? super DockerRegistryCredentials> action) {
        action.execute(getRegistryCredentials());
    }

    /**
     * Submits the build and waits for its result.
     * @throws IOException if an earlier result cannot be removed
     */
    @TaskAction
    public void buildImage() throws IOException {
        // Never return an earlier invocation's image ID after a failed build.
        Files.deleteIfExists(getImageIdFile().get().getAsFile().toPath());
        if (getUrl().isPresent()) {
            var url = getUrl().get();
            try {
                if (java.net.URI.create(url).getUserInfo() != null) {
                    throw new GradleException("Docker URLs must not contain credentials; use registryCredentials.");
                }
            } catch (IllegalArgumentException exception) {
                throw new GradleException("Invalid Docker URL; configure ospackageDocker.url with a valid endpoint.");
            }
        }
        var queue = getWorkerExecutor().processIsolation(spec ->
                spec.getClasspath().from(getWorkerClasspath()));
        queue.submit(DockerBuildWorkAction.class, parameters -> {
            parameters.getInputDir().set(getInputDir());
            parameters.getDockerFile().set(getDockerFile());
            parameters.getImages().set(getImages());
            parameters.getBuildArgs().set(getBuildArgs());
            parameters.getLabels().set(getLabels());
            parameters.getExtraHosts().set(getExtraHosts());
            parameters.getCacheFrom().set(getCacheFrom());
            parameters.getTarget().set(getTarget());
            parameters.getPlatform().set(getPlatform());
            parameters.getNetwork().set(getNetwork());
            parameters.getMemory().set(getMemory());
            parameters.getShmSize().set(getShmSize());
            parameters.getNoCache().set(getNoCache());
            parameters.getPull().set(getPull());
            parameters.getRemove().set(getRemove());
            parameters.getQuiet().set(getQuiet());
            parameters.getUrl().set(getUrl());
            parameters.getApiVersion().set(getApiVersion());
            parameters.getTlsVerify().set(getTlsVerify());
            parameters.getCertPath().set(getCertPath());
            parameters.getImageIdFile().set(getImageIdFile());
            parameters.getRegistryUrl().set(getRegistryCredentials().getUrl());
            parameters.getRegistryUsername().set(getRegistryCredentials().getUsername());
            parameters.getRegistryPassword().set(getRegistryCredentials().getPassword());
            parameters.getRegistryEmail().set(getRegistryCredentials().getEmail());
        });
        queue.await();
    }
}
