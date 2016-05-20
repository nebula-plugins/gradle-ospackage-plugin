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

import com.netflix.gradle.plugins.daemon.DaemonExtension
import com.netflix.gradle.plugins.daemon.OspackageDaemonPlugin
import nebula.test.PluginProjectSpec
import org.gradle.api.plugins.ApplicationPlugin

class OspackageApplicationDaemonPluginSpec extends PluginProjectSpec {
    @Override
    String getPluginName() {
        'nebula.ospackage-application-daemon'
    }

    def 'project modified by plugin'() {
        when:
        project.plugins.apply OspackageApplicationDaemonPlugin

        then:
        project.plugins.getPlugin(ApplicationPlugin)
        project.plugins.getPlugin(OspackageDaemonPlugin)
        project.ext.has('applicationdaemon')

        def daemonExt = project.extensions.getByType(DaemonExtension)
        daemonExt.daemons.size() == 0

        when:
        project.evaluate()

        then:
        daemonExt.daemons.size() == 1

    }
}
