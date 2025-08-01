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

import com.netflix.gradle.plugins.BaseIntegrationTestKitSpec
import com.netflix.gradle.plugins.SupportedGradleVersions
import com.netflix.gradle.plugins.deb.Scanner
import org.junit.Rule
import org.junit.contrib.java.lang.system.ProvideSystemProperty
import spock.lang.Unroll

import java.util.jar.JarFile

class OspackageApplicationSpringBootPluginLauncherSpec extends BaseIntegrationTestKitSpec {
    @Rule
    public final ProvideSystemProperty ignoreDeprecations = new ProvideSystemProperty("ignoreDeprecations", "true")

    def setup() {
        disableConfigurationCache()
        // org.gradle.api.tasks.application.CreateStartScript does not support config cache and it is used in Spring Boot plugin in these tests
    }

    def 'plugin throws exception if spring-boot plugin not applied'() {
        buildFile << """
            plugins {
                id 'com.netflix.nebula.ospackage-application-spring-boot'
            } 
        """

        when:
        def result = runTasksAndFail("help")

        then:
        result.output.contains("The 'com.netflix.nebula.ospackage-application-spring-boot' plugin requires the 'org.springframework.boot' plugin.")
    }

    String buildScript(String bootVersion, File startScript) {
        writeHelloWorld('nebula.test')

        return """
            plugins {
                id 'org.springframework.boot' version '$bootVersion'
                id 'com.netflix.nebula.ospackage-application-spring-boot'
            }

            application {
                mainClass = 'nebula.test.HelloWorld'
            }

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
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
        """.stripIndent()
    }

    @Unroll
    def 'application shows up in deb for boot #bootVersion and gradle #testGradleVersion'() {
        final applicationDir = "$moduleName-boot"
        final startScript = "./opt/${applicationDir}/bin/${moduleName}"
        buildFile << buildScript(bootVersion, null)

        when:
        forwardOutput = true
        gradleVersion = testGradleVersion
        runTasks('build', 'buildDeb', "--stacktrace")

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
        bootVersion | testGradleVersion                  | moduleSuffix
        '3.5.2'     | SupportedGradleVersions.GRADLE_MAX | '-plain'
        '3.5.2'     | SupportedGradleVersions.GRADLE_MIN | '-plain'
        '2.7.18'    | SupportedGradleVersions.GRADLE_MIN | '-plain'
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
        forwardOutput = true
        def result = runTasks('runStartScript')

        then:
        result.output.contains('Hello Integration Test')

        where:
        bootVersion << ['3.5.2']
    }

    @Unroll
    def 'application runs for boot #bootVersion when mainClass configured using springBoot extension'() {
        final applicationDir = "$moduleName-boot"
        final startScript = file("build/install/$applicationDir/bin/$moduleName")

        buildFile << buildScript(bootVersion, startScript)
        buildFile << """
        
        application {
            mainClass = null
        }

        springBoot {
            mainClass = 'nebula.test.HelloWorld'
        }
        """

        when:
        def result = runTasks('runStartScript')

        then:
        result.output.contains('Hello Integration Test')

        where:
        bootVersion << ['3.5.2']
    }

    @Unroll
    def 'can customize destination for boot #bootVersion'() {
        buildFile << buildScript(bootVersion, null)
        buildFile << """
            application.applicationName = 'myapp'

            ospackage_application {
                prefix = '/usr/local'
            }
        """.stripIndent()

        when:
        runTasks('buildDeb')

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
        '3.5.2'     | '-plain'
    }

    @Unroll
    def 'application fails if mainClass is not present for #bootVersion'() {
        final applicationDir = "$moduleName-boot"
        final startScript = file("build/install/$applicationDir/bin/$moduleName")

        buildFile << buildScript(bootVersion, startScript)
        buildFile << """
        application.mainClass.set(null)
        """

        when:
        def result = runTasksAndFail('installDist')

        then:
        result.output.contains("mainClass should be configured in order to generate a valid start script. i.e. mainClass = 'com.netflix.app.MyApp'")

        where:
        bootVersion << ['3.5.2']
    }
}
