package com.trigonic.gradle.plugins.packaging

import com.trigonic.gradle.plugins.deb.Deb
import com.trigonic.gradle.plugins.rpm.Rpm
import com.trigonic.gradle.plugins.rpm.Scanner
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.freecompany.redline.header.Header.HeaderTag.*
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class SystemPackagingPluginTest {
    @Test
    public void tasksCreated() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()

        project.apply plugin: 'os-package'

        assertNotNull( project.getPlugins().getPlugin(SystemPackagingPlugin) )
        assertNotNull( project.getPlugins().getPlugin(SystemPackagingBasePlugin) )

        def ext = project.getExtensions().getByType(ProjectPackagingExtension)
        assertNotNull(ext)

        def debTask = project.tasks.getByName('buildDeb')
        assertTrue( debTask instanceof Deb )

        def rpmTask = project.tasks.getByName('buildRpm')
        assertTrue( rpmTask instanceof Rpm )

    }

}
