package com.github.porotkin.dependencylens

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

class DependencyLensCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = DependencyLensOption.entries

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        val lensOption = option as? DependencyLensOption
            ?: throw CliOptionProcessingException("Unknown option: ${option.optionName}")

        configuration.put(lensOption.key, value)
    }
}
