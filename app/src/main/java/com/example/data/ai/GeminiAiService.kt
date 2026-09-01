package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.builder.BuildResult
import com.example.core.BuildDiagnostic
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import com.example.ui.viewmodel.AgentPlanStep
import com.example.ui.viewmodel.AgentStage
import com.example.ui.viewmodel.AgentStepStatus
import com.example.ui.viewmodel.AiFileOperation
import com.example.ui.viewmodel.AiOperationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class StudioBotResult(
    val explanation: String,
    val extractedCode: String? = null,
    val fileOperations: List<AiFileOperation> = emptyList(),
    val isAutoHealed: Boolean = false,
    val isApiKeyRequired: Boolean = false
)

object GeminiAiService {
    private const val TAG = "GeminiAiService"
    const val MODEL_FLASH = "gemini-3.5-flash"
    const val MODEL_PRO = "gemini-3.1-pro-preview"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun resolveApiKey(customApiKey: String?): String {
        return when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }.isNotBlank() &&
                    BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY.trim()
            else -> ""
        }
    }

    /**
     * Tests live connection to Gemini API and measures latency in milliseconds.
     */
    suspend fun testApiKeyConnection(apiKey: String, model: String = MODEL_FLASH): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val resolved = resolveApiKey(apiKey)
        if (resolved.isBlank()) {
            return@withContext Pair(false, "API Key is empty. Please enter your Gemini API Key.")
        }
        val startTime = System.currentTimeMillis()
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$resolved"
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val content = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", "Respond with 'OK'") })
                        }
                        put("parts", parts)
                    }
                    put(content)
                }
                put("contents", contents)
            }
            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            httpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    Pair(true, "✓ Connected successfully ($latency ms) to $model")
                } else {
                    val errorBody = response.body?.string() ?: ""
                    val errorMsg = try {
                        JSONObject(errorBody).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}"
                    }
                    Pair(false, "Connection error: $errorMsg ($latency ms)")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Network error: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Main entry point for Real Gemini AI Studio Bot multi-file code generation & assistance.
     * Generates complete Android applications from scratch (zero to complete).
     */
    suspend fun promptStudioBot(
        userPrompt: String,
        currentProject: ProjectEntity? = null,
        allFiles: List<ProjectFileEntity> = emptyList(),
        activeFile: ProjectFileEntity? = null,
        diagnostics: List<BuildDiagnostic> = emptyList(),
        lastBuildResult: BuildResult? = null,
        customApiKey: String? = null,
        model: String = MODEL_FLASH
    ): StudioBotResult = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)

        if (apiKey.isBlank()) {
            return@withContext StudioBotResult(
                explanation = "🔑 **Gemini API Key Diperlukan**\n\nUntuk menjalankan Agent AI Coding otonom dan membangun aplikasi dari nol sampai selesai secara nyata, silakan masukkan **Gemini API Key** Anda pada ikon kunci API di atas.\n\nAnda dapat memperoleh API Key secara gratis di [Google AI Studio](https://aistudio.google.com/app/apikey).",
                isApiKeyRequired = true
            )
        }

        try {
            val systemInstruction = buildString {
                append("You are Studio Bot, a World-Class Autonomous Principal Android AI Coding Agent integrated into Android Studio.\n")
                append("Your task is to build, refactor, and complete real Android applications from scratch to 100% working state based on the user's instructions.\n\n")
                append("CORE PRINCIPLES & GUIDELINES:\n")
                append("1. Always write complete, production-ready, compile-safe Kotlin and Jetpack Compose code with Material Design 3.\n")
                append("2. Strictly adhere to Clean Architecture (Data Models, Room Entity/DAO if storage needed, ViewModels with StateFlow, Compose UI Screens, Navigation).\n")
                append("3. For every file that needs to be created, modified, or deleted across the project, you MUST output a structured [FILE_OPERATION] block.\n")
                append("4. Never use placeholders, '...', '// TODO', or omit implementation details. Every class and function must be fully written and ready to compile.\n")
                append("5. Ensure all necessary package headers and imports (Jetpack Compose, Coroutines, Material 3, Icons) are properly included.\n\n")
                append("REQUIRED STRUCTURED OUTPUT FORMAT FOR EVERY FILE:\n")
                append("[FILE_OPERATION]\n")
                append("ACTION: CREATE | EDIT | DELETE\n")
                append("PATH: relative/path/to/File.kt\n")
                append("NAME: File.kt\n")
                append("TYPE: KOTLIN | XML | GRADLE | JSON\n")
                append("SUMMARY: Clear summary of what was implemented\n")
                append("[CODE]\n")
                append("... complete file content ...\n")
                append("[/CODE]\n")
                append("[/FILE_OPERATION]\n\n")
                append("After all [FILE_OPERATION] blocks, provide a concise explanation in Indonesian or the user's prompt language explaining what was built.")
            }

            val fullPrompt = buildProjectWidePrompt(
                userPrompt = userPrompt,
                project = currentProject,
                allFiles = allFiles,
                activeFile = activeFile,
                diagnostics = diagnostics,
                lastBuildResult = lastBuildResult
            )

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", fullPrompt) })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val systemObj = JSONObject().apply {
                    val sysParts = JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    }
                    put("parts", sysParts)
                }
                put("systemInstruction", systemObj)

                val configObj = JSONObject().apply {
                    put("temperature", 0.2)
                    put("topP", 0.95)
                }
                put("generationConfig", configObj)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseString = response.body?.string() ?: ""
                    val rootJson = JSONObject(responseString)
                    val candidates = rootJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text")
                    if (!text.isNullOrBlank()) {
                        return@withContext parseStructuredOperations(text)
                    }
                } else {
                    val errBody = response.body?.string() ?: ""
                    val errMsg = try {
                        JSONObject(errBody).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}"
                    }
                    return@withContext StudioBotResult(
                        explanation = "❌ **Gemini API Error ($errMsg)**\n\nPastikan API Key Anda valid dan kuota mencukupi.",
                        isAutoHealed = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API request failed", e)
            return@withContext StudioBotResult(
                explanation = "❌ **Gagal Terhubung ke Gemini AI**\n\nError: ${e.localizedMessage ?: e.message}\nPastikan koneksi internet stabil dan Gemini API Key sudah benar.",
                isAutoHealed = false
            )
        }

        return@withContext StudioBotResult(
            explanation = "Tidak ada respon dari Gemini AI.",
            isAutoHealed = false
        )
    }

    /**
     * Generates a dynamic, structured sub-task plan for ANY autonomous goal using Gemini AI.
     */
    suspend fun generateAutonomousPlan(
        userGoal: String,
        planId: String,
        project: ProjectEntity?,
        allFiles: List<ProjectFileEntity>,
        customApiKey: String? = null,
        model: String = MODEL_FLASH
    ): List<AgentPlanStep> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)

        if (apiKey.isNotBlank()) {
            try {
                val prompt = """You are an Autonomous AI Project Planner.
Deconstruct the following Android application goal into a sequence of 5 distinct sub-tasks to code it from scratch to 100% completion.

Goal: "$userGoal"
Project: ${project?.name ?: "App"} (${project?.packageName ?: "com.example.app"})

Format your response as a valid JSON array of 5 objects with the following keys:
- "order": Integer (1 to 5)
- "title": String (Short sub-task title, e.g. "1. Architecture & Data Entities")
- "description": String (Detailed description of what will be implemented)
- "stage": String (One of: "PLANNING", "GENERATING_CODE", "APPLYING_CHANGES", "COMPILING", "DEPLOYING")

Respond with ONLY the raw JSON array. Do not include markdown formatting or extra text."""

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val requestJson = JSONObject().apply {
                    val contents = JSONArray().apply {
                        val content = JSONObject().apply {
                            val parts = JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            }
                            put("parts", parts)
                        }
                        put(content)
                    }
                    put("contents", contents)
                }

                val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(url).post(body).build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val resStr = response.body?.string() ?: ""
                        val rootJson = JSONObject(resStr)
                        val text = rootJson.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text") ?: ""

                        val cleanJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                        val jsonArray = JSONArray(cleanJson)
                        val steps = mutableListOf<AgentPlanStep>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val order = obj.optInt("order", i + 1)
                            val title = obj.optString("title", "Step $order")
                            val desc = obj.optString("description", "")
                            val stageStr = obj.optString("stage", "GENERATING_CODE").uppercase()
                            val stage = try {
                                AgentStage.valueOf(stageStr)
                            } catch (e: Exception) {
                                when (order) {
                                    1 -> AgentStage.PLANNING
                                    2 -> AgentStage.GENERATING_CODE
                                    3 -> AgentStage.APPLYING_CHANGES
                                    4 -> AgentStage.COMPILING
                                    else -> AgentStage.DEPLOYING
                                }
                            }
                            steps.add(
                                AgentPlanStep(
                                    id = UUID.randomUUID().toString(),
                                    planId = planId,
                                    stepOrder = order,
                                    title = if (title.startsWith("$order.")) title else "$order. $title",
                                    description = desc,
                                    stage = stage,
                                    status = AgentStepStatus.PENDING
                                )
                            )
                        }
                        if (steps.isNotEmpty()) return@withContext steps
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini plan generation failed", e)
            }
        }

        // Standard 5-stage plan structure
        return@withContext listOf(
            AgentPlanStep(
                id = UUID.randomUUID().toString(),
                planId = planId,
                stepOrder = 1,
                title = "1. Architecture & Domain Model Planning",
                description = "Analyze user goal \"$userGoal\" and design data structures, state contracts, and component tree.",
                stage = AgentStage.PLANNING,
                status = AgentStepStatus.PENDING
            ),
            AgentPlanStep(
                id = UUID.randomUUID().toString(),
                planId = planId,
                stepOrder = 2,
                title = "2. Multi-File Code Synthesis & Generation",
                description = "Generate complete Kotlin data models, Room persistence DAOs, ViewModels, and Jetpack Compose screens via Gemini AI.",
                stage = AgentStage.GENERATING_CODE,
                status = AgentStepStatus.PENDING
            ),
            AgentPlanStep(
                id = UUID.randomUUID().toString(),
                planId = planId,
                stepOrder = 3,
                title = "3. File System & Room DB Persistence",
                description = "Atomically persist generated files to local project repository database and workspace filesystem.",
                stage = AgentStage.APPLYING_CHANGES,
                status = AgentStepStatus.PENDING
            ),
            AgentPlanStep(
                id = UUID.randomUUID().toString(),
                planId = planId,
                stepOrder = 4,
                title = "4. Gradle Build Verification & Self-Healing",
                description = "Execute Kotlin compiler task, validate AST, and automatically heal any syntax or import diagnostics.",
                stage = AgentStage.COMPILING,
                status = AgentStepStatus.PENDING
            ),
            AgentPlanStep(
                id = UUID.randomUUID().toString(),
                planId = planId,
                stepOrder = 5,
                title = "5. Streamed APK Deployment to Pixel 9 Pro",
                description = "Deploy built APK to virtual Android 15 device and boot MainActivity in emulator.",
                stage = AgentStage.DEPLOYING,
                status = AgentStepStatus.PENDING
            )
        )
    }

    /**
     * Explains Kotlin / Compose code with deep architectural insights using Gemini AI.
     */
    suspend fun explainCode(
        code: String,
        fileName: String,
        customApiKey: String? = null,
        model: String = MODEL_FLASH
    ): String = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isBlank()) {
            return@withContext "🔑 **Gemini API Key Diperlukan**\nSilakan masukkan Gemini API Key Anda untuk menganalisis arsitektur file `$fileName` menggunakan AI."
        }

        try {
            val prompt = """Analyze and explain the architecture of this file ($fileName) in Indonesian.
Include:
1. Architectural role (Clean Architecture / MVVM / Compose UI)
2. Jetpack Compose UI component tree and layouts
3. State management & reactivity (StateFlow, remember, mutableStateOf)
4. Key methods & logic flow

File content:
```kotlin
$code
```"""
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val content = JSONObject().apply {
                        val parts = JSONArray().apply { put(JSONObject().apply { put("text", prompt) }) }
                        put("parts", parts)
                    }
                    put(content)
                }
                put("contents", contents)
            }
            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val resStr = response.body?.string() ?: ""
                    val text = JSONObject(resStr).optJSONArray("candidates")
                        ?.optJSONObject(0)?.optJSONObject("content")
                        ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                    if (!text.isNullOrBlank()) return@withContext text
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini explain code failed", e)
            return@withContext "❌ Gagal menganalisis kode: ${e.message}"
        }

        return@withContext "Tidak ada respon dari Gemini AI."
    }

    /**
     * Generates Robolectric / JUnit test cases for the active file using Gemini AI.
     */
    suspend fun generateUnitTests(
        code: String,
        fileName: String,
        packageName: String,
        customApiKey: String? = null,
        model: String = MODEL_FLASH
    ): StudioBotResult = withContext(Dispatchers.IO) {
        val testFileName = "${fileName.substringBeforeLast('.')}Test.kt"
        val testFilePath = "app/src/test/java/${packageName.replace('.', '/')}/$testFileName"

        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isBlank()) {
            return@withContext StudioBotResult(
                explanation = "🔑 **Gemini API Key Diperlukan**\nSilakan masukkan Gemini API Key Anda untuk membuat unit test suite secara otomatis.",
                isApiKeyRequired = true
            )
        }

        val prompt = """Write comprehensive JUnit 4 and Robolectric unit tests for the following file ($fileName).
Package: $packageName
Source code:
```kotlin
$code
```

Include [FILE_OPERATION] for $testFilePath with complete, compile-ready test code covering all edge cases, state transitions, and component behavior."""

        return@withContext promptStudioBot(
            userPrompt = prompt,
            customApiKey = apiKey,
            model = model
        )
    }

    private fun buildProjectWidePrompt(
        userPrompt: String,
        project: ProjectEntity?,
        allFiles: List<ProjectFileEntity>,
        activeFile: ProjectFileEntity?,
        diagnostics: List<BuildDiagnostic>,
        lastBuildResult: BuildResult?
    ): String = buildString {
        append("=== USER GOAL / INSTRUCTION ===\n")
        append(userPrompt)
        append("\n\n")

        if (project != null) {
            append("=== TARGET ANDROID PROJECT ===\n")
            append("Project Name: ${project.name}\n")
            append("Package Name: ${project.packageName}\n")
            append("Template: ${project.templateType}\n")
            append("Files in Project: ${allFiles.size}\n\n")
        }

        if (diagnostics.isNotEmpty() || (lastBuildResult != null && !lastBuildResult.isSuccess)) {
            append("=== ACTIVE COMPILER DIAGNOSTICS & ERRORS (PLEASE FIX) ===\n")
            if (lastBuildResult?.errorMessage != null) {
                append("Gradle Compiler Error: ${lastBuildResult.errorMessage}\n")
            }
            diagnostics.forEachIndexed { i, d ->
                append("${i + 1}. [${d.errorType.label}] File: ${d.fileName} Line ${d.line}: ${d.message}\n")
                if (!d.codeSnippet.isNullOrBlank()) {
                    append("   Snippet: ${d.codeSnippet}\n")
                }
            }
            append("\n")
        }

        append("=== PROJECT FILE TREE ===\n")
        allFiles.forEach { file ->
            if (!file.isDirectory) {
                append("- ${file.path} (${file.fileType}, ${file.content.lines().size} lines)\n")
            }
        }
        append("\n")

        append("=== SOURCE CODE CONTEXT ===\n")
        if (activeFile != null) {
            append("--- ACTIVE FILE: ${activeFile.path} ---\n```\n${activeFile.content}\n```\n\n")
        }

        var charBudget = 18000
        for (file in allFiles) {
            if (file.id != activeFile?.id && !file.isDirectory) {
                val len = file.content.length
                if (charBudget - len > 0) {
                    append("--- FILE: ${file.path} ---\n```\n${file.content}\n```\n\n")
                    charBudget -= len
                } else {
                    append("--- FILE: ${file.path} (summary: ${file.content.lines().size} lines) ---\n\n")
                }
            }
        }
    }

    fun parseStructuredOperations(rawResponse: String): StudioBotResult {
        val operations = mutableListOf<AiFileOperation>()
        val operationRegex = Regex(
            "\\[FILE_OPERATION\\][\\s\\S]*?ACTION:\\s*(CREATE|EDIT|DELETE)[\\s\\S]*?PATH:\\s*([^\\n]+)[\\s\\S]*?NAME:\\s*([^\\n]+)[\\s\\S]*?TYPE:\\s*([^\\n]+)[\\s\\S]*?SUMMARY:\\s*([^\\n]+)[\\s\\S]*?\\[CODE\\]([\\s\\S]*?)\\[/CODE\\][\\s\\S]*?\\[/FILE_OPERATION\\]",
            RegexOption.IGNORE_CASE
        )

        val matches = operationRegex.findAll(rawResponse).toList()
        for (match in matches) {
            val actionStr = match.groups[1]?.value?.trim()?.uppercase() ?: "EDIT"
            val path = match.groups[2]?.value?.trim() ?: "MainActivity.kt"
            val name = match.groups[3]?.value?.trim() ?: path.substringAfterLast('/')
            val type = match.groups[4]?.value?.trim() ?: "KOTLIN"
            val summary = match.groups[5]?.value?.trim() ?: "Updated file"
            var code = match.groups[6]?.value?.trim() ?: ""

            if (code.startsWith("```kotlin") || code.startsWith("```java") || code.startsWith("```xml") || code.startsWith("```gradle")) {
                code = code.substringAfter('\n').substringBeforeLast("```").trim()
            } else if (code.startsWith("```")) {
                code = code.removePrefix("```").removeSuffix("```").trim()
            }

            val opType = when (actionStr) {
                "CREATE" -> AiOperationType.CREATE
                "DELETE" -> AiOperationType.DELETE
                else -> AiOperationType.EDIT
            }

            operations.add(
                AiFileOperation(
                    id = UUID.randomUUID().toString(),
                    type = opType,
                    filePath = path,
                    fileName = name,
                    fileType = type,
                    content = code,
                    summary = summary
                )
            )
        }

        val cleanExplanation = rawResponse.replace(
            Regex("\\[FILE_OPERATION\\][\\s\\S]*?\\[/FILE_OPERATION\\]"),
            ""
        ).trim()

        val codeRegex = Regex("```(?:kotlin|java|xml|gradle)?([\\s\\S]*?)```")
        val codeMatch = codeRegex.find(rawResponse)
        val extractedCode = codeMatch?.groups?.get(1)?.value?.trim()

        return StudioBotResult(
            explanation = if (cleanExplanation.isNotBlank()) cleanExplanation else "Berhasil menghasilkan ${operations.size} operasi file proyek.",
            extractedCode = extractedCode,
            fileOperations = operations,
            isAutoHealed = operations.isNotEmpty()
        )
    }
}
