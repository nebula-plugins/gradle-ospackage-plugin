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

package com.netflix.gradle.plugins.packaging

import com.netflix.gradle.plugins.utils.DeprecationLoggerUtils
import groovy.transform.CompileDynamic
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionExecuter
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.util.GradleVersion
import org.gradle.work.DisableCachingByDefault
import org.redline_rpm.header.Architecture
import org.gradle.api.provider.Property

import java.util.concurrent.Callable

@DisableCachingByDefault
abstract class SystemPackagingTask extends OsPackageAbstractArchiveTask {
    private static final String HOST_NAME = getLocalHostName()

    @Internal
    final ObjectFactory objectFactory = project.objects

    @Nested
    abstract SystemPackagingExtension getExten() // Not File extension or ext list of properties, different kind of Extension

    @Internal
    ProjectPackagingExtension parentExten

    @Internal
    ProjectLayout projectLayout

    // TODO Add conventions to pull from extension
    SystemPackagingTask(ProjectLayout projectLayout) {
        super()
        // exten is now an abstract getter - Gradle will instantiate it
        this.projectLayout = projectLayout
        // I have no idea where Project came from
        parentExten = project.extensions.findByType(ProjectPackagingExtension)
        if (parentExten) {
            getRootSpec().with(parentExten.delegateCopySpec)
        }

        configureDuplicateStrategy()
        notCompatibleWithConfigurationCache("nebula.ospackage does not support configuration cache")
    }

    private void configureDuplicateStrategy() {
        setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
        rootSpec.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
        mainSpec.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
    }

    // Explicit delegation methods (replacing @Delegate)
    // These provide backward-compatible plain value access while using Property API internally

    @Input
    @Optional
    String getPackageName() { getExten().getPackageName().getOrNull() }
    void setPackageName(String value) { getExten().packageName(value) }
    void packageName(String value) { setPackageName(value) }

    @Input
    @Optional
    String getRelease() { getExten().getRelease().getOrNull() }
    void setRelease(String value) { getExten().release(value) }
    void release(String value) { setRelease(value) }

    @Input
    @Optional
    String getVersion() { getExten().getVersion().getOrNull() }
    void setVersion(String value) { getExten().version(value) }
    void version(String value) { setVersion(value) }

    @Input
    @Optional
    String getUser() { getExten().getUser().getOrNull() }
    void setUser(String value) { getExten().user(value) }
    void user(String value) { setUser(value) }

    @Input
    @Optional
    String getPermissionGroup() { getExten().getPermissionGroup().getOrNull() }
    void setPermissionGroup(String value) { getExten().permissionGroup(value) }
    void permissionGroup(String value) { setPermissionGroup(value) }

    @Input
    @Optional
    String getPackageGroup() { getExten().getPackageGroup().getOrNull() }
    void setPackageGroup(String value) { getExten().packageGroup(value) }
    void packageGroup(String value) { setPackageGroup(value) }

    @Input
    @Optional
    String getBuildHost() { getExten().getBuildHost().getOrNull() }
    void setBuildHost(String value) { getExten().buildHost(value) }
    void buildHost(String value) { setBuildHost(value) }

    @Input
    @Optional
    String getSummary() { getExten().getSummary().getOrNull() }
    void setSummary(String value) { getExten().summary(value) }
    void summary(String value) { setSummary(value) }

    @Input
    @Optional
    String getPackageDescription() { getExten().getPackageDescription().getOrNull() }
    void setPackageDescription(String value) { getExten().packageDescription(value) }
    void packageDescription(String value) { setPackageDescription(value) }

    @Input
    @Optional
    String getLicense() { getExten().getLicense().getOrNull() }
    void setLicense(String value) { getExten().license(value) }
    void license(String value) { setLicense(value) }

    @Input
    @Optional
    String getPackager() { getExten().getPackager().getOrNull() }
    void setPackager(String value) { getExten().packager(value) }
    void packager(String value) { setPackager(value) }

