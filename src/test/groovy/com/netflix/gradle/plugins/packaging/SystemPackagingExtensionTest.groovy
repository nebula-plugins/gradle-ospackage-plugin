package com.netflix.gradle.plugins.packaging

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.provider.Provider
import spock.lang.Specification

class SystemPackagingExtensionTest extends Specification {
    SystemPackagingExtension extension = ProjectBuilder.builder().build().objects.newInstance(SystemPackagingExtension)

    def "legacy accessors and options share the same packaging values"() {
        when:
        extension.packageName = 'legacy-name'
        extension.options.dependencies.add(new Dependency('first', '', 0))
        extension.dependencies.add(new Dependency('second', '', 0))
        extension.customFields.put('Section', 'java')
        extension.options.packageName.set('modern-name')

        then:
        extension.packageName == 'modern-name'
        extension.options.packageName.get() == 'modern-name'
        extension.options.dependencies.get()*.packageName == ['first', 'second']
        extension.dependencies*.packageName == ['first', 'second']
        extension.options.customFields.get() == [Section: 'java']

        when:
        extension.customFields.entrySet().first().setValue('admin')
        extension.dependencies.remove(0)

        then:
        extension.options.customFields.get() == [Section: 'admin']
        extension.options.dependencies.get()*.packageName == ['second']
    }

    def "every option exposes its backing Gradle provider"() {
        expect:
        SystemPackagingOptions.declaredMethods.findAll { it.name.startsWith('get') && it.name != 'getMetaClass' }.every { method ->
            method.invoke(extension.options) instanceof Provider
        }
    }

    def "legacy packaging getters retain their 12.x binary return types"() {
        expect:
        SystemPackagingExtension.getMethod('getPackageName').returnType == String
        SystemPackagingExtension.getMethod('getDependencies').returnType == List
        SystemPackagingExtension.getMethod('getCustomFields').returnType == Map
        SystemPackagingExtension.getMethod('getPreInstallFile').returnType == File
        SystemPackagingExtension.getMethod('setSetgid', boolean).returnType == void
        SystemPackagingExtension.getMethod('configurationFile', String).returnType == Object
        extension.setgid == false
    }

    def "Can define required package name without version and flag"() {
        given:
        String packageName = 'myPackage'

        when:
        extension.requires(packageName)

        then:
        extension.options.dependencies.get().size() == 1
        Dependency dep = extension.options.dependencies.get()[0]
        dep.packageName == packageName
        dep.version == ''
        dep.flag == 0
    }

    def "Can define required package name with version and without flag"(){
        given:
        String packageName = 'myPackage'

        when:
        extension.requires(packageName, '1.0.0')

        then:
        extension.options.dependencies.get().size() == 1
        Dependency dep = extension.options.dependencies.get()[0]
        dep.packageName == packageName
        dep.version == '1.0.0'
        dep.flag == 0
    }

    def "Can define required package name with version and flag"() {
        given:
        String packageName = 'myPackage'

        when:
        extension.requires(packageName, '1.0.0', 5)

        then:
        extension.options.dependencies.get().size() == 1
        Dependency dep = extension.options.dependencies.get()[0]
        dep.packageName == packageName
        dep.version == '1.0.0'
        dep.flag == 5
    }

    def "Cannot define required package name containing comma without version and flag"() {
        given:
        String packageName = 'myPackage,something'

        when:
        extension.requires(packageName)

        then:
        Throwable t = thrown(AssertionError)
        t.message == 'Package name (myPackage,something) can not include commas. Expression: packageName.contains(,)'
    }

    def "Cannot define required package name containing comma with version and flag"() {
        given:
        String packageName = 'myPackage,something'

        when:
        extension.requires(packageName, '1.0.0', 5)

        then:
        Throwable t = thrown(AssertionError)
        t.message == 'Package name (myPackage,something) can not include commas. Expression: packageName.contains(,)'
    }

