package com.netflix.gradle.plugins.packaging

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Trigger implements Serializable {
    // File or Package Name
    Dependency dependency
    final File command

    Trigger(Dependency dependency, File command) {
        assert command, "Trigger script is required"
        this.dependency = dependency
        this.command = command
    }
}
