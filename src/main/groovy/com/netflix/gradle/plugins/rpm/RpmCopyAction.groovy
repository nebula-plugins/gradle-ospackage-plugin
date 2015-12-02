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
import com.netflix.gradle.plugins.rpm.validation.RpmTaskPropertiesValidator
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.redline_rpm.Builder
import org.redline_rpm.header.Architecture
import org.redline_rpm.header.Header.HeaderTag
import org.redline_rpm.payload.Directive
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.channels.FileChannel

import static com.netflix.gradle.plugins.utils.GradleUtils.lookup

class RpmCopyAction extends AbstractPackagingCopyAction<Rpm> {
    static final Logger logger = LoggerFactory.getLogger(RpmCopyAction.class)

    Builder builder
    boolean includeStandardDefines = true // candidate for being pushed up to packaging level
    private final RpmTaskPropertiesValidator rpmTaskPropertiesValidator = new RpmTaskPropertiesValidator()
    private RpmFileVisitorStrategy rpmFileVisitorStrategy

    RpmCopyAction(Rpm rpmTask) {
        super(rpmTask)
        rpmTaskPropertiesValidator.validate(rpmTask)
    }

    @Override
    void startVisit(CopyAction action) {
        super.startVisit(action)

        assert task.getVersion() != null, 'RPM requires a version string'

        builder = new Builder()
        builder.setPackage task.packageName, task.version, task.release, task.epoch
        builder.setType task.type
        builder.setPlatform Architecture.valueOf(task.archStr.toUpperCase()), task.os
        builder.setGroup task.packageGroup
        builder.setBuildHost task.buildHost
        builder.setSummary task.summary
        builder.setDescription task.packageDescription ?: ''
        builder.setLicense task.license
        builder.setPackager task.packager
        builder.setDistribution task.distribution
        builder.setVendor task.vendor
        builder.setUrl task.url
        builder.setProvides task.provides
        if (task.allPrefixes) {
            builder.setPrefixes(task.allPrefixes as String[])
        }

        String sourcePackage = task.sourcePackage
        if (!sourcePackage) {
            // need a source package because createrepo will assume your package is a source package without it
            sourcePackage = builder.defaultSourcePackage
        }
        builder.addHeaderEntry HeaderTag.SOURCERPM, sourcePackage

        builder.setPreInstallScript(scriptWithUtils(task.allCommonCommands, task.allPreInstallCommands))
        builder.setPostInstallScript(scriptWithUtils(task.allCommonCommands, task.allPostInstallCommands))
        builder.setPreUninstallScript(scriptWithUtils(task.allCommonCommands, task.allPreUninstallCommands))
        builder.setPostUninstallScript(scriptWithUtils(task.allCommonCommands, task.allPostUninstallCommands))

        rpmFileVisitorStrategy = new RpmFileVisitorStrategy(builder)
    }

    @Override
    void visitFile(FileCopyDetailsInternal fileDetails, def specToLookAt) {
        logger.debug "adding file {}", fileDetails.relativePath.pathString

        def inputFile = extractFile(fileDetails)

        Directive fileType = lookup(specToLookAt, 'fileType')
        String user = lookup(specToLookAt, 'user') ?: task.user
        String group = lookup(specToLookAt, 'permissionGroup') ?: task.permissionGroup

        int fileMode = lookup(specToLookAt, 'fileMode') ?: fileDetails.mode
        def specAddParentsDir = lookup(specToLookAt, 'addParentDirs')
        boolean addParentsDir = specAddParentsDir ?: task.addParentDirs

        rpmFileVisitorStrategy.addFile(fileDetails, inputFile, fileMode, -1, fileType, user, group, addParentsDir)
    }

    @Override
    void visitDir(FileCopyDetailsInternal dirDetails, def specToLookAt) {
        if (specToLookAt == null) {
            logger.info("Got an empty spec from ${dirDetails.class.name} for ${dirDetails.path}/${dirDetails.name}")
            return
        }
        // Have to take booleans specially, since they would fail an elvis operator if set to false
        def specCreateDirectoryEntry = lookup(specToLookAt, 'createDirectoryEntry')
        boolean createDirectoryEntry = specCreateDirectoryEntry ?: task.createDirectoryEntry
        def specAddParentsDir = lookup(specToLookAt, 'addParentDirs')
        boolean addParentsDir = specAddParentsDir ?: task.addParentDirs

        if (createDirectoryEntry) {
            logger.debug 'adding directory {}', dirDetails.relativePath.pathString
            int dirMode = lookup(specToLookAt, 'fileMode') ?: dirDetails.mode
            Directive directive = (Directive) lookup(specToLookAt, 'fileType') ?: task.fileType
            String user = lookup(specToLookAt, 'user') ?: task.user
            String group = lookup(specToLookAt, 'permissionGroup') ?: task.permissionGroup

            rpmFileVisitorStrategy.addDirectory(dirDetails, dirMode, directive, user, group, addParentsDir)
        }
    }

    @Override
    protected void addLink(Link link) {
        builder.addLink link.path, link.target, link.permissions
    }

    @Override
    protected void addDependency(Dependency dep) {
        builder.addDependency(dep.packageName, dep.flag, dep.version)
    }

    @Override 
    protected void addConflict(Dependency dep) {
        builder.addConflicts(dep.packageName, dep.flag, dep.version)
    }

    @Override
    protected void addObsolete(Dependency dep) {
        builder.addObsoletes(dep.packageName, dep.flag, dep.version)
    }

    @Override
    protected void addDirectory(Directory directory) {
        builder.addDirectory(directory.path, directory.permissions, null, task.user, task.permissionGroup, false)
    }

    @Override
    protected void end() {
        File rpmFile = task.getArchivePath()
        FileChannel fc = new RandomAccessFile( rpmFile, "rw").getChannel()
        builder.build(fc)
        logger.info 'Created rpm {}', rpmFile
    }

    String standardScriptDefines() {
        includeStandardDefines ?
            String.format(" RPM_ARCH=%s \n RPM_OS=%s \n RPM_PACKAGE_NAME=%s \n RPM_PACKAGE_VERSION=%s \n RPM_PACKAGE_RELEASE=%s \n\n",
                task.getArchString(),
                task.os?.toString()?.toLowerCase() ?: '',
                task.getPackageName(),
                task.getVersion(),
                task.getRelease()) : null
    }

    String scriptWithUtils(List<Object> utils, List<Object> scripts) {
        def l = []
        def stdDefines = standardScriptDefines()
        if(stdDefines) l.add(stdDefines)
        l.addAll(utils)
        l.addAll(scripts)

        concat(l)
    }
}
