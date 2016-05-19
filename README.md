# Gradle Linux Packaging Plugin

![Support Status](https://img.shields.io/badge/nebula-supported-brightgreen.svg)
[![Build Status](https://travis-ci.org/nebula-plugins/gradle-ospackage-plugin.svg?branch=master)](https://travis-ci.org/nebula-plugins/gradle-ospackage-plugin)
[![Coverage Status](https://coveralls.io/repos/nebula-plugins/gradle-ospackage-plugin/badge.svg?branch=master&service=github)](https://coveralls.io/github/nebula-plugins/gradle-ospackage-plugin?branch=master)
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
  id 'nebula.ospackage' version '3.5.0'
}
```

For older versions of gradle

```groovy
buildscript {
  repositories { jcenter() }
  dependencies {
    classpath 'com.netflix.nebula:gradle-ospackage-plugin:3.5.0'
  }
}

apply plugin: 'nebula.ospackage'
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

# ospackage-daemon plugin

Builds the proper scripts for running a daemon (with daemontools) on CentOS and Ubuntu.

## Applying the Plugin

To include, add the following to your build.gradle

    buildscript {
        repositories { jcenter() }

        dependencies {
            classpath 'com.netflix.nebula:nebula-ospackage-plugin:3.+'
        }
    }

    apply plugin: 'nebula.ospackage-daemon'

## Usage

An extension is provided to configure possible daemons.

The simplest usage is a single daemon:

    daemon {
        daemonName = "foobar" // default = packageName
        command = "sleep infinity" // required
    }

Technically only the command field is needed, and the daemon's name will default to the package being built. This is the
complete list of fields available:

* daemonName:String - Name used to start and stop the dameon
* command:String - Command to ensure is running
* user:String - User to run as, defaults to "root"
* logCommand:String - Log command to run, defaults to "multilog t ./main"
* runLevels:List<Integer> - Run levels for daemon, rpm defaults to [3,4,5], deb defaults to [2,3,4,5]
* autoStart:Boolean - Should auto start, defaults to true
* startSequence:Integer - Boot ordering, default is 85
* stopSequence:Integer - Shutdown ordering, default is 15

Multiple daemons can be defined using the _daemons_ extension, e.g.

    daemons {
        daemon {
            // daemonName default = packageName
            command = 'exit 0'
        }
        daemon {
            daemonName = "foobar"
            command = "sleep infinity" // required
        }
        daemon {
            daemonName = "fooqux"
            command = "sleep infinity"
            user = "nobody"
            logCommand = "cronolog /logs/foobar/foobar.log"
            runLevels = [3,4]
            autoStart = false
            startSequence = 99
            stopSequence = 1
        }
    }

### Tasks Provided

A task used for templating will be created for each combination of System Packaging task and daemon. For example, if there's
a daemon called Foobar, a Rpm task, and a Deb task, there would also be a buildDebFoobarDaemon task and a buildRpmFoobarDaemon
task.

# ospackage-application plugin

Takes the output of the [Application plugin](http://www.gradle.org/docs/current/userguide/application_plugin.html) and
packages it into a system package like a RPM or DEB.  It uses the os-package plugin to accomplish this.

## Applying the Plugin

To include, add the following to your build.gradle

    buildscript {
        repositories { jcenter() }

        dependencies {
            classpath 'com.netflix.nebula:nebula-ospackage-plugin:3.+'
        }
    }

    apply plugin: 'nebula.ospackage-application'

## Usage

There is a single property available on a _ospackage-application_ extension which controls the prefix.

    ospackage_application {
        prefix = '/usr/local'
    }

Otherwise the prefix defaults to _/opt_, the actual installation will be to into _/opt/$applicationName_, where
_applicationName_ comes from the Application plugin. As usual to the Application plugin, the user has to provide a
_mainClassName_.

Once configured with a system packaging task, the project will produce a DEB or RPM with the context of the application:

    gradlew buildDeb


# ospackage-application-daemon plugin

Combine the above two plugins to create a self-running daemon out of a [Application plugin](http://www.gradle.org/docs/current/userguide/application_plugin.html)
project.

## Applying the Plugin

To include, add the following to your build.gradle

    buildscript {
        repositories { jcenter() }

        dependencies {
            classpath 'com.netflix.nebula:nebula-ospackage-plugin:3.+'
        }
    }

    apply plugin: 'nebula.ospackage-application-daemon'

## Usage

Since this plugin is making the daemon for you, it could be difficult to access the standard daemon configuration. To
alleviate this, there's a extension provided for configuring the application daemon, called _applicationdaemon_:

    applicationdaemon {
        user = "nobody"
    }

Once configured, the project will produce a DEB and a RPM with the context of the application and the relevant daemon scripts:

    gradlew buildDeb buildRpm

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

Gradle Compatibility Tested
---------------------------

Built with Oracle JDK7
Tested with Oracle JDK8

| Gradle Version | Works |
| :------------: | :---: |
| 2.2.1          | yes   |
| 2.3            | yes   |
| 2.4            | yes   |
| 2.5            | yes   |
| 2.6            | yes   |
| 2.7            | yes   |
| 2.8            | yes   |
| 2.9            | yes   |
| 2.10           | yes   |

LICENSE
=======

Copyright 2014-2016 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

