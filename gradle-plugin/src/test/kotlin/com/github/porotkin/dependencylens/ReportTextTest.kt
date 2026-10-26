package com.github.porotkin.dependencylens

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReportTextTest {
    @Test
    fun `reports failed analysis before undeclared dependencies, without findings rows for it`() {
        val findings = listOf(
            AnalysisFailed(Use("src/App.kt", "", Kind.FAILED, "java.lang.IllegalStateException: broken")),
            Undeclared(
                UndeclaredDependency("project :exposed", listOf("project :facade"), emptyList()),
                Use("src/App.kt", "exposed", Kind.DIRECT, "exposed/exposed"),
            ),
        )

        assertEquals(
            """
            Dependency Lens: :app js/main

            Analysis failed, unused dependencies not reported:
              src/App.kt: java.lang.IllegalStateException: broken

            Used but not declared (reached through another dependency's api):
              project :exposed  via project :facade  e.g. exposed/exposed in App.kt

            """.trimIndent(),
            reportText(":app js/main", findings),
        )
        assertEquals(
            listOf(
                "UNDECLARED\tjsCompileClasspath\tproject :exposed\tproject :facade\t" +
                        "project :exposed is used in :app js/main, e.g. exposed/exposed in App.kt",
            ),
            FindingsTable(":app js/main", "jsCompileClasspath", emptyMap()).lines(findings),
        )
    }
}
