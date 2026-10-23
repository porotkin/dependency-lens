import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("dependencylens.kotlin-jvm")
    alias(libs.plugins.mavenPublish)
}

kotlin {
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_2
        languageVersion = KotlinVersion.KOTLIN_2_2
    }
}
