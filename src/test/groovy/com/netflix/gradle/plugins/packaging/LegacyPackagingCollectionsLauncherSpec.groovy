package com.netflix.gradle.plugins.packaging

import com.netflix.gradle.plugins.BaseIntegrationTestKitSpec
import com.netflix.gradle.plugins.deb.Scanner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import spock.lang.Unroll

class LegacyPackagingCollectionsLauncherSpec extends BaseIntegrationTestKitSpec {
    @Unroll
    def 'legacy #operation selects the replacement post-install script on Gradle #testedGradleVersion'() {
        given:
        gradleVersion = testedGradleVersion
        file('src/app.txt').text = 'content'
        file('scripts/inherited.sh').text = 'echo inherited file command'
        def selectedScript = '#!/bin/sh\necho selected post-install script\n'
        file('scripts/postinst.sh').text = selectedScript
        buildFile << """
            plugins {
                id 'com.netflix.nebula.ospackage'
            }
            version = '1.0.0'
            ospackage {
                packageName = 'legacy-collections'
                from('src') { into '/opt/app' }
                postInstall file('scripts/inherited.sh')
                postInstall 'echo inherited string command'
            }
            // Realize the task before replacing commands supplied by another plugin.
            tasks.named('buildDeb').get()
            gradle.projectsEvaluated {
                def extension = extensions.getByName('ospackage')
                // Exercise the view installed by the legacy convention bridge as well.
                assert extension.postInstallCommands.size == 2
                ${operation}
                assert extension.postInstallCommands == []
                assert extension.options.postInstallCommands.get() == []
                extension.postInstallFile(file('scripts/postinst.sh'))
            }
        """.stripIndent()

        when:
        def first = runTasks('buildDeb', '--configuration-cache')
        def second = runTasks('buildDeb', '--configuration-cache')

        then:
        first.task(':buildDeb').outcome == TaskOutcome.SUCCESS
        second.output.contains('Configuration cache entry reused')
        def archive = new File(projectDir, 'build/distributions/legacy-collections_1.0.0_all.deb')
        archive.exists()
        new Scanner(archive).controlContents['./postinst'] == selectedScript

        where:
        [testedGradleVersion, operation] << [
            [GradleVersion.current().version, '9.7.1', '9.9.0-rc-1'],
            ['extension.postInstallCommands.removeRange(0, extension.postInstallCommands.size)',
             'extension.postInstallCommands.clear()']
        ].combinations()
    }
}
