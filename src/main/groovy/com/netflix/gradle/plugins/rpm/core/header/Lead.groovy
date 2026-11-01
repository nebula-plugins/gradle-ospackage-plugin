/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.header

import com.netflix.gradle.plugins.rpm.core.Util

import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.charset.StandardCharsets

/**
 * The fixed 96 byte lead that starts every RPM file.
 */
@CompileStatic
class Lead {

    public static final int LEAD_SIZE = 96
    // 0xEDABEEDB does not fit in a signed int, so Groovy would make it a long without the cast.
    private static final int MAGIC = (int) 0xEDABEEDBL
    // Unknown rpm tag defaults to 0xFF (see rpmtag.h)
    private static final int UNKNOWN_OS = 0xFF

    protected byte major = 3
    protected byte minor
    protected RpmType type = RpmType.BINARY
    protected Architecture arch = Architecture.NOARCH
    protected String name
    protected Os os = Os.LINUX
    protected short sigtype = 5

    CharSequence getName() { return name }

    Architecture getArch() { return arch }

    void setMajor(byte major) {
        this.major = major
    }

    void setMinor(byte minor) {
        this.minor = minor
    }

    void setType(RpmType type) {
        this.type = type
    }

    void setArch(Architecture arch) {
        this.arch = arch
    }

    void setName(String name) {
        this.name = name
    }

    void setOs(Os os) {
        this.os = os
    }

    void setSigtype(short sigtype) {
        this.sigtype = sigtype
    }

    void read(ReadableByteChannel channel) throws IOException {
        ByteBuffer lead = Util.fill(channel, LEAD_SIZE)
        Util.check(MAGIC, lead.getInt())
        major = lead.get()
        minor = lead.get()
        type = RpmType.values()[lead.getShort()]
        final short tmp = lead.getShort()
        if (tmp < Architecture.values().length) arch = Architecture.values()[tmp]
        ByteBuffer data = ByteBuffer.allocate(66)
        lead.get(data.array())
        StringBuilder builder = new StringBuilder()
        while (data.hasRemaining()) {
            byte b = data.get()
            if (b == (byte) 0) break
            builder.append((char) b)
        }
        name = builder.toString()
        short o = lead.getShort()
        os = o != UNKNOWN_OS ? Os.values()[o] : Os.UNKNOWN
        sigtype = lead.getShort()
        if (lead.remaining() != 16) throw new IllegalStateException("Expected 16 remaining, found '" + lead.remaining() + "'.")
    }

    void write(WritableByteChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(LEAD_SIZE)
        buffer.putInt(MAGIC)
        buffer.put(major)
        buffer.put(minor)
        buffer.putShort((short) type.ordinal())
        buffer.putShort((short) arch.ordinal())
        byte[] data = new byte[66]
        byte[] encoded = name.getBytes(StandardCharsets.UTF_8)
        System.arraycopy(encoded, 0, data, 0, Math.min(data.length, encoded.length))
        buffer.put(data)
        buffer.putShort((short) (os.ordinal() != 0 ? os.ordinal() : UNKNOWN_OS))
        buffer.putShort(sigtype)
        buffer.position(buffer.position() + 16)
        buffer.flip()
        if (buffer.remaining() != LEAD_SIZE) throw new IllegalStateException("Invalid lead size generated with '" + buffer.remaining() + "' bytes.")
        Util.empty(channel, buffer)
    }

    String toString() {
        StringBuilder builder = new StringBuilder()
        builder.append('Version: ').append((int) major).append('.').append((int) minor).append('\n')
        builder.append('Type: ').append(type).append('\n')
        builder.append('Arch: ').append(arch).append('\n')
        builder.append('Name: ').append(name).append('\n')
        builder.append('OS: ').append(os).append('\n')
        builder.append('Sig type: ').append((int) sigtype).append('\n')
        return builder.toString()
    }
}
