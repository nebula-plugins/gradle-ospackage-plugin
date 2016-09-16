package com.netflix.gradle.plugins.packaging

class Directory {
    String path
    int permissions = -1
    String user = null
    String permissionGroup = null
    boolean addParents = false
}
