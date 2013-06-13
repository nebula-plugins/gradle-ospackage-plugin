package com.trigonic.gradle.plugins.packaging

import com.trigonic.gradle.plugins.deb.Deb
import com.trigonic.gradle.plugins.rpm.Rpm
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import com.trigonic.gradle.plugins.rpm.Scanner
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.freecompany.redline.header.Header.HeaderTag.*

class SystemPackagingPluginTest {
    @Test
    public void callExtensionDynamically() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()

        project.apply plugin: 'os-package'

        project.ospackage {
            release '3'
            into '/opt/bleah'
            from(srcDir)
        }
        // No Exception Thrown
    }

    @Test
    public void executeBothTasks() {
        Project project = ProjectBuilder.builder().build()
        project.version = '1.0.0'
        project.description = 'Test Description'

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.getPlugins().apply(SystemPackagingPlugin.class)

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
            arch I386
        })

        debTask.execute()
        rpmTask.execute()

        def debScanner = new com.trigonic.gradle.plugins.deb.Scanner(debTask.getArchivePath())
        assertTrue(debScanner.getHeaderEntry('Version').endsWith("-3"))
        assertEquals('awesomesauce', debScanner.getHeaderEntry('Depends'))
        assertEquals('test', debScanner.getHeaderEntry('Provides'))
        assertEquals('test\n Test Description', debScanner.getHeaderEntry('Description'))
        assertEquals('http://notawesome.com', debScanner.getHeaderEntry('Homepage'))

        def file = debScanner.getEntry('./opt/bleah/apple')
        assertTrue(file.isFile())

        def rpmScanner = Scanner.scan(rpmTask.getArchivePath())
        assertEquals('3', Scanner.getHeaderEntryString(rpmScanner, RELEASE))
        assertTrue(Scanner.getHeaderEntryString(rpmScanner, REQUIRENAME).contains('awesomesauce'))
        assertEquals('i386', Scanner.getHeaderEntryString(rpmScanner, ARCH))
        assertEquals(['./opt/bleah', './opt/bleah/apple'], rpmScanner.files*.name)

    }
}
