package com.netflix.gradle.plugins.packaging

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Directory implements Serializable {
    String path
    int permissions = -1
    String user = null
    String permissionGroup = null
    boolean addParents = false
}
