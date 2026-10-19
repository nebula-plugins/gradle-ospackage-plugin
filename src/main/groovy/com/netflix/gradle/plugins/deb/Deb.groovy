/*
 * Copyright 2011-2019 the original author or authors.
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

package com.netflix.gradle.plugins.deb

import com.netflix.gradle.plugins.packaging.AbstractPackagingCopyAction
import com.netflix.gradle.plugins.packaging.Dependency
import com.netflix.gradle.plugins.packaging.SystemPackagingTask
import com.netflix.gradle.plugins.utils.DeprecationLoggerUtils
import groovy.transform.CompileDynamic
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

@DisableCachingByDefault
@CompileDynamic
abstract class Deb extends SystemPackagingTask {

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    List<File> getSupplementaryControlInputFiles() {
        getAllSupplementaryControlFiles().collect { String path -> projectLayout.projectDirectory.file(path).asFile }
    }

    @Inject
    Deb(ProjectLayout projectLayout) {
        super(projectLayout)
        archiveExtension.set 'deb'
    }

    @Override
    String assembleArchiveName() {
        String name = getPackageName();
        DeprecationLoggerUtils.whileDisabled {
            name += getVersion() ? "_${getVersion()}" : ''
            name += getRelease() ? "-${getRelease()}" : ''
            name += getArchString() ? "_${getArchString()}" : ''
            def ext = getArchiveExtension().getOrNull()
            name += ext ? ".${ext}" : ''
        }

        return name;
    }

    @Override
    AbstractPackagingCopyAction createCopyAction() {
        return new DebCopyAction(this, new File(projectLayout.buildDirectory.getAsFile().get(), "debian"))
    }

    @Override
    protected void applyConventions(Project project) {
        applyBaseConventions(project)

        // Apply default conventions FIRST (lowest priority)
        exten.uidProperty.convention(0)
        exten.gidProperty.convention(0)
        exten.packageGroupProperty.convention('java')
        exten.archStrProperty.convention('all')
        exten.maintainerProperty.convention(System.getProperty('user.name', ''))
        exten.uploadersProperty.convention('')
        exten.priorityProperty.convention('optional')

        // Then apply conventions from parentExten (higher priority - override defaults)
        // Only override if parentExten has a value
        if (parentExten) {
            if (parentExten.fileTypeProperty.isPresent()) {
                exten.fileTypeProperty.convention(parentExten.fileTypeProperty)
            }
            if (parentExten.uidProperty.isPresent()) {
                exten.uidProperty.convention(parentExten.uidProperty)
            }
            if (parentExten.gidProperty.isPresent()) {
                exten.gidProperty.convention(parentExten.gidProperty)
            }
            if (parentExten.packageGroupProperty.isPresent()) {
                exten.packageGroupProperty.convention(parentExten.packageGroupProperty)
            }
            if (parentExten.multiArchProperty.isPresent()) {
                exten.multiArchProperty.convention(parentExten.multiArchProperty)
            }
            if (parentExten.archStrProperty.isPresent()) {
                exten.archStrProperty.convention(parentExten.archStrProperty)
            }
            if (parentExten.maintainerProperty.isPresent()) {
                exten.maintainerProperty.convention(parentExten.maintainerProperty)
            }
            if (parentExten.uploadersProperty.isPresent()) {
                exten.uploadersProperty.convention(parentExten.uploadersProperty)
            }
            if (parentExten.priorityProperty.isPresent()) {
                exten.priorityProperty.convention(parentExten.priorityProperty)
            }
        }
    }

    @Input @Optional
    List<Dependency> getAllRecommends() {
        return getRecommends() + (parentExten?.getRecommendsProperty()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllSuggests() {
        return getSuggests() + (parentExten?.getSuggestsProperty()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllEnhances() {
        return getEnhances() + (parentExten?.getEnhancesProperty()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllPreDepends() {
        return getPreDepends() + (parentExten?.getPreDependsProperty()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllBreaks() {
        return getBreaks() + (parentExten?.getBreaksProperty()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllReplaces() {
        return getReplaces() + (parentExten?.getReplacesProperty()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    Map<String, String> getAllCustomFields() {
        return getCustomFields() + (parentExten?.getCustomFieldsProperty()?.getOrElse([:]) ?: [:])
    }

    @OutputFile
    File getChangesFile() {
        return new File(getArchiveFile().get().asFile.path.replaceFirst('deb$', 'changes'))
    }
}
