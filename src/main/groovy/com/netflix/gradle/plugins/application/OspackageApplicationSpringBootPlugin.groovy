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
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.application.CreateStartScripts
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
 *         apply plugin: 'com.netflix.nebula.ospackage-application-spring-boot'
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

    @Override
    void apply(Project project) {
        project.plugins.apply(OspackageApplicationPlugin)

        // Validate Spring Boot plugin at configuration time using plugins.withId
        // This automatically waits for plugin application
        boolean springBootFound = false
        project.plugins.withId("org.springframework.boot") {
            springBootFound = true
        }

        // Only validate after other plugins have a chance to apply
        project.afterEvaluate {
            if (!springBootFound) {
                project.logger.error("The '{}' plugin requires the '{}' plugin.",
                        "com.netflix.nebula.ospackage-application-spring-boot",
                        "org.springframework.boot")
                throw new RuntimeException("The 'com.netflix.nebula.ospackage-application-spring-boot' plugin requires the 'org.springframework.boot' plugin.")
            }
        }

        project.plugins.withId("org.springframework.boot") {
            // Spring Boot 2.0+ configured distributions that have everything we need
            if (project.distributions.findByName('boot') != null) {
                project.logger.info("Spring Boot 2+ detected")
                // Use the main distribution and configure it to have the same baseName as the boot distribution
                project.tasks.named(JavaPlugin.JAR_TASK_NAME) {
                    enabled = true
                }
                project.afterEvaluate {
                    project.tasks.named('bootJar').configure {
                        if (GradleVersion.current().baseVersion < GradleVersion.version('6.0').baseVersion) {
                            classifier = 'boot'
                        } else {
                            archiveClassifier = 'boot'
                        }
                    }
                    project.distributions {
                        main {
                            if (GradleVersion.current().baseVersion < GradleVersion.version('6.0').baseVersion) {
                                baseName = "${project.distributions.main.baseName}-boot"
                            } else {
                                getDistributionBaseName().set "${project.distributions.main.getDistributionBaseName().getOrNull()}-boot"
                            }
                        }
                    }

                    // Allow the springBoot extension configuration to propagate to the application plugin
                    def mainClass = project.objects.property(String)
                    try {
                        mainClass.set(project.springBoot.mainClass)
                    } catch (Exception ignore) {
                        mainClass.set(project.springBoot.mainClassName)
                    }
                    if (!mainClass.isPresent()) {
                        try {
                            mainClass.set(project.application.mainClass.isPresent() ? project.application.mainClass.get() : project.application.mainClassName)
                        } catch (Exception ignore) {
                        }
                    }
                    if (GradleVersion.current().baseVersion < GradleVersion.version('6.4').baseVersion) {
                        if (project.application.mainClassName == null) {
                            project.application.mainClassName = mainClass.getOrNull()
                            // Fail only when startScripts runs
                        }
                    } else {
                        project.application.mainClass.convention(mainClass)
                    }
                }

                // Workaround for https://github.com/gradle/gradle/issues/16371
                if (GradleVersion.current().baseVersion >= GradleVersion.version('6.4').baseVersion) {
                    project.tasks.named(ApplicationPlugin.TASK_START_SCRIPTS_NAME).configure {
                        doFirst {
                            if (!project.application.mainClass.isPresent()) {
                                throw new GradleException("mainClass should be configured in order to generate a valid start script. i.e. mainClass = 'com.netflix.app.MyApp'")
                            }
                        }
                    }
                }
            } else {
                project.logger.info("Spring Boot 1 detected")
                project.afterEvaluate {
                    project.tasks.named(DistributionPlugin.TASK_INSTALL_NAME).configure {
                        it.dependsOn('bootRepackage')
                    }
                    project.tasks.named(ApplicationPlugin.TASK_START_SCRIPTS_NAME).configure { CreateStartScripts createStartScripts ->
                        createStartScripts.mainClassName = 'org.springframework.boot.loader.JarLauncher'
                    }

                    // `ApplicationPlugin` automatically adds `runtimeClasspath` files to the distribution. We want most of that
                    // stripped out since we want just the fat jar that Spring produces.
                    project.distributions {
                        main {
                            contents {
                                into('lib') {
                                    def jarTaskProvider = project.tasks.named(JavaPlugin.JAR_TASK_NAME)
                                    project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).files.findAll { file ->
                                        file.getName() != jarTaskProvider.get().outputs.files.singleFile.name
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
}