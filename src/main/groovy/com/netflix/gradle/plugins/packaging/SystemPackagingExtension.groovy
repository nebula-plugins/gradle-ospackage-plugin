package com.netflix.gradle.plugins.packaging

import com.netflix.gradle.plugins.deb.control.MultiArch
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.redline_rpm.header.Architecture
import org.redline_rpm.header.Flags
import org.redline_rpm.header.Os
import org.redline_rpm.header.RpmType
import org.redline_rpm.payload.Directive

import javax.inject.Inject

/**
 * Extension that can be used to configure both DEB and RPM.
 *
 * Ideally we'd have multiple levels, e.g. an extension for everything, one for deb which extends the, one for rpm
 * that extends the base and have all the tasks take from the specific extension. But since tasks.withType can be used,
 * there isn't really a need for "all rpm" extension.
 */

class SystemPackagingExtension {
    static final IllegalStateException MULTIPLE_PREINSTALL_FILES = multipleFilesDefined('PreInstall')
    static final IllegalStateException MULTIPLE_POSTINSTALL_FILES = multipleFilesDefined('PostInstall')
    static final IllegalStateException MULTIPLE_PREUNINSTALL_FILES = multipleFilesDefined('PreUninstall')
    static final IllegalStateException MULTIPLE_POSTUNINSTALL_FILES = multipleFilesDefined('PostUninstall')
    static final IllegalStateException MULTIPLE_TRIGGERINSTALL_FILES = multipleFilesDefined('TriggerInstall')
    static final IllegalStateException MULTIPLE_TRIGGERUNINSTALL_FILES = multipleFilesDefined('TriggerUninstall')
    static final IllegalStateException MULTIPLE_TRIGGERPOSTUNINSTALL_FILES = multipleFilesDefined('TriggerPostUninstall')
    static final IllegalStateException PREINSTALL_COMMANDS_AND_FILE_DEFINED = conflictingDefinitions('PreInstall')
    static final IllegalStateException POSTINSTALL_COMMANDS_AND_FILE_DEFINED = conflictingDefinitions('PostInstall')
    static final IllegalStateException PREUNINSTALL_COMMANDS_AND_FILE_DEFINED = conflictingDefinitions('PreUninstall')
    static final IllegalStateException POSTUNINSTALL_COMMANDS_AND_FILE_DEFINED = conflictingDefinitions('PostUninstall')
    static final IllegalStateException TRIGGERINSTALL_COMMANDS_AND_FILE_DEFINED = conflictingDefinitions('TriggerInstall')
    static final IllegalStateException TRIGGERUNINSTALL_COMMANDS_AND_FILE_DEFINED = conflictingDefinitions('TriggerUninstall')
    static final IllegalStateException TRIGGERPOSTUNINSTALL_COMMANDS_AND_FILE_DEFINED = conflictingDefinitions('TriggerPostUninstall')

    private final ObjectFactory objects
    private final ProviderFactory providers

    // Track when commands are added via File (Provider) - needed for validation
    private boolean hasPreInstallCommands = false
    private boolean hasPostInstallCommands = false
    private boolean hasPreUninstallCommands = false
    private boolean hasPostUninstallCommands = false

    @Inject
    SystemPackagingExtension(ObjectFactory objects, ProviderFactory providers) {
        this.objects = objects
        this.providers = providers
    }

    @Internal
    ObjectFactory getObjects() {
        return objects
    }

    // Core String Properties
    @Input
    @Optional
    final Property<String> packageName = objects.property(String)

    @Input
    @Optional
    final Property<String> release = objects.property(String)

    @Input
    @Optional
    final Property<String> version = objects.property(String)

    @Input
    @Optional
    final Property<String> user = objects.property(String)

    @Input
    @Optional
    final Property<String> permissionGroup = objects.property(String)

    /**
     * In Debian, this is the Section and has to be provided. Valid values are: admin, cli-mono, comm, database, debug,
     * devel, doc, editors, education, electronics, embedded, fonts, games, gnome, gnu-r, gnustep, graphics, hamradio,
     * haskell, httpd, interpreters, introspection, java, kde, kernel, libdevel, libs, lisp, localization, mail, math,
     * metapackages, misc, net, news, ocaml, oldlibs, otherosfs, perl, php, python, ruby, science, shells, sound, tasks,
     * tex, text, utils, vcs, video, web, x11, xfce, zope. The section can be prefixed with contrib or non-free, if
     * not part of main.
     */
    @Input
    @Optional
    final Property<String> packageGroup = objects.property(String)

    @Input
    @Optional
    final Property<String> buildHost = objects.property(String)

    @Input
    @Optional
    final Property<String> summary = objects.property(String)

    @Input
    @Optional
    final Property<String> packageDescription = objects.property(String)

    @Input
    @Optional
    final Property<String> license = objects.property(String)

