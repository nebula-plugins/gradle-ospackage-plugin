/*
 * Copyright 2011 the original author or authors.
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

package com.trigonic.gradle.plugins.packaging

import com.trigonic.gradle.plugins.deb.DebPlugin
import com.trigonic.gradle.plugins.rpm.RpmPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.BasePlugin

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

        project.plugins.apply(BasePlugin.class)
        project.plugins.apply(RpmPlugin.class)
        project.plugins.apply(DebPlugin.class)
    }

    ProjectPackagingExtension createExtension() {
        ProjectPackagingExtension extension = project.extensions.create(taskBaseName, ProjectPackagingExtension, project)

        // Ensure extension is IConventionAware
        ConventionMapping mapping = ((IConventionAware) extension).getConventionMapping()

        return extension
    }
}