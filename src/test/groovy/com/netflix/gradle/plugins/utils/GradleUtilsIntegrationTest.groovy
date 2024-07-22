package com.netflix.gradle.plugins.utils

import static org.redline_rpm.payload.CpioHeader.SYMLINK

import com.netflix.gradle.plugins.rpm.Scanner
import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class GradleUtilsIntegrationTest extends IntegrationSpec {

    def 'verifySymlinkDirNested'() {
        given:
        File rootSourceFile = createFile('source/sourceDir/subdir/text.txt')
        Path link = Paths.get(projectDir.absolutePath, 'source','notlib')
        Files.createSymbolicLink(link, rootSourceFile.parentFile.toPath())

        buildFile << """
apply plugin: 'nebula.rpm'

task buildRpm(type: Rpm) {
    packageName 'test'
    from('source') {
        into('/usr/local/')
    }
}
"""

        when:
        ExecutionResult result = runTasksSuccessfully('buildRpm')
        println result.standardOutput

        then:
        File archive = new File(projectDir, 'build/distributions/test-0.noarch.rpm')
        archive.exists()
        Scanner.ScannerResult scan = Scanner.scan(archive)
        Scanner.ScannerFile linkFile = scan.files.find { it.name == './usr/local/notlib'}
        linkFile.type == SYMLINK
        linkFile.asString() == 'sourceDir/subdir'
    }

    def 'verifySymlinkDirNestedWithLib'() {
        given:
        File rootSourceFile = createFile('source/sourceDir/subdir/text.txt')
        Path link = Paths.get(projectDir.absolutePath, 'source','lib')
        Files.createSymbolicLink(link, rootSourceFile.parentFile.toPath())

        buildFile << """
apply plugin: 'nebula.rpm'

task buildRpm(type: Rpm) {
    packageName 'test'
    from('source') {
        into('/usr/local/')
    }
}
"""

        when:
        ExecutionResult result = runTasksSuccessfully('buildRpm')
        println result.standardOutput

        then:
        File archive = new File(projectDir, 'build/distributions/test-0.noarch.rpm')
        archive.exists()
        Scanner.ScannerResult scan = Scanner.scan(archive)
        Scanner.ScannerFile linkFile = scan.files.find { it.name == './usr/local/lib'}
        linkFile.type == SYMLINK
        linkFile.asString() == 'sourceDir/subdir'
    }

}
