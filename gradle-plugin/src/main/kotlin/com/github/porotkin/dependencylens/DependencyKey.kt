package com.github.porotkin.dependencylens

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ComponentSelector
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentSelector

internal val Dependency.key: String
    get() = if (this is ProjectDependency) projectKey(path) else moduleKey(group, name)

internal val ModuleIdentifier.key: String
    get() = moduleKey(group, name)

internal val ComponentIdentifier.key: String
    get() = when (this) {
        is ProjectComponentIdentifier -> projectKey(buildTreePath)
        is ModuleComponentIdentifier -> moduleKey(group, module)
        else -> displayName
    }

internal val ComponentSelector.key: String
    get() = when (this) {
        is ProjectComponentSelector -> projectKey(projectPath)
        is ModuleComponentSelector -> moduleKey(group, module)
        else -> displayName
    }

private fun projectKey(path: String): String =
    "project $path"

private fun moduleKey(
    group: String?,
    name: String,
): String =
    "$group:$name"
