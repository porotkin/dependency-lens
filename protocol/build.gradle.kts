import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("dependencylens.kotlin-jvm")
    id("dependencylens.publishing")
    alias(libs.plugins.mavenPublish)
}

kotlin {
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_2
        languageVersion = KotlinVersion.KOTLIN_2_2
    }
}
