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
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

@DisableCachingByDefault
@CompileDynamic
abstract class Deb extends SystemPackagingTask {

    @Inject
    Deb(ProjectLayout projectLayout) {
        super(projectLayout)
        archiveExtension.set 'deb'
        // notCompatibleWithConfigurationCache("nebula.ospackage does not support configuration cache")
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
    protected void applyConventions() {
        super.applyConventions()

        // Apply default conventions FIRST (lowest priority)
        exten.uid.convention(0)
        exten.gid.convention(0)
        exten.packageGroup.convention('java')
        exten.archStr.convention('all')
        exten.maintainer.convention(System.getProperty('user.name', ''))
        exten.uploaders.convention('')
        exten.priority.convention('optional')

        // Then apply conventions from parentExten (higher priority - override defaults)
        // Only override if parentExten has a value
        if (parentExten) {
            if (parentExten.fileType.isPresent()) {
                exten.fileType.convention(parentExten.fileType)
            }
            if (parentExten.uid.isPresent()) {
                exten.uid.convention(parentExten.uid)
            }
            if (parentExten.gid.isPresent()) {
                exten.gid.convention(parentExten.gid)
            }
            if (parentExten.packageGroup.isPresent()) {
                exten.packageGroup.convention(parentExten.packageGroup)
            }
            if (parentExten.multiArch.isPresent()) {
                exten.multiArch.convention(parentExten.multiArch)
            }
            if (parentExten.archStr.isPresent()) {
                exten.archStr.convention(parentExten.archStr)
            }
            if (parentExten.maintainer.isPresent()) {
                exten.maintainer.convention(parentExten.maintainer)
            }
            if (parentExten.uploaders.isPresent()) {
                exten.uploaders.convention(parentExten.uploaders)
            }
            if (parentExten.priority.isPresent()) {
                exten.priority.convention(parentExten.priority)
            }
        }
    }

    @Input @Optional
    List<Dependency> getAllRecommends() {
        return getRecommends() + (parentExten?.getRecommends()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllSuggests() {
        return getSuggests() + (parentExten?.getSuggests()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllEnhances() {
        return getEnhances() + (parentExten?.getEnhances()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllPreDepends() {
        return getPreDepends() + (parentExten?.getPreDepends()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllBreaks() {
        return getBreaks() + (parentExten?.getBreaks()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    List<Dependency> getAllReplaces() {
        return getReplaces() + (parentExten?.getReplaces()?.getOrElse([]) ?: [])
    }

    @Input @Optional
    Map<String, String> getAllCustomFields() {
        return getCustomFields() + (parentExten?.getCustomFields()?.getOrElse([:]) ?: [:])
    }

    @OutputFile
    File getChangesFile() {
        return new File(getArchiveFile().get().asFile.path.replaceFirst('deb$', 'changes'))
    }
}
