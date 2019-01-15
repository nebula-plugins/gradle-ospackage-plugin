/*
 * Copyright 2011-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.gradle.plugins.packaging

import com.netflix.gradle.plugins.deb.DebPlugin
import com.netflix.gradle.plugins.docker.OsPackageDockerBasePlugin
import com.netflix.gradle.plugins.docker.OsPackageDockerPlugin
import com.netflix.gradle.plugins.rpm.RpmPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class SystemPackagingBasePlugin implements Plugin<Project> {
    private static Logger logger = Logging.getLogger(SystemPackagingBasePlugin);

    Project project
    ProjectPackagingExtension extension

    public static final String taskBaseName = 'ospackage'

    void apply(Project project) {

        this.project = project

        // Extension is created before plugins are, so tasks
        extension = createExtension()
        RpmPlugin.applyAliases(extension) // RPM Specific aliases
        DebPlugin.applyAliases(extension) // DEB-specific aliases

        project.plugins.apply(RpmPlugin.class)
        project.plugins.apply(DebPlugin.class)
        project.plugins.apply(OsPackageDockerBasePlugin)
    }

    ProjectPackagingExtension createExtension() {
        ProjectPackagingExtension extension = project.extensions.create(taskBaseName, ProjectPackagingExtension, project)

        // Ensure extension is IConventionAware
        ConventionMapping mapping = ((IConventionAware) extension).getConventionMapping()

        return extension
    }
}