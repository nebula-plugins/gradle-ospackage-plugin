package com.netflix.gradle.plugins.deb

import com.netflix.gradle.plugins.BaseIntegrationTestKitSpec
import com.netflix.gradle.plugins.utils.GradleUtils
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Issue
import spock.lang.Unroll

class DebPluginLauncherSpec extends BaseIntegrationTestKitSpec {
    def 'not up-to-date when specifying any value'() {
        when:
        writeHelloWorld('nebula.test')
        buildFile << '''
            plugins {
                id 'com.netflix.nebula.ospackage'
            }
            ospackage {
            }
        '''.stripIndent()

        def results = runTasks('buildDeb')

        then:
        results.task(':buildDeb').outcome != TaskOutcome.UP_TO_DATE
    }

    def 'not up-to-date when specifying a value and not files'() {
        when:
        buildFile << '''
            plugins {
                id 'com.netflix.nebula.ospackage'
            }
            buildDeb {
                summary = 'No copy spec values, but still not up to date'
            }
        '''.stripIndent()

        def results = runTasks('buildDeb')

        then:
        results.task(':buildDeb').outcome != TaskOutcome.UP_TO_DATE
    }

    def 'not up-to-date when specifying a value on task'() {
        when:
        // Run once with a file
        writeHelloWorld('nebula.test')
        buildFile << '''
            plugins {
                id 'com.netflix.nebula.ospackage'
            }
            buildDeb {
                from('src')
            }
        '''.stripIndent()

        def results = runTasks('buildDeb')

        then:
        results.task(':buildDeb').outcome != TaskOutcome.UP_TO_DATE

        when:
        // Nothing changing
        buildFile << '''
            // Adding nothing.
        '''.stripIndent()

        def secondResult = runTasks('buildDeb')

        then:
        secondResult.task(':buildDeb').outcome == TaskOutcome.UP_TO_DATE

        when:
        // Adding a non-file input
        buildFile << '''
            buildDeb {
                preInstall('echo Hello')
            }
        '''.stripIndent()

        def thirdResult = runTasks('buildDeb')

        then:
        thirdResult.task(':buildDeb').outcome != TaskOutcome.UP_TO_DATE
    }

    def 'not up-to-date when specifying a value on extension'() {
        when:
        // Run once with a file
        writeHelloWorld('nebula.test')
        buildFile << '''
            plugins {
                id 'com.netflix.nebula.ospackage'
            }
            buildDeb {
                from('src')
            }
        '''.stripIndent()

        def results = runTasks('buildDeb')

        then:
        results.task(':buildDeb').outcome != TaskOutcome.UP_TO_DATE // Need to run once with a file input

        when:
        // Nothing changing
        buildFile << '''
            // Adding nothing.
        '''.stripIndent()

        def secondResult = runTasks('buildDeb')

        then:
        secondResult.task(':buildDeb').outcome == TaskOutcome.UP_TO_DATE

        when:
        // Adding a non-file input
        buildFile << '''
            buildDeb {
                summary = '@Input is on delegate'
            }
        '''.stripIndent()

        def thirdResult = runTasks('buildDeb')

        then:
        thirdResult.task(':buildDeb').outcome != TaskOutcome.UP_TO_DATE
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/104")
    @Unroll
    def "Translates extension packageDescription '#description' to header entry for Debian task"() {
        given:
        File libDir = new File(projectDir, 'lib')
        libDir.mkdirs()
        new File(libDir, 'a.java').text = "public class A { }"


        buildFile << """
plugins {
    id 'com.netflix.nebula.ospackage'
}

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
        runTasks('buildDeb', '-i')

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
