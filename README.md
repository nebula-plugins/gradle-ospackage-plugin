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

Version 13 adds a provider-backed `options` view to the packaging and application extensions while retaining their 12.x plain-value getters during migration. Most Groovy build scripts that assign values in `ospackage { ... }` or `buildDeb { ... }` can keep doing so:

```groovy
ospackage {
    packageName = 'my-package'
    requires 'other-package'
}
```

Code that needs lazy values should use `options`. Prefer wiring a property or a mapped provider into another Gradle property when the value is needed later. Plain-value getters remain available for existing consumers but are deprecated.

```groovy
def packaging = extensions.getByType(com.netflix.gradle.plugins.packaging.ProjectPackagingExtension)

String name = packaging.packageName // legacy, eagerly reads the value
packaging.options.packageName.set(providers.provider { 'my-package' })

packaging.options.preInstallFile.set(layout.projectDirectory.file('scripts/preinst'))

// Use the existing DSL method to add a dependency.
packaging.requires('other-package')
```

The same `options` view covers other scalar, file, list, and map values on `SystemPackagingExtension` and `ProjectPackagingExtension`. Use `.add(...)` and `.put(...)` on the corresponding Gradle properties. The legacy list and map getters remain mutable views backed by those properties. Java and Kotlin callers that obtain Gradle-created extensions can keep their 12.x getter types or migrate to `getOptions()`; new code should use `getOptions()`. Direct construction of `SystemPackagingExtension` is outside this bridge.

Script command and control-file lists still accept both `String` and `File`; their provider-backed form is `ListProperty<Object>`.

Legacy defaults registered on the `ospackage` extension through `conventionMapping`, including CI release metadata, are adopted after all projects finish configuration. Packaging tasks inherit these defaults even if they were realized earlier. Explicit values set through either API take precedence. New plugin code should use `options.release.convention(...)` and the corresponding provider-backed properties. Legacy mapping closures are resolved during configuration; they are not retained for task execution.

The 12.x helper methods and mutable collection getters on `buildDeb` and `buildRpm` tasks also remain available. New plugin code should use `task.exten.options` to wire providers into packaging task settings. The old task `setExten(...)` setter is unavailable because Gradle now creates `exten` as a managed nested object; configure that object instead of replacing it.

`OspackageApplicationExtension` remains a class with `String` getters for `prefix` and `distribution`. Use `options.prefix.get()` when a value is required, or `options.prefix.map { ... }` to keep it lazy. Existing `conventionMapping.map('prefix')` defaults are honored; migrate them to `options.prefix.convention('/apps')` when updating the caller.

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

