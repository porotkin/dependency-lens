# Dependency Lens

[![GitHub Licence](https://img.shields.io/github/license/porotkin/dependency-lens)](https://github.com/porotkin/dependency-lens/blob/main/LICENSE)
[![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/porotkin/dependency-lens/test.yml)](https://github.com/porotkin/dependency-lens/actions/workflows/test.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.porotkin.dependencylens/compiler-plugin)](https://central.sonatype.com/artifact/io.github.porotkin.dependencylens/compiler-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.porotkin.dependency-lens)](https://plugins.gradle.org/plugin/io.github.porotkin.dependency-lens)

Reports unused and undeclared Gradle dependencies of Kotlin/JS, Kotlin/Wasm and Kotlin/Native compilations.

```kotlin
plugins {
    id("io.github.porotkin.dependency-lens") version "0.1.0"
}
```

```
./gradlew dependencyLens
```

```
Dependency Lens: :app js/main

Unused (declared, but not needed to compile):
  jsMainImplementation(project :unused)

Used only by imports (remove the imports, then the dependency):
  jsMainImplementation(project :imported)  e.g. imported/imported in App.kt

Used but not declared (reached through another dependency's api):
  project :exposed  via project :facade  e.g. exposed/exposed in App.kt
```

Reports are written to `build/dependency-lens/<target>/<compilation>/report.txt`, and for IDEs to `findings.tsv` next to
it.

## IntelliJ IDEA

With the [Gradle Lens](https://plugins.jetbrains.com/plugin/35278-gradle-lens) plugin, IDEA marks what the last
`dependencyLens` run found on the declarations in `build.gradle.kts` and in the Dependency Analyzer.

## Ignoring dependencies

```kotlin
dependencyLens {
    ignored.add("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    ignored.add("project :shared")
}
```

Modules are written `group:name`, without a version, and project dependencies `project :path`.

`kotlin-stdlib`, `kotlin-dom-api-compat` and `kotlin-js-plain-objects`, which the Kotlin Gradle plugin and its
subplugins add on their own, are ignored by default.

## Compatibility

The compiler plugin uses K2 (FIR) internals, so it is built for one Kotlin version: 2.4.21. On any other version the
plugin logs a warning.
