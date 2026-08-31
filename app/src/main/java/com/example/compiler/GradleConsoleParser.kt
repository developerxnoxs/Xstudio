package com.example.compiler

import com.example.core.BuildDiagnostic
import com.example.core.DiagnosticType
import com.example.data.local.ProjectFileEntity

object GradleConsoleParser {

    /**
     * Parses raw Gradle console / compiler output lines and maps them to project files with exact line numbers.
     */
    fun parseLogOutput(
        logLines: List<String>,
        projectFiles: List<ProjectFileEntity>
    ): List<BuildDiagnostic> {
        val diagnostics = mutableListOf<BuildDiagnostic>()

        for (rawLine in logLines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            // 1. Kotlin / Java compiler error patterns:
            // Format A: "e: /path/to/File.kt:14:25: Unresolved reference: foo"
            // Format B: "e: file:///path/to/File.kt:14:25 Unresolved reference 'foo'"
            // Format C: "w: /path/to/File.kt: (14, 25): Variable is never used"
            // Format D: "e: [ksp] /path/to/File.kt:14: Cannot generate adapter"
            val kotlinMatcher = Regex("^(e|w):\\s*(?:\\[ksp\\]\\s*)?(?:file://)?([^:]+):(?:\\s*\\(?(\\d+)(?:,\\s*(\\d+)\\)?)?:?(\\d+)?:?\\s*)?\\s*(.*)$")
            val matchKt = kotlinMatcher.find(line)
            if (matchKt != null) {
                val isWarning = matchKt.groupValues[1] == "w"
                val rawPath = matchKt.groupValues[2].trim()
                val lineStr = matchKt.groupValues[3].ifEmpty { matchKt.groupValues[4] }
                val colStr = matchKt.groupValues[5]
                val message = matchKt.groupValues[6].trim().ifEmpty { line }

                val lineNum = lineStr.toIntOrNull() ?: 1
                val colNum = colStr.toIntOrNull() ?: 1

                val mappedFile = resolveFile(rawPath, projectFiles)
                val snippet = extractSnippet(mappedFile, lineNum)
                val suggestion = generateSuggestion(message, mappedFile?.fileType ?: "KOTLIN")

                diagnostics.add(
                    BuildDiagnostic(
                        fileName = mappedFile?.name ?: extractFileName(rawPath),
                        filePath = mappedFile?.path ?: rawPath,
                        line = lineNum,
                        column = colNum,
                        message = cleanCompilerMessage(message),
                        rawLogLine = line,
                        isWarning = isWarning,
                        errorType = if (isWarning) DiagnosticType.LINT_WARNING else DiagnosticType.KOTLIN_COMPILATION,
                        codeSnippet = snippet,
                        suggestion = suggestion
                    )
                )
                continue
            }

            // 2. Java standard javac error:
            // Format: "/path/to/MainActivity.java:18: error: cannot find symbol"
            val javaMatcher = Regex("^([^:]+\\.java):(\\d+):\\s*(error|warning):\\s*(.*)$")
            val matchJava = javaMatcher.find(line)
            if (matchJava != null) {
                val rawPath = matchJava.groupValues[1].trim()
                val lineNum = matchJava.groupValues[2].toIntOrNull() ?: 1
                val isWarning = matchJava.groupValues[3].equals("warning", ignoreCase = true)
                val message = matchJava.groupValues[4].trim()

                val mappedFile = resolveFile(rawPath, projectFiles)
                val snippet = extractSnippet(mappedFile, lineNum)
                val suggestion = generateSuggestion(message, "JAVA")

                diagnostics.add(
                    BuildDiagnostic(
                        fileName = mappedFile?.name ?: extractFileName(rawPath),
                        filePath = mappedFile?.path ?: rawPath,
                        line = lineNum,
                        column = 1,
                        message = cleanCompilerMessage(message),
                        rawLogLine = line,
                        isWarning = isWarning,
                        errorType = if (isWarning) DiagnosticType.LINT_WARNING else DiagnosticType.JAVA_COMPILATION,
                        codeSnippet = snippet,
                        suggestion = suggestion
                    )
                )
                continue
            }

            // 3. AAPT2 / Android XML Resource error:
            // Format A: "app/src/main/res/layout/activity_main.xml:12: AAPT: error: resource not found"
            // Format B: "AAPT: error: resource string/app_name not found (at app/src/main/res/layout/activity_main.xml:15)"
            val aaptMatcher1 = Regex("^([^:]+\\.xml):(\\d+):(?:\\s*(\\d+):)?\\s*AAPT:\\s*(error|warning):\\s*(.*)$")
            val matchAapt1 = aaptMatcher1.find(line)
            if (matchAapt1 != null) {
                val rawPath = matchAapt1.groupValues[1].trim()
                val lineNum = matchAapt1.groupValues[2].toIntOrNull() ?: 1
                val colNum = matchAapt1.groupValues[3].toIntOrNull() ?: 1
                val isWarning = matchAapt1.groupValues[4].equals("warning", ignoreCase = true)
                val message = matchAapt1.groupValues[5].trim()

                val mappedFile = resolveFile(rawPath, projectFiles)
                val snippet = extractSnippet(mappedFile, lineNum)
                val suggestion = generateSuggestion(message, "XML")

                diagnostics.add(
                    BuildDiagnostic(
                        fileName = mappedFile?.name ?: extractFileName(rawPath),
                        filePath = mappedFile?.path ?: rawPath,
                        line = lineNum,
                        column = colNum,
                        message = cleanCompilerMessage(message),
                        rawLogLine = line,
                        isWarning = isWarning,
                        errorType = DiagnosticType.AAPT2_RESOURCE,
                        codeSnippet = snippet,
                        suggestion = suggestion
                    )
                )
                continue
            }

            val aaptMatcher2 = Regex("^AAPT:\\s*error:\\s*(.*)\\s*\\(at\\s+([^:]+\\.xml):(\\d+)\\)$")
            val matchAapt2 = aaptMatcher2.find(line)
            if (matchAapt2 != null) {
                val message = matchAapt2.groupValues[1].trim()
                val rawPath = matchAapt2.groupValues[2].trim()
                val lineNum = matchAapt2.groupValues[3].toIntOrNull() ?: 1

                val mappedFile = resolveFile(rawPath, projectFiles)
                val snippet = extractSnippet(mappedFile, lineNum)
                val suggestion = generateSuggestion(message, "XML")

                diagnostics.add(
                    BuildDiagnostic(
                        fileName = mappedFile?.name ?: extractFileName(rawPath),
                        filePath = mappedFile?.path ?: rawPath,
                        line = lineNum,
                        column = 1,
                        message = cleanCompilerMessage(message),
                        rawLogLine = line,
                        isWarning = false,
                        errorType = DiagnosticType.AAPT2_RESOURCE,
                        codeSnippet = snippet,
                        suggestion = suggestion
                    )
                )
                continue
            }

            // 4. Manifest Merger error:
            // Format: "[AndroidManifest.xml:14] Manifest merger failed : Attribute application@theme value..."
            val manifestMatcher = Regex("^\\[([^:]+\\.xml):(\\d+)\\]\\s*Manifest merger failed\\s*:\\s*(.*)$")
            val matchManifest = manifestMatcher.find(line)
            if (matchManifest != null) {
                val rawPath = matchManifest.groupValues[1].trim()
                val lineNum = matchManifest.groupValues[2].toIntOrNull() ?: 1
                val message = matchManifest.groupValues[3].trim()

                val mappedFile = resolveFile(rawPath, projectFiles) ?: projectFiles.find { it.name == "AndroidManifest.xml" }
                val snippet = extractSnippet(mappedFile, lineNum)

                diagnostics.add(
                    BuildDiagnostic(
                        fileName = mappedFile?.name ?: "AndroidManifest.xml",
                        filePath = mappedFile?.path ?: "app/src/main/AndroidManifest.xml",
                        line = lineNum,
                        column = 1,
                        message = "Manifest Merger: $message",
                        rawLogLine = line,
                        isWarning = false,
                        errorType = DiagnosticType.MANIFEST_MERGER,
                        codeSnippet = snippet,
                        suggestion = "Verify that android:theme, package name, and exported attributes match in AndroidManifest.xml"
                    )
                )
                continue
            }

            // 5. Gradle Kotlin DSL Build Script error:
            // Format: "build.gradle.kts:24:1: Expression 'foo' cannot be invoked"
            val gradleDslMatcher = Regex("^([^:]+\\.gradle(?:\\.kts)?):(\\d+):(?:(\\d+):)?\\s*(.*)$")
            val matchGradle = gradleDslMatcher.find(line)
            if (matchGradle != null) {
                val rawPath = matchGradle.groupValues[1].trim()
                val lineNum = matchGradle.groupValues[2].toIntOrNull() ?: 1
                val colNum = matchGradle.groupValues[3].toIntOrNull() ?: 1
                val message = matchGradle.groupValues[4].trim()

                val mappedFile = resolveFile(rawPath, projectFiles)
                val snippet = extractSnippet(mappedFile, lineNum)

                diagnostics.add(
                    BuildDiagnostic(
                        fileName = mappedFile?.name ?: extractFileName(rawPath),
                        filePath = mappedFile?.path ?: rawPath,
                        line = lineNum,
                        column = colNum,
                        message = cleanCompilerMessage(message),
                        rawLogLine = line,
                        isWarning = false,
                        errorType = DiagnosticType.GRADLE_SCRIPT,
                        codeSnippet = snippet,
                        suggestion = "Check plugins block, dependencies syntax, or dot-notation in libs.versions.toml"
                    )
                )
                continue
            }

            // 6. Generic "Compilation error in File at line X"
            val genericMatcher = Regex("> Compilation error in ([^\\s]+) at line (\\d+)")
            val matchGen = genericMatcher.find(line)
            if (matchGen != null) {
                val rawPath = matchGen.groupValues[1].trim()
                val lineNum = matchGen.groupValues[2].toIntOrNull() ?: 1
                val mappedFile = resolveFile(rawPath, projectFiles)

                diagnostics.add(
                    BuildDiagnostic(
                        fileName = mappedFile?.name ?: extractFileName(rawPath),
                        filePath = mappedFile?.path ?: rawPath,
                        line = lineNum,
                        column = 1,
                        message = "Compilation error at line $lineNum",
                        rawLogLine = line,
                        isWarning = false,
                        errorType = DiagnosticType.KOTLIN_COMPILATION,
                        codeSnippet = extractSnippet(mappedFile, lineNum),
                        suggestion = "Inspect syntax near line $lineNum"
                    )
                )
            }
        }

        return diagnostics
    }

