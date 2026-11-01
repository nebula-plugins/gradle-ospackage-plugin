/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.changelog

import groovy.transform.CompileStatic

/**
 * Raised when a changelog entry is missing its date, user or description.
 */
@CompileStatic
class IncompleteChangelogEntryException extends ChangelogParseException {
    IncompleteChangelogEntryException() {
        super(ParserExceptionClient.INCOMPLETE_ENTRY)
    }
}
