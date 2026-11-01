/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.header

import groovy.transform.CompileStatic

/**
 * Dependency comparison and script flags, as defined by {@code lib/rpmds.h}.
 */
@CompileStatic
interface Flags {
    int LESS = 0x02
    int GREATER = 0x04
    int EQUAL = 0x08
    int SCRIPT_POSTTRANS = 0x20
    int PREREQ = 0x40
    int SCRIPT_PRETRANS = 0x80
    int INTERP = 0x100
    int SCRIPT_PRE = 0x200
    int SCRIPT_POST = 0x400
    int SCRIPT_PREUN = 0x800
    int SCRIPT_POSTUN = 0x1000
    int SCRIPT_TRIGGERIN = 0x10000
    int SCRIPT_TRIGGERUN = 0x20000
    int SCRIPT_TRIGGERPOSTUN = 0x40000
    int SCRIPT_TRIGGERPREIN = 0x2000000
    int RPMLIB = (0x1000000 | PREREQ)
}
