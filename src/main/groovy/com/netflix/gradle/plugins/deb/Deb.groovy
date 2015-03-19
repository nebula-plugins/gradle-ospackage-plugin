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

package com.netflix.gradle.plugins.deb

import com.netflix.gradle.plugins.packaging.AbstractPackagingCopyAction
import com.netflix.gradle.plugins.packaging.Dependency
import com.netflix.gradle.plugins.packaging.SystemPackagingTask
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class Deb extends SystemPackagingTask {
    static final String DEB_EXTENSION = "deb";

    Deb() {
        super()
        extension = DEB_EXTENSION
    }

    @Override
    String assembleArchiveName() {
        String name = getPackageName();
        name += getVersion() ? "_${getVersion()}" : ''
        name += getRelease() ? "-${getRelease()}" : ''
        name += getArchString() ? "_${getArchString()}" : ''
        name += getExtension() ? ".${getExtension()}" : ''
        return name;
    }

    @Override
    protected String getArchString() {
        return 'all'; // TODO Make this configurable
    }

    @Override
    AbstractPackagingCopyAction createCopyAction() {
        return new DebCopyAction(this)
    }

    @Override
    protected void applyConventions() {
        super.applyConventions()

        // For all mappings, we're only being called if it wasn't explicitly set on the task. In which case, we'll want
        // to pull from the parentExten. And only then would we fallback on some other value.
        ConventionMapping mapping = ((IConventionAware) this).getConventionMapping()

        // Could come from extension
        mapping.map('uid', { parentExten?.getUid()?:0 })
        mapping.map('gid', { (parentExten?.getGid())?:0 })
        mapping.map('packageGroup', { parentExten?.getPackageGroup() ?: 'java' })
        mapping.map('multiArch', { parentExten?.getMultiArch() })
    }

    @Input @Optional
    List<Dependency> getAllRecommends() {
        if (parentExten) {
            return getRecommends() + parentExten.getRecommends()
        } else {
            return getRecommends()
        }
    }

    @Input @Optional
    List<Dependency> getAllSuggests() {
        if (parentExten) {
            return getSuggests() + parentExten.getSuggests()
        } else {
            return getSuggests()
        }
    }

    @Input @Optional
    List<Dependency> getAllEnhances() {
        if (parentExten) {
            return getEnhances() + parentExten.getEnhances()
        } else {
            return getEnhances()
        }
    }

    @Input @Optional
    List<Dependency> getAllPreDepends() {
        if (parentExten) {
            return getPreDepends() + parentExten.getPreDepends()
        } else {
            return getPreDepends()
        }
    }

    @Input @Optional
    List<Dependency> getAllBreaks() {
        if (parentExten) {
            return getBreaks() + parentExten.getBreaks()
        } else {
            return getBreaks()
        }
    }

    @Input @Optional
    List<Dependency> getAllReplaces() {
        if (parentExten) {
            return getReplaces() + parentExten.getReplaces()
        } else {
            return getReplaces()
        }
    }
}
