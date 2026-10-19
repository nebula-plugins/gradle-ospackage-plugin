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

import com.netflix.gradle.plugins.packaging.MutableListPropertyView
import com.netflix.gradle.plugins.packaging.MutableMapPropertyView
import org.gradle.api.internal.ConventionTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
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

import javax.inject.Inject

/**
 * Monster class that does everything.
 */
@DisableCachingByDefault
abstract class DaemonTemplateTask extends ConventionTask {

    @Input
    abstract MapProperty<String, Object> getContextProperty()

    @Input
    abstract ListProperty<String> getTemplatesProperty()

    @OutputDirectory
    abstract DirectoryProperty getDestDirProperty()

    @Input
    abstract Property<String> getTemplatesFolderProperty()

    @Internal
    abstract Property<File> getProjectDirectory()

    @Inject
    abstract ProjectLayout getProjectLayout()

    DaemonTemplateTask() {
        projectDirectory.convention(projectLayout.projectDirectory.asFile)
    }

    @Internal
    Map<String, Object> getContext() { new MutableMapPropertyView<String, Object>(contextProperty) }
    void setContext(Map<String, Object> value) { contextProperty.set(value) }

    @Internal
    Collection<String> getTemplates() { new MutableListPropertyView<String>(templatesProperty) }
    void setTemplates(Collection<String> value) { templatesProperty.set(value) }

    @Internal
    File getDestDir() { destDirProperty.getOrNull()?.asFile }
    void setDestDir(File value) { destDirProperty.set(value) }

    @Internal
    String getTemplatesFolder() { templatesFolderProperty.getOrNull() }
    void setTemplatesFolder(String value) { templatesFolderProperty.set(value) }

    @Internal
    Collection<File> getTemplatesOutput() {
        templates.collect { String name -> new File(destDir, name) }
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    List<File> getTemplateFiles() {
        String folder = templatesFolderProperty.get()
        File baseDir = projectDirectory.get()
        templatesProperty.get().collect { String templateName ->
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
            destDirProperty.get().asFile,
            templatesFolderProperty.get(),
            projectDirectory.get()
        )
        Map<String, Object> templateContext = contextProperty.get()
        templatesProperty.get().each { String templateName ->
            templateHelper.generateFile(templateName, templateContext)
        }
    }
}
