/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core

import groovy.transform.CompileStatic

/**
 * An RPM dependency. RPM knows four kinds of dependency relation, all of which are
 * represented by this class: requires, provides, conflicts and obsoletes.
 */
@CompileStatic
class Dependency {

    private final String name
    private final String version
    private final Integer flags

    /**
     * Creates a new dependency.
     * @param name Name (e.g. "httpd")
     * @param version Version (e.g. "1.0")
     * @param flags Flags, see {@link com.netflix.gradle.plugins.rpm.core.header.Flags} (e.g. "GREATER | EQUAL")
     */
    Dependency(String name, String version, Integer flags) {
        this.name = name
        this.version = version
        this.flags = flags
    }

    String getName() {
        return name
    }

    String getVersion() {
        return version
    }

    Integer getFlags() {
        return flags
    }

    /**
     * @param dependencies the dependencies
     * @return the names of the dependencies, in iteration order
     */
    static String[] getArrayOfNames(Collection<Dependency> dependencies) {
        return dependencies.collect { Dependency it -> it.name } as String[]
    }

    /**
     * @param dependencies the dependencies
     * @return the versions of the dependencies, in iteration order
     */
    static String[] getArrayOfVersions(Collection<Dependency> dependencies) {
        return dependencies.collect { Dependency it -> it.version } as String[]
    }

    /**
     * @param dependencies the dependencies
     * @return the flags of the dependencies, in iteration order
     */
    static Integer[] getArrayOfFlags(Collection<Dependency> dependencies) {
        return dependencies.collect { Dependency it -> it.flags } as Integer[]
    }

    /**
     * @param dependencies the dependencies, keyed by name
     * @return the names of the dependencies, in iteration order
     */
    static String[] getArrayOfNames(Map<String, Dependency> dependencies) {
        return getArrayOfNames(dependencies.values())
    }

    /**
     * @param dependencies the dependencies, keyed by name
     * @return the versions of the dependencies, in iteration order
     */
    static String[] getArrayOfVersions(Map<String, Dependency> dependencies) {
        return getArrayOfVersions(dependencies.values())
    }

    /**
     * @param dependencies the dependencies, keyed by name
     * @return the flags of the dependencies, in iteration order
     */
    static Integer[] getArrayOfFlags(Map<String, Dependency> dependencies) {
        return getArrayOfFlags(dependencies.values())
    }
}
