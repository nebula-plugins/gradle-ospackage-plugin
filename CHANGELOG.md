2.2.4 / 2015-04-30
------------------

* Omit epoch in debian control files if it is 0

2.2.3
-----


2.2.2
-----
  * Changed type of arch field to String to allow specifying "amd64" for Deb packages. Still supports old Architecture from freeline syntax.
  * Deb: Add support to specify Architecture
  * Deb: Add support to specify Maintainer in the correct format
  * Deb: Add support to specify Priority
  * Deb: Add support to specify configuration files in conffiles

2.2.1
-----

* Issue #60: Upgrade JDeb library to latest version to avoid having to monkey patch code through reflection.
* Enabled declaration of Conflicts, Recommends, Suggests, Enhances, Pre-Depends, Breaks, and Replaces on DEB packages
* Pull request #88: Upgraded Redline library to version 1.2.1 and introduced epoch property to be configured. This is a
potentially BREAKING change as consumers might rely on the old `org.freecompany` package name
* Pull request #92: Fixed rpm task on hosts with incorrect hostname.
* Issue #85: Validation of Debian version attribute.
* Issue #90: Validation of Debian and RPM package name attribute.
* Add Docker as target artifact.

2.2.0
-----

* Upgrade to Gradle 2.2.1

2.0.3
-----

* Issue #66: Directory ownership for RPMs are not set correctly

2.0.2
-----

* Issue #48: Fix UnsupportedOperationException in NormalizingCopyActionDecorator$StubbedFileCopyDetails

2.0.1
-----

* Issue #51: Avoid runtime dependency on Google Guava

2.0.0
-----

* Upgrade to Gradle 2.0

1.12.8
------

* Issue #60: Upgrade JDeb library to latest version to avoid having to monkey batch code through reflection.

1.12.6
------

* Issue #48: Fix UnsupportedOperationException in NormalizingCopyActionDecorator$StubbedFileCopyDetails

1.9.3
------
* Upgrade of Redline to 1.1.15 (Thanks to @merscwog for tracking down)
* Support version constraints on depends in DEBs
* BREAKING: Enforce that there's no commas in a required package name. It will now fail instead of allowing a borked package to be created.
* Add RPM aliases to DEBs too
* Fix postUninstalls being treated as preUninstalls (Thanks to @ngutzmann)
* Use @Input for incremental task execution, so now more elements of a package will  be used to determine if a task is up-to-date.
* Add support for prefixes (Thanks to @merscwog)

1.9.2
------
* Using nebula-plugin-plugin for build and deploy

v1.8
------
  * Add support for Gradle 1.8
  * Add support for debian files
  * Add extension that can configure all packaging tasks
  * Breaking change: Changed group and description to packageGroup and packageDescription, respectively to
    avoid conflict with Gradle's similarly named variables.
  * bugfix: setter syntax for postUninstall was appending to preUninstall

v1.4 - 2013-08-28
------
  * issue #44: Gradle 1.7 incompatibility
  * issue #29: optional parents info
  * issue #18: add syntactic sugar to easily combine file type
    directives with a logical or (|)

v1.3 - 2013-01-08
------
  * bugfix: RPM_ARCH and RPM_OS were being added to the scripts in
    uppercase, which was inconsistent with rpmbuild
  * update to use Redline 1.1.10
  * add support to configure addParentDirs to not auto create parent
    directories for files

v1.2 - 2012-11-13
------
  * bugfix: Use a default value when InetAddress.getLocalHost() throws
    an UnknownHostException
  * internal: Configure gradle to also generate IntelliJ project files.
  * internal: convert to using nexus plugin for Sonatype OSS deployment
  * internal: upgrade to gradle 1.2

v1.1 - 2012-04-03
------
  * update to Gradle 1.0-milestone-9
  * change to use archivesBaseName rather than the project name directly
  * change to use extension properties rather than dynamic properties
  * update plugin to apply BasePlugin

v1.0 - 2012-01-03
------
  * update to use "fileType" instead of "directive" since the latter
    is apparently a reserved property on Closures

v0.9 - 2011-09-27
------
  * update to use Redline 1.1.9

v0.8 - 2011-08-04
------
  * add defines of RPM_ARCH, RPM_OS, RPM_PACKAGE_NAME, RPM_PACKAGE_VERSION,
    and RPM_PACKAGE_RELEASE to scripts to be consistent with rpmbuild

v0.7 - 2011-06-28
------
  * add support for a utilities script that automatically gets included in
    any pre/post/preun/postun scripts

v0.6 - 2011-06-27
------
  * add specification of required dependencies

v0.5 - 2011-06-10
------
  * add default for sourcePackage because yum createrepo assumes your rpm
    is a source package without it
  * default packageName to the project.name

v0.4 - 2011-06-06
------
  * initial Maven Central release
