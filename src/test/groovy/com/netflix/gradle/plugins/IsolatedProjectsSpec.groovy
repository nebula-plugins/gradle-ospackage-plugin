package com.netflix.gradle.plugins

import com.netflix.gradle.plugins.deb.Scanner
import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

/** Project-local daemon DSL regressions; isolation diagnostics remain build failures. */
@Requires({ JavaVersion.current() <= JavaVersion.VERSION_21 })
class IsolatedProjectsSpec extends Specification {
    @TempDir File projectDir

    @Unroll
    def '#profile packaging in #shape with isolated=#isolated on Gradle #testedGradleVersion'() {
        given:
        List<String> projects = fixture(shape, profile)
        List<String> requested = projects.collectMany { path ->
            ['buildDeb', 'buildRpm']
                .collect { task -> "${path}:${task}".toString() }
        }

        when:
        def first = run(testedGradleVersion, isolated, requested)
        def second = run(testedGradleVersion, isolated, requested)

        then:
        stored(first)
        reused(second)
        requested.every { first.task(it).outcome == TaskOutcome.SUCCESS }
        requested.every { second.task(it).outcome == TaskOutcome.UP_TO_DATE }
        projects.every { artifacts(it, '1', 'original') }

        when: 'task input changes without changing the configuration model'
        projects.each { write("${directory(it)}/payload/payload.txt", 'changed') }
        def contentChange = run(testedGradleVersion, isolated, requested)

        then:
        reused(contentChange)
        requested.each { assert contentChange.task(it).outcome == TaskOutcome.SUCCESS: contentChange.output }
        projects.every { artifacts(it, '1', 'changed') }

        when: 'a configuration input changes'
        write('gradle.properties', 'packageRelease=2\n')
        def metadataChange = run(testedGradleVersion, isolated, requested)
        def metadataReuse = run(testedGradleVersion, isolated, requested)

        then:
        stored(metadataChange)
        !metadataChange.output.contains('Reusing configuration cache.')
        reused(metadataReuse)
        requested.every { metadataChange.task(it).outcome == TaskOutcome.SUCCESS }
        projects.every { artifacts(it, '2', 'changed') }

        where:
        [testedGradleVersion, shape, profile, isolated] <<
            [[SupportedGradleVersions.GRADLE_MIN, GradleVersion.current().version].unique(),
             ['single', 'multi', 'nested', 'hierarchy'], ['legacy', 'application-daemon'],
             [false, true]].combinations()
    }

    @Unroll
    def 'root-only application leaves children unenrolled with isolated=#isolated on Gradle #testedGradleVersion'() {
        given:
        fixture('single', 'application-daemon')
        write('settings.gradle', "rootProject.name = 'isolated-packaging'\ninclude 'child'\n")
        write('child/build.gradle', '''
            assert plugins.findPlugin('com.netflix.nebula.ospackage') == null
            assert plugins.findPlugin('com.netflix.nebula.ospackage-application-daemon') == null
            assert !extensions.extraProperties.has('daemon')
            assert !extensions.extraProperties.has('applicationdaemon')
            assert extensions.findByName('ospackage') == null
            assert tasks.findByName('buildDeb') == null
            assert tasks.findByName('buildRpm') == null
        ''')

        when:
        def first = run(testedGradleVersion, isolated, [':buildDeb', ':buildRpm', ':child:help'])
        def second = run(testedGradleVersion, isolated, [':buildDeb', ':buildRpm', ':child:help'])

        then:
        stored(first)
        reused(second)
        artifacts('', '1', 'original')
        !new File(projectDir, 'child/build/distributions').exists()

        where:
        [testedGradleVersion, isolated] <<
            [[SupportedGradleVersions.GRADLE_MIN, GradleVersion.current().version].unique(), [false, true]].combinations()
    }

