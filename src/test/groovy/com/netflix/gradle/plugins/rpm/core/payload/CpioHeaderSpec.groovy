package com.netflix.gradle.plugins.rpm.core.payload

import java.nio.channels.Channels

import spock.lang.Specification

class CpioHeaderSpec extends Specification {

    def 'writes and reads back a header'() {
        given:
        CpioHeader header = new CpioHeader('./opt/app/file.txt')
        header.setType(CpioHeader.FILE)
        header.setPermissions(0640)
        header.setInode(7)
        header.setFileSize(12)
        header.setMtime(1_700_000_000_123L)
        ByteArrayOutputStream out = new ByteArrayOutputStream()

        when:
        int written = header.write(Channels.newChannel(out), 0)
        CpioHeader read = new CpioHeader()
        int total = read.read(Channels.newChannel(new ByteArrayInputStream(out.toByteArray())), 0)

        then:
        written == out.size()
        written % 4 == 0
        total == written
        read.name == './opt/app/file.txt'
        read.type == CpioHeader.FILE
        read.permissions == 0640
        read.mode == 0100640
        read.inode == 7
        read.fileSize == 12
        read.mtime == 1_700_000_000
        new String(out.toByteArray(), 0, 6, 'US-ASCII') == '070701'
    }

    def 'marks the trailer'() {
        given:
        CpioHeader trailer = new CpioHeader()

        when:
        trailer.setLast()

        then:
        trailer.isLast()
        trailer.name == 'TRAILER!!!'
    }

    def 'uses octal default permissions'() {
        expect:
        CpioHeader.DEFAULT_FILE_PERMISSION == 420
        CpioHeader.DEFAULT_DIRECTORY_PERMISSION == 493
    }
}
