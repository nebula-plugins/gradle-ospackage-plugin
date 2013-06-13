package com.trigonic.gradle.plugins.packaging

import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

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
    @InputFile @Optional File installUtils
    @InputFile @Optional File preInstall
    @InputFile @Optional File postInstall
    @InputFile @Optional File preUninstall
    @InputFile @Optional File postUninstall

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
