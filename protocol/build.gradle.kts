import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("dependencylens.kotlin-jvm")
}

kotlin {
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_2
        languageVersion = KotlinVersion.KOTLIN_2_2
    }
}
