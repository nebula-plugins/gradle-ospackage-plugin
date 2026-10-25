package com.netflix.gradle.plugins

import com.netflix.gradle.plugins.deb.Scanner as DebScanner
import com.netflix.gradle.plugins.rpm.Scanner as RpmScanner
import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import spock.lang.Issue

import static org.redline_rpm.header.Header.HeaderTag.*

@Issue('https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/503')
class CopySpecConfigurationCacheSpec extends BaseIntegrationTestKitSpec {
    def 'child copy spec metadata survives configuration cache storage and reuse on Gradle #testedGradleVersion'() {
        given:
        gradleVersion = testedGradleVersion
        file('shared/dir/app.txt').text = 'shared content'
        file('task/dir/task.txt').text = 'task content'
        file('defaults.txt').text = 'default content'
        new File(projectDir, 'omitted/empty').mkdirs()
        buildFile << '''
            plugins { id 'com.netflix.nebula.ospackage' }
            version = '1.0.0'
            ospackage {
                packageName = 'copy-spec-settings'
                user = 'taskowner'
                permissionGroup = 'taskgroup'
                uid = 1001
                gid = 1002
                setgid = true
                createDirectoryEntry = true
                addParentDirs = true
                into('/opt') {
                    from('shared') {
                        into 'shared'
                        user 'app'
                        permissionGroup 'appgroup'
                        uid 2001
                        gid 2002
                        setgid true
                        createDirectoryEntry true
                        addParentDirs false
                        fileType new org.redline_rpm.payload.Directive(17) // CONFIG | NOREPLACE
                        filePermissions { unix(0750) }
                        dirPermissions { unix(0750) }
                    }
                }
                from('omitted') {
                    into '/opt/omitted'
                    createDirectoryEntry false
                    addParentDirs false
                }
                from('defaults.txt') { into '/opt/defaults' }
            }
            tasks.withType(com.netflix.gradle.plugins.packaging.SystemPackagingTask).configureEach {
                from('task') {
                    into '/opt/task'
                    user 'child'
                    permissionGroup 'childgroup'
                    uid 3001
                    gid 3002
                    setgid false
                    createDirectoryEntry true
                    addParentDirs false
                    fileType DOC
                    dirPermissions { unix(0750) }
                }
            }
        '''.stripIndent()

        when:
        def baseline = runTasks('buildDeb', 'buildRpm', '--no-configuration-cache', '--rerun-tasks')

        then:
        baseline.task(':buildDeb').outcome == TaskOutcome.SUCCESS
        baseline.task(':buildRpm').outcome == TaskOutcome.SUCCESS
        assertPackageMetadata()

        when:
        def stored = runTasks('buildDeb', 'buildRpm', '--configuration-cache', '--rerun-tasks')

        then:
        stored.output.contains('Configuration cache entry stored')
        stored.task(':buildDeb').outcome == TaskOutcome.SUCCESS
        stored.task(':buildRpm').outcome == TaskOutcome.SUCCESS
        assertPackageMetadata()

        when:
        def reused = runTasks('buildDeb', 'buildRpm', '--configuration-cache', '--rerun-tasks')

        then:
        reused.output.contains('Configuration cache entry reused')
        reused.task(':buildDeb').outcome == TaskOutcome.SUCCESS
        reused.task(':buildRpm').outcome == TaskOutcome.SUCCESS
        assertPackageMetadata()

        where:
        // Gradle 8.6 can run on at most JDK 21; keep newer Gradle coverage on JDK 25.
        testedGradleVersion << ['8.6', GradleVersion.current().version, '9.8.2'].unique().findAll {
            it != '8.6' || JavaVersion.current() <= JavaVersion.VERSION_21
        }
    }

