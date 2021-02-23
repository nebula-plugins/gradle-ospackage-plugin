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

package com.netflix.gradle.plugins.application

import groovy.transform.CompileDynamic
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.tooling.BuildException
import org.gradle.util.GradleVersion

/**
 * Combine the os-package with the Application plugin. Currently heavily opinionated to where
 * the code will live, though that is slightly configurable using the ospackage_application extension.
 *
 * Usage:
 * <ul>
 *     <li>
 *       <pre>
 *         apply plugin: 'org.springframework.boot'
 *         apply plugin: 'nebula.ospackage-application-spring-boot'
 *
 *         dependencies {*           implementation 'org.springframework.boot:spring-boot-starter'
 *
 *           testCompile 'org.springframework.boot:spring-boot-starter-test'
 *}*       </pre>
 *     </li>
 *     <li>{@code $ ./gradlew buildDeb}</li>
 *     <li>{@code $ ./gradlew run}</li>
 * </ul>
 */
@CompileDynamic
class OspackageApplicationSpringBootPlugin implements Plugin<Project> {
    OspackageApplicationExtension extension

    @Override
    void apply(Project project) {
        project.plugins.apply(OspackageApplicationPlugin)

        if (!project.plugins.hasPlugin('org.springframework.boot')) {
            throw new IllegalStateException("The 'org.springframework.boot' plugin must be applied before applying this plugin")
        }

        // Spring Boot 2.0 configures distributions that have everything we need
        OspackageApplicationExtension extension = project.extensions.getByType(OspackageApplicationExtension)
        if (project.distributions.findByName('boot') != null) {
            // Use the main distribution and configure it to have the same baseName as the boot distribution
            project.jar {
                enabled = true
            }
            project.afterEvaluate {
                project.bootJar {
                    if(GradleVersion.current().baseVersion < GradleVersion.version('6.0').baseVersion) {
                        classifier = "boot"
                    } else {
                        archiveClassifier = "boot"
                    }
                }
                project.distributions {
                    main {
                        if(GradleVersion.current().baseVersion < GradleVersion.version('6.0').baseVersion) {
                            baseName = "${project.distributions.main.baseName}-boot"
                        } else {
                            getDistributionBaseName().set "${project.distributions.main.getDistributionBaseName().getOrNull()}-boot"
                        }
                    }
                }

                if(GradleVersion.current().baseVersion < GradleVersion.version('6.4').baseVersion) {
                    // Allow the springBoot extension configuration to propagate to the application plugin
                    if (project.application.mainClassName == null) {
                        project.application.mainClassName = project.springBoot.mainClassName
                    }
                    if(!project.application.mainClassName) {
                        throw new GradleException("mainClassName should be configured in order to generate a valid debian file. Ex. mainClassName = 'com.netflix.app.MyApp'")
                    }
                } else {
                    // Allow the springBoot extension configuration to propagate to the application plugin
                    if (!project.application.mainClass.isPresent()) {
                        project.application.mainClass.set(project.springBoot.mainClassName)
                    }

                    if(!project.application.mainClass.isPresent()) {
                        throw new GradleException("mainClass should be configured in order to generate a valid debian file. Ex. mainClass.set('com.netflix.app.MyApp')")
                    }
                }
            }
        } else {
            project.tasks.getByName(DistributionPlugin.TASK_INSTALL_NAME).dependsOn('bootRepackage')
            CreateStartScripts createStartScripts = project.tasks.getByName(ApplicationPlugin.TASK_START_SCRIPTS_NAME) as CreateStartScripts
            createStartScripts.mainClassName = 'org.springframework.boot.loader.JarLauncher'

            // `ApplicationPlugin` automatically adds `runtimeClasspath` files to the distribution. We want most of that
            // stripped out since we want just the fat jar that Spring produces.
            project.afterEvaluate {
                project.distributions {
                    main {
                        contents {
                            into('lib') {
                                project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).files.findAll { file ->
                                    file.getName() != project.tasks.getByName(JavaPlugin.JAR_TASK_NAME).outputs.files.singleFile.name
                                }.each { file ->
                                    exclude file.name
                                }
                            }
                        }
                    }
                }
            }
        }


    }
}
