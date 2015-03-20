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

import com.netflix.gradle.plugins.deb.control.MultiArch
import com.netflix.gradle.plugins.deb.filevisitor.DebFileVisitorStrategyFactory
import com.netflix.gradle.plugins.deb.validation.DebVersionValidator
import com.netflix.gradle.plugins.deb.validation.VersionValidator
import com.netflix.gradle.plugins.packaging.AbstractPackagingCopyAction
import com.netflix.gradle.plugins.packaging.Dependency
import com.netflix.gradle.plugins.packaging.Directory
import com.netflix.gradle.plugins.packaging.Link
import groovy.transform.Canonical
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.vafer.jdeb.Compression
import org.vafer.jdeb.Console
import org.vafer.jdeb.DataProducer
import org.vafer.jdeb.DebMaker
import org.vafer.jdeb.producers.DataProducerLink

/**
 * Forked and modified from org.jamel.pkg4j.gradle.tasks.BuildDebTask
 */
class DebCopyAction extends AbstractPackagingCopyAction {
    static final Logger logger = LoggerFactory.getLogger(DebCopyAction.class)

    Deb debTask
    def debianDir
    List<String> dependencies
    List<String> conflicts
    List<String> recommends
    List<String> suggests
    List<String> enhances
    List<String> preDepends
    List<String> breaks
    List<String> replaces
    List<DataProducer> dataProducers
    List<InstallDir> installDirs
    boolean includeStandardDefines = true
    TemplateHelper templateHelper
    VersionValidator versionValidator = new DebVersionValidator()
    private DebFileVisitorStrategyFactory debFileVisitorStrategyFactory

    DebCopyAction(Deb debTask) {
        super(debTask)
        this.debTask = debTask
        validateUserInput(debTask)
        dependencies = []
        conflicts = []
        recommends = []
        suggests = []
        enhances = []
        preDepends = []
        breaks = []
        replaces = []
        dataProducers = []
        installDirs = []
        debianDir = new File(debTask.project.buildDir, "debian")
        templateHelper = new TemplateHelper(debianDir, '/deb')
        debFileVisitorStrategyFactory = new DebFileVisitorStrategyFactory(dataProducers, installDirs)
    }

    private void validateUserInput(Deb debTask) {
        if(debTask.getVersion() != 'unspecified' && !versionValidator.validate(debTask.getVersion())) {
            throw new InvalidUserDataException("Invalid upstream version '${debTask.getVersion()}' - a valid version must start with a digit and only contain [A-Za-z0-9.+:~-]")
        }
    }

    @Canonical
    static class InstallDir {
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

        String user = lookup(specToLookAt, 'user') ?: debTask.user
        int uid = (int) (lookup(specToLookAt, 'uid') ?: debTask.uid)
        String group = lookup(specToLookAt, 'permissionGroup') ?: debTask.permissionGroup
        int gid = (int) (lookup(specToLookAt, 'gid') ?: debTask.gid)

        int fileMode = fileDetails.mode

        debFileVisitorStrategyFactory.strategy.addFile(fileDetails, inputFile, user, uid, group, gid, fileMode)
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

            int fileMode = dirDetails.mode

