/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.header

import com.netflix.gradle.plugins.rpm.core.header.AbstractHeader.Tag

import groovy.transform.CompileStatic

/**
 * The RPM header section, holding the package metadata.
 */
@CompileStatic
class Header extends AbstractHeader {

    Header() {
        for (HeaderTag tag : HeaderTag.values()) tags.put(tag.getCode(), tag)
    }

    protected boolean pad() { return false }

    static enum HeaderTag implements Tag {
        NAME(1000, Tag.STRING_ENTRY, 'name'),
        VERSION(1001, Tag.STRING_ENTRY, 'version'),
        RELEASE(1002, Tag.STRING_ENTRY, 'release'),
        EPOCH(1003, Tag.INT32_ENTRY, 'epoch'),
        SUMMARY(1004, Tag.I18NSTRING_ENTRY, 'summary'),
        DESCRIPTION(1005, Tag.I18NSTRING_ENTRY, 'description'),
        BUILDTIME(1006, Tag.INT32_ENTRY, 'buildtime'),
        BUILDHOST(1007, Tag.STRING_ENTRY, 'buildhost'),
        SIZE(1009, Tag.INT32_ENTRY, 'size'),
        DISTRIBUTION(1010, Tag.STRING_ENTRY, 'distribution'),
        VENDOR(1011, Tag.STRING_ENTRY, 'vendor'),
        LICENSE(1014, Tag.STRING_ENTRY, 'license'),
        PACKAGER(1015, Tag.STRING_ENTRY, 'packager'),
        GROUP(1016, Tag.I18NSTRING_ENTRY, 'group'),
        CHANGELOG(1017, Tag.STRING_ARRAY_ENTRY, 'changelog'),
        URL(1020, Tag.STRING_ENTRY, 'url'),
        OS(1021, Tag.STRING_ENTRY, 'os'),
        ARCH(1022, Tag.STRING_ENTRY, 'arch'),
        SOURCERPM(1044, Tag.STRING_ENTRY, 'sourcerpm'),
        FILEVERIFYFLAGS(1045, Tag.INT32_ENTRY, 'fileverifyflags'),
        ARCHIVESIZE(1046, Tag.INT32_ENTRY, 'archivesize'),
        RPMVERSION(1064, Tag.STRING_ENTRY, 'rpmversion'),
        CHANGELOGTIME(1080, Tag.INT32_ENTRY, 'changelogtime'),
        CHANGELOGNAME(1081, Tag.STRING_ARRAY_ENTRY, 'changelogname'),
        CHANGELOGTEXT(1082, Tag.STRING_ARRAY_ENTRY, 'changelogtext'),
        COOKIE(1094, Tag.STRING_ENTRY, 'cookie'),
        OPTFLAGS(1122, Tag.STRING_ENTRY, 'optflags'),
        PAYLOADFORMAT(1124, Tag.STRING_ENTRY, 'payloadformat'),
        PAYLOADCOMPRESSOR(1125, Tag.STRING_ENTRY, 'payloadcompressor'),
        PAYLOADFLAGS(1126, Tag.STRING_ENTRY, 'payloadflags'),
        RHNPLATFORM(1131, Tag.STRING_ENTRY, 'rhnplatform'),
        PLATFORM(1132, Tag.STRING_ENTRY, 'platform'),
        FILECOLORS(1140, Tag.INT32_ENTRY, 'filecolors'),
        FILECLASS(1141, Tag.INT32_ENTRY, 'fileclass'),
        CLASSDICT(1142, Tag.STRING_ARRAY_ENTRY, 'classdict'),
        FILEDEPENDSX(1143, Tag.INT32_ENTRY, 'filedependsx'),
        FILEDEPENDSN(1144, Tag.INT32_ENTRY, 'filedependsn'),
        DEPENDSDICT(1145, Tag.INT32_ENTRY, 'dependsdict'),
        SOURCEPKGID(1146, Tag.BIN_ENTRY, 'sourcepkgid'),
        FILECONTEXTS(1147, Tag.STRING_ARRAY_ENTRY, 'filecontexts'),
        HEADERIMMUTABLE(63, Tag.BIN_ENTRY, 'headerimmutable'),
        HEADERI18NTABLE(100, Tag.STRING_ARRAY_ENTRY, 'headeri18ntable'),
        PREINSCRIPT(1023, Tag.STRING_ENTRY, 'prein'),
        POSTINSCRIPT(1024, Tag.STRING_ENTRY, 'postin'),
        PREUNSCRIPT(1025, Tag.STRING_ENTRY, 'preun'),
        POSTUNSCRIPT(1026, Tag.STRING_ENTRY, 'postun'),
        PREINPROG(1085, Tag.STRING_ENTRY, 'preinprog'),
        POSTINPROG(1086, Tag.STRING_ENTRY, 'postinprog'),
        PREUNPROG(1087, Tag.STRING_ENTRY, 'preunprog'),
        POSTUNPROG(1088, Tag.STRING_ENTRY, 'postunprog'),
        PRETRANSSCRIPT(1151, Tag.STRING_ENTRY, 'pretrans'),
        POSTTRANSSCRIPT(1152, Tag.STRING_ENTRY, 'posttrans'),
        PRETRANSPROG(1153, Tag.STRING_ENTRY, 'pretransprog'),
        POSTTRANSPROG(1154, Tag.STRING_ENTRY, 'pretransprog'),
        TRIGGERSCRIPTS(1065, Tag.STRING_ARRAY_ENTRY, 'triggerscripts'),
        TRIGGERNAME(1066, Tag.STRING_ARRAY_ENTRY, 'triggername'),
        TRIGGERVERSION(1067, Tag.STRING_ARRAY_ENTRY, 'triggerversion'),
        TRIGGERFLAGS(1068, Tag.INT32_ENTRY, 'triggerflags'),
        TRIGGERINDEX(1069, Tag.INT32_ENTRY, 'triggerindex'),
        TRIGGERSCRIPTPROG(1092, Tag.STRING_ARRAY_ENTRY, 'triggerscriptprog'),
        OLDFILENAMES(1027, Tag.STRING_ARRAY_ENTRY, 'oldfilenames'),
        FILESIZES(1028, Tag.INT32_ENTRY, 'filesizes'),
        FILEMODES(1030, Tag.INT16_ENTRY, 'filemodes'),
        FILERDEVS(1033, Tag.INT16_ENTRY, 'filerdevs'),
        FILEMTIMES(1034, Tag.INT32_ENTRY, 'filemtimes'),
        FILEDIGESTS(1035, Tag.STRING_ARRAY_ENTRY, 'filedigests'),
        FILELINKTOS(1036, Tag.STRING_ARRAY_ENTRY, 'filelinktos'),
        FILEFLAGS(1037, Tag.INT32_ENTRY, 'fileflags'),
        FILEUSERNAME(1039, Tag.STRING_ARRAY_ENTRY, 'fileusername'),
        FILEGROUPNAME(1040, Tag.STRING_ARRAY_ENTRY, 'filegroupname'),
        FILEDEVICES(1095, Tag.INT32_ENTRY, 'filedevices'),
        FILEINODES(1096, Tag.INT32_ENTRY, 'fileinodes'),
        FILELANGS(1097, Tag.STRING_ARRAY_ENTRY, 'filelangs'),
        PREFIXES(1098, Tag.STRING_ARRAY_ENTRY, 'prefixes'),
        DIRINDEXES(1116, Tag.INT32_ENTRY, 'dirindexes'),
        BASENAMES(1117, Tag.STRING_ARRAY_ENTRY, 'basenames'),
        DIRNAMES(1118, Tag.STRING_ARRAY_ENTRY, 'dirnames'),
        PROVIDENAME(1047, Tag.STRING_ARRAY_ENTRY, 'providename'),
        REQUIREFLAGS(1048, Tag.INT32_ENTRY, 'requireflags'),
        REQUIRENAME(1049, Tag.STRING_ARRAY_ENTRY, 'requirename'),
        REQUIREVERSION(1050, Tag.STRING_ARRAY_ENTRY, 'requireversion'),
        CONFLICTFLAGS(1053, Tag.INT32_ENTRY, 'conflictflags'),
        CONFLICTNAME(1054, Tag.STRING_ARRAY_ENTRY, 'conflictname'),
        CONFLICTVERSION(1055, Tag.STRING_ARRAY_ENTRY, 'conflictversion'),
        OBSOLETENAME(1090, Tag.STRING_ARRAY_ENTRY, 'obsoletename'),
        PROVIDEFLAGS(1112, Tag.INT32_ENTRY, 'provideflags'),
        PROVIDEVERSION(1113, Tag.STRING_ARRAY_ENTRY, 'provideversion'),
        OBSOLETEFLAGS(1114, Tag.INT32_ENTRY, 'obsoleteflags'),
        OBSOLETEVERSION(1115, Tag.STRING_ARRAY_ENTRY, 'obsoleteversion'),
        DISTURL(1123, Tag.STRING_ENTRY, 'disturl'),
        DISTTAG(1155, Tag.STRING_ENTRY, 'disttag'),
        BUGURL(5012, Tag.STRING_ENTRY, 'bugurl'),
        ENCODING(5062, Tag.STRING_ENTRY, 'encoding'),
        PAYLOADDIGEST(5092, Tag.STRING_ARRAY_ENTRY, 'payloaddigest'),
        PAYLOADDIGESTALGO(5093, Tag.INT32_ENTRY, 'payloaddigestalgo'),
        PAYLOADDIGESTALT(5097, Tag.STRING_ARRAY_ENTRY, 'payloaddigestalt')

        private final int code
        private final int type
        private final String tagName

        private HeaderTag(final int code, final int type, final String tagName) {
            this.code = code
            this.type = type
            this.tagName = tagName
        }

        int getCode() { return code }

        int getType() { return type }

        String getName() { return tagName }
    }
}
