package com.netflix.gradle.plugins.packaging

import com.netflix.gradle.plugins.deb.Deb
import com.netflix.gradle.plugins.rpm.Rpm
import nebula.test.ProjectSpec
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Issue
import spock.lang.Unroll

class SystemPackagingPluginTest extends ProjectSpec {
    def 'tasks created'() {

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()

        when:
        project.apply plugin: 'com.netflix.nebula.ospackage'

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
        project.apply plugin: 'com.netflix.nebula.ospackage'

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

    def 'legacy task methods retain their typed results and live collections'() {
        given:
        project.apply plugin: 'com.netflix.nebula.ospackage'
        Deb deb = project.tasks.getByName('buildDeb')

        when:
        Dependency required = deb.requires('first')
        deb.dependencies.add(new Dependency('second', '', 0))
        deb.setSetgid(true)

        then:
        required.packageName == 'first'
        deb.exten.options.dependencies.get()*.packageName == ['first', 'second']
        deb.setgid
        SystemPackagingTask.getMethod('requires', String).returnType == Dependency
        SystemPackagingTask.getMethod('setSetgid', boolean).returnType == void
    }

    def 'legacy task collection getters retain their 12.x element types'() {
        expect:
        [
            (Trigger): ['TriggerInstallCommands', 'TriggerUninstallCommands', 'TriggerPostUninstallCommands'],
            (Link): ['Links'],
            (Dependency): ['Dependencies', 'Obsoletes', 'Conflicts', 'Recommends', 'Suggests',
                           'Enhances', 'PreDepends', 'Breaks', 'Replaces', 'Provides'],
            (Directory): ['Directories']
        ].each { Class elementType, List<String> propertyNames ->
            propertyNames.each { String propertyName ->
                assert SystemPackagingTask.getMethod("get${propertyName}").genericReturnType.actualTypeArguments[0] == elementType
            }
        }
        SystemPackagingTask.getMethod('setDependencies', List).genericParameterTypes[0] == List
        SystemPackagingTask.getMethod('setCustomFields', Map).genericParameterTypes[0] == Map
    }

}
