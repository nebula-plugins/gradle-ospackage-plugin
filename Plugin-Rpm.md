# Gradle RPM plugin

This plugin provides Gradle-based assembly of RPM packages, typically for Linux distributions
derived from RedHat.  It leverages [Redline](http://redline-rpm.org/) Java library.

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

    apply plugin: 'nebula.rpm'

    task fooRpm(type: Rpm) {
        release '1'
    }

```

# Task Usage

The RPM plugin provides a Task based on a standard copy task, similar to the Zip and Tar tasks. The power comes from
specifying "from" and "into" calls, read more in the [Copying files](http://www.gradle.org/docs/current/userguide/working_with_files.html#sec:copying_files)
section of the Gradle documentation.  On top of the standard available methods, there are some additional values that
be set, which are specific to RPMs. Quite of them have defaults which fall back to fields on the project.

* _packageName_ - Default to project.name
* _release_ - RPM Release
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
* _fileType [org.freecompany.redline.payload.Directive]_ - Default for types, e.g. CONFIG, DOC, NOREPLACE, LICENSE
* _createDirectoryEntry [Boolean]_
* _addParentDirs [Boolean]_
* _arch [org.freecompany.redline.header.Architecture]_ - E.g. NOARCH, I386, ARM, X86_64. Defaults to NOARCH and can be a String.
* _os [org.freecompany.redline.header.Os]_ - E.g. LINUX
* _type [org.freecompany.redline.header.RpmType]_ - BINARY, SOURCE
* _prefix_

# Symbolic Links

Symbolic links are specified via the links method, where the permissions umask is optional:

```
link(String symLinkPath, String targetPath, int permissions)
```

# Requires

Required packages are specified via the required method:

```
requires(String packageName, String version, int flag)
```

Version and flag are both optional parameters. Flag comes from the org.freecompany.redline.header.Flags class. They can be "|" or'd together.
E.g. GREATER | EQUAL

# Prefix

Register a prefix with this package. Can be called by setting the prefixes variable as a list, or individually.

```
prefix(String prefixPath)
```

# Obsoletes

Specifies packages for which this package obsoletes. 


```
obsoletes(String packageName, String version, int flag)
```

E.g.

```
obsoletes('blech')
obsoletes('blarg', '1.0', GREATER | EQUAL)
```

# Conflicts 

Specifies packages for which this package conflicts with. 


```
conflicts(String packageName, String version, int flag)
```

E.g.

```
conflicts('packageA')
conflicts('packageB', '2.2', GREATER)
```

# Directories
 
Specifies directory that should be created within package.


```
directory(String path, int permissions)
```

E.g.

```
directory('/some/dir')
directory('/some/dir', 644)
```

# Scripts

To provide the scripts traditionally seen in the spec files, they are provided as Strings or as files. Their
corresponding methods can be called multiple times, and the contents will be appended in the order provided.

* _preInstall_
* _postInstall_
* _preUninstall_
* _postUninstall_
* _preTrans_
* _postTrans_
* _installUtils_ - Scripts which are prefixed to all the other scripts.

# Aliases

To make it easier to use the types above which are not native types, they have been injected into the task to make them
available without imports. Specifically Architecture, Os, RpmType, Directive, Flags.

# Copy Spec

The following attributes can be used inside _from_ and _into_ closures to complement the [Copy Spec](http://www.gradle.org/docs/current/userguide/working_with_files.html#sec:copying_files).

* user
* permissionGroup
* fileType
* addParentDirs [boolean]
* createDirectoryEntry [boolean]

(Above can be set via property syntax, e.g. "user='jryan'", or method syntax, e.g. "user 'jryan'")

# Example

```
    task fooRpm(type: Rpm) {
        packageName 'foo'
        version '1.2.3'
        release 1
        arch I386
        os LINUX

        installUtils = file('scripts/rpm/utils.sh')
        preInstall file('scripts/rpm/preInstall.sh')
        postInstall file('scripts/rpm/postInstall.sh')
        preUninstall file('scripts/rpm/preUninstall.sh')
        postUninstall file('scripts/rpm/postUninstall.sh')
        preTrans file('scripts/rpm/preTrans.sh')
        postTrans file('scripts/rpm/postTrans.sh')

        requires('bar', '2.2', GREATER | EQUAL)
        requires('baz', '1.0.1', LESS)
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
            // Creating directory entries (or not) in the RPM is normally left up to redline-rpm library.
            // Use this to explicitly create an entry -- for setting directory fileMode on system directories.
            createDirectoryEntry = true
            fileMode 0500
            into 'home'
        }
        from('endorsed') {
            // Will tell redline-rpm not to auto create directories, which
            // is sometimes necessary to avoid rpm directory conflicts
            addParentDirs false
            into '/usr/share/tomcat/endorsed'
        }

        link('/etc/init.d/foo', '/opt/foo/bin/foo.init')
        
    }
```
