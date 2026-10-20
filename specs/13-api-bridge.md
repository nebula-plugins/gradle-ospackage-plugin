# 13.x packaging API bridge

## Goal

Allow plugins that obtain the Gradle-created packaging and application extensions
or packaging tasks and were compiled against their 12.x APIs to run while
consumers adopt the provider-based 13.x API. The two APIs must share one backing
value so that configuration through either view affects the same package.

This bridge covers `SystemPackagingExtension` (including
`ProjectPackagingExtension`), `SystemPackagingTask`, and
`OspackageApplicationExtension`. Direct construction of
`SystemPackagingExtension` and third-party APIs, such as the bundled Docker
plugin, are outside this compatibility promise.

## Public API

The 12.x getters keep their original return types. For example,
`SystemPackagingExtension.getDependencies()` returns a mutable
`List<Dependency>`, and `OspackageApplicationExtension.getPrefix()` returns a
`String`. `OspackageApplicationExtension` remains a class.

Each extension also exposes an `options` object. Its getters return Gradle
`Property`, `ListProperty`, `MapProperty`, or `RegularFileProperty` values with the
original domain names:

```groovy
application.prefix                    // legacy String
application.options.prefix.set('/opt') // Property<String>

packaging.dependencies                 // legacy mutable List<Dependency>
packaging.options.dependencies.add(dep) // ListProperty<Dependency>
```

`options` is the permanent provider-based API. The legacy getters will be
deprecated in 13.x. They may be removed only in a later major release after
downstream usage is checked. The provider-based API will not be renamed when
that happens.

The application plugin adopts legacy `conventionMapping` defaults for `prefix`
and `distribution` after configuration, so existing callers still influence the
provider-backed package paths. New callers set conventions on `options` directly.

The nine script and control-file lists were `List<Object>` in 12.x because they
accept both `String` and `File`. Their legacy getters retain that source type,
and their options use `ListProperty<Object>`. Direct legacy list edits keep
`File` values; existing helper methods that accept a `File` can still supply
file contents through a provider.

Packaging tasks keep the public methods that 12.x generated through Groovy
delegation, including typed results for `requires(...)` and `link(...)`, legacy
value setters, and mutable list and map getters with their original element
types. The old generated
`setExten(SystemPackagingExtension)` setter cannot coexist with Gradle's
managed `@Nested getExten()` getter. Consumers that replaced the task's entire
extension must configure `task.exten.options` instead.

## Behavior

- The options and legacy getters share the same Gradle properties. There is no
  second state store or eager synchronization step.
- Legacy list and map getters are mutable live views: mutations update the
  backing Gradle property. Reading a legacy getter may realize that property;
  ospackage's own configuration uses the options view to stay lazy.
- Existing DSL assignments and helper methods such as `requires(...)` continue
  to configure the same backing properties.
- Packaging task inputs still read the provider-backed state, and task actions
  do not capture `Project` through the compatibility layer.

## Verification and rollout

The published 12.3.0 JAR exposes 65 domain getters on
`SystemPackagingExtension` and two on `OspackageApplicationExtension`. Every
one has a matching getter on the corresponding `options` view. The additional
`ProjectPackagingExtension` getters belong to its existing `CopySpec` API and
remain available there.

1. Compare public JVM method descriptors against the published 12.3.0 JAR and
   run representative old-style extension and task calls, including application
   extension type lookup.
2. Test both directions of state sharing, legacy collection mutation, package
   contents, and provider-backed configuration without early realization.
3. Run the plugin's configuration-cache tests twice and run the full build.
4. Publish a 13.x candidate before changing an upstream version constraint.
   Roll back by restoring the previous 12.x lock if a canary exposes an
   uncovered consumer API.

The bridge does not repair configuration-cache violations in consumer task
actions. Those require changes in the consuming plugin.
