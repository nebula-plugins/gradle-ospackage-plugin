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

import com.trigonic.gradle.plugins.packaging.AbstractPackagingCopySpecVisitor
import com.trigonic.gradle.plugins.packaging.SystemPackagingTask
import org.freecompany.redline.header.Architecture
import org.freecompany.redline.header.Flags
import org.freecompany.redline.header.Os
import org.freecompany.redline.header.RpmType
import org.freecompany.redline.payload.Directive

class Rpm extends SystemPackagingTask {
    static final String RPM_EXTENSION = "rpm";

    Architecture arch = Architecture.NOARCH
    Os os = Os.UNKNOWN
    RpmType type = RpmType.BINARY

    Rpm() {
        super()
        extension = RPM_EXTENSION
    }

    @Override
    String assembleArchiveName() {
        String name = getPackageName();
        name += getVersion() ? "-${getVersion()}" : ''
        name += getRelease() ? "-${getRelease()}" : ''
        name += getArchString() ? ".${getArchString()}" : ''
        name += getExtension() ? ".${getExtension()}" : ''
        return name;
    }

    @Override
    protected String getArchString() {
        return arch?.name().toLowerCase();
    }

    @Override
    protected AbstractPackagingCopySpecVisitor getVisitor() {
        return new RpmCopySpecVisitor(this)
    }
}
