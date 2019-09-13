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

package com.netflix.gradle.plugins.daemon

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

/**
 * Monster class that does everything.
 */
class DaemonTemplateTask extends ConventionTask {

    @Internal
    Map<String, String> context

    @Internal
    Collection<String> templates

    @Internal
    File destDir

    @Internal
    String templatesFolder

    @TaskAction
    def template() {
        TemplateHelper templateHelper = new TemplateHelper(getDestDir(), getTemplatesFolder(), project)
        getTemplates().collect { String templateName ->
            templateHelper.generateFile(templateName, getContext())
        }
    }

    @Internal
    Collection<File> getTemplatesOutput() {
        return templates.collect {
            new File(destDir, it)
        }
    }
}
