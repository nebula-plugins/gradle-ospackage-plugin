# Gradle DEB plugin

This plugin provides Gradle-based assembly of DEB packages, typically for Linux distributions
derived from Debian, e.g. Ubuntu.  It leverages [JDeb](https://github.com/tcurdt/jdeb) Java library.

# Basic Usage

```
    buildscript {
        repositories {
            jcenter()
        }

        dependencies {
            classpath 'com.netflix.nebula:gradle-ospackage-plugin:1.12.2'
        }
    }

    apply plugin: 'nebula.deb'

    task fooDeb(type: Deb) {
        release '1'
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
* _epoch_ - Epoch, defaults to 0
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
* _signingKeyId_
* _signingKeyPassphrase_
* _signingKeyRingFile_
* _sourcePackage_
* _provides_
* _createDirectoryEntry [Boolean]_
* uid - Default uid of files
* gid - Default gid of files
* _arch_ - Architecture, defaults ot "all". E.g. "amd64", "all"
* _maintainers_ - Defaults to packager
* _uploaders_ _ Defaults to packager
* _priority_
* _multiArch_ - Configure multi-arch behavior: NONE (default), SAME, FOREIGN, ALLOWED (see: https://wiki.ubuntu.com/MultiarchSpec )
* _conflicts_
* _recommends_
* _suggests_
* _enhances_
* _preDepends_
* _breaks_
* _replaces_

# Symbolic Links

Symbolic links are specified via the links method, where the permissions umask is optional:

```
link(String symLinkPath, String targetPath, int permissions)
```



# Requires

Required packages are specified via the required method:

```
requires(String packageName)
```
or
```
requires(String packageName, String version)
```
or
```
requires(String packageName, String version, int flag) 
```

For information about possible flags, see [Dependency.groovy](https://github.com/nebula-plugins/gradle-ospackage-plugin/blob/master/src/main/groovy/com/netflix/gradle/plugins/packaging/Dependency.groovy#L37).

# Other Relationships

Debian packages support declaration of other relationships. The Deb task supports the following in addition to Requires:
* _conflicts_
* _recommends_
* _suggests_
* _enhances_
* _preDepends_ (configures the Pre-Depends field)
* _breaks_
* _replaces_

For more information, see the [Debian Policy Manual section on relationships.](https://www.debian.org/doc/debian-policy/ch-relationships.html)
Syntax is identical to `requires`

# Scripts

To provide the scripts traditionally seen in the spec files, they are provided as Strings or as files. Their
corresponding methods can be called multiple times, and the contents will be appended in the order provided.

* _preInstall_
* _postInstall_
* _preUninstall_
* _postUninstall_
* _installUtils_ - Scripts which are prefixed to all the other scripts.
* _configurationFiles_ - Files to be labeled as configuration files

# User-Defined Control Headers

Per the [Debian Policy Manual](https://www.debian.org/doc/debian-policy/ch-controlfields.html#s5.7), user-defined headers
may be contributed to a package. Use the `customField` method to add key/val pairs, merge existing maps, or modify the
`customFields` map directly. See example below.

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
    task fooDeb(type: Deb) {
        packageName = 'foo'
        version = '1.2.3'
        release = 1

        configurationFile('/etc/defaults/myapp')
        installUtils file('scripts/deb/utils.sh')
        preInstall file('scripts/deb/preInstall.sh')
        postInstall file('scripts/deb/postInstall.sh')
        preUninstall file('scripts/deb/preUninstall.sh')
        postUninstall file('scripts/deb/postUninstall.sh')

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
            fileMode 0550
        }
        from('src/main/resources') {
            fileType CONFIG | NOREPLACE
            into 'conf'
        }
        from('home') {
            createDirectoryEntry = true
            fileMode 0500
            into 'home'
        }
        from('endorsed') {
            into '/usr/share/tomcat/endorsed'
        }

        link('/etc/init.d/foo', '/opt/foo/bin/foo.init')

        customField 'Build-Host', 'http://mycihost'
        customField([
            'Commit-ID': 'deadbeef',
            'Owner': 'John Doe <johndoe@sweetdomain.io>'
        ])
        customFields << [
            'Build-Job': 'FooProject'
        ]
    }
```

