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
        TemplateHelper templateHelper = new TemplateHelper(destinationFolder.root, tmpFolder.root.path)

        when:
        templateHelper.generateFile("test", [:])

        then:
        thrown(FileNotFoundException)
    }

    def 'generates files does not fail with valid templates'() {
        File initd = tmpFolder.newFile('initd.tpl')
        initd.text = """
              #!/bin/sh
        """

        TemplateHelper templateHelper = new TemplateHelper(destinationFolder.root, tmpFolder.root.path)

        when:
        templateHelper.generateFile("initd", [:])

        then:
        notThrown(FileNotFoundException)
    }
}
