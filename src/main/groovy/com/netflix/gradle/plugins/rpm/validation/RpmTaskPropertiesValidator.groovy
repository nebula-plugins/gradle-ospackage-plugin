package com.netflix.gradle.plugins.rpm.validation

import com.netflix.gradle.plugins.packaging.validation.SystemPackagingAttributeValidator
import com.netflix.gradle.plugins.packaging.validation.SystemPackagingTaskPropertiesValidator
import com.netflix.gradle.plugins.rpm.Rpm
import org.gradle.api.InvalidUserDataException

class RpmTaskPropertiesValidator implements SystemPackagingTaskPropertiesValidator<Rpm> {
    private final SystemPackagingAttributeValidator packageNameValidator = new RpmPackageNameAttributeValidator()

    @Override
    void validate(Rpm task) {
        if(!packageNameValidator.validate(task.getPackageName())) {
            throw new InvalidUserDataException(packageNameValidator.getErrorMessage(task.getPackageName()))
        }
    }
}