    @Unroll
    def 'CHARACTERIZATION inherited consumer defaults fail isolation on Gradle #testedGradleVersion'() {
        given: 'plugins are still applied locally; only this legacy customization reads parent state'
        fixture('nested', 'extension')
        write('build.gradle', "ext.inheritedRelease = '1'\n")
        File leaf = new File(projectDir, 'parent/leaf/build.gradle')
        leaf.text = leaf.text.replace("options.release.set(providers.gradleProperty('packageRelease'))",
            'release = inheritedRelease')
        List<String> requested = [':parent:leaf:buildDeb', ':parent:leaf:buildRpm']

        when:
        def ordinary = run(testedGradleVersion, false, requested)
        def ordinaryReuse = run(testedGradleVersion, false, requested)
        def isolated = runner(testedGradleVersion, true, requested).buildAndFail()
        evidence("inherited-defaults-${testedGradleVersion}", isolated)

        then: 'expected failure is a consumer migration requirement, not compatibility evidence'
        stored(ordinary)
        reused(ordinaryReuse)
        artifacts(':parent:leaf', '1', 'original')
        isolated.output.contains('cannot dynamically look up a property in the parent project')
        !isolated.output.contains('Configuration cache entry stored')
        !isolated.output.contains('Configuration cache entry reused')

        where:
        testedGradleVersion << [SupportedGradleVersions.GRADLE_MIN, GradleVersion.current().version].unique()
    }

    @Unroll
    def 'bare #callback callback in #shape preserves artifacts and strict isolation on Gradle #testedGradleVersion'() {
        given:
        List<String> projects = fixture(shape, 'application-daemon')
        projects.each { path ->
            File script = new File(projectDir, "${directory(path)}/build.gradle")
            if (callback == 'daemon') {
                script << "\ndaemon.call { daemonName = 'extra'; command = 'sleep infinity'; " +
                    "logCommand = providers.gradleProperty('daemonLogCommand').get() }\n"
            } else {
                script.text = script.text.replace('applicationdaemon {', 'applicationdaemon.call {')
                    .replace("user = '${path.tokenize(':').last()}-user'",
                        "user = '${path.tokenize(':').last()}-user'; logCommand = providers.gradleProperty('daemonLogCommand').get()")
            }
            // Real template files allow input changes while reusing the configured model.
            ['run', 'log-run', 'initd', 'postInstall'].each { template ->
                write("${directory(path)}/templates/${template}.tpl",
                    getClass().getResource("/com/netflix/gradle/plugins/daemon/${template}.tpl").text)
            }
            script << "\ndaemonsTemplates.folder = 'templates'\n"
        }
        List<String> requested = projects.collectMany { ["${it}:buildDeb".toString(), "${it}:buildRpm".toString()] }

        when: 'explicit Closure calls establish the ordinary packaging reference'
        def explicit = run(testedGradleVersion, false, requested)
        def explicitReuse = run(testedGradleVersion, false, requested)
        def reference = projects.collectEntries { [(it): daemonArtifacts(it)] }
        projects.each { path ->
            File script = new File(projectDir, "${directory(path)}/build.gradle")
            script.text = script.text.replace("${callback}.call {", "${callback} {")
        }
        def ordinary = run(testedGradleVersion, false, requested)
        def ordinaryReuse = run(testedGradleVersion, false, requested)

        then:
        stored(explicit)
        reused(explicitReuse)
        stored(ordinary)
        reused(ordinaryReuse)
        projects.every { daemonArtifacts(it) == reference[it] }

        when: 'the bare DSL is used under strict Isolated Projects'
        def isolated = run(testedGradleVersion, true, requested)
        def isolatedReuse = run(testedGradleVersion, true, requested)

        then:
        stored(isolated)
        reused(isolatedReuse)
        requested.every { isolatedReuse.task(it).outcome == TaskOutcome.UP_TO_DATE }
        projects.every { artifacts(it, '1', 'original') && daemonArtifacts(it) == reference[it] }

        when: 'payload and template inputs change without reconfiguring'
        projects.each { path ->
            write("${directory(path)}/payload/payload.txt", 'changed')
            new File(projectDir, "${directory(path)}/templates/run.tpl") << '\n# changed template\n'
        }
        def changed = run(testedGradleVersion, true, requested)

        then:
        reused(changed)
        requested.every { changed.task(it).outcome == TaskOutcome.SUCCESS }
        projects.every { path ->
            assert artifacts(path, '1', 'changed')
            def snapshot = daemonArtifacts(path)
            assert snapshot != reference[path]
            assert snapshot.templates.findAll { it.key.endsWith('/run') }.every { it.value.contains('# changed template') }
            assert snapshot.debScripts.findAll { it.key.endsWith('/run') && !it.key.contains('/log/') }
                .every { it.value.contains('# changed template') }
            assert snapshot.rpmScripts.findAll { it.key.endsWith('/run') && !it.key.contains('/log/') }
                .every { it.value.contains('# changed template') }
            true
        }

        when: 'daemon configuration and package metadata change'
        write('gradle.properties', 'packageRelease=2\ndaemonLogCommand=svlogd /var/log/service\n')
        def configured = run(testedGradleVersion, true, requested)
        def configuredReuse = run(testedGradleVersion, true, requested)

        then:
        stored(configured)
        !configured.output.contains('Reusing configuration cache.')
        reused(configuredReuse)
        requested.every { configured.task(it).outcome == TaskOutcome.SUCCESS }
        requested.every { configuredReuse.task(it).outcome == TaskOutcome.UP_TO_DATE }
        projects.every { path ->
            assert artifacts(path, '2', 'changed')
            String daemonName = callback == 'daemon' ? 'extra' : path.tokenize(':').last()
            def snapshot = daemonArtifacts(path, '2')
            assert snapshot.templates["${daemonName.capitalize()}/buildDeb/log-run".toString()].contains('svlogd /var/log/service')
            assert snapshot.debScripts["./service/${daemonName}/log/run".toString()].contains('svlogd /var/log/service')
            assert snapshot.rpmScripts["./service/${daemonName}/log/run".toString()].contains('svlogd /var/log/service')
            true
        }

        where:
        [testedGradleVersion, shape, callback] <<
            [[SupportedGradleVersions.GRADLE_MIN, GradleVersion.current().version].unique(),
             ['multi', 'nested'], ['applicationdaemon', 'daemon']].combinations()
    }

