package com.netflix.gradle.plugins.deb

import groovy.text.GStringTemplateEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TemplateHelper {
    static final Logger logger = LoggerFactory.getLogger(DebCopyAction.class)

    private final GStringTemplateEngine engine = new GStringTemplateEngine()

    File destDir
    String templatePrefix

    TemplateHelper(File destDir, String templatePrefix) {
        this.destDir = destDir
        this.templatePrefix = templatePrefix
    }

    File generateFile(String templateName, Map context) {
        logger.info("Generating ${templateName} file...")
        def template = getClass().getResourceAsStream("${templatePrefix}/${templateName}.ftl").newReader()
        def content = engine.createTemplate(template).make(context).toString()
        def contentFile = new File(destDir, templateName)
        destDir.mkdirs()
        contentFile.text = content
        return contentFile
    }

}
