/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.header

/**
 * Target operating systems. The ordinal is the value written to the RPM lead,
 * so the order of these constants must not change.
 */
enum Os {
    UNKNOWN,
    LINUX,
    IRIX,
    SOLARIS,
    SUNOS,
    AMIGAOS,
    AIX,
    HPUX10,
    OSF1,
    FREEBSD,
    SCO,
    IRIX64,
    NEXTSTEP,
    BSDI,
    MACHTEN,
    CYGWINNT,
    CYGWIN95,
    UNIXSV,
    MINT,
    OS390,
    VMESA,
    LINUX390,
    MACOSX
}
