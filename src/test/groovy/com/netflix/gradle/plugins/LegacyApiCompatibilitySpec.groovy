package com.netflix.gradle.plugins

import com.netflix.gradle.plugins.application.OspackageApplicationExtension
import com.netflix.gradle.plugins.application.OspackageApplicationPlugin
import com.netflix.gradle.plugins.daemon.DaemonTemplateTask
import com.netflix.gradle.plugins.deb.Deb
import com.netflix.gradle.plugins.packaging.Dependency
import com.netflix.gradle.plugins.packaging.ProjectPackagingExtension
import com.netflix.gradle.plugins.packaging.SystemPackagingBasePlugin
import com.netflix.gradle.plugins.packaging.SystemPackagingExtension
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class LegacyApiCompatibilitySpec extends Specification {
    def 'v12 getter signatures and application extension class remain available'() {
        expect:
        SystemPackagingExtension.getMethod('getPackageName').returnType == String
        SystemPackagingExtension.getMethod('getDependencies').returnType == List
        SystemPackagingExtension.getMethod('getPreInstallFile').returnType == File
        OspackageApplicationExtension.getMethod('getPrefix').returnType == String
        OspackageApplicationExtension.getConstructor() != null
        DaemonTemplateTask.getMethod('getContext').returnType == Map
        DaemonTemplateTask.getMethod('getTemplates').returnType == Collection
        DaemonTemplateTask.getMethod('getDestDir').returnType == File
        SystemPackagingBasePlugin.getMethod('createExtension') != null
    }

    def 'legacy values and mutable collections update lazy task inputs'() {
        given:
        def project = ProjectBuilder.builder().build()
        SystemPackagingExtension extension = project.objects.newInstance(SystemPackagingExtension)

        when:
        extension.packageName = 'legacy-name'
        extension.preInstallFile = new File('preinst')
        extension.dependencies << new Dependency('libfoo', '')
        extension.customFields['X-Legacy'] = 'yes'
        def entry = extension.customFields.entrySet().first()
        entry.setValue('updated')

        then:
        extension.packageName == 'legacy-name'
        extension.packageNameProperty.get() == 'legacy-name'
        extension.preInstallFile.name == 'preinst'
        extension.preInstallFileProperty.get().asFile.name == 'preinst'
        extension.dependencies*.packageName == ['libfoo']
        extension.dependenciesProperty.get()*.packageName == ['libfoo']
        extension.customFields['X-Legacy'] == 'updated'
        extension.customFieldsProperty.get()['X-Legacy'] == 'updated'
        entry.value == 'updated'
    }

    def 'legacy daemon getters delegate to tracked properties'() {
        given:
        def project = ProjectBuilder.builder().build()
        DaemonTemplateTask task = project.tasks.create('legacyDaemon', DaemonTemplateTask)

        when:
        task.context = [daemonName: 'legacy']
        task.templates = ['service']
        task.destDir = new File(project.buildDir, 'legacy')
        task.templatesFolder = 'templates'
        task.context['extra'] = 'value'
        task.templates << 'postInstall'

        then:
        task.context.daemonName == 'legacy'
        task.context.extra == 'value'
        task.contextProperty.get().daemonName == 'legacy'
        task.contextProperty.get().extra == 'value'
        task.templates == ['service', 'postInstall']
        task.templatesProperty.get() == ['service', 'postInstall']
        task.destDir == new File(project.buildDir, 'legacy')
        task.templatesFolder == 'templates'
    }

    def 'legacy task collection mutation reaches the packaging input'() {
        given:
        def project = ProjectBuilder.builder().build()
        Deb task = project.tasks.create('legacyDeb', Deb)

        when:
        task.dependencies << new Dependency('task-lib', '')

        then:
        task.dependencies*.packageName == ['task-lib']
        task.exten.dependenciesProperty.get()*.packageName == ['task-lib']
    }

    def 'legacy application prefix convention remains visible'() {
        given:
        def project = ProjectBuilder.builder().build()
        OspackageApplicationPlugin plugin = project.plugins.apply(OspackageApplicationPlugin)

        when:
        plugin.extension.conventionMapping.map('prefix') { '/apps' }

        then:
        plugin.extension.prefix == '/apps'
    }

    def 'legacy no-argument extension override is called'() {
        given:
        def project = ProjectBuilder.builder().build()

        when:
        project.plugins.apply(LegacyBasePlugin)

        then:
        project.plugins.getPlugin(LegacyBasePlugin).created
    }

    static class LegacyBasePlugin extends SystemPackagingBasePlugin {
        boolean created

        @Override
        ProjectPackagingExtension createExtension() {
            created = true
            super.createExtension()
        }
    }
}
