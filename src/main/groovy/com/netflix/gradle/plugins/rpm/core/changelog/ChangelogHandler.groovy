/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.changelog

import com.netflix.gradle.plugins.rpm.core.header.Header
import com.netflix.gradle.plugins.rpm.core.header.Header.HeaderTag

import groovy.transform.CompileStatic

/**
 * Parses a changelog file and adds its entries to the CHANGELOGTIME, CHANGELOGNAME and
 * CHANGELOGTEXT header tags.
 */
@CompileStatic
class ChangelogHandler {

    private final Header header

    ChangelogHandler(Header header) {
        this.header = header
    }

    /**
     * Adds the specified changelog file to the RPM header.
     * @param changelogFile the changelog file
     * @throws IOException if the file cannot be read
     * @throws ChangelogParseException if the file is not in the expected format
     */
    void addChangeLog(File changelogFile) throws IOException, ChangelogParseException {
        List<ChangelogEntry> entries
        InputStream changelog = new FileInputStream(changelogFile)
        try {
            entries = new ChangelogParser().parse(changelog)
        } finally {
            changelog.close()
        }
        for (ChangelogEntry entry : entries) {
            addChangeLogEntry(entry)
        }
    }

    private void addChangeLogEntry(ChangelogEntry entry) {
        long epochSecs = entry.getChangeLogTime().getTime().intdiv(1000L).longValue()
        int unixdate = (int) epochSecs
        header.addOrAppendEntry(HeaderTag.CHANGELOGTIME, [unixdate] as int[])
        header.addOrAppendEntry(HeaderTag.CHANGELOGNAME, [entry.getUserMakingChange()] as String[])
        header.addOrAppendEntry(HeaderTag.CHANGELOGTEXT, [entry.getDescription()] as String[])
    }
}