    @Input
    @Optional
    String getDistribution() { getExten().getDistribution().getOrNull() }
    void setDistribution(String value) { getExten().distribution(value) }
    void distribution(String value) { setDistribution(value) }

    @Input
    @Optional
    String getVendor() { getExten().getVendor().getOrNull() }
    void setVendor(String value) { getExten().vendor(value) }
    void vendor(String value) { setVendor(value) }

    @Input
    @Optional
    String getUrl() { getExten().getUrl().getOrNull() }
    void setUrl(String value) { getExten().url(value) }
    void url(String value) { setUrl(value) }

    @Input
    @Optional
    String getSourcePackage() { getExten().getSourcePackage().getOrNull() }
    void setSourcePackage(String value) { getExten().sourcePackage(value) }
    void sourcePackage(String value) { setSourcePackage(value) }

    @Input
    @Optional
    String getArchStr() { getExten().getArchStr().getOrNull() }
    void setArchStr(String value) { getExten().getArchStr().set(value) }

    void setArch(Object arch) {
        setArchStr((arch instanceof Architecture) ? arch.name() : arch.toString())
    }

    @Input
    @Optional
    String getMaintainer() { getExten().getMaintainer().getOrNull() }
    void setMaintainer(String value) { getExten().maintainer(value) }
    void maintainer(String value) { setMaintainer(value) }

    @Input
    @Optional
    String getUploaders() { getExten().getUploaders().getOrNull() }
    void setUploaders(String value) { getExten().uploaders(value) }
    void uploaders(String value) { setUploaders(value) }

    @Input
    @Optional
    String getPriority() { getExten().getPriority().getOrNull() }
    void setPriority(String value) { getExten().priority(value) }
    void priority(String value) { setPriority(value) }

    @Input
    @Optional
    Integer getEpoch() { getExten().getEpoch().getOrNull() }
    void setEpoch(Integer value) { getExten().epoch(value) }
    void epoch(Integer value) { setEpoch(value) }

    @Input
    @Optional
    Integer getUid() { getExten().getUid().getOrNull() }
    void setUid(Integer value) { getExten().uid(value) }
    void uid(Integer value) { setUid(value) }

    @Input
    @Optional
    Integer getGid() { getExten().getGid().getOrNull() }
    void setGid(Integer value) { getExten().gid(value) }
    void gid(Integer value) { setGid(value) }

    @Input
    @Optional
    Boolean getSetgid() { getExten().getSetgid().getOrNull() }
    void setSetgid(Boolean value) { getExten().setgid(value) }
    // Note: No convenience method due to naming collision with setter

    @Input
    @Optional
    Boolean getCreateDirectoryEntry() { getExten().getCreateDirectoryEntry().getOrNull() }
    void setCreateDirectoryEntry(Boolean value) { getExten().createDirectoryEntry(value) }
    void createDirectoryEntry(Boolean value) { setCreateDirectoryEntry(value) }

    @Input
    @Optional
    Boolean getAddParentDirs() { getExten().getAddParentDirs().getOrNull() }
    void setAddParentDirs(Boolean value) { getExten().addParentDirs(value) }
    void addParentDirs(Boolean value) { setAddParentDirs(value) }

    @Input
    @Optional
    String getSigningKeyId() { getExten().getSigningKeyId().getOrNull() }
    void setSigningKeyId(String value) { getExten().signingKeyId(value) }
    void signingKeyId(String value) { setSigningKeyId(value) }

