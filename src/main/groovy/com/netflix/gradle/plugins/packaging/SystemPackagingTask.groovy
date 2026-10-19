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
import com.netflix.gradle.plugins.deb.control.MultiArch
import groovy.transform.CompileDynamic
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionExecuter
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.util.GradleVersion
import org.gradle.work.DisableCachingByDefault
import org.redline_rpm.header.Architecture
import org.redline_rpm.header.Os
import org.redline_rpm.header.RpmType
import org.redline_rpm.payload.Directive

import javax.inject.Inject
import java.util.concurrent.Callable

@DisableCachingByDefault
abstract class SystemPackagingTask extends OsPackageAbstractArchiveTask {
    private static final String HOST_NAME = getLocalHostName()

    @Inject
    abstract ObjectFactory getObjectFactory()

    @Inject
    abstract ProviderFactory getProviders()

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
    String getPackageName() { getExten().getPackageNameProperty().getOrNull() }
    void setPackageName(String value) { getExten().packageName(value) }
    void packageName(String value) { setPackageName(value) }

    @Input
    @Optional
    String getRelease() { getExten().getReleaseProperty().getOrNull() }
    void setRelease(String value) { getExten().release(value) }
    void release(String value) { setRelease(value) }

    @Input
    @Optional
    String getVersion() { getExten().getVersionProperty().getOrNull() }
    void setVersion(String value) { getExten().version(value) }
    void version(String value) { setVersion(value) }

    @Input
    @Optional
    String getUser() { getExten().getUserProperty().getOrNull() }
    void setUser(String value) { getExten().user(value) }
    void user(String value) { setUser(value) }

    @Input
    @Optional
    String getPermissionGroup() { getExten().getPermissionGroupProperty().getOrNull() }
    void setPermissionGroup(String value) { getExten().permissionGroup(value) }
    void permissionGroup(String value) { setPermissionGroup(value) }

    @Input
    @Optional
    String getPackageGroup() { getExten().getPackageGroupProperty().getOrNull() }
    void setPackageGroup(String value) { getExten().packageGroup(value) }
    void packageGroup(String value) { setPackageGroup(value) }

    @Input
    @Optional
    String getBuildHost() { getExten().getBuildHostProperty().getOrNull() }
    void setBuildHost(String value) { getExten().buildHost(value) }
    void buildHost(String value) { setBuildHost(value) }

    @Input
    @Optional
    String getSummary() { getExten().getSummaryProperty().getOrNull() }
    void setSummary(String value) { getExten().summary(value) }
    void summary(String value) { setSummary(value) }

    @Input
    @Optional
    String getPackageDescription() { getExten().getPackageDescriptionProperty().getOrNull() }
    void setPackageDescription(String value) { getExten().packageDescription(value) }
    void packageDescription(String value) { setPackageDescription(value) }

    @Input
    @Optional
    String getLicense() { getExten().getLicenseProperty().getOrNull() }
    void setLicense(String value) { getExten().license(value) }
    void license(String value) { setLicense(value) }

    @Input
    @Optional
    String getPackager() { getExten().getPackagerProperty().getOrNull() }
    void setPackager(String value) { getExten().packager(value) }
    void packager(String value) { setPackager(value) }

    @Input
    @Optional
    String getDistribution() { getExten().getDistributionProperty().getOrNull() }
    void setDistribution(String value) { getExten().distribution(value) }
    void distribution(String value) { setDistribution(value) }

    @Input
    @Optional
    String getVendor() { getExten().getVendorProperty().getOrNull() }
    void setVendor(String value) { getExten().vendor(value) }
    void vendor(String value) { setVendor(value) }

    @Input
    @Optional
    String getUrl() { getExten().getUrlProperty().getOrNull() }
    void setUrl(String value) { getExten().url(value) }
    void url(String value) { setUrl(value) }

    @Input
    @Optional
    String getSourcePackage() { getExten().getSourcePackageProperty().getOrNull() }
    void setSourcePackage(String value) { getExten().sourcePackage(value) }
    void sourcePackage(String value) { setSourcePackage(value) }

