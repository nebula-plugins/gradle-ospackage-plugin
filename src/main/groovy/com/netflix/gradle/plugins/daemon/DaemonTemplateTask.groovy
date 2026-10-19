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

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Monster class that does everything.
 */
@DisableCachingByDefault
abstract class DaemonTemplateTask extends DefaultTask {

    @Input
    abstract MapProperty<String, Object> getContext()

    @Input
    abstract ListProperty<String> getTemplates()

    @OutputDirectory
    abstract DirectoryProperty getDestDir()

    @Input
    abstract Property<String> getTemplatesFolder()

    @Internal
    abstract Property<File> getProjectDirectory()

    DaemonTemplateTask() {
        // Capture project directory during configuration
        projectDirectory.convention(project.projectDir)
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    List<File> getTemplateFiles() {
        String folder = templatesFolder.get()
        File baseDir = projectDirectory.get()
        templates.get().collect { String templateName ->
            String path = folder ? "${folder}/${templateName}.tpl" : "${templateName}.tpl"
            if (TemplateHelper.class.getResource(path)) {
                return null
            }
            (folder && new File(folder).isAbsolute()) ? new File(path) : new File(baseDir, path)
        }.findAll { it != null }
    }

    @TaskAction
    def template() {
        TemplateHelper templateHelper = new TemplateHelper(
            destDir.get().asFile,
            templatesFolder.get(),
            projectDirectory.get()
        )
        Map<String, Object> templateContext = context.get()
        templates.get().each { String templateName ->
            templateHelper.generateFile(templateName, templateContext)
        }
    }
}
