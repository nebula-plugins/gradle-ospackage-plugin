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

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logging;
import org.gradle.workers.WorkAction;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Docker references are confined to execution in the isolated worker.
 * @hidden
 */
public abstract class DockerBuildWorkAction implements WorkAction<DockerBuildParameters> {
    @Override
    public void execute() {
        var parameters = getParameters();
        var secrets = new ArrayList<String>();
        remember(secrets, parameters.getRegistryUsername().getOrNull());
        remember(secrets, parameters.getRegistryPassword().getOrNull());
        remember(secrets, parameters.getRegistryEmail().getOrNull());
        try {
            var builder = DefaultDockerClientConfig.createDefaultConfigBuilder();
            if (parameters.getUrl().isPresent()) builder.withDockerHost(parameters.getUrl().get());
            if (parameters.getApiVersion().isPresent()) builder.withApiVersion(parameters.getApiVersion().get());
            if (parameters.getCertPath().isPresent()) {
                builder.withDockerCertPath(parameters.getCertPath().get().getAsFile().getAbsolutePath());
            }
            if (parameters.getTlsVerify().isPresent()) builder.withDockerTlsVerify(parameters.getTlsVerify().get());
            var config = builder.build();
            Logging.getLogger(DockerBuildWorkAction.class).info("Docker worker JVM {} (client: {})",
                    ProcessHandle.current().pid(), DockerClientImpl.class.getProtectionDomain().getCodeSource().getLocation());
            var auth = config.getAuthConfigurations();
            if (parameters.getRegistryUsername().isPresent() || parameters.getRegistryPassword().isPresent()) {
                auth.addConfig(new AuthConfig()
                        .withRegistryAddress(parameters.getRegistryUrl().getOrElse(AuthConfig.DEFAULT_SERVER_ADDRESS))
                        .withUsername(parameters.getRegistryUsername().getOrNull())
                        .withPassword(parameters.getRegistryPassword().getOrNull())
                        .withEmail(parameters.getRegistryEmail().getOrNull()));
            }
            for (var credentials : auth.getConfigs().values()) {
                remember(secrets, credentials.getUsername());
                remember(secrets, credentials.getPassword());
                remember(secrets, credentials.getEmail());
                remember(secrets, credentials.getAuth());
                remember(secrets, credentials.getIdentitytoken());
                remember(secrets, credentials.getRegistrytoken());
            }
            String imageId;
            try (var transport = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .connectionTimeout(Duration.ofSeconds(30))
                    .responseTimeout(Duration.ofMinutes(30))
                    .build();
                 var client = DockerClientImpl.getInstance(config, transport);
                 var command = client.buildImageCmd(parameters.getInputDir().get().getAsFile());
                 var progress = new BuildProgress(secrets)) {
                command.withTags(parameters.getImages().get());
                command.withLabels(parameters.getLabels().get());
                command.withExtraHosts(parameters.getExtraHosts().get());
                command.withCacheFrom(parameters.getCacheFrom().get());
                command.withBuildAuthConfigs(auth);
                parameters.getBuildArgs().get().forEach(command::withBuildArg);
                if (parameters.getDockerFile().isPresent()) {
                    command.withDockerfile(parameters.getDockerFile().get().getAsFile());
                }
                if (parameters.getTarget().isPresent()) command.withTarget(parameters.getTarget().get());
                if (parameters.getPlatform().isPresent()) command.withPlatform(parameters.getPlatform().get());
                if (parameters.getNetwork().isPresent()) command.withNetworkMode(parameters.getNetwork().get());
                if (parameters.getMemory().isPresent()) command.withMemory(parameters.getMemory().get());
                if (parameters.getShmSize().isPresent()) command.withShmsize(parameters.getShmSize().get());
                if (parameters.getNoCache().isPresent()) command.withNoCache(parameters.getNoCache().get());
                if (parameters.getPull().isPresent()) command.withPull(parameters.getPull().get());
                if (parameters.getRemove().isPresent()) command.withRemove(parameters.getRemove().get());
                if (parameters.getQuiet().isPresent()) command.withQuiet(parameters.getQuiet().get());
                command.exec(progress);
                imageId = progress.result.get();
            }
            var output = parameters.getImageIdFile().get().getAsFile().toPath();
            Files.createDirectories(output.getParent());
            var temporary = Files.createTempFile(output.getParent(), "image-id-", ".tmp");
            try {
                Files.writeString(temporary, imageId + System.lineSeparator(), StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GradleException("Docker image build was interrupted.");
        } catch (Exception exception) {
            var failure = exception instanceof ExecutionException ? exception.getCause() : exception;
            // Original exceptions can contain daemon responses and authentication values.
            throw new GradleException("Docker image build failed: " + redact(
                    failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage(), secrets));
        }
    }

    private static void remember(List<String> secrets, String value) {
        if (value != null && !value.isEmpty()) secrets.add(value);
    }

    private static String redact(String message, List<String> secrets) {
        var redacted = message;
        // Replace longer values first when credentials overlap.
        for (var secret : secrets.stream().sorted((a, b) -> Integer.compare(b.length(), a.length())).toList()) {
            redacted = redacted.replace(secret, "[redacted]");
        }
        return redacted;
    }

    private static final class BuildProgress implements ResultCallback<BuildResponseItem> {
        private final CompletableFuture<String> result = new CompletableFuture<>();
        private final List<String> secrets;
        private Closeable stream;
        private String imageId;
        private String error;

        private BuildProgress(List<String> secrets) {
            this.secrets = secrets;
        }

        @Override public void onStart(Closeable stream) { this.stream = stream; }

        @Override
        public void onNext(BuildResponseItem item) {
            if (item.isErrorIndicated()) {
                error = item.getError() != null ? item.getError() : item.getErrorDetail().getMessage();
                if (error == null) error = "Docker reported a build error.";
            }
            if (item.isBuildSuccessIndicated()) imageId = item.getImageId();
            // docker-java retains the build-specific aux.ID field in the raw response map.
            if (item.getRawValues().get("aux") instanceof java.util.Map<?, ?> auxiliary
                    && auxiliary.get("ID") instanceof String id) {
                imageId = id;
            }
            if (item.getStream() != null) {
                Logging.getLogger(DockerBuildWorkAction.class).lifecycle(
                        "{}", redact(item.getStream().stripTrailing(), secrets));
            }
        }

        @Override public void onError(Throwable throwable) { result.completeExceptionally(throwable); }

        @Override
        public void onComplete() {
            if (error != null) result.completeExceptionally(new IOException(error));
            else if (imageId == null || imageId.isBlank()) {
                result.completeExceptionally(new IOException("Docker returned no image ID."));
            } else result.complete(imageId);
        }

        @Override public void close() throws IOException { if (stream != null) stream.close(); }
    }
}
