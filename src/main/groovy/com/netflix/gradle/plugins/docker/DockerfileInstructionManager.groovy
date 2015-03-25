package com.netflix.gradle.plugins.docker

class DockerfileInstructionManager {
    private final List<String> instructions = new ArrayList<String>()

    void addInstruction(String instruction) {
        instructions << instruction
    }

    void create(File dockerFile) {
        dockerFile.withWriter { out ->
            instructions.each { instruction ->
                out.println instruction
            }
        }
    }
}
