package com.netflix.gradle.plugins.deb

import com.netflix.gradle.plugins.deb.DebCopyAction.InstallDir
import spock.lang.Specification
import spock.lang.Unroll

class InstallLineGeneratorSpec extends Specification {

    @Unroll
    def 'should generate correct install line for #dir.name'(InstallDir dir, String line) {
        given:
        def generator = new InstallLineGenerator()

        expect:
        generator.generate(dir) == line

        where:
        dir                                                            | line
        new InstallDir('dir-with-default-user-and-group')              | 'install -d dir-with-default-user-and-group'
        new InstallDir('dir-with-user-only', 'user-b')                 | 'install -o user-b -d dir-with-user-only'
        new InstallDir('dir-with-user-and-group', 'user-c', 'group-c') | 'install -o user-c -g group-c -d dir-with-user-and-group'
        new InstallDir('dir-with-group-only', null, 'group-d')         | 'install -g group-d -d dir-with-group-only'
    }
}
