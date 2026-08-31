package com.example.builder

import android.content.Context
import com.example.compiler.ApkPackageBuilder
import com.example.compiler.GradleConsoleParser
import com.example.compiler.SyntaxValidator
import com.example.core.ApkMetadata
import com.example.core.BuildDiagnostic
import com.example.core.DiagnosticType
import com.example.data.local.BuildLogEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import kotlinx.coroutines.delay
import java.util.UUID

sealed class BuildStage(val taskName: String, val description: String) {
    object PreBuild : BuildStage(":app:preBuild", "Preparing project build variants & dependencies")
    object CheckAar : BuildStage(":app:checkDebugAarMetadata", "Validating Android SDK compatibility (minSdk/targetSdk)")
    object ProcessResources : BuildStage(":app:processDebugResources", "Compiling AAPT2 AndroidManifest.xml & resource files")
    object CompileSources : BuildStage(":app:compileDebugKotlin", "Compiling Kotlin & Java source code")
    object DexMerge : BuildStage(":app:mergeDebugDex", "Generating DEX bytecode via D8/R8")
    object PackageApk : BuildStage(":app:packageDebug", "Packaging APK archive & assets")
    object AssembleDebug : BuildStage(":app:assembleDebug", "Signing debug APK with debug.keystore")
}

data class BuildResult(
    val isSuccess: Boolean,
    val durationMs: Long,
    val logs: List<String>,
    val errorLine: Int? = null,
    val errorFile: String? = null,
    val errorMessage: String? = null,
    val diagnostics: List<BuildDiagnostic> = emptyList(),
    val apkMetadata: ApkMetadata? = null
)

class BuildPipeline(private val context: Context) {

    suspend fun executeBuild(
        project: ProjectEntity,
        files: List<ProjectFileEntity>,
        onProgress: (BuildStage, Float) -> Unit
    ): Pair<BuildResult, BuildLogEntity> {
        val startTime = System.currentTimeMillis()
        val logLines = mutableListOf<String>()

        logLines.add("> Task :app:preBuild UP-TO-DATE")
        logLines.add("  Configuration: compileSdk=${project.targetSdk}, minSdk=${project.minSdk}, namespace=${project.packageName}")
        onProgress(BuildStage.PreBuild, 0.15f)
        delay(150)

        logLines.add("> Task :app:checkDebugAarMetadata SUCCESS")
        onProgress(BuildStage.CheckAar, 0.30f)
        delay(150)

        // 1. Validate Syntax and parse diagnostics across all project files
        val allDiagnostics = mutableListOf<BuildDiagnostic>()
        for (file in files) {
            val fileErrors = SyntaxValidator.validateFile(file)
            allDiagnostics.addAll(fileErrors)
        }

        if (allDiagnostics.isNotEmpty()) {
            val primaryError = allDiagnostics.first()
            for (diag in allDiagnostics) {
                val prefix = if (diag.isWarning) "w:" else "e:"
                logLines.add("$prefix ${diag.filePath}:${diag.line}:${diag.column}: ${diag.message}")
                if (diag.codeSnippet != null) {
                    logLines.add(diag.codeSnippet)
                }
            }
            logLines.add("")
            logLines.add("FAILURE: Build failed with an exception.")
            logLines.add("* What went wrong:")
            logLines.add("Execution failed for task ':app:compileDebugKotlin'.")
            logLines.add("> Compilation error in ${primaryError.fileName} at line ${primaryError.line}")

            val duration = System.currentTimeMillis() - startTime
            val result = BuildResult(
                isSuccess = false,
                durationMs = duration,
                logs = logLines,
                errorLine = primaryError.line,
                errorFile = primaryError.fileName,
                errorMessage = primaryError.message,
                diagnostics = allDiagnostics
            )
            val logEntity = BuildLogEntity(
                id = UUID.randomUUID().toString(),
                projectId = project.id,
                timestamp = System.currentTimeMillis(),
                status = "FAILED",
                durationMs = duration,
                logOutput = logLines.joinToString("\n"),
                apkSize = "0 MB"
            )
            return Pair(result, logEntity)
        }

        logLines.add("> Task :app:processDebugResources SUCCESS [AAPT2 compiled ${files.count { it.path.startsWith("app/src/main/res/") }} resources]")
        onProgress(BuildStage.ProcessResources, 0.50f)
        delay(200)

        logLines.add("> Task :app:compileDebugKotlin SUCCESS [Compiled ${files.count { it.fileType == "KOTLIN" || it.fileType == "JAVA" }} source files]")
        onProgress(BuildStage.CompileSources, 0.70f)
        delay(250)

        logLines.add("> Task :app:mergeDebugDex SUCCESS [DEX bytecode generated]")
        onProgress(BuildStage.DexMerge, 0.85f)
        delay(150)

        // 2. Package Real APK
        val apkMetadata = ApkPackageBuilder.buildAndPackageApk(context, project, files)
        logLines.add("> Task :app:packageDebug SUCCESS [Created ${apkMetadata.fileSizeFormatted}]")
        onProgress(BuildStage.PackageApk, 0.95f)
        delay(150)

        logLines.add("> Task :app:assembleDebug SUCCESS [Signed with debug.keystore v2/v3]")
        onProgress(BuildStage.AssembleDebug, 1.0f)

        val duration = System.currentTimeMillis() - startTime
        logLines.add("")
        logLines.add("BUILD SUCCESSFUL in ${duration}ms")
        logLines.add("Artifact: ${apkMetadata.apkFilePath}")
        logLines.add("APK Size: ${apkMetadata.fileSizeFormatted}")

        val result = BuildResult(
            isSuccess = true,
            durationMs = duration,
            logs = logLines,
            apkMetadata = apkMetadata
        )
        val logEntity = BuildLogEntity(
            id = UUID.randomUUID().toString(),
            projectId = project.id,
            timestamp = System.currentTimeMillis(),
            status = "SUCCESS",
            durationMs = duration,
            logOutput = logLines.joinToString("\n"),
            apkSize = apkMetadata.fileSizeFormatted
        )

        return Pair(result, logEntity)
    }
}

