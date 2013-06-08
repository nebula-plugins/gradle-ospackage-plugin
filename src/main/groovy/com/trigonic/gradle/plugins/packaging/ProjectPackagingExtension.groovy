package com.trigonic.gradle.plugins.packaging

import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.CopySpecImpl
import org.gradle.api.internal.project.ProjectInternal

import javax.inject.Inject

/**
 * An extension which can be attached to the project. This is a superset of SystemPackagingExtension because we don't
 * want the @Delegate to inherit the copy spec parts.
 */
class ProjectPackagingExtension extends CopySpecImpl {
    @Delegate
    SystemPackagingExtension exten // Not File extension or ext list of properties, different kind of Extension

    // @Inject // Not supported yet.
    public ProjectPackagingExtension(Project project) {
        super( ((ProjectInternal) project).getFileResolver() )
        exten = new SystemPackagingExtension()
    }

}
