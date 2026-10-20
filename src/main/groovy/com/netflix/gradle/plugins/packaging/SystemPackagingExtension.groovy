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
    private final SystemPackagingOptions options

    // Track when commands are added via File (Provider) - needed for validation
    private boolean hasPreInstallCommands = false
    private boolean hasPostInstallCommands = false
    private boolean hasPreUninstallCommands = false
    private boolean hasPostUninstallCommands = false

    @Inject
    SystemPackagingExtension(ObjectFactory objects, ProviderFactory providers) {
        this.objects = objects
        this.providers = providers
        this.options = new SystemPackagingOptions([
            packageName: packageName,
            release: release,
            version: version,
            user: user,
            permissionGroup: permissionGroup,
            packageGroup: packageGroup,
            buildHost: buildHost,
            summary: summary,
            packageDescription: packageDescription,
            license: license,
            packager: packager,
            distribution: distribution,
            vendor: vendor,
            url: url,
            sourcePackage: sourcePackage,
            archStr: archStr,
            maintainer: maintainer,
            uploaders: uploaders,
            priority: priority,
            epoch: epoch,
            uid: uid,
            gid: gid,
            setgid: setgid,
            createDirectoryEntry: createDirectoryEntry,
            addParentDirs: addParentDirs,
            signingKeyId: signingKeyId,
            signingKeyPassphrase: signingKeyPassphrase,
            signingKeyRingFile: signingKeyRingFile,
            preInstallFile: preInstallFile,
            postInstallFile: postInstallFile,
            preUninstallFile: preUninstallFile,
            postUninstallFile: postUninstallFile,
            triggerInstallFile: triggerInstallFile,
            triggerUninstallFile: triggerUninstallFile,
            triggerPostUninstallFile: triggerPostUninstallFile,
            fileType: fileType,
            os: os,
            type: type,
            multiArch: multiArch,
            prefixes: prefixes,
            configurationFiles: configurationFiles,
            preInstallCommands: preInstallCommands,
            postInstallCommands: postInstallCommands,
            preUninstallCommands: preUninstallCommands,
            postUninstallCommands: postUninstallCommands,
            triggerInstallCommands: triggerInstallCommands,
            triggerUninstallCommands: triggerUninstallCommands,
            triggerPostUninstallCommands: triggerPostUninstallCommands,
            preTransCommands: preTransCommands,
            postTransCommands: postTransCommands,
            commonCommands: commonCommands,
            supplementaryControlFiles: supplementaryControlFiles,
            links: links,
            dependencies: dependencies,
            obsoletes: obsoletes,
            conflicts: conflicts,
            recommends: recommends,
            suggests: suggests,
            enhances: enhances,
            preDepends: preDepends,
            breaks: breaks,
            replaces: replaces,
            provides: provides,
            directories: directories,
            customFields: customFields
        ])
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

    /** Provider-backed values; use this view from new plugin code. */
    @Internal
    SystemPackagingOptions getOptions() { options }

    // Plain 12.x accessors remain binary-compatible during the bridge release.
    @Deprecated
    String getPackageName() { packageName.getOrNull() }
    @Deprecated
    String getRelease() { release.getOrNull() }
    @Deprecated
    String getVersion() { version.getOrNull() }
    @Deprecated
    String getUser() { user.getOrNull() }
    @Deprecated
    String getPermissionGroup() { permissionGroup.getOrNull() }
    @Deprecated
    String getPackageGroup() { packageGroup.getOrNull() }
    @Deprecated
    String getBuildHost() { buildHost.getOrNull() }
    @Deprecated
    String getSummary() { summary.getOrNull() }
    @Deprecated
    String getPackageDescription() { packageDescription.getOrNull() }
    @Deprecated
    String getLicense() { license.getOrNull() }
    @Deprecated
    String getPackager() { packager.getOrNull() }
    @Deprecated
    String getDistribution() { distribution.getOrNull() }
    @Deprecated
    String getVendor() { vendor.getOrNull() }
    @Deprecated
    String getUrl() { url.getOrNull() }
    @Deprecated
    String getSourcePackage() { sourcePackage.getOrNull() }
    @Deprecated
    String getArchStr() { archStr.getOrNull() }
    @Deprecated
    String getMaintainer() { maintainer.getOrNull() }
    @Deprecated
    String getUploaders() { uploaders.getOrNull() }
    @Deprecated
    String getPriority() { priority.getOrNull() }
    @Deprecated
    Integer getEpoch() { epoch.getOrNull() }
    @Deprecated
    Integer getUid() { uid.getOrNull() }
    @Deprecated
    Integer getGid() { gid.getOrNull() }
    @Deprecated
    Boolean getSetgid() { setgid.getOrElse(false) }
    @Deprecated
    Boolean getCreateDirectoryEntry() { createDirectoryEntry.getOrNull() }
    @Deprecated
    Boolean getAddParentDirs() { addParentDirs.getOrNull() }
    @Deprecated
    String getSigningKeyId() { signingKeyId.getOrNull() }
    @Deprecated
    String getSigningKeyPassphrase() { signingKeyPassphrase.getOrNull() }
    @Deprecated
    File getSigningKeyRingFile() { signingKeyRingFile.getOrNull()?.asFile }
    @Deprecated
    File getPreInstallFile() { preInstallFile.getOrNull()?.asFile }
    @Deprecated
    File getPostInstallFile() { postInstallFile.getOrNull()?.asFile }
    @Deprecated
    File getPreUninstallFile() { preUninstallFile.getOrNull()?.asFile }
    @Deprecated
    File getPostUninstallFile() { postUninstallFile.getOrNull()?.asFile }
    @Deprecated
    File getTriggerInstallFile() { triggerInstallFile.getOrNull()?.asFile }
    @Deprecated
    File getTriggerUninstallFile() { triggerUninstallFile.getOrNull()?.asFile }
    @Deprecated
    File getTriggerPostUninstallFile() { triggerPostUninstallFile.getOrNull()?.asFile }
    @Deprecated
    Directive getFileType() { fileType.getOrNull() }
    @Deprecated
    Os getOs() { os.getOrNull() }
    @Deprecated
    RpmType getType() { type.getOrNull() }
    @Deprecated
    MultiArch getMultiArch() { multiArch.getOrNull() }
    @Deprecated
    List<String> getPrefixes() { new PropertyBackedList<>(prefixes) }
    @Deprecated
    List<String> getConfigurationFiles() { new PropertyBackedList<>(configurationFiles) }
    @Deprecated
    List<String> getPreInstallCommands() { new PropertyBackedList<>(preInstallCommands) }
    @Deprecated
    List<String> getPostInstallCommands() { new PropertyBackedList<>(postInstallCommands) }
    @Deprecated
    List<String> getPreUninstallCommands() { new PropertyBackedList<>(preUninstallCommands) }
    @Deprecated
    List<String> getPostUninstallCommands() { new PropertyBackedList<>(postUninstallCommands) }
    @Deprecated
    List<Trigger> getTriggerInstallCommands() { new PropertyBackedList<>(triggerInstallCommands) }
    @Deprecated
    List<Trigger> getTriggerUninstallCommands() { new PropertyBackedList<>(triggerUninstallCommands) }
    @Deprecated
    List<Trigger> getTriggerPostUninstallCommands() { new PropertyBackedList<>(triggerPostUninstallCommands) }
    @Deprecated
    List<String> getPreTransCommands() { new PropertyBackedList<>(preTransCommands) }
    @Deprecated
    List<String> getPostTransCommands() { new PropertyBackedList<>(postTransCommands) }
    @Deprecated
    List<String> getCommonCommands() { new PropertyBackedList<>(commonCommands) }
    @Deprecated
    List<String> getSupplementaryControlFiles() { new PropertyBackedList<>(supplementaryControlFiles) }
    @Deprecated
    List<Link> getLinks() { new PropertyBackedList<>(links) }
    @Deprecated
    List<Dependency> getDependencies() { new PropertyBackedList<>(dependencies) }
    @Deprecated
    List<Dependency> getObsoletes() { new PropertyBackedList<>(obsoletes) }
    @Deprecated
    List<Dependency> getConflicts() { new PropertyBackedList<>(conflicts) }
    @Deprecated
    List<Dependency> getRecommends() { new PropertyBackedList<>(recommends) }
    @Deprecated
    List<Dependency> getSuggests() { new PropertyBackedList<>(suggests) }
    @Deprecated
    List<Dependency> getEnhances() { new PropertyBackedList<>(enhances) }
    @Deprecated
    List<Dependency> getPreDepends() { new PropertyBackedList<>(preDepends) }
    @Deprecated
    List<Dependency> getBreaks() { new PropertyBackedList<>(breaks) }
    @Deprecated
    List<Dependency> getReplaces() { new PropertyBackedList<>(replaces) }
    @Deprecated
    List<Dependency> getProvides() { new PropertyBackedList<>(provides) }
    @Deprecated
    List<Directory> getDirectories() { new PropertyBackedList<>(directories) }
    @Deprecated
    Map<String, String> getCustomFields() { new PropertyBackedMap<>(customFields) }

    void setPackageName(String value) { packageName.set(value) }
    void setRelease(String value) { release.set(value) }
    void setVersion(String value) { version.set(value) }
    void setUser(String value) { user.set(value) }
    void setPermissionGroup(String value) { permissionGroup.set(value) }
    void setPackageGroup(String value) { packageGroup.set(value) }
    void setBuildHost(String value) { buildHost.set(value) }
    void setSummary(String value) { summary.set(value) }
    void setPackageDescription(String value) { packageDescription.set(value) }
    void setLicense(String value) { license.set(value) }
    void setPackager(String value) { packager.set(value) }
    void setDistribution(String value) { distribution.set(value) }
    void setVendor(String value) { vendor.set(value) }
    void setUrl(String value) { url.set(value) }
    void setSourcePackage(String value) { sourcePackage.set(value) }
    void setArchStr(String value) { archStr.set(value) }
    void setMaintainer(String value) { maintainer.set(value) }
    void setUploaders(String value) { uploaders.set(value) }
    void setPriority(String value) { priority.set(value) }
    void setEpoch(Integer value) { epoch.set(value) }
    void setUid(Integer value) { uid.set(value) }
    void setGid(Integer value) { gid.set(value) }
    void setSetgid(Boolean value) { setgid.set(value) }
    void setSetgid(boolean value) { setgid.set(value) }
    void setCreateDirectoryEntry(Boolean value) { createDirectoryEntry.set(value) }
    void setAddParentDirs(Boolean value) { addParentDirs.set(value) }
    void setSigningKeyId(String value) { signingKeyId.set(value) }
    void setSigningKeyPassphrase(String value) { signingKeyPassphrase.set(value) }
    void setSigningKeyRingFile(File value) { signingKeyRingFile.fileValue(value) }
    void setPreInstallFile(File value) { preInstallFile.fileValue(value) }
    void setPostInstallFile(File value) { postInstallFile.fileValue(value) }
    void setPreUninstallFile(File value) { preUninstallFile.fileValue(value) }
    void setPostUninstallFile(File value) { postUninstallFile.fileValue(value) }
    void setTriggerInstallFile(File value) { triggerInstallFile.fileValue(value) }
    void setTriggerUninstallFile(File value) { triggerUninstallFile.fileValue(value) }
    void setTriggerPostUninstallFile(File value) { triggerPostUninstallFile.fileValue(value) }
    void setFileType(Directive value) { fileType.set(value) }
    void setOs(Os value) { os.set(value) }
    void setType(RpmType value) { type.set(value) }
    void setMultiArch(MultiArch value) { multiArch.set(value) }
    void setPrefixes(List<String> value) { prefixes.set(value) }
    void setConfigurationFiles(List<String> value) { configurationFiles.set(value) }
    void setPreInstallCommands(List<String> value) { preInstallCommands.set(value) }
    void setPostInstallCommands(List<String> value) { postInstallCommands.set(value) }
    void setPreUninstallCommands(List<String> value) { preUninstallCommands.set(value) }
    void setPostUninstallCommands(List<String> value) { postUninstallCommands.set(value) }
    void setTriggerInstallCommands(List<Trigger> value) { triggerInstallCommands.set(value) }
    void setTriggerUninstallCommands(List<Trigger> value) { triggerUninstallCommands.set(value) }
    void setTriggerPostUninstallCommands(List<Trigger> value) { triggerPostUninstallCommands.set(value) }
    void setPreTransCommands(List<String> value) { preTransCommands.set(value) }
    void setPostTransCommands(List<String> value) { postTransCommands.set(value) }
    void setCommonCommands(List<String> value) { commonCommands.set(value) }
    void setSupplementaryControlFiles(List<String> value) { supplementaryControlFiles.set(value) }
    void setLinks(List<Link> value) { links.set(value) }
    void setDependencies(List<Dependency> value) { dependencies.set(value) }
    void setObsoletes(List<Dependency> value) { obsoletes.set(value) }
    void setConflicts(List<Dependency> value) { conflicts.set(value) }
    void setRecommends(List<Dependency> value) { recommends.set(value) }
    void setSuggests(List<Dependency> value) { suggests.set(value) }
    void setEnhances(List<Dependency> value) { enhances.set(value) }
    void setPreDepends(List<Dependency> value) { preDepends.set(value) }
    void setBreaks(List<Dependency> value) { breaks.set(value) }
    void setReplaces(List<Dependency> value) { replaces.set(value) }
    void setProvides(List<Dependency> value) { provides.set(value) }
    void setDirectories(List<Directory> value) { directories.set(value) }
    void setCustomFields(Map<String, String> value) { customFields.set(value) }

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

    def configurationFile(String path) {
        configurationFile((Object) path)
    }

    def configurationFile(Object path) {
        // Convert File to path String, keep String as-is
        configurationFiles.add(path instanceof File ? ((File)path).path : path.toString())
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
