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

import com.netflix.gradle.plugins.packaging.SystemPackagingBasePlugin
import com.netflix.gradle.plugins.packaging.SystemPackagingTask
import com.netflix.gradle.plugins.rpm.Rpm
import com.netflix.gradle.plugins.utils.BackwardsCompatibleDomainObjectCollectionFactory
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project

class OspackageDaemonPlugin implements Plugin<Project> {
    Project project
    DaemonExtension extension
    DaemonTemplatesConfigExtension daemonTemplatesConfigExtension

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
        ]
    }

    @Override
    void apply(Project project) {
        this.project = project
        project.plugins.apply(SystemPackagingBasePlugin)

        def factory = new BackwardsCompatibleDomainObjectCollectionFactory<>(project.gradle.gradleVersion)
        DomainObjectCollection<DaemonDefinition> daemonsList = factory.create(DaemonDefinition)
        extension = project.extensions.create('daemons', DaemonExtension, daemonsList)
        daemonTemplatesConfigExtension = project.extensions.create('daemonsTemplates', DaemonTemplatesConfigExtension)

        // Add daemon to project
        project.ext.daemon = { Closure closure ->
            extension.daemon(closure)
        }

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
                def isRedhat = task instanceof Rpm
                DaemonDefinition defaults = getDefaultDaemonDefinition(isRedhat)

                // Calculate daemonName really early, but everything else can be done later.
                // tasks' package name wont' exists if it's a docker task
                def daemonName = definition.daemonName ?: defaults.daemonName ?: task.getPackageName() ?: project.name

                if (!daemonName) {
                    throw new IllegalArgumentException("Unable to find a name on definition ${definition}")
                }
                String cleanedName = daemonName.replaceAll("\\W", "").capitalize()


                def outputDir = new File(project.buildDir, "daemon/${cleanedName}/${task.name}")

                def mapping = [
                        'log-run': "/service/${daemonName}/log/run",
                        'run': "/service/${daemonName}/run",
                        'initd': isRedhat?"/etc/rc.d/init.d/${daemonName}":"/etc/init.d/${daemonName}"
                ]

                def templateTask = project.tasks.create("${task.name}${cleanedName}Daemon", DaemonTemplateTask)
                templateTask.conventionMapping.map('destDir') { outputDir }
                templateTask.conventionMapping.map('templatesFolder') {  daemonTemplatesConfigExtension.folder ?: DEFAULT_TEMPLATES_FOLDER  }
                templateTask.conventionMapping.map('context') {
                    Map<String, String> context = toContext(defaults, definition)
                    context.daemonName = daemonName
                    context.isRedhat = isRedhat
                    context
                }
                templateTask.conventionMapping.map('templates') { mapping.keySet() }

                task.dependsOn(templateTask)
                mapping.each { String templateName, String destPath ->
                    File rendered = new File(outputDir, templateName) // To be created by task, ok that it's not around yet

                    // Gradle CopySpec can't set the name of a file on the fly, we need to do a rename.
                    def slashIdx = destPath.lastIndexOf('/')
                    def destDir = destPath.substring(0,slashIdx)
                    def destFile = destPath.substring(slashIdx+1)
                    task.from(rendered) {
                        into(destDir)
                        rename('.*', destFile)
                        fileMode 0555 // Since source files don't have the correct permissions
                        user 'root'
                    }
                }

                task.doFirst {
                    task.postInstall("[ -x /bin/touch ] && touch=/bin/touch || touch=/usr/bin/touch")
                    task.postInstall("\$touch /service/${daemonName}/down")
                    def ctx = templateTask.getContext()

                    def installCmd = definition.installCmd ?: LegacyInstallCmd.create(ctx)

                    if (ctx.autoStart) {
                        task.postInstall(installCmd)
                    }

                }
            }
        }
    }

    def getDefaultDaemonDefinition(boolean isRedhat) {
        new DaemonDefinition(null, null, 'root', 'multilog t ./main', "./main", "nobody", isRedhat ? [3, 4, 5] : [2, 3, 4, 5], Boolean.TRUE, 85, 15)
    }
}
