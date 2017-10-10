package com.netflix.gradle.plugins.daemon

class LegacyInstallCmd {
    static String create(Map<String, Object> ctx) {
        if (ctx.isRedHat) {
            "/sbin/chkconfig ${ctx.daemonName} on"
        } else {
            def startRunLevels = ctx.runLevels.join(' ')
            def stopRunLevels = ([0, 1, 2, 3, 4, 5, 6] - ctx.runLevels).join(' ')
            "/usr/sbin/update-rc.d ${ctx.daemonName} start ${ctx.startSequence} ${startRunLevels} . stop ${ctx.stopSequence} ${stopRunLevels} ."
        }
    }
}
