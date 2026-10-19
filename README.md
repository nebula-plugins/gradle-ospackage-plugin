# Gradle Linux Packaging Plugin

![Support Status](https://img.shields.io/badge/nebula-active-green.svg)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.netflix.nebula/gradle-ospackage-plugin/maven-metadata.xml.svg?label=gradlePluginPortal)](https://plugins.gradle.org/plugin/com.netflix.nebula.ospackage)
[![Maven Central](https://img.shields.io/maven-central/v/com.netflix.nebula/gradle-ospackage-plugin)](https://maven-badges.herokuapp.com/maven-central/com.netflix.nebula/gradle-ospackage-plugin)
![Build](https://github.com/nebula-plugins/gradle-ospackage-plugin/actions/workflows/nebula.yml/badge.svg)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/gradle-ospackage-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)


This plugin provides Gradle-based assembly of system packages, typically for RedHat and Debian based distributions,
using a canonical Gradle Copy Specs. It's structured as three plugins, which work in concert, and a fourth plugin
to pull them all together. Keep reading to see some power examples, follow the links to further pages for the formal
documentation. All the plugins are pure-java and don't require any local native binaries.

# Quick Start

Refer to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.netflix.nebula.ospackage) for instructions on how to apply the main plugin.

# Documentation

The project wiki contains the [full documentation](https://github.com/nebula-plugins/gradle-ospackage-plugin/wiki) for the plugin.

## Migrating from 12.x to 13.0

Version 13 changes the programmatic APIs of the packaging, application, and daemon task extensions to use Gradle's lazy properties. Most Groovy build scripts that assign values in `ospackage { ... }` or `buildDeb { ... }` can keep doing so:

```groovy
ospackage {
    packageName = 'my-package'
    requires 'other-package'
}
```

Code that obtains an extension object and reads its values must now resolve its properties. Prefer wiring the property or a mapped provider into another Gradle property when the value is needed later.

```groovy
def packaging = extensions.getByType(com.netflix.gradle.plugins.packaging.ProjectPackagingExtension)

// 12.x: String name = packaging.packageName
String name = packaging.packageName.getOrNull()
packaging.packageName.set(providers.provider { 'my-package' })

// 12.x: packaging.preInstallFile = file('scripts/preinst')
packaging.preInstallFile.set(layout.projectDirectory.file('scripts/preinst'))

// Use the existing DSL method to add a dependency.
packaging.requires('other-package')
```

The same change applies to other scalar, file, list, and map properties on `SystemPackagingExtension` and `ProjectPackagingExtension`. Direct list or map mutation must use methods such as `.add(...)` or `.put(...)` on the property. Java and Kotlin callers must update their getter types from plain values to `Property`, `RegularFileProperty`, `ListProperty`, or `MapProperty`. Recompile plugins that call these APIs against 13.0.

`OspackageApplicationExtension` is now an interface with `Property<String>` getters for `prefix` and `distribution`. Read `prefix.get()` when a value is required, or use `prefix.map { ... }` to keep it lazy. Configure its default with `prefix.convention('/apps')` instead of `conventionMapping.map('prefix')`.

`DaemonTemplateTask` exposes `context`, `templates`, `destDir`, and `templatesFolder` as Gradle properties. Configure them with `.set(...)` or `.convention(...)` and resolve them with `.get()` only when needed. Custom subclasses of `SystemPackagingBasePlugin` that override `createExtension()` must update the override to `createExtension(Project)`.

The bundled Docker Gradle plugin moves from 3.2.1 to 10.0.0; builds that configure its task types directly should review that plugin's API. The Spring Boot application integration also excludes development-only dependencies from the packaged distribution.

LICENSE
=======

Copyright 2014-2022 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

