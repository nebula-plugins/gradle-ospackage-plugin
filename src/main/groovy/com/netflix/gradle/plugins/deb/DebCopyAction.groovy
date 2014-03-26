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
import com.netflix.gradle.plugins.packaging.Link
import groovy.text.GStringTemplateEngine
import groovy.transform.Canonical
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.freecompany.redline.header.Flags
import org.gradle.api.GradleException
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
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
class DebCopyAction extends AbstractPackagingCopyAction {
    static final Logger logger = LoggerFactory.getLogger(DebCopyAction.class)

    private final GStringTemplateEngine engine = new GStringTemplateEngine()

    Deb debTask
    def debianDir
    List<String> dependencies
    List<DataProducer> dataProducers
    List<InstallDir> installDirs
    boolean includeStandardDefines = true

    DebCopyAction(Deb debTask) {
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
/*
    boolean useJavaNioForMask() {
        try {
            Class.forName('java.nio.file.attribute.PosixFilePermission')
            return true
        } catch(Exception e) {
            // Java 7 Classes weren't available
            return false
        }

    }

    int fileModeFromFile(File file) {
        if (useJavaNioForMask()) {
            return fileModeUsingPosix(file)
        } else {
            return fileModeUsingShell(file)
        }
    }

    int fileModeUsingShell(File file) {
        // "ls -l post2crucible | awk '{k=0;for(i=0;i<=8;i++)k+=((substr(\$1,i+2,1)~/[rwx]/)*2^(8-i));if(k)printf(\"%0o \",k)}'"
        throw new IllegalStateException("Unable to determine permission mask, try using Java 7")
    }

    int fileModeUsingPosix(File file) {
        try {
            Set<PosixFilePermission> filePerm = Files.getPosixFilePermissions(file.toPath());
            UnixFileModeAttribute.toUnixMode(filePerm)
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
*/
    @Override
    void visitFile(FileCopyDetailsInternal fileDetails, def specToLookAt) {
        logger.debug "adding file {}", fileDetails.relativePath.pathString

        def inputFile = extractFile(fileDetails)

        String path = "/" + fileDetails.relativePath.pathString
        String user = lookup(specToLookAt, 'user') ?: debTask.user
        int uid = (int) (lookup(specToLookAt, 'uid') ?: debTask.uid)
        String group = lookup(specToLookAt, 'permissionGroup') ?: debTask.permissionGroup
        int gid = (int) (lookup(specToLookAt, 'gid') ?: debTask.gid)

        Integer specFileMode = lookup(specToLookAt, 'fileMode') // Integer to allow for null
        int fileMode = (int) (specFileMode?:fileDetails.mode)

        dataProducers << new DataProducerFileSimple(path, inputFile, user, uid, group, gid, fileMode)
    }

    @Override
    void visitDir(FileCopyDetailsInternal dirDetails, def specToLookAt) {
        def specCreateDirectoryEntry = lookup(specToLookAt, 'createDirectoryEntry')
        boolean createDirectoryEntry = specCreateDirectoryEntry!=null ? specCreateDirectoryEntry : debTask.createDirectoryEntry
        if (createDirectoryEntry) {

            logger.debug "adding directory {}", dirDetails.relativePath.pathString
            String user = lookup(specToLookAt, 'user') ?: debTask.user
            int uid = (int) (lookup(specToLookAt, 'uid') ?: debTask.uid)
            String group = lookup(specToLookAt, 'permissionGroup') ?: debTask.permissionGroup
            int gid = (int) (lookup(specToLookAt, 'gid') ?: debTask.gid)

            Integer specFileMode = lookup(specToLookAt, 'fileMode') // Integer to allow for null
            int fileMode = (int) (specFileMode?:dirDetails.mode)

            String dirName =  "/" + dirDetails.relativePath.pathString
            dataProducers << new DataProducerDirectorySimple(dirName,user,uid,group,gid,fileMode)

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

    def signMap = [
            (Flags.GREATER|Flags.EQUAL): '>=',
            (Flags.LESS|Flags.EQUAL):    '<=',
            (Flags.EQUAL):               '==',
            (Flags.GREATER):             '>>',
            (Flags.LESS):                '<<'
    ]
    @Override
    protected void addDependency(Dependency dep) {
        // Depends: e2fsprogs (>= 1.27-2), libc6 (>= 2.2.4-4).
        def depStr = dep.packageName
        if (dep.flag && dep.version) {
            def sign = signMap[dep.flag]
            if (sign==null) {
                throw new IllegalArgumentException()
            }
            depStr += " (${sign} ${dep.version})"
        } else if (dep.version) {
            depStr += " (${dep.version})"
        }
        dependencies << depStr
    }

    @Override
    protected void end() {
        File debFile = debTask.getArchivePath()

        def context = toContext()
        List<File> debianFiles = new ArrayList<String>();

        debianFiles << generateFile(debianDir, "control", context)

        def configurationFiles = debTask.allConfigurationFiles
        if (configurationFiles.any()) {
            debianFiles << generateFile(debianDir, "conffiles", context + [files: configurationFiles] )
        }

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
                maintainer: debTask.getMaintainer(),
                uploaders: debTask.getUploaders(),
                priority: debTask.getPriority(),
                description: debTask.getPackageDescription(),
                distribution: debTask.getDistribution(),
                summary: debTask.getSummary(),
                section: debTask.getPackageGroup(),
                time: DateFormatUtils.SMTP_DATETIME_FORMAT.format(new Date()),
                provides: debTask.getProvides(),
                depends: StringUtils.join(dependencies, ", "),
                url: debTask.getUrl(),
                arch: debTask.getArch(),

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