    @Input
    @Optional
    final Property<String> packager = objects.property(String)

    @Input
    @Optional
    final Property<String> distribution = objects.property(String)

    @Input
    @Optional
    final Property<String> vendor = objects.property(String)

    @Input
    @Optional
    final Property<String> url = objects.property(String)

    @Input
    @Optional
    final Property<String> sourcePackage = objects.property(String)

    @Input
    @Optional
    final Property<String> archStr = objects.property(String)

    @Input
    @Optional
    final Property<String> maintainer = objects.property(String)

    @Input
    @Optional
    final Property<String> uploaders = objects.property(String)

    @Input
    @Optional
    final Property<String> priority = objects.property(String)

    // Integer Properties
    @Input
    @Optional
    final Property<Integer> epoch = objects.property(Integer)

    @Input
    @Optional
    final Property<Integer> uid = objects.property(Integer)

    @Input
    @Optional
    final Property<Integer> gid = objects.property(Integer)

    // Boolean Properties
    @Input
    @Optional
    final Property<Boolean> setgid = objects.property(Boolean)

    @Input
    @Optional
    final Property<Boolean> createDirectoryEntry = objects.property(Boolean)

    @Input
    @Optional
    final Property<Boolean> addParentDirs = objects.property(Boolean)

    // Package Signing Properties
    @Input
    @Optional
    final Property<String> signingKeyId = objects.property(String)

    @Input
    @Optional
    final Property<String> signingKeyPassphrase = objects.property(String)

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    final RegularFileProperty signingKeyRingFile = objects.fileProperty()

    // Script Files
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    final RegularFileProperty preInstallFile = objects.fileProperty()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    final RegularFileProperty postInstallFile = objects.fileProperty()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    final RegularFileProperty preUninstallFile = objects.fileProperty()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    final RegularFileProperty postUninstallFile = objects.fileProperty()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    final RegularFileProperty triggerInstallFile = objects.fileProperty()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    final RegularFileProperty triggerUninstallFile = objects.fileProperty()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    final RegularFileProperty triggerPostUninstallFile = objects.fileProperty()

    // Enum Properties
    @Input
    @Optional
    final Property<Directive> fileType = objects.property(Directive)

    @Input
    @Optional
    final Property<Os> os = objects.property(Os)

    @Input
    @Optional
    final Property<RpmType> type = objects.property(RpmType)

    @Input
    @Optional
    final Property<MultiArch> multiArch = objects.property(MultiArch)

    // List Properties
    @Input
    @Optional
    final ListProperty<String> prefixes = objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> configurationFiles = objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> preInstallCommands = objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> postInstallCommands = objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> preUninstallCommands = objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> postUninstallCommands = objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<Trigger> triggerInstallCommands = objects.listProperty(Trigger)

    @Input
    @Optional
    final ListProperty<Trigger> triggerUninstallCommands = objects.listProperty(Trigger)

    @Input
    @Optional
    final ListProperty<Trigger> triggerPostUninstallCommands = objects.listProperty(Trigger)

    @Input
    @Optional
    final ListProperty<String> preTransCommands = objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> postTransCommands = objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> commonCommands = objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> supplementaryControlFiles = objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<Link> links = objects.listProperty(Link)

    @Input
    @Optional
    final ListProperty<Dependency> dependencies = objects.listProperty(Dependency)

    @Input
    @Optional
    final ListProperty<Dependency> obsoletes = objects.listProperty(Dependency)

    @Input
    @Optional
    final ListProperty<Dependency> conflicts = objects.listProperty(Dependency)

    @Input
    @Optional
    final ListProperty<Dependency> recommends = objects.listProperty(Dependency)

    @Input
    @Optional
    final ListProperty<Dependency> suggests = objects.listProperty(Dependency)

    @Input
    @Optional
    final ListProperty<Dependency> enhances = objects.listProperty(Dependency)

    @Input
    @Optional
    final ListProperty<Dependency> preDepends = objects.listProperty(Dependency)

    @Input
    @Optional
    final ListProperty<Dependency> breaks = objects.listProperty(Dependency)

    @Input
    @Optional
    final ListProperty<Dependency> replaces = objects.listProperty(Dependency)

    @Input
    @Optional
    final ListProperty<Dependency> provides = objects.listProperty(Dependency)

    @Input
    @Optional
    final ListProperty<Directory> directories = objects.listProperty(Directory)

    // Map Property
    @Input
    @Optional
    final MapProperty<String, String> customFields = objects.mapProperty(String, String)

