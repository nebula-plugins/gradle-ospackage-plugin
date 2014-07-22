package com.netflix.gradle.plugins.deb

import com.netflix.gradle.plugins.packaging.SystemPackagingPlugin
import nebula.test.IntegrationSpec

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
}
