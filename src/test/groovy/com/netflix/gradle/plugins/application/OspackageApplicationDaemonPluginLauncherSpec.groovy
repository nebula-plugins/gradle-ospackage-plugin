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

class OspackageApplicationDaemonPluginLauncherSpec extends BaseIntegrationTestKitSpec {

    def 'daemon script from application'() {
        writeHelloWorld('nebula.test')
        buildFile << """
            plugins {
                id 'com.netflix.nebula.ospackage-application-daemon'
            } 

            application {
                mainClass = 'nebula.test.HelloWorld'
            }
        """.stripIndent()

        when:
        runTasks('buildDeb')

        then:
        def archivePath = file("build/distributions/${moduleName}_0_all.deb")
        def scan = new com.netflix.gradle.plugins.deb.Scanner(archivePath)

        ["/service/${moduleName}/run", "/etc/init.d/${moduleName}", "/opt/${moduleName}/lib/${moduleName}.jar", "/opt/${moduleName}/bin/${moduleName}"].each {
            scan.getEntry(".${it}").isFile()
        }

        scan.controlContents.containsKey('./postinst')
        scan.controlContents['./postinst'].contains("/usr/sbin/update-rc.d ${moduleName} start 85 2 3 4 5 . stop 15 0 1 6 .")
    }
}