    def "Cannot define preInstallFile more than once"() {
        given:
        extension.preInstallFile(new File('/tmp/some-file'))

        when:
        extension.preInstallFile(new File('/tmp/some-other-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.MULTIPLE_PREINSTALL_FILES
    }

    def "Cannot define preInstallFile after preInstall string commands"() {
        given:
        extension.preInstall('echo "Done Installing..."')

        when:
        extension.preInstallFile(new File('/tmp/some-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.PREINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define preInstallFile after preInstall file commands"() {
        given:
        extension.preInstall(new File('/tmp/some-other-file'))

        when:
        extension.preInstallFile(new File('/tmp/some-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.PREINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define preInstall string commands after preInstallFile"() {
        given:
        extension.preInstallFile(new File('/tmp/some-file'))

        when:
        extension.preInstall('echo "Done Installing..."')

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.PREINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define preInstall file commands after preInstallFile"() {
        given:
        extension.preInstallFile(new File('/tmp/some-file'))

        when:
        extension.preInstall(new File('/tmp/some-other-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.PREINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define postInstallFile more than once"() {
        given:
        extension.postInstallFile(new File('/tmp/some-file'))

        when:
        extension.postInstallFile(new File('/tmp/some-other-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.MULTIPLE_POSTINSTALL_FILES
    }

    def "Cannot define postInstallFile after postInstall string commands"() {
        given:
        extension.postInstall('echo "Done Installing..."')

        when:
        extension.postInstallFile(new File('/tmp/some-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.POSTINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define postInstallFile after postInstall file commands"() {
        given:
        extension.postInstall(new File('/tmp/some-other-file'))

        when:
        extension.postInstallFile(new File('/tmp/some-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.POSTINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define postInstall string commands after postInstallFile"() {
        given:
        extension.postInstallFile(new File('/tmp/some-file'))

        when:
        extension.postInstall('echo "Done Installing..."')

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.POSTINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define postInstall file commands after postInstallFile"() {
        given:
        extension.postInstallFile(new File('/tmp/some-file'))

        when:
        extension.postInstall(new File('/tmp/some-other-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.POSTINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define preUninstallFile more than once"() {
        given:
        extension.preUninstallFile(new File('/tmp/some-file'))

        when:
        extension.preUninstallFile(new File('/tmp/some-other-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.MULTIPLE_PREUNINSTALL_FILES
    }

    def "Cannot define preUninstallFile after preUninstall string commands"() {
        given:
        extension.preUninstall('echo "Done Uninstalling..."')

        when:
        extension.preUninstallFile(new File('/tmp/some-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.PREUNINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define preUninstallFile after preUninstall file commands"() {
        given:
        extension.preUninstall(new File('/tmp/some-other-file'))

        when:
        extension.preUninstallFile(new File('/tmp/some-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.PREUNINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define preUninstall string commands after preUninstallFile"() {
        given:
        extension.preUninstallFile(new File('/tmp/some-file'))

        when:
        extension.preUninstall('echo "Starting Uninstalling..."')

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.PREUNINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define preUninstall file commands after preUninstallFile"() {
        given:
        extension.preUninstallFile(new File('/tmp/some-file'))

        when:
        extension.preUninstall(new File('/tmp/some-other-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.PREUNINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define postUninstallFile more than once"() {
        given:
        extension.postUninstallFile(new File('/tmp/some-file'))

        when:
        extension.postUninstallFile(new File('/tmp/some-other-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.MULTIPLE_POSTUNINSTALL_FILES
    }

    def "Cannot define postUninstallFile after postUninstall string commands"() {
        given:
        extension.postUninstall('echo "Done Uninstalling..."')

        when:
        extension.postUninstallFile(new File('/tmp/some-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.POSTUNINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define postUninstallFile after postUninstall file commands"() {
        given:
        extension.postUninstall(new File('/tmp/some-other-file'))

        when:
        extension.postUninstallFile(new File('/tmp/some-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.POSTUNINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define postUninstall string commands after postUninstallFile"() {
        given:
        extension.postUninstallFile(new File('/tmp/some-file'))

        when:
        extension.postUninstall('echo "Done Installing..."')

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.POSTUNINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Cannot define postUninstall file commands after postUninstallFile"() {
        given:
        extension.postUninstallFile(new File('/tmp/some-file'))

        when:
        extension.postUninstall(new File('/tmp/some-other-file'))

        then:
        Throwable t = thrown(IllegalStateException)
        t == SystemPackagingExtension.POSTUNINSTALL_COMMANDS_AND_FILE_DEFINED
    }

    def "Can define triggerIn with file and dependency"() {
        given:
        File file = new File('/tmp/some-file')
        String packageName = 'myPackage'

        when:
        extension.triggerInstall(file, packageName)

        then:
        extension.options.triggerInstallCommands.get().size() == 1
        Trigger trig = extension.options.triggerInstallCommands.get()[0]
        trig.command == file
        trig.dependency.packageName == packageName
        trig.dependency.version == ''
        trig.dependency.flag == 0
    }

    def "Can not define triggerIn without file"() {
        given:
        String packageName = 'myPackage'

        when:
        extension.triggerInstall(null, packageName)

        then:
        Throwable t = thrown(AssertionError)
        t.message == 'Trigger script is required. Expression: command. Values: command = null'
    }


    def "Can not define triggerIn with bad dependency"() {
        given:
        File file = new File('/tmp/some-file')
        String packageName = 'myPackage,something'

        when:
        extension.triggerInstall(file, packageName)

        then:
        Throwable t = thrown(AssertionError)
        t.message == 'Package name (myPackage,something) can not include commas. Expression: packageName.contains(,)'
    }

    def "Can define triggerUn with file and dependency"() {
        given:
        File file = new File('/tmp/some-file')
        String packageName = 'myPackage'

        when:
        extension.triggerUninstall(file, packageName)

        then:
        extension.options.triggerUninstallCommands.get().size() == 1
        Trigger trig = extension.options.triggerUninstallCommands.get()[0]
        trig.command == file
        trig.dependency.packageName == packageName
        trig.dependency.version == ''
        trig.dependency.flag == 0
    }

    def "Can not define triggerUn without file"() {
        given:
        String packageName = 'myPackage'

        when:
        extension.triggerUninstall(null, packageName)

        then:
        Throwable t = thrown(AssertionError)
        t.message == 'Trigger script is required. Expression: command. Values: command = null'
    }


    def "Can not define triggerUn with bad dependency"() {
        given:
        File file = new File('/tmp/some-file')
        String packageName = 'myPackage,something'

        when:
        extension.triggerUninstall(file, packageName)

        then:
        Throwable t = thrown(AssertionError)
        t.message == 'Package name (myPackage,something) can not include commas. Expression: packageName.contains(,)'
    }

    def "Can define triggerPostUn with file and dependency"() {
        given:
        File file = new File('/tmp/some-file')
        String packageName = 'myPackage'

        when:
        extension.triggerPostUninstall(file, packageName)

        then:
        extension.options.triggerPostUninstallCommands.get().size() == 1
        Trigger trig = extension.options.triggerPostUninstallCommands.get()[0]
        trig.command == file
        trig.dependency.packageName == packageName
        trig.dependency.version == ''
        trig.dependency.flag == 0
    }

    def "Can not define triggerPostUn without file"() {
        given:
        String packageName = 'myPackage'

        when:
        extension.triggerPostUninstall(null, packageName)

        then:
        Throwable t = thrown(AssertionError)
        t.message == 'Trigger script is required. Expression: command. Values: command = null'
    }


    def "Can not define triggerPostUn with bad dependency"() {
        given:
        File file = new File('/tmp/some-file')
        String packageName = 'myPackage,something'

        when:
        extension.triggerPostUninstall(file, packageName)

        then:
        Throwable t = thrown(AssertionError)
        t.message == 'Package name (myPackage,something) can not include commas. Expression: packageName.contains(,)'
    }
}