    @Input
    @Optional
    String getSigningKeyPassphrase() { getExten().getSigningKeyPassphrase().getOrNull() }
    void setSigningKeyPassphrase(String value) { getExten().signingKeyPassphrase(value) }
    void signingKeyPassphrase(String value) { setSigningKeyPassphrase(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    File getSigningKeyRingFile() { getExten().getSigningKeyRingFile().getOrNull()?.asFile }
    void setSigningKeyRingFile(File value) { getExten().getSigningKeyRingFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPreInstallFile() { getExten().getPreInstallFile().getOrNull()?.asFile }
    void setPreInstallFile(File value) { getExten().getPreInstallFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPostInstallFile() { getExten().getPostInstallFile().getOrNull()?.asFile }
    void setPostInstallFile(File value) { getExten().getPostInstallFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPreUninstallFile() { getExten().getPreUninstallFile().getOrNull()?.asFile }
    void setPreUninstallFile(File value) { getExten().getPreUninstallFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPostUninstallFile() { getExten().getPostUninstallFile().getOrNull()?.asFile }
    void setPostUninstallFile(File value) { getExten().getPostUninstallFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getTriggerInstallFile() { getExten().getTriggerInstallFile().getOrNull()?.asFile }
    void setTriggerInstallFile(File value) { getExten().getTriggerInstallFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getTriggerUninstallFile() { getExten().getTriggerUninstallFile().getOrNull()?.asFile }
    void setTriggerUninstallFile(File value) { getExten().getTriggerUninstallFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getTriggerPostUninstallFile() { getExten().getTriggerPostUninstallFile().getOrNull()?.asFile }
    void setTriggerPostUninstallFile(File value) { getExten().getTriggerPostUninstallFile().set(value) }

    @Input
    @Optional
    def getFileType() { getExten().getFileType().getOrNull() }
    void setFileType(def value) { getExten().fileType(value) }

    @Input
    @Optional
    def getOs() { getExten().getOs().getOrNull() }
    void setOs(def value) { getExten().os(value) }

    @Input
    @Optional
    def getType() { getExten().getType().getOrNull() }
    void setType(def value) { getExten().type(value) }

    @Input
    @Optional
    def getMultiArch() { getExten().getMultiArch().getOrNull() }
    void setMultiArch(def value) { getExten().multiArch(value) }

    // List properties - return List for backward compatibility
    @Input
    @Optional
    List<String> getPrefixes() { getExten().getPrefixes().getOrElse([]) }
    def prefix(String value) { getExten().prefix(value) }

    @Input
    @Optional
    List<Object> getConfigurationFiles() { getExten().getConfigurationFiles().getOrElse([]) }
    def configurationFile(String value) { getExten().configurationFile(value) }

    @Input
    @Optional
    List<Object> getPreInstallCommands() { getExten().getPreInstallCommands().getOrElse([]) }
    def preInstall(String value) { getExten().preInstall(value) }
    def preInstall(File value) { getExten().preInstall(value) }
    def preInstallFile(File value) { getExten().preInstallFile(value) }

    @Input
    @Optional
    List<Object> getPostInstallCommands() { getExten().getPostInstallCommands().getOrElse([]) }
    def postInstall(String value) { getExten().postInstall(value) }
    def postInstall(File value) { getExten().postInstall(value) }
    def postInstallFile(File value) { getExten().postInstallFile(value) }

    @Input
    @Optional
    List<Object> getPreUninstallCommands() { getExten().getPreUninstallCommands().getOrElse([]) }
    def preUninstall(String value) { getExten().preUninstall(value) }
    def preUninstall(File value) { getExten().preUninstall(value) }
    def preUninstallFile(File value) { getExten().preUninstallFile(value) }

    @Input
    @Optional
    List<Object> getPostUninstallCommands() { getExten().getPostUninstallCommands().getOrElse([]) }
    def postUninstall(String value) { getExten().postUninstall(value) }
    def postUninstall(File value) { getExten().postUninstall(value) }
    def postUninstallFile(File value) { getExten().postUninstallFile(value) }

    @Input
    @Optional
    List getTriggerInstallCommands() { getExten().getTriggerInstallCommands().getOrElse([]) }
    def triggerInstall(File script, String packageName, String version='', int flag=0) {
        getExten().triggerInstall(script, packageName, version, flag)
    }
    def triggerInstallFile(File value) { getExten().triggerInstallFile(value) }

    @Input
    @Optional
    List getTriggerUninstallCommands() { getExten().getTriggerUninstallCommands().getOrElse([]) }
    def triggerUninstall(File script, String packageName, String version='', int flag=0) {
        getExten().triggerUninstall(script, packageName, version, flag)
    }
    def triggerUninstallFile(File value) { getExten().triggerUninstallFile(value) }

    @Input
    @Optional
    List getTriggerPostUninstallCommands() { getExten().getTriggerPostUninstallCommands().getOrElse([]) }
    def triggerPostUninstall(File script, String packageName, String version='', int flag=0) {
        getExten().triggerPostUninstall(script, packageName, version, flag)
    }
    def triggerPostUninstallFile(File value) { getExten().triggerPostUninstallFile(value) }

    @Input
    @Optional
    List<Object> getPreTransCommands() { getExten().getPreTransCommands().getOrElse([]) }
    def preTrans(String value) { getExten().preTrans(value) }
    def preTrans(File value) { getExten().preTrans(value) }

    @Input
    @Optional
    List<Object> getPostTransCommands() { getExten().getPostTransCommands().getOrElse([]) }
    def postTrans(String value) { getExten().postTrans(value) }
    def postTrans(File value) { getExten().postTrans(value) }

    @Input
    @Optional
    List<Object> getCommonCommands() { getExten().getCommonCommands().getOrElse([]) }
    def installUtils(String value) { getExten().installUtils(value) }
    def installUtils(File value) { getExten().installUtils(value) }

    @Input
    @Optional
    List<Object> getSupplementaryControlFiles() { getExten().getSupplementaryControlFiles().getOrElse([]) }
    def supplementaryControl(Object value) { getExten().supplementaryControl(value) }

    @Input
    @Optional
    List getLinks() { getExten().getLinks().getOrElse([]) }
    def link(String path, String target) { getExten().link(path, target) }
    def link(String path, String target, int permissions) { getExten().link(path, target, permissions) }
    def link(String path, String target, String user, String permissionGroup) {
        getExten().link(path, target, user, permissionGroup)
    }
    def link(String path, String target, int permissions, String user, String permissionGroup) {
        getExten().link(path, target, permissions, user, permissionGroup)
    }

    @Input
    @Optional
    List getDependencies() { getExten().getDependencies().getOrElse([]) }
    def requires(String packageName) { getExten().requires(packageName) }
    def requires(String packageName, String version) { getExten().requires(packageName, version) }
    def requires(String packageName, String version, int flag) { getExten().requires(packageName, version, flag) }

    @Input
    @Optional
    List getObsoletes() { getExten().getObsoletes().getOrElse([]) }
    def obsoletes(String packageName) { getExten().obsoletes(packageName) }
    def obsoletes(String packageName, String version, int flag) { getExten().obsoletes(packageName, version, flag) }

    @Input
    @Optional
    List getConflicts() { getExten().getConflicts().getOrElse([]) }
    def conflicts(String packageName) { getExten().conflicts(packageName) }
    def conflicts(String packageName, String version, int flag) { getExten().conflicts(packageName, version, flag) }

    @Input
    @Optional
    List getRecommends() { getExten().getRecommends().getOrElse([]) }
    def recommends(String packageName) { getExten().recommends(packageName) }
    def recommends(String packageName, String version, int flag) { getExten().recommends(packageName, version, flag) }

    @Input
    @Optional
    List getSuggests() { getExten().getSuggests().getOrElse([]) }
    def suggests(String packageName) { getExten().suggests(packageName) }
    def suggests(String packageName, String version, int flag) { getExten().suggests(packageName, version, flag) }

    @Input
    @Optional
    List getEnhances() { getExten().getEnhances().getOrElse([]) }
    def enhances(String packageName) { getExten().enhances(packageName) }
    def enhances(String packageName, String version, int flag) { getExten().enhances(packageName, version, flag) }

    @Input
    @Optional
    List getPreDepends() { getExten().getPreDepends().getOrElse([]) }
    def preDepends(String packageName) { getExten().preDepends(packageName) }
    def preDepends(String packageName, String version, int flag) { getExten().preDepends(packageName, version, flag) }

    @Input
    @Optional
    List getBreaks() { getExten().getBreaks().getOrElse([]) }
    def breaks(String packageName) { getExten().breaks(packageName) }
    def breaks(String packageName, String version, int flag) { getExten().breaks(packageName, version, flag) }

    @Input
    @Optional
    List getReplaces() { getExten().getReplaces().getOrElse([]) }
    def replaces(String packageName) { getExten().replaces(packageName) }
    def replaces(String packageName, String version, int flag) { getExten().replaces(packageName, version, flag) }

    @Input
    @Optional
    List getProvides() { getExten().getProvides().getOrElse([]) }
    def provides(String packageName) { getExten().provides(packageName) }
    def provides(String packageName, String version) { getExten().provides(packageName, version) }
    def provides(String packageName, String version, int flag) { getExten().provides(packageName, version, flag) }

    @Input
    @Optional
    List getDirectories() { getExten().getDirectories().getOrElse([]) }
    def directory(String path) { getExten().directory(path) }
    def directory(String path, boolean addParents) { getExten().directory(path, addParents) }
    def directory(String path, int permissions) { getExten().directory(path, permissions) }
    def directory(String path, int permissions, boolean addParents) { getExten().directory(path, permissions, addParents) }
    def directory(String path, int permissions, String user, String permissionGroup) {
        getExten().directory(path, permissions, user, permissionGroup)
    }
    def directory(String path, int permissions, String user, String permissionGroup, boolean addParents) {
        getExten().directory(path, permissions, user, permissionGroup, addParents)
    }

    @Input
    @Optional
    Map<String, String> getCustomFields() { getExten().getCustomFields().getOrElse([:]) }
    def customField(String key, String val) { getExten().customField(key, val) }
    def customField(Map<String, String> fields) { getExten().customField(fields) }

    // TODO Move outside task, since it's specific to a plugin
    protected void applyConventions() {
        // For all mappings, we're only being called if it wasn't explicitly set on the task. In which case, we'll want
        // to pull from the parentExten. And only then would we fallback on some other value.

        ConventionMapping mapping = ((IConventionAware) this).getConventionMapping()

        DeprecationLoggerUtils.whileDisabled {
            // Could come from extension
            mapping.map('packageName', {
                // BasePlugin defaults this to pluginConvention.getArchivesBaseName(), which in turns comes form project.name
                parentExten?.getPackageName() ?: getArchiveBaseName().getOrNull()
            })
            mapping.map('release', { parentExten?.getRelease() ?: getArchiveClassifier().getOrNull() })
            mapping.map('version', { sanitizeVersion() })
            mapping.map('epoch', { parentExten?.getEpoch() ?: 0 })
            mapping.map('signingKeyId', { parentExten?.getSigningKeyId() ?: '' })
            mapping.map('signingKeyPassphrase', { parentExten?.getSigningKeyPassphrase() ?: '' })
            mapping.map('signingKeyRingFile', {
                File defaultFile = new File(System.getProperty('user.home'), '.gnupg/secring.gpg')
                parentExten?.getSigningKeyRingFile() ?: (defaultFile.exists() ? defaultFile : null)
            })
            mapping.map('user', { parentExten?.getUser() ?: getPackager() })
            mapping.map('maintainer', { parentExten?.getMaintainer() ?: getPackager() })
            mapping.map('uploaders', { parentExten?.getUploaders() ?: getPackager() })
            mapping.map('permissionGroup', { parentExten?.getPermissionGroup() ?: '' })
            mapping.map('setgid', { parentExten?.getSetgid() ?: false })
            mapping.map('packageGroup', { parentExten?.getPackageGroup() })
            mapping.map('buildHost', { parentExten?.getBuildHost() ?: HOST_NAME })
            mapping.map('summary', { parentExten?.getSummary() ?: getPackageName() })
            mapping.map('packageDescription', {
                String packageDescription = parentExten?.getPackageDescription() ?: project.getDescription()
                packageDescription ?: ''
            })
            mapping.map('license', { parentExten?.getLicense() ?: '' })
            mapping.map('packager', { parentExten?.getPackager() ?: System.getProperty('user.name', '') })
            mapping.map('distribution', { parentExten?.getDistribution() ?: '' })
            mapping.map('vendor', { parentExten?.getVendor() ?: '' })
            mapping.map('url', { parentExten?.getUrl() ?: '' })
            mapping.map('sourcePackage', { parentExten?.getSourcePackage() ?: '' })
            mapping.map('createDirectoryEntry', { parentExten?.getCreateDirectoryEntry() ?: false })
            mapping.map('priority', { parentExten?.getPriority() ?: 'optional' })

            mapping.map('preInstallFile', { parentExten?.getPreInstallFile() })
            mapping.map('postInstallFile', { parentExten?.getPostInstallFile() })
            mapping.map('preUninstallFile', { parentExten?.getPreUninstallFile() })
            mapping.map('postUninstallFile', { parentExten?.getPostUninstallFile() })

            // Task Specific
            if(GradleVersion.current().compareTo(GradleVersion.version("7.0.0")) >= 0) {
                getArchiveFileName().convention(project.provider { assembleArchiveName() })
                getArchiveVersion().convention(determineArchiveVersion())
            } else {
                mapping.map('archiveFile', { determineArchiveFile() })
                mapping.map('archiveName', { assembleArchiveName() })
                mapping.map('archivePath', { determineArchivePath() })
                mapping.map('archiveVersion', { determineArchiveVersion() })
            }
        }
    }

    private String sanitizeVersion() {
        sanitizeVersion(parentExten?.getVersion()?.getOrNull() ?: project.getVersion().toString())
    }

    private String sanitizeVersion(String version) {
        version == 'unspecified' ? '0' : version.replaceAll(/\+.*/, '').replaceAll(/-/, '~')
    }

    abstract String assembleArchiveName()

    Provider<RegularFile> determineArchiveFile() {
        // Use map() to avoid eager .get() call - lazily compute the file when needed
        return getDestinationDirectory().map { directory ->
            new DestinationFile(new File(directory.asFile.path, assembleArchiveName())) as RegularFile
        }
    }

    Provider<String> determineArchiveVersion() {
        String version = sanitizeVersion(parentExten?.getVersion()?.getOrNull() ?: project.getVersion().toString())
        Property<String> archiveVersion = objectFactory.property(String)
        archiveVersion.set(version)
        return archiveVersion
    }

    File determineArchivePath() {
        return determineArchiveFile().get().asFile
    }

    private static String getLocalHostName() {
        try {
            return InetAddress.localHost.hostName
        } catch (UnknownHostException ignore) {
            return "unknown"
        }
    }

    @Override
    @TaskAction
    @CompileDynamic
    void copy() {
        use(CopySpecEnhancement) {
            CopyActionExecuter copyActionExecuter = this.createCopyActionExecuter();
            CopyAction copyAction = this.createCopyAction();
            WorkResult didWork = copyActionExecuter.execute(this.rootSpec, copyAction);
            this.setDidWork(didWork.getDidWork());
        }
    }


    @Input
    @Optional
    List<Object> getAllConfigurationFiles() {
        return getConfigurationFiles() + (parentExten?.getConfigurationFiles()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPreInstallCommands() {
        return getPreInstallCommands() + (parentExten?.getPreInstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPostInstallCommands() {
        return getPostInstallCommands() + (parentExten?.getPostInstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPreUninstallCommands() {
        return getPreUninstallCommands() + (parentExten?.getPreUninstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPostUninstallCommands() {
        return getPostUninstallCommands() + (parentExten?.getPostUninstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Trigger> getAllTriggerIn() {
        return getTriggerInstallCommands() + (parentExten?.getTriggerInstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Trigger> getAllTriggerUn() {
        return getTriggerUninstallCommands() + (parentExten?.getTriggerUninstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Trigger> getAllTriggerPostUn() {
        return getTriggerPostUninstallCommands() + (parentExten?.getTriggerPostUninstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPreTransCommands() {
        return getPreTransCommands() + (parentExten?.getPreTransCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPostTransCommands() {
        return getPostTransCommands() + (parentExten?.getPostTransCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllCommonCommands() {
        return getCommonCommands() + (parentExten?.getCommonCommands()?.getOrElse([]) ?: [])
    }

    /**
     * @return supplementary control files consisting of a combination of Strings and Files
     */
    @Input
    @Optional
    List<Object> getAllSupplementaryControlFiles() {
        return getSupplementaryControlFiles() + (parentExten?.getSupplementaryControlFiles()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Link> getAllLinks() {
        return getLinks() + (parentExten?.getLinks()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Dependency> getAllDependencies() {
        return getDependencies() + (parentExten?.getDependencies()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    def getAllPrefixes() {
        return (getPrefixes() + (parentExten?.getPrefixes()?.getOrElse([]) ?: [])).unique()
    }

    @Input
    @Optional
    List<Dependency> getAllProvides() {
        return getProvides() + (parentExten?.getProvides()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Dependency> getAllObsoletes() {
        return getObsoletes() + (parentExten?.getObsoletes()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Dependency> getAllConflicts() {
        return getConflicts() + (parentExten?.getConflicts()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Directory> getAllDirectories() {
        return getDirectories() + (parentExten?.getDirectories()?.getOrElse([]) ?: [])
    }

    @Override
    abstract AbstractPackagingCopyAction createCopyAction()

    @Internal
    String getArchString() {
        return getArchStr()?.toLowerCase();
    }

    @Override
    AbstractCopyTask from(Object... sourcePaths) {
        for (Object sourcePath : sourcePaths) {
            from(sourcePath, {})
        }
        return this
    }

    @Override
    @CompileDynamic
    AbstractCopyTask from(Object sourcePath, Closure c) {
        use(CopySpecEnhancement) {
            getMainSpec().from(sourcePath, c)
        }
        return this
    }

    @Override
    @CompileDynamic
    AbstractArchiveTask into(Object destPath, Closure configureClosure) {
        use(CopySpecEnhancement) {
            getMainSpec().into(destPath, configureClosure)
        }
        return this
    }

    /**
     * Defines input files annotation with @SkipWhenEmpty as a workaround to force building the archive even if no
     * from clause is declared. Without this method the task would be marked UP-TO-DATE - the actual archive creation
     * would be skipped. For more information see discussion on <a href="http://gradle.1045684.n5.nabble.com/Allow-subclass-of-AbstractCopyTask-to-execute-task-action-without-declared-sources-td5712928.html">Gradle dev list</a>.
     *
     * The provided file collection is not supposed to be used or modified anywhere else in the task.
     *
     * @return Collection of files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    FileCollection getFakeFiles() {
        project.files('fake')
    }

    @Override
    @CompileDynamic
    AbstractCopyTask exclude(Closure excludeSpec) {
        use(CopySpecEnhancement) {
            getMainSpec().exclude(excludeSpec)
        }
        return this
    }

    @Override
    @CompileDynamic
    AbstractCopyTask filter(Closure closure) {
        use(CopySpecEnhancement) {
            getMainSpec().filter(closure)
        }
        return this
    }

    @Override
    @CompileDynamic
    AbstractCopyTask rename(Closure closure) {
        use(CopySpecEnhancement) {
            getMainSpec().rename(closure)
        }
        return this
    }

    private static class DestinationFile implements RegularFile {
        private final File file

        DestinationFile(File file) {
            this.file = file
        }

        String toString() {
            return this.file.toString()
        }

        @Override
        File getAsFile() {
            return this.file
        }
    }
}
