package com.netflix.gradle.plugins.deb

import com.netflix.gradle.plugins.packaging.SystemPackagingPlugin
import com.netflix.gradle.plugins.utils.GradleUtils
import nebula.test.IntegrationSpec
import spock.lang.Issue
import spock.lang.Unroll

class DebPluginLauncherSpec extends IntegrationSpec {
    def 'not up-to-date when specifying any value'() {
        when:
        writeHelloWorld('nebula.test')
        buildFile << applyPlugin(SystemPackagingPlugin)
        buildFile << '''
            ospackage {
            }
        '''.stripIndent()

        runTasksSuccessfully('buildDeb')

        then:
        !wasUpToDate(':buildDeb')
    }

    def 'not up-to-date when specifying a value and not files'() {
        when:
        buildFile << applyPlugin(SystemPackagingPlugin)
        buildFile << '''
            buildDeb {
                summary 'No copy spec values, but still not up to date'
            }
        '''.stripIndent()

        runTasksSuccessfully('buildDeb')

        then:
        !wasUpToDate(':buildDeb')
    }

    def 'not up-to-date when specifying a value on task'() {
        when:
        // Run once with a file
        writeHelloWorld('nebula.test')
        buildFile << applyPlugin(SystemPackagingPlugin)
        buildFile << '''
            buildDeb {
                from('src')
            }
        '''.stripIndent()

        runTasksSuccessfully('buildDeb')

        then:
        !wasUpToDate(':buildDeb')

        when:
        // Nothing changing
        buildFile << '''
            // Adding nothing.
        '''.stripIndent()

        runTasksSuccessfully('buildDeb')

        then:
        wasUpToDate(':buildDeb')

        when:
        // Adding a non-file input
        buildFile << '''
            buildDeb {
                preInstall('echo Hello')
            }
        '''.stripIndent()

        runTasksSuccessfully('buildDeb')

        then:
        !wasUpToDate(':buildDeb')
    }
    def 'not up-to-date when specifying a value on extension'() {
        when:
        // Run once with a file
        writeHelloWorld('nebula.test')
        buildFile << applyPlugin(SystemPackagingPlugin)
        buildFile << '''
            buildDeb {
                from('src')
            }
        '''.stripIndent()

        runTasksSuccessfully('buildDeb')

        then:
        !wasUpToDate(':buildDeb') // Need to run once with a file input

        when:
        // Nothing changing
        buildFile << '''
            // Adding nothing.
        '''.stripIndent()

        runTasksSuccessfully('buildDeb')

        then:
        wasUpToDate(':buildDeb')

        when:
        // Adding a non-file input
        buildFile << '''
            buildDeb {
                summary = '@Input is on delegate'
            }
        '''.stripIndent()

        runTasksSuccessfully('buildDeb')

        then:
        !wasUpToDate(':buildDeb')
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/104")
    @Unroll
    def "Translates extension packageDescription '#description' to header entry for Debian task"() {
        given:
        File libDir = new File(projectDir, 'lib')
        libDir.mkdirs()
        new File(libDir, 'a.java').text = "public class A { }"


        buildFile << """
apply plugin: 'com.netflix.nebula.ospackage'

ospackage {
    packageName = 'translates-extension-description'
    packageDescription = ${GradleUtils.quotedIfPresent(description)}
    version = '1.0'
    from('lib') {
            into 'lib'
    }
}


"""

        when:
        runTasksSuccessfully('buildDeb', '-i')

        then:
        def scan = new Scanner(file('build/distributions/translates-extension-description_1.0_all.deb'))
        scan.getHeaderEntry('Description') == headerEntry

        where:
        description             | headerEntry
        'This is a description' | 'translates-extension-description\n This is a description'
        ''                      | 'translates-extension-description'
        null                    | 'translates-extension-description'
    }
}
