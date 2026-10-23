package com.github.porotkin.dependencylens

enum class TestTarget(
    val id: String,
    val classpath: String,
) {
    JS("js", "jsCompileClasspath"),
    WASM_JS("wasmJs", "wasmJsCompileClasspath"),
}