    @Input
    @Optional
    String getArchStr() { getExten().getArchStrProperty().getOrNull() }
    void setArchStr(String value) { getExten().getArchStrProperty().set(value) }

    void setArch(Object arch) {
        setArchStr((arch instanceof Architecture) ? arch.name() : arch.toString())
    }

    @Input
    @Optional
    String getMaintainer() { getExten().getMaintainerProperty().getOrNull() }
    void setMaintainer(String value) { getExten().maintainer(value) }
    void maintainer(String value) { setMaintainer(value) }

    @Input
    @Optional
    String getUploaders() { getExten().getUploadersProperty().getOrNull() }
    void setUploaders(String value) { getExten().uploaders(value) }
    void uploaders(String value) { setUploaders(value) }

    @Input
    @Optional
    String getPriority() { getExten().getPriorityProperty().getOrNull() }
    void setPriority(String value) { getExten().priority(value) }
    void priority(String value) { setPriority(value) }

    @Input
    @Optional
    Integer getEpoch() { getExten().getEpochProperty().getOrNull() }
    void setEpoch(Integer value) { getExten().epoch(value) }
    void epoch(Integer value) { setEpoch(value) }

    @Input
    @Optional
    Integer getUid() { getExten().getUidProperty().getOrNull() }
    void setUid(Integer value) { getExten().uid(value) }
    void uid(Integer value) { setUid(value) }

    @Input
    @Optional
    Integer getGid() { getExten().getGidProperty().getOrNull() }
    void setGid(Integer value) { getExten().gid(value) }
    void gid(Integer value) { setGid(value) }

    @Input
    @Optional
    Boolean getSetgid() { getExten().getSetgidProperty().getOrNull() }
    void setSetgid(Boolean value) { getExten().setgid(value) }
    // Note: No convenience method due to naming collision with setter

    @Input
    @Optional
    Boolean getCreateDirectoryEntry() { getExten().getCreateDirectoryEntryProperty().getOrNull() }
    void setCreateDirectoryEntry(Boolean value) { getExten().createDirectoryEntry(value) }
    void createDirectoryEntry(Boolean value) { setCreateDirectoryEntry(value) }

    @Input
    @Optional
    Boolean getAddParentDirs() { getExten().getAddParentDirsProperty().getOrNull() }
    void setAddParentDirs(Boolean value) { getExten().addParentDirs(value) }
    void addParentDirs(Boolean value) { setAddParentDirs(value) }

    @Input
    @Optional
    String getSigningKeyId() { getExten().getSigningKeyIdProperty().getOrNull() }
    void setSigningKeyId(String value) { getExten().signingKeyId(value) }
    void signingKeyId(String value) { setSigningKeyId(value) }

