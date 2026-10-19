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

Existing value-style Groovy and Java APIs remain available. You can upgrade the plugin before changing build scripts that assign package values or use methods such as `requires(...)`:

```groovy
ospackage {
    packageName = 'my-package'
    requires 'other-package'
}
```

The packaging extension's old getters still return `String`, `File`, `List`, or `Map` values. Existing list and map mutations update the package. For new configuration, use the corresponding `*Property` accessor to wire providers lazily:

```groovy
def packaging = extensions.getByType(com.netflix.gradle.plugins.packaging.ProjectPackagingExtension)
packaging.packageNameProperty.set(providers.provider { 'my-package' })
packaging.dependenciesProperty.add(new com.netflix.gradle.plugins.packaging.Dependency('other-package', ''))
packaging.preInstallFileProperty.set(layout.projectDirectory.file('scripts/preinst'))
```

For example, `getPackageName()` returns a `String`, while `getPackageNameProperty()` returns a `Property<String>`. Pass providers to Gradle properties instead of calling `.get()` during configuration. The same pattern applies to file, list, and map properties. Mutating a legacy list or map reads its current value, so use the provider accessor when its source must stay lazy. `DaemonTemplateTask` also retains its value getters and exposes `contextProperty`, `templatesProperty`, `destDirProperty`, and `templatesFolderProperty` for lazy configuration. `OspackageApplicationExtension` remains a concrete class with `String` values for `prefix` and `distribution`; existing assignments and conventions continue to work.

Two less common uses require source changes:

| In 12.x | In 13.0 |
| --- | --- |
| `new SystemPackagingExtension()` | `project.objects.newInstance(SystemPackagingExtension)` |
| `task.exten = replacement` | Configure the existing `task.exten`, such as `task.exten.packageNameProperty.set(provider)` |

The no-argument `SystemPackagingBasePlugin.createExtension()` override remains supported. If you use a custom plugin subclass, you can migrate its override to `createExtension(Project)` when convenient. Use the `Project` argument only during plugin configuration; packaging tasks read their declared properties during execution.

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

