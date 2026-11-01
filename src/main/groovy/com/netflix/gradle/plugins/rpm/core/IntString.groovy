/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core

import groovy.transform.CompileStatic

/**
 * Simple class to pair an int and a String with each other.
 */
@CompileStatic
class IntString {

    private int theInt = 0
    private String theString = ''

    IntString(int theInt, String theString) {
        this.theInt = theInt
        this.theString = theString
    }

    void setInt(int theInt) {
        this.theInt = theInt
    }

    int getInt() {
        return this.theInt
    }

    void setString(String theString) {
        this.theString = theString
    }

    String getString() {
        return this.theString
    }
}