            debFileVisitorStrategyFactory.strategy.addDirectory(dirDetails, user, uid, group, gid, fileMode)
        }
    }

    @Override
    protected void addLink(Link link) {
        dataProducers << new DataProducerLink(link.path, link.target, true, null, null, null);
    }

    @Override
    protected void addDependency(Dependency dep) {
        dependencies << dep.toDebString()
    }

    @Override
    protected void addConflict(Dependency dep) {
        conflicts << dep.toDebString()
    }

    @Override
    protected void addObsolete(Dependency dep) {
        logger.warn "Obsoletes functionality not implemented for deb files"
    }

    protected void addRecommends(Dependency dep) {
        recommends << dep.toDebString()
    }

    protected void addSuggests(Dependency dep) {
        suggests << dep.toDebString()
    }

    protected void addEnhances(Dependency dep) {
        enhances << dep.toDebString()
    }

    protected void addPreDepends(Dependency dep) {
        preDepends << dep.toDebString()
    }

    protected void addBreaks(Dependency dep) {
        breaks << dep.toDebString()
    }

    protected void addReplaces(Dependency dep) {
        replaces << dep.toDebString()
    }

    @Override
    protected void addDirectory(Directory directory) {
        logger.warn "Directory functionality not implemented for deb files"
    }

    protected String getMultiArch() {
        def archString = debTask.getArchString()
        def multiArch = debTask.getMultiArch()
        if (('all' == archString) && (MultiArch.SAME == multiArch)) {
            throw new IllegalArgumentException('Deb packages with Architecture: all cannot declare Multi-Arch: same')
        }
        def multiArchString = multiArch?.name()?.toLowerCase() ?: ''
        return multiArchString
    }

    @Override
    protected void end() {
        for (Dependency recommends : task.getAllRecommends()) {
            logger.debug "adding recommends on {} {}", recommends.packageName, recommends.version
            addRecommends(recommends)
        }

        for (Dependency suggests : task.getAllSuggests()) {
            logger.debug "adding suggests on {} {}", suggests.packageName, suggests.version
            addSuggests(suggests)
        }

        for (Dependency enhances : task.getAllEnhances()) {
            logger.debug "adding enhances on {} {}", enhances.packageName, enhances.version
            addEnhances(enhances)
        }

        for (Dependency preDepends : task.getAllPreDepends()) {
            logger.debug "adding preDepends on {} {}", preDepends.packageName, preDepends.version
            addPreDepends(preDepends)
        }

        for (Dependency breaks : task.getAllBreaks()) {
            logger.debug "adding breaks on {} {}", breaks.packageName, breaks.version
            addBreaks(breaks)
        }

        for (Dependency replaces : task.getAllReplaces()) {
            logger.debug "adding replaces on {} {}", replaces.packageName, replaces.version
            addReplaces(replaces)
        }

        File debFile = debTask.getArchivePath()

        def context = toContext()
        List<File> debianFiles = new ArrayList<File>();

        debianFiles << templateHelper.generateFile("control", context)

        def installUtils = debTask.allCommonCommands.collect { stripShebang(it) }
        def preInstall = installUtils + debTask.allPreInstallCommands.collect { stripShebang(it) }
        def postInstall = installUtils + debTask.allPostInstallCommands.collect { stripShebang(it) }
        def preUninstall = installUtils + debTask.allPreUninstallCommands.collect { stripShebang(it) }
        def postUninstall = installUtils + debTask.allPostUninstallCommands.collect { stripShebang(it) }

        def addlFiles = [preinst: preInstall, postinst: postInstall, prerm: preUninstall, postrm:postUninstall]
                .collect {
                    templateHelper.generateFile(it.key, context + [commands: it.value] )
                }
        debianFiles.addAll(addlFiles)

        DebMaker maker = new DebMaker(new GradleLoggerConsole(), dataProducers, null)
        File contextFile = templateHelper.generateFile("control", context)
        maker.setControl(contextFile.parentFile)
        maker.setDeb(debFile)

        logger.info("Creating debian package: ${debFile}")

        try {
            logger.info("Creating debian package: ${debFile}")
            maker.createDeb(Compression.GZIP)
        } catch (Exception e) {
            throw new GradleException("Can't build debian package ${debFile}", e)
        }

        // TODO Put changes file into a separate task
        //def changesFile = new File("${packagePath}_all.changes")
        //createChanges(pkg, changesFile, descriptor, processor)

        logger.info 'Created deb {}', debFile
    }

    private static class GradleLoggerConsole implements Console {
        @Override
        void debug(String message) {
            logger.warn(message)
        }

        @Override
        void info(String message) {
            logger.info(message)
        }

        @Override
        void warn(String message) {
            logger.warn(message)
        }
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
                provides: debTask.getProvides(),
                depends: StringUtils.join(dependencies, ", "),
                url: debTask.getUrl(),
                arch: debTask.getArchString(),
                multiArch: getMultiArch(),
                conflicts: StringUtils.join(conflicts, ", "),
                recommends: StringUtils.join(recommends, ", "),
                suggests: StringUtils.join(suggests, ", "),
                enhances: StringUtils.join(enhances, ", " ),
                preDepends: StringUtils.join(preDepends, ", "),
                breaks: StringUtils.join(breaks, ", "),
                replaces: StringUtils.join(replaces, ", "),

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
