package com.netflix.gradle.plugins.packaging.validation

interface SystemPackagingAttributeValidator {
    boolean validate(String attribute)
    String getErrorMessage(String attribute)
}