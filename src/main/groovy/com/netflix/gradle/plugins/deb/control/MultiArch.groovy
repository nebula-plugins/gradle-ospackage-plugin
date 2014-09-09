package com.netflix.gradle.plugins.deb.control

// See https://wiki.ubuntu.com/MultiarchSpec
public enum MultiArch {
    SAME,
    FOREIGN,
    ALLOWED
}