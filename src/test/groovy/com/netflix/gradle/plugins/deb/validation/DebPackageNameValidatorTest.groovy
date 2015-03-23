package com.netflix.gradle.plugins.deb.validation

import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

class DebPackageNameValidatorTest extends Specification {
    DebPackageNameAttributeValidator validator = new DebPackageNameAttributeValidator()

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/90")
    @Unroll
    def "verifies #description: '#attribute'"() {
        when:
        boolean valid = validator.validate(attribute)

        then:
        valid == result

        where:
        attribute            | result | description
        'abc-1.0'            | true   | 'valid package name with mixed alphanumeric characters'
        'My-Awesome-Package' | false  | 'package with upper case characters'
        'a'                  | false  | 'package name too short'
        '-abc'               | false  | 'package name does not start with alphanumeric character'
        'abc^'               | false  | 'package name with an invalid character'
    }

    def "provide error message"() {
        when:
        String errorMessage = validator.getErrorMessage('a')

        then:
        errorMessage == "Invalid package name 'a' - a valid package name must start with an alphanumeric character, have a length of at least two characters and only contain [A-Za-z0-9.+-]"
    }
}
