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

import com.netflix.gradle.plugins.deb.Deb
import com.netflix.gradle.plugins.packaging.ProjectPackagingExtension
import com.netflix.gradle.plugins.packaging.SystemPackagingPlugin
import com.netflix.gradle.plugins.packaging.SystemPackagingTask
import com.netflix.gradle.plugins.rpm.Rpm
import groovy.transform.CompileDynamic
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.IConventionAware
import org.gradle.api.plugins.ApplicationPlugin

/**
 * Combine the os-package with the Application plugin. Currently heavily opinionated to where
 * the code will live, though that is slightly configurable using the ospackage-application extension.
 *
 * TODO Make a base plugin, so that this plugin can require os-package
 *
 * Usage:
 * <ul>
 *     <li>User has to provide a mainClassName
 *     <li>User has to create SystemPackaging tasks, the easiest way is to apply plugin: 'os-package'
 * </ul>
 */
class OspackageApplicationPlugin implements Plugin<Project> {
    OspackageApplicationExtension extension

    @Override
    void apply(Project project) {
        extension = project.extensions.create('ospackage_application', OspackageApplicationExtension)
        def conventionMapping = ((IConventionAware) extension).conventionMapping
        conventionMapping.map('prefix') { '/opt' }
        conventionMapping.map('distribution') { '' }

        project.plugins.apply(ApplicationPlugin)
        project.plugins.apply(SystemPackagingPlugin)

        def packagingExt = project.extensions.getByType(ProjectPackagingExtension)

        // Configure packaging extension - use provider for lazy evaluation
        packagingExt.from(project.provider {
            def distributionName = extension.distribution ?: ''
            def installTask = project.tasks.named("install${distributionName.capitalize()}Dist")
            installTask.get().outputs.files.singleFile.parent
        })

        packagingExt.into(project.provider { extension.getPrefix() })

        // Link install task to package tasks lazily
        linkInstallToPackageTasks(project, Deb)
        linkInstallToPackageTasks(project, Rpm)
    }

    @CompileDynamic
    private <T extends Class> void linkInstallToPackageTasks(Project project, T type) {
        project.tasks.withType(type).configureEach { SystemPackagingTask task ->
            // Use TaskProvider for type-safe lazy task dependency
            def distributionName = extension.distribution ?: ''
            def installTaskName = "install${distributionName.capitalize()}Dist"
            task.dependsOn(project.tasks.named(installTaskName))
        }
    }
}
