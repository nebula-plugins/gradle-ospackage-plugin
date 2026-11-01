package com.netflix.gradle.plugins.rpm.core

import com.netflix.gradle.plugins.rpm.core.header.Format
import com.netflix.gradle.plugins.rpm.core.header.Header.HeaderTag
import com.netflix.gradle.plugins.rpm.core.header.Signature.SignatureTag
import com.netflix.gradle.plugins.rpm.core.payload.Directive

import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureList
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider

import spock.lang.Specification
import spock.lang.TempDir

import static com.netflix.gradle.plugins.rpm.core.RpmTestFixtures.newBuilder
import static com.netflix.gradle.plugins.rpm.core.RpmTestFixtures.payload
import static com.netflix.gradle.plugins.rpm.core.RpmTestFixtures.resource
import static com.netflix.gradle.plugins.rpm.core.RpmTestFixtures.scan
import static com.netflix.gradle.plugins.rpm.core.header.Flags.EQUAL
import static com.netflix.gradle.plugins.rpm.core.header.Flags.GREATER
import static com.netflix.gradle.plugins.rpm.core.header.Flags.LESS

class BuilderSpec extends Specification {

    @TempDir
    File tmp

    def 'truncates long package names in the lead'() {
        given:
        String name = 'x' * 74 + 'a'
        Builder builder = newBuilder(name)

        when:
        Format format = scan(new File(tmp, builder.build(tmp)))

        then:
        format.lead.name == 'x' * 66
    }

    def 'writes the rpm and header magic numbers'() {
        when:
        byte[] bytes = new File(tmp, newBuilder('magic').build(tmp)).bytes

        then:
        bytes[0..3] == [0xED, 0xAB, 0xEE, 0xDB].collect { it as byte }
        bytes[96..98] == [0x8E, 0xAD, 0xE8].collect { it as byte }
    }

    def 'records file owner, group and directive flags'() {
        given:
        Builder builder = newBuilder('filestest')
        int flags = Directive.RPMFILE_CONFIG | Directive.RPMFILE_DOC | Directive.RPMFILE_NOREPLACE
        builder.addFile('/etc', resource('prein.sh'), 0755, 0755, new Directive(flags), 'jabberwocky', 'vorpal')

        when:
        Format format = scan(new File(tmp, builder.build(tmp)))

        then:
        format.header.getEntry(HeaderTag.FILEUSERNAME).values == ['jabberwocky'] as String[]
        format.header.getEntry(HeaderTag.FILEGROUPNAME).values == ['vorpal'] as String[]
        format.header.getEntry(HeaderTag.FILEFLAGS).values == [flags] as int[]
    }

    def 'builds an rpm without a signature'() {
        when:
        Format format = scan(new File(tmp, newBuilder('test').build(tmp)))

        then:
        format.signature.getEntry(SignatureTag.RSAHEADER) == null
        format.signature.getEntry(SignatureTag.LEGACY_PGP) == null
        format.header.getEntry(HeaderTag.NAME).values == ['test'] as String[]
    }

    def 'signs the header and the header plus payload'() {
        given:
        Builder builder = newBuilder('signing-test')
        builder.addFile('/opt/signed/prein.sh', resource('prein.sh'))
        builder.setPrivateKeyRingFile(resource('pgp/secring.gpg'))
        builder.setPrivateKeyPassphrase('redline')
        File rpm = new File(tmp, builder.build(tmp))
        byte[] bytes = rpm.bytes
        Format format = scan(rpm)
        int headerStart = format.header.startPos

        expect:
        verifies(format, SignatureTag.RSAHEADER, bytes, headerStart, format.header.endPos)
        verifies(format, SignatureTag.LEGACY_PGP, bytes, headerStart, bytes.length)
    }

    def 'stores the epoch'() {
        given:
        Builder builder = newBuilder('testEpoch')
        builder.setPackage('testEpoch', '1.0', '1', 1)

        when:
        Format format = scan(new File(tmp, builder.build(tmp)))

        then:
        format.header.getEntry(HeaderTag.EPOCH).values == [1] as int[]
        format.header.getEntry(HeaderTag.PROVIDEVERSION).values == ['1:1.0-1'] as String[]
    }

    def 'builds a metapackage with only dependencies'() {
        given:
        Builder builder = newBuilder('testMetapkg')
        builder.addDependencyMore('glibc', '2.17')

        when:
        Format format = scan(new File(tmp, builder.build(tmp)))
        String[] requires = format.header.getEntry(HeaderTag.REQUIRENAME).values

        then:
        requires.last() == 'glibc'
        (format.header.getEntry(HeaderTag.REQUIREFLAGS).values as List).last() == (GREATER | EQUAL)
        format.header.getEntry(HeaderTag.BASENAMES) == null
    }