    @Unroll
    def 'local Maven publication preserves package artifacts with isolated=#isolated on Gradle #testedGradleVersion'() {
        given:
        fixture('nested', 'extension')
        publication()
        List<String> requested = [':parent:leaf:publish']

        when:
        def first = run(testedGradleVersion, isolated, requested)
        def second = run(testedGradleVersion, isolated, requested)
        write('parent/leaf/payload/payload.txt', 'published change')
        def changed = run(testedGradleVersion, isolated, requested)

        then:
        stored(first)
        reused(second)
        reused(changed)
        first.task(':parent:leaf:buildDeb').outcome == TaskOutcome.SUCCESS
        first.task(':parent:leaf:buildRpm').outcome == TaskOutcome.SUCCESS
        changed.task(':parent:leaf:buildDeb').outcome == TaskOutcome.SUCCESS
        changed.task(':parent:leaf:buildRpm').outcome == TaskOutcome.SUCCESS
        artifacts(':parent:leaf', '1', 'published change')
        ['deb', 'rpm'].every { extension ->
            File published = new File(projectDir, "parent/leaf/build/repository/example/leaf/1.2.3/leaf-1.2.3.${extension}")
            File packaged = new File(projectDir, 'parent/leaf/build/distributions').listFiles().find { it.name.endsWith(".${extension}") }
            assert published.isFile()
            assert Arrays.equals(published.bytes, packaged.bytes)
            true
        }

        where:
        [testedGradleVersion, isolated] <<
            [[SupportedGradleVersions.GRADLE_MIN, GradleVersion.current().version].unique(), [false, true]]
                .combinations().findAll { version, isolated -> version != '8.6' || !isolated }
    }

    def 'CHARACTERIZATION Gradle 8.6 nested Maven publication fails isolation even without ospackage'() {
        given:
        fixture('nested', 'extension')
        publication()
        List<String> requested = [':parent:leaf:publish']

        when:
        def packaging = runner('8.6', true, requested).buildAndFail()
        evidence('maven-packaging-8.6', packaging)
        write('parent/leaf/build.gradle', '''
            plugins { id 'maven-publish' }
            group = 'example'
            version = '1.2.3'
            publishing {
                publications {
                    os(MavenPublication) {
                        artifactId = 'leaf'
                        artifact file('payload/payload.txt')
                    }
                }
                repositories { maven { url = layout.buildDirectory.dir('repository') } }
            }
        ''')
        def coreOnly = runner('8.6', true, requested).buildAndFail()
        evidence('maven-core-only-8.6', coreOnly)

        then: 'this integration limitation also exists in the Gradle core control'
        [packaging, coreOnly].every {
            it.output.contains("Cannot access project ':' from project ':parent:leaf'") &&
                !it.output.contains('Configuration cache entry stored') &&
                !it.output.contains('Configuration cache entry reused')
        }
    }

