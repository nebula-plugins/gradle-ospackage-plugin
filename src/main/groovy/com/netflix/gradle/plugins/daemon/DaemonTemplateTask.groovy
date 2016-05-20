/*
 * Copyright 2014-2016 Netflix, Inc.
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
import org.gradle.api.tasks.TaskAction

/**
 * Monster class that does everything.
 */
class DaemonTemplateTask extends ConventionTask {
    //@Input
    Map<String, String> context

    //@Input
    Collection<String> templates

    @Input
    File destDir

    @TaskAction
    def template() {
        TemplateHelper templateHelper = new TemplateHelper(getDestDir(), '/com/netflix/gradle/plugins/daemon')
        getTemplates().collect { String templateName ->
            templateHelper.generateFile(templateName, getContext())
        }
    }

    //@OutputFiles
    Collection<File> getTemplatesOutout() {
        return templates.collect {
            new File(destDir, it)
        }
    }
}
