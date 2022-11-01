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

import com.netflix.gradle.plugins.daemon.DaemonDefinition
import com.netflix.gradle.plugins.daemon.DaemonExtension
import com.netflix.gradle.plugins.daemon.OspackageDaemonPlugin
import com.netflix.gradle.plugins.utils.ConfigureUtil
import groovy.transform.CompileDynamic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.tasks.application.CreateStartScripts

/**
 * Combine the nebula-ospackage-application with the nebula-ospackage-daemon plugin. As with the nebula-ospackage-application,
 * this is opinionated to where the application lives, but is relatively flexible to how the daemon runs.
 *
 * TODO Make a base plugin, so that this plugin can require os-package
 *
 * Usage (from nebula-ospackage-application):
 * <ul>
 *     <li>User has to provide a mainClassName
 *     <li>User has to create SystemPackaging tasks, the easiest way is to apply plugin: 'os-package'
 * </ul>
 */
class OspackageApplicationDaemonPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.plugins.apply(OspackageApplicationPlugin)
        def ospackageApplicationExtension = project.extensions.getByType(OspackageApplicationExtension)

        CreateStartScripts startScripts = (CreateStartScripts) project.tasks.getByName(ApplicationPlugin.TASK_START_SCRIPTS_NAME)

        project.plugins.apply(OspackageDaemonPlugin)

        // Mechanism for user to configure daemon further
        List<Closure> daemonConfiguration = []
        setApplicationDaemon(project, daemonConfiguration)

        // TODO Convention mapping on definition instead of afterEvaluate
        project.afterEvaluate {
            // TODO Sanitize name
            def name = startScripts.applicationName

            // Add daemon to project
            DaemonExtension daemonExt = project.extensions.getByType(DaemonExtension)
            def definition = daemonExt.daemon { DaemonDefinition daemonDefinition ->
                daemonDefinition.setDaemonName(name)
                daemonDefinition.setCommand("${ospackageApplicationExtension.prefix}/${name}/bin/${name}".toString())
            }

            daemonConfiguration.each { confClosure ->
                ConfigureUtil.configure(confClosure, definition)
            }
        }
    }

    @CompileDynamic
    private void setApplicationDaemon(Project project, List<Closure> daemonConfiguration) {
        project.ext.applicationdaemon = { Closure closure ->
            daemonConfiguration << closure
        }
    }
}
