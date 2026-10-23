package com.github.porotkin.dependencylens

import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal const val OPT_IN_FILE = "<opt-in>"

internal class Usage(
    private val root: File,
) {
    private data class Key(
        val file: String,
        val library: String,
        val kind: Kind,
    )

    private val compiled: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val examples = ConcurrentHashMap<Key, String>()

    fun relative(path: String): String =
        File(path).relativeToOrNull(root)
            ?.takeUnless { it.startsWith("..") }
            ?.invariantSeparatorsPath
            ?: path

    fun compiled(file: String) {
        compiled += file
    }

    fun record(
        file: String,
        library: KotlinLibrary,
        kind: Kind,
        example: () -> String,
    ) {
        put(Key(file, library.uniqueName, kind), example)
    }

    fun optIn(
        library: KotlinLibrary,
        marker: String,
    ) {
        put(Key(OPT_IN_FILE, library.uniqueName, Kind.OPT_IN)) { marker }
    }

    fun catching(
        file: String,
        analysis: () -> Unit,
    ) {
        try {
            analysis()
        } catch (e: Exception) {
            put(Key(file, "", Kind.FAILED), e::toString)
        }
    }

    fun writeTo(output: File) {
        val kept = output.takeIf(File::exists)?.readUses().orEmpty().filter(::isKept)
        val fresh = examples.map { (key, example) -> Use(key.file, key.library, key.kind, example) }

        output.writeUses(kept + fresh)
    }

    private fun put(
        key: Key,
        example: () -> String,
    ) {
        examples.computeIfAbsent(key) { example() }
    }

    private fun isKept(use: Use): Boolean =
        use.file != OPT_IN_FILE && use.file !in compiled && root.resolve(use.file).exists()
}
