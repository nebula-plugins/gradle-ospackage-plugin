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

import com.trigonic.gradle.plugins.packaging.Dependency
import com.trigonic.gradle.plugins.packaging.Link
import com.trigonic.gradle.plugins.packaging.SystemPackagingCopySpecVisitor
import org.freecompany.redline.Builder
import org.freecompany.redline.header.Header.HeaderTag
import org.freecompany.redline.payload.Directive
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopySpecImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.channels.FileChannel

class RpmCopySpecVisitor extends SystemPackagingCopySpecVisitor {
    static final Logger logger = LoggerFactory.getLogger(RpmCopySpecVisitor.class)

    Rpm rpmTask
    Builder builder
    boolean includeStandardDefines = true

    RpmCopySpecVisitor(Rpm rpmTask) {
        super(rpmTask)
        this.rpmTask = rpmTask
    }

    @Override
    void startVisit(CopyAction action) {
        super.startVisit(action)

        builder = new Builder()
        builder.setPackage rpmTask.packageName, rpmTask.version, rpmTask.release
        builder.setType rpmTask.type
        builder.setPlatform rpmTask.arch, rpmTask.os
        builder.setGroup rpmTask.packageGroup
        builder.setBuildHost rpmTask.buildHost
        builder.setSummary rpmTask.summary
        builder.setDescription rpmTask.packageDescription
        builder.setLicense rpmTask.license
        builder.setPackager rpmTask.packager
        builder.setDistribution rpmTask.distribution
        builder.setVendor rpmTask.vendor
        builder.setUrl rpmTask.url
        builder.setProvides rpmTask.provides ?: rpmTask.packageName

        String sourcePackage = rpmTask.sourcePackage
        if (!sourcePackage) {
            // need a source package because createrepo will assume your package is a source package without it
            sourcePackage = builder.defaultSourcePackage
        }
        builder.addHeaderEntry HeaderTag.SOURCERPM, sourcePackage

        builder.setPreInstallScript(scriptWithUtils(rpmTask.installUtils, rpmTask.preInstall))
        builder.setPostInstallScript(scriptWithUtils(rpmTask.installUtils, rpmTask.postInstall))
        builder.setPreUninstallScript(scriptWithUtils(rpmTask.installUtils, rpmTask.preUninstall))
        builder.setPostUninstallScript(scriptWithUtils(rpmTask.installUtils, rpmTask.postUninstall))
    }

    @Override
    void visitFile(FileVisitDetails fileDetails) {
        logger.debug "adding file {}", fileDetails.relativePath.pathString
        builder.addFile(
                "/" + fileDetails.relativePath.pathString,
                fileDetails.file,
                (int) (spec.fileMode == null ? -1 : spec.fileMode),
                -1,
                (Directive) (spec.fileType),
                (String) spec.user ?: rpmTask.user,
                (String) (spec.group ?: rpmTask.group),
                (boolean) (spec.addParentDirs)
        )
    }

    @Override
    void visitDir(FileVisitDetails dirDetails) {
        def specToLookAt = (spec instanceof CopySpecImpl)?:spec.spec // WrapperCopySpec has a nested spec
        boolean createDirectoryEntry = specToLookAt.hasProperty('createDirectoryEntry') && specToLookAt.createDirectoryEntry
        if (createDirectoryEntry) {
            logger.debug "adding directory {}", dirDetails.relativePath.pathString
            builder.addDirectory(
                    "/" + dirDetails.relativePath.pathString,
                    (int) (spec.dirMode == null ? -1 : spec.dirMode),
                    (Directive) (spec.fileType),
                    (String) (spec.user ?: rpmTask.user),
                    (String) (spec.group ?: rpmTask.group),
                    (boolean) (spec.addParentDirs)
            )
        }
    }

    @Override
    protected void addLink(Link link) {
        builder.addLink link.path, link.target, link.permissions
    }

    @Override
    protected void addDependency(Dependency dep) {
        builder.addDependency dep.packageName, dep.version, dep.flag
    }

    @Override
    protected void end() {
        File rpmFile = rpmTask.getArchivePath()
        FileChannel fc = new RandomAccessFile( rpmFile, "rw").getChannel()
        builder.build(fc)
        logger.info 'Created rpm {}', rpmFile
    }

    String standardScriptDefines() {
        includeStandardDefines ?
            String.format(" RPM_ARCH=%s \n RPM_OS=%s \n RPM_PACKAGE_NAME=%s \n RPM_PACKAGE_VERSION=%s \n RPM_PACKAGE_RELEASE=%s \n\n",
                rpmTask.getArchString(),
                rpmTask.os?.toString().toLowerCase(),
                rpmTask.getPackageName(),
                rpmTask.getVersion(),
                rpmTask.getRelease()) : null
    }

    Object scriptWithUtils(File utils, File script) {
        concat(standardScriptDefines(), utils, script)
    }
}
