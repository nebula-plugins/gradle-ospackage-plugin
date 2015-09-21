package com.netflix.gradle.plugins.deb

import spock.lang.Specification

class TemplateHelperSpec extends Specification {

    File tmpDir = File.createTempFile('template', 'tmp')
    TemplateHelper helper = new TemplateHelper(tmpDir, '/deb')

    def setup() {
        tmpDir.delete() // to make space for it to be a directory
        //tmpDir.mkdirs()
    }

    def cleanup() {
        //tmpDir.deleteDir()
    }

    def defaultContext = [name: 'PackageName',
        version: 'Version',
        release: 'Release',
        author: 'User',
        description: 'Description',
        distribution: 'Distribution',
        summary: 'Summary',
        section: 'Group',
        priority: 'optional',
        maintainer: 'Maintainer',
        uploaders: 'Uploaders',
        epoch: '0',
        time: '2013-11-11',
        provides: 'Provides',
        url: 'URL',
        depends: '',
        arch: 'Arch',
        dirs: 'dirs',
        multiArch: '',
        conflicts: '',
        recommends: '',
        suggests: '',
        enhances: '',
        preDepends: '',
        breaks: '',
        replaces: '',
        customFields: '',
        fullVersion: '0:1.0.0-1']

    def 'produces template'() {
        when:
        def resultFile = helper.generateFile('control', defaultContext)

        then:
        resultFile.exists()
        resultFile.text.contains('Architecture: Arch')
        resultFile.text.contains('Depends')
        !resultFile.text.contains('Multi-Arch')
    }

    def 'produces template with conditioned block'() {
        when:
        def resultFile = helper.generateFile('control', defaultContext + [depends: 'Depends1', multiArch: 'MultiArch1'])

        then:
        resultFile.exists()
        resultFile.text.contains('Depends: Depends1')
        resultFile.text.contains('Multi-Arch: MultiArch1')
    }
}
