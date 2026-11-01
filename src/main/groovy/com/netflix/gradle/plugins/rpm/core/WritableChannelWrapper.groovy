/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core

import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel

/**
 * Wrapper around a writable channel that allows for observing written data.
 */
@CompileStatic
class WritableChannelWrapper extends ChannelWrapper implements WritableByteChannel {

    protected WritableByteChannel channel

    WritableChannelWrapper(final WritableByteChannel channel) {
        this.channel = channel
    }

    /**
     * Writes data to the wrapped channel, while passing an exact copy to the registered consumers.
     *
     * @param buffer the buffer to write to the wrapped channel
     * @return the number of bytes written
     * @throws IOException if an IO error occurs
     */
    int write(final ByteBuffer buffer) throws IOException {
        for (Consumer<?> consumer : consumers.values()) consumer.consume(buffer.duplicate())
        return channel.write(buffer)
    }

    /**
     * Closes the underlying channel and completes any outstanding operations in the consumers.
     *
     * @throws IOException if an IO error occurs
     */
    void close() throws IOException {
        channel.close()
        super.close()
    }

    /**
     * @return true if the underlying channel is open, false otherwise
     */
    boolean isOpen() {
        return channel.isOpen()
    }
}
