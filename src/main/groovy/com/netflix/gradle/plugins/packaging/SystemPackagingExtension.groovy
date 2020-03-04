package com.netflix.gradle.plugins.packaging

import com.netflix.gradle.plugins.deb.control.MultiArch
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
    static final IllegalStateException PREINSTALL_COMMANDS_AND_FILE_DEFINED = conflictingDefinitions('PreInstall')
    static final IllegalStateException POSTINSTALL_COMMANDS_AND_FILE_DEFINED = conflictingDefinitions('PostInstall')
    static final IllegalStateException PREUNINSTALL_COMMANDS_AND_FILE_DEFINED = conflictingDefinitions('PreUninstall')
    static final IllegalStateException POSTUNINSTALL_COMMANDS_AND_FILE_DEFINED = conflictingDefinitions('PostUninstall')

    // File name components
    String packageName
    String release
    String version
    Integer epoch
    // Package signing data
    String signingKeyId
    String signingKeyPassphrase
    File signingKeyRingFile
    String user
    String permissionGroup // Group is used by Gradle on tasks.
    /**
     * In Debian, this is the Section and has to be provided. Valid values are: admin, cli-mono, comm, database, debug,
     * devel, doc, editors, education, electronics, embedded, fonts, games, gnome, gnu-r, gnustep, graphics, hamradio,
     * haskell, httpd, interpreters, introspection, java, kde, kernel, libdevel, libs, lisp, localization, mail, math,
     * metapackages, misc, net, news, ocaml, oldlibs, otherosfs, perl, php, python, ruby, science, shells, sound, tasks,
     * tex, text, utils, vcs, video, web, x11, xfce, zope. The section can be prefixed with contrib or non-free, if
     * not part of main.
     */
    String packageGroup
    String buildHost
    String summary
    String packageDescription
    String license
    String packager
    String distribution
    String vendor
    String url
    String sourcePackage
    // For Backward compatibility for those that passed in a Architecture object
    String archStr // This is what can be convention mapped and then referenced
    Directive fileType
    Boolean createDirectoryEntry
    Boolean addParentDirs
    Os os
    RpmType type
    List<String> prefixes = new ArrayList<String>()
    // DEB Only
    Integer uid
    Integer gid
    MultiArch multiArch
    String maintainer
    String uploaders
    String priority
    File preInstallFile
    File postInstallFile
    File preUninstallFile
    File postUninstallFile

    final List<Object> configurationFiles = []
    final List<Object> preInstallCommands = []
    final List<Object> postInstallCommands = []
    final List<Object> preUninstallCommands = []
    final List<Object> postUninstallCommands = []

    // RPM specific
    final List<Object> preTransCommands = []
    final List<Object> postTransCommands = []
    final List<Object> commonCommands = []

    /**
     * Can be of type String or File
     */
    final List<Object> supplementaryControlFiles = []

    List<Link> links = new ArrayList<Link>()
    List<Dependency> dependencies = new ArrayList<Dependency>()
    List<Dependency> obsoletes = new ArrayList<Dependency>()
    List<Dependency> conflicts = new ArrayList<Dependency>()
    // Deb-specific special dependencies
    List<Dependency> recommends = new ArrayList<Dependency>()
    List<Dependency> suggests = new ArrayList<Dependency>()
    List<Dependency> enhances = new ArrayList<Dependency>()
    List<Dependency> preDepends = new ArrayList<Dependency>()
    List<Dependency> breaks = new ArrayList<Dependency>()
    List<Dependency> replaces = new ArrayList<Dependency>()
    List<Dependency> provides = new ArrayList<Dependency>()
    List<Directory> directories = new ArrayList<Directory>()

    // DEB-specific user-defined fields
    // https://www.debian.org/doc/debian-policy/ch-controlfields.html#s5.7
    Map<String, String> customFields = [:]

    @Input
    @Optional
    String getPackageName() {
        return packageName
    }

    @Input
    @Optional
    String getRelease() {
        return release
    }

    @Input
    @Optional
    String getVersion() {
        return version
    }

    @Input
    @Optional
    Integer getEpoch() {
        return epoch
    }

    @Input
    @Optional
    String getSigningKeyId() {
        return signingKeyId
    }

    @Input
    @Optional
    String getSigningKeyPassphrase() {
        return signingKeyPassphrase
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    File getSigningKeyRingFile() {
        return signingKeyRingFile
    }

    @Input
    @Optional
    String getUser() {
        return user
    }

    @Input
    @Optional
    String getPermissionGroup() {
        return permissionGroup
    }

    @Input
    @Optional
    String getPackageGroup() {
        return packageGroup
    }

    @Input
    @Optional
    String getBuildHost() {
        return buildHost
    }

    @Input
    @Optional
    String getSummary() {
        return summary
    }

    @Input
    @Optional
    String getPackageDescription() {
        return packageDescription
    }

    @Input
    @Optional
    String getLicense() {
        return license
    }

    @Input
    @Optional
    String getPackager() {
        return packager
    }

    @Input
    @Optional
    String getDistribution() {
        return distribution
    }

    @Input
    @Optional
    String getVendor() {
        return vendor
    }

    @Input
    @Optional
    String getUrl() {
        return url
    }

    @Input
    @Optional
    String getSourcePackage() {
        return sourcePackage
    }

    @Input
    @Optional
    String getArchStr() {
        return archStr
    }

    void setArch(Object arch) {
        archStr = (arch instanceof Architecture) ? arch.name() : arch.toString()
    }

    @Input
    @Optional
    Directive getFileType() {
        return fileType
    }

    @Input
    @Optional
    Boolean getCreateDirectoryEntry() {
        return createDirectoryEntry
    }

    @Input
    @Optional
    Boolean getAddParentDirs() {
        return addParentDirs
    }

    @Input
    @Optional
    Os getOs() {
        return os
    }

    @Input
    @Optional
    RpmType getType() {
        return type
    }


    def prefix(String prefixStr) {
        prefixes << prefixStr
        return this
    }

    @Input
    @Optional
    List<String> getPrefixes() {
        return prefixes
    }

    @Input
    @Optional
    Integer getUid() {
        return uid
    }

    @Input
    @Optional
    Integer getGid() {
        return gid
    }

    @Input
    @Optional
    MultiArch getMultiArch() {
        return multiArch
    }

    @Input
    @Optional
    String getMaintainer() {
        return maintainer
    }

    @Input
    @Optional
    String getUploaders() {
        return uploaders
    }

    @Input
    @Optional
    String getPriority() {
        return priority
    }

    @Input
    @Optional
    List<Object> getSupplementaryControlFiles() {
        return supplementaryControlFiles
    }

    def supplementaryControl(Object file) {
        supplementaryControlFiles << file
        return this
    }

    // Scripts
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPreInstallFile() {
        return preInstallFile
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPostInstallFile() {
        return postInstallFile
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPreUninstallFile() {
        return preUninstallFile
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    File getPostUninstallFile() {
        return postUninstallFile
    }

    @Input
    @Optional
    List<Object> getConfigurationFiles() {
        return configurationFiles
    }

    @Input
    @Optional
    List<Object> getPreInstallCommands() {
        return preInstallCommands
    }

    @Input
    @Optional
    List<Object> getPostInstallCommands() {
        return postInstallCommands
    }

    @Input
    @Optional
    List<Object> getPreUninstallCommands() {
        return preUninstallCommands
    }

    @Input
    @Optional
    List<Object> getPostUninstallCommands() {
        return postUninstallCommands
    }

    @Input
    @Optional
    List<Object> getPreTransCommands() {
        return preTransCommands
    }

    @Input
    @Optional
    List<Object> getPostTransCommands() {
        return postTransCommands
    }

    @Input
    @Optional
    List<Object> getCommonCommands() {
        return commonCommands
    }

    @Input
    @Optional
    List<Link> getLinks() {
        return links
    }

    @Input
    @Optional
    List<Dependency> getDependencies() {
        return dependencies
    }

    @Input
    @Optional
    List<Dependency> getObsoletes() {
        return obsoletes
    }

    @Input
    @Optional
    List<Dependency> getRecommends() {
        return recommends
    }

    @Input
    @Optional
    List<Dependency> getSuggests() {
        return suggests
    }

    @Input
    @Optional
    List<Dependency> getEnhances() {
        return enhances
    }

    @Input
    @Optional
    List<Dependency> getPreDepends() {
        return preDepends
    }

    @Input
    @Optional
    List<Dependency> getBreaks() {
        return breaks
    }

    @Input
    @Optional
    List<Dependency> getReplaces() {
        return replaces
    }

    @Input
    @Optional
    List<Dependency> getProvides() {
        return provides
    }

    @Input
    @Optional
    List<Dependency> getConflicts() {
        return conflicts
    }

    @Input
    @Optional
    List<Directory> getDirectories() {
        return directories
    }

    @Input
    @Optional
    Map<String, String> getCustomFields() {
        return customFields
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setInstallUtils(File script) {
        installUtils(script)
    }

    def installUtils(String script) {
        commonCommands << script
        return this
    }

    def installUtils(File script) {
        commonCommands << script
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
        configurationFiles << path
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
        if(preInstallFile) { throw PREINSTALL_COMMANDS_AND_FILE_DEFINED }
        preInstallCommands << script
        return this
    }

    def preInstall(File script) {
        if(preInstallFile) { throw PREINSTALL_COMMANDS_AND_FILE_DEFINED }
        preInstallCommands << script
        return this
    }

    def preInstallFile(File path) {
        if(preInstallFile) { throw MULTIPLE_PREINSTALL_FILES }
        if(preInstallCommands) { throw PREINSTALL_COMMANDS_AND_FILE_DEFINED }
        preInstallFile = path
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setPostInstall(File script) {
        postInstall(script)
    }

    def postInstall(String script) {
        if(postInstallFile) { throw POSTINSTALL_COMMANDS_AND_FILE_DEFINED }
        postInstallCommands << script
        return this
    }

    def postInstall(File script) {
        if(postInstallFile) { throw POSTINSTALL_COMMANDS_AND_FILE_DEFINED }
        postInstallCommands << script
        return this
    }

    def postInstallFile(File path) {
        if(postInstallFile) { throw MULTIPLE_POSTINSTALL_FILES }
        if(postInstallCommands) { throw POSTINSTALL_COMMANDS_AND_FILE_DEFINED }
        postInstallFile = path
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setPreUninstall(File script) {
        preUninstall(script)
    }

    def preUninstall(String script) {
        if(preUninstallFile) { throw PREUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        preUninstallCommands << script
        return this
    }

    def preUninstall(File script) {
        if(preUninstallFile) { throw PREUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        preUninstallCommands << script
        return this
    }

    def preUninstallFile(File script) {
        if(preUninstallFile) { throw MULTIPLE_PREUNINSTALL_FILES }
        if(preUninstallCommands) { throw PREUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        preUninstallFile = script
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setPostUninstall(File script) {
        postUninstall(script)
    }

    def postUninstall(String script) {
        if(postUninstallFile) { throw POSTUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        postUninstallCommands << script
        return this
    }

    def postUninstall(File script) {
        if(postUninstallFile) { throw POSTUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        postUninstallCommands << script
        return this
    }

    def postUninstallFile(File script) {
        if(postUninstallFile) { throw MULTIPLE_POSTUNINSTALL_FILES }
        if(postUninstallCommands) { throw POSTUNINSTALL_COMMANDS_AND_FILE_DEFINED }
        postUninstallFile = script
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setPreTrans(File script) {
        preTrans(script)
    }

    def preTrans(String script) {
        preTransCommands << script
        return this
    }

    def preTrans(File script) {
        preTransCommands << script
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
        postTransCommands << script
        return this
    }

    def postTrans(File script) {
        postTransCommands << script
        return this
    }

    // @groovy.transform.PackageScope doesn't seem to set the proper scope when going through a @Delegate



    Link link(String path, String target) {
        link(path, target, -1)
    }

    Link link(String path, String target, int permissions) {
        Link link = new Link()
        link.path = path
        link.target = target
        link.permissions = permissions
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
        directories << directory
        directory
    }

    Directory directory(String path, boolean addParents) {
        Directory directory = new Directory(path: path, addParents: addParents)
        directories << directory
        directory
    }

    Directory directory(String path, int permissions) {
        Directory directory = new Directory(path: path, permissions: permissions)
        directories << directory
        directory
    }

    Directory directory(String path, int permissions, boolean addParents) {
        Directory directory = new Directory(path: path, permissions: permissions, addParents: addParents)
        directories << directory
        directory
    }

    Directory directory(String path, int permissions, String user, String permissionGroup) {
        Directory directory = new Directory(path: path, permissions: permissions, user: user, permissionGroup: permissionGroup)
        directories << directory
        directory
    }

    Directory directory(String path, int permissions, String user, String permissionGroup, boolean addParents) {
        Directory directory = new Directory(path: path, permissions: permissions, user: user, permissionGroup: permissionGroup, addParents: addParents)
        directories << directory
        directory
    }

    def customField(String key, String val) {
        customFields[key] = val
        return this
    }

    def customField(Map<String, String> fields) {
        customFields += fields
        return this
    }

    private static IllegalStateException multipleFilesDefined(String fileName) {
        new IllegalStateException("Cannot specify more than one $fileName File")
    }

    private static IllegalStateException conflictingDefinitions(String type) {
        new IllegalStateException("Cannot specify $type File and $type Commands")
    }
}
