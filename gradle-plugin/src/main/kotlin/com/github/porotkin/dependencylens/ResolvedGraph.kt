package com.github.porotkin.dependencylens

import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.ResolvedVariantResult

internal val ResolvedComponentResult.firstLevelDependencies: List<ResolvedDependencyResult>
    get() = resolvedDependencies.filterNot { it.isConstraint }

internal val ResolvedDependencyResult.owners: Set<ComponentIdentifier>
    get() = setOf(selected.id, resolvedVariant.owner) + resolvedVariant.redirects

internal fun ResolvedComponentResult.reachableIds(): Set<ComponentIdentifier> =
    reachableComponents()
        .flatMap { component -> sequenceOf(component.id) + component.variants.asSequence().flatMap { it.redirects } }
        .toSet()

internal fun ResolvedComponentResult.redirectOrigins(): Map<ComponentIdentifier, ComponentIdentifier> =
    reachableComponents()
        .flatMap { component ->
            component.variants.asSequence()
                .flatMap { it.redirects }
                .map { it to component.id }
        }
        .toMap()

private val ResolvedComponentResult.resolvedDependencies: List<ResolvedDependencyResult>
    get() = dependencies.filterIsInstance<ResolvedDependencyResult>()

private val ResolvedVariantResult.redirects: Sequence<ComponentIdentifier>
    get() = generateSequence(externalVariant.orElse(null)) { it.externalVariant.orElse(null) }
        .map { it.owner }

private fun ResolvedComponentResult.reachableComponents(
    visited: MutableSet<ComponentIdentifier> = mutableSetOf(),
): Sequence<ResolvedComponentResult> = sequence {
    if (!visited.add(id)) return@sequence

    yield(this@reachableComponents)

    for (dependency in resolvedDependencies) {
        yieldAll(dependency.selected.reachableComponents(visited))
    }
}
