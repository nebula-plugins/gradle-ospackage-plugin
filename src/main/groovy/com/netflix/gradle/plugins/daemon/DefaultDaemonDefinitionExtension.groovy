package com.netflix.gradle.plugins.daemon

class DefaultDaemonDefinitionExtension extends DaemonDefinition {
    boolean useExtensionDefaults = false
    
    String runScriptLocation
    String runLogScriptLocation
    String initDScriptLocation
}
