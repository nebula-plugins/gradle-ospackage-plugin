package com.netflix.gradle.plugins.packaging

import com.netflix.gradle.plugins.BaseIntegrationTestKitSpec
import com.netflix.gradle.plugins.deb.Scanner
import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import spock.lang.Requires
import spock.lang.Unroll

import static org.redline_rpm.header.Header.HeaderTag.DISTRIBUTION
import static org.redline_rpm.header.Header.HeaderTag.RELEASE
import static org.redline_rpm.header.Header.HeaderTag.SUMMARY

class LegacyPackagingConventionsLauncherSpec extends BaseIntegrationTestKitSpec {
    @Requires({ JavaVersion.current() <= JavaVersion.VERSION_21 })
    @Unroll
    def 'legacy extension conventions preserve #release metadata on Gradle #testedGradleVersion'() {
        given:
        gradleVersion = testedGradleVersion
        file('src/app.txt').text = 'content'
        buildFile << """
            plugins {
                id 'com.netflix.nebula.ospackage'
            }
            version = '2.580.1-snapshot.20261023+6fa5b01'
            ospackage {
                packageName = 'nflx-jenkins'
                from('src') { into '/apps/jenkins' }
            }
            // Consumers can realize tasks before another plugin supplies its defaults.
            tasks.named('buildDeb').get()
            tasks.named('buildRpm').get()
            ext.buildRelease = '${release}'
            afterEvaluate {
                ospackage.conventionMapping.map('release') { project.buildRelease }
                ospackage.conventionMapping.map('version') { '2.580.1-snapshot' }
                ospackage.conventionMapping.map('distribution') { 'nflx' }
                ospackage.conventionMapping.map('summary') { 'Jenkins package' }
            }
        """.stripIndent()

        when:
        def first = runTasks('buildDeb', 'buildRpm', '--configuration-cache')
        def second = runTasks('buildDeb', 'buildRpm', '--configuration-cache')

        then:
        first.task(':buildDeb').outcome == TaskOutcome.SUCCESS
        first.task(':buildRpm').outcome == TaskOutcome.SUCCESS
        second.output.contains('Configuration cache entry reused')
        def deb = new File(projectDir, "build/distributions/nflx-jenkins_2.580.1~snapshot-${release}_all.deb")
        deb.exists()
        new Scanner(deb).getHeaderEntry('Version') == "2.580.1~snapshot-${release}"
        def rpmFile = new File(projectDir, "build/distributions/nflx-jenkins-2.580.1~snapshot-${release}.noarch.rpm")
        rpmFile.exists()
        def rpm = com.netflix.gradle.plugins.rpm.Scanner.scan(rpmFile)
        com.netflix.gradle.plugins.rpm.Scanner.getHeaderEntryString(rpm, RELEASE) == release
        com.netflix.gradle.plugins.rpm.Scanner.getHeaderEntryString(rpm, DISTRIBUTION) == 'nflx'
        com.netflix.gradle.plugins.rpm.Scanner.getHeaderEntryString(rpm, SUMMARY) == 'Jenkins package'

        where:
        release            | testedGradleVersion
        'h11145.6fa5b01'    | GradleVersion.current().version
        'LOCAL'            | GradleVersion.current().version
        'h11145.6fa5b01'    | '8.6'
    }

    @Unroll
    def 'explicit #configuration overrides legacy extension defaults'() {
        given:
        file('src/app.txt').text = 'content'
        buildFile << """
            plugins {
                id 'com.netflix.nebula.ospackage'
            }
            version = '2.580.1'
            ospackage {
                packageName = 'nflx-jenkins'
                from('src')
                conventionMapping.map('release') { 'h11145.6fa5b01' }
            }
            tasks.named('buildDeb').get()
            ${configuration}
        """.stripIndent()

        when:
        def result = runTasks('buildDeb', '--configuration-cache')

        then:
        result.task(':buildDeb').outcome == TaskOutcome.SUCCESS
        def deb = new File(projectDir, 'build/distributions/nflx-jenkins_2.580.1-custom_all.deb')
        deb.exists()
        new Scanner(deb).getHeaderEntry('Version') == '2.580.1-custom'

        where:
        configuration << [
                "ospackage.release = 'custom'",
                "ospackage.options.release.set('custom')",
                "tasks.named('buildDeb') { release = 'custom' }",
                "tasks.named('buildDeb') { exten.options.release.set('custom') }"
        ]
    }

    def 'legacy file and collection conventions populate debian control files'() {
        given:
        file('src/app.txt').text = 'content'
        file('scripts/postinst').text = 'echo legacy postinstall'
        buildFile << '''
            plugins {
                id 'com.netflix.nebula.ospackage'
            }
            version = '1.0.0'
            ospackage {
                packageName = 'legacy-controls'
                from('src')
                conventionMapping.map('postInstallFile') { file('scripts/postinst') }
                conventionMapping.map('preInstallCommands') { ['echo legacy preinstall'] }
                conventionMapping.map('customFields') { ['Build-Origin': 'legacy'] }
            }
        '''.stripIndent()

        when:
        runTasks('buildDeb', '--configuration-cache')
        def second = runTasks('buildDeb', '--configuration-cache')

        then:
        second.output.contains('Configuration cache entry reused')
        def archive = new File(projectDir, 'build/distributions/legacy-controls_1.0.0_all.deb')
        archive.exists()
        def scanner = new Scanner(archive)
        scanner.controlContents['./postinst'].contains('echo legacy postinstall')
        scanner.controlContents['./preinst'].contains('echo legacy preinstall')
        scanner.getHeaderEntry('Build-Origin') == 'legacy'
    }

    def 'legacy numeric boolean and enum defaults shape debian metadata and file ownership'() {
        given:
        file('src/config/app.txt').text = 'content'
        buildFile << '''
            plugins {
                id 'com.netflix.nebula.ospackage'
            }
            version = '1.0.0'
            ospackage {
                from('src') { into '/apps/metadata' }
            }
            tasks.named('buildDeb').get()
            afterEvaluate {
                def mapping = ospackage.conventionMapping
                mapping.map('packageName') { 'legacy-metadata' }
                mapping.map('epoch') { 2 }
                mapping.map('uid') { 1001 }
                mapping.map('gid') { 1002 }
                mapping.map('user') { 'legacy-user' }
                mapping.map('permissionGroup') { 'legacy-group' }
                mapping.map('createDirectoryEntry') { true }
                mapping.map('setgid') { true }
                mapping.map('multiArch') { com.netflix.gradle.plugins.deb.control.MultiArch.FOREIGN }
                mapping.map('maintainer') { 'Legacy Maintainer' }
            }
        '''.stripIndent()

        when:
        def first = runTasks('buildDeb', '--configuration-cache')
        def second = runTasks('buildDeb', '--configuration-cache')

        then:
        first.task(':buildDeb').outcome == TaskOutcome.SUCCESS
        second.output.contains('Configuration cache entry reused')
        def archive = new File(projectDir, 'build/distributions/legacy-metadata_1.0.0_all.deb')
        archive.exists()
        def scanner = new Scanner(archive)
        scanner.getHeaderEntry('Version') == '2:1.0.0'
        scanner.getHeaderEntry('Multi-Arch') == 'foreign'
        scanner.getHeaderEntry('Maintainer') == 'Legacy Maintainer'
        def entry = scanner.getEntry('./apps/metadata/config/app.txt')
        entry.userId == 1001
        entry.groupId == 1002
        entry.userName == 'legacy-user'
        entry.groupName == 'legacy-group'
        (scanner.getEntry('./apps/metadata/config/').mode & 02000) != 0
    }
}
