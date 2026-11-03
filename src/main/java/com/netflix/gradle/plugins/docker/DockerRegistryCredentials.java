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

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

/** Authentication values are passed only to the Docker worker. */
public abstract class DockerRegistryCredentials {
    /** @return the registry address, defaulting to Docker Hub */
    @Internal public abstract Property<String> getUrl();
    /** @return an optional registry username */
    @Internal public abstract Property<String> getUsername();
    /** @return an optional registry password */
    @Internal public abstract Property<String> getPassword();
    /** @return an optional registry email address */
    @Internal public abstract Property<String> getEmail();
}
