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

package com.netflix.gradle.plugins.rpm

import com.netflix.gradle.plugins.packaging.AbstractPackagingCopyAction
import com.netflix.gradle.plugins.packaging.Dependency
import com.netflix.gradle.plugins.packaging.Directory
import com.netflix.gradle.plugins.packaging.Link
import com.netflix.gradle.plugins.rpm.filevisitor.RpmFileVisitorStrategyFactory
import com.netflix.gradle.plugins.rpm.validation.RpmTaskPropertiesValidator
import org.apache.commons.lang3.StringUtils
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.redline_rpm.Builder
import org.redline_rpm.header.Architecture
import org.redline_rpm.header.Header.HeaderTag
import org.redline_rpm.payload.Directive
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.channels.FileChannel

class RpmCopyAction extends AbstractPackagingCopyAction {
    static final Logger logger = LoggerFactory.getLogger(RpmCopyAction.class)

    Rpm rpmTask
    Builder builder
    boolean includeStandardDefines = true // candidate for being pushed up to packaging level
    private final RpmTaskPropertiesValidator rpmTaskPropertiesValidator = new RpmTaskPropertiesValidator()
    private RpmFileVisitorStrategyFactory rpmFileVisitorStrategyFactory

    RpmCopyAction(Rpm rpmTask) {
        super(rpmTask)
        this.rpmTask = rpmTask
        rpmTaskPropertiesValidator.validate(rpmTask)
    }

    @Override
    void startVisit(CopyAction action) {
        super.startVisit(action)

        assert rpmTask.getVersion() != null, "RPM requires a version string"

        builder = new Builder()
        builder.setPackage rpmTask.packageName, rpmTask.version, rpmTask.release, rpmTask.epoch
        builder.setType rpmTask.type
        builder.setPlatform Architecture.valueOf(rpmTask.archStr.toUpperCase()), rpmTask.os
        builder.setGroup rpmTask.packageGroup
        builder.setBuildHost rpmTask.buildHost
        builder.setSummary rpmTask.summary
        builder.setDescription !StringUtils.isEmpty(rpmTask.packageDescription) ? rpmTask.packageDescription : ''
        builder.setLicense rpmTask.license
        builder.setPackager rpmTask.packager
        builder.setDistribution rpmTask.distribution
        builder.setVendor rpmTask.vendor
        builder.setUrl rpmTask.url
        builder.setProvides rpmTask.provides
        if (rpmTask.allPrefixes) {
            builder.setPrefixes(rpmTask.allPrefixes as String[])
        }

        String sourcePackage = rpmTask.sourcePackage
        if (!sourcePackage) {
            // need a source package because createrepo will assume your package is a source package without it
            sourcePackage = builder.defaultSourcePackage
        }
        builder.addHeaderEntry HeaderTag.SOURCERPM, sourcePackage

        builder.setPreInstallScript(scriptWithUtils(rpmTask.allCommonCommands, rpmTask.allPreInstallCommands))
        builder.setPostInstallScript(scriptWithUtils(rpmTask.allCommonCommands, rpmTask.allPostInstallCommands))
        builder.setPreUninstallScript(scriptWithUtils(rpmTask.allCommonCommands, rpmTask.allPreUninstallCommands))
        builder.setPostUninstallScript(scriptWithUtils(rpmTask.allCommonCommands, rpmTask.allPostUninstallCommands))

        rpmFileVisitorStrategyFactory = new RpmFileVisitorStrategyFactory(builder)
    }

    @Override
    void visitFile(FileCopyDetailsInternal fileDetails, def specToLookAt) {
        logger.debug "adding file {}", fileDetails.relativePath.pathString

        def inputFile = extractFile(fileDetails)

        Directive fileType = lookup(specToLookAt, 'fileType')
        String user = lookup(specToLookAt, 'user') ?: rpmTask.user
        String group = lookup(specToLookAt, 'permissionGroup') ?: rpmTask.permissionGroup

        Integer specFileMode = lookup(specToLookAt, 'fileMode') // Integer to allow for null
        int fileMode = (int) (specFileMode?:fileDetails.mode)

        def specAddParentsDir = lookup(specToLookAt, 'addParentDirs')
        boolean addParentsDir = specAddParentsDir!=null ? specAddParentsDir : rpmTask.addParentDirs

        rpmFileVisitorStrategyFactory.strategy.addFile(fileDetails, inputFile, fileMode, -1, fileType, user, group, addParentsDir)
    }

    @Override
    void visitDir(FileCopyDetailsInternal dirDetails, def specToLookAt) {
        if (specToLookAt == null) {
            logger.info("Got an empty spec from ${dirDetails.class.name} for ${dirDetails.path}/${dirDetails.name}")
            return
        }
        // Have to take booleans specially, since they would fail an elvis operator if set to false
        def specCreateDirectoryEntry = lookup(specToLookAt, 'createDirectoryEntry')
        boolean createDirectoryEntry = specCreateDirectoryEntry!=null ? specCreateDirectoryEntry : rpmTask.createDirectoryEntry
        def specAddParentsDir = lookup(specToLookAt, 'addParentDirs')
        boolean addParentsDir = specAddParentsDir!=null ? specAddParentsDir : rpmTask.addParentDirs

        if (createDirectoryEntry) {
            logger.debug "adding directory {}", dirDetails.relativePath.pathString
            Integer specFileMode = lookup(specToLookAt, 'fileMode') // Integer to allow for null
            int dirMode = (int) (specFileMode?:dirDetails.mode)
            Directive directive = (Directive) lookup(specToLookAt, 'fileType') ?: rpmTask.fileType
            String user = (String) lookup(specToLookAt, 'user') ?: rpmTask.user
            String group = (String) lookup(specToLookAt, 'permissionGroup') ?: rpmTask.permissionGroup

            rpmFileVisitorStrategyFactory.strategy.addDirectory(dirDetails, dirMode, directive, user, group, addParentsDir)
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
    protected void addConflict(Dependency dep) {
        builder.addConflicts dep.packageName, dep.version, dep.flag
    }

    @Override
    protected void addObsolete(Dependency dep) {
        builder.addObsoletes dep.packageName, dep.version, dep.flag
    }

    @Override
    protected void addDirectory(Directory directory) {
        builder.addDirectory(directory.path, directory.permissions, null, rpmTask.user, rpmTask.permissionGroup, false)
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

    Object scriptWithUtils(List utils, List scripts) {
        def l = []
        def stdDefines = standardScriptDefines()
        if(stdDefines) l.add(stdDefines)
        l.addAll(utils)
        l.addAll(scripts)

        concat(l)
    }
}
