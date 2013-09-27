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

import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask

public abstract class SystemPackagingTask extends AbstractArchiveTask {
    private static Logger logger = Logging.getLogger(SystemPackagingTask);

    @Delegate
    SystemPackagingExtension exten // Not File extension or ext list of properties, different kind of Extension

    ProjectPackagingExtension parentExten

    // TODO Add conventions to pull from extension

    SystemPackagingTask() {
        super()
        exten = new SystemPackagingExtension()

        // I have no idea where Project came from
        parentExten = project.extensions.findByType(ProjectPackagingExtension)
        if(parentExten) {
            getRootSpec().with(parentExten.delegateCopySpec)
        }
    }

    // TODO Move outside task, since it's specific to a plugin
    protected void applyConventions() {
        // For all mappings, we're only being called if it wasn't explicitly set on the task. In which case, we'll want
        // to pull from the parentExten. And only then would we fallback on some other value.
        ConventionMapping mapping = ((IConventionAware) this).getConventionMapping()

        // Could come from extension
        mapping.map('packageName', {
            // BasePlugin defaults this to pluginConvention.getArchivesBaseName(), which in turns comes form project.name
            parentExten?.getPackageName()?:getBaseName()
        })
        mapping.map('release', { parentExten?.getRelease()?:getClassifier() })
        mapping.map('version', { parentExten?.getVersion()?:project.getVersion().toString() })
        mapping.map('user', { parentExten?.getUser()?:getPackager() })
        mapping.map('permissionGroup', { parentExten?.getPermissionGroup()?:'' })
        mapping.map('packageGroup', { parentExten?.getPackageGroup() })
        mapping.map('buildHost', { parentExten?.getBuildHost()?:getLocalHostName() })
        mapping.map('summary', { parentExten?.getSummary()?:getPackageName() })
        mapping.map('packageDescription', { parentExten?.getPackageDescription()?:project.getDescription() })
        mapping.map('license', { parentExten?.getLicense()?:'' })
        mapping.map('packager', { parentExten?.getPackager()?:System.getProperty('user.name', '')  })
        mapping.map('distribution', { parentExten?.getDistribution()?:'' })
        mapping.map('vendor', { parentExten?.getVendor()?:'' })
        mapping.map('url', { parentExten?.getUrl()?:'' })
        mapping.map('sourcePackage', { parentExten?.getSourcePackage()?:'' })
        mapping.map('provides', { parentExten?.getProvides()?:getPackageName() })
        mapping.map('createDirectoryEntry', { parentExten?.getCreateDirectoryEntry()?:false })

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

    @Override
    @TaskAction
    protected void copy() {
        use(CopySpecEnhancement) {
            super.copy()
        }
    }

    def getAllPreInstallCommands() {
        return getPreInstallCommands() + parentExten?.getPreInstallCommands()
    }

    def getAllPostInstallCommands() {
        return getPostInstallCommands() + parentExten?.getPostInstallCommands()
    }

    def getAllPreUninstallCommands() {
        return getPreUninstallCommands() + parentExten?.getPreUninstallCommands()
    }

    def getAllPostUninstallCommands() {
        return getPostUninstallCommands() + parentExten?.getPostUninstallCommands()
    }

    def getAllCommonCommands() {
        return getCommonCommands() + parentExten?.getCommonCommands()
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

    @Override
    abstract AbstractPackagingCopyAction createCopyAction()

    protected abstract String getArchString();

    @Override
    public AbstractCopyTask from(Object sourcePath, Closure c) {
        use(CopySpecEnhancement) {
            getMainSpec().from(sourcePath, c);
        }
        return this
    }

    @Override
    def AbstractArchiveTask into(Object destPath, Closure configureClosure) {
        use(CopySpecEnhancement) {
            getMainSpec().into(destPath, configureClosure)
        }
        return this
    }

    @Override
    public AbstractCopyTask exclude(Closure excludeSpec) {
        use(CopySpecEnhancement) {
            getMainSpec().exclude(excludeSpec)
        }
        return this
    }

    @Override
    public AbstractCopyTask filter(Closure closure) {
        use(CopySpecEnhancement) {
            getMainSpec().filter(closure)
        }
        return this
    }

    @Override
    public AbstractCopyTask rename(Closure closure) {
        use(CopySpecEnhancement) {
            getMainSpec().rename(closure)
        }
        return this
    }

}
