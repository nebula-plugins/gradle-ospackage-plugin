/*
 * Copyright 2011-2019 the original author or authors.
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

package com.netflix.gradle.plugins.rpm


import com.netflix.gradle.plugins.packaging.ProjectPackagingExtension
import com.netflix.gradle.plugins.utils.JavaNIOUtils
import nebula.test.ProjectSpec
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.JavaVersion
import org.apache.commons.lang3.SystemUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.redline_rpm.header.Header
import org.redline_rpm.header.Signature
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Issue
import spock.lang.Unroll

import static org.redline_rpm.header.Flags.*
import static org.redline_rpm.header.Header.HeaderTag.*
import static org.redline_rpm.payload.CpioHeader.*

class RpmPluginTest extends ProjectSpec {
    def 'files'() {
        Project project = ProjectBuilder.builder().build()

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        File noParentsDir = new File(projectDir, 'noParentsDir')
        noParentsDir.mkdirs()
        FileUtils.writeStringToFile(new File(noParentsDir, 'alone'), 'alone')

        project.apply plugin: 'com.netflix.nebula.rpm'

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))


            packageName = 'bleah'
            version = '1.0'
            release = '1'
            type = BINARY
            arch = I386.name()
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
                createDirectoryEntry true
                fileType CONFIG | NOREPLACE
            }

            from(noParentsDir) {
                addParentDirs false
                into '/a/path/not/to/create'
            }

            link('/opt/bleah/banana', '/opt/bleah/apple')
        })

        when:
        project.tasks.buildRpm.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        'bleah' == Scanner.getHeaderEntryString(scan, NAME)
        '1.0' == Scanner.getHeaderEntryString(scan, VERSION)
        '1' == Scanner.getHeaderEntryString(scan, RELEASE)
        0 == Scanner.getHeaderEntry(scan, EPOCH).values[0]
        'i386' == Scanner.getHeaderEntryString(scan, ARCH)
        'linux' == Scanner.getHeaderEntryString(scan, OS)
        ['SuperSystem'] == Scanner.getHeaderEntry(scan, DISTRIBUTION).values
        scan.files*.name.every { fileName ->
            ['./a/path/not/to/create/alone', './opt/bleah',
             './opt/bleah/apple', './opt/bleah/banana'].any { path ->
                path.startsWith(fileName)
            }
        }
        scan.files*.name.every { fileName ->
            ['./a/path/not/to/create'].every { path ->
                ! path.startsWith(fileName)
            }
        }
    }

    def 'obsoletesAndConflicts'() {

        Project project = ProjectBuilder.builder().build()
        File buildDir = project.layout.buildDirectory.getAsFile().get()
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'com.netflix.nebula.rpm'

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/ObsoletesConflictsTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/ObsoletesConflictsTest'))

            packageName = 'testing'
            version = '1.2'
            release = '3'
            type = BINARY
            arch = I386
            os = LINUX
            license = 'Free'
            distribution = 'SuperSystem'
            vendor = 'Super Associates, LLC'
            url = 'http://www.example.com/'

            obsoletes('blarg', '1.0', GREATER | EQUAL)
            conflicts('blech')
            conflicts('packageA', '1.0', LESS)
            obsoletes('packageB', '2.2', GREATER)

            from(srcDir)
            into '/opt/bleah'
        })

        when:
        project.tasks.buildRpm.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/ObsoletesConflictsTest/testing-1.2-3.i386.rpm'))
        def obsoletes = Scanner.getHeaderEntry(scan, OBSOLETENAME)
        def obsoleteVersions = Scanner.getHeaderEntry(scan, OBSOLETEVERSION)
        def obsoleteComparisons = Scanner.getHeaderEntry(scan, OBSOLETEFLAGS)
        def conflicts = Scanner.getHeaderEntry(scan, CONFLICTNAME)
        def conflictVersions = Scanner.getHeaderEntry(scan, CONFLICTVERSION)
        def conflictComparisons = Scanner.getHeaderEntry(scan, CONFLICTFLAGS)
        def distribution = Scanner.getHeaderEntry(scan, DISTRIBUTION)

        'blarg' == obsoletes.values[0]
        '1.0' == obsoleteVersions.values[0]
        (GREATER | EQUAL) == obsoleteComparisons.values[0]

        'blech' == conflicts.values[0]
        '' == conflictVersions.values[0]
        0 == conflictComparisons.values[0]

        'packageA' == conflicts.values[1]
        '1.0' ==conflictVersions.values[1]
        LESS == conflictComparisons.values[1]

        'packageB' == obsoletes.values[1]
        '2.2' == obsoleteVersions.values[1]
        GREATER == obsoleteComparisons.values[1]

        ['SuperSystem'] == distribution.values
    }

    def 'verifyValuesCanComeFromExtension'() {

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'com.netflix.nebula.rpm'
        def parentExten = project.extensions.create('rpmParent', ProjectPackagingExtension, project)

        Rpm rpmTask = project.task([type: Rpm], 'buildRpm')
        rpmTask.permissionGroup = 'GROUP'
        rpmTask.requires('openjdk')
        rpmTask.link('/dev/null', '/dev/random')

        when:
        parentExten.user = 'USER'
        parentExten.permissionGroup = 'GROUP2'
        parentExten.requires('java')
        parentExten.link('/tmp', '/var/tmp')

        project.description = 'DESCRIPTION'

        then:
        'USER' == rpmTask.user // From Extension
        'GROUP' == rpmTask.permissionGroup // From task, overriding extension
        'DESCRIPTION' == rpmTask.packageDescription // From Project, even though extension could have a value
        2 == rpmTask.getAllLinks().size()
        2 == rpmTask.getAllDependencies().size()
    }

    def 'differentUsersBetweenCopySpecs'() {

        File srcDir1 = new File(projectDir, 'src1')
        File srcDir2 = new File(projectDir, 'src2')
        File srcDir3 = new File(projectDir, 'src3')

        srcDir1.mkdirs()
        srcDir2.mkdirs()
        srcDir3.mkdirs()

        FileUtils.writeStringToFile(new File(srcDir1, 'apple'),  'apple')
        FileUtils.writeStringToFile(new File(srcDir2, 'banana'), 'banana')
        FileUtils.writeStringToFile(new File(srcDir3, 'cherry'), 'cherry')

        project.apply plugin: 'com.netflix.nebula.rpm'

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'userTest'
            version     = '2.0'
            release     = '2'
            type        = BINARY
            arch        = I386
            os          = LINUX

            into '/tiny'
            user = 'default'

            from(srcDir1) {
                user 'user1'
                // user = 'user1' // Won't work, since setter via Categories won't pass hasProperty
            }

            from(srcDir2) {
                // should be default user
            }

            from(srcDir3) {
                user 'user2'
            }
        })

        when:
        project.tasks.buildRpm.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/userTest-2.0-2.i386.rpm'))

        [DIR, FILE, FILE, FILE] == scan.files*.type
        ['./tiny', './tiny/apple', './tiny/banana', './tiny/cherry'] == scan.files*.name

        ['user1', 'user1', 'default', 'user2'] == scan.format.header.getEntry(FILEUSERNAME).values.toList()
    }

    def 'differentGroupsBetweenCopySpecs'() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.layout.buildDirectory.getAsFile().get()
        File srcDir1 = new File(buildDir, 'src1')
        File srcDir2 = new File(buildDir, 'src2')
        File srcDir3 = new File(buildDir, 'src3')

        srcDir1.mkdirs()
        srcDir2.mkdirs()
        srcDir3.mkdirs()

        FileUtils.writeStringToFile(new File(srcDir1, 'apple'),  'apple')
        FileUtils.writeStringToFile(new File(srcDir2, 'banana'), 'banana')
        FileUtils.writeStringToFile(new File(srcDir3, 'cherry'), 'cherry')

        project.apply plugin: 'com.netflix.nebula.rpm'

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))
            packageName = 'userTest'
            version     = '2.0'
            release     = '2'
            type        = BINARY
            arch        = I386
            os          = LINUX

            into '/tiny'
            permissionGroup 'default'

            from(srcDir1) {
                // should be default group
            }

            from(srcDir2) {
                //setPermissionGroup 'group2' // works
                //permissionGroup = 'group2' // Does not work
                permissionGroup 'group2' // Does not work
            }

            from(srcDir3) {
                // should be default group
            }
        })

        when:
        project.tasks.buildRpm.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/userTest-2.0-2.i386.rpm'))

        [DIR, FILE, FILE, FILE] == scan.files*.type
        ['./tiny', './tiny/apple', './tiny/banana', './tiny/cherry'] == scan.files*.name

        def allFiles = scan.files
        def groups = scan.format.header.getEntry(FILEGROUPNAME).values

        ['default', 'default', 'group2', 'default'] == scan.format.header.getEntry(FILEGROUPNAME).values.toList()
    }

    def 'differentPermissionsBetweenCopySpecs'() {
        File srcDir1 = new File(projectDir, 'src1')
        File srcDir2 = new File(projectDir, 'src2')
        File srcDir3 = new File(projectDir, 'src3')

        srcDir1.mkdirs()
        srcDir2.mkdirs()
        srcDir3.mkdirs()

        FileUtils.writeStringToFile(new File(srcDir1, 'apple'),  'apple')
        FileUtils.writeStringToFile(new File(srcDir2, 'banana'), 'banana')
        FileUtils.writeStringToFile(new File(srcDir3, 'cherry'), 'cherry')

        project.apply plugin: 'com.netflix.nebula.rpm'

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'userTest'
            version     = '2.0'
            release     = '2'
            type        = BINARY
            arch        = I386
            os          = LINUX

            into '/tiny'

            filePermissions {
                unix(0555)
            }

            from(srcDir1) {
                // should be default group
            }

            from(srcDir2) {
                filePermissions {
                    unix(0666)
                }
            }

            from(srcDir3) {
                filePermissions {
                    unix(0555)
                }
            }
        })

        when:
        project.tasks.buildRpm.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/userTest-2.0-2.i386.rpm'))

        [DIR, FILE, FILE, FILE] == scan.files*.type
        ['./tiny', './tiny/apple', './tiny/banana', './tiny/cherry'] == scan.files*.name

        // #define S_IFIFO  0010000  /* named pipe (fifo) */
        // #define S_IFCHR  0020000  /* character special */
        // #define S_IFDIR  0040000  /* directory */
        // #define S_IFBLK  0060000  /* block special */
        // #define S_IFREG  0100000  /* regular */
        // #define S_IFLNK  0120000  /* symbolic link */
        // #define S_IFSOCK 0140000  /* socket */
        // #define S_ISUID  0004000 /* set user id on execution */
        // #define S_ISGID  0002000 /* set group id on execution */
        // #define S_ISTXT  0001000 /* sticky bit */
        // #define S_IRWXU  0000700 /* RWX mask for owner */
        // #define S_IRUSR  0000400 /* R for owner */
        // #define S_IWUSR  0000200 /* W for owner */
        // #define S_IXUSR  0000100 /* X for owner */
        // #define S_IRWXG  0000070 /* RWX mask for group */
        // #define S_IRGRP  0000040 /* R for group */
        // #define S_IWGRP  0000020 /* W for group */
        // #define S_IXGRP  0000010 /* X for group */
        // #define S_IRWXO  0000007 /* RWX mask for other */
        // #define S_IROTH  0000004 /* R for other */
        // #define S_IWOTH  0000002 /* W for other */
        // #define S_IXOTH  0000001 /* X for other */
        // #define S_ISVTX  0001000 /* save swapped text even after use */

        // drwxr-xr-x is 0040755
        // NOTE: Not sure why directory is getting user write permission
        [(short)0040755, (short)0100555, (short)0100666, (short)0100555] == scan.format.header.getEntry(FILEMODES).values.toList()
    }

    def 'no Prefix Value'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'com.netflix.nebula.rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'one-prefix'
            version = '1.0'
            release = '1'
            arch = I386
            os = LINUX

            into '/opt/myprefix'
            from (srcDir)
        })

        when:
        rpmTask.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/one-prefix-1.0-1.i386.rpm'))
        null == Scanner.getHeaderEntry(scan, PREFIXES)
    }

    def 'one Prefix Value'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'com.netflix.nebula.rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'one-prefix'
            version = '1.0'
            release = '1'
            arch = I386
            os = LINUX

            into '/opt/myprefix'
            from (srcDir)

            prefixes '/opt/myprefix'
        })

        when:
        rpmTask.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/one-prefix-1.0-1.i386.rpm'))
        '/opt/myprefix' == Scanner.getHeaderEntryString(scan, PREFIXES)
    }

    def 'multiple Prefix Values'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'com.netflix.nebula.rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'one-prefix'
            version = '1.0'
            release = '1'
            arch = I386
            os = LINUX

            into '/opt/myprefix'
            from (srcDir)

            prefixes '/opt/myprefix', '/etc/init.d'
        })

        when:
        rpmTask.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/one-prefix-1.0-1.i386.rpm'))
        // NOTE: Scanner just jams things together as one string
        '/opt/myprefix/etc/init.d' == Scanner.getHeaderEntryString(scan, PREFIXES)
    }

    def 'multiple Added then cleared Prefix Values'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'com.netflix.nebula.rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'one-prefix'
            version = '1.0'
            release = '1'
            arch = I386
            os = LINUX

            into '/opt/myprefix'
            from (srcDir)

            prefixes '/opt/myprefix', '/etc/init.d'
            prefixes.clear()
        })

        when:
        rpmTask.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/one-prefix-1.0-1.i386.rpm'))
        null == Scanner.getHeaderEntry(scan, PREFIXES)
    }

    def 'direct assignment of Prefix Values'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'com.netflix.nebula.rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'multi-prefix'
            version = '1.0'
            release = '1'
            arch = I386
            os = LINUX

            into '/opt/myprefix'
            from (srcDir)

            prefixes = ['/opt/myprefix', '/etc/init.d']
        })

        when:
        rpmTask.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/multi-prefix-1.0-1.i386.rpm'))
        // NOTE: Scanner just jams things together as one string
        '/opt/myprefix/etc/init.d' == Scanner.getHeaderEntryString(scan, PREFIXES)
    }

    def 'ospackage assignment of Prefix Values'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'com.netflix.nebula.ospackage-base'
        project.ospackage { prefixes = ['/opt/ospackage', '/etc/maybe'] }

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'multi-prefix'
            version = '1.0'
            release = '1'
            arch = I386
            os = LINUX
            prefix '/apps'

            into '/opt/myprefix'
            from (srcDir)
        })

        when:
        rpmTask.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/multi-prefix-1.0-1.i386.rpm'))
        // NOTE: Scanner just jams things together as one string
        def foundPrefixes = Scanner.getHeaderEntry(scan, PREFIXES)
        foundPrefixes.values.contains('/apps')
        foundPrefixes.values.contains('/opt/ospackage')
        foundPrefixes.values.contains('/etc/maybe')
    }

    def 'Avoids including empty directories'() {
        Project project = ProjectBuilder.builder().build()

        File myDir = new File(projectDir, 'my')
        File contentDir = new File(myDir, 'own/content')
        contentDir.mkdirs()
        FileUtils.writeStringToFile(new File(contentDir, 'myfile.txt'), 'test')

        File emptyDir = new File(myDir, 'own/empty')
        emptyDir.mkdirs()

        project.apply plugin: 'com.netflix.nebula.rpm'

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            arch = I386

            from(myDir) {
                addParentDirs false
            }
            includeEmptyDirs false
        })

        when:
        project.tasks.buildRpm.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        scan.files*.name.every { './own/content/myfile.txt'.startsWith(it) }
    }

    def 'Can create empty directories'() {
        Project project = ProjectBuilder.builder().build()

        File myDir = new File(projectDir, 'my')
        File contentDir = new File(myDir, 'own/content')
        contentDir.mkdirs()
        FileUtils.writeStringToFile(new File(contentDir, 'myfile.txt'), 'test')

        File otherDir = new File(projectDir, 'other')
        File someDir = new File(otherDir, 'some')
        File emptyDir = new File(someDir, 'empty')
        emptyDir.mkdirs()

        project.apply plugin: 'com.netflix.nebula.rpm'

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            arch = I386

            from(myDir) {
                addParentDirs false
            }

            from(someDir) {
                into '/inside/the/archive'
                addParentDirs false
                createDirectoryEntry true
            }

            directory('/using/the/dsl')
        })

        when:
        project.tasks.buildRpm.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        scan.files*.name.containsAll(['./inside/the/archive/empty', './own/content/myfile.txt', './using/the/dsl'])
        scan.files*.type.containsAll([DIR, FILE])
    }

    def 'Sets owner and group for directory DSL'() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'com.netflix.nebula.rpm'

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            arch = I386

            user 'test'
            permissionGroup 'test'

            directory('/using/the/dsl')
        })

        when:
        project.tasks.buildRpm.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        scan.files*.name == ['./using/the/dsl']
        scan.files*.type == [DIR]
        scan.format.header.getEntry(FILEGROUPNAME).values.toList() == ['test']
    }

    def 'has epoch value'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'com.netflix.nebula.rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'has-epoch'
            version = '1.0'
            release = '1'
            epoch = 2
            arch = I386
            os = LINUX

            into '/opt/bleah'
            from (srcDir)
        })

        when:
        rpmTask.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/has-epoch-1.0-1.i386.rpm'))
        2 == Scanner.getHeaderEntry(scan, EPOCH).values[0]
    }

    def 'Does not include signature header if signing is not fully configured'() {
        given:
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'com.netflix.nebula.rpm'

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            arch = I386
        })

        when:
        project.tasks.buildRpm.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        scan.format.signature.getEntry(Signature.SignatureTag.LEGACY_PGP) == null
    }

    def 'Does include signature header'() {
        given:
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'com.netflix.nebula.rpm'

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            arch = I386

            signingKeyId = '92D555F5'
            signingKeyPassphrase = 'os-package'
            signingKeyRingFile = new File(getClass().getClassLoader().getResource('pgp-test-key/secring.gpg').toURI())
        })

        when:
        project.tasks.buildRpm.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        scan.format.signature.getEntry(Signature.SignatureTag.LEGACY_PGP) != null
    }

    /**
     * Verifies that a symlink can be preserved.
     *
     * The following directory structure is assumed:
     *
     * .
     * └── usr
     *     └── bin
     *         ├── foo -> foo-1.2
     *         └── foo-1.2
     *             └── foo.txt
     */
    @IgnoreIf({ !SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_7) })
    def 'Preserves symlinks'() {
        setup:
        File symlinkDir = new File(projectDir, 'symlink')
        File binDir = new File(symlinkDir, 'usr/bin')
        File fooDir = new File(binDir, 'foo-1.2')
        binDir.mkdirs()
        FileUtils.writeStringToFile(new File(fooDir, 'foo.txt'), 'foo')
        JavaNIOUtils.createSymbolicLink(new File(binDir, 'foo'), fooDir)

        when:
        project.apply plugin: 'com.netflix.nebula.rpm'

        Task task = project.task('buildRpm', type: Rpm) {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            type = BINARY
            arch = I386

            from(symlinkDir) {
                createDirectoryEntry true
            }
        }

        task.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        scan.files*.name == ['./usr', './usr/bin', './usr/bin/foo', './usr/bin/foo-1.2', './usr/bin/foo-1.2/foo.txt']
        scan.files*.type == [DIR, DIR, SYMLINK, DIR, FILE]
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/104")
    @Unroll
    def "Translates package description '#description' to header entry"() {
        given:
        project.apply plugin: 'com.netflix.nebula.rpm'

        Rpm rpmTask = project.task('buildRpm', type: Rpm) {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            version = '1.0'
            packageName = 'bleah'
            packageDescription = description
        }

        when:
        rpmTask.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0.noarch.rpm'))
        Scanner.getHeaderEntryString(scan, DESCRIPTION) == headerEntry

        where:
        description             | headerEntry
        'This is a description' | 'This is a description'
        ''                      | ''
        null                    | ''
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/104")
    @Unroll
    def "Translates project description '#description' to header entry"() {
        given:
        project.apply plugin: 'com.netflix.nebula.rpm'
        project.description = description

        Rpm rpmTask = project.task('buildRpm', type: Rpm) {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            version = '1.0'
            packageName = 'bleah'
        }

        when:
        rpmTask.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0.noarch.rpm'))
        Scanner.getHeaderEntryString(scan, DESCRIPTION) == headerEntry

        where:
        description             | headerEntry
        'This is a description' | 'This is a description'
        ''                      | ''
        null                    | ''
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/102")
    def "Can set user and group for packaged files"() {
        given:
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'com.netflix.nebula.rpm'

        Rpm rpmTask = project.task('buildRpm', type: Rpm) {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            version = '1.0'
            packageName = 'bleah'

            from(srcDir) {
                user = 'me'
                permissionGroup = 'awesome'
            }
        }

        when:
        rpmTask.copy()

        then:
        Header header = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0.noarch.rpm')).format.header
        ['awesome'] == header.getEntry(FILEGROUPNAME).values.toList()
        ['me'] == header.getEntry(FILEUSERNAME).values.toList()
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/102")
    def "Can set multiple users and groups for packaged files"() {
        given:
        File srcDir = new File(projectDir, 'src')
        File scriptDir = new File(projectDir, 'script')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple', "UTF-8")
        FileUtils.writeStringToFile(new File(scriptDir, 'orange'), 'orange', "UTF-8")
        FileUtils.writeStringToFile(new File(scriptDir, 'banana'), 'banana', "UTF-8")

        project.apply plugin: 'com.netflix.nebula.rpm'

        Rpm rpmTask = project.task('buildRpm', type: Rpm) {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            version = '1.0'
            packageName = 'bleah'

            user 'defaultUser'
            permissionGroup 'defaultGroup'

            from(srcDir) {
                user 'me'
                permissionGroup 'awesome'
            }

            from(scriptDir) {
                into '/etc'
            }
        }

        when:
        rpmTask.copy()

        then:
        Header header = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0.noarch.rpm')).format.header
        ['awesome', 'defaultGroup', 'defaultGroup'] == header.getEntry(FILEGROUPNAME).values.toList()
        ['me', 'defaultUser', 'defaultUser'] == header.getEntry(FILEUSERNAME).values.toList()
    }

    @Unroll
    def 'handle semantic versions with dashes and metadata (+) expect #version to be #expected'() {
        given:
        project.apply plugin: 'com.netflix.nebula.rpm'
        project.version = version

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))
            packageName = 'semvertest'
        })

        project.tasks.buildRpm.copy()

        expect:
        project.file("build/tmp/RpmPluginTest/semvertest-${expected}.noarch.rpm").exists()

        where:
        version              | expected
        '1.0'                | '1.0'
        '1.0.0'              | '1.0.0'
        '1.0.0-rc.1'         | '1.0.0~rc.1'
        '1.0.0-dev.3+abc219' | '1.0.0~dev.3'
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/148")
    def 'handles multiple provides'() {
        given:
        project.apply plugin: 'com.netflix.nebula.rpm'
        project.version = '1.0'

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))
            packageName = 'providesTest'
            provides 'foo'
            provides 'bar'
        })

        when:
        project.tasks.buildRpm.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/providesTest-1.0.noarch.rpm'))
        def provides = Scanner.getHeaderEntry(scan, PROVIDENAME)
        ['foo', 'bar'].every { it in provides.values }
    }

    def 'Add preTrans and postTrans scripts'() {
        given:
        File prescript = new File(projectDir, 'prescript')
        File postscript = new File(projectDir, 'postscript')
        FileUtils.writeStringToFile(prescript, 'MyPreTransScript')
        FileUtils.writeStringToFile(postscript, 'MyPostTransScript')

        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'com.netflix.nebula.rpm'

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            arch = I386

            preTrans prescript
            postTrans postscript
        })

        when:
        project.tasks.buildRpm.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        def PRE_TRANS_HEADER_INDEX = 1151
        def POST_TRANS_HEADER_INDEX = 1152
        scan.format.header.entries[PRE_TRANS_HEADER_INDEX].values[0].contains('MyPreTransScript')
        scan.format.header.entries[POST_TRANS_HEADER_INDEX].values[0].contains('MyPostTransScript')
    }

    def 'equal task directories are equal'() {
        expect:
        def taskA = project.task('buildRpmA', type: Rpm) {
            directory('test', 755)
        }
        def taskB = project.task('buildRpmB', type: Rpm) {
            directory('test', 755)
        }
        taskA.getDirectories() == taskB.getDirectories()
    }

    def 'Add triggerIn, triggerUn, and triggerPostUn scripts'() {
        given:
        File triggerInScript = new File(projectDir, 'triggerinscript')
        File triggerUnScript = new File(projectDir, 'triggerunscript')
        File triggerPostUnScript = new File(projectDir, 'triggerpostunscript')
        FileUtils.writeStringToFile(triggerInScript, 'MyTriggerInScript')
        FileUtils.writeStringToFile(triggerUnScript, 'MyTriggerUnScript')
        FileUtils.writeStringToFile(triggerPostUnScript, 'MyTriggerPostUnScript')

        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'com.netflix.nebula.rpm'

        project.task([type: Rpm], 'buildRpm', {
            File destination = project.file('build/tmp/RpmPluginTest')
            destination.mkdirs()
            destinationDirectory.set(project.file('build/tmp/RpmPluginTest'))

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            arch = I386

            triggerInstall triggerInScript, 'the-package'
            triggerUninstall triggerUnScript, 'the-package'
            triggerPostUninstall triggerPostUnScript, 'the-pacakge'
        })

        when:
        project.tasks.buildRpm.copy()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        def TRIGGER_SCRIPTS_HEADER_INDEX = 1065
        def triggerScriptHeaders = scan.format.header.entries[TRIGGER_SCRIPTS_HEADER_INDEX]
        triggerScriptHeaders.values[0].contains('MyTriggerInScript')
        triggerScriptHeaders.values[1].contains('MyTriggerUnScript')
        triggerScriptHeaders.values[2].contains('MyTriggerPostUnScript')
    }
}
