package com.netflix.gradle.plugins.deb

import com.netflix.gradle.plugins.BaseIntegrationTestKitSpec
import com.netflix.gradle.plugins.utils.GradleUtils
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Issue
import spock.lang.Unroll

class DebPluginLauncherSpec extends BaseIntegrationTestKitSpec {
    def 'legacy dependency changes made before packaging appear in the deb'() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << '''
            plugins {
                id 'com.netflix.nebula.ospackage'
            }
            version = '1.0.0'
            ospackage {
                from('src')
                requires('versioned-dependency')
            }
            def packaging = extensions.getByType(com.netflix.gradle.plugins.packaging.ProjectPackagingExtension)
            tasks.named('buildDeb') {
                doFirst {
                    packaging.requires('late-dependency')
                    packaging.dependencies.find { it.packageName == 'versioned-dependency' }.version = '2.0.0'
                }
            }
        '''.stripIndent()

        when:
        def result = runTasks('buildDeb', '--no-configuration-cache')

        then:
        result.task(':buildDeb').outcome == TaskOutcome.SUCCESS
        def archive = new File(projectDir, 'build/distributions').listFiles().find { it.name.endsWith('.deb') }
        archive.exists()
        def dependencies = new Scanner(archive).getHeaderEntry('Depends')
        dependencies.contains('late-dependency')
        dependencies.contains('versioned-dependency (2.0.0)')
    }

    def 'legacy and provider-backed extension views shape the same deb package'() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << '''
            plugins {
                id 'com.netflix.nebula.ospackage'
            }
            version = '1.0.0'
            ospackage {
                from('src')
                options.packageName.set(providers.provider { 'bridge-package' })
                options.dependencies.add(new com.netflix.gradle.plugins.packaging.Dependency('modern-dependency', '', 0))
                dependencies.add(new com.netflix.gradle.plugins.packaging.Dependency('legacy-dependency', '', 0))
            }
        '''.stripIndent()

        when:
        def first = runTasks('buildDeb', '--configuration-cache')
        def second = runTasks('buildDeb', '--configuration-cache')

        then:
        first.task(':buildDeb').outcome == TaskOutcome.SUCCESS
        second.output.contains('Configuration cache entry reused')
        def archive = new File(projectDir, 'build/distributions/bridge-package_1.0.0_all.deb')
        archive.exists()
        def dependencies = new Scanner(archive).getHeaderEntry('Depends')
        dependencies.contains('modern-dependency')
        dependencies.contains('legacy-dependency')
    }

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
