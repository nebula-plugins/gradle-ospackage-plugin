package com.netflix.gradle.plugins

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

@Requires({ JavaVersion.current() <= JavaVersion.VERSION_21 })
class DaemonDslCompatibilitySpec extends Specification {
    @TempDir File projectDir

    @Unroll
    def 'daemon keeps closure access delegation returns and replacements with isolated=#isolated on Gradle #version'() {
        expect:
        run(version, isolated, '''
            plugins { id 'com.netflix.nebula.ospackage-daemon' }
            import com.netflix.gradle.plugins.daemon.DaemonDefinition

            def original = extensions.extraProperties.get('daemon')
            assert original instanceof Closure
            assert daemon.is(original)
            def scriptCommand = 'sleep infinity'
            def makeBody = { String name ->
                { DaemonDefinition definition ->
                    assert definition instanceof DaemonDefinition
                    daemonName = name
                    command = scriptCommand
                    user = 'service'
                    'ignored body return'
                }
            }
            def body = makeBody('bare')
            def state = [body.owner, body.delegate, body.thisObject, body.resolveStrategy]
            def bare = daemon(body)
            assert bare instanceof DaemonDefinition
            assert [bare.daemonName, bare.command, bare.user] == ['bare', scriptCommand, 'service']
            assert [body.owner, body.delegate, body.thisObject, body.resolveStrategy] == state
            assert daemons.daemons.any { it.is(bare) }
            def explicit = daemon.call(makeBody('explicit'))
            assert [explicit.command, explicit.user] == [bare.command, bare.user]

            // Resolve the current extra property on every call, including wrappers of the original.
            def replacement = { Closure conf ->
                def result = original.call(conf)
                result.command += ' wrapped'
                result
            }
            ext.daemon = replacement
            assert daemon.is(replacement)
            assert daemon(makeBody('replaced')).command == scriptCommand + ' wrapped'
            assert daemon.call(makeBody('explicit-replaced')).command == scriptCommand + ' wrapped'
            assert daemons.daemons.size() == 4
        ''')

        where:
        [version, isolated] << [[SupportedGradleVersions.GRADLE_MIN, GradleVersion.current().version].unique(),
                                [false, true]].combinations()
    }

    @Unroll
    def 'applicationdaemon keeps delayed ordered configuration and list return with isolated=#isolated on Gradle #version'() {
        expect:
        run(version, isolated, '''
            plugins { id 'com.netflix.nebula.ospackage-application-daemon' }
            import com.netflix.gradle.plugins.daemon.DaemonDefinition

            def original = extensions.extraProperties.get('applicationdaemon')
            assert original instanceof Closure
            assert applicationdaemon.is(original)
            def seen = []
            def first = { DaemonDefinition definition ->
                seen << definition
                assert daemonName == 'late-name'
                assert command == '/custom/late-name/bin/late-name'
                user = 'first'
            }
            def state = [first.owner, first.delegate, first.thisObject, first.resolveStrategy]
            def queued = applicationdaemon(first)
            assert queued instanceof List
            assert queued.size() == 1 && queued[0].is(first)
            def second = { user = 'second' }
            assert applicationdaemon.call(second).is(queued)

            def replacement = { Closure conf -> original.call(conf); 'consumer return' }
            ext.applicationdaemon = replacement
            assert applicationdaemon.is(replacement)
            def third = { assert user == 'second'; user = 'third' }
            assert applicationdaemon(third) == 'consumer return'
            assert queued.size() == 3 && queued[2].is(third)
            assert applicationdaemon.call { assert user == 'third'; logUser = 'logger' } == 'consumer return'
            assert seen.empty
            assert daemons.daemons.empty
            application { applicationName = 'late-name'; mainClass = 'example.Main' }
            ospackage_application { prefix = '/custom' }
            afterEvaluate {
                assert seen.size() == 1
                assert daemons.daemons.size() == 1
                def definition = daemons.daemons.iterator().next()
                assert definition.is(seen[0])
                assert definition.user == 'third' && definition.logUser == 'logger'
                assert [first.owner, first.delegate, first.thisObject, first.resolveStrategy] == state
            }
        ''')

        where:
        [version, isolated] << [[SupportedGradleVersions.GRADLE_MIN, GradleVersion.current().version].unique(),
                                [false, true]].combinations()
    }

    @Unroll
    def 'script methods retain precedence over project callback dispatch with isolated=#isolated on Gradle #version'() {
        expect:
        run(version, isolated, '''
            plugins { id 'com.netflix.nebula.ospackage-application-daemon' }
            def daemon(Closure conf) { 'script daemon' }
            def applicationdaemon(Closure conf) { 'script applicationdaemon' }
            assert daemon { throw new AssertionError() } == 'script daemon'
            assert applicationdaemon { throw new AssertionError() } == 'script applicationdaemon'
            assert extensions.extraProperties.get('daemon') instanceof Closure
            assert extensions.extraProperties.get('applicationdaemon') instanceof Closure
        ''')

        where:
        [version, isolated] << [[SupportedGradleVersions.GRADLE_MIN, GradleVersion.current().version].unique(),
                                [false, true]].combinations()
    }

    @Unroll
    def 'existing project methods retain precedence with isolated=#isolated on Gradle #version'() {
        expect:
        run(version, isolated, '''
            plugins { id 'com.netflix.nebula.ospackage-application-daemon' apply false }
            project.metaClass.daemon = { Closure conf -> 'consumer daemon' }
            project.metaClass.applicationdaemon = { Closure conf -> 'consumer applicationdaemon' }
            apply plugin: 'com.netflix.nebula.ospackage-application-daemon'
            assert daemon { throw new AssertionError() } == 'consumer daemon'
            assert applicationdaemon { throw new AssertionError() } == 'consumer applicationdaemon'
            assert extensions.extraProperties.get('daemon') instanceof Closure
            assert extensions.extraProperties.get('applicationdaemon') instanceof Closure
        ''')

        where:
        [version, isolated] << [[SupportedGradleVersions.GRADLE_MIN, GradleVersion.current().version].unique(),
                                [false, true]].combinations()
    }

    private boolean run(String version, boolean isolated, String script) {
        new File(projectDir, 'settings.gradle').text = "rootProject.name = 'daemon-contract'" +
            (isolated ? "\ninclude 'parent:leaf'" : '')
        File participant = isolated ? new File(projectDir, 'parent/leaf') : projectDir
        participant.mkdirs()
        new File(participant, 'build.gradle').text = script.stripIndent()
        List<String> arguments = [isolated ? ':parent:leaf:help' : 'help', '--configuration-cache',
                                  '--configuration-cache-problems=fail', '--stacktrace']
        if (isolated) arguments.add('-Dorg.gradle.unsafe.isolated-projects=true')
        def result = GradleRunner.create().withProjectDir(projectDir).withGradleVersion(version)
            .withPluginClasspath().withArguments(arguments).build()
        assert result.output.contains('Configuration cache entry stored')
        true
    }
}
