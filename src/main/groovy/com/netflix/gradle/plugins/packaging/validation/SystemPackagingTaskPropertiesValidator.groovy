package com.netflix.gradle.plugins.packaging.validation

import org.gradle.api.Task

interface SystemPackagingTaskPropertiesValidator<T extends Task> {
    void validate(T task)
}
