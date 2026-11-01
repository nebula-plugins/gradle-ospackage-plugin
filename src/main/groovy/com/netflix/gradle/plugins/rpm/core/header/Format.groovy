/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.header

import groovy.transform.CompileStatic

import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel

/**
 * The three metadata sections of an RPM file: lead, signature and header.
 */
@CompileStatic
class Format {

    protected Lead lead = new Lead()
    protected Signature signature = new Signature()
    protected Header header = new Header()

    Lead getLead() {
        return lead
    }

    Signature getSignature() {
        return signature
    }

    Header getHeader() {
        return header
    }

    void read(final ReadableByteChannel channel) throws IOException {
        lead.read(channel)
        signature.read(channel)
        header.read(channel)
    }

    void write(final FileChannel channel) throws IOException {
        lead.write(channel)
        signature.write(channel)
        header.write(channel)
    }

    String toString() {
        return lead.toString() + signature + header
    }
}