    // Preserve the v12 getter signatures while exposing providers separately.
    // The provider fields remain the source of truth for task inputs and conventions.
    String getPackageName() { packageName.getOrNull() }
    void setPackageName(String value) { packageName.set(value) }
    @Internal
    Property<String> getPackageNameProperty() { packageName }
    String getRelease() { release.getOrNull() }
    void setRelease(String value) { release.set(value) }
    @Internal
    Property<String> getReleaseProperty() { release }
    String getVersion() { version.getOrNull() }
    void setVersion(String value) { version.set(value) }
    @Internal
    Property<String> getVersionProperty() { version }
    String getUser() { user.getOrNull() }
    void setUser(String value) { user.set(value) }
    @Internal
    Property<String> getUserProperty() { user }
    String getPermissionGroup() { permissionGroup.getOrNull() }
    void setPermissionGroup(String value) { permissionGroup.set(value) }
    @Internal
    Property<String> getPermissionGroupProperty() { permissionGroup }
    String getPackageGroup() { packageGroup.getOrNull() }
    void setPackageGroup(String value) { packageGroup.set(value) }
    @Internal
    Property<String> getPackageGroupProperty() { packageGroup }
    String getBuildHost() { buildHost.getOrNull() }
    void setBuildHost(String value) { buildHost.set(value) }
    @Internal
    Property<String> getBuildHostProperty() { buildHost }
    String getSummary() { summary.getOrNull() }
    void setSummary(String value) { summary.set(value) }
    @Internal
    Property<String> getSummaryProperty() { summary }
    String getPackageDescription() { packageDescription.getOrNull() }
    void setPackageDescription(String value) { packageDescription.set(value) }
    @Internal
    Property<String> getPackageDescriptionProperty() { packageDescription }
    String getLicense() { license.getOrNull() }
    void setLicense(String value) { license.set(value) }
    @Internal
    Property<String> getLicenseProperty() { license }
    String getPackager() { packager.getOrNull() }
    void setPackager(String value) { packager.set(value) }
    @Internal
    Property<String> getPackagerProperty() { packager }
    String getDistribution() { distribution.getOrNull() }
    void setDistribution(String value) { distribution.set(value) }
    @Internal
    Property<String> getDistributionProperty() { distribution }
    String getVendor() { vendor.getOrNull() }
    void setVendor(String value) { vendor.set(value) }
    @Internal
    Property<String> getVendorProperty() { vendor }
    String getUrl() { url.getOrNull() }
    void setUrl(String value) { url.set(value) }
    @Internal
    Property<String> getUrlProperty() { url }
    String getSourcePackage() { sourcePackage.getOrNull() }
    void setSourcePackage(String value) { sourcePackage.set(value) }
    @Internal
    Property<String> getSourcePackageProperty() { sourcePackage }
    String getArchStr() { archStr.getOrNull() }
    void setArchStr(String value) { archStr.set(value) }
    @Internal
    Property<String> getArchStrProperty() { archStr }
    String getMaintainer() { maintainer.getOrNull() }
    void setMaintainer(String value) { maintainer.set(value) }
    @Internal
    Property<String> getMaintainerProperty() { maintainer }
    String getUploaders() { uploaders.getOrNull() }
    void setUploaders(String value) { uploaders.set(value) }
    @Internal
    Property<String> getUploadersProperty() { uploaders }
    String getPriority() { priority.getOrNull() }
    void setPriority(String value) { priority.set(value) }
    @Internal
    Property<String> getPriorityProperty() { priority }
    String getSigningKeyId() { signingKeyId.getOrNull() }
    void setSigningKeyId(String value) { signingKeyId.set(value) }
    @Internal
    Property<String> getSigningKeyIdProperty() { signingKeyId }
    String getSigningKeyPassphrase() { signingKeyPassphrase.getOrNull() }
    void setSigningKeyPassphrase(String value) { signingKeyPassphrase.set(value) }
    @Internal
    Property<String> getSigningKeyPassphraseProperty() { signingKeyPassphrase }
    Integer getEpoch() { epoch.getOrNull() }
    void setEpoch(Integer value) { epoch.set(value) }
    @Internal
    Property<Integer> getEpochProperty() { epoch }
    Integer getUid() { uid.getOrNull() }
    void setUid(Integer value) { uid.set(value) }
    @Internal
    Property<Integer> getUidProperty() { uid }
    Integer getGid() { gid.getOrNull() }
    void setGid(Integer value) { gid.set(value) }
    @Internal
    Property<Integer> getGidProperty() { gid }
    Boolean getSetgid() { setgid.getOrNull() }
    void setSetgid(boolean value) { setgid.set(value) }
    @Internal
    Property<Boolean> getSetgidProperty() { setgid }
    Boolean getCreateDirectoryEntry() { createDirectoryEntry.getOrNull() }
    void setCreateDirectoryEntry(Boolean value) { createDirectoryEntry.set(value) }
    @Internal
    Property<Boolean> getCreateDirectoryEntryProperty() { createDirectoryEntry }
    Boolean getAddParentDirs() { addParentDirs.getOrNull() }
    void setAddParentDirs(Boolean value) { addParentDirs.set(value) }
    @Internal
    Property<Boolean> getAddParentDirsProperty() { addParentDirs }
    Directive getFileType() { fileType.getOrNull() }
    void setFileType(Directive value) { fileType.set(value) }
    @Internal
    Property<Directive> getFileTypeProperty() { fileType }
    Os getOs() { os.getOrNull() }
    void setOs(Os value) { os.set(value) }
    @Internal
    Property<Os> getOsProperty() { os }
    RpmType getType() { type.getOrNull() }
    void setType(RpmType value) { type.set(value) }
    @Internal
    Property<RpmType> getTypeProperty() { type }
    MultiArch getMultiArch() { multiArch.getOrNull() }
    void setMultiArch(MultiArch value) { multiArch.set(value) }
    @Internal
    Property<MultiArch> getMultiArchProperty() { multiArch }
    File getSigningKeyRingFile() { signingKeyRingFile.getOrNull()?.asFile }
    void setSigningKeyRingFile(File value) { signingKeyRingFile.set(value) }
    @Internal
    RegularFileProperty getSigningKeyRingFileProperty() { signingKeyRingFile }
    File getPreInstallFile() { preInstallFile.getOrNull()?.asFile }
    void setPreInstallFile(File value) { preInstallFile.set(value) }
    @Internal
    RegularFileProperty getPreInstallFileProperty() { preInstallFile }
    File getPostInstallFile() { postInstallFile.getOrNull()?.asFile }
    void setPostInstallFile(File value) { postInstallFile.set(value) }
    @Internal
    RegularFileProperty getPostInstallFileProperty() { postInstallFile }
    File getPreUninstallFile() { preUninstallFile.getOrNull()?.asFile }
    void setPreUninstallFile(File value) { preUninstallFile.set(value) }
    @Internal
    RegularFileProperty getPreUninstallFileProperty() { preUninstallFile }
    File getPostUninstallFile() { postUninstallFile.getOrNull()?.asFile }
    void setPostUninstallFile(File value) { postUninstallFile.set(value) }
    @Internal
    RegularFileProperty getPostUninstallFileProperty() { postUninstallFile }
    File getTriggerInstallFile() { triggerInstallFile.getOrNull()?.asFile }
    void setTriggerInstallFile(File value) { triggerInstallFile.set(value) }
    @Internal
    RegularFileProperty getTriggerInstallFileProperty() { triggerInstallFile }
    File getTriggerUninstallFile() { triggerUninstallFile.getOrNull()?.asFile }
    void setTriggerUninstallFile(File value) { triggerUninstallFile.set(value) }
    @Internal
    RegularFileProperty getTriggerUninstallFileProperty() { triggerUninstallFile }
    File getTriggerPostUninstallFile() { triggerPostUninstallFile.getOrNull()?.asFile }
    void setTriggerPostUninstallFile(File value) { triggerPostUninstallFile.set(value) }
    @Internal
    RegularFileProperty getTriggerPostUninstallFileProperty() { triggerPostUninstallFile }
    List<String> getPrefixes() { new MutableListPropertyView<String>(prefixes) }
    void setPrefixes(List<String> value) { prefixes.set(value) }
    @Internal
    ListProperty<String> getPrefixesProperty() { prefixes }
    List<String> getConfigurationFiles() { new MutableListPropertyView<String>(configurationFiles) }
    void setConfigurationFiles(List<String> value) { configurationFiles.set(value) }
    @Internal
    ListProperty<String> getConfigurationFilesProperty() { configurationFiles }
    List<String> getPreInstallCommands() { new MutableListPropertyView<String>(preInstallCommands) }
    void setPreInstallCommands(List<String> value) { preInstallCommands.set(value) }
    @Internal
    ListProperty<String> getPreInstallCommandsProperty() { preInstallCommands }
    List<String> getPostInstallCommands() { new MutableListPropertyView<String>(postInstallCommands) }
    void setPostInstallCommands(List<String> value) { postInstallCommands.set(value) }
    @Internal
    ListProperty<String> getPostInstallCommandsProperty() { postInstallCommands }
    List<String> getPreUninstallCommands() { new MutableListPropertyView<String>(preUninstallCommands) }
    void setPreUninstallCommands(List<String> value) { preUninstallCommands.set(value) }
    @Internal
    ListProperty<String> getPreUninstallCommandsProperty() { preUninstallCommands }
    List<String> getPostUninstallCommands() { new MutableListPropertyView<String>(postUninstallCommands) }
    void setPostUninstallCommands(List<String> value) { postUninstallCommands.set(value) }
    @Internal
    ListProperty<String> getPostUninstallCommandsProperty() { postUninstallCommands }
    List<String> getPreTransCommands() { new MutableListPropertyView<String>(preTransCommands) }
    void setPreTransCommands(List<String> value) { preTransCommands.set(value) }
    @Internal
    ListProperty<String> getPreTransCommandsProperty() { preTransCommands }
    List<String> getPostTransCommands() { new MutableListPropertyView<String>(postTransCommands) }
    void setPostTransCommands(List<String> value) { postTransCommands.set(value) }
    @Internal
    ListProperty<String> getPostTransCommandsProperty() { postTransCommands }
    List<String> getCommonCommands() { new MutableListPropertyView<String>(commonCommands) }
    void setCommonCommands(List<String> value) { commonCommands.set(value) }
    @Internal
    ListProperty<String> getCommonCommandsProperty() { commonCommands }
    List<String> getSupplementaryControlFiles() { new MutableListPropertyView<String>(supplementaryControlFiles) }
    void setSupplementaryControlFiles(List<String> value) { supplementaryControlFiles.set(value) }
    @Internal
    ListProperty<String> getSupplementaryControlFilesProperty() { supplementaryControlFiles }
    List<Trigger> getTriggerInstallCommands() { new MutableListPropertyView<Trigger>(triggerInstallCommands) }
    void setTriggerInstallCommands(List<Trigger> value) { triggerInstallCommands.set(value) }
    @Internal
    ListProperty<Trigger> getTriggerInstallCommandsProperty() { triggerInstallCommands }
    List<Trigger> getTriggerUninstallCommands() { new MutableListPropertyView<Trigger>(triggerUninstallCommands) }
    void setTriggerUninstallCommands(List<Trigger> value) { triggerUninstallCommands.set(value) }
    @Internal
    ListProperty<Trigger> getTriggerUninstallCommandsProperty() { triggerUninstallCommands }
    List<Trigger> getTriggerPostUninstallCommands() { new MutableListPropertyView<Trigger>(triggerPostUninstallCommands) }
    void setTriggerPostUninstallCommands(List<Trigger> value) { triggerPostUninstallCommands.set(value) }
    @Internal
    ListProperty<Trigger> getTriggerPostUninstallCommandsProperty() { triggerPostUninstallCommands }
    List<Link> getLinks() { new MutableListPropertyView<Link>(links) }
    void setLinks(List<Link> value) { links.set(value) }
    @Internal
    ListProperty<Link> getLinksProperty() { links }
    List<Dependency> getDependencies() { new MutableListPropertyView<Dependency>(dependencies) }
    void setDependencies(List<Dependency> value) { dependencies.set(value) }
    @Internal
    ListProperty<Dependency> getDependenciesProperty() { dependencies }
    List<Dependency> getObsoletes() { new MutableListPropertyView<Dependency>(obsoletes) }
    void setObsoletes(List<Dependency> value) { obsoletes.set(value) }
    @Internal
    ListProperty<Dependency> getObsoletesProperty() { obsoletes }
    List<Dependency> getConflicts() { new MutableListPropertyView<Dependency>(conflicts) }
    void setConflicts(List<Dependency> value) { conflicts.set(value) }
    @Internal
    ListProperty<Dependency> getConflictsProperty() { conflicts }
    List<Dependency> getRecommends() { new MutableListPropertyView<Dependency>(recommends) }
    void setRecommends(List<Dependency> value) { recommends.set(value) }
    @Internal
    ListProperty<Dependency> getRecommendsProperty() { recommends }
    List<Dependency> getSuggests() { new MutableListPropertyView<Dependency>(suggests) }
    void setSuggests(List<Dependency> value) { suggests.set(value) }
    @Internal
    ListProperty<Dependency> getSuggestsProperty() { suggests }
    List<Dependency> getEnhances() { new MutableListPropertyView<Dependency>(enhances) }
    void setEnhances(List<Dependency> value) { enhances.set(value) }
    @Internal
    ListProperty<Dependency> getEnhancesProperty() { enhances }
    List<Dependency> getPreDepends() { new MutableListPropertyView<Dependency>(preDepends) }
    void setPreDepends(List<Dependency> value) { preDepends.set(value) }
    @Internal
    ListProperty<Dependency> getPreDependsProperty() { preDepends }
    List<Dependency> getBreaks() { new MutableListPropertyView<Dependency>(breaks) }
    void setBreaks(List<Dependency> value) { breaks.set(value) }
    @Internal
    ListProperty<Dependency> getBreaksProperty() { breaks }
    List<Dependency> getReplaces() { new MutableListPropertyView<Dependency>(replaces) }
    void setReplaces(List<Dependency> value) { replaces.set(value) }
    @Internal
    ListProperty<Dependency> getReplacesProperty() { replaces }
    List<Dependency> getProvides() { new MutableListPropertyView<Dependency>(provides) }
    void setProvides(List<Dependency> value) { provides.set(value) }
    @Internal
    ListProperty<Dependency> getProvidesProperty() { provides }
    List<Directory> getDirectories() { new MutableListPropertyView<Directory>(directories) }
    void setDirectories(List<Directory> value) { directories.set(value) }
    @Internal
    ListProperty<Directory> getDirectoriesProperty() { directories }
    Map<String, String> getCustomFields() { new MutableMapPropertyView<String, String>(customFields) }
    void setCustomFields(Map<String, String> value) { customFields.set(value) }
    @Internal
    MapProperty<String, String> getCustomFieldsProperty() { customFields }
    // Convenience methods for backward compatibility (v13.0 recommended API design)

