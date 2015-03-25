2.2.1
-----

* Issue #60: Upgrade JDeb library to latest version to avoid having to monkey batch code through reflection.
* Enabled declaration of Conflicts, Recommends, Suggests, Enhances, Pre-Depends, Breaks, and Replaces on DEB packages
* Pull request #88: Upgraded Redline library to version 1.2.1 and introduced epoch property to be configured. This is a
potentially BREAKING change as consumers might rely on the old `org.freecompany` package name
* Pull request #92: Fixed rpm task on hosts with incorrect hostname.
* Issue #85: Validation of Debian version attribute.
* Issue #90: Validation of Debian and RPM package name attribute.

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
