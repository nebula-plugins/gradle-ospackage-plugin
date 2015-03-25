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
        attribute            | result | description
        'a25b'               | true   | 'valid package name with mixed alphanumeric characters'
        'my.awesome.package' | true   | 'package with dot characters'
        'my-awesome-package' | true   | 'package with dash characters'
        'my_awesome_package' | true   | 'package with underscore characters'
        'My-Awesome-Package' | true   | 'package with upper case characters'
        'abc^'               | false  | 'package name with an invalid character'
    }

    def "provide error message"() {
        when:
        String errorMessage = validator.getErrorMessage('abc^')

        then:
        errorMessage == "Invalid package name 'abc^' - a valid package name must only contain [a-zA-Z0-9-._+]"
    }
}
