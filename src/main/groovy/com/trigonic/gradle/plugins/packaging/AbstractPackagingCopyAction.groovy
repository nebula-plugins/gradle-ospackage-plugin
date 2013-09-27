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
import org.gradle.api.internal.file.CopyActionProcessingStreamAction
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.internal.file.copy.DefaultFileCopyDetails
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.internal.tasks.SimpleWorkResult
import org.gradle.api.tasks.WorkResult
import org.gradle.internal.UncheckedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Field

public abstract class AbstractPackagingCopyAction implements CopyAction {
    static final Logger logger = LoggerFactory.getLogger(AbstractPackagingCopyAction.class)

    SystemPackagingTask task
    File tempDir
    Collection<File> filteredFiles = []

    protected AbstractPackagingCopyAction(SystemPackagingTask task) {
        this.task = task
    }

    public WorkResult execute(CopyActionProcessingStream stream) {

        try {
            startVisit(this)
            stream.process(new StreamAction());
            endVisit()
        } catch (Exception e) {
            UncheckedException.throwAsUncheckedException(e);
        } finally {
            visitFinally()
        }

        return new SimpleWorkResult(true);
    }

    // Not a static class
    private class StreamAction implements CopyActionProcessingStreamAction {
        public void processFile(FileCopyDetailsInternal details) {
            // While decoupling the spec from the action is nice, it contains some needed info
            def ourSpec = extractSpec(details) // Can be null
            if (details.isDirectory()) {
                visitDir(details, ourSpec);
            } else {
                visitFile(details, ourSpec);
            }
        }
    }

    protected abstract void visitDir(FileCopyDetailsInternal dirDetails, def specToLookAt)
    protected abstract void visitFile(FileCopyDetailsInternal fileDetails, def specToLookAt)
    protected abstract void addLink(Link link);
    protected abstract void addDependency(Dependency dependency);
    protected abstract void end();

    void startVisit(CopyAction action) {
        // Delay reading destinationDir until we start executing
        tempDir = task.getTemporaryDir()
    }

    void visitFinally(Exception e) {
    }

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
        if (specToLookAt?.metaClass?.hasProperty(specToLookAt, propertyName) != null) {
            return specToLookAt.metaClass.getProperty(specToLookAt, propertyName)
        } else {
            return null
        }
    }

    CopySpecInternal extractSpec(FileCopyDetailsInternal fileDetails) {
        if (fileDetails instanceof DefaultFileCopyDetails) {
            def startingClass = fileDetails.getClass() // It's in there somewhere
            while( startingClass != null && startingClass != DefaultFileCopyDetails) {
                startingClass = startingClass.superclass
            }
            Field specField = startingClass.getDeclaredField('spec')
            specField.setAccessible(true)
            CopySpecInternal ret = specField.get(fileDetails)
            return ret
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
