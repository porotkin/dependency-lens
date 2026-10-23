package com.github.porotkin.dependencylens

import com.github.porotkin.dependencylens.DependencyLensOption.OUTPUT
import com.github.porotkin.dependencylens.DependencyLensOption.ROOT
import com.github.porotkin.dependencylens.fir.UsageFirExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class DependencyLensRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = PLUGIN_ID

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration,
    ) {
        val output = OUTPUT.file(configuration) ?: return
        val usage = Usage(ROOT.file(configuration) ?: return)

        FirExtensionRegistrarAdapter.registerExtension(UsageFirExtensionRegistrar(usage))
        IrGenerationExtension.registerExtension(UsageWriter(usage, output))
    }
}
