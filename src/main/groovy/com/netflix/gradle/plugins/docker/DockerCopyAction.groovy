package com.netflix.gradle.plugins.docker

import com.netflix.gradle.plugins.packaging.AbstractPackagingCopyAction
import com.netflix.gradle.plugins.packaging.Dependency
import com.netflix.gradle.plugins.packaging.Directory
import com.netflix.gradle.plugins.packaging.Link
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.internal.file.copy.CopyAction
import groovy.json.JsonOutput

class DockerCopyAction extends AbstractPackagingCopyAction<SystemPackageDockerfile> {
    private final DockerfileInstructionManager dockerfileInstructionManager

    DockerCopyAction(SystemPackageDockerfile task, DockerfileInstructionManager dockerfileInstructionManager) {
        super(task)
        this.dockerfileInstructionManager = dockerfileInstructionManager
    }

    @Override
    void startVisit(CopyAction action) {
        super.startVisit(action)
        task.fileSystemOperations.delete { spec -> spec.delete(task.contextFilesDirectory) }
        task.contextFilesDirectory.get().asFile.mkdirs()
    }

    @Override
    protected void visitDir(FileCopyDetailsInternal dirDetails,def Object specToLookAt) {}

    @Override
    protected void visitFile(FileCopyDetailsInternal fileDetails,def Object specToLookAt) {
        logger.debug "adding file {}", fileDetails.relativePath.pathString

        String targetFile = "/$fileDetails.path"
        File staging = task.contextFilesDirectory.get().asFile
        File contextFile = new File(staging, fileDetails.path)
        if (!contextFile.toPath().normalize().startsWith(staging.toPath().normalize())) {
            throw new IllegalArgumentException('Docker context paths must stay within the context directory')
        }
        contextFile.parentFile.mkdirs()
        fileDetails.copyTo(contextFile)
        String sourcePath = ".ospackage/$fileDetails.path"
        dockerfileInstructionManager.addInstruction('ADD ' + JsonOutput.toJson([sourcePath, targetFile]))
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
    protected void addProvides(Dependency dependency) {}

    @Override
    protected void end() {
        dockerfileInstructionManager.create(task.archiveFile.get().asFile)
    }
}
