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

package com.netflix.gradle.plugins.application

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

import javax.inject.Inject

/** The plain 12.x getters and the provider-backed options share one value. */
class OspackageApplicationExtension {
    private String legacyPrefix
    private String legacyDistribution
    private final Options options

    /** Retained for consumers that construct the 12.x extension directly. */
    OspackageApplicationExtension() {
        options = null
    }

    @Inject
    OspackageApplicationExtension(ObjectFactory objects) {
        options = objects.newInstance(Options)
    }

    Options getOptions() { options }

    @Deprecated
    String getPrefix() { options == null ? legacyPrefix : options.prefix.getOrNull() }

    void setPrefix(String value) {
        if (options == null) legacyPrefix = value
        else options.prefix.set(value)
    }

    @Deprecated
    String getDistribution() { options == null ? legacyDistribution : options.distribution.getOrNull() }

    void setDistribution(String value) {
        if (options == null) legacyDistribution = value
        else options.distribution.set(value)
    }

    static abstract class Options {
        abstract Property<String> getPrefix()
        abstract Property<String> getDistribution()
    }
}
