package com.github.porotkin.dependencylens

internal class FindingsTable(
    private val compilation: String,
    private val classpath: String,
    private val catalogAccessors: Map<String, List<String>>,
) {
    fun lines(findings: List<Finding>): List<String> =
        findings.flatMap { finding ->
            finding.rows(compilation).map { row ->
                tsvLine(finding.issue, classpath, row.dependency, spellings(row.declaration), row.message)
            }
        }

    private fun spellings(declaration: String): String =
        (listOf(declaration) + catalogAccessors[declaration].orEmpty()).joinToString(",")

    companion object {
        val HEADER: String = tsvLine("issue", "classpath", "dependency", "declaration", "message")
    }
}
