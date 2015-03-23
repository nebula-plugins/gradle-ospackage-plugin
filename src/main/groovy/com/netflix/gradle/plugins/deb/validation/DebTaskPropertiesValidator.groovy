package com.netflix.gradle.plugins.deb.validation

import com.netflix.gradle.plugins.deb.Deb
import com.netflix.gradle.plugins.packaging.validation.SystemPackagingAttributeValidator
import com.netflix.gradle.plugins.packaging.validation.SystemPackagingTaskPropertiesValidator
import org.gradle.api.InvalidUserDataException

class DebTaskPropertiesValidator implements SystemPackagingTaskPropertiesValidator<Deb> {
    private final SystemPackagingAttributeValidator versionValidator = new DebVersionAttributeValidator()
    private final SystemPackagingAttributeValidator packageNameValidator = new DebPackageNameAttributeValidator()

    @Override
    void validate(Deb task) {
        if(task.getVersion() != 'unspecified' && !versionValidator.validate(task.getVersion())) {
            throw new InvalidUserDataException(versionValidator.getErrorMessage(task.getVersion()))
        }

        if(!packageNameValidator.validate(task.getPackageName())) {
            throw new InvalidUserDataException(packageNameValidator.getErrorMessage(task.getPackageName()))
        }
    }
}