    private void publication() {
        File leaf = new File(projectDir, 'parent/leaf/build.gradle')
        leaf.text = leaf.text.replace('plugins {', "plugins { id 'maven-publish';") + '''
            group = 'example'
            publishing {
                publications {
                    os(MavenPublication) {
                        groupId = 'example'
                        artifactId = 'leaf'
                        version = '1.2.3'
                        artifact tasks.named('buildDeb')
                        artifact tasks.named('buildRpm')
                    }
                }
                repositories { maven { url = layout.buildDirectory.dir('repository') } }
            }
        '''
    }

    private List<String> fixture(String shape, String profile) {
        def projects = shape == 'single' ? [''] : shape == 'multi' ? [':alpha', ':beta'] :
            shape == 'nested' ? [':parent:leaf'] : ['', ':parent', ':parent:leaf']
        write('settings.gradle', "rootProject.name = 'isolated-packaging'\n" +
            (shape == 'single' ? '' : "include ${projects.findAll { it }.collect { "'${it.substring(1)}'" }.join(', ')}\n"))
        write('build.gradle', '')
        if (shape == 'nested') write('parent/build.gradle', '')
        write('gradle.properties', 'packageRelease=1\ndaemonLogCommand=multilog t ./main\n')
        projects.each { path ->
            String name = path ? path.tokenize(':').last() : 'single'
            String configuration = """
                packageName = '${name}'
                requires('bash')
                options.release.set(providers.gradleProperty('packageRelease'))
                from('payload') { into '/opt/${name}' }
            """
            String plugins = profile == 'application-daemon' ? "id 'com.netflix.nebula.ospackage-application-daemon'" :
                "id 'com.netflix.nebula.ospackage'"
            String extra = profile == 'application-daemon' ? """
                application {
                    mainClass = 'example.Main'
                    applicationName = '${name}'
                }
                ospackage_application { prefix = '/srv' }
                applicationdaemon { user = '${name}-user' }
            """ : profile == 'legacy' ? """
                tasks.named('buildDeb').get()
                tasks.named('buildRpm').get()
                afterEvaluate {
                    ospackage.conventionMapping.map('release') { providers.gradleProperty('packageRelease').get() }
                }
            """ : ''
            if (profile == 'legacy') {
                configuration = configuration.replace("options.release.set(providers.gradleProperty('packageRelease'))", '')
            }
            String script = """
                plugins { ${plugins} }
                version = '1.2.3'
                description = '${name} package'
                ospackage { ${configuration} }
                ${extra}
            """
            write("${directory(path)}/build.gradle", script.stripIndent())
            write("${directory(path)}/payload/payload.txt", 'original')
            if (profile == 'application-daemon') {
                write("${directory(path)}/src/main/java/example/Main.java", '''
                    package example;
                    public class Main { public static void main(String[] args) { } }
                ''')
            }
        }
        projects
    }

    private BuildResult run(String version, boolean isolated, List<String> tasks) {
        runner(version, isolated, tasks).build()
    }

    private GradleRunner runner(String version, boolean isolated, List<String> tasks) {
        List<String> arguments = tasks + ['--configuration-cache', '--configuration-cache-problems=fail',
                                          '--no-build-cache', '--stacktrace', '--max-workers=2']
        // Both 8.6 and the 9.5.1 wrapper predate the public flag introduced in 9.7.
        if (isolated) arguments.add(GradleVersion.version(version).baseVersion >= GradleVersion.version('9.7') ?
            '--isolated-projects' : '-Dorg.gradle.unsafe.isolated-projects=true')
        GradleRunner.create().withProjectDir(projectDir).withGradleVersion(version)
            .withPluginClasspath().withArguments(arguments)
    }

