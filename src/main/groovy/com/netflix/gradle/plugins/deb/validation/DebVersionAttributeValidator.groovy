package com.netflix.gradle.plugins.deb.validation

import com.netflix.gradle.plugins.packaging.validation.SystemPackagingAttributeValidator
class DebVersionAttributeValidator implements SystemPackagingAttributeValidator {
    /**
     * Per <a href="http://manpages.ubuntu.com/manpages/quantal/man5/deb-version.5.html">Debian manpage</a> a valid
     * version has to follow these conventions:
     *
     * The upstream-version may contain only alphanumerics ("A-Za-
     * z0-9") and the characters . + - : ~ (full stop, plus, hyphen,
     * colon, tilde) and should start with a digit.
     *
     * @param version Provided version
     * @return Result
     */
    @Override
    boolean validate(String version) {
        startWithDigit(version) && matchesExpectedCharacters(version)
    }

    private boolean startWithDigit(String version) {
        Character.isDigit(version.charAt(0))
    }

    private boolean matchesExpectedCharacters(String version) {
        version ==~ /[A-Za-z0-9.+:~-]+/
    }

    @Override
    String getErrorMessage(String attribute) {
        "Invalid upstream version '$attribute' - a valid version must start with a digit and only contain [A-Za-z0-9.+:~-]"
    }
}
