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

package com.netflix.gradle.plugins.application

import com.netflix.gradle.plugins.deb.Scanner
import nebula.test.IntegrationSpec
import org.junit.Rule
import org.junit.contrib.java.lang.system.ProvideSystemProperty
import spock.lang.Shared
import spock.lang.Unroll
import spock.lang.IgnoreIf

import java.util.jar.JarFile

class OspackageApplicationSpringBootPluginLauncherSpec extends IntegrationSpec {
    @Rule
    public final ProvideSystemProperty ignoreDeprecations = new ProvideSystemProperty("ignoreDeprecations", "true")

    def 'plugin throws exception if spring-boot plugin not applied'() {
        buildFile << """
            ${applyPlugin(OspackageApplicationSpringBootPlugin)}
        """

        when:
        def result = runTasks("help")

        then:
        result.failure.getCause().getCause().getCause().message == "The 'org.springframework.boot' plugin must be applied before applying this plugin"
    }

    String buildScript(String bootVersion, File startScript) {
        writeHelloWorld('nebula.test')

        return """
            buildscript {
                repositories {
                    mavenCentral()
                    maven { url 'https://repo.spring.io/milestone' }
                }
                dependencies {
                    classpath 'org.springframework.boot:spring-boot-gradle-plugin:$bootVersion'
                }
            }

            apply plugin: 'org.springframework.boot'
            ${applyPlugin(OspackageApplicationSpringBootPlugin)}

            mainClassName = 'nebula.test.HelloWorld'

            repositories {
                mavenCentral()
                maven { url 'https://repo.spring.io/milestone' }
            }

            dependencies {
                implementation 'org.springframework.boot:spring-boot-starter:$bootVersion'
            }
            
            bootJar.mustRunAfter jar

            ospackage {
                packageName = 'test'
            }
            
            task runStartScript(type: Exec) {
                commandLine '$startScript'
            }

            runStartScript.dependsOn installDist
        """.stripIndent()
    }

    @Unroll
    def 'application shows up in deb for boot #bootVersion'() {
        final applicationDir = "$moduleName-boot"
        final startScript = "./opt/${applicationDir}/bin/${moduleName}"
        buildFile << buildScript(bootVersion, null)

        when:
        runTasksSuccessfully('build', 'buildDeb')

        then:
        final archivePath = file("build/distributions/test_0_all.deb")
        final scanner = new Scanner(archivePath, new File("${getProjectDir()}/build/tmp/extract"))

        final moduleJarName = "./opt/${applicationDir}/lib/${moduleName}${moduleSuffix}.jar"

        scanner.getEntry(startScript).mode == 0755
        [
                startScript,
                "./opt/${applicationDir}/bin/${moduleName}.bat",
                moduleJarName].each {
            assert scanner.getEntry("${it}").isFile()
        }

        //make sure we don't have boot jar in debian
        def jarFile = new JarFile(scanner.getEntryFile(moduleJarName))
        !isBootJar(jarFile)

        !scanner.controlContents.containsKey('./postinst')

        where:
        bootVersion | moduleSuffix
        '2.4.0'     | ''
        '2.5.0'     | '-plain'
        '2.6.0'     | '-plain'
    }

    private boolean isBootJar(JarFile jarFile) {
        jarFile.entries().any {
            it.name.contains("BOOT-INF")
        }
    }

    @Unroll
    def 'application runs for boot #bootVersion'() {
        final applicationDir = "$moduleName-boot"
        final startScript = file("build/install/$applicationDir/bin/$moduleName")

        buildFile << buildScript(bootVersion, startScript)

        when:
        def result = runTasks('runStartScript')

        then:
        result.standardOutput.contains('Hello Integration Test')

        where:
        bootVersion << ['2.4.0', '2.5.0', '2.6.0']
    }

    @Unroll
    def 'application runs for boot #bootVersion when mainClassName configured using springBoot extension'() {
        final applicationDir = "$moduleName-boot"
        final startScript = file("build/install/$applicationDir/bin/$moduleName")

        buildFile << buildScript(bootVersion, startScript)
        buildFile << """
        mainClassName = null

        springBoot {
            mainClassName = 'nebula.test.HelloWorld'
        }
        """

        when:
        def result = runTasksSuccessfully('runStartScript')

        then:
        result.standardOutput.contains('Hello Integration Test')

        where:
        bootVersion << ['2.4.0', '2.5.0', '2.6.0']
    }

    @Unroll
    def 'can customize destination for boot #bootVersion'() {
        buildFile << buildScript(bootVersion, null)
        buildFile << """
            applicationName = 'myapp'

            ospackage_application {
                prefix = '/usr/local'
            }
        """.stripIndent()

        when:
        runTasksSuccessfully('buildDeb')

        then:
        final appName = "myapp-boot"
        final archivePath = file("build/distributions/test_0_all.deb")
        final scan = new Scanner(archivePath)

        final startScript = "./usr/local/$appName/bin/myapp"

        scan.getEntry(startScript).mode == 493

        [startScript,
         "./usr/local/$appName/bin/myapp.bat",
         "./usr/local/$appName/lib/${moduleName}${moduleSuffix}.jar"].each {
            assert scan.getEntry("${it}").isFile()
        }

        where:
        bootVersion | moduleSuffix
        '2.4.0'     | ''
        '2.5.0'     | '-plain'
        '2.6.0'     | '-plain'
    }

    @Unroll
    def 'application fails if mainClass is not present for #bootVersion'() {
        final applicationDir = "$moduleName-boot"
        final startScript = file("build/install/$applicationDir/bin/$moduleName")

        buildFile << buildScript(bootVersion, startScript)
        buildFile << """
        mainClassName = null
        application.mainClass.set(null)
        """

        when:
        def result = runTasksWithFailure('installDist')

        then:
        result.standardError.contains("mainClass should be configured in order to generate a valid start script. i.e. mainClass.set('com.netflix.app.MyApp')")

        where:
        bootVersion << ['2.4.0', '2.5.0', '2.6.0']
    }

    @IgnoreIf({ jvm.isJava17() })
    @Unroll
    def 'application fails if mainClassName is not present (old versions of Gradle)'() {
        final applicationDir = "$moduleName-boot"
        final startScript = file("build/install/$applicationDir/bin/$moduleName")

        gradleVersion = '6.3'
        buildFile << buildScript(bootVersion, startScript)
        buildFile << """
        mainClassName = null
        """

        when:
        def result = runTasksWithFailure('installDist')

        then:
        result.standardError.contains("No value has been specified for property 'mainClassName'")

        where:
        bootVersion << ['2.4.0']
    }
}
