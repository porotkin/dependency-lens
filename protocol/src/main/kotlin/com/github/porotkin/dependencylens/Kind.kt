package com.github.porotkin.dependencylens

enum class Kind {
    DIRECT,
    SIGNATURE,
    SUPERTYPE,
    IMPORT,
    OPT_IN,
    FAILED,
}
