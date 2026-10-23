package com.github.porotkin.dependencylens

internal enum class Issue(
    val title: String,
) {
    ANALYSIS_FAILED("Analysis failed, unused dependencies not reported"),
    UNUSED("Unused (declared, but not needed to compile)"),
    IMPORTS_ONLY("Used only by imports (remove the imports, then the dependency)"),
    UNDECLARED("Used but not declared (reached through another dependency's api)"),
}
