package com.github.porotkin.dependencylens.fir

import com.github.porotkin.dependencylens.Usage
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

internal class UsageFirExtensionRegistrar(
    private val usage: Usage,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +{ session: FirSession -> UsageCheckers(session, usage) }
    }
}
