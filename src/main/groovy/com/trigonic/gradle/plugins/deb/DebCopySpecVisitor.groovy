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

package com.trigonic.gradle.plugins.deb

import com.trigonic.gradle.plugins.packaging.AbstractPackagingCopySpecVisitor
import com.trigonic.gradle.plugins.packaging.Dependency
import com.trigonic.gradle.plugins.packaging.Link
import groovy.text.GStringTemplateEngine
import groovy.transform.Canonical
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.time.DateFormatUtils
import org.gradle.api.GradleException
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopySpecImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.vafer.jdeb.Compression
import org.vafer.jdeb.Console
import org.vafer.jdeb.DataProducer
import org.vafer.jdeb.Processor
import org.vafer.jdeb.descriptors.PackageDescriptor
import org.vafer.jdeb.producers.DataProducerLink

/**
 * Forked and modified from org.jamel.pkg4j.gradle.tasks.BuildDebTask
 */
class DebCopySpecVisitor extends AbstractPackagingCopySpecVisitor {
    static final Logger logger = LoggerFactory.getLogger(DebCopySpecVisitor.class)

    private final GStringTemplateEngine engine = new GStringTemplateEngine()

    Deb debTask
    def debianDir
    List<String> dependencies
    List<DataProducer> dataProducers
    List<InstallDir> installDirs
    boolean includeStandardDefines = true

    DebCopySpecVisitor(Deb debTask) {
        super(debTask)
        this.debTask = debTask
        debianDir = new File(debTask.project.buildDir, "debian")
        dependencies = []
        dataProducers = []
        installDirs = []
    }

    @Canonical
    private static class InstallDir {
        String name
        String user
        String group
    }

    @Override
    void startVisit(CopyAction action) {
        super.startVisit(action)

        debianDir.deleteDir()
        debianDir.mkdirs()

    }

    @Override
    void visitFile(FileVisitDetails fileDetails) {
        logger.debug "adding file {}", fileDetails.relativePath.pathString
        def specToLookAt = (spec instanceof CopySpecImpl)?spec:spec.spec // WrapperCopySpec has a nested spec

        def outputFile = extractFile(fileDetails)

        String path = "/" + fileDetails.relativePath.pathString
        String user = lookup(specToLookAt, 'user') ?: debTask.user
        int uid = (int) (lookup(specToLookAt, 'uid') ?: debTask.uid)
        String group = lookup(specToLookAt, 'permissionGroup') ?: debTask.permissionGroup
        int gid = (int) (lookup(specToLookAt, 'gid') ?: debTask.gid)
        int fileMode = (int) (lookup(specToLookAt, 'fileMode') ?: 0)  // TODO see if -1 works for mode

        dataProducers << new DataProducerFileSimple(path, outputFile, user, uid, group, gid, fileMode)
    }

    @Override
    void visitDir(FileVisitDetails dirDetails) {
        def specToLookAt = (spec instanceof CopySpecImpl)?spec:spec.spec // WrapperCopySpec has a nested spec

        def specCreateDirectoryEntry = lookup(specToLookAt, 'createDirectoryEntry')
        boolean createDirectoryEntry = specCreateDirectoryEntry!=null ? specCreateDirectoryEntry : debTask.createDirectoryEntry
        if (createDirectoryEntry) {

            logger.debug "adding directory {}", dirDetails.relativePath.pathString
            def user = lookup(specToLookAt, 'user') ?: debTask.user
            def group = lookup(specToLookAt, 'permissionGroup') ?: debTask.permissionGroup

            dataProducers << new DataProducerDirectorySimple(
                    dirname: "/" + dirDetails.relativePath.pathString,
                    user: user,
                    uid: lookup(specToLookAt, 'uid')?:debTask.uid,
                    group: group,
                    gid: lookup(specToLookAt, 'gid')?: debTask.gid,
                    mode: (lookup(specToLookAt, 'dirMode')?:0) // TODO see if -1 works for mode
            )
            // addParentDirs is implicit in jdeb, I think.
            installDirs << new InstallDir(
                    name: "/" + dirDetails.relativePath.pathString,
                    user: user,
                    group: group,
            )
        }
    }

    @Override
    protected void addLink(Link link) {
        dataProducers << new DataProducerLink(link.path, link.target, true, null, null, null);
    }

    @Override
    protected void addDependency(Dependency dep) {
        dependencies << dep.packageName // Losing version and flag info
    }

