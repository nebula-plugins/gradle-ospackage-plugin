/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.changelog

import groovy.transform.CompileStatic

/**
 * Raised when the first non-comment line of a changelog does not start with an asterisk.
 */
@CompileStatic
class NoInitialAsteriskException extends ChangelogParseException {
    NoInitialAsteriskException() {
        super(ParserExceptionClient.MUST_START_WITH_ASTERISK)
    }
}