    def 'writes requires, provides, conflicts and obsoletes'() {
        given:
        Builder builder = newBuilder('testCapabilities')
        builder.addDependency('httpd', 0, '')
        builder.addProvides('frobnicator', '')
        builder.addProvides('barnacle', '3.89')
        builder.addConflicts('fooberry', GREATER | EQUAL, '1a')
        builder.addObsoletes('testCappypkg', 0, '')

        when:
        Format format = scan(new File(tmp, builder.build(tmp)))
        def values = { HeaderTag tag -> format.header.getEntry(tag).values as List }

        then:
        values(HeaderTag.REQUIRENAME).last() == 'httpd'
        values(HeaderTag.REQUIREFLAGS).last() == 0
        values(HeaderTag.REQUIREVERSION).last() == ''

        values(HeaderTag.PROVIDENAME) == ['testCapabilities', 'frobnicator', 'barnacle']
        values(HeaderTag.PROVIDEFLAGS) == [EQUAL, 0, EQUAL]
        values(HeaderTag.PROVIDEVERSION) == ['0:1.0-1', '', '3.89']

        values(HeaderTag.CONFLICTNAME) == ['fooberry']
        values(HeaderTag.CONFLICTFLAGS) == [GREATER | EQUAL]
        values(HeaderTag.CONFLICTVERSION) == ['1a']

        values(HeaderTag.OBSOLETENAME) == ['testCappypkg']
        values(HeaderTag.OBSOLETEFLAGS) == [0]
        values(HeaderTag.OBSOLETEVERSION) == ['']
    }

    def 'keeps several requirements on the same package'() {
        given:
        Builder builder = newBuilder('testMultipleCapabilities')
        builder.addDependency('httpd', GREATER | EQUAL, '1.0')
        builder.addDependency('httpd', LESS, '2.0')

        when:
        Format format = scan(new File(tmp, builder.build(tmp)))
        def lastTwo = { HeaderTag tag -> (format.header.getEntry(tag).values as List).takeRight(2) }

        then:
        lastTwo(HeaderTag.REQUIRENAME) == ['httpd', 'httpd']
        lastTwo(HeaderTag.REQUIREFLAGS) == [GREATER | EQUAL, LESS]
        lastTwo(HeaderTag.REQUIREVERSION) == ['1.0', '2.0']
    }

    def 'an explicit provide replaces the implicit one for the package itself'() {
        given:
        Builder builder = newBuilder('testProvideOverride')
        builder.addProvides('testProvideOverride', '1.0')

        when:
        Format format = scan(new File(tmp, builder.build(tmp)))

        then:
        format.header.getEntry(HeaderTag.PROVIDENAME).values == ['testProvideOverride'] as String[]
        format.header.getEntry(HeaderTag.PROVIDEFLAGS).values == [EQUAL] as int[]
        format.header.getEntry(HeaderTag.PROVIDEVERSION).values == ['1.0'] as String[]
    }

    def 'accepts a header value of the type the tag expects'() {
        given:
        Builder builder = new Builder()

        when:
        builder.addHeaderEntry(HeaderTag.CHANGELOGTIME, 1)

        then:
        noExceptionThrown()
    }

    def 'rejects a header value of the wrong type for #tag'() {
        given:
        Builder builder = new Builder()

        when:
        builder.addHeaderEntry(tag, value)

        then:
        thrown(ClassCastException)

        where:
        tag                     | value
        HeaderTag.CHANGELOGNAME | 1
        HeaderTag.CHANGELOGTIME | 'Mon Jan 01 2016'
        HeaderTag.CHANGELOGTIME | 1L
        HeaderTag.CHANGELOGTIME | (short) 1
        HeaderTag.CHANGELOGTIME | (char) 'c'
    }

    def 'rejects #field containing an illegal character'() {
        when:
        new Builder().setPackage(name, version, release)

        then:
        IllegalArgumentException e = thrown()
        e.message.startsWith(field + ' with value')

        where:
        field     | name   | version | release
        'name'    | 'a b'  | '1'     | '1'
        'name'    | 'a/b'  | '1'     | '1'
        'version' | 'a'    | '1-2'   | '1'
        'release' | 'a'    | '1'     | '1/2'
    }

