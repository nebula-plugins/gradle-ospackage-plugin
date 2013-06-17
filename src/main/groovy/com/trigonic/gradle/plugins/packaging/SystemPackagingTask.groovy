/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trigonic.gradle.plugins.packaging

import org.gradle.api.file.CopySourceSpec
import org.gradle.api.file.CopySpec
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.CopyActionImpl
import org.gradle.api.internal.file.copy.CopySpecImpl
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask

import java.lang.reflect.Field
import java.lang.reflect.Modifier

public abstract class SystemPackagingTask extends AbstractArchiveTask {
    private static Logger logger = Logging.getLogger(SystemPackagingTask);

    def fileType = null
    boolean createDirectoryEntry = true
    boolean addParentDirs = true

    final SystemPackagingCopyAction action

    @Delegate
    SystemPackagingExtension exten // Not File extension or ext list of properties, different kind of Extension

    ProjectPackagingExtension parentExten

    // TODO Add conventions to pull from extension

    SystemPackagingTask() {
        super()
        action = new SystemPackagingCopyAction(services.get(FileResolver.class), getVisitor())
        exten = new SystemPackagingExtension()

        // I have no idea where Project came from
        parentExten = project.extensions.findByType(ProjectPackagingExtension)
        if(parentExten) {
            getCopyAction().with(parentExten)
        }
    }

    def applyConventions() {
        // For all mappings, we're only being called if it wasn't explicitly set on the task. In which case, we'll want
        // to pull from the parentExten. And only then would we fallback on some other value.
        ConventionMapping mapping = ((IConventionAware) this).getConventionMapping()

        // Could come from extension
        mapping.map('packageName', {
            // BasePlugin defaults this to pluginConvention.getArchivesBaseName(), which in turns comes form project.name
            parentExten?.getPackageName()?:getBaseName()
        })
        mapping.map('release', { parentExten?.getRelease()?:getClassifier() })
        mapping.map('user', { parentExten?.getUser()?:getPackager() })
        mapping.map('group', { parentExten?.getGroup() })
        mapping.map('packageGroup', { parentExten?.getPackageGroup() })
        mapping.map('buildHost', { parentExten?.getBuildHost()?:getLocalHostName() })
        mapping.map('summary', { parentExten?.getSummary()?:getPackageName() })
        mapping.map('packageDescription', { parentExten?.getPackageDescription()?:project.getDescription() })
        mapping.map('license', { parentExten?.getLicense() })
        mapping.map('packager', { parentExten?.getPackager()?:System.getProperty('user.name', '')  })
        mapping.map('distribution', { parentExten?.getDistribution() })
        mapping.map('vendor', { parentExten?.getVendor() })
        mapping.map('url', { parentExten?.getUrl() })
        mapping.map('sourcePackage', { parentExten?.getSourcePackage() })
        mapping.map('provides', { parentExten?.getProvides()?:getPackageName() })
        mapping.map('installUtils', { parentExten?.getInstallUtils() })
        mapping.map('preInstall', { parentExten?.getPreInstall() })
        mapping.map('postInstall', { parentExten?.getPostInstall() })
        mapping.map('preUninstall', { parentExten?.getPreUninstall() })
        mapping.map('postUninstall', { parentExten?.getPostUninstall() })

        // Task Specific
        mapping.map('archiveName', { assembleArchiveName() })
    }

    abstract String assembleArchiveName();

    protected static String getLocalHostName() {
        try {
            return InetAddress.localHost.hostName
        } catch (UnknownHostException ignore) {
            return "unknown"
        }
    }

    List<Link> getAllLinks() {
        if(parentExten) {
            return getLinks() + parentExten.getLinks()
        } else {
            return getLinks()
        }
    }

    List<Dependency> getAllDependencies() {
        if(parentExten) {
            return getDependencies() + parentExten.getDependencies()
        } else {
            return getDependencies()
        }
    }

    protected <T extends Enum<T>> void aliasEnumValues(T[] values) {
        for (T value : values) {
            assert !ext.hasProperty(value.name())
            ext.set value.name(), value
        }
    }

    protected <T> void aliasStaticInstances(Class<T> forClass) {
        aliasStaticInstances forClass, forClass
    }

    private hasModifier(Field field, int modifier) {
        (field.modifiers & modifier) == modifier
    }

    protected <T, U> void aliasStaticInstances(Class<T> forClass, Class<U> ofClass) {
        for (Field field : forClass.fields) {
            if (field.type == ofClass && hasModifier(field, Modifier.STATIC)) {
                assert !ext.hasProperty(field.name)
                ext.set field.name, field.get(null)
            }
        }
    }

    CopyActionImpl getCopyAction() {
        action
    }

    protected abstract String getArchString();

    protected abstract AbstractPackagingCopySpecVisitor getVisitor();

    class SystemPackagingCopyAction extends CopyActionImpl {
        public SystemPackagingCopyAction(FileResolver resolver, AbstractPackagingCopySpecVisitor visitor) {
            super(resolver, visitor);
        }
    }

//    @Override
//    public AbstractCopyTask from(Object sourcePath, Closure c) {
//        use(CopySpecEnhancement) {
//            super.from(sourcePath, c)
//        }
//    }
//
//    @Override
//    def AbstractArchiveTask into(Object destPath, Closure configureClosure) {
//        use(CopySpecEnhancement) {
//            super.into(destPath, configureClosure)
//        }
//    }
//
//    @Override
//    public AbstractCopyTask exclude(Closure excludeSpec) {
//        use(CopySpecEnhancement) {
//            super.exclude(excludeSpec)
//        }
//    }
//
//    @Override
//    public AbstractCopyTask filter(Closure closure) {
//        use(CopySpecEnhancement) {
//            super.filter(closure)
//        }
//    }
//
//    @Override
//    public AbstractCopyTask rename(Closure closure) {
//        use(CopySpecEnhancement) {
//            super.rename(closure)
//        }
//    }

}
