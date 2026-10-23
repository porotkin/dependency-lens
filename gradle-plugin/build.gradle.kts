import dependencylens.buildlogic.PUBLISH_TO_TEST_REPOSITORY
import dependencylens.buildlogic.testRepository

plugins {
    `kotlin-dsl`
    id("dependencylens.kotlin-jvm")
    id("dependencylens.publishing")
}

dependencies {
    implementation(projects.protocol)
    compileOnly(libs.kotlin.gradlePluginApi)

    testImplementation(gradleTestKit())
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

tasks.test {
    dependsOn(rootProject.subprojects.map { "${it.path}:$PUBLISH_TO_TEST_REPOSITORY" })

    systemProperty("pluginId", pluginId)
    systemProperty("testRepository", testRepository.get().asFile.absolutePath)
    systemProperty("pluginVersion", "$version")
    systemProperty("kotlinVersion", libs.versions.kotlin.get())
}
