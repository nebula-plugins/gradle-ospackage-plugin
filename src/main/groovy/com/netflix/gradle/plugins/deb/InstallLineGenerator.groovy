package com.netflix.gradle.plugins.deb

class InstallLineGenerator {
    String generate(DebCopyAction.InstallDir dir) {
        StringBuilder sb = new StringBuilder("install ")
        if(dir.user) {
            sb.append("-o ${dir.user} ")
        }
        if(dir.group) {
            sb.append("-g ${dir.group} ")
        }
        sb.append("-d ${dir.name}")
        sb.toString()
    }
}
