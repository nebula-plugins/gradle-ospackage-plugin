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

import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopySpecVisitor
import org.gradle.api.internal.file.copy.MappingCopySpecVisitor
import org.gradle.api.internal.file.copy.ReadableCopySpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public abstract class AbstractPackagingCopySpecVisitor implements CopySpecVisitor {
    static final Logger logger = LoggerFactory.getLogger(AbstractPackagingCopySpecVisitor.class)

    SystemPackagingTask task
    File tempDir
    Collection<File> filteredFiles = []
    ReadableCopySpec spec
    boolean didWork

    protected AbstractPackagingCopySpecVisitor(SystemPackagingTask task) {
        this.task = task
    }

    @Override
    void startVisit(CopyAction action) {
        // Delay reading destinationDir until we start executing
        tempDir = task.getTemporaryDir()
        didWork = false
    }

    // Implementation provide visitFile and visitDir directly.

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

        // TODO Clean up filteredFiles

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

    String concat(Collection<Object> scripts) {
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

    /**
     * Works with nulls, Strings and Files.
     *
     * @param script
     * @return
     */
    String stripShebang(Object script) {
        StringBuilder result = new StringBuilder();
        script?.eachLine { line ->
            if (!line.matches('^#!.*$')) {
                result.append line
                result.append "\n"
            }
        }
        result.toString()

    }

    def static lookup(def specToLookAt, String propertyName) {
        if (specToLookAt.metaClass.hasProperty(specToLookAt, propertyName) != null) {
            return specToLookAt.metaClass.getProperty(specToLookAt, propertyName)
        } else {
            return null
        }
    }

    /**
     * Look at FileDetails to get a file. If it's filtered file, we need to write it out to the filesystem ourselves.
     * Issue #30, FileVisitDetailsImpl won't give us file, since it filters on the fly.
     */
    File extractFile(FileVisitDetails fileDetails) {
        File outputFile = null
        try {
            outputFile = fileDetails.getFile()
        } catch (UnsupportedOperationException uoe) {
            // Can't access MappingCopySpecVisitor.FileVisitDetailsImpl since it's private, so we have to probe. We would test this:
            // if (fileDetails instanceof MappingCopySpecVisitor.FileVisitDetailsImpl && fileDetails.filterChain.hasFilters())
            outputFile = new File(tempDir, fileDetails.name)
            fileDetails.copyTo(outputFile)
            filteredFiles << outputFile
        }
        return outputFile
    }
}
