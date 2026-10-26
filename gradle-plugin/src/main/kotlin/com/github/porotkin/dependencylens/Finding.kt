package com.github.porotkin.dependencylens

internal sealed interface Finding {
    val issue: Issue

    val summary: String

    fun rows(compilation: String): List<FindingRow>
}

internal class FindingRow(
    val dependency: String,
    val declaration: String,
    val message: String,
)

internal class AnalysisFailed(
    failure: Use,
) : Finding {
    override val issue: Issue = Issue.ANALYSIS_FAILED

    override val summary: String = "${failure.file}: ${failure.example}"

    override fun rows(compilation: String): List<FindingRow> =
        emptyList()
}

internal class Unused(
    private val dependency: DeclaredDependency,
    replacement: String?,
) : Finding {
    private val advice = replacement?.let(::declareInstead)

    private val exportedAdvice = "but it is exported as api: check that consumers don't need it".takeIf { dependency.exported }

    override val issue: Issue = if (dependency.exported) Issue.UNUSED_API else Issue.UNUSED

    override val summary: String = listOfNotNull(dependency.declaration, advice).joinToString("  ")

    override fun rows(compilation: String): List<FindingRow> =
        dependency.rows(listOfNotNull("Not needed to compile $compilation", exportedAdvice, advice).joinToString(", "))
}

internal class ImportsOnly(
    private val dependency: DeclaredDependency,
    private val use: Use,
    replacement: String?,
) : Finding {
    private val advice = replacement?.let(::declareInstead)

    override val issue: Issue = Issue.IMPORTS_ONLY

    override val summary: String = listOfNotNull(dependency.declaration, use.where, advice).joinToString("  ")

    override fun rows(compilation: String): List<FindingRow> =
        dependency.rows(
            listOfNotNull("Remove the imports in $compilation, ${use.where}, then the dependency", advice)
                .joinToString(", "),
        )
}

internal class Undeclared(
    private val dependency: UndeclaredDependency,
    private val use: Use,
) : Finding {
    val component: String = dependency.component

    private val routes = dependency.routes.joinToString()

    override val issue: Issue = Issue.UNDECLARED

    override val summary: String = "$component  via $routes  ${use.where}"

    override fun rows(compilation: String): List<FindingRow> =
        dependency.routes.map { route ->
            FindingRow(
                dependency = component,
                declaration = route,
                message = "$component is used in $compilation, ${use.where}",
            )
        }
}

private fun declareInstead(replacement: String): String =
    "but it is how this module gets $replacement: declare that instead"

private val Use.where: String
    get() = "e.g. $example in ${file.substringAfterLast('/')}"

private fun DeclaredDependency.rows(message: String): List<FindingRow> =
    listOf(FindingRow(dependency = key, declaration = key, message = message))
