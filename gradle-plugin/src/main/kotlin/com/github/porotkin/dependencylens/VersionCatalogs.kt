package com.github.porotkin.dependencylens

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.findByType

internal fun Project.catalogAccessors(): Map<String, List<String>> {
    val catalogs = extensions.findByType<VersionCatalogsExtension>() ?: return emptyMap()

    return catalogs
        .flatMap { catalog -> catalog.libraryAliases.map { alias -> catalog.moduleKey(alias) to catalog.accessor(alias) } }
        .groupBy({ it.first }, { it.second })
}

private fun VersionCatalog.moduleKey(alias: String): String =
    findLibrary(alias).get().get().module.key

private fun VersionCatalog.accessor(alias: String): String =
    "$name.${alias.replace('-', '.').replace('_', '.')}"
