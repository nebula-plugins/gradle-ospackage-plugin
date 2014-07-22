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

package com.netflix.gradle.plugins.rpm

import com.google.common.io.Files
import com.netflix.gradle.plugins.packaging.ProjectPackagingExtension
import com.netflix.gradle.plugins.packaging.SystemPackagingTask
import nebula.test.ProjectSpec
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.testfixtures.ProjectBuilder

import static org.freecompany.redline.header.Header.HeaderTag.*
import static org.freecompany.redline.payload.CpioHeader.*
import static org.freecompany.redline.header.Flags.*

class RpmPluginTest extends ProjectSpec {
    def 'files'() {
        Project project = ProjectBuilder.builder().build()

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        File noParentsDir = new File(projectDir, 'noParentsDir')
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
        project.tasks.buildRpm.execute()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        'bleah' == Scanner.getHeaderEntryString(scan, NAME)
        '1.0' == Scanner.getHeaderEntryString(scan, VERSION)
        '1' == Scanner.getHeaderEntryString(scan, RELEASE)
        'i386' == Scanner.getHeaderEntryString(scan, ARCH)
        'linux' == Scanner.getHeaderEntryString(scan, OS)
        ['./a/path/not/to/create/alone', './opt/bleah',
                './opt/bleah/apple', './opt/bleah/banana'] == scan.files*.name
        [FILE, DIR, FILE, SYMLINK] == scan.files*.type
    }

    def 'obsoletesAndConflicts'() {

        Project project = ProjectBuilder.builder().build()
        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/ObsoletesConflictsTest')
            destinationDir.mkdirs()

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
        project.tasks.buildRpm.execute()

        then:
        def scan = Scanner.scan(project.file('build/tmp/ObsoletesConflictsTest/testing-1.2-3.i386.rpm'))
        def obsoletes = Scanner.getHeaderEntry(scan, OBSOLETENAME)
        def obsoleteVersions = Scanner.getHeaderEntry(scan, OBSOLETEVERSION)
        def obsoleteComparisons = Scanner.getHeaderEntry(scan, OBSOLETEFLAGS)
        def conflicts = Scanner.getHeaderEntry(scan, CONFLICTNAME)
        def conflictVersions = Scanner.getHeaderEntry(scan, CONFLICTVERSION)
        def conflictComparisons = Scanner.getHeaderEntry(scan, CONFLICTFLAGS)

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
    }


    def 'projectNameDefault'() {
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        when:
        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {})

        then:
        'projectNameDefault' == project.buildRpm.packageName

        when:
        project.tasks.buildRpm.execute()

