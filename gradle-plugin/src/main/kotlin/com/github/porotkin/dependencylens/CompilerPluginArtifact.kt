package com.github.porotkin.dependencylens

import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import java.util.*

private val PROPERTIES: Properties =
    Properties().apply {
        DependencyLensPlugin::class.java.getResourceAsStream("/dependency-lens.properties")!!.use(::load)
    }

internal val COMPILER_PLUGIN_ARTIFACT: SubpluginArtifact =
    SubpluginArtifact(
        groupId = PROPERTIES.getProperty("group"),
        artifactId = "compiler-plugin",
        version = PROPERTIES.getProperty("version"),
    )

internal val PROTOCOL_DEPENDENCY: String =
    with(COMPILER_PLUGIN_ARTIFACT) { "$groupId:protocol:$version" }

internal val KOTLIN_VERSION: String =
    PROPERTIES.getProperty("kotlin")
