package com.netflix.gradle.plugins.rpm.validation

import com.netflix.gradle.plugins.packaging.validation.SystemPackagingAttributeValidator

class RpmPackageNameAttributeValidator implements SystemPackagingAttributeValidator {
    /**
     * Per <a href="http://fedoraproject.org/wiki/Packaging:NamingGuidelines#Common_Character_Set_for_Package_Naming">RPM manpage</a> a valid
     * package name has to follow these conventions:
     *
     * abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._+
     *
     * @param packageName Package name
     * @return Result
     */
    @Override
    boolean validate(String packageName) {
        matchesExpectedCharacters(packageName)
    }

    private boolean matchesExpectedCharacters(String packageName) {
        packageName ==~ /[a-zA-Z0-9-._+]+/
    }

    @Override
    String getErrorMessage(String attribute) {
        "Invalid package name '$attribute' - a valid package name must only contain [a-zA-Z0-9-._+]"
    }
}
