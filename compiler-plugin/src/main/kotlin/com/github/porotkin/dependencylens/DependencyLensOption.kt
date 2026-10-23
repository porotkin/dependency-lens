package com.github.porotkin.dependencylens

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.io.File

internal enum class DependencyLensOption(
    override val optionName: String,
    override val description: String,
) : AbstractCliOption {
    OUTPUT(
        optionName = OUTPUT_OPTION,
        description = "File to write the library usage of this module to",
    ),

    ROOT(
        optionName = ROOT_OPTION,
        description = "Directory source paths are recorded relative to",
    ),

    ;

    override val valueDescription: String = "<path>"

    override val required: Boolean = false

    override val allowMultipleOccurrences: Boolean = false

    val key: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create("dependency-lens $optionName")

    fun file(configuration: CompilerConfiguration): File? =
        configuration.get(key)?.let(::File)
}
