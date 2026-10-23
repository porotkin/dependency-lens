package com.github.porotkin.dependencylens

import java.io.File
import java.util.Properties
import java.util.zip.ZipFile

private const val MANIFEST = "default/manifest"

internal fun File.klibUniqueName(): String? {
    val manifest = Properties()

    when {
        isDirectory -> resolve(MANIFEST).takeIf(File::exists)?.inputStream()?.use(manifest::load)
        extension == "klib" -> ZipFile(this).use { zip ->
            zip.getEntry(MANIFEST)?.let { entry -> zip.getInputStream(entry).use(manifest::load) }
        }
    }

    return manifest.getProperty("unique_name")
}
