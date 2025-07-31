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
import com.netflix.gradle.plugins.packaging.Dependency
import com.netflix.gradle.plugins.packaging.Directory
import com.netflix.gradle.plugins.packaging.Link
import com.netflix.gradle.plugins.rpm.validation.RpmTaskPropertiesValidator
import com.netflix.gradle.plugins.utils.DeprecationLoggerUtils
import com.netflix.gradle.plugins.utils.FilePermissionUtil
import groovy.transform.CompileDynamic
import org.apache.commons.lang3.StringUtils
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.redline_rpm.Builder
import org.redline_rpm.IntString
import org.redline_rpm.header.Architecture
import org.redline_rpm.header.Flags
import org.redline_rpm.header.Header.HeaderTag
import org.redline_rpm.payload.Directive
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.netflix.gradle.plugins.utils.GradleUtils.lookup

@CompileDynamic
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
        DeprecationLoggerUtils.whileDisabled {

            assert task.getVersion() != null, 'RPM requires a version string'
            if ([task.preInstallFile, task.postInstallFile, task.preUninstallFile, task.postUninstallFile].any()) {
                logger.warn('At least one of (preInstallFile|postInstallFile|preUninstallFile|postUninstallFile) is defined ' +
                        'and will be ignored for RPM builds')
            }

            builder = createBuilder()
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
            if (task.allPrefixes) {
                builder.setPrefixes(task.allPrefixes as String[])
            }
            if (StringUtils.isNotBlank(task.getSigningKeyId())
                    && StringUtils.isNotBlank(task.getSigningKeyPassphrase())
                    && task.getSigningKeyRingFile().exists()) {
                builder.setPrivateKeyId task.getSigningKeyId()
                builder.setPrivateKeyPassphrase task.getSigningKeyPassphrase()
                builder.setPrivateKeyRingFile task.getSigningKeyRingFile()
            }

            String sourcePackage = task.sourcePackage
            if (!sourcePackage) {
                // need a source package because createrepo will assume your package is a source package without it
                sourcePackage = builder.defaultSourcePackage
            }
            builder.addHeaderEntry HeaderTag.SOURCERPM, sourcePackage

            if (!task.allPreInstallCommands?.empty) {
                builder.setPreInstallScript(scriptWithUtils(task.allCommonCommands, task.allPreInstallCommands))
            }
            if (!task.allPostInstallCommands?.empty) {
                builder.setPostInstallScript(scriptWithUtils(task.allCommonCommands, task.allPostInstallCommands))
            }
            if (!task.allPreUninstallCommands?.empty) {
                builder.setPreUninstallScript(scriptWithUtils(task.allCommonCommands, task.allPreUninstallCommands))
            }
            if (!task.allPostUninstallCommands?.empty) {
                builder.setPostUninstallScript(scriptWithUtils(task.allCommonCommands, task.allPostUninstallCommands))
            }
            if (!task.allTriggerIn?.empty) {
                task.allTriggerIn.each { trigger ->
                    def dependencyMap = [:]
                    dependencyMap.putAt(trigger.dependency.packageName,
                            new IntString(trigger.dependency.flag, trigger.dependency.version))
                    builder.addTrigger(trigger.command, null, dependencyMap, Flags.SCRIPT_TRIGGERIN)
                }
            }
            if (!task.allTriggerUn?.empty) {
                task.allTriggerUn.each { trigger ->
                    def dependencyMap = [:]
                    dependencyMap.putAt(trigger.dependency.packageName,
                            new IntString(trigger.dependency.flag, trigger.dependency.version))
                    builder.addTrigger(trigger.command, null, dependencyMap, Flags.SCRIPT_TRIGGERUN)
                }
            }
            if (!task.allTriggerPostUn?.empty) {
                task.allTriggerPostUn.each { trigger ->
                    def dependencyMap = [:]
                    dependencyMap.putAt(trigger.dependency.packageName,
                            new IntString(trigger.dependency.flag, trigger.dependency.version))
                    builder.addTrigger(trigger.command, null, dependencyMap, Flags.SCRIPT_TRIGGERPOSTUN)
                }
            }

            if (!task.allPreTransCommands?.empty) {
                // pretrans* scriptlets are special. They may be run in an
                // environment where no shell exists. It's recommended that they
                // be avoided where possible, but where not, written in Lua:
                // https://fedoraproject.org/wiki/Packaging:Scriptlets#The_.25pretrans_Scriptlet
                builder.setPreTransScript(concat(task.allPreTransCommands))
            }
            if (!task.allPostTransCommands?.empty) {
                builder.setPostTransScript(scriptWithUtils(task.allCommonCommands, task.allPostTransCommands))
            }

            if (((Rpm) task).changeLogFile != null) {
                builder.addChangelogFile(((Rpm) task).changeLogFile)
            }

            rpmFileVisitorStrategy = new RpmFileVisitorStrategy(builder)
        }
    }

    @Override
    void visitFile(FileCopyDetailsInternal fileDetails, def specToLookAt) {
        logger.debug "adding file {}", fileDetails.relativePath.pathString

        def inputFile = extractFile(fileDetails)

        Directive fileType = lookup(specToLookAt, 'fileType')
        String user = lookup(specToLookAt, 'user') ?: task.user
        String group = lookup(specToLookAt, 'permissionGroup') ?: task.permissionGroup

        int fileMode =  FilePermissionUtil.getFileMode(specToLookAt) ?: FilePermissionUtil.getUnixPermission(fileDetails)
        def specAddParentsDir = lookup(specToLookAt, 'addParentDirs')
        boolean addParentsDir = specAddParentsDir != null ? specAddParentsDir : task.addParentDirs

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
        boolean createDirectoryEntry = specCreateDirectoryEntry != null ? specCreateDirectoryEntry : task.createDirectoryEntry
        def specAddParentsDir = lookup(specToLookAt, 'addParentDirs')
        boolean addParentsDir = specAddParentsDir != null ? specAddParentsDir : task.addParentDirs

        if (createDirectoryEntry) {
            logger.debug 'adding directory {}', dirDetails.relativePath.pathString
            int dirMode = FilePermissionUtil.getDirMode(specToLookAt) ?: FilePermissionUtil.getUnixPermission(dirDetails)
            Directive directive = (Directive) lookup(specToLookAt, 'fileType') ?: task.fileType
            String user = lookup(specToLookAt, 'user') ?: task.user
            String group = lookup(specToLookAt, 'permissionGroup') ?: task.permissionGroup
            Boolean setgid = lookup(specToLookAt, 'setgid')
            if (setgid == null) {
                setgid = task.setgid
            }
            if (setgid) {
                dirMode = dirMode | 02000
            }
            rpmFileVisitorStrategy.addDirectory(dirDetails, dirMode, directive, user, group, addParentsDir)
        }
    }

    @Override
    protected void addLink(Link link) {
        def user = link.user ?: task.user
        def permissionGroup = link.permissionGroup ?: task.permissionGroup
        builder.addLink(link.path, link.target, link.permissions, user, permissionGroup)
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
    protected void addProvides(Dependency dep) {
        builder.addProvides(dep.packageName, dep.version, dep.flag)
    }

    @Override
    protected void addDirectory(Directory directory) {
        def user = directory.user ? directory.user : task.user
        def permissionGroup = directory.permissionGroup ? directory.permissionGroup : task.permissionGroup
        builder.addDirectory(directory.path, directory.permissions, null, user, permissionGroup, directory.addParents)
    }

    @Override
    protected void end() {

        RandomAccessFile rpmFile
        try {
            rpmFile = new RandomAccessFile(task.getArchiveFile().get().asFile, "rw")
            rpmFile.setLength(0);
            builder.build(rpmFile.getChannel())
            logger.info 'Created rpm {}', rpmFile
        } finally {
            if (rpmFile != null) {
                rpmFile.close()
            }
        }
    }

    protected Builder createBuilder() {
        return new Builder()
    }

    String standardScriptDefines() {
        String result
        DeprecationLoggerUtils.whileDisabled {
            result = includeStandardDefines ?
                    String.format(" RPM_ARCH=%s \n RPM_OS=%s \n RPM_PACKAGE_NAME=%s \n RPM_PACKAGE_VERSION=%s \n RPM_PACKAGE_RELEASE=%s \n\n",
                            task.getArchString(),
                            task.os?.toString()?.toLowerCase() ?: '',
                            task.getPackageName(),
                            task.getVersion(),
                            task.getRelease()) : null
        }
        return result
    }

    String scriptWithUtils(List<Object> utils, List<Object> scripts) {
        def l = []
        def stdDefines = standardScriptDefines()
        if (stdDefines) {
            l.add(stdDefines)
        }
        l.addAll(utils)
        l.addAll(scripts)

        concat(l)
    }
}
