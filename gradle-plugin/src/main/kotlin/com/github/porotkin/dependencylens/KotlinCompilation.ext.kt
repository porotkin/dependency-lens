package com.github.porotkin.dependencylens

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

internal val KotlinCompilation<*>.lensId: String
    get() = "${target.name}/$name"

internal fun KotlinCompilation<*>.lensFile(name: String): Provider<RegularFile> =
    target.project.layout.buildDirectory.file("dependency-lens/$lensId/$name")
