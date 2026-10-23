package com.github.porotkin.dependencylens

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class UsageTest {
    @Test
    fun `keeps from the last run only the files not analysed this time that still exist`(
        @TempDir root: File,
        @TempDir elsewhere: File,
    ) {
        File(root, "Kept.kt").writeText("")
        val changed = File(root, "Changed.kt").apply { writeText("") }
        val outside = File(elsewhere, "Outside.kt").apply { writeText("") }
        val output = File(root, "build/usage.tsv").apply { parentFile.mkdirs() }
        output.writeText(
            """
            ${Use.HEADER}
            $OPT_IN_FILE	opt	OPT_IN	opt.Marker
            ${outside.path}	lib	DIRECT	lib/outside
            Changed.kt	lib	DIRECT	lib/changed
            Deleted.kt	lib	DIRECT	lib/deleted
            Kept.kt	lib	DIRECT	lib/kept

            """.trimIndent(),
        )

        val usage = Usage(root)
        val file = usage.relative(changed.path)
        usage.compiled(file)
        usage.catching(file) { throw IllegalStateException("broken\n  here") }
        usage.writeTo(output)

        assertEquals(
            """
            ${Use.HEADER}
            ${outside.path}	lib	DIRECT	lib/outside
            Changed.kt		FAILED	java.lang.IllegalStateException: broken here
            Kept.kt	lib	DIRECT	lib/kept

            """.trimIndent(),
            output.readText(),
        )
    }
}
