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
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.CopyActionImpl
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.bundling.AbstractArchiveTask

import java.lang.reflect.Field
import java.lang.reflect.Modifier

public abstract class SystemPackagingTask extends AbstractArchiveTask {
    private static Logger logger = Logging.getLogger(SystemPackagingTask);

    def fileType = null
    def createDirectoryEntry = null
    boolean addParentDirs = true

    final SystemPackagingCopyAction action

    @Delegate
    SystemPackagingExtension exten // Not File extension or ext list of properties, different kind of Extension

    SystemPackagingExtension parentExten

    // TODO Add conventions to pull from extension

    SystemPackagingTask() {
        super()
        action = new SystemPackagingCopyAction(services.get(FileResolver.class), getVisitor())
        exten = new SystemPackagingExtension()

        // I have no idea where Project came from
        parentExten = project.extensions.findByType(SystemPackagingExtension)
        if(!parentExten) {
            // For example, SystemPackagingPlugin was not applied. In which case, we'll initialize to something to make
            // null checks later so much easier. We expect every value in the extension to be unset.
            parentExten = new SystemPackagingExtension();
        }
    }

    def applyConventions() {
        // For all mappings, we're only being called if it wasn't explicitly set on the task. In which case, we'll want
        // to pull from the parentExten. And only then would we fallback on some other value.
        ConventionMapping mapping = ((IConventionAware) this).getConventionMapping()

        // Could come from extension
        mapping.map('packageName', {
            // BasePlugin defaults this to pluginConvention.getArchivesBaseName(), which in turns comes form project.name
            parentExten.getPackageName()?:getBaseName()
        })
        mapping.map('release', { parentExten.getRelease()?:getClassifier() })
        mapping.map('user', { parentExten.getUser() })
        mapping.map('group', { parentExten.getGroup() })
        mapping.map('packageGroup', { parentExten.getPackageGroup() })
        mapping.map('buildHost', { parentExten.getBuildHost()?:getLocalHostName() })
        mapping.map('summary', { parentExten.getSummary() })
        mapping.map('packageDescription', {
            def possible = parentExten.getPackageDescription()
            def progDesc = project.getDescription()
            parentExten.getPackageDescription()?:project.getDescription()
        })
        mapping.map('license', { parentExten.getLicense() })
        mapping.map('packager', { parentExten.getPackager()?:System.getProperty('user.name', '')  })
        mapping.map('distribution', { parentExten.getDistribution() })
        mapping.map('vendor', { parentExten.getVendor() })
        mapping.map('url', { parentExten.getUrl() })
        mapping.map('sourcePackage', { parentExten.getSourcePackage() })
        mapping.map('provides', { parentExten.getProvides() })
        mapping.map('installUtils', { parentExten.getInstallUtils() })
        mapping.map('preInstall', { parentExten.getPreInstall() })
        mapping.map('postInstall', { parentExten.getPostInstall() })
        mapping.map('preUninstall', { parentExten.getPreUninstall() })
        mapping.map('postUninstall', { parentExten.getPostUninstall() })

        // Task specific
        mapping.map('archiveName', { assembleArchiveName() })
    }

    protected String assembleArchiveName() {
        String.format("%s-%s-%s.%s.%s",
                getPackageName(),
                getVersion(),
                getRelease(),
                getArchString(),
                getExtension())
    }

    protected static String getLocalHostName() {
        try {
            return InetAddress.localHost.hostName
        } catch (UnknownHostException ignore) {
            return "unknown"
        }
    }

    List<Link> getAllLinks() {
        return getLinks() + parentExten.getLinks()
    }

    List<Dependency> getAllDependencies() {
        return getDependencies() + parentExten.getDependencies()
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

    protected abstract SystemPackagingCopySpecVisitor getVisitor();

    class SystemPackagingCopyAction extends CopyActionImpl {
        public SystemPackagingCopyAction(FileResolver resolver, SystemPackagingCopySpecVisitor visitor) {
            super(resolver, visitor);
        }
    }

}
