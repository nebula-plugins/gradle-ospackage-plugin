package com.netflix.gradle.plugins.packaging

import com.netflix.gradle.plugins.deb.Deb
import com.netflix.gradle.plugins.rpm.Rpm
import com.netflix.gradle.plugins.rpm.Scanner
import nebula.test.ProjectSpec
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Issue
import spock.lang.Unroll

import static org.redline_rpm.header.Header.HeaderTag.DESCRIPTION

class SystemPackagingPluginTest extends ProjectSpec {
    def 'tasks created'() {

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()

        when:
        project.apply plugin: 'nebula.ospackage'

        then:
        project.getPlugins().getPlugin(SystemPackagingPlugin) != null
        project.getPlugins().getPlugin(SystemPackagingBasePlugin) != null

        def ext = project.getExtensions().getByType(ProjectPackagingExtension)
        ext != null

        def debTask = project.tasks.getByName('buildDeb')
        debTask instanceof Deb

        def rpmTask = project.tasks.getByName('buildRpm')
        rpmTask instanceof Rpm

    }

    def 'inputsSetFromExtension'() {
        Project project = ProjectBuilder.builder().build()

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()

        // Create some content, or else source will be empty
        new File(srcDir, 'a.java').text = "public class A { }"

        when:
        project.apply plugin: 'nebula.ospackage'

        def ext = project.getExtensions().getByType(ProjectPackagingExtension)
        ext.from(srcDir)

        then:
        Deb debTask = project.tasks.getByName('buildDeb')

        then: "Task should have inputs from extension"
        debTask.inputs.hasInputs

        then: "Should have sourceFiles from extension"
        debTask.inputs.hasSourceFiles

        then: "Should have source from extension"
        !debTask.getSource().empty

        then: "Task should have output"
        debTask.outputs.hasOutput

    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/104")
    @Unroll
    def "Translates extension packageDescription '#description' to header entry for RPM task"() {
        given:
        project.apply plugin: 'nebula.ospackage'

        project.ospackage {
            packageName = 'bleah'
            packageDescription = description
            version = '1.0'
        }

        when:
        project.tasks.getByName('buildRpm').execute()

        then:
        def scan = Scanner.scan(project.file('build/distributions/bleah-1.0.noarch.rpm'))
        Scanner.getHeaderEntryString(scan, DESCRIPTION) == headerEntry

        where:
        description             | headerEntry
        'This is a description' | 'This is a description'
        ''                      | ''
        null                    | ''
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/104")
    @Unroll
    def "Translates extension packageDescription '#description' to header entry for Debian task"() {
        given:
        project.apply plugin: 'nebula.ospackage'

        project.ospackage {
            packageName = 'translates-extension-description'
            packageDescription = description
        }

        when:
        Task buildDep = project.tasks.getByName('buildDeb')
        buildDep.execute()

        then:
        def scan = new com.netflix.gradle.plugins.deb.Scanner(buildDep.archivePath)
        scan.getHeaderEntry('Description') == headerEntry

        where:
        description             | headerEntry
        'This is a description' | 'translates-extension-description\n This is a description'
        ''                      | 'translates-extension-description'
        null                    | 'translates-extension-description'
    }
}
