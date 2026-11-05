package com.netflix.gradle.plugins.packaging

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class PropertyBackedCollectionsTest extends Specification {
    def property = ProjectBuilder.builder().build().objects.listProperty(String)
    def commands = new PropertyBackedList<String>(property)

    def 'size property and method report the collection size for string elements'() {
        given:
        property.set(['echo first', 'echo second'])

        expect:
        commands.size == 2
        commands.size() == 2

        when:
        property.add('echo third')

        then:
        commands.size == 3
    }

    def 'legacy removeRange removes only the requested commands from the backing property'() {
        given:
        property.set(['echo first', 'echo second', 'echo third', 'echo fourth'])

        when:
        commands.removeRange(1, 3)

        then:
        commands == ['echo first', 'echo fourth']
        property.get() == ['echo first', 'echo fourth']

        when:
        commands.removeRange(0, commands.size)

        then:
        commands.isEmpty()
        property.get() == []
    }

    def 'clear replaces inherited defaults without querying the provider'() {
        given:
        boolean queried = false
        property.convention(ProjectBuilder.builder().build().providers.provider {
            queried = true
            ['echo inherited']
        })

        when:
        commands.clear()

        then:
        !queried
        commands == []
        property.get() == []
    }
}
