package com.netflix.gradle.plugins.packaging

import com.netflix.gradle.plugins.deb.control.MultiArch
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
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

    @Inject
    SystemPackagingExtension(ObjectFactory objects) {
        this.objects = objects
    }

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
    final ListProperty<Object> configurationFiles = objects.listProperty(Object)

    @Input
    @Optional
    final ListProperty<Object> preInstallCommands = objects.listProperty(Object)

    @Input
    @Optional
    final ListProperty<Object> postInstallCommands = objects.listProperty(Object)

    @Input
    @Optional
    final ListProperty<Object> preUninstallCommands = objects.listProperty(Object)

    @Input
    @Optional
    final ListProperty<Object> postUninstallCommands = objects.listProperty(Object)

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
    final ListProperty<Object> preTransCommands = objects.listProperty(Object)

    @Input
    @Optional
    final ListProperty<Object> postTransCommands = objects.listProperty(Object)

    @Input
    @Optional
    final ListProperty<Object> commonCommands = objects.listProperty(Object)

    @Input
    @Optional
    final ListProperty<Object> supplementaryControlFiles = objects.listProperty(Object)

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

    // Getter methods to maintain API compatibility
    Property<String> getPackageName() { packageName }
    Property<String> getRelease() { release }
    Property<String> getVersion() { version }
    Property<String> getUser() { user }
    Property<String> getPermissionGroup() { permissionGroup }
    Property<String> getPackageGroup() { packageGroup }
    Property<String> getBuildHost() { buildHost }
    Property<String> getSummary() { summary }
    Property<String> getPackageDescription() { packageDescription }
    Property<String> getLicense() { license }
    Property<String> getPackager() { packager }
    Property<String> getDistribution() { distribution }
    Property<String> getVendor() { vendor }
    Property<String> getUrl() { url }
    Property<String> getSourcePackage() { sourcePackage }
    Property<String> getArchStr() { archStr }
    Property<String> getMaintainer() { maintainer }
    Property<String> getUploaders() { uploaders }
    Property<String> getPriority() { priority }
    Property<Integer> getEpoch() { epoch }
    Property<Integer> getUid() { uid }
    Property<Integer> getGid() { gid }
    Property<Boolean> getSetgid() { setgid }
    Property<Boolean> getCreateDirectoryEntry() { createDirectoryEntry }
    Property<Boolean> getAddParentDirs() { addParentDirs }
    Property<String> getSigningKeyId() { signingKeyId }
    Property<String> getSigningKeyPassphrase() { signingKeyPassphrase }
    RegularFileProperty getSigningKeyRingFile() { signingKeyRingFile }
    RegularFileProperty getPreInstallFile() { preInstallFile }
    RegularFileProperty getPostInstallFile() { postInstallFile }
    RegularFileProperty getPreUninstallFile() { preUninstallFile }
    RegularFileProperty getPostUninstallFile() { postUninstallFile }
    RegularFileProperty getTriggerInstallFile() { triggerInstallFile }
    RegularFileProperty getTriggerUninstallFile() { triggerUninstallFile }
    RegularFileProperty getTriggerPostUninstallFile() { triggerPostUninstallFile }
    Property<Directive> getFileType() { fileType }
    Property<Os> getOs() { os }
    Property<RpmType> getType() { type }
    Property<MultiArch> getMultiArch() { multiArch }
    ListProperty<String> getPrefixes() { prefixes }
    ListProperty<Object> getConfigurationFiles() { configurationFiles }
    ListProperty<Object> getPreInstallCommands() { preInstallCommands }
    ListProperty<Object> getPostInstallCommands() { postInstallCommands }
    ListProperty<Object> getPreUninstallCommands() { preUninstallCommands }
    ListProperty<Object> getPostUninstallCommands() { postUninstallCommands }
    ListProperty<Trigger> getTriggerInstallCommands() { triggerInstallCommands }
    ListProperty<Trigger> getTriggerUninstallCommands() { triggerUninstallCommands }
    ListProperty<Trigger> getTriggerPostUninstallCommands() { triggerPostUninstallCommands }
    ListProperty<Object> getPreTransCommands() { preTransCommands }
    ListProperty<Object> getPostTransCommands() { postTransCommands }
    ListProperty<Object> getCommonCommands() { commonCommands }
    ListProperty<Object> getSupplementaryControlFiles() { supplementaryControlFiles }
    ListProperty<Link> getLinks() { links }
    ListProperty<Dependency> getDependencies() { dependencies }
    ListProperty<Dependency> getObsoletes() { obsoletes }
    ListProperty<Dependency> getConflicts() { conflicts }
    ListProperty<Dependency> getRecommends() { recommends }
    ListProperty<Dependency> getSuggests() { suggests }
    ListProperty<Dependency> getEnhances() { enhances }
    ListProperty<Dependency> getPreDepends() { preDepends }
    ListProperty<Dependency> getBreaks() { breaks }
    ListProperty<Dependency> getReplaces() { replaces }
    ListProperty<Dependency> getProvides() { provides }
    ListProperty<Directory> getDirectories() { directories }
    MapProperty<String, String> getCustomFields() { customFields }

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
        supplementaryControlFiles.add(file)
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
        commonCommands.add(script)
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
        preInstallCommands.add(script)
        return this
    }

    def preInstallFile(File path) {
        if(preInstallFile.isPresent()) { throw MULTIPLE_PREINSTALL_FILES }
        if(!preInstallCommands.get().isEmpty()) { throw PREINSTALL_COMMANDS_AND_FILE_DEFINED }
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
        postInstallCommands.add(script)
        return this
    }

    def postInstallFile(File path) {
        if(postInstallFile.isPresent()) { throw MULTIPLE_POSTINSTALL_FILES }
        if(!postInstallCommands.get().isEmpty()) { throw POSTINSTALL_COMMANDS_AND_FILE_DEFINED }
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
        preUninstallCommands.add(script)
        return this
    }

    def preUninstallFile(File script) {
        if(preUninstallFile.isPresent()) { throw MULTIPLE_PREUNINSTALL_FILES }
        if(!preUninstallCommands.get().isEmpty()) { throw PREUNINSTALL_COMMANDS_AND_FILE_DEFINED }
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
        postUninstallCommands.add(script)
        return this
    }

    def postUninstallFile(File script) {
        if(postUninstallFile.isPresent()) { throw MULTIPLE_POSTUNINSTALL_FILES }
        if(!postUninstallCommands.get().isEmpty()) { throw POSTUNINSTALL_COMMANDS_AND_FILE_DEFINED }
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
        preTransCommands.add(script)
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
        postTransCommands.add(script)
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
