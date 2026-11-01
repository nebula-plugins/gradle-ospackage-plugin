/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core

import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.Signature
import java.security.SignatureException

import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPPrivateKey
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureGenerator
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder

/**
 * Wraps an IO channel so that bytes may be observed during transmission. Wrappers around IO
 * channels are used for a variety of purposes, including counting byte output for use in
 * generating headers, calculating a signature across output bytes, and digesting output
 * bytes using a one-way secure hash.
 */
@CompileStatic
abstract class ChannelWrapper {

    /** Handle returned by the {@code start} methods and passed back to {@link #finish}. */
    static class Key<T> {}

    /**
     * Interface describing an object that consumes data from a NIO buffer.
     */
    protected static interface Consumer<T> {
        /**
         * Consume some input from the given buffer.
         * @param buffer the buffer to consume
         */
        void consume(ByteBuffer buffer)

        /**
         * Complete operations and optionally return a value to the holder of the key.
         * @return reference to the object
         */
        T finish()
    }

    protected Map<Key<?>, Consumer<?>> consumers = new HashMap<Key<?>, Consumer<?>>()

    /**
     * Initializes a byte counter on this channel that also copies every observed buffer to
     * the given output channel.
     * @param output channel that receives a copy of the observed data
     * @return reference to the new key added to the consumers
     */
    Key<Integer> start(final WritableByteChannel output) {
        final Key<Integer> object = new Key<Integer>()
        consumers.put(object, new CopyingCounter(output))
        return object
    }

    /**
     * Initializes a byte counter on this channel.
     * @return reference to the new key added to the consumers
     */
    Key<Integer> start() {
        final Key<Integer> object = new Key<Integer>()
        consumers.put(object, new Counter())
        return object
    }

    /**
     * Initialize a signature on this channel.
     *
     * @param key the private key to use in signing this data stream.
     * @return reference to the new key added to the consumers
     * @throws NoSuchAlgorithmException if the key algorithm is not supported
     * @throws InvalidKeyException if the key provided is invalid for signing
     */
    Key<byte[]> start(final PrivateKey key) throws NoSuchAlgorithmException, InvalidKeyException {
        final Signature signature = Signature.getInstance(key.getAlgorithm())
        signature.initSign(key)
        final Key<byte[]> object = new Key<byte[]>()
        consumers.put(object, new JcaSigner(signature))
        return object
    }

    /**
     * Initialize a PGP signature on the channel.
     *
     * @param key the private key to use in signing this data stream.
     * @param algorithm the algorithm to use. Can be extracted from public key.
     * @return reference to the new key added to the consumers
     */
    Key<byte[]> start(final PGPPrivateKey key, int algorithm) {
        BcPGPContentSignerBuilder contentSignerBuilder = new BcPGPContentSignerBuilder(algorithm, HashAlgorithmTags.SHA1)
        final PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder)
        try {
            signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, key)
        } catch (PGPException e) {
            throw new RuntimeException('Could not initialize PGP signature generator', e)
        }
        final Key<byte[]> object = new Key<byte[]>()
        consumers.put(object, new PgpSigner(signatureGenerator))
        return object
    }

    /**
     * Initialize a digest on this channel.
     *
     * @param algorithm the digest algorithm to use in computing the hash
     * @return reference to the new key added to the consumers
     * @throws NoSuchAlgorithmException if the given algorithm does not exist
     */
    Key<byte[]> start(final String algorithm) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance(algorithm)
        final Key<byte[]> object = new Key<byte[]>()
        consumers.put(object, new Digester(digest))
        return object
    }

    @SuppressWarnings('unchecked')
    public <T> T finish(final Key<T> object) {
        return (T) consumers.remove(object).finish()
    }

    void close() throws IOException {
        if (!consumers.isEmpty()) throw new IOException("There are '" + consumers.size() + "' unfinished operations.")
    }

    private static class Counter implements Consumer<Integer> {
        private int count

        void consume(final ByteBuffer buffer) { count += buffer.remaining() }

        Integer finish() { return count }
    }

    private static class CopyingCounter implements Consumer<Integer> {
        private final WritableByteChannel output
        private int count

        CopyingCounter(WritableByteChannel output) {
            this.output = output
        }

        void consume(final ByteBuffer buffer) {
            try {
                count += output.write(buffer)
            } catch (IOException e) {
                throw new RuntimeException(e)
            }
        }

        Integer finish() { return count }
    }

    private static class JcaSigner implements Consumer<byte[]> {
        private final Signature signature

        JcaSigner(Signature signature) {
            this.signature = signature
        }

        void consume(final ByteBuffer buffer) {
            try {
                signature.update(buffer)
            } catch (Exception e) {
                throw new RuntimeException(e)
            }
        }

        byte[] finish() {
            try {
                return signature.sign()
            } catch (Exception e) {
                throw new RuntimeException(e)
            }
        }
    }

    private static class PgpSigner implements Consumer<byte[]> {
        private final PGPSignatureGenerator signatureGenerator

        PgpSigner(PGPSignatureGenerator signatureGenerator) {
            this.signatureGenerator = signatureGenerator
        }

        void consume(final ByteBuffer buffer) {
            if (!buffer.hasRemaining()) {
                return
            }
            try {
                write(buffer)
            } catch (SignatureException e) {
                throw new RuntimeException('Could not write buffer to PGP signature generator.', e)
            }
        }

        private void write(final ByteBuffer buffer) throws SignatureException {
            if (buffer.hasArray()) {
                byte[] bufferBytes = buffer.array()
                int offset = buffer.arrayOffset()
                int position = buffer.position()
                int limit = buffer.limit()
                signatureGenerator.update(bufferBytes, offset + position, limit - position)
                buffer.position(limit)
            } else {
                int length = buffer.remaining()
                byte[] bytes = new byte[Util.getTempArraySize(length)]
                while (length > 0) {
                    int chunk = Math.min(length, bytes.length)
                    buffer.get(bytes, 0, chunk)
                    signatureGenerator.update(bytes, 0, chunk)
                    length -= chunk
                }
            }
        }

        byte[] finish() {
            try {
                return signatureGenerator.generate().getEncoded()
            } catch (Exception e) {
                throw new RuntimeException('Could not generate signature.', e)
            }
        }
    }

    private static class Digester implements Consumer<byte[]> {
        private final MessageDigest digest

        Digester(MessageDigest digest) {
            this.digest = digest
        }

        void consume(final ByteBuffer buffer) {
            try {
                digest.update(buffer)
            } catch (Exception e) {
                throw new RuntimeException(e)
            }
        }

        byte[] finish() {
            try {
                return digest.digest()
            } catch (Exception e) {
                throw new RuntimeException(e)
            }
        }
    }
}
