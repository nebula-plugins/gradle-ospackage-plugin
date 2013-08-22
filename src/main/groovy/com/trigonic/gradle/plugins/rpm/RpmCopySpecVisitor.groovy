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

package com.trigonic.gradle.plugins.rpm

import org.freecompany.redline.Builder
import org.freecompany.redline.header.Header.HeaderTag
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.EmptyCopySpecVisitor
import org.gradle.api.internal.file.copy.ReadableCopySpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RpmCopySpecVisitor extends EmptyCopySpecVisitor {
    static final Logger logger = LoggerFactory.getLogger(RpmCopySpecVisitor.class)

    Rpm task
    Builder builder
    File destinationDir
    ReadableCopySpec spec
    boolean didWork
    boolean includeStandardDefines = true

    @Override
    void startVisit(CopyAction action) {
        task = action.task

        destinationDir = task.destinationDir
        didWork = false

        builder = new Builder()
        builder.setPackage task.packageName, task.version, task.release
        builder.setType task.type
        builder.setPlatform task.arch, task.os
        builder.setGroup task.packageGroup
        builder.setBuildHost task.buildHost
        builder.setSummary task.summary
        builder.setDescription task.description
        builder.setLicense task.license
        builder.setPackager task.packager
        builder.setDistribution task.distribution
        builder.setVendor task.vendor
        builder.setUrl task.url
        builder.setProvides task.provides ?: task.packageName

        String sourcePackage = task.sourcePackage
        if (!sourcePackage) {
            // need a source package because createrepo will assume your package is a source package without it
            sourcePackage = builder.defaultSourcePackage
        }
        builder.addHeaderEntry HeaderTag.SOURCERPM, sourcePackage
        
        builder.setPreInstallScript(scriptWithUtils(task.installUtils, task.preInstall))
        builder.setPostInstallScript(scriptWithUtils(task.installUtils, task.postInstall))
        builder.setPreUninstallScript(scriptWithUtils(task.installUtils, task.preUninstall))
        builder.setPostUninstallScript(scriptWithUtils(task.installUtils, task.postUninstall))
    }

    @Override
    void visitSpec(ReadableCopySpec spec) {
        this.spec = spec
    }

    @Override
    void visitFile(FileCopyDetails fileDetails) {
        logger.debug "adding file {}", fileDetails.relativePath.pathString
        builder.addFile "/" + fileDetails.relativePath.pathString, fileDetails.file,
            spec.fileMode == null ? -1 : spec.fileMode, -1, spec.fileType, spec.user ?: task.user, spec.group ?: task.group,
                spec.addParentDirs
    }

    @Override
    void visitDir(FileCopyDetails dirDetails) {
        if (spec.createDirectoryEntry) {
            logger.debug "adding directory {}", dirDetails.relativePath.pathString
            builder.addDirectory "/" + dirDetails.relativePath.pathString, spec.dirMode == null ? -1 : spec.dirMode,
                spec.fileType, spec.user ?: task.user, spec.group ?: task.group, spec.addParentDirs
        }
    }

    @Override
    void endVisit() {
        for (Link link : task.links) {
            logger.debug "adding link {} -> {}", link.path, link.target
            builder.addLink link.path, link.target, link.permissions
        }
        
        for (Dependency dep : task.dependencies) {
            logger.debug "adding dependency on {} {}", dep.packageName, dep.version
            builder.addDependency dep.packageName, dep.version, dep.flag
        }
        
        String rpmFile = builder.build(destinationDir)
        didWork = true
        logger.info 'Created rpm {}', rpmFile
    }

    @Override
    boolean getDidWork() {
        didWork
    }
    
    String standardScriptDefines() {
        includeStandardDefines ? 
            String.format(" RPM_ARCH=%s \n RPM_OS=%s \n RPM_PACKAGE_NAME=%s \n RPM_PACKAGE_VERSION=%s \n RPM_PACKAGE_RELEASE=%s \n\n",
                task?.arch?.toString().toLowerCase(), task?.os?.toString()?.toLowerCase(), task?.packageName, task?.version, task?.release) : null 
    }
    
    Object scriptWithUtils(File utils, File script) {
        concat(standardScriptDefines(), utils, script)
    }
    
    String concat(Object... scripts) {
        String shebang
        StringBuilder result = new StringBuilder();        
        scripts.each { script ->
            script?.eachLine { line ->
                if (line.matches('^#!.*$')) {
                    if (!shebang) {
                        shebang = line
                    } else if (line != shebang) {
                        throw new IllegalArgumentException("mismatching #! script lines")
                    }
                } else {
                    result.append line
                    result.append "\n"
                }
            }
        }
        if (shebang) {
            result.insert(0, shebang + "\n")
        }
        result.toString()
    }
}
