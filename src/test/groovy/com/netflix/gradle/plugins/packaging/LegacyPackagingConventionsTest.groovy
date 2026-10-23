package com.netflix.gradle.plugins.packaging

import com.netflix.gradle.plugins.deb.control.MultiArch
import nebula.test.ProjectSpec
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.IConventionAware
import org.gradle.api.provider.Provider
import org.redline_rpm.header.Os
import org.redline_rpm.header.RpmType
import org.redline_rpm.payload.Directive
import spock.lang.Unroll

import java.beans.Introspector

class LegacyPackagingConventionsTest extends ProjectSpec {
    ProjectPackagingExtension extension

    def setup() {
        extension = project.extensions.create('ospackage', ProjectPackagingExtension, project)
    }

    @Unroll
    def 'legacy #propertyName convention is shared by both extension APIs'() {
        given:
        def mapping = ((IConventionAware) extension).conventionMapping
        mapping.map(propertyName) { value }

        when:
        extension.options.adoptLegacyConventions(mapping, project.objects)

        then:
        plainValue(extension.options."${propertyName}".get()) == value
        extension."${propertyName}" == value

        where:
        [propertyName, value] << legacyValues().collect { name, value -> [name, value] }
    }

    def 'fixtures cover every provider-backed packaging property'() {
        expect:
        legacyValues().keySet() == SystemPackagingOptions.declaredMethods
                .findAll { it.name.startsWith('get') && Provider.isAssignableFrom(it.returnType) }
                .collect { Introspector.decapitalize(it.name.substring(3)) }.toSet()
    }

    @Unroll
    def 'explicit #api #propertyName overrides its legacy convention'() {
        given:
        def mapping = ((IConventionAware) extension).conventionMapping
        mapping.map(propertyName) { legacyValues()[propertyName] }
        if (api == 'legacy') {
            extension."${propertyName}" = explicitValue
        } else {
            def property = extension.options."${propertyName}"
            if (property instanceof RegularFileProperty) property.fileValue(explicitValue)
            else property.set(explicitValue)
        }

        when:
        extension.options.adoptLegacyConventions(mapping, project.objects)

        then:
        plainValue(extension.options."${propertyName}".get()) == explicitValue
        extension."${propertyName}" == explicitValue

        where:
        [api, propertyName, explicitValue] << ['legacy', 'options'].collectMany { api ->
            [release: 'custom', uid: 0, setgid: false,
             postInstallFile: new File('explicit-postinst').absoluteFile,
             multiArch: MultiArch.NO, dependencies: [], customFields: [:]]
                    .collect { name, value -> [api, name, value] }
        }
    }

    def 'null legacy defaults preserve modern conventions without realizing their providers'() {
        given:
        boolean queried = false
        extension.options.release.convention(project.providers.provider {
            queried = true
            'modern'
        })
        def mapping = ((IConventionAware) extension).conventionMapping
        mapping.map('release') { null }

        when:
        extension.options.adoptLegacyConventions(mapping, project.objects)

        then:
        !queried
        extension.release == 'modern'
        queried
    }

    private static Object plainValue(Object value) {
        value instanceof RegularFile ? value.asFile : value
    }

    private static Map<String, Object> legacyValues() {
        def script = new File('legacy-script').absoluteFile
        def dependency = new Dependency('legacy-dependency', '1.0')
        def trigger = new Trigger(dependency, script)
        def values = [:]
        ['packageName', 'release', 'version', 'user', 'permissionGroup', 'packageGroup',
         'buildHost', 'summary', 'packageDescription', 'license', 'packager',
         'distribution', 'vendor', 'url', 'sourcePackage', 'archStr', 'maintainer',
         'uploaders', 'priority', 'signingKeyId', 'signingKeyPassphrase'].each {
            values[it] = "legacy-${it}".toString()
        }
        values.putAll([epoch: 2, uid: 1001, gid: 1002,
                       setgid: true, createDirectoryEntry: true, addParentDirs: false,
                       fileType: Directive.CONFIG, os: Os.LINUX, type: RpmType.BINARY,
                       multiArch: MultiArch.FOREIGN])
        ['signingKeyRingFile', 'preInstallFile', 'postInstallFile', 'preUninstallFile',
         'postUninstallFile', 'triggerInstallFile', 'triggerUninstallFile',
         'triggerPostUninstallFile'].each { values[it] = script }
        ['prefixes', 'configurationFiles', 'preInstallCommands', 'postInstallCommands',
         'preUninstallCommands', 'postUninstallCommands', 'preTransCommands',
         'postTransCommands', 'commonCommands', 'supplementaryControlFiles'].each {
            values[it] = ["legacy-${it}".toString()]
        }
        ['triggerInstallCommands', 'triggerUninstallCommands', 'triggerPostUninstallCommands'].each {
            values[it] = [trigger]
        }
        ['dependencies', 'obsoletes', 'conflicts', 'recommends', 'suggests', 'enhances',
         'preDepends', 'breaks', 'replaces', 'provides'].each { values[it] = [dependency] }
        values.links = [new Link(path: '/opt/current', target: '/opt/legacy')]
        values.directories = [new Directory(path: '/opt/legacy')]
        values.customFields = ['Build-Origin': 'legacy']
        values
    }
}
