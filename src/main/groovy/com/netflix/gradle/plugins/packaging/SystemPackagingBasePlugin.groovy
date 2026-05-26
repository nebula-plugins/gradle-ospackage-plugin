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
import com.netflix.gradle.plugins.rpm.RpmPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class SystemPackagingBasePlugin implements Plugin<Project> {
    ProjectPackagingExtension extension

    public static final String taskBaseName = 'ospackage'

    void apply(Project project) {
        // Extension is created before plugins are, so tasks
        extension = createExtension(project)
        RpmPlugin.applyAliases(extension) // RPM Specific aliases
        DebPlugin.applyAliases(extension) // DEB-specific aliases

        project.plugins.apply("com.netflix.nebula.rpm")
        project.plugins.apply("com.netflix.nebula.deb")
        project.plugins.apply("com.netflix.nebula.ospackage-docker-base")
    }

    ProjectPackagingExtension createExtension(Project project) {
        ProjectPackagingExtension extension = project.extensions.create(taskBaseName, ProjectPackagingExtension, project)
        return extension
    }
}