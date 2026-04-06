/*
 * Copyright 2014-2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.gradle.plugins

import spock.lang.Ignore

/**
 * Tests that verify configuration cache support for Debian packaging.
 * These tests run tasks twice to ensure the configuration cache is stored and reused.
 */
class ConfigurationCacheSpec extends BaseIntegrationTestKitSpec {

    def 'deb plugin works with configuration cache'() {
        given:
        buildFile << """
            plugins {
                id 'com.netflix.nebula.deb'
            }

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'test-package'
                version = '1.0.0'

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        // Create a dummy file to package
        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'test.txt').text = 'test content'

        when: 'run myDeb first time - stores configuration cache'
        def result1 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run myDeb second time - reuses configuration cache'
        def result2 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'build artifact exists'
        new File(projectDir, "build/distributions/test-package_1.0.0_all.deb").exists()
    }

    def 'daemon plugin works with configuration cache'() {
        given:
        buildFile << """
            plugins {
                id 'com.netflix.nebula.ospackage-daemon'
                id 'com.netflix.nebula.deb'
            }

            daemon {
                daemonName = 'test-daemon'
                command = 'sleep infinity'
            }

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'daemon-test'
                version = '1.0.0'
            }
        """.stripIndent()

        when: 'run myDeb first time'
        def result1 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run myDeb second time'
        def result2 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created'
        new File(projectDir, 'build/distributions/daemon-test_1.0.0_all.deb').exists()
    }

    def 'application plugin works with configuration cache'() {
        given:
        buildFile << """
            plugins {
                id 'application'
                id 'com.netflix.nebula.ospackage-application'
            }

            application {
                mainClass = 'com.test.Main'
            }

