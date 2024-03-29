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

import com.netflix.gradle.plugins.utils.ConfigureUtil
import groovy.transform.Canonical
import org.gradle.api.DomainObjectCollection

@Canonical
class DaemonExtension {
    final DomainObjectCollection<DaemonDefinition> daemons

    // TBD Add defaults, like user name for all daemons

    DaemonDefinition daemon(Closure configure) {
        DaemonDefinition definition = new DaemonDefinition()
        ConfigureUtil.configure(configure, definition)
        if(daemons.contains(definition)) {
            throw new IllegalArgumentException("Could not configure as duplicated daemons are present: ${definition?.daemonName}")
        }
        daemons.add(definition)
        return definition
    }
}
