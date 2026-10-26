package com.github.porotkin.dependencylens

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
            module("gateway", "package gateway\nfun gateway() = 1") { api(":exposed") }
            module("exported", "package exported\nfun exported() = 1")
            module("inner", "package inner\nfun inner() = 1")
            module("outer", "package outer\nfun outer() = 1") { api(":inner") }
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
                import outer.outer
                import stars.*

                fun main() {
                    used.used()
                    val handler: alias.Handler = {}
                    handler()
                    derived.Derived()
                    exposed.exposed()
                    inner.inner()
                    val callback = provider.callback()
                    callback()
                    takes.takes(null)
                    val things: wrapper.Things = emptyList()
                    println(things)
                }
                """,
                testSource = "package app\nfun check() = main()",
            ) {
                api(":exported")
                implementation(
                    ":used",
                    ":alias",
                    ":base",
                    ":derived",
                    ":imported",
                    ":unused",
                    ":ignored",
                    ":facade",
                    ":gateway",
                    ":outer",
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
              ${target.id}MainImplementation(project :gateway)  but it is how this module gets project :exposed: declare that instead
              ${target.id}MainImplementation(project :callbacks)
              ${target.id}MainImplementation(org.jetbrains.kotlinx:kotlinx-coroutines-core)

            Unused here, but exported as api (check that consumers don't need it):
              ${target.id}MainApi(project :exported)

            Used only by imports (remove the imports, then the dependency):
              ${target.id}MainImplementation(project :imported)  e.g. imported/imported in App.kt
              ${target.id}MainImplementation(project :outer)  e.g. outer/outer in App.kt  but it is how this module gets project :inner: declare that instead
              ${target.id}MainImplementation(project :stars)  e.g. stars.* in App.kt

            Used but not declared (reached through another dependency's api):
              project :exposed  via project :facade, project :gateway  e.g. exposed/exposed in App.kt
              project :inner  via project :outer  e.g. inner/inner in App.kt

            """.trimIndent(),
            project.read("app/build/dependency-lens/${target.id}/main/report.txt"),
        )
        assertEquals(
            """
            issue	classpath	dependency	declaration	message
            UNUSED_API	${target.classpath}	project :exported	project :exported	Not needed to compile :app ${target.id}/main, but it is exported as api: check that consumers don't need it
            UNUSED	${target.classpath}	project :unused	project :unused	Not needed to compile :app ${target.id}/main
            UNUSED	${target.classpath}	project :facade	project :facade	Not needed to compile :app ${target.id}/main, but it is how this module gets project :exposed: declare that instead
            UNUSED	${target.classpath}	project :gateway	project :gateway	Not needed to compile :app ${target.id}/main, but it is how this module gets project :exposed: declare that instead
            UNUSED	${target.classpath}	project :callbacks	project :callbacks	Not needed to compile :app ${target.id}/main
            UNUSED	${target.classpath}	org.jetbrains.kotlinx:kotlinx-coroutines-core	org.jetbrains.kotlinx:kotlinx-coroutines-core,libs.kotlinx.coroutines.core	Not needed to compile :app ${target.id}/main
            IMPORTS_ONLY	${target.classpath}	project :imported	project :imported	Remove the imports in :app ${target.id}/main, e.g. imported/imported in App.kt, then the dependency
            IMPORTS_ONLY	${target.classpath}	project :outer	project :outer	Remove the imports in :app ${target.id}/main, e.g. outer/outer in App.kt, then the dependency, but it is how this module gets project :inner: declare that instead
            IMPORTS_ONLY	${target.classpath}	project :stars	project :stars	Remove the imports in :app ${target.id}/main, e.g. stars.* in App.kt, then the dependency
            UNDECLARED	${target.classpath}	project :exposed	project :facade	project :exposed is used in :app ${target.id}/main, e.g. exposed/exposed in App.kt
            UNDECLARED	${target.classpath}	project :exposed	project :gateway	project :exposed is used in :app ${target.id}/main, e.g. exposed/exposed in App.kt
            UNDECLARED	${target.classpath}	project :inner	project :outer	project :inner is used in :app ${target.id}/main, e.g. inner/inner in App.kt

            """.trimIndent(),
            project.read("app/build/dependency-lens/${target.id}/main/findings.tsv"),
        )
        assertEquals(
            "Dependency Lens: :app ${target.id}/test\nNo issues.\n",
            project.read("app/build/dependency-lens/${target.id}/test/report.txt"),
        )
    }

    @ParameterizedTest
    @EnumSource
    fun `records the klib each resolved symbol comes from`(target: TestTarget) {
        val project = TestProject(dir, target).apply {
            module("used", "package used\nfun used() = 1")
            module("app", "package app\nfun main() = used.used()") {
                implementation(":used")
                lens(ignored = emptyList())
            }
        }

        project.run(":app:dependencyLens")

        val uses = dir.resolve("app/build/dependency-lens/${target.id}/main/usage.tsv").readUses()
        assertTrue(uses.any { it.kind == Kind.DIRECT && it.example == "used/used" }, "No klib usage recorded: $uses")
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
        val expected = mapOf(
            ":app:compileKotlin$name" to if (target.cacheableCompilation) TaskOutcome.FROM_CACHE else TaskOutcome.SUCCESS,
            ":app:dependencyLens${name}Main" to TaskOutcome.FROM_CACHE,
            ":app:dependencyLens${name}Test" to TaskOutcome.FROM_CACHE,
        )

        assertEquals(expected, expected.mapValues { (task) -> second.task(task)?.outcome })
    }

    @Test
    fun `warns on a Kotlin version it is not built for`() {
        val project = TestProject(dir, TestTarget.JS, kotlinVersion = "2.4.10").apply {
            module("app", "package app\nfun main() = Unit") { lens(ignored = emptyList()) }
        }

        assertTrue("not 2.4.10, and may fail." in project.run(":app:help").output)
    }
}