    @Input
    @Optional
    String getSigningKeyPassphrase() { getExten().getSigningKeyPassphraseProperty().getOrNull() }
    void setSigningKeyPassphrase(String value) { getExten().signingKeyPassphrase(value) }
    void signingKeyPassphrase(String value) { setSigningKeyPassphrase(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    File getSigningKeyRingFile() { getExten().getSigningKeyRingFileProperty().getOrNull()?.asFile }
    void setSigningKeyRingFile(File value) { getExten().getSigningKeyRingFileProperty().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPreInstallFile() { getExten().getPreInstallFileProperty().getOrNull()?.asFile }
    void setPreInstallFile(File value) { getExten().getPreInstallFileProperty().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPostInstallFile() { getExten().getPostInstallFileProperty().getOrNull()?.asFile }
    void setPostInstallFile(File value) { getExten().getPostInstallFileProperty().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPreUninstallFile() { getExten().getPreUninstallFileProperty().getOrNull()?.asFile }
    void setPreUninstallFile(File value) { getExten().getPreUninstallFileProperty().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPostUninstallFile() { getExten().getPostUninstallFileProperty().getOrNull()?.asFile }
    void setPostUninstallFile(File value) { getExten().getPostUninstallFileProperty().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getTriggerInstallFile() { getExten().getTriggerInstallFileProperty().getOrNull()?.asFile }
    void setTriggerInstallFile(File value) { getExten().getTriggerInstallFileProperty().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getTriggerUninstallFile() { getExten().getTriggerUninstallFileProperty().getOrNull()?.asFile }
    void setTriggerUninstallFile(File value) { getExten().getTriggerUninstallFileProperty().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getTriggerPostUninstallFile() { getExten().getTriggerPostUninstallFileProperty().getOrNull()?.asFile }
    void setTriggerPostUninstallFile(File value) { getExten().getTriggerPostUninstallFileProperty().set(value) }

    @Input
    @Optional
    Directive getFileType() { getExten().getFileTypeProperty().getOrNull() }
    void setFileType(Directive value) { getExten().fileType(value) }

    @Input
    @Optional
    Os getOs() { getExten().getOsProperty().getOrNull() }
    void setOs(Os value) { getExten().os(value) }

    @Input
    @Optional
    RpmType getType() { getExten().getTypeProperty().getOrNull() }
    void setType(RpmType value) { getExten().type(value) }

    @Input
    @Optional
    MultiArch getMultiArch() { getExten().getMultiArchProperty().getOrNull() }
    void setMultiArch(MultiArch value) { getExten().multiArch(value) }

    // List properties - return List for backward compatibility
    @Input
    @Optional
    List<String> getPrefixes() { getExten().getPrefixes() }
    def prefix(String value) { getExten().prefix(value) }

    @Input
    @Optional
    List<String> getConfigurationFiles() { getExten().getConfigurationFiles() }
    def configurationFile(String value) { getExten().configurationFile(value) }

    @Input
    @Optional
    List<String> getPreInstallCommands() { getExten().getPreInstallCommands() }
    def preInstall(String value) { getExten().preInstall(value) }
    def preInstall(File value) { getExten().preInstall(value) }
    def preInstallFile(File value) { getExten().preInstallFile(value) }

    @Input
    @Optional
    List<String> getPostInstallCommands() { getExten().getPostInstallCommands() }
    def postInstall(String value) { getExten().postInstall(value) }
    def postInstall(File value) { getExten().postInstall(value) }
    def postInstallFile(File value) { getExten().postInstallFile(value) }

    @Input
    @Optional
    List<String> getPreUninstallCommands() { getExten().getPreUninstallCommands() }
    def preUninstall(String value) { getExten().preUninstall(value) }
    def preUninstall(File value) { getExten().preUninstall(value) }
    def preUninstallFile(File value) { getExten().preUninstallFile(value) }

    @Input
    @Optional
    List<String> getPostUninstallCommands() { getExten().getPostUninstallCommands() }
    def postUninstall(String value) { getExten().postUninstall(value) }
    def postUninstall(File value) { getExten().postUninstall(value) }
    def postUninstallFile(File value) { getExten().postUninstallFile(value) }

    @Input
    @Optional
    List getTriggerInstallCommands() { getExten().getTriggerInstallCommands() }
    def triggerInstall(File script, String packageName, String version='', int flag=0) {
        getExten().triggerInstall(script, packageName, version, flag)
    }
    def triggerInstallFile(File value) { getExten().triggerInstallFile(value) }

    @Input
    @Optional
    List getTriggerUninstallCommands() { getExten().getTriggerUninstallCommands() }
    def triggerUninstall(File script, String packageName, String version='', int flag=0) {
        getExten().triggerUninstall(script, packageName, version, flag)
    }
    def triggerUninstallFile(File value) { getExten().triggerUninstallFile(value) }

    @Input
    @Optional
    List getTriggerPostUninstallCommands() { getExten().getTriggerPostUninstallCommands() }
    def triggerPostUninstall(File script, String packageName, String version='', int flag=0) {
        getExten().triggerPostUninstall(script, packageName, version, flag)
    }
    def triggerPostUninstallFile(File value) { getExten().triggerPostUninstallFile(value) }

    @Input
    @Optional
    List<String> getPreTransCommands() { getExten().getPreTransCommands() }
    def preTrans(String value) { getExten().preTrans(value) }
    def preTrans(File value) { getExten().preTrans(value) }

    @Input
    @Optional
    List<String> getPostTransCommands() { getExten().getPostTransCommands() }
    def postTrans(String value) { getExten().postTrans(value) }
    def postTrans(File value) { getExten().postTrans(value) }

    @Input
    @Optional
    List<String> getCommonCommands() { getExten().getCommonCommands() }
    def installUtils(String value) { getExten().installUtils(value) }
    def installUtils(File value) { getExten().installUtils(value) }

    @Input
    @Optional
    List<String> getSupplementaryControlFiles() { getExten().getSupplementaryControlFiles() }
    def supplementaryControl(Object value) { getExten().supplementaryControl(value) }

    @Input
    @Optional
    List getLinks() { getExten().getLinks() }
    Link link(String path, String target) { getExten().link(path, target) }
    Link link(String path, String target, int permissions) { getExten().link(path, target, permissions) }
    Link link(String path, String target, String user, String permissionGroup) {
        getExten().link(path, target, user, permissionGroup)
    }
    Link link(String path, String target, int permissions, String user, String permissionGroup) {
        getExten().link(path, target, permissions, user, permissionGroup)
    }

    @Input
    @Optional
    List getDependencies() { getExten().getDependencies() }
    Dependency requires(String packageName) { getExten().requires(packageName) }
    Dependency requires(String packageName, String version) { getExten().requires(packageName, version) }
    Dependency requires(String packageName, String version, int flag) { getExten().requires(packageName, version, flag) }

    @Input
    @Optional
    List getObsoletes() { getExten().getObsoletes() }
    Dependency obsoletes(String packageName) { getExten().obsoletes(packageName) }
    Dependency obsoletes(String packageName, String version, int flag) { getExten().obsoletes(packageName, version, flag) }

    @Input
    @Optional
    List getConflicts() { getExten().getConflicts() }
    Dependency conflicts(String packageName) { getExten().conflicts(packageName) }
    Dependency conflicts(String packageName, String version, int flag) { getExten().conflicts(packageName, version, flag) }

    @Input
    @Optional
    List getRecommends() { getExten().getRecommends() }
    Dependency recommends(String packageName) { getExten().recommends(packageName) }
    Dependency recommends(String packageName, String version, int flag) { getExten().recommends(packageName, version, flag) }

    @Input
    @Optional
    List getSuggests() { getExten().getSuggests() }
    Dependency suggests(String packageName) { getExten().suggests(packageName) }
    Dependency suggests(String packageName, String version, int flag) { getExten().suggests(packageName, version, flag) }

    @Input
    @Optional
    List getEnhances() { getExten().getEnhances() }
    Dependency enhances(String packageName) { getExten().enhances(packageName) }
    Dependency enhances(String packageName, String version, int flag) { getExten().enhances(packageName, version, flag) }

    @Input
    @Optional
    List getPreDepends() { getExten().getPreDepends() }
    Dependency preDepends(String packageName) { getExten().preDepends(packageName) }
    Dependency preDepends(String packageName, String version, int flag) { getExten().preDepends(packageName, version, flag) }

    @Input
    @Optional
    List getBreaks() { getExten().getBreaks() }
    Dependency breaks(String packageName) { getExten().breaks(packageName) }
    Dependency breaks(String packageName, String version, int flag) { getExten().breaks(packageName, version, flag) }

    @Input
    @Optional
    List getReplaces() { getExten().getReplaces() }
    Dependency replaces(String packageName) { getExten().replaces(packageName) }
    Dependency replaces(String packageName, String version, int flag) { getExten().replaces(packageName, version, flag) }

    @Input
    @Optional
    List getProvides() { getExten().getProvides() }
    Dependency provides(String packageName) { getExten().provides(packageName) }
    Dependency provides(String packageName, String version) { getExten().provides(packageName, version) }
    Dependency provides(String packageName, String version, int flag) { getExten().provides(packageName, version, flag) }

    @Input
    @Optional
    List getDirectories() { getExten().getDirectories() }
    Directory directory(String path) { getExten().directory(path) }
    Directory directory(String path, boolean addParents) { getExten().directory(path, addParents) }
    Directory directory(String path, int permissions) { getExten().directory(path, permissions) }
    Directory directory(String path, int permissions, boolean addParents) { getExten().directory(path, permissions, addParents) }
    Directory directory(String path, int permissions, String user, String permissionGroup) {
        getExten().directory(path, permissions, user, permissionGroup)
    }
    Directory directory(String path, int permissions, String user, String permissionGroup, boolean addParents) {
        getExten().directory(path, permissions, user, permissionGroup, addParents)
    }

    @Input
    @Optional
    Map<String, String> getCustomFields() { getExten().getCustomFields() }
    def customField(String key, String val) { getExten().customField(key, val) }
    def customField(Map<String, String> fields) { getExten().customField(fields) }

    // Methods generated by @Delegate in 12.x remain callable by compiled consumers.
    void setPrefixes(List value) { getExten().setPrefixes(value) }
    void setLinks(List value) { getExten().setLinks(value) }
    void setDependencies(List value) { getExten().setDependencies(value) }
    void setObsoletes(List value) { getExten().setObsoletes(value) }
    void setConflicts(List value) { getExten().setConflicts(value) }
    void setRecommends(List value) { getExten().setRecommends(value) }
    void setSuggests(List value) { getExten().setSuggests(value) }
    void setEnhances(List value) { getExten().setEnhances(value) }
    void setPreDepends(List value) { getExten().setPreDepends(value) }
    void setBreaks(List value) { getExten().setBreaks(value) }
    void setReplaces(List value) { getExten().setReplaces(value) }
    void setProvides(List value) { getExten().setProvides(value) }
    void setDirectories(List value) { getExten().setDirectories(value) }
    void setCustomFields(Map<String, String> value) { getExten().setCustomFields(value) }
    void setSetgid(boolean value) { getExten().setSetgid(value) }
    Object setInstallUtils(File value) { getExten().setInstallUtils(value) }
    Object setConfigurationFile(String value) { getExten().setConfigurationFile(value) }
    Object setPreInstall(File value) { getExten().setPreInstall(value) }
    Object setPostInstall(File value) { getExten().setPostInstall(value) }
    Object setPreUninstall(File value) { getExten().setPreUninstall(value) }
    Object setPostUninstall(File value) { getExten().setPostUninstall(value) }
    Object setPreTrans(File value) { getExten().setPreTrans(value) }
    Object setPostTrans(File value) { getExten().setPostTrans(value) }
    Object setTriggerInstall(File script, String packageName, String version='', int flag=0) {
        getExten().setTriggerInstall(script, packageName, version, flag)
    }
    Object setTriggerUninstall(File script, String packageName, String version='', int flag=0) {
        getExten().setTriggerUninstall(script, packageName, version, flag)
    }
    Object setTriggerPostUninstall(File script, String packageName, String version='', int flag=0) {
        getExten().setTriggerPostUninstall(script, packageName, version, flag)
    }

    // Apply conventions to task extension properties using modern Property API
    protected void applyConventions() {
        applyConventions(project)
    }

    protected void applyConventions(Project project) {
        applyBaseConventions(project)
    }

    // Calling an overloaded super.applyConventions(Project) recurses on Gradle 8.6's Groovy runtime.
    protected final void applyBaseConventions(Project project) {
        Provider<String> projectVersion = ProjectDefaultProviders.version(project)
        // Apply default conventions FIRST (lowest priority)
        exten.packageNameProperty.convention(getArchiveBaseName())
        exten.releaseProperty.convention(getArchiveClassifier())
        Provider<String> defaultVersion = parentExten ? parentExten.versionProperty.orElse(projectVersion) : projectVersion
        exten.versionProperty.convention(defaultVersion.map { sanitizeVersion(it) })
        exten.epochProperty.convention(0)
        exten.signingKeyIdProperty.convention('')
        exten.signingKeyPassphraseProperty.convention('')
        exten.signingKeyRingFileProperty.convention(
            projectLayout.file(getProviders().provider {
                File defaultFile = new File(System.getProperty('user.home'), '.gnupg/secring.gpg')
                defaultFile.exists() ? defaultFile : null
            })
        )
        exten.userProperty.convention(getProviders().provider { getPackager() })
        exten.maintainerProperty.convention(getProviders().provider { getPackager() })
        exten.uploadersProperty.convention(getProviders().provider { getPackager() })
        exten.permissionGroupProperty.convention('')
        exten.setgidProperty.convention(false)
        exten.buildHostProperty.convention(HOST_NAME)
        exten.summaryProperty.convention(exten.packageNameProperty)
        exten.packageDescriptionProperty.convention(ProjectDefaultProviders.description(project))
        exten.licenseProperty.convention('')
        exten.packagerProperty.convention(System.getProperty('user.name', ''))
        exten.distributionProperty.convention('')
        exten.vendorProperty.convention('')
        exten.urlProperty.convention('')
        exten.sourcePackageProperty.convention('')
        exten.createDirectoryEntryProperty.convention(false)
        exten.priorityProperty.convention('optional')

        // Then apply conventions from parentExten (higher priority - override defaults)
        // Only override if parentExten has a value
        if (parentExten) {
            if (parentExten.packageNameProperty.isPresent()) exten.packageNameProperty.convention(parentExten.packageNameProperty)
            if (parentExten.releaseProperty.isPresent()) exten.releaseProperty.convention(parentExten.releaseProperty)
            if (parentExten.versionProperty.isPresent()) exten.versionProperty.convention(parentExten.versionProperty)
            if (parentExten.epochProperty.isPresent()) exten.epochProperty.convention(parentExten.epochProperty)
            if (parentExten.signingKeyIdProperty.isPresent()) exten.signingKeyIdProperty.convention(parentExten.signingKeyIdProperty)
            if (parentExten.signingKeyPassphraseProperty.isPresent()) exten.signingKeyPassphraseProperty.convention(parentExten.signingKeyPassphraseProperty)
            if (parentExten.signingKeyRingFileProperty.isPresent()) exten.signingKeyRingFileProperty.convention(parentExten.signingKeyRingFileProperty)
            if (parentExten.userProperty.isPresent()) exten.userProperty.convention(parentExten.userProperty)
            if (parentExten.maintainerProperty.isPresent()) exten.maintainerProperty.convention(parentExten.maintainerProperty)
            if (parentExten.uploadersProperty.isPresent()) exten.uploadersProperty.convention(parentExten.uploadersProperty)
            if (parentExten.permissionGroupProperty.isPresent()) exten.permissionGroupProperty.convention(parentExten.permissionGroupProperty)
            if (parentExten.setgidProperty.isPresent()) exten.setgidProperty.convention(parentExten.setgidProperty)
            if (parentExten.packageGroupProperty.isPresent()) exten.packageGroupProperty.convention(parentExten.packageGroupProperty)
            if (parentExten.buildHostProperty.isPresent()) exten.buildHostProperty.convention(parentExten.buildHostProperty)
            if (parentExten.summaryProperty.isPresent()) exten.summaryProperty.convention(parentExten.summaryProperty)
            if (parentExten.packageDescriptionProperty.isPresent()) exten.packageDescriptionProperty.convention(parentExten.packageDescriptionProperty)
            if (parentExten.licenseProperty.isPresent()) exten.licenseProperty.convention(parentExten.licenseProperty)
            if (parentExten.packagerProperty.isPresent()) exten.packagerProperty.convention(parentExten.packagerProperty)
            if (parentExten.distributionProperty.isPresent()) exten.distributionProperty.convention(parentExten.distributionProperty)
            if (parentExten.vendorProperty.isPresent()) exten.vendorProperty.convention(parentExten.vendorProperty)
            if (parentExten.urlProperty.isPresent()) exten.urlProperty.convention(parentExten.urlProperty)
            if (parentExten.sourcePackageProperty.isPresent()) exten.sourcePackageProperty.convention(parentExten.sourcePackageProperty)
            if (parentExten.createDirectoryEntryProperty.isPresent()) exten.createDirectoryEntryProperty.convention(parentExten.createDirectoryEntryProperty)
            if (parentExten.priorityProperty.isPresent()) exten.priorityProperty.convention(parentExten.priorityProperty)
            if (parentExten.preInstallFileProperty.isPresent()) exten.preInstallFileProperty.convention(parentExten.preInstallFileProperty)
            if (parentExten.postInstallFileProperty.isPresent()) exten.postInstallFileProperty.convention(parentExten.postInstallFileProperty)
            if (parentExten.preUninstallFileProperty.isPresent()) exten.preUninstallFileProperty.convention(parentExten.preUninstallFileProperty)
            if (parentExten.postUninstallFileProperty.isPresent()) exten.postUninstallFileProperty.convention(parentExten.postUninstallFileProperty)
        }

        // Task-specific conventions
        if(GradleVersion.current().compareTo(GradleVersion.version("7.0.0")) >= 0) {
            getArchiveFileName().convention(getProviders().provider { assembleArchiveName() })
            getArchiveVersion().convention(determineArchiveVersion())
        }
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
        return exten.versionProperty
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
    List<String> getAllConfigurationFiles() {
        return getConfigurationFiles() + (parentExten?.getConfigurationFilesProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<String> getAllPreInstallCommands() {
        return getPreInstallCommands() + (parentExten?.getPreInstallCommandsProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<String> getAllPostInstallCommands() {
        return getPostInstallCommands() + (parentExten?.getPostInstallCommandsProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<String> getAllPreUninstallCommands() {
        return getPreUninstallCommands() + (parentExten?.getPreUninstallCommandsProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<String> getAllPostUninstallCommands() {
        return getPostUninstallCommands() + (parentExten?.getPostUninstallCommandsProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Trigger> getAllTriggerIn() {
        return getTriggerInstallCommands() + (parentExten?.getTriggerInstallCommandsProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Trigger> getAllTriggerUn() {
        return getTriggerUninstallCommands() + (parentExten?.getTriggerUninstallCommandsProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Trigger> getAllTriggerPostUn() {
        return getTriggerPostUninstallCommands() + (parentExten?.getTriggerPostUninstallCommandsProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<String> getAllPreTransCommands() {
        return getPreTransCommands() + (parentExten?.getPreTransCommandsProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<String> getAllPostTransCommands() {
        return getPostTransCommands() + (parentExten?.getPostTransCommandsProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<String> getAllCommonCommands() {
        return getCommonCommands() + (parentExten?.getCommonCommandsProperty()?.getOrElse([]) ?: [])
    }

    /**
     * @return supplementary control files consisting of Strings (file paths)
     */
    @Input
    @Optional
    List<String> getAllSupplementaryControlFiles() {
        return getSupplementaryControlFiles() + (parentExten?.getSupplementaryControlFilesProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Link> getAllLinks() {
        return getLinks() + (parentExten?.getLinksProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Dependency> getAllDependencies() {
        return getDependencies() + (parentExten?.getDependenciesProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    def getAllPrefixes() {
        return (getPrefixes() + (parentExten?.getPrefixesProperty()?.getOrElse([]) ?: [])).unique()
    }

    @Input
    @Optional
    List<Dependency> getAllProvides() {
        return getProvides() + (parentExten?.getProvidesProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Dependency> getAllObsoletes() {
        return getObsoletes() + (parentExten?.getObsoletesProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Dependency> getAllConflicts() {
        return getConflicts() + (parentExten?.getConflictsProperty()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Directory> getAllDirectories() {
        return getDirectories() + (parentExten?.getDirectoriesProperty()?.getOrElse([]) ?: [])
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
        getObjectFactory().fileCollection().from('fake')
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