    /**
     * Resolves a file path string from Gradle output to an existing ProjectFileEntity.
     * Also handles generated files mapping back to origin files.
     */
    fun resolveFile(rawPath: String, projectFiles: List<ProjectFileEntity>): ProjectFileEntity? {
        val cleanPath = rawPath.replace("\\", "/").removePrefix("file://")
        val fileName = cleanPath.substringAfterLast("/")

        // Exact path match
        projectFiles.find { it.path == cleanPath }?.let { return it }

        // Suffix path match (e.g. "app/src/main/java/com/example/MainActivity.kt")
        projectFiles.find { cleanPath.endsWith(it.path) || it.path.endsWith(cleanPath) }?.let { return it }

        // Filename match
        projectFiles.find { it.name.equals(fileName, ignoreCase = true) }?.let { return it }

        // Mapping generated files:
        // E.g., "ActivityMainBinding.java" -> "activity_main.xml"
        if (fileName.endsWith("Binding.java") || fileName.endsWith("Binding.kt")) {
            val layoutName = fileName.removeSuffix("Binding.java").removeSuffix("Binding.kt")
                .replace(Regex("([a-z])([A-Z])"), "$1_$2")
                .lowercase() + ".xml"
            projectFiles.find { it.name == layoutName }?.let { return it }
        }

        // E.g. "R.java" or "BR.java" -> "AndroidManifest.xml" or strings.xml
        if (fileName == "R.java" || fileName == "R.kt") {
            return projectFiles.find { it.name == "strings.xml" }
                ?: projectFiles.find { it.name == "AndroidManifest.xml" }
        }

        return null
    }

