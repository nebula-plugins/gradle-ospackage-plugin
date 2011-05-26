/*
 * Copyright 2011 the original author or authors.
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

package com.trigonic.gradle.plugins.rpm

import static org.junit.Assert.assertTrue

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import com.trigonic.gradle.plugins.rpm.Rpm;

class RpmPluginTest {
	@Test
	public void files() {
		Project project = ProjectBuilder.builder().build()

		File buildDir = project.buildDir
		println "Extracting to $buildDir"
		File srcDir = new File(buildDir, 'src')
		srcDir.mkdirs()
		FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')
		
		project.apply plugin: 'rpm'
		
		project.task([type: Rpm], 'buildRpm', {
			baseName = 'bleah'
			version = '1.0'
			release = '1'
			architecture = 'i386'
			from(srcDir)
		})
		
		project.tasks.buildRpm.execute()
	}
}
