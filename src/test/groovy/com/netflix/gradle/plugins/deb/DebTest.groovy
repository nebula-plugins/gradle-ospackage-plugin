package com.netflix.gradle.plugins.deb

import nebula.test.ProjectSpec
import org.apache.commons.io.FileUtils

class DebTest extends ProjectSpec {
    def 'can execute Deb task with valid version'() {
        given:
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'deb'
        project.version = '1.0'

        Deb debTask = project.task('buildDeb', type:Deb) {
            version = '1.0'
            from(srcDir)
        }

        when:
        debTask.execute()

        then:
        noExceptionThrown()
    }
}
