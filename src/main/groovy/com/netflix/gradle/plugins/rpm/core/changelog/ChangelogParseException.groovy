/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.changelog

import groovy.transform.CompileStatic

/**
 * Base class of the errors raised for a malformed changelog.
 */
@CompileStatic
abstract class ChangelogParseException extends Exception implements ParserExceptionClient {
    ChangelogParseException(String message) {
        super(message)
    }
}
