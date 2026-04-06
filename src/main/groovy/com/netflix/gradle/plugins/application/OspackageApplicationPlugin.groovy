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

import com.netflix.gradle.plugins.packaging.ProjectPackagingExtension
import com.netflix.gradle.plugins.packaging.SystemPackagingPlugin
import groovy.transform.CompileDynamic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.plugins.ApplicationPlugin

/**
 * Combine the os-package with the Application plugin. Currently heavily opinionated to where
 * the code will live, though that is slightly configurable using the ospackage-application extension.
 * <p>
 * Usage:
 * <ul>
 *     <li>User has to provide a mainClassName
 *     <li>User has to create SystemPackaging tasks, the easiest way is to apply plugin: 'os-package'
 * </ul>
 */
class OspackageApplicationPlugin implements Plugin<Project> {
    OspackageApplicationExtension extension

    @Override
    @CompileDynamic
    void apply(Project project) {
        extension = project.extensions.create('ospackage_application', OspackageApplicationExtension)
        extension.prefix.convention('/opt')
        extension.distribution.convention('')

        project.plugins.apply(ApplicationPlugin)
        project.plugins.apply(SystemPackagingPlugin)

        def distributions = project.getExtensions().getByType(DistributionContainer.class)
        def mainDistribution = distributions.getByName(DistributionPlugin.MAIN_DISTRIBUTION_NAME)
        def name = extension.prefix.map { prefix ->
            String baseName = mainDistribution.getDistributionBaseName().get()
            String classifier = mainDistribution.getDistributionClassifier().getOrNull()
            return baseName + (classifier != null ? '-' + classifier : '')
        }
        def packaging = project.extensions.getByType(ProjectPackagingExtension)
        def copyMain = project.copySpec() {
            with(mainDistribution.contents)
            into(name)
        }
        packaging.with(copyMain)
        packaging.into(extension.prefix.map { it })
    }
}
