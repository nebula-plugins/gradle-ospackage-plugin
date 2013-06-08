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

package com.trigonic.gradle.plugins.packaging

import com.trigonic.gradle.plugins.rpm.Rpm
import org.freecompany.redline.Builder
import org.freecompany.redline.header.Header.HeaderTag
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopySpecVisitor
import org.gradle.api.internal.file.copy.EmptyCopySpecVisitor
import org.gradle.api.internal.file.copy.ReadableCopySpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public abstract class SystemPackagingCopySpecVisitor implements CopySpecVisitor {
    static final Logger logger = LoggerFactory.getLogger(SystemPackagingCopySpecVisitor.class)

    SystemPackagingTask task
    File destinationDir
    ReadableCopySpec spec
    boolean didWork

    protected SystemPackagingCopySpecVisitor(SystemPackagingTask task) {
        this.task = task
    }

    @Override
    void startVisit(CopyAction action) {
        // Delay reading destinationDir until we start executing
        destinationDir = task.destinationDir
        didWork = false
    }

    @Override
    void visitSpec(ReadableCopySpec spec) {
        this.spec = spec
    }

    @Override
    void endVisit() {
        for (Link link : task.getAllLinks()) {
            logger.debug "adding link {} -> {}", link.path, link.target
            addLink link
        }

        for (Dependency dep : task.getAllDependencies()) {
            logger.debug "adding dependency on {} {}", dep.packageName, dep.version
            addDependency dep
        }

        end()
        // TODO Investigate, we seem to always set to true.
        didWork = true
    }

    protected abstract void addLink(Link link);
    protected abstract void addDependency(Dependency dependency);
    protected abstract void end();

    @Override
    boolean getDidWork() {
        didWork
    }

    String concat(Object... scripts) {
        String shebang
        StringBuilder result = new StringBuilder();
        scripts.each { script ->
            script?.eachLine { line ->
                if (line.matches('^#!.*$')) {
                    if (!shebang) {
                        shebang = line
                    } else if (line != shebang) {
                        throw new IllegalArgumentException("mismatching #! script lines")
                    }
                } else {
                    result.append line
                    result.append "\n"
                }
            }
        }
        if (shebang) {
            result.insert(0, shebang + "\n")
        }
        result.toString()
    }
}
