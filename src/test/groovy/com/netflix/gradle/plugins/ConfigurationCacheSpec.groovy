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

        and: 'daemon template files are created'
        new File(projectDir, 'build/daemon/TestDaemon/myDeb/initd').exists()
        new File(projectDir, 'build/daemon/TestDaemon/myDeb/run').exists()
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

        and: 'both daemon template files exist'
        new File(projectDir, 'build/daemon/Daemon1/myDeb/initd').exists()
        new File(projectDir, 'build/daemon/Daemon2/myDeb/initd').exists()
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

        and: 'custom template is used'
        def initdFile = new File(projectDir, 'build/daemon/CustomDaemon/myDeb/initd')
        initdFile.exists()
        initdFile.text.contains('Custom init script for custom-daemon')
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
}
