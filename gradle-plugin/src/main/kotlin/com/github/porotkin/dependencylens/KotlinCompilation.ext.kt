package com.github.porotkin.dependencylens

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

internal val KotlinCompilation<*>.lensId: String
    get() = "${target.name}/$name"

internal val KotlinCompilation<*>.lensTaskName: String
    get() = DEPENDENCY_LENS + target.name.capitalized() + name.capitalized()

internal fun KotlinCompilation<*>.lensFile(name: String): Provider<RegularFile> =
    target.project.layout.buildDirectory.file("dependency-lens/$lensId/$name")

internal fun KotlinCompilation<*>.declaringConfigurations(): Map<String, String> =
    allKotlinSourceSets
        .flatMap { listOf(it.apiConfigurationName, it.implementationConfigurationName, it.compileOnlyConfigurationName) }
        .mapNotNull(target.project.configurations::findByName)
        .flatMap { configuration -> configuration.dependencies.map { it.key to configuration.name } }
        .distinctBy { (key) -> key }
        .toMap()

private fun String.capitalized(): String =
    replaceFirstChar(Char::uppercaseChar)
