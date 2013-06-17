package com.trigonic.gradle.plugins.packaging
/**
 * Extension that can be used to configure both DEB and RPM.
 *
 * Ideally we'd have multiple levels, e.g. an extension for everything, one for deb which extends the, one for rpm
 * that extends the base and have all the tasks take from the specific extension. But since tasks.withType can be used,
 * there isn't really a need for "all rpm" extension.
 */

class SystemPackagingExtension {
    // File name components
    String packageName
    String release

    // Metadata
    String user
    String group
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
    String provides

    // Scripts
    final List<Object> installUtilCommands = []
    final List<Object> preInstallCommands = []
    final List<Object> postInstallCommands = []
    final List<Object> preUninstallCommands = []
    final List<Object> postUninstallCommands = []


    /**
     * For backwards compatibility
     * @param script
     */
    def setInstallUtils(File script) {
        installUtils(script)
    }

    def installUtils(String script) {
        installUtilCommands << script
        return this
    }

    def installUtils(File script) {
        installUtilCommands << script
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
        preInstallCommands << script
        return this
    }

    def preInstall(File script) {
        preInstallCommands << script
        return this
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setPostInstall(File script) {
        preInstall(script)
    }

    def postInstall(String script) {
        postInstallCommands << script
        return this
    }

    def postInstall(File script) {
        postInstallCommands << script
        return this
    }


    /**
     * For backwards compatibility
     * @param script
     */
    def setPreUninstall(File script) {
        preUninstall(script)
    }

    def preUninstall(String script) {
        preUninstallCommands << script
        return this
    }

    def preUninstall(File script) {
        preUninstallCommands << script
        return this
    }

    /**
     * For backwards compatibility
     * @param script
     */
    def setPostUninstall(File script) {
        preUninstall(script)
    }

    def postUninstall(String script) {
        postUninstallCommands << script
        return this
    }

    def postUninstall(File script) {
        postUninstallCommands << script
        return this
    }


    // @groovy.transform.PackageScope doesn't seem to set the proper scope when going through a @Delegate
    List<Link> links = new ArrayList<Link>()
    List<Dependency> dependencies = new ArrayList<Dependency>();

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
        Dependency dep = new Dependency()
        dep.packageName = packageName
        dep.version = version
        dep.flag = flag
        dependencies.add(dep)
        dep
    }

    Dependency requires(String packageName) {
        requires(packageName, '', 0)
    }

}