    @Override
    protected void end() {
        File debFile = debTask.getArchivePath()

        def context = toContext()
        List<File> debianFiles = new ArrayList<String>();

        debianFiles << generateFile(debianDir, "control", context)

        def installUtils = debTask.allCommonCommands.collect { stripShebang(it) }
        def preInstall = installUtils + debTask.allPreInstallCommands.collect { stripShebang(it) }
        def postInstall = installUtils + debTask.allPostInstallCommands.collect { stripShebang(it) }
        def preUninstall = installUtils + debTask.allPreUninstallCommands.collect { stripShebang(it) }
        def postUninstall = installUtils + debTask.allPostUninstallCommands.collect { stripShebang(it) }

        debianFiles << generateFile(debianDir, "preinst", context + [commands: preInstall] )
        debianFiles << generateFile(debianDir, "postinst", context + [commands:  postInstall] )
        debianFiles << generateFile(debianDir, "prerm", context + [commands:  preUninstall] )
        debianFiles << generateFile(debianDir, "postrm", context + [commands:  postUninstall]  )
        File[] debianFileArray = debianFiles.toArray() as File[]

        def producers = dataProducers.toArray() as DataProducer[]
        def processor = new Processor([
                info: {msg -> logger.info(msg) },
                warn: {msg -> logger.warn(msg) }] as Console, null)

        PackageDescriptor descriptor = createDeb(debianFileArray, debFile, processor, producers)

        // TODO Put changes file into a separate task
        //def changesFile = new File("${packagePath}_all.changes")
        //createChanges(pkg, changesFile, descriptor, processor)

        logger.info 'Created deb {}', debFile
    }

    /**
     * Map to be consumed by generateFile when transforming template
     */
    def Map toContext() {
        [
                name: debTask.getPackageName(),
                version: debTask.getVersion(),
                release: debTask.getRelease(),
                author: debTask.getUser(),
                description: debTask.getPackageDescription(),
                distribution: debTask.getDistribution(),
                summary: debTask.getSummary(),
                section: debTask.getPackageGroup(),
                time: DateFormatUtils.SMTP_DATETIME_FORMAT.format(new Date()),
                epoch: (new Date().getTime() / 1000) as long,
                provides: debTask.getProvides(),
                depends: StringUtils.join(dependencies, ", "),
                url: debTask.getUrl(),
                arch: debTask.getArchString(),

                // Uses install command for directory
                dirs: installDirs.collect { InstallDir dir ->
                    def map = [name: dir.name]
                    if(dir.user) {
                        if (dir.group) {
                            map['owner'] = "${dir.user}:${dir.group}"
                        } else {
                            map['owner'] = dir.user
                        }
                    }
                    return map
                }
        ]
    }

    File generateFile(File debianDir, String fileName, Map context) {
        logger.info("Generating ${fileName} file...")
        def template = getClass().getResourceAsStream("/deb/${fileName}.ftl").newReader()
        def content = engine.createTemplate(template).make(context).toString()
        def contentFile = new File(debianDir, fileName)
        contentFile.text = content
        return contentFile
    }

    private PackageDescriptor createDeb(File[] controlFiles, File debFile, Processor processor, DataProducer[] data) {
        try {
            logger.info("Creating debian package: ${debFile}")
            return processor.createDeb(controlFiles, data, debFile, Compression.GZIP)
        } catch (Exception e) {
            throw new GradleException("Can't build debian package ${debFile}", e)
        }
    }

    /*
    private void createChanges(File changes, File changesFile, PackageDescriptor descriptor, Processor processor) {
        try {
            logger.info("Creating changes file ${changesFile}")
            def changesIn = changes.newInputStream()
            def provider = new TextfileChangesProvider(changesIn, descriptor)
            processor.createChanges(descriptor, provider, null, null, null, changesFile.newOutputStream())
        } catch (Exception e) {
            throw new GradleException("Can't create changes file " + changesFile, e)
        }

        def secRing = new File(pkg.secureRing.replaceFirst("^~", System.getProperty("user.home")))
        if (secRing.exists()) {
            signChangesFile(changesFile, secRing, pkg.key)
        } else {
            logger.info("Secure keyring file does not exists. Changes will not be signed")
        }
    }

    private void signChangesFile(File changesFile, File secRing, String keyId) {
        try {
            logger.info("Signing changes file ${changesFile} with ${secRing} and keyId ${keyId}")
            def secretKey = SigningUtils.readSecretKey(secRing.newInputStream(), keyId)

            if (secretKey) {
                def privateKey = getPrivateKey(secretKey)
                if (privateKey) {
                    InputStream fIn = new ByteArrayInputStream(changesFile.getText("UTF-8").bytes)
                    OutputStream fOut = new FileOutputStream(changesFile, false) // override previous file
                    SigningUtils.sign(fIn, fOut, secretKey, privateKey, PGPUtil.SHA256)
                    fOut.close()
                }
            }
        } catch (Exception e) {
            throw new GradleException("Can't sign changes file " + changesFile, e)
        }
    }

    private static PGPPrivateKey getPrivateKey(PGPSecretKey secretKey) {
        while (true) {
            try {
                String pass = PassphraseProvider.provide()
                if (pass.length() == 0) {
                    PassphraseProvider.remember(pass)
                    return null
                }

                PGPPrivateKey key = SigningUtils.readPrivateKey(secretKey, pass)
                PassphraseProvider.remember(pass)
                return key
            } catch (PGPException e) {
                System.err.println("Invalid passphrase. Please try again")
            }
        }
    }
    */
}
