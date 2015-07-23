package com.netflix.gradle.plugins.rpm

import nebula.test.ProjectSpec
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Issue


class RpmFileOwnerTest extends ProjectSpec{

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/59")
    def 'make /opt/test considered built-in'() {

        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'rpm'

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()

        File testDir = new File(srcDir, 'main/test')
        testDir.mkdirs();
        FileUtils.writeStringToFile(new File(testDir, 'apple'), 'apple')


        project.task([type: Rpm], 'fooRpm', {
            destinationDir = project.file('build/tmp/FooRpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'foo'
            version = '1.0'
            release = '1'
            type = BINARY
            arch = I386.name()
            os = LINUX

            user 'nobody'
            permissionGroup 'nobody'

            from(srcDir.toString()) {
                into('/opt/test/foo')
            }

        })

        project.task([type: Rpm], 'barRpm', {
            destinationDir = project.file('build/tmp/BarRpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bar'
            version = '1.0'
            release = '1'
            type = BINARY
            arch = I386.name()
            os = LINUX

            user 'adm'
            permissionGroup 'adm'

            from(srcDir.toString()) {
                into('/opt/test/bar')
            }

        })

        when:
        project.tasks.fooRpm.execute()
        project.tasks.barRpm.execute()

        then:
        def fooScan = Scanner.scan(project.file('build/tmp/FooRpmPluginTest/foo-1.0-1.i386.rpm'))
        def barScan = Scanner.scan(project.file('build/tmp/BarRpmPluginTest/bar-1.0-1.i386.rpm'))
        assert !(fooScan.files*.name.contains('./opt/test') && barScan.files*.name.contains('./opt/test'))

    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/59")
    def 'workaround for setting /opt/test as built-in'() {

        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'rpm'

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()

        File testDir = new File(srcDir, 'main/test')
        testDir.mkdirs();
        FileUtils.writeStringToFile(new File(testDir, 'apple'), 'apple')

        project.task([type: Rpm], 'fooBarRpm', {
            destinationDir = project.file('build/tmp/FooBarRpmPluginTest')
            destinationDir.mkdirs()

            user 'nobody'
            permissionGroup 'nobody'

            packageName = 'foobar'
            version = '1.0'
            release = '1'
            type = BINARY
            arch = I386.name()
            os = LINUX

            directory('/opt/test/foobar')

            FileTree fileTree = project.fileTree(srcDir)
            fileTree.visit { FileVisitDetails fileVisitDetails ->
                if (fileVisitDetails.isDirectory()) {
                    directory('/opt/test/foobar/' + fileVisitDetails.relativePath)
                }
            }


            from(srcDir.toString()) {
                addParentDirs false
                into('/opt/test/foobar')
            }

        })


        when:
        project.tasks.fooBarRpm.execute()

        then:
        def fooBarScan = Scanner.scan(project.file('build/tmp/FooBarRpmPluginTest/foobar-1.0-1.i386.rpm'))
        assert !fooBarScan.files*.name.contains('./opt/test')

    }

}
