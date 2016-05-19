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

import org.gradle.api.internal.DefaultDomainObjectCollection
import spock.lang.Specification

class DaemonExtensionSpec extends Specification {
    def definitionList = new DefaultDomainObjectCollection<DaemonDefinition>(DaemonDefinition, [])
    DaemonExtension extension = new DaemonExtension( new DefaultDomainObjectCollection<DaemonDefinition>(DaemonDefinition, definitionList) )

    def 'configures on add'() {
        when:
        extension.daemon {
            daemonName = 'foobar'
            command = 'exit 0'
            user = 'builds'
            logUser = 'root'
            logDir = '/tmp'
            logCommand = 'multipass'
            runLevels = [1,2]
            autoStart = false
            startSequence = 85
            stopSequence = 15
        }

        then:
        !definitionList.isEmpty()
        def definition = definitionList.iterator().next()
        definition.daemonName == 'foobar'
        definition.command == 'exit 0'
        definition.user == 'builds'
        definition.logUser == 'root'
        definition.logDir == '/tmp'
        definition.logCommand == 'multipass'
        definition.runLevels == [1,2]
        !definition.autoStart
        definition.startSequence == 85
        definition.stopSequence == 15
    }
}
