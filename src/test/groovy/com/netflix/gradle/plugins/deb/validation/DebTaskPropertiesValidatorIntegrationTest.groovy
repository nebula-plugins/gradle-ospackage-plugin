package com.netflix.gradle.plugins.deb.validation

import com.netflix.gradle.plugins.deb.Deb
import nebula.test.ProjectSpec
import org.gradle.api.InvalidUserDataException
import spock.lang.Issue

class DebTaskPropertiesValidatorIntegrationTest extends ProjectSpec {
    DebTaskPropertiesValidator validator = new DebTaskPropertiesValidator()

    def setup() {
        project.apply plugin: 'com.netflix.nebula.deb'
    }

    def 'can execute Deb task with valid version and package name'() {
        given:
        Deb debTask = project.task('buildDeb', type: Deb) {
            version = '1.0'
            packageName = 'can-execute-deb-task-with-valid-version'
        }

        when:
        validator.validate(debTask)

        then:
        noExceptionThrown()
    }

    def 'can execute Deb task with undefined version and valid package name'() {
        given:
        Deb debTask = project.task('buildDeb', type: Deb) {
            packageName = 'can-execute-deb-task-with-valid-version'
        }

        when:
        validator.validate(debTask)

        then:
        noExceptionThrown()
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/85")
    def 'executing a Deb task with invalid version throws exception'() {
        given:
        Deb debTask = project.task('buildDeb', type: Deb) {
            version = '-1.0'
            packageName = 'executing-a-deb-task-with-invalid-version-throws-exception'
        }

        when:
        validator.validate(debTask)

        then:
        Throwable t = thrown(InvalidUserDataException)
        t.message == "Invalid upstream version '-1.0' - a valid version must start with a digit and only contain [A-Za-z0-9.+:~-]"
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/90")
    def 'executing a Deb task with invalid package name throws exception'() {
        given:
        Deb debTask = project.task('buildDeb', type: Deb) {
            version = '1.0'
            packageName = 'a'
        }

        when:
        validator.validate(debTask)

        then:
        Throwable t = thrown(InvalidUserDataException)
        t.message == "Invalid package name 'a' - a valid package name must start with an alphanumeric character, have a length of at least two characters and only contain [a-z0-9.+-]"
    }
}
