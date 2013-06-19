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

package com.trigonic.gradle.plugins.deb

import com.trigonic.gradle.plugins.packaging.CommonPackagingPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class DebPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.plugins.apply(CommonPackagingPlugin.class)

        // Register class, so users don't have to add imports
        project.ext.Deb = Deb.class

        // Some defaults, if not set by the user
        project.tasks.withType(Deb) { Deb task ->
            task.applyConventions()
        }

    }
}