/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trigonic.gradle.plugins.rpm

import java.io.File

import org.apache.commons.lang.StringUtils;
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.CopyActionImpl
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.util.GUtil
import org.slf4j.helpers.MessageFormatter;

class Rpm extends AbstractArchiveTask {
	static final String RPM_EXTENSION = "rpm";

	final CopyActionImpl action;
	
	Rpm() {
		action = new RpmCopyAction(getServices().get(FileResolver.class))
		extension = RPM_EXTENSION
	}

	CopyActionImpl getCopyAction() {
		action
	}
	
	String getPackageName() {
		baseName
	}
	
	void setPackageName(String packageName) {
		baseName = packageName
	}
	
	String getRelease() {
		classifier
	}
	
	void setRelease(String release) {
		classifier = release
	}
	
	String getArchiveName() {
		String.format("%s-%s-%s.noarch.%s", packageName, version, release, extension) 
	}
	
	class RpmCopyAction extends CopyActionImpl {
		public RpmCopyAction(FileResolver resolver) {
			super(resolver, new RpmCopySpecVisitor());
		}
		
		File getDestinationDir() {
			Rpm.this.destinationDir
		}
		
		String getPackageName() {
			Rpm.this.packageName
		}
		
		String getVersion() {
			Rpm.this.version
		}
		
		String getRelease() {
			Rpm.this.release
		}
	}
}
