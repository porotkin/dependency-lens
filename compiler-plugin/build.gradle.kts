plugins {
    id("dependencylens.kotlin-jvm")
    id("dependencylens.publishing")
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
