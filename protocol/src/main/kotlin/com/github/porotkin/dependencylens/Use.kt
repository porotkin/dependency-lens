package com.github.porotkin.dependencylens

import java.io.File

data class Use(
    val file: String,
    val library: String,
    val kind: Kind,
    val example: String,
) {
    companion object {
        val HEADER: String = tsvLine("file", "library", "kind", "example")
    }
}

fun File.readUses(): List<Use> =
    readTsv().map { line ->
        val (file, library, kind, example) = line.tsvColumns()
        Use(file, library, Kind.valueOf(kind), example)
    }

fun File.writeUses(uses: List<Use>) {
    writeTsv(Use.HEADER, uses.map { tsvLine(it.file, it.library, it.kind, it.example) }.sorted())
}
