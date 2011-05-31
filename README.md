# Gradle RPM plugin

This plugin provides Gradle-based assembly of RPM packages, typically for Linux distributions
derived from RedHat.  It leverages [Redline](http://redline-rpm.org/) Java library.

## Usage

    apply plugin: 'rpm'

    // ...

    buildscript {
        repositories {
            add(new org.apache.ivy.plugins.resolver.URLResolver()) {
                name = 'GitHub Gradle RPM Plugin'
                addArtifactPattern 'http://cloud.github.com/downloads/AlanKrueger/gradle-rpm-plugin/[module]-[revision].[ext]'
            }
            mavenCentral()
        }

        dependencies {
            classpath ':gradle-rpm-plugin:0.7'
            classpath 'org.freecompany.redline:redline:1.1.2'
        }
    }

    // ...

    task fooRpm(type: Rpm) {
        packageName = 'foo'
        version = '1.2.3'
        release = 1
        arch = I386
        os = LINUX
    
        into '/opt/foo'
        from jar.outputs.files
    }

## Task

The RPM plugin is a copy task, similar to the Zip and Tar tasks.