    /**
     * Extracts a 3-line code preview snippet around the given line number with line indicators.
     */
    fun extractSnippet(file: ProjectFileEntity?, lineNum: Int): String? {
        if (file == null || file.content.isBlank()) return null
        val lines = file.content.lines()
        if (lines.isEmpty()) return null

        val targetIdx = (lineNum - 1).coerceIn(0, lines.lastIndex)
        val startIdx = (targetIdx - 1).coerceAtLeast(0)
        val endIdx = (targetIdx + 1).coerceAtMost(lines.lastIndex)

        val sb = StringBuilder()
        for (i in startIdx..endIdx) {
            val currentLineNumber = i + 1
            val prefix = if (currentLineNumber == lineNum) "> " else "  "
            val lineNumPadded = currentLineNumber.toString().padStart(3, ' ')
            sb.append("$prefix$lineNumPadded | ${lines[i]}")
            if (i < endIdx) sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * Generates intelligent auto-suggestions based on the compiler error message.
     */
    private fun generateSuggestion(message: String, fileType: String): String {
        val lower = message.lowercase()
        return when {
            lower.contains("unresolved reference") -> {
                val ref = message.substringAfter("reference:").substringAfter("reference '").substringBefore("'").trim()
                if (ref.isNotEmpty()) {
                    "Missing import for '$ref'. Check if required dependency is added or add 'import ...$ref'"
                } else {
                    "Verify symbol name spelling or add missing import statement."
                }
            }
            lower.contains("type mismatch") -> "Ensure the variable or function argument matches the expected type."
            lower.contains("closing brace") || lower.contains("unclosed") || lower.contains("expecting '}'") ->
                "Check matching opening and closing braces '{ ... }'."
            lower.contains("unexpected closing") -> "Check for extra closing parenthesis ')' or brace '}'."
            lower.contains("resource not found") || lower.contains("no resource identifier") ->
                "Verify resource ID exists in strings.xml, colors.xml, or layout directory."
            lower.contains("mismatched closing tag") || lower.contains("unclosed xml tag") ->
                "Ensure every XML opening tag <tag> has a matching closing tag </tag> or is self-closing <tag />."
            lower.contains("cannot find symbol") -> "Symbol not found in current scope. Check method/variable definition or class import."
            lower.contains("modifier") && lower.contains("composable") ->
                "Remember @Composable functions can only be called from other @Composable functions or SetContent {}."
            else -> "Inspect the code at the highlighted line."
        }
    }

    private fun cleanCompilerMessage(message: String): String {
        return message
            .replace(Regex("^error:\\s*"), "")
            .replace(Regex("^warning:\\s*"), "")
            .trim()
    }

    private fun extractFileName(path: String): String {
        return path.replace("\\", "/").substringAfterLast("/")
    }
}
