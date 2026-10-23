plugins {
    id("dependencylens.kotlin-jvm")
}

dependencies {
    implementation(projects.protocol)
    compileOnly(libs.kotlin.compilerEmbeddable)

    testImplementation(libs.kotlin.compilerEmbeddable)
}
