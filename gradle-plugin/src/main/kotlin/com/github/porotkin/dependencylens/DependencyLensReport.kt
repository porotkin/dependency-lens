package com.github.porotkin.dependencylens

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "The resolved dependency graph is not a tracked input")
abstract class DependencyLensReport : DefaultTask() {
    @get:Input
    abstract val compilation: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val usage: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val classpath: ConfigurableFileCollection

    @get:Internal
    abstract val rootComponent: Property<ResolvedComponentResult>

    @get:Internal
    abstract val artifacts: SetProperty<ResolvedArtifactResult>

    @get:Input
    abstract val declarations: MapProperty<String, String>

    @get:Input
    abstract val ignored: SetProperty<String>

    @get:Input
    abstract val classpathConfiguration: Property<String>

    @get:Input
    abstract val catalogAccessors: MapProperty<String, List<String>>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @get:OutputFile
    abstract val findings: RegularFileProperty

    @TaskAction
    fun run() {
        val usageFile = usage.get().asFile

        if (usageFile.exists()) {
            write(analyze(usageFile.readUses()))
        } else {
            writeNoSources()
        }
    }

    private fun analyze(uses: List<Use>): List<Finding> {
        val (failures, resolved) = uses.partition { it.kind == Kind.FAILED }
        val usesByLibrary = resolved.groupBy(Use::library)

        return UsageAnalysis(
            root = rootComponent.get(),
            usesByArtifact = artifacts.get().associateWith { usesByLibrary[it.file.klibUniqueName()].orEmpty() },
            failures = failures,
            declarations = declarations.get(),
            ignored = ignored.get(),
        ).findings()
    }

    private fun write(found: List<Finding>) {
        val table = FindingsTable(compilation.get(), classpathConfiguration.get(), catalogAccessors.get())
        val text = reportText(compilation.get(), found)

        report.get().asFile.writeText(text)
        findings.get().asFile.writeTsv(FindingsTable.HEADER, table.lines(found))

        if (found.isNotEmpty()) {
            logger.lifecycle(text)
        }
    }

    private fun writeNoSources() {
        report.get().asFile.writeText(noSourcesReportText(compilation.get()))
        findings.get().asFile.writeTsv(FindingsTable.HEADER, emptyList())
    }
}
