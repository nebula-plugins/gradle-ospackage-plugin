/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/**
 * Raised when an RPM cannot be produced or read.
 */
@CompileStatic
@InheritConstructors
class RpmException extends IOException {
}
