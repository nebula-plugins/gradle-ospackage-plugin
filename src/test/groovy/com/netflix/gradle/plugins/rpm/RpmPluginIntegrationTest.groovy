package com.netflix.gradle.plugins.rpm

import com.netflix.gradle.plugins.utils.GradleUtils
import nebula.test.IntegrationSpec
import nebula.test.dependencies.DependencyGraph
import nebula.test.dependencies.GradleDependencyGenerator
import org.apache.commons.io.FileUtils
import spock.lang.Issue
import spock.lang.Unroll

import static org.redline_rpm.header.Header.HeaderTag.DESCRIPTION
import static org.redline_rpm.header.Header.HeaderTag.NAME
import static org.redline_rpm.payload.CpioHeader.*

class RpmPluginIntegrationTest extends IntegrationSpec {
    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/82")
    def "rpm task is marked up-to-date when setting arch or os property"() {

            given:
            File libDir = new File(projectDir, 'lib')
            libDir.mkdirs()
            new File(libDir, 'a.java').text = "public class A { }"
        buildFile << '''
apply plugin: 'com.netflix.nebula.rpm'

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
        runTasksSuccessfully('buildRpm')

        and:
        def result = runTasksSuccessfully('buildRpm')

        then:
        result.wasUpToDate(':buildRpm')
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/104")
    @Unroll
    def "Translates extension packageDescription '#description' to header entry for RPM task"() {
        given:
        File libDir = new File(projectDir, 'lib')
        libDir.mkdirs()
        new File(libDir, 'a.java').text = "public class A { }"
        buildFile << """
apply plugin: 'com.netflix.nebula.ospackage'

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
        runTasksSuccessfully('buildRpm')

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
apply plugin: 'com.netflix.nebula.rpm'

task buildRpm(type: Rpm) {
    version '1'
    from('lib') {
            into 'lib'
    }
}
"""

        when:
        runTasksSuccessfully('buildRpm', '--warning-mode', 'none')

        then:
        def scan = Scanner.scan(file('build/distributions/projectNameDefault-1.noarch.rpm'))
        def actual = Scanner.getHeaderEntryString(scan, NAME)
        'projectNameDefault' == actual
    }


    def 'file handle closed'() {
        given:
        buildFile << """
apply plugin: 'com.netflix.nebula.rpm'

task buildRpm(type: Rpm) {
}
"""
        when:
        runTasksSuccessfully('buildRpm')

        then:
        // see https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/200#issuecomment-244666158
        // if file is not closed this will fail
        runTasksSuccessfully('clean')
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
apply plugin: 'com.netflix.nebula.rpm'

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
        runTasksSuccessfully('buildRpm')

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
apply plugin: 'com.netflix.nebula.rpm'

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
        runTasksSuccessfully('buildRpm')

        then:
        def scan = Scanner.scan(file('build/distributions/sample-1.0.0.noarch.rpm'))
        def scannerApple = scan.files.find { it.name =='./usr/local/myproduct/bin/apple'}
        scannerApple.asString() == '/usr/local/myproduct/apple'
    }

    def 'usesArchivesBaseName'() {
        System.setProperty('ignoreDeprecations', 'true')
        File srcDir = new File(projectDir, 'lib')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')
        // archivesBaseName is an artifact of the BasePlugin, and won't exist until it's applied.
        buildFile << """
apply plugin: BasePlugin
apply plugin: 'com.netflix.nebula.rpm'

archivesBaseName = 'foo'
version = '1'

task buildRpm(type: Rpm) {
    from('lib') {
            into 'lib'
    }
}
"""
        when:
        runTasksSuccessfully('buildRpm')

        then:
        def scan = Scanner.scan(file('build/distributions/foo-1.noarch.rpm'))
        def actual = Scanner.getHeaderEntryString(scan, NAME)
        'foo' == actual
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
apply plugin: 'com.netflix.nebula.rpm'

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
        runTasksSuccessfully('buildRpm')

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
apply plugin: 'com.netflix.nebula.rpm'

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
        runTasksSuccessfully('buildRpm')
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
apply plugin: 'com.netflix.nebula.rpm'

task buildRpm(type: Rpm) {
    packageName = 'example'
    version '3'
    from 'package'
}
"""

        when:
        runTasksSuccessfully('buildRpm', '--warning-mode', 'none')

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
apply plugin: 'com.netflix.nebula.rpm'

task buildRpm(type: Rpm) {
    packageName = 'example'
    version '4'
    from('package') {
        into '/lib'
    }
}
"""

        when:
        runTasksSuccessfully('buildRpm', '--warning-mode', 'none')

        then:
        def scan = Scanner.scan(this.file('build/distributions/example-4.noarch.rpm'))
        def symlink = scan.files.find { it.name == './lib/bin/my-symlink' }
        symlink.header.type == SYMLINK
    }
}