        then:
        noExceptionThrown()
    }

    def 'category_on_spec'() {
        project.version = '1.0.0'

        File bananaFile = new File(projectDir, 'test/banana')
        Files.createParentDirs(bananaFile)
        bananaFile.text = 'banana'

        File appleFile = new File(projectDir, 'src/apple')
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
                createDirectoryEntry true
            }
        })

        when:
        rpmTask.execute()

        then:
        // Evaluate response
        def scanFiles = Scanner.scan(rpmTask.getArchivePath()).files

        ['./usr/local/myproduct', './usr/local/myproduct/bin', './usr/local/myproduct/bin/apple', './usr/share/myproduct', './usr/share/myproduct/etc', './usr/share/myproduct/etc/banana'] == scanFiles*.name
        [ DIR, DIR, FILE, DIR, DIR, FILE] == scanFiles*.type

    }

    def 'filter_expression'() {

        project.version = '1.0.0'
        File appleFile = new File(projectDir, 'src/apple')
        Files.createParentDirs(appleFile)
        appleFile.text = '{{BASE}}/apple'

        project.apply plugin: 'rpm'

        def rpmTask = (Rpm) project.task([type: Rpm], 'buildRpm') {
            from(appleFile.getParentFile()) {
                into '/usr/local/myproduct/bin'
                filter({ line ->
                    return line.replaceAll(/\{\{BASE\}\}/, '/usr/local/myproduct')
                })
            }
        }

        when:
        rpmTask.execute()

        then:
        def scan = Scanner.scan(rpmTask.getArchivePath())
        def scannerApple = scan.files.find { it.name =='./usr/local/myproduct/bin/apple'}
        scannerApple.asString() == '/usr/local/myproduct/apple'
    }

    def 'buildHost_shouldHaveASensibleDefault'() {
        setup:
        InetAddress mockInetAddress = Mock()
        mockInetAddress.hostName >> { throw new UnknownHostException() }

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        when:
        project.apply plugin: 'rpm'

        Rpm rpmTask = (Rpm) project.task([type: Rpm], 'buildRpm', {})
        SystemPackagingTask.machineAddress = mockInetAddress

        then:
        'unknown' == rpmTask.getBuildHost()

        when:
        rpmTask.execute()

        then:
        noExceptionThrown()

    }

    def 'usesArchivesBaseName'() {

        // archivesBaseName is an artifact of the BasePlugin, and won't exist until it's applied.
        project.apply plugin: BasePlugin
        project.archivesBaseName = 'foo'

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'rpm'

        when:
        project.task([type: Rpm], 'buildRpm', {})

        then:
        'foo' == project.buildRpm.packageName

        when:
        project.tasks.buildRpm.execute()

        then:
        noExceptionThrown()
    }

    def 'verifyValuesCanComeFromExtension'() {

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'rpm'
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

    def 'verifyCopySpecCanComeFromExtension'() {
        setup:
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'apple').text = 'apple'

        File etcDir = new File(projectDir, 'etc')
        etcDir.mkdirs()
        new File(etcDir, 'banana.conf').text = 'banana=true'

        // Simulate SystemPackagingBasePlugin
        project.apply plugin: 'rpm'
        ProjectPackagingExtension parentExten = project.extensions.create('rpmParent', ProjectPackagingExtension, project)

        // Configure
        Rpm rpmTask = (Rpm) project.task([type: Rpm, name:'buildRpm']) {
            release 3
        }
        project.version = '1.0'

        rpmTask.from(srcDir) {
            into('/usr/local/src')
        }
        parentExten.from(etcDir) {
            createDirectoryEntry true
            into('/conf/defaults')
        }

        // Execute
        when:
        rpmTask.execute()

        then:
        // Evaluate response
        rpmTask.getArchivePath().exists()
        println("Path to RPM: " + rpmTask.getArchivePath().getAbsoluteFile().toString())
        def scan = Scanner.scan(rpmTask.getArchivePath())
        // Parent will come first
        ['./conf', './conf/defaults', './conf/defaults/banana.conf', './usr/local/src', './usr/local/src/apple'] == scan.files*.name
        [DIR, DIR, FILE, DIR, FILE] == scan.files*.type
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

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

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
        project.tasks.buildRpm.execute()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/userTest-2.0-2.i386.rpm'))

        [DIR, FILE, FILE, FILE] == scan.files*.type
        ['./tiny', './tiny/apple', './tiny/banana', './tiny/cherry'] == scan.files*.name

        ['user1', 'user1', 'default', 'user2'] == scan.format.header.getEntry(FILEUSERNAME).values.toList()
    }

    def 'differentGroupsBetweenCopySpecs'() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir1 = new File(buildDir, 'src1')
        File srcDir2 = new File(buildDir, 'src2')
        File srcDir3 = new File(buildDir, 'src3')

        srcDir1.mkdirs()
        srcDir2.mkdirs()
        srcDir3.mkdirs()

        FileUtils.writeStringToFile(new File(srcDir1, 'apple'),  'apple')
        FileUtils.writeStringToFile(new File(srcDir2, 'banana'), 'banana')
        FileUtils.writeStringToFile(new File(srcDir3, 'cherry'), 'cherry')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

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
        project.tasks.buildRpm.execute()

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

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'userTest'
            version     = '2.0'
            release     = '2'
            type        = BINARY
            arch        = I386
            os          = LINUX

            into '/tiny'
            fileMode 0555

            from(srcDir1) {
                // should be default group
            }

            from(srcDir2) {
                fileMode 0666
            }

            from(srcDir3) {
                fileMode 0555
            }
        })

        when:
        project.tasks.buildRpm.execute()

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

        project.apply plugin: 'rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'one-prefix'
            version = '1.0'
            release = '1'
            arch = I386
            os = LINUX

            into '/opt/myprefix'
            from (srcDir)
        })

        when:
        rpmTask.execute()

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

        project.apply plugin: 'rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

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
        rpmTask.execute()

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

        project.apply plugin: 'rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

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
        rpmTask.execute()

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

        project.apply plugin: 'rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

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
        rpmTask.execute()

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

        project.apply plugin: 'rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

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
        rpmTask.execute()

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

        project.apply plugin: 'os-package-base'
        project.ospackage { prefixes = ['/opt/ospackage', '/etc/maybe'] }

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

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
        rpmTask.execute()

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

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

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
        project.tasks.buildRpm.execute()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        scan.files*.name == ['./own/content/myfile.txt']
        scan.files*.type == [FILE]
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

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

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
        project.tasks.buildRpm.execute()

        then:
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        scan.files*.name == ['./inside/the/archive/empty', './own/content/myfile.txt', './using/the/dsl']
        scan.files*.type == [DIR, FILE, DIR]
    }
}
