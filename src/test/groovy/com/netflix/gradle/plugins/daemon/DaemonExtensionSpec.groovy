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

import com.netflix.gradle.plugins.utils.BackwardsCompatibleDomainObjectCollectionFactory
import nebula.test.IntegrationSpec
import nebula.test.ProjectSpec
import org.gradle.api.internal.CollectionCallbackActionDecorator

class DaemonExtensionSpec extends ProjectSpec {

    def 'configures on add'() {
        given:
        def mockCollectionCallbackActionDecorator = Mock(CollectionCallbackActionDecorator)
        BackwardsCompatibleDomainObjectCollectionFactory factory = new BackwardsCompatibleDomainObjectCollectionFactory<>(project.gradle.gradleVersion, mockCollectionCallbackActionDecorator)
        def definitionList = factory.create(DaemonDefinition)
        DaemonExtension extension = new DaemonExtension(factory.create(DaemonDefinition, definitionList))

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
            installCmd = 'exit 2'
        }

        then:
        !extension.daemons.isEmpty()
        def definition = extension.daemons.iterator().next()
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
        definition.installCmd == 'exit 2'
    }
}
