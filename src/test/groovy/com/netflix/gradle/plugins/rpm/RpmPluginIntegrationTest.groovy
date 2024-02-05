package com.netflix.gradle.plugins.rpm

import com.netflix.gradle.plugins.BaseIntegrationTestKitSpec
import com.netflix.gradle.plugins.utils.GradleUtils
import nebula.test.dependencies.DependencyGraph
import nebula.test.dependencies.GradleDependencyGenerator
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Issue
import spock.lang.Unroll

import static org.redline_rpm.header.Header.HeaderTag.DESCRIPTION
import static org.redline_rpm.header.Header.HeaderTag.NAME
import static org.redline_rpm.payload.CpioHeader.*

class RpmPluginIntegrationTest extends BaseIntegrationTestKitSpec {
    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/82")
    def "rpm task is marked up-to-date when setting arch or os property"() {

            given:
            File libDir = new File(projectDir, 'lib')
            libDir.mkdirs()
            new File(libDir, 'a.java').text = "public class A { }"
        buildFile << '''
plugins {
    id 'com.netflix.nebula.rpm'
}

task buildRpm(type: Rpm) {
    packageName = 'rpmIsUpToDate'
    arch = NOARCH
    os = LINUX
     from('lib') {
            into 'lib'
    }
}
'''
        when:
        runTasks('buildRpm')

        and:
        def result = runTasks('buildRpm')

        then:
        result.task(':buildRpm').outcome == TaskOutcome.UP_TO_DATE
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/104")
    @Unroll
    def "Translates extension packageDescription '#description' to header entry for RPM task"() {
        given:
        File libDir = new File(projectDir, 'lib')
        libDir.mkdirs()
        new File(libDir, 'a.java').text = "public class A { }"
        buildFile << """
plugins {
    id 'com.netflix.nebula.ospackage'
}

ospackage {
    packageName = 'bleah'
    packageDescription = ${GradleUtils.quotedIfPresent(description)}
    version = '1.0'
    from('lib') {
            into 'lib'
    }
}
"""

        when:
        runTasks('buildRpm')

        then:
        def scan = Scanner.scan(file('build/distributions/bleah-1.0.noarch.rpm'))
        def actual = Scanner.getHeaderEntryString(scan, DESCRIPTION)
        actual == headerEntry

        where:
        description             | headerEntry
        'This is a description' | 'This is a description'
        ''                      | ''
        null                    | ''
    }

    def 'projectNameDefault'() {
        given:
        File libDir = new File(projectDir, 'lib')
        libDir.mkdirs()
        new File(libDir, 'a.java').text = "public class A { }"
        buildFile << """
plugins {
    id 'com.netflix.nebula.rpm'
}

task buildRpm(type: Rpm) {
    version '1'
    from('lib') {
            into 'lib'
    }
}
"""

        when:
        runTasks('buildRpm', '--warning-mode', 'none')

        then:
        def scan = Scanner.scan(file('build/distributions/projectNameDefault-1.noarch.rpm'))
        def actual = Scanner.getHeaderEntryString(scan, NAME)
        'projectNameDefault' == actual
    }


    def 'file handle closed'() {
        given:
        buildFile << """
plugins {
    id 'com.netflix.nebula.rpm'
}
task buildRpm(type: Rpm) {
}
"""
        when:
        runTasks('buildRpm')

        then:
        // see https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/200#issuecomment-244666158
        // if file is not closed this will fail
        runTasks('clean')
    }

    def 'category_on_spec'() {
        given:
        File bananaFile = new File(projectDir, 'test/banana')
        FileUtils.forceMkdirParent(bananaFile)
        bananaFile.text = 'banana'

        File appleFile = new File(projectDir, 'src/apple')
        FileUtils.forceMkdirParent(appleFile)
        appleFile.text = 'apple'

        buildFile << """
plugins {
    id 'com.netflix.nebula.rpm'
}

version = '1.0.0'

task buildRpm(type: Rpm) {
    packageName = 'sample'
    addParentDirs = true
    from(${GradleUtils.quotedIfPresent(bananaFile.getParentFile().path)}) {
        into '/usr/share/myproduct/etc'
        createDirectoryEntry false
    }
    from(${GradleUtils.quotedIfPresent(appleFile.getParentFile().path)}) {
        into '/usr/local/myproduct/bin'
        createDirectoryEntry true
    }
}
"""

        when:
        runTasks('buildRpm')

        then:
        // Evaluate response
        def scanFiles = Scanner.scan(file('build/distributions/sample-1.0.0.noarch.rpm')).files

        ['./usr/local/myproduct', './usr/local/myproduct/bin', './usr/local/myproduct/bin/apple', './usr/share/myproduct', './usr/share/myproduct/etc', './usr/share/myproduct/etc/banana'] == scanFiles*.name
        [ DIR, DIR, FILE, DIR, DIR, FILE] == scanFiles*.type

    }

    def 'filter_expression'() {
        given:
        File appleFile = new File(projectDir, 'src/apple')
        FileUtils.forceMkdirParent(appleFile)
        appleFile.text = '{{BASE}}/apple'

        buildFile << """
plugins {
    id 'com.netflix.nebula.rpm'
}

version = '1.0.0'

task buildRpm(type: Rpm) {
    packageName = 'sample'
    from(${GradleUtils.quotedIfPresent(appleFile.getParentFile().path)}) {
        into '/usr/local/myproduct/bin'
        filter({ line ->
            return line.replaceAll(/\\{\\{BASE\\}\\}/, '/usr/local/myproduct')
        })
    }
}
"""

        when:
        runTasks('buildRpm')

        then:
        def scan = Scanner.scan(file('build/distributions/sample-1.0.0.noarch.rpm'))
        def scannerApple = scan.files.find { it.name =='./usr/local/myproduct/bin/apple'}
        scannerApple.asString() == '/usr/local/myproduct/apple'
    }
    
    def 'verifyCopySpecCanComeFromExtension'() {
        given:
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'apple').text = 'apple'

        File etcDir = new File(projectDir, 'etc')
        etcDir.mkdirs()
        new File(etcDir, 'banana.conf').text = 'banana=true'

        // Simulate SystemPackagingBasePlugin
        buildFile << """
plugins {
    id 'com.netflix.nebula.rpm'
}

def parentExten = project.extensions.create('rpmParent', com.netflix.gradle.plugins.packaging.ProjectPackagingExtension, project)

version = '1.0'

task buildRpm(type: Rpm) {
    packageName 'example'
    from(${GradleUtils.quotedIfPresent(srcDir.path)}) {
        into('/usr/local/src')
    }
    
    parentExten.from(${GradleUtils.quotedIfPresent(etcDir.path)}) {
        createDirectoryEntry true
        into('/conf/defaults')
    }
}
"""

        when:
        runTasks('buildRpm')

        then:
        // Evaluate response
        def archive = file('build/distributions/example-1.0.noarch.rpm')
        archive.exists()
        def scan = Scanner.scan(archive)
        // Parent will come first
        ['./conf', './conf/defaults', './conf/defaults/banana.conf', './usr/local/src', './usr/local/src/apple'] == scan.files*.name
        [DIR, DIR, FILE, DIR, FILE] == scan.files*.type
    }


    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/48")
    def "Does not throw UnsupportedOperationException when copying external artifact with createDirectoryEntry option"() {
        given:
        String testCoordinates = 'com.netflix.nebula:a:1.0.0'
        DependencyGraph graph = new DependencyGraph([testCoordinates])
        File reposRootDir = directory('build/repos')
        GradleDependencyGenerator generator = new GradleDependencyGenerator(graph, reposRootDir.absolutePath)
        generator.generateTestMavenRepo()

        when:
        buildFile << """
plugins {
    id 'com.netflix.nebula.rpm'
}

configurations {
    myConf
}

dependencies {
    myConf ${GradleUtils.quotedIfPresent(testCoordinates)}
}

repositories {
    maven {
        url {
            "file://${reposRootDir}/mavenrepo"
        }
    }
}

task buildRpm(type: Rpm) {
    packageName = 'bleah'
    
    from(configurations.myConf) {
        createDirectoryEntry = true
        into('root/lib')
    }
}
"""

        then:
        runTasks('buildRpm')
    }


    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/58")
    def 'preserve symlinks without closure'() {
        given:
        File packageDir = directory("package")
        packageDir.mkdirs()
        File target = new File(packageDir,"my-script.sh")
        target.createNewFile()
        File file = new File(packageDir,'bin/my-symlink')
        FileUtils.forceMkdirParent(file)
        java.nio.file.Files.createSymbolicLink(file.toPath(), target.toPath())
        buildFile << """
plugins {
    id 'com.netflix.nebula.rpm'
}

task buildRpm(type: Rpm) {
    packageName = 'example'
    version '3'
    from 'package'
}
"""

        when:
        runTasks('buildRpm', '--warning-mode', 'none')

        then:
        def scan = Scanner.scan(this.file('build/distributions/example-3.noarch.rpm'))
        def symlink = scan.files.find { it.name == './bin/my-symlink' }
        symlink.header.type == SYMLINK
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/58")
    def 'preserve symlinks with closure'() {
        given:
        File packageDir = directory("package")
        packageDir.mkdirs()
        File target = new File(packageDir,"my-script.sh")
        target.createNewFile()
        File file = new File(packageDir,'bin/my-symlink')
        FileUtils.forceMkdirParent(file)
        java.nio.file.Files.createSymbolicLink(file.toPath(), target.toPath())
        buildFile << """
plugins {
    id 'com.netflix.nebula.rpm'
}

task buildRpm(type: Rpm) {
    packageName = 'example'
    version '4'
    from('package') {
        into '/lib'
    }
}
"""

        when:
        runTasks('buildRpm', '--warning-mode', 'none')

        then:
        def scan = Scanner.scan(this.file('build/distributions/example-4.noarch.rpm'))
        def symlink = scan.files.find { it.name == './lib/bin/my-symlink' }
        symlink.header.type == SYMLINK
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/416")
    def 'directory entries in ospackage extension propagates to rpm and deb'() {
        given:
        File bananaFile = new File(projectDir, 'test/banana')
        FileUtils.forceMkdirParent(bananaFile)
        bananaFile.text = 'banana'

        buildFile << """
apply plugin: 'com.netflix.nebula.rpm'
apply plugin: 'com.netflix.nebula.ospackage-base'
version = '1.0.0'

ospackage {
    directory('/usr/share/myproduct/from-extension')
}
task buildRpm(type: Rpm) {
    packageName = 'sample'    
    directory('/usr/share/myproduct/from-task')
}
"""
        when:
        runTasksSuccessfully('buildRpm')

        then:
        // Evaluate response
        def scanFiles = Scanner.scan(file('build/distributions/sample-1.0.0.noarch.rpm')).files

        ['./usr/share/myproduct/from-extension', './usr/share/myproduct/from-task'] == scanFiles*.name
        [ DIR, DIR] == scanFiles*.type
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/246")
    def 'addParentDirs in ospackage extension propagates to rpm and deb'() {
        given:
        File bananaFile = new File(projectDir, 'test/banana')
        FileUtils.forceMkdirParent(bananaFile)
        bananaFile.text = 'banana'

        buildFile << """
apply plugin: 'com.netflix.nebula.rpm'
apply plugin: 'com.netflix.nebula.ospackage-base'
version = '1.0.0'

ospackage {
    addParentDirs false
}
task buildRpm(type: Rpm) {
    packageName = 'sample'
    
    from(${GradleUtils.quotedIfPresent(bananaFile.getParentFile().path)}) {
        into '/usr/share/myproduct/etc'     
    }
}
"""
        when:
        runTasksSuccessfully('buildRpm')

        then:
        // Evaluate response
        def scanFiles = Scanner.scan(file('build/distributions/sample-1.0.0.noarch.rpm')).files

        ['./usr/share/myproduct/etc/banana'] == scanFiles*.name
        [ FILE] == scanFiles*.type
    }	
}
