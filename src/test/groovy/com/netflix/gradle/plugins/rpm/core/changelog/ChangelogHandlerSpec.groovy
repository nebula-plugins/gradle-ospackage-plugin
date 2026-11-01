package com.netflix.gradle.plugins.rpm.core.changelog

import com.netflix.gradle.plugins.rpm.core.Builder
import com.netflix.gradle.plugins.rpm.core.header.Header
import com.netflix.gradle.plugins.rpm.core.header.Header.HeaderTag

import java.text.SimpleDateFormat

import spock.lang.Specification

import static com.netflix.gradle.plugins.rpm.core.RpmTestFixtures.resource

class ChangelogHandlerSpec extends Specification {

    def 'fails for a missing changelog file'() {
        when:
        new Builder().addChangelogFile(new File('non.existent.file'))

        then:
        thrown(FileNotFoundException)
    }

    def 'fails for a malformed changelog'() {
        when:
        new Builder().addChangelogFile(resource('changelog/bad.changelog'))

        then:
        thrown(NoInitialAsteriskException)
    }

    def 'accepts a changelog with comments'() {
        when:
        new Builder().addChangelogFile(resource('changelog/changelog.with.comments'))

        then:
        noExceptionThrown()
    }

    def 'adds one time, name and text per entry'() {
        given:
        Header header = new Header()

        when:
        new ChangelogHandler(header).addChangeLog(resource('changelog/changelog'))

        then:
        (header.getEntry(HeaderTag.CHANGELOGTIME).values as int[]).length == 10
        (header.getEntry(HeaderTag.CHANGELOGNAME).values as String[]).length == 10
        (header.getEntry(HeaderTag.CHANGELOGTEXT).values as String[]).length == 10
        (header.getEntry(HeaderTag.CHANGELOGTIME).values as int[])[0] == new SimpleDateFormat('EEE MMM dd yyyy', Locale.ENGLISH).parse('Tue Feb 24 2015').time.intdiv(1000)
        (header.getEntry(HeaderTag.CHANGELOGNAME).values as String[])[0] == 'George Washington'
    }
}
