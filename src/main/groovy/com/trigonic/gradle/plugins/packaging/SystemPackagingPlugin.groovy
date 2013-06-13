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

import com.trigonic.gradle.plugins.deb.Deb
import com.trigonic.gradle.plugins.rpm.Rpm
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Create implicit tasks, which will inherit from the ospackage extension.
 */
public class SystemPackagingPlugin implements Plugin<Project> {
    private static Logger logger = Logging.getLogger(SystemPackagingPlugin);

    Project project
    Deb debTask
    Rpm rpmTask

    void apply(Project project) {

        this.project = project

        project.plugins.apply(SystemPackagingBasePlugin.class)
        debTask = project.task([type: Deb], 'buildDeb')
        rpmTask = project.task([type: Rpm], 'buildRpm')

    }

}