package com.netflix.gradle.plugins.deb.validation

interface VersionValidator {
    boolean validate(String version)
}