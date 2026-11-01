package com.netflix.gradle.plugins.rpm.core

import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.channels.Channels

import spock.lang.Specification

class UtilSpec extends Specification {

    def 'rounds #start up to the boundary mask #boundary'() {
        expect:
        Util.round(start, boundary) == rounded
        Util.difference(start, boundary) == rounded - start

        where:
        start | boundary || rounded
        0     | 3        || 0
        1     | 3        || 4
        4     | 3        || 4
        5     | 7        || 8
        9     | 7        || 16
        110   | 3        || 112
    }

    def 'encodes bytes as lower case hex'() {
        expect:
        Util.hex([0x00, 0x0f, 0x7f, 0x80, 0xff].collect { it as byte } as byte[]) == '000f7f80ff'
        Util.hex(new byte[0]) == ''
    }

    def 'normalizes windows paths'() {
        expect:
        Util.normalizePath('a\\b\\c') == 'a/b/c'
    }

    def 'fills a buffer completely'() {
        given:
        def input = Channels.newChannel(new ByteArrayInputStream([1, 2, 3] as byte[]))

        when:
        ByteBuffer buffer = Util.fill(input, 2)

        then:
        buffer.remaining() == 2
        buffer.get(1) == 2 as byte
    }

    def 'fails when the channel ends before the buffer is full'() {
        given:
        def input = Channels.newChannel(new ByteArrayInputStream([1, 2, 3] as byte[]))

        when:
        Util.fill(input, 4)

        then:
        thrown(BufferUnderflowException)
    }

    def 'reports mismatches in hex'() {
        when:
        Util.check(0xED, 0xEE)

        then:
        IOException e = thrown()
        e.message == 'check expected ed, found ee'
    }

    def 'dumps a buffer without moving its position'() {
        given:
        ByteBuffer buffer = ByteBuffer.wrap('hello'.bytes)
        StringBuilder out = new StringBuilder()

        when:
        Util.dump(buffer, out)

        then:
        buffer.position() == 0
        out.toString().contains(' 68 65 6c 6c 6f')
    }
}
