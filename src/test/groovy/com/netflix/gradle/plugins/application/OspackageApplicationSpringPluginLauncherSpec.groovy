/*
 * Copyright 2014-2016 Netflix, Inc.
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

import java.util.jar.Manifest
import java.util.zip.ZipFile

class OspackageApplicationSpringPluginLauncherSpec extends IntegrationSpec {
    def 'application shows up in deb'() {
        writeHelloWorld('nebula.test')
        buildFile << """
            repositories {
                mavenCentral()
            }

            ${applyPlugin(OspackageApplicationSpringPlugin)}

            dependencies {
                compile 'org.springframework.boot:spring-boot-starter:1.5.10.RELEASE'
            }
        """.stripIndent()

        when:
        runTasksSuccessfully('buildDeb')

        then:
        final archivePath = file("build/distributions/${moduleName}_unspecified_all.deb")
        final scanner = new Scanner(archivePath, new File("${getProjectDir()}/build/tmp/extract"))

        final startScript = "./opt/${moduleName}/bin/${moduleName}"
        final moduleJarName = "./opt/${moduleName}/lib/${moduleName}.jar"

        0755 == scanner.getEntry(startScript).mode

        [
            startScript,
            "./opt/${moduleName}/bin/${moduleName}.bat",
            moduleJarName].each {
            scanner.getEntry("${it}").isFile()
        }

        final moduleJar = new ZipFile(scanner.getEntryFile(moduleJarName))
        final manifest = new Manifest(moduleJar.getInputStream(moduleJar.getEntry('META-INF/MANIFEST.MF')))
        manifest.getMainAttributes().getValue('Main-Class') == 'org.springframework.boot.loader.JarLauncher'

        !scanner.dataContents.keySet().find { tarArchiveEntry ->
            tarArchiveEntry.name.endsWith('.jar') && tarArchiveEntry.name != moduleJarName
        }
        !scanner.controlContents.containsKey('./postinst')
    }

    def 'can customize destination'() {
        writeHelloWorld('nebula.test')
        buildFile << """
            repositories {
                mavenCentral()
            }

            ${applyPlugin(OspackageApplicationSpringPlugin)}

            applicationName = 'myapp'

            ospackage_application {
                prefix = '/usr/local'
            }
        """.stripIndent()

        when:
        runTasksSuccessfully('buildDeb')

        then:
        final archivePath = file("build/distributions/${moduleName}_unspecified_all.deb")
        final scan = new Scanner(archivePath)

        final startScript = "./usr/local/myapp/bin/myapp"

        0755 == scan.getEntry(startScript).mode

        [
            startScript,
            "./usr/local/myapp/bin/myapp.bat",
            "./usr/local/myapp/lib/${moduleName}.jar"].each {
            scan.getEntry("${it}").isFile()
        }

    }
}
