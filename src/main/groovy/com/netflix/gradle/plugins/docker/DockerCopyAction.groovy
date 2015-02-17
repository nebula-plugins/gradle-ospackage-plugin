package com.netflix.gradle.plugins.docker

import com.netflix.gradle.plugins.packaging.AbstractPackagingCopyAction
import com.netflix.gradle.plugins.packaging.Dependency
import com.netflix.gradle.plugins.packaging.Directory
import com.netflix.gradle.plugins.packaging.Link
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal

class DockerCopyAction extends AbstractPackagingCopyAction {
    private final DockerfileInstructionManager dockerfileInstructionManager

    DockerCopyAction(Docker task, DockerfileInstructionManager dockerfileInstructionManager) {
        super(task)
        this.dockerfileInstructionManager = dockerfileInstructionManager
    }

    @Override
    protected void visitDir(FileCopyDetailsInternal dirDetails,def Object specToLookAt) {}

    @Override
    protected void visitFile(FileCopyDetailsInternal fileDetails,def Object specToLookAt) {
        logger.debug "adding file {}", fileDetails.relativePath.pathString

        def inputFile = extractFile(fileDetails)
        def targetFile = "/$fileDetails.path"
        dockerfileInstructionManager.addInstruction("ADD $inputFile.name $targetFile")
    }

    @Override
    protected void addLink(Link link) {}

    @Override
    protected void addDependency(Dependency dependency) {}

    @Override
    protected void addConflict(Dependency dependency) {}

    @Override
    protected void addObsolete(Dependency dependency) {}

    @Override
    protected void addDirectory(Directory directory) {}

    @Override
    protected void end() {
        dockerfileInstructionManager.create(task.archivePath)
    }
}
