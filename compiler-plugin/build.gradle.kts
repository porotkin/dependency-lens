plugins {
    id("dependencylens.kotlin-jvm")
}

dependencies {
    implementation(projects.protocol)
    compileOnly(libs.kotlin.compilerEmbeddable)

    testImplementation(libs.kotlin.compilerEmbeddable)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}
