/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core

import com.netflix.gradle.plugins.rpm.core.ChannelWrapper.Key
import com.netflix.gradle.plugins.rpm.core.header.AbstractHeader.Entry
import com.netflix.gradle.plugins.rpm.core.header.Signature
import com.netflix.gradle.plugins.rpm.core.header.Signature.SignatureTag

import groovy.transform.CompileStatic

import java.util.logging.Logger

import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPPrivateKey
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider

/**
 * Generates PGP signatures for NIO channels: an RSA signature of the header only, and a PGP
 * signature of the header and payload.
 */
@CompileStatic
class SignatureGenerator {

    protected static final int SIGNATURE_SIZE = 287

    private static final Logger logger = Logger.getLogger(SignatureGenerator.name)

    protected final boolean enabled
    protected Entry<byte[]> headerOnlyRSAEntry
    protected Entry<byte[]> headerAndPayloadPGPEntry
    protected final PGPPrivateKey privateKey
    protected Key<byte[]> headerOnlyKey = null
    protected Key<byte[]> headerAndPayloadKey = null

    SignatureGenerator(PGPPrivateKey privateKey) {
        this.privateKey = privateKey
        this.enabled = privateKey != null
    }

    SignatureGenerator(File privateKeyRingFile, String privateKeyId, String privateKeyPassphrase) {
        PGPPrivateKey key = null
        if (privateKeyRingFile != null) {
            PGPSecretKeyRingCollection keyRings = readKeyRings(privateKeyRingFile)
            PGPSecretKey secretKey = findMatchingSecretKey(keyRings, privateKeyId)
            try {
                key = extractPrivateKey(secretKey, privateKeyPassphrase)
            } catch (IllegalArgumentException e) {
                logger.warning('Private Key could not be extracted and therefore a signature will not be generated! ' + e.getLocalizedMessage())
            }
        }
        this.privateKey = key
        this.enabled = key != null
    }

    @SuppressWarnings('unchecked')
    void prepare(Signature signature) {
        if (enabled) {
            headerOnlyRSAEntry = (Entry<byte[]>) signature.addEntry(SignatureTag.RSAHEADER, SIGNATURE_SIZE)
            headerAndPayloadPGPEntry = (Entry<byte[]>) signature.addEntry(SignatureTag.LEGACY_PGP, SIGNATURE_SIZE)
        }
    }

    void startBeforeHeader(WritableChannelWrapper output) {
        if (enabled) {
            headerOnlyKey = output.start(privateKey, getAlgorithm())
            headerAndPayloadKey = output.start(privateKey, getAlgorithm())
        }
    }

    void finishAfterHeader(WritableChannelWrapper output) {
        finishEntry(output, headerOnlyRSAEntry, headerOnlyKey)
    }

    void finishAfterPayload(WritableChannelWrapper output) {
        finishEntry(output, headerAndPayloadPGPEntry, headerAndPayloadKey)
    }

    protected PGPSecretKeyRingCollection readKeyRings(File privateKeyRingFile) {
        try {
            InputStream keyInputStream = new BufferedInputStream(new FileInputStream(privateKeyRingFile))
            InputStream decoderStream = PGPUtil.getDecoderStream(keyInputStream)
            try {
                return new PGPSecretKeyRingCollection(decoderStream, new BcKeyFingerprintCalculator())
            } finally {
                decoderStream.close()
            }
        } catch (IOException e) {
            throw new IllegalArgumentException('Could not read key ring file: ' + privateKeyRingFile, e)
        } catch (PGPException e) {
            throw new IllegalArgumentException('Could not extract key rings from: ' + privateKeyRingFile, e)
        }
    }

    protected PGPSecretKey findMatchingSecretKey(PGPSecretKeyRingCollection keyRings, String privateKeyId) {
        String keyId = privateKeyId != null ? privateKeyId.toLowerCase() : null
        Iterator<PGPSecretKeyRing> iter = keyRings.getKeyRings()
        while (iter.hasNext()) {
            PGPSecretKeyRing keyRing = iter.next()
            Iterator<PGPSecretKey> keyIter = keyRing.getSecretKeys()
            while (keyIter.hasNext()) {
                PGPSecretKey key = keyIter.next()
                if (key.isSigningKey() && isMatchingKeyId(key, keyId)) {
                    return key
                }
            }
        }
        throw new IllegalArgumentException("Can't find signing key in key rings.")
    }

    protected boolean isMatchingKeyId(PGPSecretKey key, String privateKeyId) {
        if (privateKeyId == null) {
            return true
        }
        return Long.toHexString(key.getKeyID()).endsWith(privateKeyId)
    }

    protected PGPPrivateKey extractPrivateKey(PGPSecretKey secretKey, String privateKeyPassphrase) {
        BcPBESecretKeyDecryptorBuilder secretKeyDecryptorBuilder = new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider())
        try {
            PBESecretKeyDecryptor secretKeyDecryptor = secretKeyDecryptorBuilder.build(privateKeyPassphrase.toCharArray())
            return secretKey.extractPrivateKey(secretKeyDecryptor)
        } catch (Exception e) {
            throw new IllegalArgumentException('Could not extract private key from key ring', e)
        }
    }

    protected void finishEntry(WritableChannelWrapper output, Entry<byte[]> entry, Key<byte[]> key) {
        if (enabled) {
            checkKey(key)
            checkEntry(entry)
            entry.setValues(output.finish(key))
        }
    }

    protected void checkEntry(Entry<byte[]> entry) {
        if (entry == null) {
            throw new IllegalStateException('Entry not initialized. Please call prepare() first')
        }
    }

    protected void checkKey(Key<byte[]> key) {
        if (key == null) {
            throw new IllegalStateException('Key is not initialized. Please call startBeforeHeader() first.')
        }
    }

    boolean isEnabled() {
        return enabled
    }

    protected int getAlgorithm() {
        return privateKey != null ? privateKey.getPublicKeyPacket().getAlgorithm() : 0
    }
}
