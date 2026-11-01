package com.netflix.gradle.plugins.rpm.core.header

import com.netflix.gradle.plugins.rpm.core.header.AbstractHeader.Entry
import com.netflix.gradle.plugins.rpm.core.header.AbstractHeader.Tag

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import spock.lang.Specification

class AbstractHeaderSpec extends Specification {

    static class TestHeader extends AbstractHeader {
        protected boolean pad() { return false }
    }

    def 'reads and writes back #count value(s) of entry type #type'() {
        given:
        ByteBuffer buffer = ByteBuffer.wrap(bytes)
        Entry<?> entry = new TestHeader().createEntry(type)
        entry.setCount(count)

        when:
        entry.read(buffer)
        ByteBuffer written = ByteBuffer.allocate(bytes.length)
        entry.write(written)

        then:
        entry.values as List == expected
        written.array() == bytes

        where:
        type                   | count | bytes                             || expected
        Tag.CHAR_ENTRY         | 1     | [1] as byte[]                     || [1]
        Tag.CHAR_ENTRY         | 2     | [1, 2] as byte[]                  || [1, 2]
        Tag.INT8_ENTRY         | 1     | [1] as byte[]                     || [1]
        Tag.INT8_ENTRY         | 2     | [1, 2] as byte[]                  || [1, 2]
        Tag.INT16_ENTRY        | 1     | shorts(1)                         || [1]
        Tag.INT16_ENTRY        | 2     | shorts(1, 2)                      || [1, 2]
        Tag.INT32_ENTRY        | 1     | ints(1)                           || [1]
        Tag.INT32_ENTRY        | 2     | ints(1, 2)                        || [1, 2]
        Tag.INT64_ENTRY        | 1     | longs(1)                          || [1]
        Tag.INT64_ENTRY        | 2     | longs(1, 2)                       || [1, 2]
        Tag.STRING_ENTRY       | 1     | ascii('1234567\u0000')                || ['1234567']
        Tag.STRING_ENTRY       | 2     | ascii('1234567\u00007654321\u0000')   || ['1234567', '7654321']
        Tag.BIN_ENTRY          | 8     | ascii('12345678')                 || ascii('12345678') as List
        Tag.STRING_ARRAY_ENTRY | 1     | ascii('1234567\u0000')                || ['1234567']
        Tag.STRING_ARRAY_ENTRY | 2     | ascii('1234567\u00007654321\u0000')   || ['1234567', '7654321']
        Tag.I18NSTRING_ENTRY   | 1     | ascii('1234567\u0000')                || ['1234567']
        Tag.I18NSTRING_ENTRY   | 2     | ascii('1234567\u00007654321\u0000')   || ['1234567', '7654321']
    }

    def 'aligns entries to their natural boundary'() {
        given:
        TestHeader header = new TestHeader()

        expect:
        header.createEntry(type).getOffset(offset) == aligned

        where:
        type            | offset || aligned
        Tag.CHAR_ENTRY  | 3      || 3
        Tag.INT16_ENTRY | 3      || 4
        Tag.INT32_ENTRY | 5      || 8
        Tag.INT64_ENTRY | 9      || 16
    }

    def 'rejects an unknown entry type'() {
        when:
        new TestHeader().createEntry(42)

        then:
        IllegalStateException e = thrown()
        e.message == "Unknown entry type '42'."
    }

    def 'appends values to an existing entry'() {
        given:
        Header header = new Header()

        when:
        header.addOrAppendEntry(Header.HeaderTag.CHANGELOGNAME, ['a'] as String[])
        header.addOrAppendEntry(Header.HeaderTag.CHANGELOGNAME, ['b', 'c'] as String[])

        then:
        header.getEntry(Header.HeaderTag.CHANGELOGNAME).values == ['a', 'b', 'c'] as String[]
    }

    def 'round trips a header section'() {
        given:
        Header header = new Header()
        header.createEntry(Header.HeaderTag.HEADERIMMUTABLE, new byte[16])
        header.createEntry(Header.HeaderTag.NAME, 'name')
        header.createEntry(Header.HeaderTag.EPOCH, 3)
        header.createEntry(Header.HeaderTag.FILEMODES, [(short) 0100644] as short[])
        ByteArrayOutputStream out = new ByteArrayOutputStream()

        when:
        header.write(java.nio.channels.Channels.newChannel(out))
        Header read = new Header()
        int count = read.read(java.nio.channels.Channels.newChannel(new ByteArrayInputStream(out.toByteArray())))

        then:
        count == 4
        read.getEntry(Header.HeaderTag.NAME).values == ['name'] as String[]
        read.getEntry(Header.HeaderTag.EPOCH).values == [3] as int[]
        read.getEntry(Header.HeaderTag.FILEMODES).values == [(short) 0100644] as short[]
    }

    private static byte[] ascii(String value) {
        value.getBytes(StandardCharsets.US_ASCII)
    }

    private static byte[] shorts(int... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 2)
        values.each { buffer.putShort((short) it) }
        buffer.array()
    }

    private static byte[] ints(int... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4)
        values.each { buffer.putInt(it) }
        buffer.array()
    }

    private static byte[] longs(long... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 8)
        values.each { buffer.putLong(it) }
        buffer.array()
    }
}
