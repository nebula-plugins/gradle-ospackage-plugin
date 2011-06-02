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
import java.lang.reflect.Field
import java.lang.reflect.Modifier

import org.freecompany.redline.header.Architecture
import org.freecompany.redline.header.Os
import org.freecompany.redline.payload.Directive
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.CopyActionImpl
import org.gradle.api.internal.file.copy.CopySpecImpl
import org.gradle.api.tasks.bundling.AbstractArchiveTask

class Rpm extends AbstractArchiveTask {
    static final String RPM_EXTENSION = "rpm";

    final CopyActionImpl action;
    Architecture arch = Architecture.NOARCH;
    Os os = Os.UNKNOWN;

    Rpm() {
        action = new RpmCopyAction(getServices().get(FileResolver.class))
        extension = RPM_EXTENSION

        for (Architecture arch : Architecture.values()) {
            setProperty arch.name(), arch
        }

        for (Os os : Os.values()) {
            setProperty os.name(), os
        }

        for (Field field : Directive.class.fields) {
            if (field.type == Directive.class && field.hasModifier(Modifier.STATIC)) {
                setProperty field.name, field.get(null)
            }
        }
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

    Architecture getArch() {
        arch
    }

    void setArch(Architecture arch) {
        this.arch = arch
    }

    Os getOs() {
        os
    }

    void setOs(Os os) {
        this.os = os
    }

    String getArchiveName() {
        String.format("%s-%s-%s.%s.%s", packageName, version, release, arch.name().toLowerCase(), extension)
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

        Architecture getArch() {
            Rpm.this.arch
        }

        Os getOs() {
            Rpm.this.os
        }
    }
}
