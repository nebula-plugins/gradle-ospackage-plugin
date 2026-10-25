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

    @Input
    Map<String, Map<String, Object>> getCopySpecSettings() {
        CopySpecEnhancement.snapshotSettings(getRootSpec())
    }

    // Explicit delegation methods (replacing @Delegate)
    // These provide backward-compatible plain value access while using Property API internally

    @Input
    @Optional
    String getPackageName() { getExten().options.getPackageName().getOrNull() }
    void setPackageName(String value) { getExten().packageName(value) }
    void packageName(String value) { setPackageName(value) }

    @Input
    @Optional
    String getRelease() { getExten().options.getRelease().getOrNull() }
    void setRelease(String value) { getExten().release(value) }
    void release(String value) { setRelease(value) }

    @Input
    @Optional
    String getVersion() { getExten().options.getVersion().getOrNull() }
    void setVersion(String value) { getExten().version(value) }
    void version(String value) { setVersion(value) }

    @Input
    @Optional
    String getUser() { getExten().options.getUser().getOrNull() }
    void setUser(String value) { getExten().user(value) }
    void user(String value) { setUser(value) }

    @Input
    @Optional
    String getPermissionGroup() { getExten().options.getPermissionGroup().getOrNull() }
    void setPermissionGroup(String value) { getExten().permissionGroup(value) }
    void permissionGroup(String value) { setPermissionGroup(value) }

    @Input
    @Optional
    String getPackageGroup() { getExten().options.getPackageGroup().getOrNull() }
    void setPackageGroup(String value) { getExten().packageGroup(value) }
    void packageGroup(String value) { setPackageGroup(value) }

    @Input
    @Optional
    String getBuildHost() { getExten().options.getBuildHost().getOrNull() }
    void setBuildHost(String value) { getExten().buildHost(value) }
    void buildHost(String value) { setBuildHost(value) }

    @Input
    @Optional
    String getSummary() { getExten().options.getSummary().getOrNull() }
    void setSummary(String value) { getExten().summary(value) }
    void summary(String value) { setSummary(value) }

    @Input
    @Optional
    String getPackageDescription() { getExten().options.getPackageDescription().getOrNull() }
    void setPackageDescription(String value) { getExten().packageDescription(value) }
    void packageDescription(String value) { setPackageDescription(value) }

    @Input
    @Optional
    String getLicense() { getExten().options.getLicense().getOrNull() }
    void setLicense(String value) { getExten().license(value) }
    void license(String value) { setLicense(value) }

    @Input
    @Optional
    String getPackager() { getExten().options.getPackager().getOrNull() }
    void setPackager(String value) { getExten().packager(value) }
    void packager(String value) { setPackager(value) }

    @Input
    @Optional
    String getDistribution() { getExten().options.getDistribution().getOrNull() }
    void setDistribution(String value) { getExten().distribution(value) }
    void distribution(String value) { setDistribution(value) }

    @Input
    @Optional
    String getVendor() { getExten().options.getVendor().getOrNull() }
    void setVendor(String value) { getExten().vendor(value) }
    void vendor(String value) { setVendor(value) }

    @Input
    @Optional
    String getUrl() { getExten().options.getUrl().getOrNull() }
    void setUrl(String value) { getExten().url(value) }
    void url(String value) { setUrl(value) }

    @Input
    @Optional
    String getSourcePackage() { getExten().options.getSourcePackage().getOrNull() }
    void setSourcePackage(String value) { getExten().sourcePackage(value) }
    void sourcePackage(String value) { setSourcePackage(value) }

    @Input
    @Optional
    String getArchStr() { getExten().options.getArchStr().getOrNull() }
    void setArchStr(String value) { getExten().options.getArchStr().set(value) }

    void setArch(Object arch) {
        setArchStr((arch instanceof Architecture) ? arch.name() : arch.toString())
    }

    @Input
    @Optional
    String getMaintainer() { getExten().options.getMaintainer().getOrNull() }
    void setMaintainer(String value) { getExten().maintainer(value) }
    void maintainer(String value) { setMaintainer(value) }

    @Input
    @Optional
    String getUploaders() { getExten().options.getUploaders().getOrNull() }
    void setUploaders(String value) { getExten().uploaders(value) }
    void uploaders(String value) { setUploaders(value) }

    @Input
    @Optional
    String getPriority() { getExten().options.getPriority().getOrNull() }
    void setPriority(String value) { getExten().priority(value) }
    void priority(String value) { setPriority(value) }

    @Input
    @Optional
    Integer getEpoch() { getExten().options.getEpoch().getOrNull() }
    void setEpoch(Integer value) { getExten().epoch(value) }
    void epoch(Integer value) { setEpoch(value) }

    @Input
    @Optional
    Integer getUid() { getExten().options.getUid().getOrNull() }
    void setUid(Integer value) { getExten().uid(value) }
    void uid(Integer value) { setUid(value) }

    @Input
    @Optional
    Integer getGid() { getExten().options.getGid().getOrNull() }
    void setGid(Integer value) { getExten().gid(value) }
    void gid(Integer value) { setGid(value) }

    @Input
    @Optional
    Boolean getSetgid() { getExten().options.getSetgid().getOrNull() }
    void setSetgid(Boolean value) { getExten().options.setgid.set(value) }
    void setSetgid(boolean value) { getExten().options.setgid.set(value) }
    // Note: No convenience method due to naming collision with setter

    @Input
    @Optional
    Boolean getCreateDirectoryEntry() { getExten().options.getCreateDirectoryEntry().getOrNull() }
    void setCreateDirectoryEntry(Boolean value) { getExten().createDirectoryEntry(value) }
    void createDirectoryEntry(Boolean value) { setCreateDirectoryEntry(value) }

    @Input
    @Optional
    Boolean getAddParentDirs() { getExten().options.getAddParentDirs().getOrNull() }
    void setAddParentDirs(Boolean value) { getExten().addParentDirs(value) }
    void addParentDirs(Boolean value) { setAddParentDirs(value) }

    @Input
    @Optional
    String getSigningKeyId() { getExten().options.getSigningKeyId().getOrNull() }
    void setSigningKeyId(String value) { getExten().signingKeyId(value) }
    void signingKeyId(String value) { setSigningKeyId(value) }

    @Input
    @Optional
    String getSigningKeyPassphrase() { getExten().options.getSigningKeyPassphrase().getOrNull() }
    void setSigningKeyPassphrase(String value) { getExten().signingKeyPassphrase(value) }
    void signingKeyPassphrase(String value) { setSigningKeyPassphrase(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    File getSigningKeyRingFile() { getExten().options.getSigningKeyRingFile().getOrNull()?.asFile }
    void setSigningKeyRingFile(File value) { getExten().options.getSigningKeyRingFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPreInstallFile() { getExten().options.getPreInstallFile().getOrNull()?.asFile }
    void setPreInstallFile(File value) { getExten().options.getPreInstallFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPostInstallFile() { getExten().options.getPostInstallFile().getOrNull()?.asFile }
    void setPostInstallFile(File value) { getExten().options.getPostInstallFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPreUninstallFile() { getExten().options.getPreUninstallFile().getOrNull()?.asFile }
    void setPreUninstallFile(File value) { getExten().options.getPreUninstallFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPostUninstallFile() { getExten().options.getPostUninstallFile().getOrNull()?.asFile }
    void setPostUninstallFile(File value) { getExten().options.getPostUninstallFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getTriggerInstallFile() { getExten().options.getTriggerInstallFile().getOrNull()?.asFile }
    void setTriggerInstallFile(File value) { getExten().options.getTriggerInstallFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getTriggerUninstallFile() { getExten().options.getTriggerUninstallFile().getOrNull()?.asFile }
    void setTriggerUninstallFile(File value) { getExten().options.getTriggerUninstallFile().set(value) }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getTriggerPostUninstallFile() { getExten().options.getTriggerPostUninstallFile().getOrNull()?.asFile }
    void setTriggerPostUninstallFile(File value) { getExten().options.getTriggerPostUninstallFile().set(value) }

    @Input
    @Optional
    Directive getFileType() { getExten().options.getFileType().getOrNull() }
    void setFileType(Directive value) { getExten().options.fileType.set(value) }

    @Input
    @Optional
    Os getOs() { getExten().options.getOs().getOrNull() }
    void setOs(Os value) { getExten().options.os.set(value) }

    @Input
    @Optional
    RpmType getType() { getExten().options.getType().getOrNull() }
    void setType(RpmType value) { getExten().options.type.set(value) }

    @Input
    @Optional
    MultiArch getMultiArch() { getExten().options.getMultiArch().getOrNull() }
    void setMultiArch(MultiArch value) { getExten().options.multiArch.set(value) }

    // List properties - return List for backward compatibility
    @Input
    @Optional
    List<String> getPrefixes() { getExten().getPrefixes() }
    def prefix(String value) { getExten().prefix(value) }

    @Input
    @Optional
    List<Object> getConfigurationFiles() { getExten().getConfigurationFiles() }
    def configurationFile(String value) { getExten().configurationFile(value) }

    @Input
    @Optional
    List<Object> getPreInstallCommands() { getExten().getPreInstallCommands() }
    def preInstall(String value) { getExten().preInstall(value) }
    def preInstall(File value) { getExten().preInstall(value) }
    def preInstallFile(File value) { getExten().preInstallFile(value) }

    @Input
    @Optional
    List<Object> getPostInstallCommands() { getExten().getPostInstallCommands() }
    def postInstall(String value) { getExten().postInstall(value) }
    def postInstall(File value) { getExten().postInstall(value) }
    def postInstallFile(File value) { getExten().postInstallFile(value) }

    @Input
    @Optional
    List<Object> getPreUninstallCommands() { getExten().getPreUninstallCommands() }
    def preUninstall(String value) { getExten().preUninstall(value) }
    def preUninstall(File value) { getExten().preUninstall(value) }
    def preUninstallFile(File value) { getExten().preUninstallFile(value) }

    @Input
    @Optional
    List<Object> getPostUninstallCommands() { getExten().getPostUninstallCommands() }
    def postUninstall(String value) { getExten().postUninstall(value) }
    def postUninstall(File value) { getExten().postUninstall(value) }
    def postUninstallFile(File value) { getExten().postUninstallFile(value) }

    @Input
    @Optional
    List<Trigger> getTriggerInstallCommands() { getExten().getTriggerInstallCommands() }
    def triggerInstall(File script, String packageName, String version='', int flag=0) {
        getExten().triggerInstall(script, packageName, version, flag)
    }
    def triggerInstallFile(File value) { getExten().triggerInstallFile(value) }

    @Input
    @Optional
    List<Trigger> getTriggerUninstallCommands() { getExten().getTriggerUninstallCommands() }
    def triggerUninstall(File script, String packageName, String version='', int flag=0) {
        getExten().triggerUninstall(script, packageName, version, flag)
    }
    def triggerUninstallFile(File value) { getExten().triggerUninstallFile(value) }

    @Input
    @Optional
    List<Trigger> getTriggerPostUninstallCommands() { getExten().getTriggerPostUninstallCommands() }
    def triggerPostUninstall(File script, String packageName, String version='', int flag=0) {
        getExten().triggerPostUninstall(script, packageName, version, flag)
    }
    def triggerPostUninstallFile(File value) { getExten().triggerPostUninstallFile(value) }

    @Input
    @Optional
    List<Object> getPreTransCommands() { getExten().getPreTransCommands() }
    def preTrans(String value) { getExten().preTrans(value) }
    def preTrans(File value) { getExten().preTrans(value) }

    @Input
    @Optional
    List<Object> getPostTransCommands() { getExten().getPostTransCommands() }
    def postTrans(String value) { getExten().postTrans(value) }
    def postTrans(File value) { getExten().postTrans(value) }

    @Input
    @Optional
    List<Object> getCommonCommands() { getExten().getCommonCommands() }
    def installUtils(String value) { getExten().installUtils(value) }
    def installUtils(File value) { getExten().installUtils(value) }

    @Input
    @Optional
    List<Object> getSupplementaryControlFiles() { getExten().getSupplementaryControlFiles() }
    def supplementaryControl(Object value) { getExten().supplementaryControl(value) }

    @Input
    @Optional
    List<Link> getLinks() { getExten().getLinks() }
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
    List<Dependency> getDependencies() { getExten().getDependencies() }
    Dependency requires(String packageName) { getExten().requires(packageName) }
    Dependency requires(String packageName, String version) { getExten().requires(packageName, version) }
    Dependency requires(String packageName, String version, int flag) { getExten().requires(packageName, version, flag) }

    @Input
    @Optional
    List<Dependency> getObsoletes() { getExten().getObsoletes() }
    Dependency obsoletes(String packageName) { getExten().obsoletes(packageName) }
    Dependency obsoletes(String packageName, String version, int flag) { getExten().obsoletes(packageName, version, flag) }

    @Input
    @Optional
    List<Dependency> getConflicts() { getExten().getConflicts() }
    Dependency conflicts(String packageName) { getExten().conflicts(packageName) }
    Dependency conflicts(String packageName, String version, int flag) { getExten().conflicts(packageName, version, flag) }

    @Input
    @Optional
    List<Dependency> getRecommends() { getExten().getRecommends() }
    Dependency recommends(String packageName) { getExten().recommends(packageName) }
    Dependency recommends(String packageName, String version, int flag) { getExten().recommends(packageName, version, flag) }

    @Input
    @Optional
    List<Dependency> getSuggests() { getExten().getSuggests() }
    Dependency suggests(String packageName) { getExten().suggests(packageName) }
    Dependency suggests(String packageName, String version, int flag) { getExten().suggests(packageName, version, flag) }

    @Input
    @Optional
    List<Dependency> getEnhances() { getExten().getEnhances() }
    Dependency enhances(String packageName) { getExten().enhances(packageName) }
    Dependency enhances(String packageName, String version, int flag) { getExten().enhances(packageName, version, flag) }

    @Input
    @Optional
    List<Dependency> getPreDepends() { getExten().getPreDepends() }
    Dependency preDepends(String packageName) { getExten().preDepends(packageName) }
    Dependency preDepends(String packageName, String version, int flag) { getExten().preDepends(packageName, version, flag) }

    @Input
    @Optional
    List<Dependency> getBreaks() { getExten().getBreaks() }
    Dependency breaks(String packageName) { getExten().breaks(packageName) }
    Dependency breaks(String packageName, String version, int flag) { getExten().breaks(packageName, version, flag) }

    @Input
    @Optional
    List<Dependency> getReplaces() { getExten().getReplaces() }
    Dependency replaces(String packageName) { getExten().replaces(packageName) }
    Dependency replaces(String packageName, String version, int flag) { getExten().replaces(packageName, version, flag) }

    @Input
    @Optional
    List<Dependency> getProvides() { getExten().getProvides() }
    Dependency provides(String packageName) { getExten().provides(packageName) }
    Dependency provides(String packageName, String version) { getExten().provides(packageName, version) }
    Dependency provides(String packageName, String version, int flag) { getExten().provides(packageName, version, flag) }

    @Input
    @Optional
    List<Directory> getDirectories() { getExten().getDirectories() }
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

    // Entry points generated by @Delegate in 12.x. Keep their JVM signatures for
    // plugins compiled against that release, without retaining the old task field.
    def setInstallUtils(File value) { getExten().setInstallUtils(value) }
    def setConfigurationFile(String value) { getExten().setConfigurationFile(value) }
    def setPreInstall(File value) { getExten().setPreInstall(value) }
    def setPostInstall(File value) { getExten().setPostInstall(value) }
    def setPreUninstall(File value) { getExten().setPreUninstall(value) }
    def setPostUninstall(File value) { getExten().setPostUninstall(value) }
    def setTriggerInstall(File script, String packageName, String version='', int flag=0) {
        getExten().setTriggerInstall(script, packageName, version, flag)
    }
    def setTriggerUninstall(File script, String packageName, String version='', int flag=0) {
        getExten().setTriggerUninstall(script, packageName, version, flag)
    }
    def setTriggerPostUninstall(File script, String packageName, String version='', int flag=0) {
        getExten().setTriggerPostUninstall(script, packageName, version, flag)
    }
    def setPreTrans(File value) { getExten().setPreTrans(value) }
    def setPostTrans(File value) { getExten().setPostTrans(value) }

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
    void setCustomFields(Map value) { getExten().setCustomFields(value) }

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
        exten.options.packageName.convention(getArchiveBaseName())
        exten.options.release.convention(getArchiveClassifier())
        Provider<String> defaultVersion = parentExten ? parentExten.options.version.orElse(projectVersion) : projectVersion
        exten.options.version.convention(defaultVersion.map { sanitizeVersion(it) })
        exten.options.epoch.convention(0)
        exten.options.signingKeyId.convention('')
        exten.options.signingKeyPassphrase.convention('')
        exten.options.signingKeyRingFile.convention(
            projectLayout.file(getProviders().provider {
                File defaultFile = new File(System.getProperty('user.home'), '.gnupg/secring.gpg')
                defaultFile.exists() ? defaultFile : null
            })
        )
        exten.options.user.convention(getProviders().provider { getPackager() })
        exten.options.maintainer.convention(getProviders().provider { getPackager() })
        exten.options.uploaders.convention(getProviders().provider { getPackager() })
        exten.options.permissionGroup.convention('')
        exten.options.setgid.convention(false)
        exten.options.buildHost.convention(HOST_NAME)
        exten.options.summary.convention(exten.options.packageName)
        exten.options.packageDescription.convention(ProjectDefaultProviders.description(project))
        exten.options.license.convention('')
        exten.options.packager.convention(System.getProperty('user.name', ''))
        exten.options.distribution.convention('')
        exten.options.vendor.convention('')
        exten.options.url.convention('')
        exten.options.sourcePackage.convention('')
        exten.options.createDirectoryEntry.convention(false)
        exten.options.priority.convention('optional')

        // Keep parent defaults live even when supplied after this task is configured.
        if (parentExten) {
            exten.options.packageName.convention(parentExten.options.packageName.orElse(getArchiveBaseName()))
            exten.options.release.convention(parentExten.options.release.orElse(getArchiveClassifier()))
            exten.options.epoch.convention(parentExten.options.epoch.orElse(0))
            exten.options.signingKeyId.convention(parentExten.options.signingKeyId.orElse(''))
            exten.options.signingKeyPassphrase.convention(parentExten.options.signingKeyPassphrase.orElse(''))
            exten.options.signingKeyRingFile.convention(parentExten.options.signingKeyRingFile.orElse(
                projectLayout.file(getProviders().provider {
                    File defaultFile = new File(System.getProperty('user.home'), '.gnupg/secring.gpg')
                    defaultFile.exists() ? defaultFile : null
                })
            ))
            exten.options.user.convention(parentExten.options.user.orElse(getProviders().provider { getPackager() }))
            exten.options.maintainer.convention(parentExten.options.maintainer.orElse(getProviders().provider { getPackager() }))
            exten.options.uploaders.convention(parentExten.options.uploaders.orElse(getProviders().provider { getPackager() }))
            exten.options.permissionGroup.convention(parentExten.options.permissionGroup.orElse(''))
            exten.options.setgid.convention(parentExten.options.setgid.orElse(false))
            exten.options.packageGroup.convention(parentExten.options.packageGroup)
            exten.options.buildHost.convention(parentExten.options.buildHost.orElse(HOST_NAME))
            exten.options.summary.convention(parentExten.options.summary.orElse(exten.options.packageName))
            exten.options.packageDescription.convention(parentExten.options.packageDescription.orElse(ProjectDefaultProviders.description(project)))
            exten.options.license.convention(parentExten.options.license.orElse(''))
            exten.options.packager.convention(parentExten.options.packager.orElse(System.getProperty('user.name', '')))
            exten.options.distribution.convention(parentExten.options.distribution.orElse(''))
            exten.options.vendor.convention(parentExten.options.vendor.orElse(''))
            exten.options.url.convention(parentExten.options.url.orElse(''))
            exten.options.sourcePackage.convention(parentExten.options.sourcePackage.orElse(''))
            exten.options.createDirectoryEntry.convention(parentExten.options.createDirectoryEntry.orElse(false))
            exten.options.priority.convention(parentExten.options.priority.orElse('optional'))
            exten.options.preInstallFile.convention(parentExten.options.preInstallFile)
            exten.options.postInstallFile.convention(parentExten.options.postInstallFile)
            exten.options.preUninstallFile.convention(parentExten.options.preUninstallFile)
            exten.options.postUninstallFile.convention(parentExten.options.postUninstallFile)
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
        return exten.options.version
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
        return getConfigurationFiles() + (parentExten?.options?.getConfigurationFiles()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPreInstallCommands() {
        return getPreInstallCommands() + (parentExten?.options?.getPreInstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPostInstallCommands() {
        return getPostInstallCommands() + (parentExten?.options?.getPostInstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPreUninstallCommands() {
        return getPreUninstallCommands() + (parentExten?.options?.getPreUninstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPostUninstallCommands() {
        return getPostUninstallCommands() + (parentExten?.options?.getPostUninstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Trigger> getAllTriggerIn() {
        return getTriggerInstallCommands() + (parentExten?.options?.getTriggerInstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Trigger> getAllTriggerUn() {
        return getTriggerUninstallCommands() + (parentExten?.options?.getTriggerUninstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Trigger> getAllTriggerPostUn() {
        return getTriggerPostUninstallCommands() + (parentExten?.options?.getTriggerPostUninstallCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPreTransCommands() {
        return getPreTransCommands() + (parentExten?.options?.getPreTransCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPostTransCommands() {
        return getPostTransCommands() + (parentExten?.options?.getPostTransCommands()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Object> getAllCommonCommands() {
        return getCommonCommands() + (parentExten?.options?.getCommonCommands()?.getOrElse([]) ?: [])
    }

    /**
     * @return supplementary control files consisting of Strings (file paths)
     */
    @Input
    @Optional
    List<Object> getAllSupplementaryControlFiles() {
        return getSupplementaryControlFiles() + (parentExten?.options?.getSupplementaryControlFiles()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Link> getAllLinks() {
        return getLinks() + (parentExten?.options?.getLinks()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Dependency> getAllDependencies() {
        return getDependencies() + (parentExten?.options?.getDependencies()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    def getAllPrefixes() {
        return (getPrefixes() + (parentExten?.options?.getPrefixes()?.getOrElse([]) ?: [])).unique()
    }

    @Input
    @Optional
    List<Dependency> getAllProvides() {
        return getProvides() + (parentExten?.options?.getProvides()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Dependency> getAllObsoletes() {
        return getObsoletes() + (parentExten?.options?.getObsoletes()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Dependency> getAllConflicts() {
        return getConflicts() + (parentExten?.options?.getConflicts()?.getOrElse([]) ?: [])
    }

    @Input
    @Optional
    List<Directory> getAllDirectories() {
        return getDirectories() + (parentExten?.options?.getDirectories()?.getOrElse([]) ?: [])
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
