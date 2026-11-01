/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.payload

import com.netflix.gradle.plugins.rpm.core.Util

import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A header of the "new ASCII" CPIO format used for the RPM payload. When reading, iterative calls
 * to {@link #read} return the header description, which includes the number of bytes to read from
 * the channel for the file content.
 */
@CompileStatic
class CpioHeader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CpioHeader)

    public static final int DEFAULT_FILE_PERMISSION = 0644
    public static final int DEFAULT_DIRECTORY_PERMISSION = 0755
    public static final String DEFAULT_USERNAME = 'root'
    public static final String DEFAULT_GROUP = 'root'

    public static final int FIFO = 1
    public static final int CDEV = 2
    public static final int DIR = 4
    public static final int BDEV = 6
    public static final int FILE = 8
    public static final int SYMLINK = 10
    public static final int SOCKET = 12

    protected static final int CPIO_HEADER = 110
    protected static final String MAGIC = '070701'
    protected static final String TRAILER = 'TRAILER!!!'

    protected Charset charset = StandardCharsets.UTF_8

    protected int inode
    protected int type
    protected int permissions = DEFAULT_FILE_PERMISSION
    protected int uid
    protected String uname
    protected int gid
    protected String gname
    protected int nlink = 1
    protected long mtime
    protected int filesize
    protected int devMinor = 1
    protected int devMajor = 9
    protected int rdevMinor
    protected int rdevMajor
    protected int checksum
    protected String name
    protected int flags
    protected int verifyFlags = -1

    CpioHeader() {
    }

    CpioHeader(final String name) {
        this.name = name
    }

    CpioHeader(final File file) {
        this(file.getAbsolutePath(), file)
    }

    CpioHeader(final String name, final URL url) {
        try {
            URLConnection connection = url.openConnection()
            mtime = connection.getLastModified()
            filesize = connection.getContentLength()
            this.name = Util.normalizePath(name)
            setType(FILE)
        } catch (IOException e) {
            throw new RuntimeException(e)
        }
    }

    CpioHeader(final String name, final File file) {
        mtime = file.lastModified()
        filesize = (int) file.length()
        this.name = Util.normalizePath(name)
        if (file.isDirectory()) setType(DIR)
        else setType(FILE)
    }

    int getType() { return type }

    int getPermissions() { return permissions }

    int getRdevMajor() { return rdevMajor }

    int getRdevMinor() { return rdevMinor }

    int getDevMajor() { return devMajor }

    int getDevMinor() { return devMinor }

    int getMtime() { return (int) mtime.intdiv(1000L) }

    int getInode() { return inode }

    String getName() { return name }

    int getFlags() { return flags }

    int getVerifyFlags() { return verifyFlags }

    int getMode() { return (type << 12) | (permissions & 07777) }

    void setPermissions(int permissions) { this.permissions = permissions }

    void setType(int type) { this.type = type }

    void setFileSize(int filesize) { this.filesize = filesize }

    void setMtime(long mtime) { this.mtime = mtime }

    void setInode(int inode) { this.inode = inode }

    void setFlags(int flags) { this.flags = flags }

    void setVerifyFlags(int verifyFlags) { this.verifyFlags = verifyFlags }

    String getUname() { return this.uname }

    String getGname() { return this.gname }

    void setUname(String uname) { this.uname = uname }

    void setGname(String gname) { this.gname = gname }

    /**
     * Test to see if this is the last header, and is therefore the end of the archive. Uses the
     * CPIO magic trailer value to denote the last header of the stream.
     * @return true if last, false if not
     */
    boolean isLast() {
        return TRAILER == name
    }

    void setLast() {
        name = TRAILER
    }

    void setName(String name) {
        this.name = name
    }

    int getFileSize() {
        return filesize
    }

    protected ByteBuffer writeSix(CharSequence data) {
        return charset.encode(pad(data, 6))
    }

    protected ByteBuffer writeEight(int data) {
        return charset.encode(pad(Integer.toHexString(data), 8))
    }

    protected CharSequence readSix(CharBuffer buffer) {
        return readChars(buffer, 6)
    }

    protected int readEight(CharBuffer buffer) {
        return Integer.parseInt(readChars(buffer, 8).toString(), 16)
    }

    protected CharSequence readChars(CharBuffer buffer, int length) {
        if (buffer.remaining() < length) throw new IllegalStateException("Buffer has '" + buffer.remaining() + "' bytes but '" + length + "' are needed.")
        try {
            return buffer.subSequence(0, length)
        } finally {
            buffer.position(buffer.position() + length)
        }
    }

    protected String pad(CharSequence sequence, final int length) {
        String padded = sequence.toString()
        while (padded.length() < length) padded = '0' + padded
        return padded
    }

    protected int skip(final ReadableByteChannel channel, final int total) throws IOException {
        int skipped = Util.difference(total, 3)
        LOGGER.debug("Skipping '{}' bytes from stream at position '{}'.", skipped, total)
        Util.fill(channel, skipped)
        return skipped
    }

    int skip(final WritableByteChannel channel, int total) throws IOException {
        int skipped = Util.difference(total, 3)
        Util.empty(channel, ByteBuffer.allocate(skipped))
        LOGGER.debug("Skipping '{}' bytes from stream at position '{}'.", skipped, total)
        return skipped
    }

    int read(final ReadableByteChannel channel, int total) throws IOException {
        total += skip(channel, total)
        ByteBuffer descriptor = Util.fill(channel, CPIO_HEADER)
        CharBuffer buffer = charset.decode(descriptor)

        final CharSequence magic = readSix(buffer)
        if (MAGIC != magic.toString()) throw new IllegalStateException("Invalid magic number '" + magic + "' of length '" + magic.length() + "'.")
        inode = readEight(buffer)

        final int mode = readEight(buffer)
        permissions = mode & 07777
        type = mode >>> 12

        uid = readEight(buffer)
        gid = readEight(buffer)
        nlink = readEight(buffer)
        mtime = 1000L * readEight(buffer)
        filesize = readEight(buffer)
        devMajor = readEight(buffer)
        devMinor = readEight(buffer)
        rdevMajor = readEight(buffer)
        rdevMinor = readEight(buffer)
        int namesize = readEight(buffer)
        checksum = readEight(buffer)
        total += CPIO_HEADER

        name = charset.decode(Util.fill(channel, namesize - 1)).toString()
        Util.fill(channel, 1)
        total += namesize
        total += skip(channel, total)
        return total
    }

    /**
     * Write the content for the CPIO header, including the name immediately following. The name data is rounded
     * to the nearest 2 byte boundary as CPIO requires by appending a null when needed.
     * @param channel which channel to write on
     * @param total the number of bytes written to the archive so far
     * @return total written and skipped
     * @throws IOException there was an IO error
     */
    int write(final WritableByteChannel channel, int total) throws IOException {
        final ByteBuffer buffer = charset.encode(CharBuffer.wrap(name))
        int length = buffer.remaining() + 1
        ByteBuffer descriptor = ByteBuffer.allocate(CPIO_HEADER)
        descriptor.put(writeSix(MAGIC))
        descriptor.put(writeEight(inode))
        descriptor.put(writeEight(getMode()))
        descriptor.put(writeEight(uid))
        descriptor.put(writeEight(gid))
        descriptor.put(writeEight(nlink))
        descriptor.put(writeEight((int) mtime.intdiv(1000L)))
        descriptor.put(writeEight(filesize))
        descriptor.put(writeEight(devMajor))
        descriptor.put(writeEight(devMinor))
        descriptor.put(writeEight(rdevMajor))
        descriptor.put(writeEight(rdevMinor))
        descriptor.put(writeEight(length))
        descriptor.put(writeEight(checksum))
        descriptor.flip()

        total += CPIO_HEADER + length
        Util.empty(channel, descriptor)
        Util.empty(channel, buffer)
        Util.empty(channel, ByteBuffer.allocate(1))
        return total + skip(channel, total)
    }

    String toString() {
        StringBuilder builder = new StringBuilder()
        builder.append('Inode: ').append(inode).append('\n')
        builder.append('Permission: ').append(Integer.toString(permissions, 8)).append('\n')
        builder.append('Type: ').append(type).append('\n')
        builder.append('UID: ').append(uid).append('\n')
        builder.append('GID: ').append(gid).append('\n')
        builder.append('UserName: ').append(uname).append('\n')
        builder.append('GroupName: ').append(gname).append('\n')
        builder.append('Nlink: ').append(nlink).append('\n')
        builder.append('MTime: ').append(new Date(mtime)).append('\n')
        builder.append('FileSize: ').append(filesize).append('\n')
        builder.append('DevMinor: ').append(devMinor).append('\n')
        builder.append('DevMajor: ').append(devMajor).append('\n')
        builder.append('RDevMinor: ').append(rdevMinor).append('\n')
        builder.append('RDevMajor: ').append(rdevMajor).append('\n')
        builder.append('NameSize: ').append(name.length() + 1).append('\n')
        builder.append('Name: ').append(name).append('\n')
        return builder.toString()
    }
}
