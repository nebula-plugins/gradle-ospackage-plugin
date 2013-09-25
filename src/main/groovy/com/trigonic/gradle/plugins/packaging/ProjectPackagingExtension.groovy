package com.trigonic.gradle.plugins.packaging

import org.gradle.api.Project
import org.gradle.api.internal.file.copy.DefaultCopySpec
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator

/**
 * An extension which can be attached to the project. This is a superset of SystemPackagingExtension because we don't
 * want the @Delegate to inherit the copy spec parts.
 */
class ProjectPackagingExtension extends DefaultCopySpec {
    @Delegate
    SystemPackagingExtension exten // Not File extension or ext list of properties, different kind of Extension

    // @Inject // Not supported yet.
    public ProjectPackagingExtension(Project project) {
        super( ((ProjectInternal) project).getFileResolver(), project.getServices().get(Instantiator.class) )
        exten = new SystemPackagingExtension()
    }

    @Override
    public DefaultCopySpec from(Object sourcePath, Closure c) {
        use(CopySpecEnhancement) {
            super.from(sourcePath, c)
        }
        return this
    }

    @Override
    public DefaultCopySpec into(Object destPath, Closure configureClosure) {
        use(CopySpecEnhancement) {
            super.into(destPath, configureClosure)
        }
        return this
    }

    @Override
    public DefaultCopySpec exclude(Closure excludeSpec) {
        use(CopySpecEnhancement) {
            super.exclude(excludeSpec)
        }
        return this
    }

    @Override
    public DefaultCopySpec filter(Closure closure) {
        use(CopySpecEnhancement) {
            super.filter(closure)
        }
        return this
    }

    @Override
    public DefaultCopySpec rename(Closure closure) {
        use(CopySpecEnhancement) {
            super.rename(closure)
        }
        return this
    }

}
