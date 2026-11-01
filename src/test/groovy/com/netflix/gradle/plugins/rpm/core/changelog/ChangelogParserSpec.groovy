package com.netflix.gradle.plugins.rpm.core.changelog

import spock.lang.Specification

class ChangelogParserSpec extends Specification {

    private static final String FIRST = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua'
    private static final String SECOND = 'quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.'

    ChangelogParser parser = new ChangelogParser()

    def 'parses a correctly formatted changelog'() {
        when:
        List<ChangelogEntry> entries = parser.parse([
                '* Tue Feb 24 2015 George Washington',
                FIRST,
                '* Tue Feb 10 2015 George Washington',
                SECOND,
        ] as String[])

        then:
        entries.size() == 2
        entries[0].userMakingChange == 'George Washington'
        entries[0].description == FIRST
        entries[1].description == SECOND
        entries[0].changeLogTime.after(entries[1].changeLogTime)
    }

    def 'keeps multi-line descriptions'() {
        when:
        List<ChangelogEntry> entries = parser.parse([
                '* Tue Feb 24 2015 George Washington',
                '- first line',
                '  second line',
        ] as String[])

        then:
        entries.size() == 1
        entries[0].description == '- first line\n  second line'
    }

    def 'ignores comments'() {
        when:
        List<ChangelogEntry> entries = parser.parse([
                '# ORDER MUST BE DESCENDING (most recent change at top)',
                '* Tue Feb 24 2015 George Washington',
                FIRST,
                '* Tue Feb 10 2015 George Washington',
                '# a random comment',
                SECOND,
        ] as String[])

        then:
        entries.size() == 2
        entries[1].description == SECOND
    }

    def 'returns no entries for an empty changelog'() {
        expect:
        parser.parse(new String[0]).isEmpty()
    }

    def 'fails with #exception.simpleName when #reason'() {
        when:
        parser.parse(lines as String[])

        then:
        ChangelogParseException e = thrown()
        exception.isInstance(e)
        e.message.startsWith(message)

        where:
        reason                            | lines                                                                                  || exception                         | message
        'dates are out of order'          | ['* Tue Feb 10 2015 George Washington', SECOND, '* Tue Feb 24 2015 George Washington', FIRST] || DatesOutOfSequenceException       | ParserExceptionClient.OUT_OF_SEQUENCE
        'the date format is wrong'        | ['* 02/24/2015 George Washington', FIRST]                                              || InvalidChangelogDateException     | ParserExceptionClient.INVALID_DATE
        'the day of the week is wrong'    | ['* Wed Feb 24 2015 George Washington', FIRST]                                         || InvalidChangelogDateException     | ParserExceptionClient.INVALID_DATE
        'an entry has no description'     | ['* Tue Feb 24 2015 George Washington', '* Tue Feb 10 2015 George Washington']         || IncompleteChangelogEntryException | ParserExceptionClient.INCOMPLETE_ENTRY
        'the first line has no asterisk'  | ['Tue Feb 24 2015 George Washington', FIRST]                                           || NoInitialAsteriskException        | ParserExceptionClient.MUST_START_WITH_ASTERISK
        'there is no user name'           | ['* Tue Feb 24 2015', FIRST]                                                           || IncompleteChangelogEntryException | ParserExceptionClient.INCOMPLETE_ENTRY
        'the user name is on its own line' | ['* Tue Feb 24 2015', 'George Washington', FIRST]                                     || IncompleteChangelogEntryException | ParserExceptionClient.INCOMPLETE_ENTRY
    }

    def 'parses a changelog file'() {
        when:
        List<ChangelogEntry> entries = getClass().getResourceAsStream('/com/netflix/gradle/plugins/rpm/core/changelog/changelog').withCloseable {
            parser.parse(it)
        }

        then:
        entries.size() == 10
    }
}
