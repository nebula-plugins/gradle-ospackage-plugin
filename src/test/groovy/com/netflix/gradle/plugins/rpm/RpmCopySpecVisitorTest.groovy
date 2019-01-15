/*
 * Copyright 2011-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.gradle.plugins.rpm

import nebula.test.ProjectSpec
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class RpmCopySpecVisitorTest extends ProjectSpec {
    RpmCopyAction visitor

    @Before
    public void setup() {
        project.apply plugin: 'nebula.rpm'

        Rpm rpmTask = project.task('buildRpm', type: Rpm) {
            packageName = 'can-execute-rpm-task-with-valid-version'
        }

        visitor = new RpmCopyAction(rpmTask)
    }

    @Test
    public void withoutUtils() {
        visitor.includeStandardDefines = false
        File script = resourceFile("script.sh")
        Object result = visitor.scriptWithUtils([], [script])
        assertTrue result instanceof String
        assertEquals(
            "#!/bin/bash\n" +
            "hello\n", result)
    }

    @Test
    public void withUtils() {
        visitor.includeStandardDefines = false
        Object result = visitor.scriptWithUtils([resourceFile("utils.sh")], [resourceFile("script.sh")])
        assertTrue result instanceof String
        assertEquals(
            "#!/bin/bash\n" +
            "function hello() {\n" +
            "    echo 'Hello, world.'\n" +
            "}\n" +
            "hello\n", result)
    }

    File resourceFile(String name) {
        new File(getClass().getResource(name).getPath())
    }
}
