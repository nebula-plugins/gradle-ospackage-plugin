/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.changelog

import groovy.transform.CompileStatic

/**
 * Messages of the changelog parse exceptions.
 */
@CompileStatic
interface ParserExceptionClient {
    String MUST_START_WITH_ASTERISK = 'Changelog must begin with *'
    String INVALID_DATE = 'Invalid Changelog Date:'
    String OUT_OF_SEQUENCE = 'Changelog entries must be in descending date order.'
    String INCOMPLETE_ENTRY = 'Changelog entries must contain a Date, a User, and a Description'
}
