package com.netflix.gradle.plugins.packaging

import org.gradle.api.internal.file.DefaultFileLookup
import org.gradle.api.internal.file.FileCollectionFactory
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.DefaultCopySpec
import org.gradle.api.tasks.util.PatternSet
import org.gradle.api.tasks.util.internal.PatternSets
import org.gradle.internal.Factory
import org.gradle.internal.reflect.DirectInstantiator
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.GradleVersion
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

class CopySpecEnhancementTest {
    private final FileResolver fileResolver = [resolve: { it as File }, getPatternSetFactory: {
        TestFiles.getPatternSetFactory()
    }] as FileResolver
    private final Instantiator instantiator = DirectInstantiator.INSTANCE
    private final FileCollectionFactory factory = [:] as FileCollectionFactory

    def spec = createDefaultCopySpec()

    private DefaultCopySpec createDefaultCopySpec() {
        if (GradleVersion.current().baseVersion >= GradleVersion.version("6.0")) {
            new DefaultCopySpec(fileResolver, factory, instantiator);
        } else {
            new DefaultCopySpec(fileResolver, instantiator);
        }
    }

    @Test
    void addUser() {
        assertNull(spec.metaClass.hasProperty('user'))

        CopySpecEnhancement.user(spec, 'USER')

        assertEquals('USER', spec.user)
    }

    @Test
    void addAddParentDirs() {
        CopySpecEnhancement.setAddParentDirs(spec, true)

        assertEquals(true, spec.addParentDirs)
    }

    @Test
    void addCreateDirectoryEntry() {
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
class TestFiles {
    private static final DefaultFileLookup FILE_LOOKUP = new DefaultFileLookup(PatternSets.getNonCachingPatternSetFactory())

    /**
     * Returns a resolver with no base directory.
     */
    static FileResolver resolver() {
        return FILE_LOOKUP.getFileResolver()
    }

    static Factory<PatternSet> getPatternSetFactory() {
        return resolver().getPatternSetFactory()
    }
}