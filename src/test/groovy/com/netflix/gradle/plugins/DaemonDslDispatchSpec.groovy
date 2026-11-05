package com.netflix.gradle.plugins

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

/** Establish the local bridge's precedence before relying on it in the plugins. */
@Requires({ JavaVersion.current() <= JavaVersion.VERSION_21 })
class DaemonDslDispatchSpec extends Specification {
    @TempDir File projectDir

    @Unroll
    def 'local method dispatch preserves #callback closure overrides on Gradle #version'() {
        given:
        new File(projectDir, 'settings.gradle').text = "include 'parent:leaf'"
        File leaf = new File(projectDir, 'parent/leaf')
        leaf.mkdirs()
        new File(leaf, 'build.gradle').text = '''
            def received = []
            def original = { Closure body -> received << body; 'original' }
            ext.CALLBACK = original
            def extras = extensions.extraProperties
            project.metaClass.CALLBACK = { Closure body -> extras.get('CALLBACK').call(body) }

            def body = { throw new AssertionError('Must not configure the argument') }
            assert CALLBACK(body) == 'original'
            assert received == [body]
            assert CALLBACK.is(original)
            assert CALLBACK.call(body) == 'original'

            def replacement = { Closure arg -> assert arg.is(body); 'replacement' }
            ext.CALLBACK = replacement
            assert CALLBACK(body) == 'replacement'
            assert CALLBACK.call(body) == 'replacement'
            assert CALLBACK.is(replacement)
            assert received == [body, body]
        '''.replace('CALLBACK', callback)

        when:
        def result = GradleRunner.create().withProjectDir(projectDir).withGradleVersion(version)
            .withArguments(':parent:leaf:help', '--configuration-cache', '--configuration-cache-problems=fail',
                '-Dorg.gradle.unsafe.isolated-projects=true', '--stacktrace').build()

        then:
        result.output.contains('Configuration cache entry stored')

        where:
        [version, callback] << [[SupportedGradleVersions.GRADLE_MIN, GradleVersion.current().version].unique(),
                                ['daemon', 'applicationdaemon']].combinations()
    }
}