    void packageName(String value) { packageName.set(value) }
    void release(String value) { release.set(value) }
    void version(String value) { version.set(value) }
    void user(String value) { user.set(value) }
    void permissionGroup(String value) { permissionGroup.set(value) }
    void packageGroup(String value) { packageGroup.set(value) }
    void buildHost(String value) { buildHost.set(value) }
    void summary(String value) { summary.set(value) }
    void packageDescription(String value) { packageDescription.set(value) }
    void license(String value) { license.set(value) }
    void packager(String value) { packager.set(value) }
    void distribution(String value) { distribution.set(value) }
    void vendor(String value) { vendor.set(value) }
    void url(String value) { url.set(value) }
    void sourcePackage(String value) { sourcePackage.set(value) }
    void maintainer(String value) { maintainer.set(value) }
    void uploaders(String value) { uploaders.set(value) }
    void priority(String value) { priority.set(value) }
    void epoch(Integer value) { epoch.set(value) }
    void uid(Integer value) { uid.set(value) }
    void gid(Integer value) { gid.set(value) }
    void setgid(Boolean value) { setgid.set(value) }
    void createDirectoryEntry(Boolean value) { createDirectoryEntry.set(value) }
    void addParentDirs(Boolean value) { addParentDirs.set(value) }
    void signingKeyId(String value) { signingKeyId.set(value) }
    void signingKeyPassphrase(String value) { signingKeyPassphrase.set(value) }
    void fileType(Object value) { fileType.set(value as Directive) }
    void os(Object value) { os.set(value as Os) }
    void type(Object value) { type.set(value as RpmType) }
    void multiArch(Object value) { multiArch.set(value as MultiArch) }

