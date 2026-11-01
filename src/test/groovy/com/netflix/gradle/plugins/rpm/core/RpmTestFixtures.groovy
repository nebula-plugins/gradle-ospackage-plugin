package com.netflix.gradle.plugins.rpm.core

import com.netflix.gradle.plugins.rpm.core.header.Architecture
import com.netflix.gradle.plugins.rpm.core.header.Format
import com.netflix.gradle.plugins.rpm.core.header.Os
import com.netflix.gradle.plugins.rpm.core.header.RpmType
import com.netflix.gradle.plugins.rpm.core.payload.CpioHeader

import java.nio.channels.Channels

/**
 * Shared helpers for the tests of the RPM library in com.netflix.gradle.plugins.rpm.core.
 */
class RpmTestFixtures {

    private static final String RESOURCE_ROOT = '/com/netflix/gradle/plugins/rpm/core/'

    /**
     * @param path path relative to this package in the test resources
     * @return the test resource as a file
     */
    static File resource(String path) {
        URL url = RpmTestFixtures.getResource(RESOURCE_ROOT + path)
        assert url != null: "missing test resource $path"
        return new File(url.toURI())
    }

    /**
     * @return a builder with all required fields set
     */
    static Builder newBuilder(String name, String version = '1.0', String release = '1') {
        Builder builder = new Builder()
        builder.setPackage(name, version, release)
        builder.setBuildHost('localhost')
        builder.setLicense('GPL')
        builder.setPlatform(Architecture.NOARCH, Os.LINUX)
        builder.setType(RpmType.BINARY)
        return builder
    }

    /**
     * Reads the lead, signature and header of an RPM.
     */
    static Format scan(File rpm) {
        rpm.withInputStream { InputStream input ->
            new Scanner().run(new ReadableChannelWrapper(Channels.newChannel(input)))
        }
    }

    /**
     * Reads the payload of an RPM.
     * @return the content of every archive entry, keyed by path
     */
    static Map<String, byte[]> payload(File rpm) {
        Map<String, byte[]> files = [:]
        rpm.withInputStream { InputStream input ->
            Format format = new Scanner().run(new ReadableChannelWrapper(Channels.newChannel(input)))
            InputStream uncompressed = Util.openPayloadStream(format.header, input)
            ReadableChannelWrapper channel = new ReadableChannelWrapper(Channels.newChannel(uncompressed))
            int total = 0
            while (true) {
                CpioHeader header = new CpioHeader()
                total = header.read(channel, total)
                if (header.isLast()) break
                byte[] content = new byte[header.fileSize]
                new DataInputStream(uncompressed).readFully(content)
                total += header.fileSize
                files[header.name] = content
            }
        }
        return files
    }
}
