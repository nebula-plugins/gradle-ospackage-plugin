package com.netflix.gradle.plugins.daemon

import org.gradle.api.Project
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
        Project p = Mock(Project)
        TemplateHelper templateHelper = new TemplateHelper(destinationFolder.root, tmpFolder.root.path, p)

        when:
        templateHelper.generateFile("test", [:])

        then:
        thrown(FileNotFoundException)
    }

    def 'generates files does not fail with valid templates'() {
        Project p = Mock(Project)
        File initd = tmpFolder.newFile('initd.tpl')
        initd.text = """
              #!/bin/sh
        """

        TemplateHelper templateHelper = new TemplateHelper(destinationFolder.root, tmpFolder.root.path, p)

        when:
        templateHelper.generateFile("initd", [:])

        then:
        interaction {
            p.file(_) >> initd
        }
        notThrown(FileNotFoundException)
    }
}
