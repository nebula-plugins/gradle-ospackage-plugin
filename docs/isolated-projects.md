# Daemon DSL with Isolated Projects

Apply the packaging plugins in each participating project's own build script.
`daemon { ... }` and `applicationdaemon { ... }` use project-local dispatch,
including in sibling and nested projects. Applying a plugin to the root still
configures only the root.

The extra properties remain Closure objects. Property access, saved references,
and explicit `daemon.call { ... }` / `applicationdaemon.call { ... }` continue
to work. Bare calls forward to the current local extra property, so replacing
`ext.daemon` or `ext.applicationdaemon` also affects subsequent bare calls.
Script methods and existing project methods retain precedence.

`daemon` still returns the configured daemon definition. `applicationdaemon`
still queues closures in order and returns the configuration list; the closures
run after evaluation, once the application name and installation prefix have
been configured. Closure delegation and packaging task creation are unchanged.

## Verified scope

The regression suite uses JDK 21 with Gradle 8.6 and 9.5.1. Both versions use:

```text
--configuration-cache --configuration-cache-problems=fail -Dorg.gradle.unsafe.isolated-projects=true
```

The tests cover local application in single, sibling, nested, and participating
root/parent/leaf layouts, root-only participation, and late legacy defaults after
early packaging task realization. Sibling/nested callback tests compare explicit
Closure calls with bare calls under ordinary configuration cache and strict
isolation: generated templates, decoded DEB/RPM payloads, ownership/modes,
package headers/dependencies, and installation scripts. They also require cache
storage/reuse, execution after payload/template changes while reusing the cache,
and new storage/reuse after daemon options and package release inputs change.
Build-cache retrieval is disabled in these tests.

## Compatibility boundaries

Gradle 8.6 nested Maven publishing fails strict isolation with
`Cannot access project ':' from project ':parent:leaf'`. A separate test removes
all ospackage plugins and reproduces the failure using only `maven-publish` and
a plain file artifact. The daemon dispatch fix does not resolve this Gradle core
limitation. Ordinary cached publishing on 8.6 and strict isolated publishing on
9.5.1 are covered, including package artifact equality and changed payloads.

Inherited parent extra properties and callbacks that access another project's
mutable model remain incompatible with isolation. Use local configuration or
`providers.gradleProperty(...)` for shared scalar inputs. The suite keeps the
inherited-property failure as an explicit consumer migration characterization.

This scope does not establish isolated compatibility on other Gradle/JDK
versions, with Spring Boot, external publishing conventions, or Docker image
building. It does not establish compatibility for arbitrary consumer metaclass
mutations or collisions with same-named task/extension configuration methods.
During the earlier assessment, intermittent ordinary cache invalidation failures
were observed in daemon/Docker tests; their causes remain unresolved. Strict
cache and input-change assertions are retained here.
The implementation verification also observed one Gradle 9.5.1 nested
`applicationdaemon` case reusing ordinary configuration after the build script
changed from `.call` to bare syntax, instead of storing a new entry. The same
case passed in the focused run; this remains an unresolved invalidation risk,
and the storage assertion has not been relaxed.

Run the relevant regressions with:

```sh
./gradlew test --no-build-cache --tests '*DaemonDsl*Spec' --tests '*IsolatedProjectsSpec' \
  --tests '*ConfigurationCacheSpec' --tests '*LegacyPackagingConventionsLauncherSpec' \
  --tests '*OspackageDaemonPlugin*Spec' --tests '*OspackageApplicationDaemonPlugin*Spec' \
  -Pcom.netflix.gradle-predictive-test-selection.enabled=false --console=plain
```
