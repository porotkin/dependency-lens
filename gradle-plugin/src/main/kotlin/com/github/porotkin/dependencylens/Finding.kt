package com.github.porotkin.dependencylens

internal sealed interface Finding {
    val issue: Issue

    val summary: String
}

internal class AnalysisFailed(
    private val failure: Use,
) : Finding {
    override val issue: Issue = Issue.ANALYSIS_FAILED

    override val summary: String = "${failure.file}: ${failure.example}"
}

internal class Unused(
    private val dependency: DeclaredDependency,
    replacement: String?,
) : Finding {
    private val advice = replacement?.let { "but it is how this module gets $it: declare that instead" }

    override val issue: Issue = Issue.UNUSED

    override val summary: String = listOfNotNull(dependency.declaration, advice).joinToString("  ")
}

internal class ImportsOnly(
    private val dependency: DeclaredDependency,
    private val use: Use,
) : Finding {
    override val issue: Issue = Issue.IMPORTS_ONLY

    override val summary: String = "${dependency.declaration}  ${use.where}"
}

internal class Undeclared(
    private val dependency: UndeclaredDependency,
    private val use: Use,
) : Finding {
    val component: String = dependency.component

    private val routes = dependency.routes.joinToString()

    override val issue: Issue = Issue.UNDECLARED

    override val summary: String = "$component  via $routes  ${use.where}"
}

private val Use.where: String
    get() = "e.g. $example in ${file.substringAfterLast('/')}"