    def 'changing only child metadata reruns packaging and unchanged metadata stays up to date'() {
        given:
        file('src/app.txt').text = 'content'
        buildFile << '''
            plugins { id 'com.netflix.nebula.ospackage' }
            version = '1.0.0'
            def owner = providers.gradleProperty('copySpecOwner').get()
            ospackage {
                packageName = 'metadata-input'
                from('src') {
                    into '/opt/app'
                    user owner
                }
            }
        '''.stripIndent()

        when:
        def first = runTasks('buildDeb', 'buildRpm', '--configuration-cache', '-PcopySpecOwner=first')
        def unchanged = runTasks('buildDeb', 'buildRpm', '--configuration-cache', '-PcopySpecOwner=first')

        then:
        first.task(':buildDeb').outcome == TaskOutcome.SUCCESS
        first.task(':buildRpm').outcome == TaskOutcome.SUCCESS
        unchanged.output.contains('Configuration cache entry reused')
        unchanged.task(':buildDeb').outcome == TaskOutcome.UP_TO_DATE
        unchanged.task(':buildRpm').outcome == TaskOutcome.UP_TO_DATE

        when:
        def changed = runTasks('buildDeb', 'buildRpm', '--configuration-cache', '-PcopySpecOwner=second')

        then:
        changed.task(':buildDeb').outcome == TaskOutcome.SUCCESS
        changed.task(':buildRpm').outcome == TaskOutcome.SUCCESS
        new DebScanner(file('build/distributions/metadata-input_1.0.0_all.deb'))
                .getEntry('./opt/app/app.txt').userName == 'second'
        RpmScanner.scan(file('build/distributions/metadata-input-1.0.0.noarch.rpm'))
                .format.header.getEntry(FILEUSERNAME).values.every { it == 'second' }
    }

    private void assertPackageMetadata() {
        def deb = new DebScanner(file('build/distributions/copy-spec-settings_1.0.0_all.deb'))
        def sharedFile = deb.getEntry('./opt/shared/dir/app.txt')
        assert sharedFile.userName == 'app'
        assert sharedFile.groupName == 'appgroup'
        assert sharedFile.longUserId == 2001
        assert sharedFile.longGroupId == 2002
        assert sharedFile.mode == 0750
        assert deb.getEntry('./opt/shared/dir/').mode == 02750
        def taskFile = deb.getEntry('./opt/task/dir/task.txt')
        assert taskFile.userName == 'child'
        assert taskFile.groupName == 'childgroup'
        assert taskFile.longUserId == 3001
        assert taskFile.longGroupId == 3002
        assert deb.getEntry('./opt/task/dir/').mode == 0750
        assert deb.getEntry('./opt/defaults/defaults.txt').userName == 'taskowner'
        assert !deb.controlContents['./postinst'].contains('/opt/omitted/empty')

        def rpm = RpmScanner.scan(file('build/distributions/copy-spec-settings-1.0.0.noarch.rpm'))
        def entries = rpm.files.withIndex().collectEntries { entry, index -> [(entry.name): index] }
        int sharedIndex = entries['./opt/shared/dir/app.txt']
        assert rpm.format.header.getEntry(FILEUSERNAME).values[sharedIndex] == 'app'
        assert rpm.format.header.getEntry(FILEGROUPNAME).values[sharedIndex] == 'appgroup'
        assert rpm.format.header.getEntry(FILEFLAGS).values[sharedIndex] == 17 // CONFIG | NOREPLACE
        assert rpm.files[sharedIndex].permissions == 0750
        assert rpm.files[entries['./opt/shared/dir']].permissions == 02750
        int taskIndex = entries['./opt/task/dir/task.txt']
        assert rpm.format.header.getEntry(FILEUSERNAME).values[taskIndex] == 'child'
        assert rpm.format.header.getEntry(FILEGROUPNAME).values[taskIndex] == 'childgroup'
        assert rpm.format.header.getEntry(FILEFLAGS).values[taskIndex] == 2 // DOC
        assert rpm.files[entries['./opt/task/dir']].permissions == 0750
        assert rpm.format.header.getEntry(FILEUSERNAME).values[entries['./opt/defaults/defaults.txt']] == 'taskowner'
        assert !entries.containsKey('./opt/shared') // addParentDirs=false
        assert !entries.containsKey('./opt/task')
        assert !entries.containsKey('./opt/omitted/empty') // createDirectoryEntry=false
    }
}
