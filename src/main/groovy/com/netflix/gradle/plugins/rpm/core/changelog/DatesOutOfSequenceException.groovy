/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.changelog

import groovy.transform.CompileStatic

/**
 * Raised when changelog entries are not in descending date order.
 */
@CompileStatic
class DatesOutOfSequenceException extends ChangelogParseException {
    DatesOutOfSequenceException() {
        super(ParserExceptionClient.OUT_OF_SEQUENCE)
    }
}
