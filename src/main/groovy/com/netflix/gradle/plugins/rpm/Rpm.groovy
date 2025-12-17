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

package com.netflix.gradle.plugins.rpm

import com.netflix.gradle.plugins.packaging.AbstractPackagingCopyAction
import com.netflix.gradle.plugins.packaging.SystemPackagingTask
import com.netflix.gradle.plugins.utils.DeprecationLoggerUtils
import groovy.transform.CompileDynamic
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import org.redline_rpm.header.Architecture
import org.redline_rpm.header.Os
import org.redline_rpm.header.RpmType

import javax.inject.Inject

@DisableCachingByDefault
@CompileDynamic
abstract class Rpm extends SystemPackagingTask {
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    File changeLogFile

    @Inject
    Rpm(ProjectLayout projectLayout) {
        super(projectLayout)
        archiveExtension.set 'rpm'
    }

    @Override
    String assembleArchiveName() {
        String name = getPackageName();
        DeprecationLoggerUtils.whileDisabled {
            name += getVersion() ? "-${getVersion()}" : ''
            name += getRelease() ? "-${getRelease()}" : ''
            name += getArchString() ? ".${getArchString()}" : ''
            def ext = getArchiveExtension().getOrNull()
            name += ext ? ".${ext}" : ''
        }

        return name;
    }

    @Override
    AbstractPackagingCopyAction createCopyAction() {
        return new RpmCopyAction(this)
    }

    @Override
    protected void applyConventions() {
        super.applyConventions()

        // Apply default conventions FIRST (lowest priority)
        exten.addParentDirs.convention(true)
        exten.archStr.convention(Architecture.NOARCH.name())
        exten.os.convention(Os.UNKNOWN)
        exten.type.convention(RpmType.BINARY)
        exten.prefixes.convention([])

        // Then apply conventions from parentExten (higher priority - override defaults)
        // Only override if parentExten has a value
        if (parentExten) {
            if (parentExten.fileType.isPresent()) {
                exten.fileType.convention(parentExten.fileType)
            }
            if (parentExten.addParentDirs.isPresent()) {
                exten.addParentDirs.convention(parentExten.addParentDirs)
            }
            if (parentExten.archStr.isPresent()) {
                exten.archStr.convention(parentExten.archStr)
            }
            if (parentExten.os.isPresent()) {
                exten.os.convention(parentExten.os)
            }
            if (parentExten.type.isPresent()) {
                exten.type.convention(parentExten.type)
            }
            if (parentExten.prefixes.isPresent()) {
                exten.prefixes.convention(parentExten.prefixes)
            }
        }
    }

    void prefixes(String... addPrefixes) {
        exten.prefixes.addAll(addPrefixes)
    }

    List<String> getPrefixes() {
        exten.prefixes.getOrElse([])
    }

    public File getChangeLogFile() {
        return changeLogFile;
    }

    public void setChangeLogFile(File changeLogFile) {
        this.changeLogFile = changeLogFile;
    }
}
