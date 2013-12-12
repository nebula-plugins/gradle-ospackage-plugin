package com.netflix.gradle.plugins.packaging

import org.gradle.api.internal.file.copy.DefaultCopySpec
import org.junit.Test

import static org.junit.Assert.*

class CopySpecEnhancementTest {

    @Test
    public void addUser() {
        def spec = new DefaultCopySpec(null, null)
        assertNull( spec.metaClass.hasProperty('user'))

        CopySpecEnhancement.user(spec, 'USER')

        assertEquals('USER', spec.user )
    }

    @Test
    public void addAddParentDirs() {
        def spec = new DefaultCopySpec(null, null)
        CopySpecEnhancement.setAddParentDirs(spec, true)

        assertEquals(true, spec.addParentDirs )
    }

    @Test
    public void addCreateDirectoryEntry() {
        def spec = new DefaultCopySpec(null, null)

        use(CopySpecEnhancement) {
            spec.createDirectoryEntry false
        }

        assertEquals(false, spec.createDirectoryEntry )

        use(CopySpecEnhancement) {
            spec.createDirectoryEntry true
        }

        assertEquals(true, spec.createDirectoryEntry )

        use(CopySpecEnhancement) {
            spec.setCreateDirectoryEntry(false)
        }

        assertEquals(false, spec.createDirectoryEntry )

        use(CopySpecEnhancement) {
            spec.setCreateDirectoryEntry(true)
        }

        assertEquals(true, spec.createDirectoryEntry )

    }
}