    void setArch(Object arch) {
        archStr.set((arch instanceof Architecture) ? arch.name() : arch.toString())
    }

    def prefix(String prefixStr) {
        prefixes.add(prefixStr)
        return this
    }

    def supplementaryControl(Object file) {
        // Convert File to path String, keep String as-is
        supplementaryControlFiles.add(file instanceof File ? ((File)file).path : file.toString())
        return this
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setInstallUtils(File script) {
        installUtils(script)
    }

    def installUtils(String script) {
        commonCommands.add(script)
        return this
    }

    def installUtils(File script) {
        def fileProperty = objects.fileProperty()
        fileProperty.set(script)
        def contentProvider = providers.fileContents(fileProperty).asText.orElse('')
        commonCommands.addAll(contentProvider.map { content -> content ? [content] : [] })
        return this
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setConfigurationFile(String script) {
        configurationFile(script)
    }

    def configurationFile(Object path) {
        // Convert File to path String, keep String as-is
        configurationFiles.add(path instanceof File ? ((File)path).path : path.toString())
        return this
    }

    def configurationFile(String path) {
        configurationFiles.add(path)
        return this
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setPreInstall(File script) {
        preInstall(script)
    }

    def preInstall(String script) {
        if(preInstallFile.isPresent()) { throw PREINSTALL_COMMANDS_AND_FILE_DEFINED }
        preInstallCommands.add(script)
        return this
    }

    def preInstall(File script) {
        if(preInstallFile.isPresent()) { throw PREINSTALL_COMMANDS_AND_FILE_DEFINED }
        // Use providers.fileContents() for config-cache-safe file reading at execution time
        // This preserves the original behavior: file read at execution, not configuration
        def fileProperty = objects.fileProperty()
        fileProperty.set(script)
        def contentProvider = providers.fileContents(fileProperty).asText.orElse('')
        preInstallCommands.addAll(contentProvider.map { content -> content ? [content] : [] })
        hasPreInstallCommands = true
        return this
    }

    def preInstallFile(File path) {
        if(preInstallFile.isPresent()) { throw MULTIPLE_PREINSTALL_FILES }
        if(hasPreInstallCommands || !preInstallCommands.getOrElse([]).isEmpty()) { throw PREINSTALL_COMMANDS_AND_FILE_DEFINED }
        preInstallFile.set(path)
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setPostInstall(File script) {
        postInstall(script)
    }

    def postInstall(String script) {
        if(postInstallFile.isPresent()) { throw POSTINSTALL_COMMANDS_AND_FILE_DEFINED }
        postInstallCommands.add(script)
        return this
    }

    def postInstall(File script) {
        if(postInstallFile.isPresent()) { throw POSTINSTALL_COMMANDS_AND_FILE_DEFINED }
        def fileProperty = objects.fileProperty()
        fileProperty.set(script)
        def contentProvider = providers.fileContents(fileProperty).asText.orElse('')
        postInstallCommands.addAll(contentProvider.map { content -> content ? [content] : [] })
        hasPostInstallCommands = true
        return this
    }

    def postInstallFile(File path) {
        if(postInstallFile.isPresent()) { throw MULTIPLE_POSTINSTALL_FILES }
        if(hasPostInstallCommands || !postInstallCommands.getOrElse([]).isEmpty()) { throw POSTINSTALL_COMMANDS_AND_FILE_DEFINED }
        postInstallFile.set(path)
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setPreUninstall(File script) {
        preUninstall(script)
    }

    def preUninstall(String script) {
        if(preUninstallFile.isPresent()) { throw PREUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        preUninstallCommands.add(script)
        return this
    }

    def preUninstall(File script) {
        if(preUninstallFile.isPresent()) { throw PREUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        def fileProperty = objects.fileProperty()
        fileProperty.set(script)
        def contentProvider = providers.fileContents(fileProperty).asText.orElse('')
        preUninstallCommands.addAll(contentProvider.map { content -> content ? [content] : [] })
        hasPreUninstallCommands = true
        return this
    }

    def preUninstallFile(File script) {
        if(preUninstallFile.isPresent()) { throw MULTIPLE_PREUNINSTALL_FILES }
        if(hasPreUninstallCommands || !preUninstallCommands.getOrElse([]).isEmpty()) { throw PREUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        preUninstallFile.set(script)
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setPostUninstall(File script) {
        postUninstall(script)
    }

    def postUninstall(String script) {
        if(postUninstallFile.isPresent()) { throw POSTUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        postUninstallCommands.add(script)
        return this
    }

    def postUninstall(File script) {
        if(postUninstallFile.isPresent()) { throw POSTUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        def fileProperty = objects.fileProperty()
        fileProperty.set(script)
        def contentProvider = providers.fileContents(fileProperty).asText.orElse('')
        postUninstallCommands.addAll(contentProvider.map { content -> content ? [content] : [] })
        hasPostUninstallCommands = true
        return this
    }

    def postUninstallFile(File script) {
        if(postUninstallFile.isPresent()) { throw MULTIPLE_POSTUNINSTALL_FILES }
        if(hasPostUninstallCommands || !postUninstallCommands.getOrElse([]).isEmpty()) { throw POSTUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        postUninstallFile.set(script)
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setTriggerInstall(File script, String packageName, String version='', int flag=0) {
        triggerInstall(script, packageName, version, flag)
    }

    def triggerInstall(File script, String packageName, String version='', int flag=0) {
        if(triggerInstallFile.isPresent()) { throw TRIGGERINSTALL_COMMANDS_AND_FILE_DEFINED }
        triggerInstallCommands.add(new Trigger(new Dependency(packageName, version, flag), script))
        return this
    }

    def triggerInstallFile(File script) {
        if(triggerInstallFile.isPresent()) { throw MULTIPLE_TRIGGERINSTALL_FILES }
        if(!triggerInstallCommands.get().isEmpty()) { throw TRIGGERINSTALL_COMMANDS_AND_FILE_DEFINED }
        triggerInstallFile.set(script)
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setTriggerUninstall(File script, String packageName, String version='', int flag=0) {
        triggerUninstall(script, packageName, version, flag)
    }

    def triggerUninstall(File script, String packageName, String version='', int flag=0) {
        if(triggerUninstallFile.isPresent()) { throw TRIGGERUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        triggerUninstallCommands.add(new Trigger(new Dependency(packageName, version, flag), script))
        return this
    }

    def triggerUninstallFile(File script) {
        if(triggerUninstallFile.isPresent()) { throw MULTIPLE_TRIGGERUNINSTALL_FILES }
        if(!triggerUninstallCommands.get().isEmpty()) { throw TRIGGERUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        triggerUninstallFile.set(script)
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setTriggerPostUninstall(File script, String packageName, String version='', int flag=0) {
        triggerPostUninstall(script, packageName, version, flag)
    }

    def triggerPostUninstall(File script, String packageName, String version='', int flag=0) {
        if(triggerPostUninstallFile.isPresent()) { throw TRIGGERPOSTUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        triggerPostUninstallCommands.add(new Trigger(new Dependency(packageName, version, flag), script))
        return this
    }

    def triggerPostUninstallFile(File script) {
        if(triggerPostUninstallFile.isPresent()) { throw MULTIPLE_TRIGGERPOSTUNINSTALL_FILES }
        if(!triggerUninstallCommands.get().isEmpty()) { throw TRIGGERPOSTUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        triggerPostUninstallFile.set(script)
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setPreTrans(File script) {
        preTrans(script)
    }

    def preTrans(String script) {
        preTransCommands.add(script)
        return this
    }

    def preTrans(File script) {
        def fileProperty = objects.fileProperty()
        fileProperty.set(script)
        def contentProvider = providers.fileContents(fileProperty).asText.orElse('')
        preTransCommands.addAll(contentProvider.map { content -> content ? [content] : [] })
        return this
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setPostTrans(File script) {
        postTrans(script)
    }

    def postTrans(String script) {
        postTransCommands.add(script)
        return this
    }

    def postTrans(File script) {
        def fileProperty = objects.fileProperty()
        fileProperty.set(script)
        def contentProvider = providers.fileContents(fileProperty).asText.orElse('')
        postTransCommands.addAll(contentProvider.map { content -> content ? [content] : [] })
        return this
    }

    // @groovy.transform.PackageScope doesn't seem to set the proper scope when going through a @Delegate

    Link link(String path, String target) {
        link(path, target, -1, null, null)
    }

    Link link(String path, String target, int permissions) {
        link(path, target, permissions, null, null)
    }

    Link link(String path, String target, String user, String permissionGroup) {
        link(path, target, -1, user, permissionGroup)
    }

    Link link(String path, String target, int permissions, String user, String permissionGroup) {
        Link link = new Link()
        link.path = path
        link.target = target
        link.permissions = permissions
        link.user = user
        link.permissionGroup = permissionGroup
        links.add(link)
        link
    }

    Dependency requires(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        dependencies.add(dep)
        dep
    }

    Dependency requires(String packageName, String version){
        requires(packageName, version, 0)
    }

    Dependency requires(String packageName) {
        requires(packageName, '', 0)
    }

    Dependency obsoletes(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        obsoletes.add(dep)
        dep
    }

    Dependency obsoletes(String packageName) {
        obsoletes(packageName, '', 0)
    }

    Dependency conflicts(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        conflicts.add(dep)
        dep
    }

    Dependency conflicts(String packageName) {
        conflicts(packageName, '', 0)
    }

    Dependency recommends(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        recommends.add(dep)
        dep
    }

    Dependency recommends(String packageName) {
        recommends(packageName, '', 0)
    }

    Dependency suggests(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        suggests.add(dep)
        dep
    }

    Dependency suggests(String packageName) {
        suggests(packageName, '', 0)
    }

    Dependency enhances(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        enhances.add(dep)
        dep
    }

    Dependency enhances(String packageName) {
        enhances(packageName, '', 0)
    }

    Dependency preDepends(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        preDepends.add(dep)
        dep
    }

    Dependency preDepends(String packageName) {
        preDepends(packageName, '', 0)
    }

    Dependency breaks(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        breaks.add(dep)
        dep
    }

    Dependency breaks(String packageName) {
        breaks(packageName, '', 0)
    }

    Dependency replaces(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        replaces.add(dep)
        dep
    }

    Dependency replaces(String packageName) {
        replaces(packageName, '', 0)
    }

    Dependency provides(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        provides.add(dep)
        dep
    }

    Dependency provides(String packageName, String version) {
        provides(packageName, version, Flags.EQUAL)
    }

    Dependency provides(String packageName) {
        provides(packageName, '', 0)
    }

    Directory directory(String path) {
        Directory directory = directory(path, -1)
        directories.add(directory)
        directory
    }

    Directory directory(String path, boolean addParents) {
        Directory directory = new Directory(path: path, addParents: addParents)
        directories.add(directory)
        directory
    }

    Directory directory(String path, int permissions) {
        Directory directory = new Directory(path: path, permissions: permissions)
        directories.add(directory)
        directory
    }

    Directory directory(String path, int permissions, boolean addParents) {
        Directory directory = new Directory(path: path, permissions: permissions, addParents: addParents)
        directories.add(directory)
        directory
    }

    Directory directory(String path, int permissions, String user, String permissionGroup) {
        Directory directory = new Directory(path: path, permissions: permissions, user: user, permissionGroup: permissionGroup)
        directories.add(directory)
        directory
    }

    Directory directory(String path, int permissions, String user, String permissionGroup, boolean addParents) {
        Directory directory = new Directory(path: path, permissions: permissions, user: user, permissionGroup: permissionGroup, addParents: addParents)
        directories.add(directory)
        directory
    }

    def customField(String key, String val) {
        customFields.put(key, val)
        return this
    }

    def customField(Map<String, String> fields) {
        customFields.putAll(fields)
        return this
    }

    private static IllegalStateException multipleFilesDefined(String fileName) {
        new IllegalStateException("Cannot specify more than one $fileName File")
    }

    private static IllegalStateException conflictingDefinitions(String type) {
        new IllegalStateException("Cannot specify $type File and $type Commands")
    }
}
