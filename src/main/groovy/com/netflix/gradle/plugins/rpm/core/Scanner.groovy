/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core

import com.netflix.gradle.plugins.rpm.core.ChannelWrapper.Key
import com.netflix.gradle.plugins.rpm.core.header.AbstractHeader.Entry
import com.netflix.gradle.plugins.rpm.core.header.Format
import com.netflix.gradle.plugins.rpm.core.header.Header
import com.netflix.gradle.plugins.rpm.core.header.Header.HeaderTag
import com.netflix.gradle.plugins.rpm.core.header.Signature.SignatureTag
import com.netflix.gradle.plugins.rpm.core.payload.CpioHeader

import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.channels.Channels

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Reads the lead, signature and header of an RPM file, and can dump its payload entries.
 */
@CompileStatic
class Scanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scanner)

    private final PrintStream output

    Scanner() {
        this(null)
    }

    Scanner(final PrintStream out) {
        this.output = out
    }

    /**
     * Prints the metadata and payload headers of the RPM file given as the first argument.
     * @param args the command line arguments
     */
    static void main(String[] args) throws Exception {
        InputStream fios = new FileInputStream(args[0])
        ReadableChannelWrapper input = new ReadableChannelWrapper(Channels.newChannel(fios))
        Scanner scanner = new Scanner(System.out)
        Format format = scanner.run(input)
        scanner.log(format.toString())
        Header rpmHeader = format.getHeader()
        scanner.log('Payload compression: ' + rpmHeader.getEntry(HeaderTag.PAYLOADCOMPRESSOR))
        InputStream uncompressed = Util.openPayloadStream(rpmHeader, fios)
        input = new ReadableChannelWrapper(Channels.newChannel(uncompressed))
        CpioHeader header
        int total = 0
        while (true) {
            header = new CpioHeader()
            total = header.read(input, total)
            scanner.log(header.toString())
            final int skip = header.getFileSize()
            if (uncompressed.skip(skip) != skip) throw new RuntimeException('Skip failed.')
            total += header.getFileSize()
            if (header.isLast()) break
        }
    }

    /**
     * Reads the lead, signature and header sections from the channel.
     * @param input the channel positioned at the start of the RPM
     * @return the parsed sections
     * @throws IOException if the RPM cannot be read
     */
    Format run(ReadableChannelWrapper input) throws IOException {
        Format format = new Format()

        Key<Integer> headerStartKey = input.start()

        Key<Integer> lead = input.start()
        format.getLead().read(input)
        log("Lead ended at '" + input.finish(lead) + "'.")

        Key<Integer> signature = input.start()
        int count = format.getSignature().read(input)
        Entry<?> sigEntry = format.getSignature().getEntry(SignatureTag.SIGNATURES)
        int expected = sigEntry == null ? 0 : expectedCount(sigEntry)
        log("Signature ended at '" + input.finish(signature) + "' and contained '" + count + "' headers (expected '" + expected + "').")

        Integer headerStartPos = input.finish(headerStartKey)
        format.getHeader().setStartPos(headerStartPos)
        Key<Integer> headerKey = input.start()
        count = format.getHeader().read(input)
        Entry<?> immutableEntry = format.getHeader().getEntry(HeaderTag.HEADERIMMUTABLE)
        expected = immutableEntry == null ? 0 : expectedCount(immutableEntry)
        Integer headerLength = input.finish(headerKey)
        format.getHeader().setEndPos(headerStartPos + headerLength)
        log("Header ended at '" + headerLength + " and contained '" + count + "' headers (expected '" + expected + "').")

        return format
    }

    private static int expectedCount(Entry<?> regionEntry) {
        return ByteBuffer.wrap((byte[]) regionEntry.getValues(), 8, 4).getInt().intdiv(-16).intValue()
    }

    private void log(final String text) {
        if (output == null) {
            LOGGER.info(text)
        } else {
            output.println(text)
        }
    }
}