            ospackage_application {
                prefix = '/opt'
            }
        """.stripIndent()

        // Create a minimal Main class
        def srcDir = new File(projectDir, 'src/main/java/com/test')
        srcDir.mkdirs()
        new File(srcDir, 'Main.java').text = """
            package com.test;
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello");
                }
            }
        """.stripIndent()

        when: 'run buildDeb first time'
        def result1 = runTasks('buildDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run buildDeb second time'
        def result2 = runTasks('buildDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')
    }

    def 'multiple daemons work with configuration cache'() {
        given:
        buildFile << """
            plugins {
                id 'com.netflix.nebula.ospackage-daemon'
                id 'com.netflix.nebula.deb'
            }

            daemons {
                daemon {
                    daemonName = 'daemon1'
                    command = 'sleep infinity'
                }
                daemon {
                    daemonName = 'daemon2'
                    command = 'exit 0'
                    user = 'nobody'
                }
            }

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'multi-daemon'
                version = '1.0.0'
            }
        """.stripIndent()

        when: 'run myDeb first time'
        def result1 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run myDeb second time'
        def result2 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created'
        new File(projectDir, 'build/distributions/multi-daemon_1.0.0_all.deb').exists()
    }

    def 'custom daemon templates work with configuration cache'() {
        given:
        // Create custom template directory
        def templatesDir = new File(projectDir, 'templates')
        templatesDir.mkdirs()
        new File(templatesDir, 'initd.tpl').text = '''#!/bin/sh
# Custom init script for ${daemonName}
'''.stripIndent()
        new File(templatesDir, 'run.tpl').text = '''#!/bin/sh
exec ${command}
'''.stripIndent()
        new File(templatesDir, 'log-run.tpl').text = '''#!/bin/sh
exec multilog t ./main
'''.stripIndent()
        new File(templatesDir, 'postInstall.tpl').text = ''

        buildFile << """
            plugins {
                id 'com.netflix.nebula.ospackage-daemon'
                id 'com.netflix.nebula.deb'
            }

            daemonsTemplates {
                folder = 'templates'
            }

            daemon {
                daemonName = 'custom-daemon'
                command = 'sleep infinity'
            }

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'custom-templates'
                version = '1.0.0'
            }
        """.stripIndent()

        when: 'run myDeb first time'
        def result1 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run myDeb second time'
        def result2 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created'
        new File(projectDir, 'build/distributions/custom-templates_1.0.0_all.deb').exists()
    }

    def 'ospackage-base plugin works with configuration cache'() {
        given:
        buildFile << """
            plugins {
                id 'com.netflix.nebula.ospackage-base'
            }

            tasks.register('customDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'custom-package'
                version = '2.0.0'

                from('src') {
                    into '/opt/custom'
                }
            }
        """.stripIndent()

        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'custom.txt').text = 'custom content'

        when: 'run customDeb first time'
        def result1 = runTasks('customDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run customDeb second time'
        def result2 = runTasks('customDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created'
        new File(projectDir, 'build/distributions/custom-package_2.0.0_all.deb').exists()
    }

    def 'complex deb configuration works with configuration cache'() {
        given:
        buildFile << """
            plugins {
                id 'com.netflix.nebula.deb'
            }

            tasks.register('complexDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'complex-package'
                version = '3.0.0'

                // Multiple from blocks
                from('src1') {
                    into '/opt/app1'
                }
                from('src2') {
                    into '/opt/app2'
                }

                // Links
                link('/usr/bin/myapp', '/opt/app1/myapp')

                // PreInstall/PostInstall
                preInstall 'echo "Installing..."'
                postInstall 'echo "Installed!"'
            }
        """.stripIndent()

        // Create source directories
        ['src1', 'src2'].each { dir ->
            def srcDir = new File(projectDir, dir)
            srcDir.mkdirs()
            new File(srcDir, 'file.txt').text = "content in ${dir}"
        }

        when: 'run complexDeb first time'
        def result1 = runTasks('complexDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run complexDeb second time'
        def result2 = runTasks('complexDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created'
        new File(projectDir, "build/distributions/complex-package_3.0.0_all.deb").exists()
    }

    def 'buildDeb task works with configuration cache'() {
        given:
        buildFile << """
            plugins {
                id 'com.netflix.nebula.ospackage'
            }

            ospackage {
                packageName = 'simple-test'
                version = '1.0'

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'application content'

        when: 'run buildDeb first time'
        def result1 = runTasks('buildDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run buildDeb second time'
        def result2 = runTasks('buildDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created'
        new File(projectDir, "build/distributions/simple-test_1.0_all.deb").exists()
    }

    def 'no Task.getProject() deprecation warnings when using project version description and supplementaryControl'() {
        given:
        def srcDir = new File(projectDir, 'src/app')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'app content'
        def controlFile = new File(projectDir, 'src/changelog')
        controlFile.text = 'Initial release'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.deb'
            }

            version = '1.0.0'
            description = 'My application'

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'deprecation-test'
                supplementaryControl 'src/changelog'

                from('src/app') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when:
        def result = runTasks('myDeb', '--warning-mode', 'all')

        then: 'no Task.project at execution time deprecation warning'
        !result.output.contains('Task.project at execution time has been deprecated')
        !result.output.contains('Invocation of Task.project at execution time')
    }

    def 'ospackage parent extension propagates version and user to child tasks with configuration cache'() {
        given:
        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'app content'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.ospackage'
            }

            ospackage {
                packageName = 'parent-ext-test'
                version = '3.0.0'
                user = 'myuser'

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when: 'run buildDeb first time'
        def result1 = runTasks('buildDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run buildDeb second time'
        def result2 = runTasks('buildDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created with values from parent extension'
        new File(projectDir, 'build/distributions/parent-ext-test_3.0.0_all.deb').exists()
    }

    def 'rpm derives version from project.version with configuration cache'() {
        given:
        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'app content'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.rpm'
            }

            version = '2.0.0'

            tasks.register('myRpm', com.netflix.gradle.plugins.rpm.Rpm) {
                packageName = 'rpm-project-version-test'
                release = '1'

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when: 'run myRpm first time'
        def result1 = runTasks('myRpm', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run myRpm second time'
        def result2 = runTasks('myRpm', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created with version from project'
        new File(projectDir, 'build/distributions/rpm-project-version-test-2.0.0-1.noarch.rpm').exists()
    }

    def 'packager convention propagates to user maintainer and uploaders with configuration cache'() {
        given:
        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'app content'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.deb'
            }

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'packager-test'
                version = '1.0.0'
                packager = 'Test Packager'
                // user, maintainer, uploaders intentionally not set — should default to packager

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when: 'run myDeb first time'
        def result1 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run myDeb second time'
        def result2 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created'
        new File(projectDir, 'build/distributions/packager-test_1.0.0_all.deb').exists()
    }

    def 'signing key ring file convention works with configuration cache'() {
        given:
        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'app content'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.deb'
            }

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'signing-convention-test'
                version = '1.0.0'
                // Configure signing key ID and passphrase — ring file uses the lazy convention
                // that checks ~/.gnupg/secring.gpg at execution time (not config time)
                signingKeyId = 'ABCD1234'
                signingKeyPassphrase = 'test-passphrase'

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when: 'run myDeb first time'
        def result1 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run myDeb second time'
        def result2 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created (unsigned since ring file does not exist)'
        new File(projectDir, 'build/distributions/signing-convention-test_1.0.0_all.deb').exists()
    }

    def 'supplementaryControl files work with configuration cache'() {
        given:
        def controlFile = new File(projectDir, 'src/changelog')
        controlFile.parentFile.mkdirs()
        controlFile.text = 'Initial release'

        def srcDir = new File(projectDir, 'src/app')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'app content'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.deb'
            }

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'supplementary-test'
                version = '1.0.0'
                supplementaryControl 'src/changelog'

                from('src/app') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when: 'run myDeb first time'
        def result1 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run myDeb second time'
        def result2 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created'
        new File(projectDir, 'build/distributions/supplementary-test_1.0.0_all.deb').exists()
    }

    def 'version and description derived from project work with configuration cache'() {
        given:
        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'app content'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.deb'
            }

            version = '2.5.0'
            description = 'My application'

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'project-defaults-test'

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when: 'run myDeb first time'
        def result1 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run myDeb second time'
        def result2 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created with version from project'
        new File(projectDir, 'build/distributions/project-defaults-test_2.5.0_all.deb').exists()
    }

    def 'rpm plugin works with configuration cache'() {
        given:
        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'app content'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.rpm'
            }

            tasks.register('myRpm', com.netflix.gradle.plugins.rpm.Rpm) {
                packageName = 'rpm-cc-test'
                version = '1.0.0'
                release = '1'

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when: 'run myRpm first time'
        def result1 = runTasks('myRpm', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run myRpm second time'
        def result2 = runTasks('myRpm', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created'
        new File(projectDir, 'build/distributions/rpm-cc-test-1.0.0-1.noarch.rpm').exists()
    }

    def 'configuration cache is invalidated and re-stored when build file changes'() {
        given:
        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'app content'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.deb'
            }

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'cache-invalidation-test'
                version = '1.0.0'

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when: 'first run - CC stored'
        def result1 = runTasks('myDeb', '--configuration-cache')

        then:
        result1.output.contains('Configuration cache entry stored')

        when: 'build version changes'
        buildFile.text = buildFile.text.replace("version = '1.0.0'", "version = '2.0.0'")

        and: 're-run with changed build file'
        def result2 = runTasks('myDeb', '--configuration-cache')

        then: 'CC is re-stored because the build configuration changed'
        result2.output.contains('Configuration cache entry stored')

        and: 'new version artifact is built'
        new File(projectDir, 'build/distributions/cache-invalidation-test_2.0.0_all.deb').exists()
    }

    def 'task re-executes when source files change while configuration cache is reused'() {
        given:
        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        def srcFile = new File(srcDir, 'app.txt')
        srcFile.text = 'original content'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.deb'
            }

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'incremental-cc-test'
                version = '1.0.0'

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when: 'first run - CC stored, task executes'
        def result1 = runTasks('myDeb', '--configuration-cache')

        then:
        result1.output.contains('Configuration cache entry stored')

        when: 'second run - CC reused, task is UP-TO-DATE'
        def result2 = runTasks('myDeb', '--configuration-cache')

        then:
        result2.output.contains('Configuration cache entry reused')
        result2.output.contains('UP-TO-DATE')

        when: 'source file is modified'
        srcFile.text = 'modified content'

        and: 'third run - CC reused but task re-executes'
        def result3 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is still reused (build file unchanged)'
        result3.output.contains('Configuration cache entry reused')

        and: 'task executed because its inputs changed'
        !result3.output.contains('1 actionable task: 1 up-to-date')
    }

    def 'rpm plugin produces no Task.getProject() deprecation warnings'() {
        given:
        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'app content'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.rpm'
            }

            version = '2.0.0'

            tasks.register('myRpm', com.netflix.gradle.plugins.rpm.Rpm) {
                packageName = 'rpm-deprecation-test'
                release = '1'

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when:
        def result = runTasks('myRpm', '--warning-mode', 'all')

        then: 'no Task.project at execution time deprecation warnings'
        !result.output.contains('Task.project at execution time has been deprecated')
        !result.output.contains('Invocation of Task.project at execution time')
    }

    def 'packager convention produces no Task.getProject() deprecation warnings'() {
        given:
        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'app content'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.deb'
            }

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'packager-deprecation-test'
                version = '1.0.0'
                packager = 'CI System'
                // user, maintainer, uploaders all default to packager via lazy providers

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when:
        def result = runTasks('myDeb', '--warning-mode', 'all')

        then: 'no Task.project at execution time deprecation warnings'
        !result.output.contains('Task.project at execution time has been deprecated')
        !result.output.contains('Invocation of Task.project at execution time')
    }

    def 'deb packaged from configuration dependency works with configuration cache'() {
        given:
        buildFile << """
            plugins {
                id 'com.netflix.nebula.deb'
                id 'java'
            }

            repositories {
                mavenCentral()
            }

            configurations {
                bundled
            }

            dependencies {
                bundled 'log4j:log4j:1.2.17'
            }

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'config-dep-test'
                version = '1.0.0'

                from(configurations.bundled) {
                    into '/opt/libs'
                    createDirectoryEntry = true
                }
            }
        """.stripIndent()

        when: 'run myDeb first time'
        def result1 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run myDeb second time'
        def result2 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created'
        new File(projectDir, 'build/distributions/config-dep-test_1.0.0_all.deb').exists()
    }

    def 'deb with file-based install scripts works with configuration cache'() {
        given:
        def scriptsDir = new File(projectDir, 'scripts')
        scriptsDir.mkdirs()
        new File(scriptsDir, 'pre.sh').text = '#!/bin/sh\necho "pre-install"'
        new File(scriptsDir, 'post.sh').text = '#!/bin/sh\necho "post-install"'
        new File(scriptsDir, 'pre-rm.sh').text = '#!/bin/sh\necho "pre-remove"'
        new File(scriptsDir, 'post-rm.sh').text = '#!/bin/sh\necho "post-remove"'

        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'app content'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.deb'
            }

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'scripts-test'
                version = '1.0.0'

                preInstall file('scripts/pre.sh')
                postInstall file('scripts/post.sh')
                preUninstall file('scripts/pre-rm.sh')
                postUninstall file('scripts/post-rm.sh')

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when: 'run myDeb first time'
        def result1 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run myDeb second time'
        def result2 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created'
        new File(projectDir, 'build/distributions/scripts-test_1.0.0_all.deb').exists()
    }

    def 'buildDeb and buildRpm both work with configuration cache in the same build'() {
        given:
        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'app.txt').text = 'app content'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.ospackage'
            }

            ospackage {
                packageName = 'multi-format-test'
                version = '1.0.0'
                release = '1'

                from('src') {
                    into '/opt/app'
                }
            }
        """.stripIndent()

        when: 'run both tasks first time'
        def result1 = runTasks('buildDeb', 'buildRpm', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run both tasks second time'
        def result2 = runTasks('buildDeb', 'buildRpm', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'deb package is created'
        new File(projectDir, 'build/distributions/multi-format-test_1.0.0-1_all.deb').exists()

        and: 'rpm package is created'
        new File(projectDir, 'build/distributions/multi-format-test-1.0.0-1.noarch.rpm').exists()
    }

    def 'deb with explicit file permissions works with configuration cache'() {
        given:
        def srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'app.sh').text = '#!/bin/sh\necho "hello"'
        new File(srcDir, 'config.txt').text = 'config value'

        buildFile << """
            plugins {
                id 'com.netflix.nebula.deb'
            }

            tasks.register('myDeb', com.netflix.gradle.plugins.deb.Deb) {
                packageName = 'permissions-test'
                version = '1.0.0'

                from('src') {
                    into '/opt/app'
                    include 'app.sh'
                    filePermissions {
                        unix(0755)
                    }
                }
                from('src') {
                    into '/opt/app'
                    include 'config.txt'
                    filePermissions {
                        unix(0644)
                    }
                }
            }
        """.stripIndent()

        when: 'run myDeb first time'
        def result1 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is stored'
        result1.output.contains('Configuration cache entry stored')

        when: 'run myDeb second time'
        def result2 = runTasks('myDeb', '--configuration-cache')

        then: 'configuration cache is reused'
        result2.output.contains('Configuration cache entry reused')

        and: 'package is created'
        new File(projectDir, 'build/distributions/permissions-test_1.0.0_all.deb').exists()
    }

    /**
     * Documents the known configuration cache limitation of OspackageApplicationSpringBootPlugin.
     *
     * The Spring Boot plugin uses project.distributions.main.contents.exclude { } with a closure
     * that captures a Provider<FileCollection> derived from project configurations. While the core
     * ospackage tasks are CC-compatible (Task.getProject() at execution time is fixed), the Spring
     * Boot integration plugin adds configuration-time closures that may not survive CC serialization
     * depending on the Gradle and Spring Boot versions in use.
     *
     * To verify Spring Boot CC support manually:
     *   1. Create a project with `org.springframework.boot` and `com.netflix.nebula.ospackage-application-spring-boot`
     *   2. Run: ./gradlew buildDeb --configuration-cache
     *   3. Check for CC incompatibility problems in the output
     */
    @Ignore('Requires a full Spring Boot project setup with the org.springframework.boot plugin')
    def 'spring boot ospackage plugin configuration cache compatibility'() {
        given:
        buildFile << """
            plugins {
                id 'org.springframework.boot' version '3.5.2'
                id 'com.netflix.nebula.ospackage-application-spring-boot'
            }

            application {
                mainClass = 'com.example.Main'
            }
        """.stripIndent()

        when:
        def result = runTasks('buildDeb', '--configuration-cache')

        then:
        result.output.contains('Configuration cache entry stored')
    }
}
