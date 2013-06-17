package com.trigonic.gradle.plugins.packaging

import org.gradle.api.Project
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.file.CopySpec
import org.gradle.api.internal.file.copy.CopySpecImpl
import org.gradle.api.internal.project.ProjectInternal

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

//    @Override
//    public CopySpec from(Object sourcePath, Closure c) {
//        use(CopySpecEnhancement) {
//            super.from(sourcePath, c)
//        }
//    }
//
//    @Override
//    public CopySpecImpl into(Object destPath, Closure configureClosure) {
//        use(CopySpecEnhancement) {
//            super.into(destPath, configureClosure)
//        }
//    }
//
//    @Override
//    public CopySpec exclude(Closure excludeSpec) {
//        use(CopySpecEnhancement) {
//            super.exclude(excludeSpec)
//        }
//    }
//
//    @Override
//    public CopySpec filter(Closure closure) {
//        use(CopySpecEnhancement) {
//            super.filter(closure)
//        }
//    }
//
//    @Override
//    public CopySpec rename(Closure closure) {
//        use(CopySpecEnhancement) {
//            super.rename(closure)
//        }
//    }

}
