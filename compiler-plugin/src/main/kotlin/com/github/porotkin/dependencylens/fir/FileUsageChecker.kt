package com.github.porotkin.dependencylens.fir

import com.github.porotkin.dependencylens.OPT_IN_FILE
import com.github.porotkin.dependencylens.Usage
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.util.concurrent.atomic.AtomicBoolean

internal class FileUsageChecker(
    private val usage: Usage,
    private val lookup: SessionLookup,
) : FirDeclarationChecker<FirFile>(MppCheckerKind.Common) {
    private val optInsRecorded = AtomicBoolean()

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFile) {
        val file = usage.relative(declaration.sourceFile?.path ?: return)
        usage.compiled(file)

        if (optInsRecorded.compareAndSet(false, true)) {
            usage.catching(OPT_IN_FILE) { recordOptIns() }
        }

        usage.catching(file) { declaration.accept(UsageVisitor(lookup, usage, file)) }
    }

    context(context: CheckerContext)
    private fun recordOptIns() {
        for (marker in context.languageVersionSettings.getFlag(AnalysisFlags.optIn)) {
            val library = lookup.classLike(ClassId.topLevel(FqName(marker)))?.library ?: continue
            usage.optIn(library, marker)
        }
    }
}
