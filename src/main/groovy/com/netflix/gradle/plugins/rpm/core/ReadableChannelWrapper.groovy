/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core

import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

/**
 * Wrapper for observing data read from a NIO channel. This wrapper is used for operations
 * that must be notified of incoming IO data.
 */
@CompileStatic
class ReadableChannelWrapper extends ChannelWrapper implements ReadableByteChannel {

    protected ReadableByteChannel channel

    ReadableChannelWrapper(final ReadableByteChannel channel) {
        this.channel = channel
    }

    /**
     * Reads data from the channel and passes it to the consumers. This method does not mutate
     * the actual data in the provided buffer, but makes its own copy to pass to the consumers.
     *
     * @param buffer the buffer to read into
     * @return the number of bytes read from the underlying channel
     * @throws IOException if an IO error occurs
     */
    int read(final ByteBuffer buffer) throws IOException {
        final int read = channel.read(buffer)
        for (Consumer<?> consumer : consumers.values()) consumer.consume(buffer.duplicate().flip())
        return read
    }

    /**
     * Close the underlying read channel and complete any operations in the consumers.
     *
     * @throws IOException if an IO error occurs
     */
    void close() throws IOException {
        channel.close()
        super.close()
    }

    /**
     * @return true if the underlying channel is open, false if not
     */
    boolean isOpen() {
        return channel.isOpen()
    }
}
