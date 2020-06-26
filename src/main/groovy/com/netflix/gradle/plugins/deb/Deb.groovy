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
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile


class Deb extends SystemPackagingTask {
    Deb() {
        super()
        archiveExtension.set 'deb'
    }

    @Override
    String assembleArchiveName() {
        String name = getPackageName();
        DeprecationLoggerUtils.whileDisabled {
            name += getVersion() ? "_${getVersion()}" : ''
            name += getRelease() ? "-${getRelease()}" : ''
            name += getArchString() ? "_${getArchString()}" : ''
            name += getArchiveExtension().getOrNull() ? ".${getArchiveExtension().getOrNull()}" : ''
        }

        return name;
    }

    @Override
    AbstractPackagingCopyAction createCopyAction() {
        return new DebCopyAction(this)
    }

    @Override
    protected void applyConventions() {
        super.applyConventions()

        // For all mappings, we're only being called if it wasn't explicitly set on the task. In which case, we'll want
        // to pull from the parentExten. And only then would we fallback on some other value.
        ConventionMapping mapping = ((IConventionAware) this).getConventionMapping()

        // Could come from extension
        mapping.map('fileType', { parentExten?.getFileType() })
        mapping.map('uid', { parentExten?.getUid()?:0 })
        mapping.map('gid', { (parentExten?.getGid())?:0 })
        mapping.map('packageGroup', { parentExten?.getPackageGroup() ?: 'java' })
        mapping.map('multiArch', { parentExten?.getMultiArch() })
        mapping.map('archStr', { parentExten?.getArchStr()?:'all'})
        mapping.map('maintainer', { parentExten?.getMaintainer() ?: System.getProperty('user.name', '') })
        mapping.map('uploaders', { parentExten?.getUploaders() ?: '' })
        mapping.map('priority', { parentExten?.getPriority() ?: 'optional' })
    }

    @Input @Optional
    List<Dependency> getAllRecommends() {
        return getRecommends() + (parentExten?.getRecommends() ?: [])
    }

    @Input @Optional
    List<Dependency> getAllSuggests() {
        return getSuggests() + (parentExten?.getSuggests() ?: [])
    }

    @Input @Optional
    List<Dependency> getAllEnhances() {
        return getEnhances() + (parentExten?.getEnhances() ?: [])
    }

    @Input @Optional
    List<Dependency> getAllPreDepends() {
        return getPreDepends() + (parentExten?.getPreDepends() ?: [])
    }

    @Input @Optional
    List<Dependency> getAllBreaks() {
        return getBreaks() + (parentExten?.getBreaks() ?: [])
    }

    @Input @Optional
    List<Dependency> getAllReplaces() {
        return getReplaces() + (parentExten?.getReplaces() ?: [])
    }

    @Input @Optional
    Map<String, String> getAllCustomFields() {
        return getCustomFields() + (parentExten?.getCustomFields() ?: [:])
    }

    @OutputFile
    File getChangesFile() {
        return new File(getArchiveFile().get().asFile.path.replaceFirst('deb$', 'changes'))
    }
}