    def 'writes the payload and the per-file headers'() {
        given:
        File big = new File(tmp, 'big.bin')
        Random random = new Random(42)
        byte[] content = new byte[100_003]
        random.nextBytes(content)
        big.bytes = content
        File ghostSource = new File(tmp, 'ghost.log')
        ghostSource.text = 'never packaged'

        Builder builder = newBuilder('payload')
        builder.addFile('/opt/app/lib/big.bin', big)
        builder.addFile('/opt/app/bin/run.sh', resource('prein.sh'), 0755)
        builder.addFile('/opt/app/log/app.log', ghostSource, 0600, -1, Directive.GHOST, null, null)
        builder.addDirectory('/var/lib/app', 0700, null, 'app', 'app')
        builder.addLink('/usr/bin/run', '/opt/app/bin/run.sh')

        when:
        File rpm = new File(tmp, builder.build(tmp))
        Format format = scan(rpm)
        Map<String, byte[]> files = payload(rpm)
        Map<String, Integer> modes = fileModes(format)

        then:
        files['./opt/app/lib/big.bin'] == content
        files['./opt/app/bin/run.sh'] == resource('prein.sh').bytes
        new String(files['./usr/bin/run']) == '/opt/app/bin/run.sh'
        !files.containsKey('./opt/app/log/app.log')
        files.containsKey('./var/lib/app')

        and: 'modes combine the file type with the permissions'
        modes['/opt/app/lib/big.bin'] == 0100644
        modes['/opt/app/bin/run.sh'] == 0100755
        modes['/opt/app'] == 040755
        modes['/var/lib/app'] == 040700
        modes['/usr/bin/run'] == 0120644

        and: 'files are digested with sha256'
        format.header.getEntry(HeaderTag.FILEDIGESTS).values.contains(content.digest('SHA-256'))
        format.header.getEntry(5011).values == [8] as int[]
        format.header.getEntry(HeaderTag.PAYLOADDIGESTALGO).values == [8] as int[]
    }

    def 'stores the build time in seconds'() {
        given:
        long before = System.currentTimeMillis().intdiv(1000)

        when:
        Format format = scan(new File(tmp, newBuilder('buildtime').build(tmp)))
        int buildTime = (format.header.getEntry(HeaderTag.BUILDTIME).values as int[])[0]

        then:
        buildTime >= before
        buildTime <= System.currentTimeMillis().intdiv(1000)
    }

    def 'adds changelog entries from a file'() {
        given:
        Builder builder = newBuilder('changelog')
        builder.addChangelogFile(resource('changelog/changelog'))

        when:
        Format format = scan(new File(tmp, builder.build(tmp)))

        then:
        (format.header.getEntry(HeaderTag.CHANGELOGNAME).values as List).size() == 10
        (format.header.getEntry(HeaderTag.CHANGELOGNAME).values as List).first() == 'George Washington'
        (format.header.getEntry(HeaderTag.CHANGELOGTIME).values as int[]).length == 10
    }

    def 'writes scripts with their interpreter'() {
        given:
        Builder builder = newBuilder('scripts')
        builder.setPreInstallScript('#!/bin/bash\necho pre')
        builder.setPostInstallScript('echo post')
        builder.setPreUninstallScript(resource('prein.sh'))

        when:
        Format format = scan(new File(tmp, builder.build(tmp)))

        then:
        format.header.getEntry(HeaderTag.PREINPROG).values == ['/bin/bash'] as String[]
        format.header.getEntry(HeaderTag.POSTINPROG).values == ['/bin/sh'] as String[]
        format.header.getEntry(HeaderTag.POSTINSCRIPT).values == ['echo post'] as String[]
        format.header.getEntry(HeaderTag.PREUNSCRIPT).values == [resource('prein.sh').readLines().join('\n') + '\n'] as String[]
    }

    private static Map<String, Integer> fileModes(Format format) {
        String[] dirNames = format.header.getEntry(HeaderTag.DIRNAMES).values
        int[] dirIndexes = format.header.getEntry(HeaderTag.DIRINDEXES).values
        String[] baseNames = format.header.getEntry(HeaderTag.BASENAMES).values
        short[] modes = format.header.getEntry(HeaderTag.FILEMODES).values
        (0..<baseNames.length).collectEntries { int i -> [(dirNames[dirIndexes[i]] + baseNames[i]): modes[i] & 0xFFFF] }
    }

    private static boolean verifies(Format format, SignatureTag tag, byte[] rpm, int start, int end) {
        PGPPublicKeyRingCollection keys = resource('pgp/pubring.gpg').withInputStream {
            new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(it), new BcKeyFingerprintCalculator())
        }
        byte[] packet = format.signature.getEntry(tag).values as byte[]
        PGPSignature signature = ((PGPSignatureList) new BcPGPObjectFactory(packet).nextObject()).get(0)
        signature.init(new BcPGPContentVerifierBuilderProvider(), keys.getPublicKey(signature.keyID))
        signature.update(rpm, start, end - start)
        return signature.verify()
    }
}
