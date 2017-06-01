package com.netflix.gradle.plugins.deb

import com.netflix.gradle.plugins.utils.FileSystemActions
import groovy.transform.Canonical

class MaintainerScriptsGenerator {
    private final Deb task
    private final TemplateHelper templateHelper
    private final File destination
    private final FileSystemActions fileSystem

    MaintainerScriptsGenerator(Deb task, TemplateHelper templateHelper, File destination, FileSystemActions fileSystem) {
        this.destination = destination
        this.task = task
        this.templateHelper = templateHelper
        this.fileSystem = fileSystem
    }

    void generate(Map<String, Object> context) {
        templateHelper.generateFile("control", context)

        def configurationFiles = task.allConfigurationFiles
        if (configurationFiles.any()) {
            templateHelper.generateFile("conffiles", [files: configurationFiles] )
        }

        def scripts = [
                new MaintainerScript("preinst", task.preInstallFile, task.allPreInstallCommands),
                new PostInstScript(task.postInstallFile, task.allPostInstallCommands, context),
                new MaintainerScript("prerm", task.preUninstallFile, task.allPreUninstallCommands),
                new MaintainerScript("postrm", task.postUninstallFile, task.allPostUninstallCommands)
        ]
        def installUtils = task.allCommonCommands.collect { stripShebang(it) }
        for (script in scripts) {
            if(script.file) {
                fileSystem.copy(script.file, new File(destination, script.name))
            } else if (script.needsTemplateGeneration()) {
                templateHelper.generateFile(script.name, context + [commands: installUtils + script.commands.collect { stripShebang(it) }])
            }
        }
        // The debconf configuration files don't include the installUtils scripts.
        def debconfScripts = [
                new MaintainerScript("config", task.debconfConfigFile, task.allDebconfConfigCommands),
                new MaintainerScript("templates", task.debconfTemplatesFile, task.allDebconfTemplatesCommands)
        ]
        for (script in debconfScripts) {
            if(script.file) {
                fileSystem.copy(script.file, new File(destination, script.name))
            } else if (script.needsTemplateGeneration()) {
                templateHelper.generateFile(script.name, context + [commands: script.commands.collect { stripShebang(it) }])
            }
        }
    }

    /**
     * Works with nulls, Strings and Files.
     *
     * @param script
     * @return
     */
    private static String stripShebang(Object script) {
        StringBuilder result = new StringBuilder();
        script?.eachLine { line ->
            if (!line.matches('^#!.*$')) {
                result.append line
                result.append "\n"
            }
        }
        result.toString()

    }

    @Canonical
    private static class MaintainerScript {
        String name
        File file
        List<Object> commands

        boolean needsTemplateGeneration() {
            return commands
        }
    }

    private static class PostInstScript extends MaintainerScript {
        Map<String, Object> context

        PostInstScript(File file, List<Object> commands, Map<String, Object> context) {
            super("postinst", file, commands)
            this.context = context
        }

        @Override
        boolean needsTemplateGeneration() {
            return super.needsTemplateGeneration() || context['dirs']
        }
    }
}
