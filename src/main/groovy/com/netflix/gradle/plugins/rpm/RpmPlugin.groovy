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

package com.netflix.gradle.plugins.rpm

import com.netflix.gradle.plugins.packaging.AliasHelper
import com.netflix.gradle.plugins.packaging.CommonPackagingPlugin
import com.netflix.gradle.plugins.rpm.core.Builder
import com.netflix.gradle.plugins.rpm.core.header.Architecture
import com.netflix.gradle.plugins.rpm.core.header.Flags
import com.netflix.gradle.plugins.rpm.core.header.Os
import com.netflix.gradle.plugins.rpm.core.header.RpmType
import com.netflix.gradle.plugins.rpm.core.payload.Directive

import groovy.transform.CompileDynamic

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileDynamic
class RpmPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.plugins.apply(CommonPackagingPlugin.class)

        project.ext.Rpm = Rpm.class

        // Some defaults, if not set by the user
        project.tasks.withType(Rpm).configureEach { Rpm rpm ->
            RpmPlugin.applyAliases(rpm) // RPM Specific aliases
            rpm.applyConventions(project)
        }
    }

    def static applyAliases(def dynamicObjectAware) {
        AliasHelper.aliasEnumValues(Architecture.values(), dynamicObjectAware)
        AliasHelper.aliasEnumValues(Os.values(), dynamicObjectAware)
        AliasHelper.aliasEnumValues(RpmType.values(), dynamicObjectAware)
        AliasHelper.aliasStaticInstances(Directive.class, dynamicObjectAware)
        AliasHelper.aliasStaticInstances(Flags.class, int.class, dynamicObjectAware)
    }
}