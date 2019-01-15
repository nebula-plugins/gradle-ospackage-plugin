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

# Quick Start

Refer to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/nebula.ospackage) for instructions on how to apply the main plugin.

# Documentation

The project wiki contains the [full documentation](https://github.com/nebula-plugins/gradle-ospackage-plugin/wiki) for the plugin.

# Gradle Compatibility Tested

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
| 2.11           | yes   |
| 2.12           | yes   |
| 2.13           | yes   |

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

