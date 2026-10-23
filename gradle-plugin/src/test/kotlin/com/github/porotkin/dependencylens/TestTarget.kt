package com.github.porotkin.dependencylens

enum class TestTarget(
    val id: String,
    val classpath: String,
    val cacheableCompilation: Boolean,
) {
    JS("js", "jsCompileClasspath", cacheableCompilation = true),
    WASM_JS("wasmJs", "wasmJsCompileClasspath", cacheableCompilation = true),
    LINUX_X64("linuxX64", "linuxX64CompileKlibraries", cacheableCompilation = false),
}
