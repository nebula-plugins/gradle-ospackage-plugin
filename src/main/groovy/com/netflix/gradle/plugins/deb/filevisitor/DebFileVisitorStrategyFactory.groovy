package com.netflix.gradle.plugins.deb.filevisitor

import com.netflix.gradle.plugins.deb.DebCopyAction
import org.apache.commons.lang3.JavaVersion
import org.apache.commons.lang3.SystemUtils
import org.vafer.jdeb.DataProducer

class DebFileVisitorStrategyFactory {
    private final DebFileVisitorStrategy preJava7DebFileVisitorStrategy
    private final DebFileVisitorStrategy java7AndHigherDebFileVisitorStrategy

    DebFileVisitorStrategyFactory(List<DataProducer> dataProducers, List<DebCopyAction.InstallDir> installDirs) {
        preJava7DebFileVisitorStrategy = new PreJava7DebFileVisitorStrategy(dataProducers, installDirs)
        java7AndHigherDebFileVisitorStrategy = new Java7AndHigherDebFileVisitorStrategy(dataProducers, installDirs)
    }

    DebFileVisitorStrategy getStrategy() {
        if(SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_7)) {
            return java7AndHigherDebFileVisitorStrategy
        }

        preJava7DebFileVisitorStrategy
    }
}
