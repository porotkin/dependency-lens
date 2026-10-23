package com.github.porotkin.dependencylens

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

internal class TestProject(
    private val dir: File,
    private val target: TestTarget,
    private val kotlinVersion: String = System.getProperty("kotlinVersion"),
) {
    private val repository = System.getProperty("testRepository")

    private val pluginId = System.getProperty("pluginId")

    private val pluginVersion = System.getProperty("pluginVersion")

    private val modules = mutableListOf<String>()

    fun module(
        name: String,
        source: String,
        testSource: String? = null,
        configure: TestModule.() -> Unit = {},
    ) {
        val fileName = name.replaceFirstChar(Char::uppercaseChar)
        modules += name
        write("$name/build.gradle.kts", TestModule(pluginId, target).apply(configure).buildScript())
        write("$name/src/${target.id}Main/kotlin/$fileName.kt", source)
        testSource?.let { write("$name/src/${target.id}Test/kotlin/${fileName}Test.kt", it) }
    }

    fun run(vararg tasks: String): BuildResult {
        writeRootBuild()

        return GradleRunner.create()
            .withProjectDir(dir)
            .withArguments(*tasks, "--stacktrace")
            .forwardOutput()
            .build()
    }

    fun read(path: String): String =
        File(dir, path).readText()

    private fun writeRootBuild() {
        write(
            "settings.gradle.kts",
            """
            pluginManagement { repositories { maven("$repository"); gradlePluginPortal(); mavenCentral() } }
            dependencyResolutionManagement {
                repositories { maven("$repository"); mavenCentral() }
                versionCatalogs { create("libs") { library("kotlinx-coroutines-core", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2") } }
            }
            include(${modules.joinToString { "\"$it\"" }})
            """,
        )
        write(
            "build.gradle.kts",
            """
            plugins {
                kotlin("multiplatform") version "$kotlinVersion" apply false
                id("$pluginId") version "$pluginVersion" apply false
            }
            subprojects {
                apply(plugin = "org.jetbrains.kotlin.multiplatform")
                extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> { ${target.id}() }
            }
            """,
        )
    }

    private fun write(
        path: String,
        text: String,
    ) {
        File(dir, path).apply { parentFile.mkdirs() }.writeText(text.trimIndent() + "\n")
    }
}

internal class TestModule(
    private val pluginId: String,
    private val target: TestTarget,
) {
    private val dependencies = mutableListOf<String>()

    private val lens = mutableListOf<String>()

    fun api(vararg notations: String) {
        declare("${target.id}MainApi", notations)
    }

    fun implementation(vararg notations: String) {
        declare("${target.id}MainImplementation", notations)
    }

    fun lens(ignored: List<String>) {
        lens += "apply(plugin = \"$pluginId\")"
        lens += "extensions.configure<${DependencyLensExtension::class.qualifiedName}> { ignored.addAll(${ignored.joinToString { "\"$it\"" }}) }"
    }

    fun buildScript(): String =
        buildString {
            appendLine("kotlin { sourceSets { ${target.id}Main { dependencies { } } } }")
            appendLine("dependencies {")
            dependencies.forEach(::appendLine)
            appendLine("}")
            lens.forEach(::appendLine)
        }

    private fun declare(
        configuration: String,
        notations: Array<out String>,
    ) {
        notations.mapTo(dependencies) { "$configuration(${it.asDependency()})" }
    }

    private fun String.asDependency(): String =
        if (startsWith(":")) "project(\"$this\")" else this
}
