# Plugin configuration cache audit

This inventory concerns consumer builds that apply this plugin. It excludes the
configuration cache status of this repository's own build and of other plugins
applied by a consumer.

## Result

On Gradle 9.5.1, the plugin's tested Debian, RPM, application, daemon,
Spring Boot 3.5.2, and Dockerfile task paths store and reuse the configuration
cache. Four plugin owned input tracking defects found during the audit have
been fixed. The fixes also correct ordinary Gradle up to date checking.

| Former blocker | Implemented change |
| --- | --- | --- |
| Daemon templates and context | `DaemonTemplateTask` now tracks context, template names, folder selection, and custom template files. Built in classpath templates do not require filesystem inputs. |
| Dockerfile instructions | `SystemPackageDockerfile` now stores DSL instructions in a lazy `ListProperty<String>` annotated `@Input`; the copy action receives their value only during execution. |
| Debian supplementary control files | `Deb` now snapshots the resolved files with `@InputFiles`, while the existing path list still determines package entry names. |
| RPM trigger scripts | `Rpm` now snapshots install, uninstall, and post uninstall trigger scripts with `@InputFiles`, separately from trigger metadata. |

## Evidence and limits

`ConfigurationCacheSpec` covers configuration cache storage and reuse, then
edits daemon templates, Dockerfile instructions, Debian supplementary control
files, and RPM trigger scripts to verify that the affected tasks rerun.
It also covers the previously ignored Spring Boot `buildDeb` path.

The Docker image build uses `com.bmuschko.docker-remote-api`, and this audit
does not establish whether that external plugin's `DockerBuildImage` task is
configuration cache compatible. This plugin's `createDockerfile` task is covered.
The suite runs with this repository's Gradle 9.5.1 wrapper; the constants in
`SupportedGradleVersions` name 8.6 and 9.0.0-rc-4, but this audit did not run
the configuration cache suite on those versions.
