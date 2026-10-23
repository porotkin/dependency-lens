package com.github.porotkin.dependencylens

import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult

internal class DeclaredDependency(
    val key: String,
    configuration: String,
    val uses: List<Use>,
) {
    val declaration: String = "$configuration($key)"
}

internal class UndeclaredDependency(
    val component: String,
    val routes: List<String>,
    val uses: List<Use>,
)

internal class UsageAnalysis(
    private val root: ResolvedComponentResult,
    private val usesByArtifact: Map<ResolvedArtifactResult, List<Use>>,
    private val failures: List<Use>,
    private val declarations: Map<String, String>,
    private val ignored: Set<String>,
) {
    private val firstLevel = root.firstLevelDependencies

    private val routesTo: Map<ComponentIdentifier, List<String>> by lazy {
        firstLevel
            .flatMap { dependency -> dependency.selected.reachableIds().map { it to dependency.requested.key } }
            .groupBy({ it.first }, { it.second })
    }

    fun findings(): List<Finding> {
        val declared = declared()
        val undeclared = undeclared()

        val worthDeclaring = undeclared
            .mapNotNull { dependency -> dependency.uses.firstOrNull { it.kind == Kind.DIRECT }?.let { Undeclared(dependency, it) } }
            .sortedBy { it.component }

        if (failures.isNotEmpty()) {
            return failures.map(::AnalysisFailed) + worthDeclaring
        }

        val unusedDeclared = declared.filter { it.uses.isEmpty() }
        val importsOnlyDeclared = declared.filter { it.uses.isNotEmpty() && it.uses.all { use -> use.kind == Kind.IMPORT } }
        val removable = (unusedDeclared + importsOnlyDeclared).mapTo(mutableSetOf()) { it.key }

        val unused = unusedDeclared.map { Unused(it, undeclared.onlyReachedThrough(it, removable)) }
        val importsOnly = importsOnlyDeclared.map { ImportsOnly(it, it.uses.first(), undeclared.onlyReachedThrough(it, removable)) }

        return unused + importsOnly + worthDeclaring
    }

    private fun declared(): List<DeclaredDependency> =
        firstLevel.mapNotNull { dependency ->
            val owners = dependency.owners
            val artifacts = usesByArtifact.filterKeys { it.variant.owner in owners }
            val key = dependency.requested.key
            val configuration = declarations[key]

            if (artifacts.isEmpty() || key in ignored || configuration == null) {
                null
            } else {
                DeclaredDependency(key, configuration, artifacts.values.flatten())
            }
        }

    private fun undeclared(): List<UndeclaredDependency> {
        val owned = firstLevel.flatMapTo(mutableSetOf(root.id)) { it.owners }
        val redirectOrigins = root.redirectOrigins()

        return usesByArtifact
            .filter { (artifact, uses) -> uses.isNotEmpty() && artifact.variant.owner !in owned }
            .entries
            .groupBy { (artifact) -> redirectOrigins[artifact.variant.owner] ?: artifact.variant.owner }
            .map { (component, entries) ->
                val owner = entries.first().key.variant.owner

                UndeclaredDependency(
                    component = component.key,
                    routes = routesTo[owner].orEmpty(),
                    uses = entries.flatMap { it.value },
                )
            }
            .filter { it.routes.isNotEmpty() }
    }

    private fun List<UndeclaredDependency>.onlyReachedThrough(
        dependency: DeclaredDependency,
        removable: Set<String>,
    ): String? =
        filter { dependency.key in it.routes && removable.containsAll(it.routes) }
            .joinToString { it.component }
            .ifEmpty { null }
}
