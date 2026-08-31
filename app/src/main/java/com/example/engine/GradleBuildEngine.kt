package com.example.engine

import com.example.data.local.BuildLogEntity
import com.example.data.local.ProjectFileEntity
import kotlinx.coroutines.delay
import java.util.UUID

sealed class BuildStage(val taskName: String, val description: String) {
    object PreBuild : BuildStage(":app:preBuild", "Preparing project build variants")
    object CheckDependencies : BuildStage(":app:checkDebugAarMetadata", "Validating Android libraries & SDK compatibility")
    object ProcessResources : BuildStage(":app:processDebugResources", "Compiling AAPT2 AndroidManifest.xml & resource files")
    object CompileKotlin : BuildStage(":app:compileDebugKotlin", "Compiling Kotlin 2.2 Jetpack Compose sources")
    object KspCodeGeneration : BuildStage(":app:kspDebugKotlin", "Executing KSP Annotation Processors")
    object MergeDex : BuildStage(":app:mergeDebugDex", "Merging DEX bytecode for ART execution")
    object PackageApk : BuildStage(":app:packageDebug", "Packaging unsigned APK bundle")
    object AssembleDebug : BuildStage(":app:assembleDebug", "Signing debug APK with debug.keystore")
}

data class BuildResult(
    val isSuccess: Boolean,
    val durationMs: Long,
    val logs: List<String>,
    val errorLine: Int? = null,
    val errorFile: String? = null,
    val errorMessage: String? = null,
    val apkSize: String = "12.8 MB"
)

object GradleBuildEngine {

    suspend fun executeBuild(
        projectId: String,
        files: List<ProjectFileEntity>,
        onProgress: (BuildStage, Float) -> Unit
    ): Pair<BuildResult, BuildLogEntity> {
        val startTime = System.currentTimeMillis()
        val logLines = mutableListOf<String>()

        logLines.add("> Task :app:preBuild UP-TO-DATE")
        onProgress(BuildStage.PreBuild, 0.15f)
        delay(250)

        logLines.add("> Task :app:checkDebugAarMetadata SUCCESS")
        onProgress(BuildStage.CheckDependencies, 0.30f)
        delay(250)

        // Validate syntax on Kotlin files
        for (file in files) {
            if (file.fileType == "KOTLIN") {
                val syntaxError = checkKotlinSyntax(file.content, file.name)
                if (syntaxError != null) {
                    logLines.add("e: ${file.path}:${syntaxError.line}: ${syntaxError.message}")
                    logLines.add("")
                    logLines.add("FAILURE: Build failed with an exception.")
                    logLines.add("* What went wrong:")
                    logLines.add("Execution failed for task ':app:compileDebugKotlin'.")
                    logLines.add("> Compilation error in ${file.name} at line ${syntaxError.line}")

                    val duration = System.currentTimeMillis() - startTime
                    val result = BuildResult(
                        isSuccess = false,
                        durationMs = duration,
                        logs = logLines,
                        errorLine = syntaxError.line,
                        errorFile = file.name,
                        errorMessage = syntaxError.message
                    )
                    val logEntity = BuildLogEntity(
                        id = UUID.randomUUID().toString(),
                        projectId = projectId,
                        timestamp = System.currentTimeMillis(),
                        status = "FAILED",
                        durationMs = duration,
                        logOutput = logLines.joinToString("\n"),
                        apkSize = "0 MB"
                    )
                    return Pair(result, logEntity)
                }
            }
        }

        logLines.add("> Task :app:processDebugResources SUCCESS [AAPT2 packed 32 resources]")
        onProgress(BuildStage.ProcessResources, 0.50f)
        delay(300)

        logLines.add("> Task :app:compileDebugKotlin SUCCESS [Kotlin 2.2.10 Compose Plugin]")
        onProgress(BuildStage.CompileKotlin, 0.70f)
        delay(350)

        logLines.add("> Task :app:kspDebugKotlin SUCCESS [0 generated sources]")
        onProgress(BuildStage.KspCodeGeneration, 0.85f)
        delay(200)

        logLines.add("> Task :app:mergeDebugDex SUCCESS [DEX classes: 182]")
        onProgress(BuildStage.MergeDex, 0.92f)
        delay(200)

        logLines.add("> Task :app:packageDebug SUCCESS")
        onProgress(BuildStage.PackageApk, 0.98f)
        delay(200)

        logLines.add("> Task :app:assembleDebug SUCCESS")
        onProgress(BuildStage.AssembleDebug, 1.0f)

        val duration = System.currentTimeMillis() - startTime
        logLines.add("")
        logLines.add("BUILD SUCCESSFUL in ${duration}ms")
        logLines.add("APK Built: app-debug.apk (12.8 MB)")

        val result = BuildResult(
            isSuccess = true,
            durationMs = duration,
            logs = logLines,
            apkSize = "12.8 MB"
        )
        val logEntity = BuildLogEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            timestamp = System.currentTimeMillis(),
            status = "SUCCESS",
            durationMs = duration,
            logOutput = logLines.joinToString("\n"),
            apkSize = "12.8 MB"
        )

        return Pair(result, logEntity)
    }

    private data class SyntaxIssue(val line: Int, val message: String)

    private fun checkKotlinSyntax(content: String, fileName: String): SyntaxIssue? {
        val lines = content.lines()
        var openBraces = 0
        var openParens = 0

        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1
            // Check unclosed strings
            val quotes = line.count { it == '"' }
            if (quotes % 2 != 0 && !line.trim().startsWith("//")) {
                return SyntaxIssue(lineNumber, "Unterminated string literal")
            }

            for (char in line) {
                when (char) {
                    '{' -> openBraces++
                    '}' -> openBraces--
                    '(' -> openParens++
                    ')' -> openParens--
                }
            }

            if (openBraces < 0) {
                return SyntaxIssue(lineNumber, "Unexpected closing brace '}'")
            }
            if (openParens < 0) {
                return SyntaxIssue(lineNumber, "Unexpected closing parenthesis ')'")
            }
        }

        if (openBraces > 0) {
            return SyntaxIssue(lines.size, "Missing closing brace '}' at end of file")
        }
        if (openParens > 0) {
            return SyntaxIssue(lines.size, "Missing closing parenthesis ')' at end of file")
        }

        return null
    }
}
