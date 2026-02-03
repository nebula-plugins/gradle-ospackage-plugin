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

import groovy.text.GStringTemplateEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TemplateHelper {
    static final Logger logger = LoggerFactory.getLogger(TemplateHelper.class)

    private final GStringTemplateEngine engine = new GStringTemplateEngine()

    File destDir
    String templatesFolder
    File projectDir

    TemplateHelper(File destDir, String templatesFolder, File projectDir) {
        this.projectDir = projectDir
        this.destDir = destDir
        this.templatesFolder = templatesFolder
    }

    File generateFile(String templateName, Map context) {
        logger.info("Generating ${templateName} file...")
        context.each { key, value ->
            if (value == null) {
                throw new IllegalArgumentException("Context key $key has a null value")
            }
        }
        def template = getTemplateContent(templateName).newReader()
        def content = engine.createTemplate(template).make(context).toString()
        def contentFile = new File(destDir, templateName)
        destDir.mkdirs()
        contentFile.text = content
        return contentFile
    }

    private InputStream getTemplateContent(String templateName) {
        try {
            String path = templatesFolder ? "${templatesFolder}/${templateName}.tpl" : "${templateName}.tpl"
            InputStream stream = getClass().getResourceAsStream(path)
            if (stream) {
                return stream
            }
            // If path is absolute (and templatesFolder is not empty), use it directly; otherwise resolve from projectDir
            File templateFile = (templatesFolder && new File(templatesFolder).isAbsolute()) ?
                new File(path) : new File(projectDir, path)
            return templateFile.newInputStream()
        } catch(Exception e) {
            throw new FileNotFoundException("Could not find template $templateName in $templatesFolder")
        }
    }

}
