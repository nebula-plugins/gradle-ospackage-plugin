package com.netflix.gradle.plugins.packaging

import com.netflix.gradle.plugins.deb.control.MultiArch
import groovy.transform.CompileDynamic
import groovy.transform.PackageScope
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.redline_rpm.header.Os
import org.redline_rpm.header.RpmType
import org.redline_rpm.payload.Directive

/** The permanent provider-backed view of a packaging extension. */
class SystemPackagingOptions {
    private final Map<String, Object> properties

    SystemPackagingOptions(Map<String, Object> properties) {
        this.properties = properties
    }

    /** Resolve legacy closures during configuration, without retaining them in task state. */
    @PackageScope
    @CompileDynamic
    void adoptLegacyConventions(ConventionMapping mapping, ObjectFactory objects) {
        properties.each { String name, Object property ->
            def value = mapping.getConventionValue(null, name, false)
            if (value != null) {
                if (property instanceof RegularFileProperty && value instanceof File) {
                    property.convention(objects.fileProperty().fileValue(value))
                } else {
                    property.convention(value)
                }
            }
            // Gradle's decorated getter still consults conventionMapping, even when
            // options was explicitly set. Redirect it to the same backing property.
            mapping.map(name) { legacyValue(name, property) }
        }
    }

    @CompileDynamic
    private static Object legacyValue(String name, Object property) {
        if (property instanceof RegularFileProperty) return property.getOrNull()?.asFile
        if (property instanceof ListProperty) return new PropertyBackedList(property)
        if (property instanceof MapProperty) return new PropertyBackedMap(property)
        if (name == 'setgid') return property.getOrElse(false)
        return property.getOrNull()
    }

