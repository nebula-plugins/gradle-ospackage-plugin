# Gradle Linux Packaging Plugin

![Support Status](https://img.shields.io/badge/nebula-active-green.svg)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.netflix.nebula/gradle-ospackage-plugin/maven-metadata.xml.svg?label=gradlePluginPortal)](https://plugins.gradle.org/plugin/nebula.ospackage)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.netflix.nebula/gradle-ospackage-plugin/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.netflix.nebula/gradle-ospackage-plugin)
![CI](https://github.com/nebula-plugins/gradle-ospackage-plugin/actions/workflows/ci.yml/badge.svg)
![Publish](https://github.com/nebula-plugins/gradle-ospackage-plugin/actions/workflows/publish.yml/badge.svg)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/gradle-ospackage-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)


This plugin provides Gradle-based assembly of system packages, typically for RedHat and Debian based distributions,
using a canonical Gradle Copy Specs. It's structured as three plugins, which work in concert, and a fourth plugin
to pull them all together. Keep reading to see some power examples, follow the links to further pages for the formal
documentation. All the plugins are pure-java and don't require any local native binaries.

# Quick Start

Refer to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/nebula.ospackage) for instructions on how to apply the main plugin.

# Documentation

The project wiki contains the [full documentation](https://github.com/nebula-plugins/gradle-ospackage-plugin/wiki) for the plugin.

# Gradle Compatibility Tested

Built with Oracle JDK8
Tested with Oracle JDK8

| Gradle Version | Works |
| :------------: | :---: |
| 4.10          | yes   |
| 5.0            | yes   |
| 5.1            | yes   |
| 5.2            | yes   |
| 5.3            | yes   |
| 5.4            | yes   |
| 5.5            | yes   |
| 5.6            | yes   |


LICENSE
=======

Copyright 2014-2019 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

