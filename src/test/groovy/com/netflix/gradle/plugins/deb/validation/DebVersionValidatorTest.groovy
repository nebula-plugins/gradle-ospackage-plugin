package com.netflix.gradle.plugins.deb.validation

import spock.lang.Specification
import spock.lang.Unroll

class DebVersionValidatorTest extends Specification {
    DebVersionValidator debianVersionValidator = new DebVersionValidator()

    @Unroll
    def "verifies #description: '#version'"() {
        when:
        boolean valid = debianVersionValidator.validate(version)

        then:
        valid == result

        where:
        version | result | description
        '1.0'   | true   | 'valid version'
        'a.0'   | false  | 'version not starting with a digit'
        '1.^'   | false  | 'version with an invalid character'
    }
}
