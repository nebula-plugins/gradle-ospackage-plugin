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
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.EmptyCopySpecVisitor
import org.gradle.api.internal.file.copy.ReadableCopySpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RpmCopySpecVisitor extends EmptyCopySpecVisitor {
    static final Logger logger = LoggerFactory.getLogger(RpmCopySpecVisitor.class)

    Builder builder
    File destinationDir
    ReadableCopySpec spec
    boolean didWork

    RpmCopySpecVisitor() {
    }

    @Override
    void startVisit(CopyAction action) {
        Rpm task = action.task

        destinationDir = task.destinationDir
        didWork = false

        builder = new Builder()
        builder.setPackage task.packageName, task.version, task.release
        builder.setType task.type
        builder.setPlatform task.arch, task.os
        builder.setGroup task.group
        builder.setBuildHost task.buildHost
        builder.setSummary task.summary
        builder.setDescription task.description
        builder.setLicense task.license
        builder.setPackager task.packager
        builder.setDistribution task.distribution
        builder.setVendor task.vendor
        builder.setUrl task.url
        builder.setProvides task.provides ?: task.packageName

        if (task.sourcePackage) {
            builder.addHeaderEntry HeaderTag.SOURCERPM, task.sourcePackage
        }

        builder.setPreInstallScript task.preInstall
        builder.setPostInstallScript task.postInstall
        builder.setPreUninstallScript task.preUninstall
        builder.setPostUninstallScript task.postUninstall
    }

    @Override
    void visitSpec(ReadableCopySpec spec) {
        this.spec = spec
    }

    @Override
    void visitFile(FileVisitDetails fileDetails) {
        builder.addFile "/" + fileDetails.relativePath.pathString, fileDetails.file, spec.fileMode, spec.directive, spec.user, spec.group
    }

    @Override
    void visitDir(FileVisitDetails dirDetails) {
        builder.addDirectory "/" + dirDetails.relativePath.pathString, spec.dirMode, spec.directive, spec.user, spec.group
    }

    @Override
    void endVisit() {
        String rpmFile = builder.build(destinationDir)
        didWork = true
        logger.info 'Created rpm {}', rpmFile
    }

    @Override
    boolean getDidWork() {
        didWork
    }
}
