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

import com.google.common.base.Throwables
import com.netflix.gradle.plugins.deb.Scanner
import nebula.test.IntegrationSpec
import org.junit.Rule
import org.junit.contrib.java.lang.system.ProvideSystemProperty
import spock.lang.Unroll

import java.util.jar.Manifest
import java.util.zip.ZipFile

class OspackageApplicationSpringBootPluginLauncherSpec extends IntegrationSpec {

    //TODO: remove this once @Optional is removed from https://github.com/spring-projects/spring-boot/blob/master/spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin/src/main/java/org/springframework/boot/gradle/tasks/application/CreateBootStartScripts.java#L33
    @Rule
    public final ProvideSystemProperty myPropertyHasMyValue = new ProvideSystemProperty("ignoreDeprecations", "true")

    def 'plugin throws exception if spring-boot plugin not applied'() {
        buildFile << """
            ${applyPlugin(OspackageApplicationSpringBootPlugin)}
        """

        when:
        def result = runTasks("help")

        then:
        Throwables.getRootCause(result.failure).message == "The 'org.springframework.boot' plugin must be applied before applying this plugin"
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

            ospackage {
                packageName = 'test'
            }
            
            task runStartScript(type: Exec) {
                commandLine '$startScript'
            }
            
            runStartScript.dependsOn buildDeb
        """.stripIndent()
    }

    @Unroll
    def 'application shows up in deb for boot #bootVersion'() {
        final applicationDir = distribution.isEmpty() ? moduleName : "$moduleName-$distribution"
        final startScript = "./opt/${applicationDir}/bin/${moduleName}"
        buildFile << buildScript(bootVersion, null)

        when:
        runTasksSuccessfully('buildDeb')

        then:
        final archivePath = file("build/distributions/test_0_all.deb")
        final scanner = new Scanner(archivePath, new File("${getProjectDir()}/build/tmp/extract"))

        final moduleJarName = "./opt/${applicationDir}/lib/${moduleName}.jar"

        scanner.getEntry(startScript).mode == fileMode

        [
                startScript,
                "./opt/${applicationDir}/bin/${moduleName}.bat",
                moduleJarName].each {
            assert scanner.getEntry("${it}").isFile()
        }

        final moduleJar = new ZipFile(scanner.getEntryFile(moduleJarName))
        final manifest = new Manifest(moduleJar.getInputStream(moduleJar.getEntry('META-INF/MANIFEST.MF')))
        manifest.getMainAttributes().getValue('Main-Class') == 'org.springframework.boot.loader.JarLauncher'

        !scanner.dataContents.keySet().find { tarArchiveEntry ->
            tarArchiveEntry.name.endsWith('.jar') && tarArchiveEntry.name != moduleJarName
        }
        !scanner.controlContents.containsKey('./postinst')

        where:
        bootVersion | distribution | fileMode
        '2.4.2'     | 'boot'       | 0755
    }

    @Unroll
    def 'application runs for boot #bootVersion'() {
        final applicationDir = distribution.isEmpty() ? moduleName : "$moduleName-$distribution"
        final startScript = file("build/install/$applicationDir/bin/$moduleName")

        buildFile << buildScript(bootVersion, startScript)

        when:
        def result = runTasksSuccessfully('runStartScript')

        then:
        result.standardOutput.contains('Hello Integration Test')

        where:
        bootVersion | distribution
        '2.4.2'     | 'boot'
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
        final appName = distribution.isEmpty() ? 'myapp' : "myapp-$distribution"
        final archivePath = file("build/distributions/test_0_all.deb")
        final scan = new Scanner(archivePath)

        final startScript = "./usr/local/$appName/bin/myapp"

        scan.getEntry(startScript).mode == fileMode

        [startScript,
         "./usr/local/$appName/bin/myapp.bat",
         "./usr/local/$appName/lib/${moduleName}.jar"].each {
            assert scan.getEntry("${it}").isFile()
        }

        where:
        bootVersion | distribution | fileMode
        '2.4.2'     | 'boot'       | 493
    }
}
