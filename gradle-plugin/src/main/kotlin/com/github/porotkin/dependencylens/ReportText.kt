package com.github.porotkin.dependencylens

internal fun reportText(
    compilation: String,
    findings: List<Finding>,
): String =
    buildReport(compilation) {
        for ((issue, issueFindings) in findings.groupBy(Finding::issue).toSortedMap()) {
            section(issue.title, issueFindings.map(Finding::summary))
        }

        if (findings.isEmpty()) {
            appendLine("No issues.")
        }
    }

internal fun noSourcesReportText(compilation: String): String =
    buildReport(compilation) {
        appendLine("No sources compiled.")
    }

private fun buildReport(
    compilation: String,
    body: StringBuilder.() -> Unit,
): String =
    buildString {
        appendLine("Dependency Lens: $compilation")
        body()
    }

private fun StringBuilder.section(
    title: String,
    lines: List<String>,
) {
    appendLine()
    appendLine("$title:")
    lines.forEach { appendLine("  $it") }
}
