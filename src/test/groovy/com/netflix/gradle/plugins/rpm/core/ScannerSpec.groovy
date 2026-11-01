package com.netflix.gradle.plugins.rpm.core

import com.netflix.gradle.plugins.rpm.core.header.Format
import com.netflix.gradle.plugins.rpm.core.header.Header.HeaderTag

import spock.lang.Specification

import static com.netflix.gradle.plugins.rpm.core.RpmTestFixtures.payload
import static com.netflix.gradle.plugins.rpm.core.RpmTestFixtures.resource
import static com.netflix.gradle.plugins.rpm.core.RpmTestFixtures.scan

class ScannerSpec extends Specification {

    def 'dumps #rpm'() {
        given:
        PrintStream original = System.out
        ByteArrayOutputStream captured = new ByteArrayOutputStream()
        System.out = new PrintStream(captured, true)

        when:
        try {
            Scanner.main(resource(rpm).absolutePath)
        } finally {
            System.out = original
        }

        then:
        captured.toString().contains("Payload compression: payloadcompressor[tag=1125,type=6,count=1,size=${compressor.length() + 1},offset=")
        captured.toString().contains('Name: TRAILER!!!')

        where:
        rpm                                  | compressor
        'rpm-1-1.0-1.noarch.rpm'             | 'gzip'
        'rpm-3-1.0-1.somearch.rpm'           | 'gzip'
        'rpmtest-3-0.1.XZ.fc19.noarch.rpm'   | 'xz'
    }

    def 'reads an xz compressed payload'() {
        when:
        Map<String, byte[]> files = payload(resource('rpmtest-3-0.1.XZ.fc19.noarch.rpm'))

        then:
        !files.isEmpty()
    }

    def 'records where the header starts and ends'() {
        when:
        Format format = scan(resource('rpm-1-1.0-1.noarch.rpm'))

        then:
        format.header.startPos == 280
        format.header.endPos == 4760
    }

    def 'prints the file modes'() {
        given:
        Format format = scan(resource('rpm-1-1.0-1.noarch.rpm'))

        when:
        String description = format.toString()
        String modes = (description =~ /(?s).*filemodes\[[^\]]*\]\n[^0-9-]*([^\n]*).*/)[0][1]

        then:
        modes.split(', ') as List == ['33188', '41471', '16877', '33261', '33261', '33261', '33261', '41453', '33261',
                                      '16877', '33188', '33188', '16877', '33188', '41471']
    }

    def 'reads the lead and header of an existing rpm'() {
        when:
        Format format = scan(resource('rpm-1-1.0-1.noarch.rpm'))

        then:
        format.lead.name == 'rpm-1-1.0-1'
        format.header.getEntry(HeaderTag.NAME).values == ['rpm-1'] as String[]
        format.header.getEntry(HeaderTag.ARCH).values == ['noarch'] as String[]
    }
}
