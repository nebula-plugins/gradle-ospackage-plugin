/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trigonic.gradle.plugins.rpm

import com.google.common.io.Files
import com.trigonic.gradle.plugins.packaging.ProjectPackagingExtension
import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Ignore
import org.junit.Test

import static org.freecompany.redline.header.Header.HeaderTag.*
import static org.freecompany.redline.payload.CpioHeader.*
import static org.junit.Assert.*

class RpmPluginTest {
    @Test
    public void files() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        File noParentsDir = new File(buildDir, 'noParentsDir')
        noParentsDir.mkdirs()
        FileUtils.writeStringToFile(new File(noParentsDir, 'alone'), 'alone')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            type = BINARY
            arch = I386
            os = LINUX
            permissionGroup = 'Development/Libraries'
            summary = 'Bleah blarg'
            packageDescription = 'Not a very interesting library.'
            license = 'Free'
            distribution = 'SuperSystem'
            vendor = 'Super Associates, LLC'
            url = 'http://www.example.com/'

            requires('blarg', '1.0', GREATER | EQUAL)
            requires('blech')

            into '/opt/bleah'
            from(srcDir)

            from(srcDir.toString() + '/main/groovy') {
                createDirectoryEntry = true
                fileType = CONFIG | NOREPLACE
            }

            from(noParentsDir) {
                addParentDirs = false
                into '/a/path/not/to/create'
            }

            link('/opt/bleah/banana', '/opt/bleah/apple')
        })

        project.tasks.buildRpm.execute()
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        assertEquals('bleah', Scanner.getHeaderEntryString(scan, NAME))
        assertEquals('1.0', Scanner.getHeaderEntryString(scan, VERSION))
        assertEquals('1', Scanner.getHeaderEntryString(scan, RELEASE))
        assertEquals('i386', Scanner.getHeaderEntryString(scan, ARCH))
        assertEquals('linux', Scanner.getHeaderEntryString(scan, OS))
        assertEquals(['./a/path/not/to/create/alone', './opt/bleah',
                      './opt/bleah/apple', './opt/bleah/banana'], scan.files*.name)
        assertEquals([FILE, DIR, FILE, SYMLINK], scan.files*.type)
    }

    @Test
    public void projectNameDefault() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {})
        assertEquals 'test', project.buildRpm.packageName

        project.tasks.buildRpm.execute()
    }

    @Test
    void category_on_spec() {
        Project project = ProjectBuilder.builder().build()
        project.version = '1.0.0'

        File bananaFile = new File(project.buildDir, 'test/banana')
        Files.createParentDirs(bananaFile)
        bananaFile.text = 'banana'

        File appleFile = new File(project.buildDir, 'src/apple')
        Files.createParentDirs(appleFile)
        appleFile.text = 'apple'

        project.apply plugin: 'rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            addParentDirs = true
            from(bananaFile.getParentFile()) {
                into '/usr/share/myproduct/etc'
                createDirectoryEntry false
            }
            from(appleFile.getParentFile()) {
                into '/usr/local/myproduct/bin'
                createDirectoryEntry = true
            }
        })
        rpmTask.execute()

        // Evaluate response
        def scan = Scanner.scan(rpmTask.getArchivePath())

        assertEquals(['./usr/local', './usr/local/myproduct', './usr/local/myproduct/bin', './usr/local/myproduct/bin/apple', './usr/share/myproduct', './usr/share/myproduct/etc', './usr/share/myproduct/etc/banana'], scan.files*.name)
        assertEquals([DIR, DIR, DIR, FILE, DIR, DIR, FILE], scan.files*.type)

    }

    @Test
    void filter_expression() {
        Project project = ProjectBuilder.builder().build()
        project.version = '1.0.0'
        File appleFile = new File(project.buildDir, 'src/apple')
        Files.createParentDirs(appleFile)
        appleFile.text = 'apple'

        project.apply plugin: 'rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            from(appleFile.getParentFile()) {
                into '/usr/local/myproduct/bin'
                filter({ line ->
                    return line //line.replaceAll('{{BASE}}', '/usr/local/myproduct')
                })
            }
        })
        rpmTask.execute()
    }

    @Test
    void buildHost_shouldHaveASensibleDefault_whenHostNameResolutionFails() {
        GMockController mock = new GMockController()
        InetAddress mockInetAddress = (InetAddress) mock.mock(InetAddress)

        mockInetAddress.static.getLocalHost().raises(new UnknownHostException())

        mock.play {
            Project project = ProjectBuilder.builder().build()

            File buildDir = project.buildDir
            File srcDir = new File(buildDir, 'src')
            srcDir.mkdirs()
            FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

            project.apply plugin: 'rpm'

            project.task([type: Rpm], 'buildRpm', {})
            assertEquals 'unknown', project.buildRpm.buildHost

            project.tasks.buildRpm.execute()
        }

    }

    @Test
    public void usesArchivesBaseName() {
        Project project = ProjectBuilder.builder().build()
        // archivesBaseName is an artifact of the BasePlugin, and won't exist until it's applied.
        project.apply plugin: BasePlugin
        project.archivesBaseName = 'foo'

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {})
        assertEquals 'foo', project.buildRpm.packageName

        project.tasks.buildRpm.execute()
    }

    @Test
    public void verifyValuesCanComeFromExtension() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'rpm'
        def parentExten = project.extensions.create('rpmParent', ProjectPackagingExtension, project)

        Rpm rpmTask = project.task([type: Rpm], 'buildRpm')
        rpmTask.permissionGroup = 'GROUP'
        rpmTask.requires('openjdk')
        rpmTask.link('/dev/null', '/dev/random')

        parentExten.user = 'USER'
        parentExten.permissionGroup = 'GROUP2'
        parentExten.requires('java')
        parentExten.link('/tmp', '/var/tmp')

        project.description = 'DESCRIPTION'

        assertEquals 'USER', rpmTask.user // From Extension
        assertEquals 'GROUP', rpmTask.permissionGroup // From task, overriding extension
        assertEquals 'DESCRIPTION', rpmTask.packageDescription // From Project, even though extension could have a value
        assertEquals 2, rpmTask.getAllLinks().size()
        assertEquals 2, rpmTask.getAllDependencies().size()
    }

    @Test
    public void verifyCopySpecCanComeFromExtension() {
        Project project = ProjectBuilder.builder().build()

        // Setup files
        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'apple').text = 'apple'

        File etcDir = new File(buildDir, 'etc')
        etcDir.mkdirs()
        new File(etcDir, 'banana.conf').text = 'banana=true'

        // Simulate SystemPackagingBasePlugin
        project.apply plugin: 'rpm'
        ProjectPackagingExtension parentExten = project.extensions.create('rpmParent', ProjectPackagingExtension, project)

        // Configure
        Rpm rpmTask = project.task([type: Rpm], 'buildRpm', {
            release = 3
        })
        project.version = '1.0'

        rpmTask.from(srcDir) {
            into('/usr/local/src')
        }
        parentExten.from(etcDir) {
            createDirectoryEntry = true
            into('/conf/defaults')
        }

        // Execute
        rpmTask.execute()

        // Evaluate response
        assertTrue(rpmTask.getArchivePath().exists())
        println("Path to RPM: " + rpmTask.getArchivePath().getAbsoluteFile().toString())
        def scan = Scanner.scan(rpmTask.getArchivePath())
        // Parent will come first
        assertEquals(['./conf', './conf/defaults', './conf/defaults/banana.conf', './usr/local/src', './usr/local/src/apple',], scan.files*.name)
        assertEquals([DIR, DIR, FILE, DIR, FILE], scan.files*.type)
    }

}
