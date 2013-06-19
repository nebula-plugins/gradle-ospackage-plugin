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

package com.trigonic.gradle.plugins.deb

import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.*

class DebPluginTest {
    @Test
    public void minimalConfig() {
        Project project = ProjectBuilder.builder().build()
        project.version = 1.0

        File appleFile = new File(project.buildDir, 'src/apple')
        Files.createParentDirs(appleFile)
        appleFile.text = 'apple'

        project.apply plugin: 'deb'

        Deb debTask = project.task([type: Deb], 'buildDeb', {
            release = '1'
            from(appleFile.getParentFile())
        })

        debTask.execute()

        File debFile = debTask.getArchivePath()
        assertNotNull(debFile)
        assertTrue(debFile.exists())
    }

//    public void alwaysRun(DefaultTask task ) {
//        assertTrue(this instanceof GroovyObject)
//        assertTrue(task.inputs instanceof GroovyObject)
//        task.inputs.metaClass.getHasSourceFiles = { true }
//
//        task.getMetaClass().setMetaMethod('getHasSourceFiles').
//        def emc = new ExpandoMetaClass(task.getClass(), false)
//        emc.say = { message -> message == "bad" ? false : sayClosure(message) }
//        emc.initialize()
//
//        specialClient.metaClass = emc
//        ExpandoMetaClass emc = new ExpandoMetaClass( Object, false )
//        emc.getHasSourceFiles = { true }
//        emc.initialize()
//
//        def obj = new groovy.util.Proxy().wrap( task.inputs )
//        obj.setMetaClass( emc )
//        task.inputs = obj
//    }

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

        project.apply plugin: 'deb'

        project.task([type: Deb], 'buildDeb', {
            destinationDir = project.file('build/tmp/DebPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            permissionGroup = 'Development/Libraries'
            summary = 'Bleah blarg'
            packageDescription = 'Not a very interesting library.'
            license = 'Free'
            distribution = 'SuperSystem'
            vendor = 'Super Associates, LLC'
            url = 'http://www.example.com/'

            //requires('blarg', '1.0', GREATER | EQUAL)
            requires('blech')

            into '/opt/bleah'
            from(srcDir)

            from(srcDir.toString() + '/main/groovy') {
                createDirectoryEntry = true
                //fileType = CONFIG | NOREPLACE
            }

            from(noParentsDir) {
                addParentDirs = false
                into '/a/path/not/to/create'
            }

            link('/opt/bleah/banana', '/opt/bleah/apple')
        })

        project.tasks.buildDeb.execute()

        def scan = new Scanner(project.file('build/tmp/DebPluginTest/bleah_1.0-1_all.deb')) // , project.file('build/tmp/deboutput')
        assertEquals('bleah', scan.getHeaderEntry('Package'))
        assertEquals('blech', scan.getHeaderEntry('Depends'))
        assertEquals('bleah', scan.getHeaderEntry('Provides'))
        assertEquals('Bleah blarg\n Not a very interesting library.', scan.getHeaderEntry('Description'))
        assertEquals('http://www.example.com/', scan.getHeaderEntry('Homepage'))

        def file = scan.getEntry('./a/path/not/to/create/alone')
        assertTrue(file.isFile())

        def dir = scan.getEntry('./opt/bleah/')
        assertTrue(dir.isDirectory())

        def file2 = scan.getEntry('./opt/bleah/apple')
        assertTrue(file2.isFile())

        def symlink = scan.getEntry('./opt/bleah/banana')
        assertTrue(symlink.isSymbolicLink())
    }

    @Test
    public void projectNameDefault() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'deb'

        Deb debTask = project.task([type: Deb], 'buildDeb', {})
        debTask.from(srcDir)

        debTask.execute()

        File debFile = debTask.getArchivePath()
    }

    @Test
    public void generateScripts() {
        Project project = ProjectBuilder.builder().build()
        project.version = 1.0

        File scriptDir = new File(project.buildDir, 'src')
        Files.createParentDirs(scriptDir)
        scriptDir.mkdir()

        File preinstallScript = new File(scriptDir, 'preinstall')
        preinstallScript.text = "#!/bin/bash\necho Preinstall"

        File postinstallScript = new File(scriptDir, 'postinstall')
        postinstallScript.text = "#!/bin/bash\necho Postinstall"

        File appleFile = new File(project.buildDir, 'src/apple')
        Files.createParentDirs(appleFile)
        appleFile.text = 'apple'

        project.apply plugin: 'deb'

        Deb debTask = project.task([type: Deb], 'buildDeb', {
            release = '1'
            preinstall = preinstallScript
            postinstall = postinstallScript

            // SkipEmptySourceFilesTaskExecuter will prevent our task from running without a source
            from(appleFile.getParentFile())
        })

        debTask.execute()

        def scan = new Scanner(debTask.getArchivePath())
        scan.controlContents['preinst'] == "#!/bin/bash\necho preinstall"
        scan.controlContents['postinst'] == "#!/bin/bash\necho postinstall"
    }

    @Test
    public void generateScriptsThatAppendInstallUtil() {
        Project project = ProjectBuilder.builder().build()
        project.version = 1.0

        File scriptDir = new File(project.buildDir, 'src')
        Files.createParentDirs(scriptDir)
        scriptDir.mkdir()

        File installScript = new File(scriptDir, 'install')
        installScript.text = "#!/bin/bash\n"

        File preinstallScript = new File(scriptDir, 'preinstall')
        preinstallScript.text = "echo Preinstall"

        File postinstallScript = new File(scriptDir, 'postinstall')
        postinstallScript.text = "echo Postinstall"

        File appleFile = new File(project.buildDir, 'src/apple')
        Files.createParentDirs(appleFile)
        appleFile.text = 'apple'

        project.apply plugin: 'deb'

        Deb debTask = project.task([type: Deb], 'buildDeb', {
            release = '1'
            installUtils = installScript
            preinstall = preinstallScript
            postinstall = postinstallScript

            // SkipEmptySourceFilesTaskExecuter will prevent our task from running without a source
            from(appleFile.getParentFile())
        })

        debTask.execute()

        def scan = new Scanner(debTask.getArchivePath())
        scan.controlContents['preinst'] == "#!/bin/bash\necho preinstall"
        scan.controlContents['postinst'] == "#!/bin/bash\necho postinstall"
    }
}
