package com.netflix.gradle.plugins.packaging

import com.netflix.gradle.plugins.deb.Deb
import com.netflix.gradle.plugins.rpm.Rpm
import com.netflix.gradle.plugins.rpm.Scanner
import nebula.test.ProjectSpec
import org.apache.commons.io.FileUtils
import org.redline_rpm.header.Architecture
import org.redline_rpm.header.Os

import static org.redline_rpm.header.Header.HeaderTag.*

class SystemPackagingBasePluginTest extends ProjectSpec {
    def 'callExtensionDynamically'() {

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()

        when:
        project.apply plugin: 'os-package-base'

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

        File srcDir = new File(project.buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        when:
        project.apply plugin: 'os-package-base'

        project.ospackage {
            release = 3
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

        def arch = rpmTask.getArch()
        Architecture.I386.name() == arch
    }


    def 'execute both tasks'() {
        project.version = '1.0.0'
        project.description = 'Test Description'

        File srcDir = new File(project.buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.getPlugins().apply(SystemPackagingBasePlugin.class)

        when:
        ProjectPackagingExtension ext = project.getConvention().getByType(ProjectPackagingExtension)
        ext.with {
            release = 3
            requires 'awesomesauce'
            url 'http://notawesome.com'
            into '/opt/bleah'
            from(srcDir)
        }

        Deb debTask = project.task([type: Deb], 'buildDeb', {})
        Rpm rpmTask = project.task([type: Rpm], 'buildRpm', {
            arch I386.name()
        })

        debTask.execute()
        rpmTask.execute()

        then:
        def debScanner = new com.netflix.gradle.plugins.deb.Scanner(debTask.getArchivePath())
        debScanner.getHeaderEntry('Version').endsWith("-3")
        'awesomesauce' == debScanner.getHeaderEntry('Depends')
        'execute-both-tasks' == debScanner.getHeaderEntry('Provides')
        'execute-both-tasks\n Test Description' == debScanner.getHeaderEntry('Description')
        'http://notawesome.com' == debScanner.getHeaderEntry('Homepage')

        def file = debScanner.getEntry('./opt/bleah/apple')
        file.isFile()

        def rpmScanner = Scanner.scan(rpmTask.getArchivePath())
        '3' == Scanner.getHeaderEntryString(rpmScanner, RELEASE)
        Scanner.getHeaderEntryString(rpmScanner, REQUIRENAME).contains('awesomesauce')
        'i386' == Scanner.getHeaderEntryString(rpmScanner, ARCH)
        ['./opt/bleah', './opt/bleah/apple'] == rpmScanner.files*.name

    }
}
