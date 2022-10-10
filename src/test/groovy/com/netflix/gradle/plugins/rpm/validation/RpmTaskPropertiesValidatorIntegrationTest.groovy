package com.netflix.gradle.plugins.rpm.validation

import com.netflix.gradle.plugins.rpm.Rpm
import nebula.test.ProjectSpec
import org.gradle.api.InvalidUserDataException
import spock.lang.Issue

class RpmTaskPropertiesValidatorIntegrationTest extends ProjectSpec {
    RpmTaskPropertiesValidator validator = new RpmTaskPropertiesValidator()

    def setup() {
        project.apply plugin: 'com.netflix.nebula.rpm'
    }

    def 'can execute Rpm task with valid version and package name'() {
        given:
        Rpm rpmTask = project.task('buildRpm', type: Rpm) {
            packageName = 'can-execute-rpm-task-with-valid-version'
        }

        when:
        validator.validate(rpmTask)

        then:
        noExceptionThrown()
    }

    @Issue("https://github.com/nebula-plugins/gradle-ospackage-plugin/issues/90")
    def 'executing a Rpm task with invalid package name throws exception'() {
        given:
        Rpm rpmTask = project.task('buildRpm', type: Rpm) {
            packageName = 'abc^'
        }

        when:
        validator.validate(rpmTask)

        then:
        Throwable t = thrown(InvalidUserDataException)
        t.message == "Invalid package name 'abc^' - a valid package name must only contain [a-zA-Z0-9-._+]"
    }
}
