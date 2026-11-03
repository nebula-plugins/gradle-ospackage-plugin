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

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

/** Managed values shared by the task and the worker protocol; no Docker client types. */
public interface DockerBuildOptions {
    /** @return the directory uploaded as the Docker build context */
    @InputDirectory @PathSensitive(PathSensitivity.RELATIVE)
    DirectoryProperty getInputDir();

    /** @return an optional Dockerfile override within the build context */
    @InputFile @Optional @PathSensitive(PathSensitivity.RELATIVE)
    RegularFileProperty getDockerFile();

    /** @return tags assigned to the built image */
    @Input SetProperty<String> getImages();
    /** @return Dockerfile build arguments */
    @Input MapProperty<String, String> getBuildArgs();
    /** @return labels assigned to the image */
    @Input MapProperty<String, String> getLabels();
    /** @return additional host mappings in host:address form */
    @Input SetProperty<String> getExtraHosts();
    /** @return image references used as layer-cache sources */
    @Input SetProperty<String> getCacheFrom();
    /** @return the optional Dockerfile target stage */
    @Input @Optional Property<String> getTarget();
    /** @return the optional target platform, such as linux/amd64 */
    @Input @Optional Property<String> getPlatform();
    /** @return the optional build network mode */
    @Input @Optional Property<String> getNetwork();
    /** @return the optional build memory limit in bytes */
    @Input @Optional Property<Long> getMemory();
    /** @return the optional shared-memory size in bytes */
    @Input @Optional Property<Long> getShmSize();
    /** @return whether Docker's layer cache is disabled */
    @Input @Optional Property<Boolean> getNoCache();
    /** @return whether base images are always pulled */
    @Input @Optional Property<Boolean> getPull();
    /** @return whether intermediate containers are removed */
    @Input @Optional Property<Boolean> getRemove();
    /** @return whether Docker suppresses build output */
    @Input @Optional Property<Boolean> getQuiet();

    /** @return an optional Docker daemon endpoint */
    @Input @Optional Property<String> getUrl();
    /** @return an optional Docker API version */
    @Input @Optional Property<String> getApiVersion();
    /** @return whether the TCP connection uses verified TLS */
    @Input @Optional Property<Boolean> getTlsVerify();
    /** @return a directory containing ca.pem, cert.pem and key.pem */
    @Internal DirectoryProperty getCertPath();
}
