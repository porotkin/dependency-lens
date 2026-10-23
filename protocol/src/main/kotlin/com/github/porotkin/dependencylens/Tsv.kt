package com.github.porotkin.dependencylens

import java.io.File

private const val TAB = "\t"

private val LINE_OR_COLUMN_BREAK = Regex("""\s*[\t\r\n]\s*""")

fun tsvLine(vararg columns: Any): String =
    columns.joinToString(TAB) { it.toString().replace(LINE_OR_COLUMN_BREAK, " ") }

internal fun String.tsvColumns(): List<String> =
    split(TAB)

internal fun File.readTsv(): List<String> =
    readLines().drop(1)

fun File.writeTsv(
    header: String,
    lines: List<String>,
) {
    parentFile.mkdirs()
    writeText((listOf(header) + lines).joinToString("\n", postfix = "\n"))
}
