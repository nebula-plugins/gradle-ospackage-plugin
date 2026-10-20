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
        exten.options.uid.convention(0)
        exten.options.gid.convention(0)
        exten.options.packageGroup.convention('java')
        exten.options.archStr.convention('all')
        exten.options.maintainer.convention(System.getProperty('user.name', ''))
        exten.options.uploaders.convention('')
        exten.options.priority.convention('optional')

        // Then apply conventions from parentExten (higher priority - override defaults)
        // Only override if parentExten has a value
        if (parentExten) {
            if (parentExten.options.fileType.isPresent()) {
                exten.options.fileType.convention(parentExten.options.fileType)
            }
            if (parentExten.options.uid.isPresent()) {
                exten.options.uid.convention(parentExten.options.uid)
            }
            if (parentExten.options.gid.isPresent()) {
                exten.options.gid.convention(parentExten.options.gid)
            }
            if (parentExten.options.packageGroup.isPresent()) {
                exten.options.packageGroup.convention(parentExten.options.packageGroup)
            }
            if (parentExten.options.multiArch.isPresent()) {
                exten.options.multiArch.convention(parentExten.options.multiArch)
            }
            if (parentExten.options.archStr.isPresent()) {
                exten.options.archStr.convention(parentExten.options.archStr)
            }
            if (parentExten.options.maintainer.isPresent()) {
                exten.options.maintainer.convention(parentExten.options.maintainer)
            }
            if (parentExten.options.uploaders.isPresent()) {
                exten.options.uploaders.convention(parentExten.options.uploaders)
            }
            if (parentExten.options.priority.isPresent()) {
                exten.options.priority.convention(parentExten.options.priority)
            }
        }
    }

    @Input @Optional
    List<Dependency> getAllRecommends() {
        return getRecommends() + (parentExten?.options?.getRecommends()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllSuggests() {
        return getSuggests() + (parentExten?.options?.getSuggests()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllEnhances() {
        return getEnhances() + (parentExten?.options?.getEnhances()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllPreDepends() {
        return getPreDepends() + (parentExten?.options?.getPreDepends()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllBreaks() {
        return getBreaks() + (parentExten?.options?.getBreaks()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllReplaces() {
        return getReplaces() + (parentExten?.options?.getReplaces()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    Map<String, String> getAllCustomFields() {
        return getCustomFields() + (parentExten?.options?.getCustomFields()?.getOrElse([:]) ?: [:])
    }

    @OutputFile
    File getChangesFile() {
        return new File(getArchiveFile().get().asFile.path.replaceFirst('deb$', 'changes'))
    }
}
