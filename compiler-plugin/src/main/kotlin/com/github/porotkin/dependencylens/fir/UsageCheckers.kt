package com.github.porotkin.dependencylens.fir

import com.github.porotkin.dependencylens.Usage
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

internal class UsageCheckers(
    session: FirSession,
    usage: Usage,
) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val fileCheckers = setOf(FileUsageChecker(usage, SessionLookup(session)))
    }
}
