/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.changelog

import groovy.transform.CompileStatic

/**
 * Raised when the date of a changelog entry cannot be parsed.
 */
@CompileStatic
class InvalidChangelogDateException extends ChangelogParseException {
    InvalidChangelogDateException(String message) {
        super(ParserExceptionClient.INVALID_DATE + message)
    }
}
