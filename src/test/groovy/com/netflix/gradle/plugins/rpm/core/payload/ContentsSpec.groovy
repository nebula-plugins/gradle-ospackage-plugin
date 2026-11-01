package com.netflix.gradle.plugins.rpm.core.payload

import spock.lang.Specification

class ContentsSpec extends Specification {

    def 'lists the parents of #path'() {
        given:
        List<String> parents = []

        when:
        new Contents().listParents(parents, new File(path))

        then:
        parents == expected

        where:
        path                       || expected
        '/one/two/three/four'      || ['/one/two/three', '/one/two', '/one']
        '/bin/one/two/three/four'  || ['/bin/one/two/three', '/bin/one/two', '/bin/one']
        '/usr/lib/app/file'        || ['/usr/lib/app']
    }

    def 'a global builtin directory applies to new contents'() {
        given:
        List<String> parents = []
        Contents.addBuiltinDirectory('/home')

        when:
        new Contents().listParents(parents, new File('/home/one/two/three/four'))

        then:
        parents == ['/home/one/two/three', '/home/one/two', '/home/one']
    }

    def 'a local builtin directory only applies to its contents'() {
        given:
        List<String> parents = []
        List<String> otherParents = []
        Contents contents = new Contents()
        contents.addLocalBuiltinDirectory('/srv/app')

        when:
        contents.listParents(parents, new File('/srv/app/one/two'))
        new Contents().listParents(otherParents, new File('/srv/app/one/two'))

        then:
        parents == ['/srv/app/one']
        otherParents == ['/srv/app/one', '/srv/app']
    }

    def 'uses the dir mode for parent directories'() {
        given:
        Contents contents = new Contents()

        when:
        contents.addFile('/test/file.txt', new File('/test/source'), 0777, null, 'testuser', 'testgroup', 0111)
        Map<String, Integer> modes = contents.headers().collectEntries { [(it.name): it.permissions] }

        then:
        modes['/test'] == 0111
        modes['/test/file.txt'] == 0777
    }

    def 'uses default permissions and owners'() {
        given:
        Contents contents = new Contents()

        when:
        contents.addFile('/opt/a/file.txt', new File('/missing'))
        contents.addLink('/opt/a/link', 'file.txt')
        Map<String, CpioHeader> headers = contents.headers().collectEntries { [(it.name): it] }

        then:
        headers['/opt/a'].permissions == CpioHeader.DEFAULT_DIRECTORY_PERMISSION
        headers['/opt/a'].type == CpioHeader.DIR
        headers['/opt/a/file.txt'].permissions == CpioHeader.DEFAULT_FILE_PERMISSION
        headers['/opt/a/file.txt'].uname == 'root'
        headers['/opt/a/file.txt'].gname == 'root'
        headers['/opt/a/link'].type == CpioHeader.SYMLINK
        headers['/opt/a/link'].fileSize == 'file.txt'.length()
    }

    def 'ignores a path that was already added'() {
        given:
        Contents contents = new Contents()

        when:
        contents.addFile('/opt/file', new File('/first'), 0600)
        contents.addFile('/opt/file', new File('/second'), 0644)

        then:
        contents.size() == 1
        contents.headers().first().permissions == 0600
    }

    def 'orders entries by path and indexes their directories'() {
        given:
        Contents contents = new Contents()
        contents.addDirectory('/opt/b')
        contents.addDirectory('/opt/a')
        contents.addLink('/opt/a/z', 'target')

        expect:
        contents.headers()*.name == ['/opt/a', '/opt/a/z', '/opt/b']
        contents.dirNames as List == ['/opt/', '/opt/a/']
        contents.dirIndexes as List == [0, 1, 0]
        contents.baseNames as List == ['a', 'z', 'b']
        contents.linkTos as List == ['', 'target', '']
    }
}
