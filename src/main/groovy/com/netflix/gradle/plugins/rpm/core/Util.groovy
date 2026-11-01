/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core

import com.netflix.gradle.plugins.rpm.core.header.AbstractHeader.Entry
import com.netflix.gradle.plugins.rpm.core.header.Header
import com.netflix.gradle.plugins.rpm.core.header.Header.HeaderTag
import com.netflix.gradle.plugins.rpm.core.header.PayloadCompressionType

import groovy.transform.CompileStatic

import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream

/**
 * General utilities needed to read and write RPM files.
 */
@CompileStatic
final class Util {

    private static final int ARRAY_SIZE = 4096
    private static final char[] HEX_DIGITS = '0123456789abcdef'.toCharArray()

    private Util() {}

    static int getTempArraySize(int totalSize) {
        return Math.min(ARRAY_SIZE, totalSize)
    }

    /**
     * Converts path characters from their native format to the "forward-slash" format expected
     * within RPM files.
     * @param path the path to the file
     * @return the normalized path
     */
    static String normalizePath(final String path) {
        return path.replace('\\', '/')
    }

    /**
     * Creates a new buffer and fills it with bytes from the provided channel. The amount of data
     * to read is specified in the arguments.
     *
     * @param input the channel to read from
     * @param size the number of bytes to read into a new buffer
     * @return a new buffer containing the bytes read
     * @throws IOException if an IO error occurs
     */
    static ByteBuffer fill(ReadableByteChannel input, int size) throws IOException {
        return fill(input, ByteBuffer.allocate(size))
    }

    /**
     * Fills the provided buffer with bytes from the provided channel. The amount of data to read
     * is dependant on the available space in the provided buffer.
     *
     * @param input the channel to read from
     * @param buffer the buffer to read into
     * @return the provided buffer, flipped and ready for reading
     * @throws IOException if an IO error occurs
     */
    static ByteBuffer fill(ReadableByteChannel input, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (input.read(buffer) == -1) throw new BufferUnderflowException()
        }
        buffer.flip()
        return buffer
    }

    /**
     * Empties the contents of the given buffer into the writable channel provided. The buffer
     * will be copied to the channel in its entirety.
     *
     * @param out the channel to write to
     * @param buffer the buffer to write out to the channel
     * @throws IOException if an IO error occurs
     */
    static void empty(WritableByteChannel out, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) out.write(buffer)
    }

    /**
     * Checks that two integers are the same, while generating a formatted error message if
     * they are not.
     *
     * @param expected the expected value
     * @param actual the actual value
     * @throws IOException if the two values do not match
     */
    static void check(int expected, int actual) throws IOException {
        if (expected != actual) throw new IOException('check expected ' + Integer.toHexString(0xff & expected) + ', found ' + Integer.toHexString(0xff & actual))
    }

    /**
     * Checks that two bytes are the same, while generating a formatted error message if
     * they are not.
     *
     * @param expected the expected value
     * @param actual the actual value
     * @throws IOException if the two values do not match
     */
    static void check(byte expected, byte actual) throws IOException {
        if (expected != actual) throw new IOException('check expected ' + Integer.toHexString(0xff & expected) + ', found ' + Integer.toHexString(0xff & actual))
    }

    static int difference(int start, int boundary) {
        return ((boundary + 1) - (start & boundary)) & boundary
    }

    static int round(int start, int boundary) {
        return (start + boundary) & ~boundary
    }

    /**
     * Pads the given buffer up to the indicated boundary. The RPM file format requires that
     * headers be aligned with various boundaries; this method pads output to match.
     *
     * @param buffer the buffer to pad zeros into
     * @param boundary the boundary to which we need to pad
     */
    static void pad(ByteBuffer buffer, int boundary) {
        buffer.position(round(buffer.position(), boundary))
    }

    static void dump(byte[] data) {
        dump(data, System.out)
    }

    static void dump(byte[] data, Appendable out) {
        dump(ByteBuffer.wrap(data), out)
    }

    static void dump(char[] data) {
        dump(data, System.out)
    }

    static void dump(CharSequence data) {
        dump(StandardCharsets.US_ASCII.encode(CharBuffer.wrap(data)), System.out)
    }

    static void dump(char[] data, Appendable out) {
        dump(StandardCharsets.US_ASCII.encode(CharBuffer.wrap(data)), out)
    }

    static void dump(ByteBuffer buf, Appendable out) {
        Formatter fmt = new Formatter(out)
        int pos = buf.position()
        fmt.format('%8x:', pos & ~0xf)
        StringBuilder builder = new StringBuilder()
        for (int i = 0; i < (pos & 0xf); i++) {
            fmt.format('   ')
            builder.append(' ')
        }
        while (buf.hasRemaining()) {
            byte b = buf.get()
            fmt.format(' %2x', b)
            if (0x20 <= b && b < 0x7f) {
                builder.append((char) b)
            } else {
                builder.append(' ')
            }
            if (buf.hasRemaining() && (buf.position() & 0xf) == 0) {
                fmt.format(' %s\n%8x:', builder, buf.position())
                builder.setLength(0)
            }
        }
        fmt.close()
        buf.position(pos)
    }

    /**
     * Encodes the given bytes as a lower case hexadecimal string.
     * @param data the bytes to encode
     * @return two hex digits per input byte
     */
    static String hex(byte[] data) {
        char[] chars = new char[data.length * 2]
        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xff
            chars[i * 2] = HEX_DIGITS[v >>> 4]
            chars[i * 2 + 1] = HEX_DIGITS[v & 0x0f]
        }
        return new String(chars)
    }

    /**
     * Creates the proper stream wrapper to handle the payload section based on the
     * payload compression header tag.
     *
     * @param header the header
     * @param rpmIS raw input stream of the rpm, positioned at the start of the payload
     * @return the decompressing input stream
     * @throws IOException an IO error occurred
     */
    static InputStream openPayloadStream(Header header, InputStream rpmIS) throws IOException {
        Entry<?> pcEntry = header.getEntry(HeaderTag.PAYLOADCOMPRESSOR)
        String[] pc = (String[]) pcEntry.getValues()
        PayloadCompressionType pcType = PayloadCompressionType.valueOf(pc[0])
        switch (pcType) {
            case PayloadCompressionType.none:
                return rpmIS
            case PayloadCompressionType.gzip:
                return new GZIPInputStream(rpmIS)
            case PayloadCompressionType.bzip2:
                try {
                    return new BZip2CompressorInputStream(rpmIS)
                } catch (Exception e) {
                    throw new IOException('Failed to load BZIP2 compression stream', e)
                }
            case PayloadCompressionType.xz:
                try {
                    return new XZCompressorInputStream(rpmIS)
                } catch (Exception e) {
                    throw new IOException('Failed to load XZ compression stream', e)
                }
            default:
                return rpmIS
        }
    }
}
