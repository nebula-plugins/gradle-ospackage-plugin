/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.changelog

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Parses a changelog in the format of the {@code %changelog} section of a spec file:
 * <pre>
 * * Tue Feb 24 2015 George Washington
 *   - Add the foo feature
 * </pre>
 * Entries must be in descending date order. Lines starting with '#' are comments.
 */
@CompileStatic
@PackageScope
class ChangelogParser {

    private static final int TIME_LEN = 15
    private static final String DATE_PATTERN = 'EEE MMM dd yyyy'

    /**
     * @param lines the lines of the changelog
     * @return the parsed entries, in file order
     * @throws ChangelogParseException if the changelog is malformed
     */
    List<ChangelogEntry> parse(String[] lines) throws ChangelogParseException {
        // SimpleDateFormat is not thread safe, so use a fresh instance per parse
        SimpleDateFormat fmt = new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH)
        fmt.setLenient(false)
        List<ChangelogEntry> result = new LinkedList<ChangelogEntry>()
        if (lines.length == 0) {
            return result
        }

        ParsingState state = ParsingState.NEW
        Date lastTime = null
        ChangelogEntry entry = new ChangelogEntry()
        String restOfLine = null
        StringBuilder descr = new StringBuilder()
        int index = 0
        String line = lines[index]
        boolean done = false

        while (!done) {
            switch (state) {
                case ParsingState.NEW:
                    if (line.startsWith('#')) {
                        if (++index < lines.length) {
                            line = lines[index]
                            break
                        } else {
                            return result
                        }
                    } else if (!line.startsWith('*')) {
                        throw new NoInitialAsteriskException()
                    }
                    restOfLine = line.substring(1).trim()
                    state = ParsingState.TIME
                    break
                case ParsingState.TIME:
                    if (restOfLine.length() < TIME_LEN) {
                        throw new InvalidChangelogDateException(restOfLine)
                    }
                    String timestr = restOfLine.substring(0, TIME_LEN)
                    try {
                        Date entryTime = fmt.parse(timestr)
                        if (lastTime != null && lastTime.before(entryTime)) {
                            throw new DatesOutOfSequenceException()
                        }
                        entry.setChangeLogTime(entryTime)
                        lastTime = entryTime
                    } catch (ParseException e) {
                        throw new InvalidChangelogDateException(e.getMessage())
                    }
                    state = ParsingState.NAME
                    break
                case ParsingState.NAME:
                    String name = restOfLine.substring(TIME_LEN).trim()
                    if (name.length() > 0) {
                        entry.setUserMakingChange(name)
                    }
                    state = ParsingState.TEXT
                    break
                case ParsingState.TEXT:
                    index++
                    if (index < lines.length) {
                        line = lines[index]
                        if (line.startsWith('#')) {
                            break
                        } else if (line.startsWith('*')) {
                            // a new entry begins
                            if (descr.length() > 1) {
                                entry.setDescription(descr.toString().substring(0, descr.length() - 1))
                            }
                            if (entry.isComplete()) {
                                result.add(entry)
                                entry = new ChangelogEntry()
                                descr = new StringBuilder()
                                state = ParsingState.NEW
                            } else {
                                throw new IncompleteChangelogEntryException()
                            }
                        } else {
                            descr.append(line).append('\n')
                        }
                    } else {
                        done = true
                    }
                    break
            }
        }
        if (descr.length() > 1) {
            entry.setDescription(descr.toString().substring(0, descr.length() - 1))
        }
        if (entry.isComplete()) {
            result.add(entry)
        } else {
            throw new IncompleteChangelogEntryException()
        }
        return result
    }

    /**
     * Reads the changelog from a stream, dropping comment lines.
     * @param stream the changelog contents
     * @return the parsed entries, in file order
     * @throws IOException if the stream cannot be read
     * @throws ChangelogParseException if the changelog is malformed
     */
    List<ChangelogEntry> parse(InputStream stream) throws IOException, ChangelogParseException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream))
        List<String> lines = new ArrayList<String>()
        String line
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith('#')) {
                lines.add(line)
            }
        }
        return parse(lines.toArray(new String[0]))
    }
}
