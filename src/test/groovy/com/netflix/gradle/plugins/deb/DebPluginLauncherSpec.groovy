package com.netflix.gradle.plugins.deb

import com.netflix.gradle.plugins.packaging.SystemPackagingPlugin
import nebula.test.IntegrationSpec
import spock.lang.Ignore

class DebPluginLauncherSpec extends IntegrationSpec {

    /**
     * This is sorta cheating since a Gradle copy task will always be up to date if there's no input files
     * @return
     */
    def 'up-to-date with nothing'() {
        when:
        writeHelloWorld('nebula.test')
        buildFile << applyPlugin(SystemPackagingPlugin)
        buildFile << '''
            ospackage {
            }
        '''.stripIndent()

        then:
        runTasksSuccessfully('buildRpm', 'buildRpm')
        wasUpToDate(':buildRpm')
    }

    @Ignore('Copy tasks with source files will always be up to date. Suck.')
    def 'not up to date when specifying a value and not files'() {
        when:
        buildFile << applyPlugin(SystemPackagingPlugin)
        buildFile << '''
            buildDeb {
                summary 'No copy spec values, but still not up to date'
            }
        '''.stripIndent()

        then:
        runTasksSuccessfully('buildDeb')
        !wasUpToDate(':buildDeb')
    }

    def 'not up to date when specifying a value on task'() {
        when:
        // Run once with a file
        writeHelloWorld('nebula.test')
        buildFile << applyPlugin(SystemPackagingPlugin)
        buildFile << '''
            buildDeb {
                from('src')
            }
        '''.stripIndent()

        then:
        runTasksSuccessfully('buildDeb')
        !wasUpToDate(':buildDeb') // Need to run once with a file input

        when:
        // Nothing changing
        buildFile << '''
            // Adding nothing.
        '''.stripIndent()

        then:
        runTasksSuccessfully('buildDeb')
        wasUpToDate(':buildDeb')

        when:
        // Adding a non-file input
        buildFile << '''
            buildDeb {
                preInstall('echo Hello')
            }
        '''.stripIndent()

        then:
        runTasksSuccessfully('buildDeb')
        !wasUpToDate(':buildDeb')
    }
    def 'not up to date when specifying a value on extension'() {
        when:
        // Run once with a file
        writeHelloWorld('nebula.test')
        buildFile << applyPlugin(SystemPackagingPlugin)
        buildFile << '''
            buildDeb {
                from('src')
            }
        '''.stripIndent()

        then:
        runTasksSuccessfully('buildDeb')
        !wasUpToDate(':buildDeb') // Need to run once with a file input

        when:
        // Nothing changing
        buildFile << '''
            // Adding nothing.
        '''.stripIndent()

        then:
        runTasksSuccessfully('buildDeb')
        wasUpToDate(':buildDeb')

        when:
        // Adding a non-file input
        buildFile << '''
            buildDeb {
                summary = '@Input is on delegate'
            }
        '''.stripIndent()

        then:
        runTasksSuccessfully('buildDeb')
        !wasUpToDate(':buildDeb')
    }
}
