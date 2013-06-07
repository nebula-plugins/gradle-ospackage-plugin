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

import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.CopyActionImpl
import org.gradle.api.tasks.bundling.AbstractArchiveTask

import java.lang.reflect.Field
import java.lang.reflect.Modifier

public abstract class SystemPackagingTask extends AbstractArchiveTask {
    def fileType = null
    def createDirectoryEntry = null
    boolean addParentDirs = true

    final SystemPackagingCopyAction action
    String user
    String group
    String packageGroup = ''
    String buildHost = getLocalHostName()
    String summary = ''
    String description = ''
    String license = ''
    String packager = System.getProperty('user.name', '')
    String distribution = ''
    String vendor = ''
    String url = ''
    String sourcePackage
    String provides

    File installUtils
    File preInstall
    File postInstall
    File preUninstall
    File postUninstall

    // TODO Add conventions to pull from extension

    List<Link> links = new ArrayList<Link>()
    List<Dependency> dependencies = new ArrayList<Dependency>();

    SystemPackagingTask() {
        action = new SystemPackagingCopyAction(services.get(FileResolver.class), getVisitor())

        packageName = project.archivesBaseName

    }

    protected <T extends Enum<T>> void aliasEnumValues(T[] values) {
        for (T value : values) {
            assert !ext.hasProperty(value.name())
            ext.set value.name(), value
        }
    }

    protected <T> void aliasStaticInstances(Class<T> forClass) {
        aliasStaticInstances forClass, forClass
    }

    private hasModifier(Field field, int modifier) {
        (field.modifiers & modifier) == modifier
    }

    protected <T, U> void aliasStaticInstances(Class<T> forClass, Class<U> ofClass) {
        for (Field field : forClass.fields) {
            if (field.type == ofClass && hasModifier(field, Modifier.STATIC)) {
                assert !ext.hasProperty(field.name)
                ext.set field.name, field.get(null)
            }
        }
    }

    protected static String getLocalHostName() {
        try {
            return InetAddress.localHost.hostName
        } catch (UnknownHostException ignore) {
            return "unknown"
        }
    }

    CopyActionImpl getCopyAction() {
        action
    }

    String getPackageName() {
        // TODO This looks wrong.
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
        String.format("%s-%s-%s.%s.%s", packageName, version, release, arch.name().toLowerCase(), extension)
    }

    Link link(String path, String target) {
        link(path, target, -1)
    }

    Link link(String path, String target, int permissions) {
        Link link = new Link()
        link.path = path
        link.target = target
        link.permissions = permissions
        links.add(link)
        link
    }

    Dependency requires(String packageName, String version, int flag) {
        Dependency dep = new Dependency()
        dep.packageName = packageName
        dep.version = version
        dep.flag = flag
        dependencies.add(dep)
        dep
    }

    Dependency requires(String packageName) {
        requires(packageName, '', 0)
    }

    protected abstract SystemPackagingCopySpecVisitor getVisitor();

    class SystemPackagingCopyAction extends CopyActionImpl {
        public SystemPackagingCopyAction(FileResolver resolver, SystemPackagingCopySpecVisitor visitor) {
            super(resolver, visitor);
        }

        SystemPackagingTask getTask() {
            SystemPackagingTask.this
        }

    }

}
