package com.github.porotkin.dependencylens

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DependencyLensTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `reports what the compiler resolved, not what the source mentions`() {
        val project = TestProject(dir).apply {
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
            Dependency Lens: :app js/main

            Unused (declared, but not needed to compile):
              jsMainImplementation(project :unused)
              jsMainImplementation(project :facade)  but it is how this module gets project :exposed: declare that instead
              jsMainImplementation(project :callbacks)
              jsMainImplementation(org.jetbrains.kotlinx:kotlinx-coroutines-core)

            Used only by imports (remove the imports, then the dependency):
              jsMainImplementation(project :imported)  e.g. imported/imported in App.kt
              jsMainImplementation(project :stars)  e.g. stars.* in App.kt

            Used but not declared (reached through another dependency's api):
              project :exposed  via project :facade  e.g. exposed/exposed in App.kt

            """.trimIndent(),
            project.read("app/build/dependency-lens/js/main/report.txt"),
        )
        assertEquals(
            """
            issue	classpath	dependency	declaration	message
            UNUSED	jsCompileClasspath	project :unused	project :unused	Not needed to compile :app js/main
            UNUSED	jsCompileClasspath	project :facade	project :facade	Not needed to compile :app js/main, but it is how this module gets project :exposed: declare that instead
            UNUSED	jsCompileClasspath	project :callbacks	project :callbacks	Not needed to compile :app js/main
            UNUSED	jsCompileClasspath	org.jetbrains.kotlinx:kotlinx-coroutines-core	org.jetbrains.kotlinx:kotlinx-coroutines-core,libs.kotlinx.coroutines.core	Not needed to compile :app js/main
            IMPORTS_ONLY	jsCompileClasspath	project :imported	project :imported	Used only by imports in :app js/main, e.g. imported/imported in App.kt: remove the imports, then the dependency
            IMPORTS_ONLY	jsCompileClasspath	project :stars	project :stars	Used only by imports in :app js/main, e.g. stars.* in App.kt: remove the imports, then the dependency
            UNDECLARED	jsCompileClasspath	project :exposed	project :facade	project :exposed is used in :app js/main, e.g. exposed/exposed in App.kt, but not declared: it comes through project :facade

            """.trimIndent(),
            project.read("app/build/dependency-lens/js/main/findings.tsv"),
        )
    }

    @Test
    fun `restores the report from the build cache in another checkout`() {
        fun checkout(name: String) =
            TestProject(dir.resolve("$name/project")).apply {
                module("used", "package used\nfun used() = 1")
                module("app", "package app\nfun main() = used.used()") {
                    implementation(":used")
                    lens(ignored = emptyList())
                }
            }

        checkout("first").run(":app:dependencyLens", "--build-cache")
        val second = checkout("second").run(":app:dependencyLens", "--build-cache")

        val tasks = listOf(":app:compileKotlinJs", ":app:dependencyLensJsMain", ":app:dependencyLensJsTest")

        assertEquals(tasks.associateWith { TaskOutcome.FROM_CACHE }, tasks.associateWith { second.task(it)?.outcome })
    }
}
