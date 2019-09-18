package com.netflix.gradle.plugins.deb.validation

import com.netflix.gradle.plugins.packaging.validation.SystemPackagingAttributeValidator
class DebPackageNameAttributeValidator implements SystemPackagingAttributeValidator {
    /**
     * Per <a href=" https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Source">Debian manpage</a> a valid
     * package name has to follow these conventions:
     *
     * Package names (both source and binary, see Package, Section 5.6.7) must consist only of lower case letters (a-z),
     * digits (0-9), plus (+) and minus (-) signs, and periods (.). They must be at least two characters long and must
     * start with an alphanumeric character.
     *
     * @param packageName Package name
     * @return Result
     */
    @Override
    boolean validate(String packageName) {
        isAtLeastTwoCharactersLong(packageName) && startWithAlphanumeric(packageName) && matchesExpectedCharacters(packageName)
    }

    private boolean isAtLeastTwoCharactersLong(String packageName) {
        packageName.length() >= 2
    }

    private boolean startWithAlphanumeric(String packageName) {
        Character.isAlphabetic(packageName.codePointAt(0)) || Character.isDigit(packageName.charAt(0))
    }

    private boolean matchesExpectedCharacters(String packageName) {
        packageName ==~ /[a-z0-9.+-]+/
    }

    @Override
    String getErrorMessage(String attribute) {
        "Invalid package name '$attribute' - a valid package name must start with an alphanumeric character, have a length of at least two characters and only contain [a-z0-9.+-]"
    }
}
