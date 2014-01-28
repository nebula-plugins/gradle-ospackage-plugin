package com.netflix.gradle.plugins.rpm

import groovy.transform.Canonical
import org.freecompany.redline.ReadableChannelWrapper
import org.freecompany.redline.Util
import org.freecompany.redline.header.Format
import org.freecompany.redline.payload.CpioHeader
import org.spockframework.util.Nullable

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.Channels
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream

import static org.freecompany.redline.header.Header.HeaderTag.HEADERIMMUTABLE
import static org.freecompany.redline.header.Signature.SignatureTag.SIGNATURES
import static org.junit.Assert.assertEquals

/**
 * Based on {@link org.freecompany.redline.Scanner}, but modified to return scanned information for
 * programmatic verification.
 */
class Scanner {
    @Canonical
    static class ScannerResult {
        Format format
        List<ScannerFile> files
    }

    @Canonical
    static class ScannerFile {
        @Delegate
        CpioHeader header

        @Nullable
        ByteBuffer contents

        String asString() {
            if (contents == null ) {
                return null
            }
            Charset charset = Charset.forName( "UTF-8");
            CharBuffer buffer = charset.decode(contents);
            return buffer.toString()
        }
    }

    static ScannerResult scan(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file)
        try {
            return scan(fileInputStream)
        } finally {
            fileInputStream.close()
        }
    }

    // TODO Conditionalize the reading of file contenst
    static ScannerResult scan(InputStream inputStream, boolean includeContents = true) {
        ReadableChannelWrapper wrapper = new ReadableChannelWrapper(Channels.newChannel(inputStream))
        Format format = scanHeader(wrapper)

        InputStream uncompressed = new GZIPInputStream(inputStream)
        wrapper = new ReadableChannelWrapper(Channels.newChannel(uncompressed))
        CpioHeader header = null

        def files = []
        int total = 0
        while (header == null || !header.isLast()) {
            header = new CpioHeader()
            total = header.read(wrapper, total)
            final int fileSize = header.getFileSize()
            boolean includingContents = includeContents&&header.type==8
            if (!header.isLast()) {
                ByteBuffer descriptor = includingContents?Util.fill(wrapper, fileSize):null
                files += new ScannerFile(header, descriptor)
            }

            if(!includingContents) {
                assertEquals(fileSize, uncompressed.skip(fileSize))
            }
            total += fileSize
        }

        return new ScannerResult(format,files)
    }

    static Format scanHeader(ReadableChannelWrapper wrapper) throws Exception {
        Format format = new Format()
        format.getLead().read(wrapper)

        int count = format.signature.read(wrapper)
        int expected = ByteBuffer.wrap(format.signature.getEntry(SIGNATURES).values, 8, 4).getInt() / -16
        assertEquals(expected, count)

        count = format.getHeader().read(wrapper)
        expected = ByteBuffer.wrap(format.getHeader().getEntry(HEADERIMMUTABLE).values, 8, 4).getInt() / -16
        assertEquals(expected, count)

        return format
    }


    def static getHeaderEntry(ScannerResult scan, tag) {
        def header = scan.format.header
        header.getEntry(tag.code)
    }

    def static getHeaderEntryString(ScannerResult scan, tag) {
        getHeaderEntry(scan, tag).values.join('')
    }
}
