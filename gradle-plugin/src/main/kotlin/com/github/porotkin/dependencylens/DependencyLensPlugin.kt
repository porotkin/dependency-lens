package com.github.porotkin.dependencylens

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

private val KLIB_PLATFORMS = setOf(
    KotlinPlatformType.js,
    KotlinPlatformType.wasm,
    KotlinPlatformType.native,
)

class DependencyLensPlugin : KotlinCompilerPluginSupportPlugin {
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        kotlinCompilation.platformType in KLIB_PLATFORMS

    override fun getCompilerPluginId(): String =
        PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        COMPILER_PLUGIN_ARTIFACT

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val usage = kotlinCompilation.lensFile("usage.tsv")

        kotlinCompilation.compileTaskProvider.configure {
            outputs.file(usage).withPropertyName("dependencyLensUsage")
        }

        val root = kotlinCompilation.target.project.projectDir.absolutePath
        return usage.map {
            listOf(
                SubpluginOption(OUTPUT_OPTION, it.asFile.absolutePath),
                SubpluginOption(ROOT_OPTION, root),
            )
        }
    }
}
