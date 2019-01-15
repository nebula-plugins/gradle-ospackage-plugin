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

import nebula.test.IntegrationSpec

class OspackageApplicationPluginLauncherSpec extends IntegrationSpec {
    def 'application shows up in deb'() {
        writeHelloWorld('nebula.test')
        buildFile << """
            ${applyPlugin(OspackageApplicationPlugin)}
            mainClassName = 'nebula.test.HelloWorld'
        """.stripIndent()

        when:
        runTasksSuccessfully('buildDeb')

        then:
        def archivePath = file("build/distributions/${moduleName}_unspecified_all.deb")
        def scan = new com.netflix.gradle.plugins.deb.Scanner(archivePath)

        0755 == scan.getEntry("./opt/${moduleName}/bin/${moduleName}").mode

        ["/opt/${moduleName}/lib/${moduleName}.jar", "/opt/${moduleName}/bin/${moduleName}.bat"].each {
            scan.getEntry(".${it}").isFile()
        }

        !scan.controlContents.containsKey('./postinst')
    }

    def 'can customize destination'() {
        writeHelloWorld('nebula.test')
        buildFile << """
            ${applyPlugin(OspackageApplicationPlugin)}
            mainClassName = 'nebula.test.HelloWorld'
            ospackage_application {
                prefix = '/usr/local'
            }
            applicationName = 'myapp'
        """.stripIndent()

        when:
        runTasksSuccessfully('buildDeb')

        then:
        def archivePath = file("build/distributions/${moduleName}_unspecified_all.deb")
        def scan = new com.netflix.gradle.plugins.deb.Scanner(archivePath)

        0755 == scan.getEntry("./usr/local/myapp/bin/myapp").mode

        ["/usr/local/myapp/lib/${moduleName}.jar", "/usr/local/myapp/bin/myapp.bat"].each {
            scan.getEntry(".${it}").isFile()
        }

    }
}
