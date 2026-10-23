plugins {
    id("dependencylens.kotlin-jvm")
    alias(libs.plugins.mavenPublish)
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