    Property<String> getPackageName() { (Property<String>) properties.get('packageName') }
    Property<String> getRelease() { (Property<String>) properties.get('release') }
    Property<String> getVersion() { (Property<String>) properties.get('version') }
    Property<String> getUser() { (Property<String>) properties.get('user') }
    Property<String> getPermissionGroup() { (Property<String>) properties.get('permissionGroup') }
    Property<String> getPackageGroup() { (Property<String>) properties.get('packageGroup') }
    Property<String> getBuildHost() { (Property<String>) properties.get('buildHost') }
    Property<String> getSummary() { (Property<String>) properties.get('summary') }
    Property<String> getPackageDescription() { (Property<String>) properties.get('packageDescription') }
    Property<String> getLicense() { (Property<String>) properties.get('license') }
    Property<String> getPackager() { (Property<String>) properties.get('packager') }
    Property<String> getDistribution() { (Property<String>) properties.get('distribution') }
    Property<String> getVendor() { (Property<String>) properties.get('vendor') }
    Property<String> getUrl() { (Property<String>) properties.get('url') }
    Property<String> getSourcePackage() { (Property<String>) properties.get('sourcePackage') }
    Property<String> getArchStr() { (Property<String>) properties.get('archStr') }
    Property<String> getMaintainer() { (Property<String>) properties.get('maintainer') }
    Property<String> getUploaders() { (Property<String>) properties.get('uploaders') }
    Property<String> getPriority() { (Property<String>) properties.get('priority') }
    Property<Integer> getEpoch() { (Property<Integer>) properties.get('epoch') }
    Property<Integer> getUid() { (Property<Integer>) properties.get('uid') }
    Property<Integer> getGid() { (Property<Integer>) properties.get('gid') }
    Property<Boolean> getSetgid() { (Property<Boolean>) properties.get('setgid') }
    Property<Boolean> getCreateDirectoryEntry() { (Property<Boolean>) properties.get('createDirectoryEntry') }
    Property<Boolean> getAddParentDirs() { (Property<Boolean>) properties.get('addParentDirs') }
    Property<String> getSigningKeyId() { (Property<String>) properties.get('signingKeyId') }
    Property<String> getSigningKeyPassphrase() { (Property<String>) properties.get('signingKeyPassphrase') }
    RegularFileProperty getSigningKeyRingFile() { (RegularFileProperty) properties.get('signingKeyRingFile') }
    RegularFileProperty getPreInstallFile() { (RegularFileProperty) properties.get('preInstallFile') }
    RegularFileProperty getPostInstallFile() { (RegularFileProperty) properties.get('postInstallFile') }
    RegularFileProperty getPreUninstallFile() { (RegularFileProperty) properties.get('preUninstallFile') }
    RegularFileProperty getPostUninstallFile() { (RegularFileProperty) properties.get('postUninstallFile') }
    RegularFileProperty getTriggerInstallFile() { (RegularFileProperty) properties.get('triggerInstallFile') }
    RegularFileProperty getTriggerUninstallFile() { (RegularFileProperty) properties.get('triggerUninstallFile') }
    RegularFileProperty getTriggerPostUninstallFile() { (RegularFileProperty) properties.get('triggerPostUninstallFile') }
    Property<Directive> getFileType() { (Property<Directive>) properties.get('fileType') }
    Property<Os> getOs() { (Property<Os>) properties.get('os') }
    Property<RpmType> getType() { (Property<RpmType>) properties.get('type') }
    Property<MultiArch> getMultiArch() { (Property<MultiArch>) properties.get('multiArch') }
    ListProperty<String> getPrefixes() { (ListProperty<String>) properties.get('prefixes') }
    ListProperty<Object> getConfigurationFiles() { (ListProperty<Object>) properties.get('configurationFiles') }
    ListProperty<Object> getPreInstallCommands() { (ListProperty<Object>) properties.get('preInstallCommands') }
    ListProperty<Object> getPostInstallCommands() { (ListProperty<Object>) properties.get('postInstallCommands') }
    ListProperty<Object> getPreUninstallCommands() { (ListProperty<Object>) properties.get('preUninstallCommands') }
    ListProperty<Object> getPostUninstallCommands() { (ListProperty<Object>) properties.get('postUninstallCommands') }
    ListProperty<Trigger> getTriggerInstallCommands() { (ListProperty<Trigger>) properties.get('triggerInstallCommands') }
    ListProperty<Trigger> getTriggerUninstallCommands() { (ListProperty<Trigger>) properties.get('triggerUninstallCommands') }
    ListProperty<Trigger> getTriggerPostUninstallCommands() { (ListProperty<Trigger>) properties.get('triggerPostUninstallCommands') }
    ListProperty<Object> getPreTransCommands() { (ListProperty<Object>) properties.get('preTransCommands') }
    ListProperty<Object> getPostTransCommands() { (ListProperty<Object>) properties.get('postTransCommands') }
    ListProperty<Object> getCommonCommands() { (ListProperty<Object>) properties.get('commonCommands') }
    ListProperty<Object> getSupplementaryControlFiles() { (ListProperty<Object>) properties.get('supplementaryControlFiles') }
    ListProperty<Link> getLinks() { (ListProperty<Link>) properties.get('links') }
    ListProperty<Dependency> getDependencies() { (ListProperty<Dependency>) properties.get('dependencies') }
    ListProperty<Dependency> getObsoletes() { (ListProperty<Dependency>) properties.get('obsoletes') }
    ListProperty<Dependency> getConflicts() { (ListProperty<Dependency>) properties.get('conflicts') }
    ListProperty<Dependency> getRecommends() { (ListProperty<Dependency>) properties.get('recommends') }
    ListProperty<Dependency> getSuggests() { (ListProperty<Dependency>) properties.get('suggests') }
    ListProperty<Dependency> getEnhances() { (ListProperty<Dependency>) properties.get('enhances') }
    ListProperty<Dependency> getPreDepends() { (ListProperty<Dependency>) properties.get('preDepends') }
    ListProperty<Dependency> getBreaks() { (ListProperty<Dependency>) properties.get('breaks') }
    ListProperty<Dependency> getReplaces() { (ListProperty<Dependency>) properties.get('replaces') }
    ListProperty<Dependency> getProvides() { (ListProperty<Dependency>) properties.get('provides') }
    ListProperty<Directory> getDirectories() { (ListProperty<Directory>) properties.get('directories') }
    MapProperty<String, String> getCustomFields() { (MapProperty<String, String>) properties.get('customFields') }
}
