package com.netflix.gradle.plugins.daemon

import spock.lang.Specification

class LegacyInstallCmdSpec extends Specification {
    def 'should call chkconfig if redhat'() {
        when:
        String actual = LegacyInstallCmd.create(isRedHat: true, daemonName: 'ABC')

        then:
        actual == "/sbin/chkconfig ABC on"
    }

    def 'should call update-rc.d if not redhat'() {
        when:
        String actual = LegacyInstallCmd.create(isRedHat: false, daemonName: 'ABC', runLevels: [3, 4, 5], startSequence: 20, stopSequence: 21)

        then:
        actual == "/usr/sbin/update-rc.d ABC start 20 3 4 5 . stop 21 0 1 2 6 ."
    }
}
