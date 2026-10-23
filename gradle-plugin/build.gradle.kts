plugins {
    `kotlin-dsl`
    id("dependencylens.kotlin-jvm")
}

dependencies {
    implementation(projects.protocol)
    compileOnly(libs.kotlin.gradlePluginApi)
}

val pluginId = "io.github.porotkin.dependency-lens"

gradlePlugin {
    plugins {
        register("dependencyLens") {
            id = pluginId
            implementationClass = "com.github.porotkin.dependencylens.DependencyLensPlugin"
        }
    }
}

tasks.processResources {
    val properties = mapOf(
        "group" to "${project.group}",
        "version" to "${project.version}",
    )

    inputs.properties(properties)

    filesMatching("dependency-lens.properties") {
        expand(properties)
    }
}
