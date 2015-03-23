package com.netflix.gradle.plugins.rpm.validation

import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

class RpmPackageNameAttributeValidatorTest extends Specification {
    RpmPackageNameAttributeValidator validator = new RpmPackageNameAttributeValidator()

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/90")
    @Unroll
    def "verifies #description: '#attribute'"() {
        when:
        boolean valid = validator.validate(attribute)

        then:
        valid == result

        where:
        attribute       | result | description
        'aBc_hello-1.0' | true   | 'valid package name with mixed alphanumeric characters'
        'abc^'          | false  | 'package name with an invalid character'
    }

    def "provide error message"() {
        when:
        String errorMessage = validator.getErrorMessage('abc^')

        then:
        errorMessage == "Invalid package name 'abc^' - a valid package name must only contain [a-zA-Z0-9-._+]"
    }
}
