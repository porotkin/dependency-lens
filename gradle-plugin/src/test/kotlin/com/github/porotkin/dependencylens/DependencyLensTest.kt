package com.github.porotkin.dependencylens

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File

class DependencyLensTest {
    @TempDir
    lateinit var dir: File

    @ParameterizedTest
    @EnumSource
    fun `reports what the compiler resolved, not what the source mentions`(target: TestTarget) {
        val project = TestProject(dir, target).apply {
            module("used", "package used\nfun used() = 1")
            module("alias", "package alias\ntypealias Handler = () -> Unit")
            module("base", "package base\nopen class Base")
            module("derived", "package derived\nclass Derived : base.Base()") { implementation(":base") }
            module("imported", "package imported\nfun imported() = 1")
            module("unused", "package unused\nfun unused() = 1")
            module("ignored", "package ignored\nfun ignored() = 1")
            module("exposed", "package exposed\nfun exposed() = 1")
            module("facade", "package facade\nfun facade() = 1") { api(":exposed") }
            module("callbacks", "package callbacks\ntypealias Callback = () -> Unit")
            module("sig", "package sig\nclass Sig")
            module("takes", "package takes\nfun takes(sig: sig.Sig?) {}") { api(":sig") }
            module("stars", "package stars\nfun star() = 1")
            module("thing", "package thing\nclass Thing")
            module("wrapper", "package wrapper\ntypealias Things = List<thing.Thing>") { api(":thing") }
            module(
                "provider",
                "package provider\nfun callback(): callbacks.Callback = {}"
            ) { implementation(":callbacks") }
            module(
                "app",
                """
                package app

                import imported.imported
                import stars.*

                fun main() {
                    used.used()
                    val handler: alias.Handler = {}
                    handler()
                    derived.Derived()
                    exposed.exposed()
                    val callback = provider.callback()
                    callback()
                    takes.takes(null)
                    val things: wrapper.Things = emptyList()
                    println(things)
                }
                """,
            ) {
                implementation(
                    ":used",
                    ":alias",
                    ":base",
                    ":derived",
                    ":imported",
                    ":unused",
                    ":ignored",
                    ":facade",
                    ":callbacks",
                    ":provider",
                    ":takes",
                    ":stars",
                    ":wrapper",
                    "libs.kotlinx.coroutines.core",
                )
                lens(ignored = listOf("project :ignored"))
            }
        }

        project.run(":app:dependencyLens")

        assertEquals(
            """
            Dependency Lens: :app ${target.id}/main

            Unused (declared, but not needed to compile):
              ${target.id}MainImplementation(project :unused)
              ${target.id}MainImplementation(project :facade)  but it is how this module gets project :exposed: declare that instead
              ${target.id}MainImplementation(project :callbacks)
              ${target.id}MainImplementation(org.jetbrains.kotlinx:kotlinx-coroutines-core)

            Used only by imports (remove the imports, then the dependency):
              ${target.id}MainImplementation(project :imported)  e.g. imported/imported in App.kt
              ${target.id}MainImplementation(project :stars)  e.g. stars.* in App.kt

            Used but not declared (reached through another dependency's api):
              project :exposed  via project :facade  e.g. exposed/exposed in App.kt

            """.trimIndent(),
            project.read("app/build/dependency-lens/${target.id}/main/report.txt"),
        )
        assertEquals(
            """
            issue	classpath	dependency	declaration	message
            UNUSED	${target.classpath}	project :unused	project :unused	Not needed to compile :app ${target.id}/main
            UNUSED	${target.classpath}	project :facade	project :facade	Not needed to compile :app ${target.id}/main, but it is how this module gets project :exposed: declare that instead
            UNUSED	${target.classpath}	project :callbacks	project :callbacks	Not needed to compile :app ${target.id}/main
            UNUSED	${target.classpath}	org.jetbrains.kotlinx:kotlinx-coroutines-core	org.jetbrains.kotlinx:kotlinx-coroutines-core,libs.kotlinx.coroutines.core	Not needed to compile :app ${target.id}/main
            IMPORTS_ONLY	${target.classpath}	project :imported	project :imported	Used only by imports in :app ${target.id}/main, e.g. imported/imported in App.kt: remove the imports, then the dependency
            IMPORTS_ONLY	${target.classpath}	project :stars	project :stars	Used only by imports in :app ${target.id}/main, e.g. stars.* in App.kt: remove the imports, then the dependency
            UNDECLARED	${target.classpath}	project :exposed	project :facade	project :exposed is used in :app ${target.id}/main, e.g. exposed/exposed in App.kt, but not declared: it comes through project :facade

            """.trimIndent(),
            project.read("app/build/dependency-lens/${target.id}/main/findings.tsv"),
        )
    }

    @ParameterizedTest
    @EnumSource
    fun `restores the report from the build cache in another checkout`(target: TestTarget) {
        fun checkout(name: String) =
            TestProject(dir.resolve("$name/project"), target).apply {
                module("used", "package used\nfun used() = 1")
                module("app", "package app\nfun main() = used.used()") {
                    implementation(":used")
                    lens(ignored = emptyList())
                }
            }

        checkout("first").run(":app:dependencyLens", "--build-cache")
        val second = checkout("second").run(":app:dependencyLens", "--build-cache")

        val name = target.id.replaceFirstChar(Char::uppercaseChar)
        val tasks = listOf(":app:compileKotlin$name", ":app:dependencyLens${name}Main", ":app:dependencyLens${name}Test")

        assertEquals(tasks.associateWith { TaskOutcome.FROM_CACHE }, tasks.associateWith { second.task(it)?.outcome })
    }
}
