package com.netflix.gradle.plugins.rpm.filevisitor

import org.apache.commons.lang3.JavaVersion
import org.apache.commons.lang3.SystemUtils
import org.redline_rpm.Builder

class RpmFileVisitorStrategyFactory {
    private final RpmFileVisitorStrategy preJava7RpmFileVisitorStrategy
    private final RpmFileVisitorStrategy java7AndHigherRpmFileVisitorStrategy

    RpmFileVisitorStrategyFactory(Builder builder) {
        preJava7RpmFileVisitorStrategy = new PreJava7RpmFileVisitorStrategy(builder)
        java7AndHigherRpmFileVisitorStrategy = new Java7AndHigherRpmFileVisitorStrategy(builder)
    }

    RpmFileVisitorStrategy getStrategy() {
        if(SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_7)) {
            return java7AndHigherRpmFileVisitorStrategy
        }

        preJava7RpmFileVisitorStrategy
    }
}