    private boolean artifacts(String path, String release, String content) {
        String name = path ? path.tokenize(':').last() : 'single'
        File base = new File(projectDir, directory(path))
        File deb = new File(base, "build/distributions/${name}_1.2.3-${release}_all.deb")
        File rpm = new File(base, "build/distributions/${name}-1.2.3-${release}.noarch.rpm")
        assert deb.isFile()
        assert rpm.isFile()
        File unpacked = new File(base, "unpacked-${release}")
        def scan = new Scanner(deb, unpacked)
        assert scan.getHeaderEntry('Package') == name
        assert scan.getHeaderEntry('Version') == "1.2.3-${release}"
        assert new File(unpacked, "opt/${name}/payload.txt").text == content
        def rpmScan = com.netflix.gradle.plugins.rpm.Scanner.scan(rpm)
        assert rpmScan.files.find { it.name.endsWith("opt/${name}/payload.txt") }.asString() == content
        File script = new File(base, 'build.gradle')
        if (script.text.contains('ospackage-application-daemon')) {
            assert new File(unpacked, "srv/${name}/bin/${name}").isFile()
            String jarName = path ? name : 'isolated-packaging'
            assert new File(unpacked, "srv/${name}/lib/${jarName}-1.2.3.jar").isFile()
            assert new File(unpacked, "service/${name}/run").text.contains("/srv/${name}/bin/${name}")
            assert new File(unpacked, "service/${name}/run").text.contains("${name}-user")
            assert scan.controlContents['./postinst'].contains("update-rc.d ${name}")
        }
        true
    }

    private Map daemonArtifacts(String path, String release = '1') {
        File base = new File(projectDir, directory(path))
        String packageName = path ? path.tokenize(':').last() : 'single'
        File distributions = new File(base, 'build/distributions')
        File unpacked = new File(base, 'comparison')
        def deb = new Scanner(new File(distributions, "${packageName}_1.2.3-${release}_all.deb"), unpacked)
        def rpm = com.netflix.gradle.plugins.rpm.Scanner.scan(new File(distributions, "${packageName}-1.2.3-${release}.noarch.rpm"))
        Map rpmPayload = rpm.files.collectEntries { file ->
            def buffer = file.contents?.duplicate()
            byte[] content = buffer == null ? null : new byte[buffer.remaining()]
            if (buffer != null) buffer.get(content)
            [(file.name): [file.mode, content?.toList()]]
        }
        Map templates = [:]
        File daemonDir = new File(base, 'build/daemon')
        daemonDir.eachFileRecurse { file ->
            if (file.isFile()) templates[daemonDir.toPath().relativize(file.toPath()).toString()] = file.text
        }
        [templates: templates, debHeaders: deb.headerFields, debControl: deb.controlContents,
         debPayload: deb.dataContents.collectEntries { entry, file ->
             [(entry.name): [entry.mode, entry.userId, entry.groupId, entry.userName, entry.groupName,
                             entry.isFile() ? file.bytes.toList() : null]]
         },
         rpmHeaders: [name: 1000, version: 1001, release: 1002, summary: 1004, description: 1005,
                      postInstall: 1024, fileUsers: 1039, fileGroups: 1040,
                      provides: 1047, requires: 1049, requireFlags: 1048, requireVersions: 1050]
             .collectEntries { name, tag -> [(name): rpm.format.header.entries[tag]?.values?.toList()]
         },
         debScripts: deb.dataContents.findAll { entry, file -> entry.isFile() &&
             (entry.name.startsWith('./service/') || entry.name.startsWith('./etc/')) }
             .collectEntries { entry, file -> [(entry.name): file.text] },
         rpmScripts: rpmPayload.findAll { name, entry -> name.startsWith('./service/') || name.startsWith('./etc/') }
             .collectEntries { name, entry -> [(name): entry[1] == null ? null : new String(entry[1] as byte[], 'UTF-8')] },
         rpmPayload: rpmPayload]
    }

    private static boolean stored(BuildResult result) {
        assert result.output.contains('Configuration cache entry stored')
        true
    }

    private static boolean reused(BuildResult result) {
        assert result.output.contains('Configuration cache entry reused')
        true
    }

    private static String directory(String path) { path ? path.substring(1).replace(':', '/') : '.' }

    private static void evidence(String name, BuildResult result) {
        File report = new File("build/reports/isolated-projects/${name}.log")
        report.parentFile.mkdirs()
        report.text = result.output
    }

    private File write(String path, String text) {
        File target = new File(projectDir, path)
        target.parentFile.mkdirs()
        target.text = text
        target
    }
}
