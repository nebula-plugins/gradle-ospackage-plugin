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

import com.netflix.gradle.plugins.packaging.SystemPackagingBasePlugin
import com.netflix.gradle.plugins.packaging.SystemPackagingTask
import com.netflix.gradle.plugins.rpm.Rpm
import com.netflix.gradle.plugins.utils.FilePermissionUtil
import com.netflix.gradle.plugins.utils.WrapUtil
import groovy.text.GStringTemplateEngine
import groovy.transform.CompileDynamic
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project

class OspackageDaemonPlugin implements Plugin<Project> {
    public static final String POST_INSTALL_TEMPLATE = "postInstall"
    Project project
    DaemonExtension extension
    DaemonTemplatesConfigExtension daemonTemplatesConfigExtension
    DefaultDaemonDefinitionExtension defaultDefinition

    private final String DEFAULT_TEMPLATES_FOLDER = '/com/netflix/gradle/plugins/daemon'


    Map<String,Object> toContext(DaemonDefinition definitionDefaults, DaemonDefinition definition) {
        return [
                daemonName: definition.daemonName ?: definitionDefaults.daemonName,
                command: definition.command,
                user: definition.user ?: definitionDefaults.user,
                logCommand: definition.logCommand ?: definitionDefaults.logCommand,
                logUser: definition.logUser ?: definitionDefaults.logUser,
                logDir: definition.logDir ?: definitionDefaults.logDir,
                runLevels: definition.runLevels ?: definitionDefaults.runLevels,
                autoStart: definition.autoStart != null? definition.autoStart : definitionDefaults.autoStart,
                startSequence: definition.startSequence ?: definitionDefaults.startSequence,
                stopSequence: definition.stopSequence ?: definitionDefaults.stopSequence
        ] as Map<String,Object>
    }

    @Override
    void apply(Project project) {
        this.project = project
        project.plugins.apply(SystemPackagingBasePlugin)

        DomainObjectSet<DaemonDefinition> daemonsList = WrapUtil.toDomainObjectSet(DaemonDefinition)
        extension = project.extensions.create('daemons', DaemonExtension, daemonsList)
        daemonTemplatesConfigExtension = project.extensions.create('daemonsTemplates', DaemonTemplatesConfigExtension)
        defaultDefinition = project.extensions.create('daemonsDefaultDefinition', DefaultDaemonDefinitionExtension)

        // Add daemon to project
        addDaemonToProject(project, { Closure closure ->
            extension.daemon(closure)
        })

        extension.daemons.all { DaemonDefinition definition ->
            // Check existing name
            def sameName = daemonsList.any { !it.is(definition) && it.daemonName == definition.daemonName }
            if (sameName) {
                if (definition.daemonName) {
                    throw new IllegalArgumentException("A daemon with the name ${definition.daemonName} is already defined")
                } else {
                    throw new IllegalArgumentException("A daemon with no name, and hence the default, is already defined")
                }
            }

            project.tasks.withType(SystemPackagingTask) { SystemPackagingTask task ->
                boolean isRedhat = task instanceof Rpm
                DaemonDefinition defaults = getDefaultDaemonDefinition(isRedhat)

                // Calculate daemonName really early, but everything else can be done later.
                // tasks' package name wont' exists if it's a docker task
                def daemonName = definition.daemonName ?: defaults.daemonName ?: task.getPackageName() ?: project.name

                if (!daemonName) {
                    throw new IllegalArgumentException("Unable to find a name on definition ${definition}")
                }
                String cleanedName = daemonName.replaceAll("\\W", "").capitalize()


                def outputDirProvider = project.layout.buildDirectory.map { dir ->
                    new File(dir.asFile, "daemon/${cleanedName}/${task.name}")
                }

                String defaultInitDScriptLocationTemplate = isRedhat ? "/etc/rc.d/init.d/\${daemonName}" : "/etc/init.d/\${daemonName}"
                Map<String, String> templatesWithFileOutput = [
                        'log-run': defaultDefinition.runLogScriptLocation ?: "/service/\${daemonName}/log/run",
                        'run': defaultDefinition.runScriptLocation ?: "/service/\${daemonName}/run",
                        'initd': defaultDefinition.initDScriptLocation ?: defaultInitDScriptLocationTemplate
                ]

                def templateTaskProvider = project.tasks.register("${task.name}${cleanedName}Daemon", DaemonTemplateTask) {
                    it.conventionMapping.map('destDir') { outputDirProvider.get() }
                    it.conventionMapping.map('templatesFolder') {  daemonTemplatesConfigExtension.folder ?: DEFAULT_TEMPLATES_FOLDER  }
                    it.conventionMapping.map('context') {
                        Map<String,Object> context = toContext(defaults, definition)
                        context.daemonName = daemonName
                        context.isRedhat = isRedhat
                        context.installCmd = definition.installCmd ?: LegacyInstallCmd.create(context)
                        context
                    }
                    it.conventionMapping.map('templates') { templatesWithFileOutput.keySet() + POST_INSTALL_TEMPLATE }
                }

                task.dependsOn(templateTaskProvider)
                templatesWithFileOutput.each { String templateName, String destPathTemplate ->
                    File rendered = new File(outputDirProvider.get(), templateName) // To be created by task, ok that it's not around yet
                    String destPath = getDestPath(destPathTemplate, templateTaskProvider.get())
                    // Gradle CopySpec can't set the name of a file on the fly, we need to do a rename.
                    int slashIdx = destPath.lastIndexOf('/')
                    String destDir = destPath.substring(0,slashIdx)
                    String destFile = destPath.substring(slashIdx+1)
                    configureTask(task, rendered, destDir, destFile)
                }

                task.doFirst {
                    File postInstallCommand = new File(outputDirProvider.get(), POST_INSTALL_TEMPLATE)
                    task.postInstall(postInstallCommand.text)
                }
            }
        }
    }

    @CompileDynamic
    private void configureTask(SystemPackagingTask task, File rendered, String destDir, String destFile) {
        task.from(rendered) {
            into(destDir)
            rename('.*', destFile)
            FilePermissionUtil.setFilePermission(it, 0555)
            user 'root'
        }
    }

    @CompileDynamic
    private void addDaemonToProject(Project project, Closure closure) {
        project.ext.daemon = closure
    }

    private String getDestPath(String destPathTemplate, DaemonTemplateTask templateTask) {
        GStringTemplateEngine engine = new GStringTemplateEngine()
        def destPath = engine.createTemplate(destPathTemplate).make(templateTask.getContext()).toString()
        destPath
    }

    DaemonDefinition getDefaultDaemonDefinition(boolean isRedhat) {
        if (defaultDefinition.useExtensionDefaults)
            defaultDefinition
        else
            new DaemonDefinition(null, null, 'root', 'multilog t ./main', "./main", "nobody", isRedhat ? [3, 4, 5] : [2, 3, 4, 5], Boolean.TRUE, 85, 15)
    }
}
