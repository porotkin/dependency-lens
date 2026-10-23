package com.github.porotkin.dependencylens

import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import java.util.Properties

internal val COMPILER_PLUGIN_ARTIFACT: SubpluginArtifact =
    Properties().run {
        DependencyLensPlugin::class.java.getResourceAsStream("/dependency-lens.properties")!!.use(::load)

        SubpluginArtifact(
            groupId = getProperty("group"),
            artifactId = "compiler-plugin",
            version = getProperty("version"),
        )
    }

internal val PROTOCOL_DEPENDENCY: String =
    with(COMPILER_PLUGIN_ARTIFACT) { "$groupId:protocol:$version" }
