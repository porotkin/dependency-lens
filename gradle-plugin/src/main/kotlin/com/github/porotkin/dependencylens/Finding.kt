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
    private val failure: Use,
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
    private val advice = replacement?.let { "but it is how this module gets $it: declare that instead" }

    override val issue: Issue = Issue.UNUSED

    override val summary: String = listOfNotNull(dependency.declaration, advice).joinToString("  ")

    override fun rows(compilation: String): List<FindingRow> =
        dependency.rows(listOfNotNull("Not needed to compile $compilation", advice).joinToString(", "))
}

internal class ImportsOnly(
    private val dependency: DeclaredDependency,
    private val use: Use,
) : Finding {
    override val issue: Issue = Issue.IMPORTS_ONLY

    override val summary: String = "${dependency.declaration}  ${use.where}"

    override fun rows(compilation: String): List<FindingRow> =
        dependency.rows("Used only by imports in $compilation, ${use.where}: remove the imports, then the dependency")
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
                message = "$component is used in $compilation, ${use.where}, but not declared: it comes through $routes",
            )
        }
}

private val Use.where: String
    get() = "e.g. $example in ${file.substringAfterLast('/')}"

private fun DeclaredDependency.rows(message: String): List<FindingRow> =
    listOf(FindingRow(dependency = key, declaration = key, message = message))
