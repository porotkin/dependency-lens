import dependencylens.buildlogic.PUBLISH_TO_TEST_REPOSITORY
import dependencylens.buildlogic.testRepository

plugins {
    `kotlin-dsl`
    id("dependencylens.kotlin-jvm")
    id("dependencylens.publishing")
    alias(libs.plugins.gradlePluginPublish)
}

dependencies {
    implementation(projects.protocol)
    compileOnly(libs.kotlin.gradlePluginApi)

    testImplementation(gradleTestKit())
}

val repositoryUrl = providers.gradleProperty("POM_URL")

val pluginId = "io.github.porotkin.dependency-lens"

gradlePlugin {
    website = repositoryUrl
    vcsUrl = repositoryUrl

    plugins {
        register("dependencyLens") {
            id = pluginId
            displayName = "Dependency Lens"
            description = "Reports unused and undeclared dependencies of Kotlin/JS, Kotlin/Wasm and Kotlin/Native compilations"
            implementationClass = "com.github.porotkin.dependencylens.DependencyLensPlugin"
            tags = listOf(
                "kotlin",
                "dependencies",
                "kotlin-js",
                "kotlin-wasm",
                "kotlin-native",
            )
        }
    }
}

tasks.processResources {
    val properties = mapOf(
        "group" to "${project.group}",
        "version" to "${project.version}",
        "kotlin" to libs.versions.kotlin.get(),
    )

    inputs.properties(properties)

    filesMatching("dependency-lens.properties") {
        expand(properties)
    }
}

tasks.test {
    dependsOn(rootProject.subprojects.map { "${it.path}:$PUBLISH_TO_TEST_REPOSITORY" })
    inputs.files(testRepository.map { fileTree(it) { include("**/$version/*") } })
        .withPropertyName("testRepository")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    systemProperty("pluginId", pluginId)
    systemProperty("testRepository", testRepository.get().asFile.absolutePath)
    systemProperty("pluginVersion", "$version")
    systemProperty("kotlinVersion", libs.versions.kotlin.get())
}
