package com.netflix.gradle.plugins.docker

import com.netflix.gradle.plugins.packaging.AbstractPackagingCopyAction
import com.netflix.gradle.plugins.packaging.SystemPackagingTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.tooling.model.UnsupportedMethodException
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

@DisableCachingByDefault
abstract class SystemPackageDockerfile extends SystemPackagingTask {
    private static final ARCHIVE_NAME = 'Dockerfile'

    @Input
    abstract ListProperty<String> getInstructions()

    @OutputDirectory
    Provider<Directory> getContextFilesDirectory() {
        destinationDirectory.dir('.ospackage')
    }

    @Inject
    abstract FileSystemOperations getFileSystemOperations()

    @Inject
    SystemPackageDockerfile(ProjectLayout projectLayout) {
        super(projectLayout)
        instructions.convention([])
        // Keep a source for instruction-only builds, but exclude it during copy
        // processing so global content filters never need to read the marker.
        from(markerFile()) { CopySpec spec ->
            spec.eachFile { FileCopyDetails details -> details.exclude() }
        }
    }

    @Override
    String assembleArchiveName() {
        ARCHIVE_NAME
    }

    static File markerFile() {
        File codeSource = new File(SystemPackageDockerfile.protectionDomain.codeSource.location.toURI())
        codeSource.isDirectory()
            ? new File(codeSource, 'com/netflix/gradle/plugins/docker/SystemPackageDockerfile.class')
            : codeSource
    }

    @Override
    AbstractPackagingCopyAction createCopyAction() {
        new DockerCopyAction(this, new DockerfileInstructionManager(instructions.get()))
    }

    @Override
    String getArchString() {
        throw new UnsupportedMethodException('The architecture is defined through FROM instruction in Docker images')
    }

    void instruction(String instruction) {
        instructions.add(instruction)
    }
}
