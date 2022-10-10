/*
 * Copyright 2014-2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.gradle.plugins.daemon

import com.netflix.gradle.plugins.packaging.SystemPackagingPlugin
import nebula.test.PluginProjectSpec
import org.codehaus.groovy.runtime.StackTraceUtils

class OspackageDaemonPluginSpec extends PluginProjectSpec {
    @Override
    String getPluginName() {
        'com.netflix.nebula.ospackage-daemon'
    }

    def 'if no daemonName is assigned use the name of the project'() {
        when:
        project.plugins.apply(SystemPackagingPlugin)
        OspackageDaemonPlugin plugin = project.plugins.apply(OspackageDaemonPlugin)
        plugin.extension.daemon {
            command = 'exit 1'
        }
        project.evaluate()

        then:
        noExceptionThrown()
        project.tasks.withType(DaemonTemplateTask) {
            assert it.context.daemonName == project.name
        }
    }

    def 'no duplicate default names'() {
        when:
        OspackageDaemonPlugin plugin = project.plugins.apply(OspackageDaemonPlugin)
        plugin.extension.daemon {
            command = 'exit 0'
        }
        plugin.extension.daemon {
            command = 'exit 0'
        }
        project.evaluate()

        then:
        def e = thrown(Exception)
        StackTraceUtils.extractRootCause(e) instanceof IllegalArgumentException
    }

    def 'no duplicate names'() {
        when:
        OspackageDaemonPlugin plugin = project.plugins.apply(OspackageDaemonPlugin)
        plugin.extension.daemon {
            daemonName = 'foo'
        }
        plugin.extension.daemon {
            daemonName = 'foo'
        }
        project.evaluate()

        then:
        def e = thrown(Exception)
        StackTraceUtils.extractRootCause(e) instanceof IllegalArgumentException
    }

    def 'can call daemon directly in project'() {
        when:
        OspackageDaemonPlugin plugin = project.plugins.apply(OspackageDaemonPlugin)
        DaemonExtension extension = plugin.extension

        project.daemon {
            daemonName = 'foobar'
            command = 'exit 0'
            installCmd = 'abc'
        }

        then: 'daemon was added to list'
        !extension.daemons.isEmpty()

        then: 'daemon configurate'
        def daemon = extension.daemons.iterator().next()
        daemon.daemonName == 'foobar'
        daemon.installCmd == 'abc'
    }

    def 'can call daemons extensions in project'() {
        when:
        OspackageDaemonPlugin plugin = project.plugins.apply(OspackageDaemonPlugin)
        DaemonExtension extension = plugin.extension

        project.daemons {
            daemon {
                daemonName = 'foobar'
                command = 'exit 0'
            }
        }

        then: 'daemon was added to list'
        !extension.daemons.isEmpty()

        then: 'daemon configurate'
        extension.daemons.iterator().next().daemonName == 'foobar'
    }

    def 'tasks are created'() {
        when:
        project.plugins.apply(SystemPackagingPlugin)
        OspackageDaemonPlugin plugin = project.plugins.apply(OspackageDaemonPlugin)
        plugin.extension.daemon {
            daemonName = 'foobar'
            command = 'exit 1'
        }
        plugin.extension.daemon {
            daemonName = 'baz'
            command = 'exit 0'
        }
        project.evaluate()

        then:
        project.tasks.getByName('buildDebFoobarDaemon')
        project.tasks.getByName('buildRpmFoobarDaemon')
        project.tasks.getByName('buildDebBazDaemon')
        project.tasks.getByName('buildRpmBazDaemon')

    }
    def 'run tasks'() {
        when:
        project.plugins.apply(SystemPackagingPlugin)
        OspackageDaemonPlugin plugin = project.plugins.apply(OspackageDaemonPlugin)
        plugin.extension.daemon {
            daemonName = 'foobar'
            command = 'exit 0'
        }
        project.evaluate()

        then: 'task is created after evaluation'
        DaemonTemplateTask templateTask = project.tasks.getByName('buildDebFoobarDaemon')
        templateTask != null

        when:
        templateTask.template()

        then:
        File initd = new File(projectDir, 'build/daemon/Foobar/buildDeb/initd')
        initd.exists()
        File logrun = new File(projectDir, 'build/daemon/Foobar/buildDeb/log-run')
        !logrun.text.contains("null")
        logrun.text.contains("chown nobody ./main")
    }
}
