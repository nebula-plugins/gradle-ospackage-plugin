package com.netflix.gradle.plugins.packaging

import com.netflix.gradle.plugins.deb.Deb
import com.netflix.gradle.plugins.rpm.Rpm
import com.netflix.gradle.plugins.rpm.Scanner
import nebula.test.ProjectSpec
import org.vafer.jdeb.shaded.commons.io.FileUtils
import org.redline_rpm.header.Os
import spock.lang.Issue

import static org.redline_rpm.header.Header.HeaderTag.*

class SystemPackagingBasePluginTest extends ProjectSpec {
    def 'callExtensionDynamically'() {

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()

        when:
        project.apply plugin: 'com.netflix.nebula.ospackage-base'

        project.ospackage {
            release '3'
            into '/opt/bleah'
            from(srcDir)
        }

        then:
        noExceptionThrown()
        // No Exception Thrown
    }

    def 'useAliasesInExtension'() {
        project.version = '1.0.0'
        project.description = 'Test Description'

        File srcDir = new File(project.layout.buildDirectory.getAsFile().get(), 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        when:
        project.apply plugin: 'com.netflix.nebula.ospackage-base'

        project.ospackage {
            release = '3'
            into '/opt/bleah'
            os = LINUX
            from(srcDir)
        }

        Rpm rpmTask = project.task([type: Rpm], 'buildRpm', {
            arch I386.name()
        })

        then:
        def os = rpmTask.getOs()
        Os.LINUX == os

        def arch = rpmTask.getArchString()
        'i386' == arch
    }


    def 'execute both tasks'() {
        project.version = '1.0.0'
        project.description = 'Test Description'

        File srcDir = new File(project.layout.buildDirectory.getAsFile().get(), 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.getPlugins().apply(SystemPackagingBasePlugin.class)

        when:
        ProjectPackagingExtension ext = project.getExtensions().getByType(ProjectPackagingExtension)
        ext.with {
            provides project.name
            release = '3'
            requires 'awesomesauce'
            url 'http://notawesome.com'
            into '/opt/bleah'
            from(srcDir)
        }

        Deb debTask = project.task([type: Deb], 'buildDeb', {})
        Rpm rpmTask = project.task([type: Rpm], 'buildRpm', {
            arch I386.name()
        })

        debTask.copy()
        rpmTask.copy()

        then:
        def debScanner = new com.netflix.gradle.plugins.deb.Scanner(debTask.archiveFile.get().asFile)
        debScanner.getHeaderEntry('Version').endsWith("-3")
        'awesomesauce' == debScanner.getHeaderEntry('Depends')
        'execute-both-tasks' == debScanner.getHeaderEntry('Provides')
        'execute-both-tasks\n Test Description' == debScanner.getHeaderEntry('Description')
        'http://notawesome.com' == debScanner.getHeaderEntry('Homepage')

        def file = debScanner.getEntry('./opt/bleah/apple')
        file.isFile()

        def rpmScanner = Scanner.scan(rpmTask.archiveFile.get().asFile)
        '3' == Scanner.getHeaderEntryString(rpmScanner, RELEASE)
        Scanner.getHeaderEntryString(rpmScanner, REQUIRENAME).contains('awesomesauce')
        'i386' == Scanner.getHeaderEntryString(rpmScanner, ARCH)
        'execute-both-tasks' == Scanner.getHeaderEntryString(rpmScanner, PROVIDENAME)
        ['./opt/bleah', './opt/bleah/apple'] == rpmScanner.files*.name

    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/365")
    def 'provides in ospackage propagates to rpm and deb'() {
        given:
        project.version = '1.0.0'

        when:
        project.apply plugin: 'com.netflix.nebula.ospackage-base'

        project.ospackage {
            release '1'
            provides 'virtualPackageA', '1.2.3'
            provides 'virtualPackageB'
        }

        Deb debTask = project.task([type: Deb], 'buildDeb', {})
        Rpm rpmTask = project.task([type: Rpm], 'buildRpm', {})

        debTask.copy()
        rpmTask.copy()

        then:
        def debScanner = new com.netflix.gradle.plugins.deb.Scanner(debTask.archiveFile.get().asFile)
        'virtualPackageA (= 1.2.3), virtualPackageB' == debScanner.getHeaderEntry('Provides')

        def rpmScanner = Scanner.scan(rpmTask.archiveFile.get().asFile)
        [project.name, 'virtualPackageA', 'virtualPackageB'] == Scanner.getHeaderEntry(rpmScanner, PROVIDENAME).values
        ['0:1.0.0-1', '1.2.3', ''] == Scanner.getHeaderEntry(rpmScanner, PROVIDEVERSION).values
    }
}
