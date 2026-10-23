package com.github.porotkin.dependencylens

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

internal const val DEPENDENCY_LENS = "dependencyLens"

private val IMPLICIT_KOTLIN_DEPENDENCIES = setOf(
    "org.jetbrains.kotlin:kotlin-stdlib",
    "org.jetbrains.kotlin:kotlin-dom-api-compat",
)

private val KLIB_PLATFORMS = setOf(
    KotlinPlatformType.js,
    KotlinPlatformType.wasm,
    KotlinPlatformType.native,
)

class DependencyLensPlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project): Unit = with(target) {
        extensions.create<DependencyLensExtension>(DEPENDENCY_LENS)

        tasks.register(DEPENDENCY_LENS) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Reports unused and undeclared dependencies of Kotlin klib compilations."

            dependsOn(tasks.withType<DependencyLensReport>())
        }
    }

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

        kotlinCompilation.registerReport(usage)

        val root = kotlinCompilation.target.project.projectDir.absolutePath
        return usage.map {
            listOf(
                SubpluginOption(OUTPUT_OPTION, it.asFile.absolutePath),
                SubpluginOption(ROOT_OPTION, root),
            )
        }
    }

    private fun KotlinCompilation<*>.registerReport(usageFile: Provider<RegularFile>) {
        val project = target.project
        val compileClasspath = project.configurations.getByName(compileDependencyConfigurationName)

        project.tasks.register<DependencyLensReport>(lensTaskName) {
            description = "Reports unused and undeclared dependencies of the $lensId compilation."
            dependsOn(compileTaskProvider)

            compilation = "${project.path} $lensId"
            usage = usageFile
            classpath.from(compileClasspath)
            rootComponent = compileClasspath.incoming.resolutionResult.rootComponent
            artifacts = compileClasspath.incoming.artifacts.resolvedArtifacts
            declarations = project.provider { declaringConfigurations() }
            ignored = project.extensions.getByType<DependencyLensExtension>().ignored.map { it + IMPLICIT_KOTLIN_DEPENDENCIES }
            classpathConfiguration = compileClasspath.name
            catalogAccessors = project.provider { project.catalogAccessors() }
            report = lensFile("report.txt")
            findings = lensFile("findings.tsv")
        }
    }
}
