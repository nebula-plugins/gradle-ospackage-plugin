package com.netflix.gradle.plugins.application

import org.gradle.testfixtures.ProjectBuilder
import com.netflix.gradle.plugins.deb.Deb
import spock.lang.Specification

class OspackageApplicationExtensionBridgeTest extends Specification {
    def "legacy application accessors and options share the same values"() {
        given:
        def extension = ProjectBuilder.builder().build().objects.newInstance(OspackageApplicationExtension)

        when:
        extension.prefix = '/legacy'
        extension.options.distribution.set('stable')

        then:
        extension.options.prefix.get() == '/legacy'
        extension.distribution == 'stable'

        when:
        extension.options.prefix.set('/modern')

        then:
        extension.prefix == '/modern'
    }

    def "application extension retains its 12.x type and getter descriptors"() {
        expect:
        !OspackageApplicationExtension.isInterface()
        OspackageApplicationExtension.getConstructor() != null
        OspackageApplicationExtension.getMethod('getPrefix').returnType == String
        OspackageApplicationExtension.getMethod('getDistribution').returnType == String
        Deb.getMethod('getArchStr').returnType == String
    }

    def "directly constructed legacy application extensions retain plain values"() {
        given:
        def extension = new OspackageApplicationExtension()

        when:
        extension.prefix = '/legacy'
        extension.distribution = 'stable'

        then:
        extension.prefix == '/legacy'
        extension.distribution == 'stable'
    }
}
