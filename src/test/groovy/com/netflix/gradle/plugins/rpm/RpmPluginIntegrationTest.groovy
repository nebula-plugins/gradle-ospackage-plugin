package com.netflix.gradle.plugins.rpm

import nebula.test.IntegrationSpec
import nebula.test.ProjectSpec
import spock.lang.Issue

class RpmPluginIntegrationTest extends IntegrationSpec {
    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/82")
    def "rpm task is marked up-to-date when setting arch or os property"() {

            given:
        buildFile << '''
apply plugin: 'nebula.rpm'

task buildRpm(type: Rpm) {
    packageName = 'rpmIsUpToDate'
    arch = NOARCH
    os = LINUX
}
'''
        when:
        runTasksSuccessfully('buildRpm')

        and:
        def result = runTasksSuccessfully('buildRpm')

        then:
        result.wasUpToDate(':buildRpm')
    }
}
