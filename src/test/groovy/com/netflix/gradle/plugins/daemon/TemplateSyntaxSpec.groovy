/*
 * Copyright 2014-2016 Netflix, Inc.
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

import nebula.test.ProjectSpec

class TemplateSyntaxSpec extends ProjectSpec {

    def 'each template validates'() {
        when:
        def plugin = new OspackageDaemonPlugin()
        def templates = ['initd', 'log-run', 'run']
        def helper = new TemplateHelper(projectDir, '/com/netflix/gradle/plugins/daemon')
        DaemonDefinition definition = new DaemonDefinition()
        def context = plugin.toContext(plugin.getDefaultDaemonDefinition(false), definition)
        context['isRedhat'] = 'true'
        context['daemonName'] = 'foobar'
        context['command'] = 'foo'
        templates.each {
            helper.generateFile(it, context)
        }

        then:
        noExceptionThrown()

    }
    def 'template fails with a null'() {
        when:
        def helper = new TemplateHelper(projectDir, '/com/netflix/gradle/plugins/daemon')

        def context = [:]
        context['isRedhat'] = true
        context['logUser'] = null
        helper.generateFile('log-run', context)

        then:
        thrown(IllegalArgumentException)

    }
}
