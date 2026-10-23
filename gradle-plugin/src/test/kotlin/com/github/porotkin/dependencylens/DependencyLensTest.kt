package com.github.porotkin.dependencylens

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
            module("exposed", "package exposed\nfun exposed() = 1")
            module("facade", "package facade\nfun facade() = 1") { api(":exposed") }
            module("callbacks", "package callbacks\ntypealias Callback = () -> Unit")
            module("sig", "package sig\nclass Sig")
            module("takes", "package takes\nfun takes(sig: sig.Sig?) {}") { api(":sig") }
            module("stars", "package stars\nfun star() = 1")
            module("thing", "package thing\nclass Thing")
            module("wrapper", "package wrapper\ntypealias Things = List<thing.Thing>") { api(":thing") }
            module("provider", "package provider\nfun callback(): callbacks.Callback = {}") { implementation(":callbacks") }
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
                    ":facade",
                    ":callbacks",
                    ":provider",
                    ":takes",
                    ":stars",
                    ":wrapper",
                    "libs.kotlinx.coroutines.core",
                )
                lens()
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
    }
}
