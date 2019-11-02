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

package com.netflix.gradle.plugins.deb

import com.netflix.gradle.plugins.deb.control.MultiArch
import com.netflix.gradle.plugins.packaging.AliasHelper
import com.netflix.gradle.plugins.packaging.CommonPackagingPlugin
import com.netflix.gradle.plugins.rpm.RpmPlugin
import groovy.transform.CompileDynamic
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project

class DebPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.plugins.apply(CommonPackagingPlugin.class)

        // Register class, so users don't have to add imports
        registerDebClass(project)

        // Some defaults, if not set by the user
        project.tasks.withType(Deb).configureEach(new Action<Deb>() {
            @Override
            void execute(Deb deb) {
                RpmPlugin.applyAliases(deb) // RPM Specific aliases
                DebPlugin.applyAliases(deb) // DEB-specific aliases
                deb.applyConventions()
            }
        })
    }

    @CompileDynamic
    private void registerDebClass(Project project) {
        project.ext.Deb = Deb.class
    }

    def static applyAliases(def dynamicObjectAware) {
        AliasHelper.aliasEnumValues(MultiArch.values(), dynamicObjectAware)
    }
}