# Gradle Linux Packaging Plugin

This plugin provides Gradle-based assembly of system packages, typically for RedHat and Debian based distributions,
using a canonical Gradle Copy Specs. It's structured as three plugins, which work in concert, and a fourth plugin
to pull them all together. Keep reading to see some power examples, follow the links to further pages for the formal
documentation. All the plugins are pure-java and don't require any local native binaries.

# Accessing the plugin

Gradle requires that plugins be added to the classpath as part of the classpath, the following can be used to use this
plugin:

```
    buildscript {
        repositories {
            mavenCentral()
        }

        dependencies {
            classpath 'com.trigonic:gradle-rpm-plugin:2.0'
        }
    }
```

# Gradle RPM plugin

[Formal Documentation](Plugin-Rpm.md)

This plugin provides Gradle-based assembly of RPM packages, typically for Linux distributions
derived from RedHat.  It leverages [Redline](http://redline-rpm.org/) Java library.

# Gradle DEB plugin

[Formal Documentation](Plugin-Deb.md)

This plugin provides Gradle-based assembly of DEB packages, typically for Linux distributions
derived from Debian, e.g. Ubuntu.  It leverages [JDeb](https://github.com/tcurdt/jdeb) Java library.

# os-package-base plugin

For the scenarios where packages have to be built for both CentOS and Ubuntu, it's likely they'll have the same paths to
copy from and into. To help share settings between the output formats, or even to share between multiple tasks of the
same type, this plugin exposes an extension that which will propagate to the tasks. Any settings set on the extension,
will serve as defaults, while values on the task will take precedence. Any copy specs, e.g. _from()_, will apply to all
tasks irrelevant of what the task specifies.

```
apply plugin: 'os-package-base'

ospackage {
    release '3'
    os = LINUX // only applied to RPM
    into '/opt/app1'
    from file('dist') {
        user 'builds'
        exclude '**/*.rb'
    }
}

task app1Rpm(type: Rpm) {
    packageName = 'foo'
    arch = I386
}

task app1Deb(type: Deb) {
}
```

# os-package plugin

Built on top of the os-package-base plugin, the os-package plugin automatically creates two tasks, buildRpm and buildDeb.
This is useful when the project defaults are good and any configuration can be put into the ospackage extension. It
 leverages the fact that most project have no need to customize on a per-package-type basis and want a package for each
 platform supported.

```
apply plugin: 'os-package'

ospackage {
    release '3'
    os = LINUX // only applied to RPM
    into '/opt/app1'
    from file('dist') {
        user 'builds'
        exclude '**/*.rb'
    }
}

// buildRpm and buildDeb are implicitly created, but can still be configured if needed

buildRpm {
    arch = I386
}
```

# Full Usage Example
```
    buildscript {
        repositories {
            mavenCentral()
        }

        dependencies {
            classpath 'com.trigonic:gradle-rpm-plugin:1.3'
        }
    }

    apply plugin: 'os-package'

    ospackage {
        packageName = 'foo'
        version = '1.2.3'
        release = 1
        arch = I386
        os = LINUX

        installUtils = file('scripts/rpm/utils.sh')
        preInstall = file('scripts/rpm/preInstall.sh')
        postInstall = file('scripts/rpm/postInstall.sh')
        preUninstall = file('scripts/rpm/preUninstall.sh')
        postUninstall = file('scripts/rpm/postUninstall.sh')

        requires('qux')

        into '/opt/foo'

        from(jar.outputs.files) {
            into 'lib'
        }
        from(configurations.runtime) {
            into 'lib'
        }
        from('lib') {
            into 'lib'
        }
        from('scripts') {
            into 'bin'
            exclude 'database'
            fileMode = 0550
        }
        from('src/main/resources') {
            fileType = CONFIG | NOREPLACE
            into 'conf'
        }
        from('home') {
            // Creating directory entries (or not) in the RPM is normally left up to redline-rpm library.
            // Use this to explicitly create an entry -- for setting directory fileMode on system directories.
            createDirectoryEntry = true
            fileMode = 0500
            into 'home'
        }
        from('endorsed') {
            // Will tell redline-rpm not to auto create directories, which
            // is sometimes necessary to avoid rpm directory conflicts
            addParentDirs = false
            into '/usr/share/tomcat/endorsed'
        }

    }

    buildRpm {
        requires('bar', '2.2', GREATER | EQUAL)
        requires('baz', '1.0.1', LESS)
        link('/opt/foo/bin/foo.init', '/etc/init.d/foo')
    }

    buildDeb {
        requires('bat', '1.0.1')
        link('/opt/foo/bin/foo.upstart', '/etc/init.d/foo')
    }

```
