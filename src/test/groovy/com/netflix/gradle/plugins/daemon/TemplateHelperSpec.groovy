package com.netflix.gradle.plugins.daemon

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Subject

@Subject(TemplateHelper)
class TemplateHelperSpec extends Specification {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder()

    @Rule
    public TemporaryFolder destinationFolder = new TemporaryFolder()

    def 'fails if file is not present'() {
        given:
        TemplateHelper templateHelper = new TemplateHelper(destinationFolder.root, tmpFolder.root.path, tmpFolder.root)

        when:
        templateHelper.generateFile("test", [:])

        then:
        thrown(FileNotFoundException)
    }

    def 'generates files does not fail with valid templates'() {
        // Use a unique template name that won't conflict with classpath resources
        File testTemplate = tmpFolder.newFile('test-custom-template.tpl')
        testTemplate.text = """
              #!/bin/sh
        """

        TemplateHelper templateHelper = new TemplateHelper(destinationFolder.root, "", tmpFolder.root)

        when:
        templateHelper.generateFile("test-custom-template", [:])

        then:
        notThrown(FileNotFoundException)
    }
}
