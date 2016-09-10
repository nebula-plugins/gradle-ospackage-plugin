package com.netflix.gradle.plugins.deb

import com.netflix.gradle.plugins.utils.FileSystemActions
import nebula.test.ProjectSpec

class MaintainerScriptsGeneratorSpec extends ProjectSpec {
    FileSystemActions fileSystemActions
    TemplateHelper templateHelper
    Map<String, Object> context

    def setup() {
        fileSystemActions = Mock(FileSystemActions)
        templateHelper = Mock(TemplateHelper)
        context = [:]
    }

    def 'does not call templateHelper if preInstallFile defined'() {
        given:
        def source = new File('sample-script.sh')
        Deb task = project.task([type: Deb], 'buildDeb', {
            preInstallFile = source
        }) as Deb

        def destination = new File('/tmp')
        def generator = new MaintainerScriptsGenerator(task, templateHelper, destination, fileSystemActions)

        when:
        generator.generate(context)

        then:
        1 * fileSystemActions.copy(source, new File(destination, 'preinst'))
        0 * templateHelper.generateFile('preinst', _ as Map<String, Object>)
    }

    def 'do not generate preinst when no preInstall and preInstallFile defined'() {
        given:
        Deb task = project.task([type: Deb], 'buildDeb', {
            preInstallFile = null
        }) as Deb
        def generator = new MaintainerScriptsGenerator(task, templateHelper, new File('/tmp'), fileSystemActions)

        when:
        generator.generate(context)

        then:
        0 * fileSystemActions.copy(_ as File, _ as File)
        0 * templateHelper.generateFile('preinst', _ as Map<String, Object>)
    }

    def 'does not call templateHelper if postInstallFile defined'() {
        given:
        def source = new File('sample-script.sh')
        Deb task = project.task([type: Deb], 'buildDeb', {
            postInstallFile = source
        }) as Deb
        def destination = new File('/tmp')
        def generator = new MaintainerScriptsGenerator(task, templateHelper, destination, fileSystemActions)

        when:
        generator.generate(context)

        then:
        1 * fileSystemActions.copy(source, new File(destination, 'postinst'))
        0 * templateHelper.generateFile('postinst', _ as Map<String, Object>)
    }

    def 'do not generate postinst when when no postInstall and postInstallFile defined'() {
        given:
        Deb task = project.task([type: Deb], 'buildDeb', {
            postInstallFile = null
        }) as Deb
        def generator = new MaintainerScriptsGenerator(task, templateHelper, new File('/tmp'), fileSystemActions)

        when:
        generator.generate(context)

        then:
        0 * fileSystemActions.copy(_ as File, _ as File)
        0 * templateHelper.generateFile('postinst', _ as Map<String, Object>)
    }

    def 'does not call templateHelper if preUninstallFile defined'() {
        given:
        def source = new File('sample-script.sh')
        Deb task = project.task([type: Deb], 'buildDeb', {
            preUninstallFile = source
        }) as Deb
        def destination = new File('/tmp')
        def generator = new MaintainerScriptsGenerator(task, templateHelper, destination, fileSystemActions)

        when:
        generator.generate(context)

        then:
        1 * fileSystemActions.copy(source, new File(destination, 'prerm'))
        0 * templateHelper.generateFile('prerm', _ as Map<String, Object>)
    }

    def 'do not generate prerm when no preUninstall and preUninstallFile defined'() {
        given:
        Deb task = project.task([type: Deb], 'buildDeb', {
            preUninstallFile = null
        }) as Deb
        def generator = new MaintainerScriptsGenerator(task, templateHelper, new File('/tmp'), fileSystemActions)

        when:
        generator.generate(context)

        then:
        0 * fileSystemActions.copy(_ as File, _ as File)
        0 * templateHelper.generateFile('prerm', _ as Map<String, Object>)
    }

    def 'does not call templateHelper if postUninstallFile defined'() {
        given:
        def source = new File('sample-script.sh')
        Deb task = project.task([type: Deb], 'buildDeb', {
            postUninstallFile = source
        }) as Deb
        def destination = new File('/tmp')
        def generator = new MaintainerScriptsGenerator(task, templateHelper, destination, fileSystemActions)

        when:
        generator.generate(context)

        then:
        1 * fileSystemActions.copy(source, new File(destination, 'postrm'))
        0 * templateHelper.generateFile('postrm', _ as Map<String, Object>)
    }

    def 'do not generate postrm when no postUninstall and postUninstallFile defined'() {
        given:
        Deb task = project.task([type: Deb], 'buildDeb', {
            postUninstallFile = null
        }) as Deb
        def generator = new MaintainerScriptsGenerator(task, templateHelper, new File('/tmp'), fileSystemActions)

        when:
        generator.generate(context)

        then:
        0 * fileSystemActions.copy(_ as File, _ as File)
        0 * templateHelper.generateFile('postrm', _ as Map<String, Object>)
    }
}
