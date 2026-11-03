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
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

/** Connection and registry defaults shared by ospackage image tasks. */
public abstract class DockerExtension {
    private final DockerRegistryCredentials registryCredentials;

    /**
     * Creates managed registry credentials.
     * @param objects Gradle's object factory
     */
    @Inject
    public DockerExtension(ObjectFactory objects) {
        registryCredentials = objects.newInstance(DockerRegistryCredentials.class);
    }

    /** @return an optional Docker daemon endpoint */
    public abstract Property<String> getUrl();
    /** @return a directory containing Docker TLS certificates and key */
    public abstract DirectoryProperty getCertPath();
    /** @return whether TCP connections use verified TLS */
    public abstract Property<Boolean> getTlsVerify();
    /** @return an optional Docker API version */
    public abstract Property<String> getApiVersion();

    /** @return the shared registry authentication defaults */
    public DockerRegistryCredentials getRegistryCredentials() {
        return registryCredentials;
    }

    /**
     * Configures shared registry authentication defaults.
     * @param action registry credentials configuration
     */
    public void registryCredentials(Action<? super DockerRegistryCredentials> action) {
        action.execute(registryCredentials);
    }
}
