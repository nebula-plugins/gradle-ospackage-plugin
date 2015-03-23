package com.netflix.gradle.plugins.deb.validation

import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

class DebVersionValidatorTest extends Specification {
    DebVersionAttributeValidator validator = new DebVersionAttributeValidator()

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/85")
    @Unroll
    def "verifies #description: '#version'"() {
        when:
        boolean valid = validator.validate(version)

        then:
        valid == result

        where:
        version | result | description
        '1.0'   | true   | 'valid version'
        'a.0'   | false  | 'version not starting with a digit'
        '1.^'   | false  | 'version with an invalid character'
    }

    def "provide error message"() {
        when:
        String errorMessage = validator.getErrorMessage('1.^')

        then:
        errorMessage == "Invalid upstream version '1.^' - a valid version must start with a digit and only contain [A-Za-z0-9.+:~-]"
    }
}
