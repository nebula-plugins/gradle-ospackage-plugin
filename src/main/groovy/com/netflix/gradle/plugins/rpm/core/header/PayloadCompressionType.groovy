/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.header

/**
 * RPM payload compression tag values. The constant names match the values
 * stored in the {@code PAYLOADCOMPRESSOR} header tag.
 */
enum PayloadCompressionType {
    none,
    gzip,
    bzip2,
    xz
}
