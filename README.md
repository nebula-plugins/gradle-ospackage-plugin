# Gradle Linux Packaging Plugin

[![Build Status](https://travis-ci.org/nebula-plugins/gradle-ospackage-plugin.svg?branch=master)](https://travis-ci.org/nebula-plugins/gradle-ospackage-plugin)
[![Coverage Status](https://coveralls.io/repos/nebula-plugins/gradle-ospackage-plugin/badge.svg?branch=masterservice=github)](https://coveralls.io/github/nebula-plugins/gradle-ospackage-plugin?branch=master)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/nebula-plugins/gradle-ospackage-plugin?utm_source=badgeutm_medium=badgeutm_campaign=pr-badge)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/gradle-ospackage-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)

This plugin provides Gradle-based assembly of system packages, typically for RedHat and Debian based distributions,
using a canonical Gradle Copy Specs. It's structured as three plugins, which work in concert, and a fourth plugin
to pull them all together. Keep reading to see some power examples, follow the links to further pages for the formal
documentation. All the plugins are pure-java and don't require any local native binaries.

# Accessing the plugin

Gradle requires that plugins be added to the classpath as part of the classpath. For Gradle 2.0+ projects, you can use the `plugins` DSL to bring in this plugin:

```groovy
plugins {
  id "nebula.os-package" version "2.2.6"
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
apply plugin: 'nebula.ospackage-base'

ospackage {
    release '3'
    os = LINUX // only applied to RPM
    prefix '/opt/local' // also only applied to RPM
    into '/opt/app1'
    from ('dist') {
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
apply plugin: 'nebula.ospackage'

ospackage {
    release '3'
    os = LINUX // only applied to RPM
    into '/opt/app1'
    from ('dist') {
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
            jcenter()
        }

        dependencies {
            classpath 'com.netflix.nebula:gradle-ospackage-plugin:1.12.2'
        }
    }

    apply plugin: 'nebula.ospackage'

    ospackage {
        packageName = 'foo'
        version = '1.2.3'
        release = '1'
        arch = I386
        os = LINUX

        installUtils file('scripts/rpm/utils.sh')
        preInstall file('scripts/rpm/preInstall.sh')
        postInstall file('scripts/rpm/postInstall.sh')
        preUninstall 'touch /tmp/myfile'
        postUninstall file('scripts/rpm/postUninstall.sh')

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
            fileType CONFIG | NOREPLACE
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
        link('/etc/init.d/foo’, '/opt/foo/bin/foo.init')
    }

    buildDeb {
        requires('bat', '1.0.1')
        link('/etc/init.d/foo', '/opt/foo/bin/foo.upstart')
    }

```
