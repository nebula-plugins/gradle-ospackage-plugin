/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.header

import com.netflix.gradle.plugins.rpm.core.header.AbstractHeader.Tag

import groovy.transform.CompileStatic

/**
 * The RPM signature section, holding digests and signatures of the header and payload.
 */
@CompileStatic
class Signature extends AbstractHeader {

    Signature() {
        for (SignatureTag tag : SignatureTag.values()) tags.put(tag.getCode(), tag)
    }

    protected boolean pad() { return true }

    static enum SignatureTag implements Tag {
        SIGNATURES(62, 7, 'signatures'),
        SIGSIZE(257, 4, 'sigsize'),
        LEGACY_SIGSIZE(1000, 4, 'sigsize'),
        // Version 3 OpenPGP Signature Packet RSA signature of the header and payload areas
        PGP(259, 7, 'pgp'),
        LEGACY_PGP(1002, 7, 'pgp'),
        MD5(261, 7, 'md5'),
        LEGACY_MD5(1004, 7, 'md5'),
        // Version 3 OpenPGP Signature Packet DSA signature of the header and payload areas
        GPG(262, 7, 'gpg'),
        LEGACY_GPG(1005, 7, 'gpg'),
        PAYLOADSIZE(1007, 4, 'payloadsize'),
        RESERVEDSPACE(1008, 4, 'reservedspace'),
        // SHA digest of just the header section
        SHA1HEADER(269, Tag.STRING_ENTRY, 'sha1header'),
        LEGACY_SHA1HEADER(1010, Tag.STRING_ENTRY, 'sha1header'),
        // DSA signature of just the header section, depends on GPG
        DSAHEADER(267, 7, 'dsaheader'),
        LEGACY_DSAHEADER(1011, 7, 'dsaheader'),
        // RSA signature of just the header section, depends on PGP
        RSAHEADER(268, 7, 'rsaheader'),
        SHA256HEADER(273, Tag.STRING_ENTRY, 'sha256header'),
        LEGACY_RSAHEADER(1012, 7, 'rsaheader'),
        FILEDIGESTALGO(5011, Tag.INT32_ENTRY, 'filedigestalgo')

        private final int code
        private final int type
        private final String tagName

        private SignatureTag(final int code, final int type, final String tagName) {
            this.code = code
            this.type = type
            this.tagName = tagName
        }

        int getCode() { return code }

        int getType() { return type }

        String getName() { return tagName }

        boolean isArrayType() {
            return this.type != Tag.NULL_ENTRY
        }
    }
}
