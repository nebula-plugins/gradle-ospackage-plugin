import org.codehaus.groovy.control.customizers.builder.CompilerCustomizationBuilder
import groovy.transform.CompileStatic

CompilerCustomizationBuilder.withConfig(configuration) {
    ast(CompileStatic)
}