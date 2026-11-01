/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.header

/**
 * Target architectures. The ordinal is the value written to the RPM lead,
 * so the order of these constants must not change.
 */
enum Architecture {
    NOARCH,
    I386,
    ALPHA,
    SPARC,
    MIPS,
    PPC,
    M68K,
    IP,
    RS6000,
    IA64,
    SPARC64,
    MIPSEL,
    ARM,
    MK68KMINT,
    S390,
    S390X,
    PPC64,
    SH,
    XTENSA,
    X86_64,
    PPC64LE,
    AARCH64
}
