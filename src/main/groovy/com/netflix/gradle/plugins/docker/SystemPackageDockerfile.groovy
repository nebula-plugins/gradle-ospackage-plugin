package com.netflix.gradle.plugins.docker

import com.netflix.gradle.plugins.packaging.AbstractPackagingCopyAction
import com.netflix.gradle.plugins.packaging.SystemPackagingTask
import org.gradle.tooling.model.UnsupportedMethodException

class SystemPackageDockerfile extends SystemPackagingTask {
    private final DockerfileInstructionManager dockerfileInstructionManager
    private static final ARCHIVE_NAME = 'Dockerfile'

    SystemPackageDockerfile() {
        dockerfileInstructionManager = new DockerfileInstructionManager()
    }

    @Override
    String assembleArchiveName() {
        ARCHIVE_NAME
    }

    @Override
    AbstractPackagingCopyAction createCopyAction() {
        new DockerCopyAction(this, dockerfileInstructionManager)
    }

    @Override
    String getArchString() {
        throw new UnsupportedMethodException('The architecture is defined through FROM instruction in Docker images')
    }

    void instruction(String instruction) {
        dockerfileInstructionManager.addInstruction(instruction)
    }
}
