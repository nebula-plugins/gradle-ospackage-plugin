package com.netflix.gradle.plugins.rpm.core

import spock.lang.Specification

import static com.netflix.gradle.plugins.rpm.core.RpmTestFixtures.resource

class SignatureGeneratorSpec extends Specification {

    def 'uses the first signing key when no key id is given'() {
        expect:
        new SignatureGenerator(resource('pgp/secring.gpg'), null, 'redline').enabled
    }

    def 'finds a key by its id'() {
        expect:
        new SignatureGenerator(resource('pgp/secring.gpg'), '5A186608', 'redline').enabled
    }

    def 'is disabled when the passphrase is wrong'() {
        expect:
        !new SignatureGenerator(resource('pgp/secring.gpg'), null, 'wrong').enabled
    }

    def 'is disabled without a key ring'() {
        expect:
        !new SignatureGenerator(null, null, null).enabled
    }

    def 'fails for an unknown key id'() {
        when:
        new SignatureGenerator(resource('pgp/secring.gpg'), 'DEADBEEF', 'redline')

        then:
        IllegalArgumentException e = thrown()
        e.message == "Can't find signing key in key rings."
    }
}
