# Gradle DEB plugin

This plugin provides Gradle-based assembly of DEB packages, typically for Linux distributions
derived from Debian, e.g. Ubuntu.  It leverages [JDeb](https://github.com/tcurdt/jdeb) Java library.

# Basic Usage

```
    buildscript {
        repositories {
            mavenCentral()
        }

        dependencies {
            classpath 'com.trigonic:gradle-rpm-plugin:2.0'
        }
    }

    apply plugin: 'deb'

    task fooRpm(type: Deb) {
        release = 1
    }

```

# Task Usage

The DEB plugin provides a Task based on a standard copy task, similar to the Zip and Tar tasks. The power comes from
specifying "from" and "into" calls, read more in the [Copying files](http://www.gradle.org/docs/current/userguide/working_with_files.html#sec:copying_files)
section of the Gradle documentation.  On top of the standard available methods, there are some additional values that
be set, which are specific to DEBs. Quite of them have defaults which fall back to fields on the project.

* _packageName_ - Default to project.name
* _release_ - DEB Release
* _version_ - Version field, defaults to project.version
* _user_ - Default user to permission files to
* _permissionGroup_ - Default group to permission files to, "group" is used by Gradle for the display of tasks
* _packageGroup_
* _buildHost_
* _summary_
* _packageDescription_
* _license_
* _packager_
* _distribution_
* _vendor_
* _url_
* _sourcePackage_
* _provides_
* _createDirectoryEntry [Boolean]_
* uid - Default uid of files
* gid - Default gid of files

# Symbolic Links

Symbolic links are specified via the links method, where the permissions umask is optional:

```
link(String src, String dest, int permissions)
```

# Requires

Required packages are specified via the required method:

```
requires(String packageName, String version)
```

# Scripts

To provide the scripts traditionally seen in the spec files, they are provided as Strings or as files. Their
corresponding methods can be called multiple times, and the contents will be appended in the order provided.

* _preInstall_
* _postInstall_
* _preUninstall_
* _postUninstall_
* _installUtils_ - Scripts which are prefixed to all the other scripts.

# Copy Spec

The following attributes can be used inside _from_ and _into_ closures to complement the [Copy Spec](http://www.gradle.org/docs/current/userguide/working_with_files.html#sec:copying_files).

* user
* permissionGroup
* fileType
* uid
* gid

(Above can be set via property syntax, e.g. "user='jryan'", or method syntax, e.g. "user 'jryan'")

# Example

```
    task fooRpm(type: Deb) {
        packageName = 'foo'
        version = '1.2.3'
        release = 1

        installUtils = file('scripts/rpm/utils.sh')
        preInstall = file('scripts/rpm/preInstall.sh')
        postInstall = file('scripts/rpm/postInstall.sh')
        preUninstall = file('scripts/rpm/preUninstall.sh')
        postUninstall = file('scripts/rpm/postUninstall.sh')

        requires('bar', '2.2')
        requires('baz', '1.0.1')
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
            createDirectoryEntry = true
            fileMode = 0500
            into 'home'
        }
        from('endorsed') {
            into '/usr/share/tomcat/endorsed'
        }

        link('/opt/foo/bin/foo.init', '/etc/init.d/foo')
    }
```

