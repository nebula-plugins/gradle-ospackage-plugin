package com.netflix.gradle.plugins.packaging

import org.gradle.api.internal.file.DefaultFileLookup
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.DefaultCopySpec
import org.gradle.api.tasks.util.PatternSet
import org.gradle.api.tasks.util.internal.PatternSets
import org.gradle.internal.Factory
import org.gradle.internal.nativeintegration.filesystem.FileSystem
import org.gradle.internal.nativeintegration.services.NativeServices
import org.gradle.internal.reflect.DirectInstantiator
import org.gradle.internal.reflect.Instantiator
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

class CopySpecEnhancementTest {
    private final FileResolver fileResolver = [resolve: { it as File }, getPatternSetFactory: {
        TestFiles.getPatternSetFactory()
    }] as FileResolver
    private final Instantiator instantiator = DirectInstantiator.INSTANCE

    def spec = new DefaultCopySpec(fileResolver, instantiator)

    @Test
    public void addUser() {
        assertNull(spec.metaClass.hasProperty('user'))

        CopySpecEnhancement.user(spec, 'USER')

        assertEquals('USER', spec.user)
    }

    @Test
    public void addAddParentDirs() {
        CopySpecEnhancement.setAddParentDirs(spec, true)

        assertEquals(true, spec.addParentDirs)
    }

    @Test
    public void addCreateDirectoryEntry() {
        use(CopySpecEnhancement) {
            spec.createDirectoryEntry false
        }

        assertEquals(false, spec.createDirectoryEntry)

        use(CopySpecEnhancement) {
            spec.createDirectoryEntry true
        }

        assertEquals(true, spec.createDirectoryEntry)

        use(CopySpecEnhancement) {
            spec.setCreateDirectoryEntry(false)
        }

        assertEquals(false, spec.createDirectoryEntry)

        use(CopySpecEnhancement) {
            spec.setCreateDirectoryEntry(true)
        }

        assertEquals(true, spec.createDirectoryEntry)
    }
}

// Copied from Gradle core as DefaultCopySpec can no longer have null arguments

public class TestFiles {
    private static final FileSystem FILE_SYSTEM = NativeServicesTestFixture.getInstance().get(FileSystem.class);
    private static
    final DefaultFileLookup FILE_LOOKUP = new DefaultFileLookup(FILE_SYSTEM, PatternSets.getNonCachingPatternSetFactory());

    /**
     * Returns a resolver with no base directory.
     */
    public static FileResolver resolver() {
        return FILE_LOOKUP.getFileResolver();
    }

    public static Factory<PatternSet> getPatternSetFactory() {
        return resolver().getPatternSetFactory();
    }
}

public class NativeServicesTestFixture {
    static NativeServices nativeServices;
    static boolean initialized;

    public static synchronized void initialize() {
        if (!initialized) {
            File nativeDir = new File("build/native-libs");
            NativeServices.initialize(nativeDir);
            initialized = true;
        }
    }

    public static synchronized NativeServices getInstance() {
        if (nativeServices == null) {
            initialize();
            nativeServices = NativeServices.getInstance();
        }
        return nativeServices;
    }
}
