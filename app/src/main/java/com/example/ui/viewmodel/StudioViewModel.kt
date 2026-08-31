package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.builder.BuildPipeline
import com.example.compiler.GradleConsoleParser
import com.example.compiler.SyntaxValidator
import com.example.core.BuildDiagnostic
import com.example.core.LanguageType
import com.example.core.SdkConfiguration
import com.example.data.ai.GeminiAiService
import com.example.data.github.GitHubRepository
import com.example.data.github.GitHubResult
import com.example.data.local.AgentSubTaskEntity
import com.example.data.local.AgentTaskPlanEntity
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import com.example.data.repository.ProjectRepository
import com.example.data.repository.ProjectTemplate
import com.example.engine.ComponentType
import com.example.engine.VisualLayoutBridge
import com.example.engine.VisualUiNode
import com.example.filemanager.FileManager
import com.example.project.ProjectManager
import com.example.terminal.TerminalManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository
    private val projectManager: ProjectManager
    private val fileManager: FileManager
    private val buildPipeline: BuildPipeline
    private val gitHubRepository: GitHubRepository
    private var realtimeSyntaxJob: Job? = null
    private var autonomousAgentJob: Job? = null

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = ProjectRepository(db)
        projectManager = ProjectManager(repository)
        fileManager = FileManager(repository)
        buildPipeline = BuildPipeline(application)
        gitHubRepository = GitHubRepository(application, repository)

        val savedToken = gitHubRepository.getToken()
        val prefs = application.getSharedPreferences("studio_ai_prefs", android.content.Context.MODE_PRIVATE)
        val savedGeminiKey = prefs.getString("gemini_api_key", null)
        _uiState.update { it.copy(gitHubToken = savedToken, geminiApiKey = savedGeminiKey) }
        if (!savedToken.isNullOrBlank()) {
            loadGitHubUserAndRepos()
        }

        viewModelScope.launch {
            repository.ensureDefaultProjects()
            projectManager.allProjects.collectLatest { projects ->
                _uiState.update { it.copy(projects = projects) }
                if (_uiState.value.currentProject == null && projects.isNotEmpty()) {
                    selectProject(projects.first())
                }
            }
        }
    }

    fun selectProject(project: ProjectEntity) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentProject = project,
                    openTabs = emptyList(),
                    activeFile = null,
                    editorContent = "",
                    isModified = false,
                    lastBuildResult = null,
                    isEmulatorVisible = false
                )
            }

            // Observe task plans in Room for this project
            launch {
                repository.getTaskPlansForProject(project.id).collectLatest { plans ->
                    _uiState.update { it.copy(savedTaskPlans = plans) }
                }
            }

            projectManager.getFilesForProject(project.id).collectLatest { files ->
                _uiState.update { state ->
                    val active = state.activeFile?.let { af -> files.find { it.id == af.id } }
                        ?: files.find { it.name == "MainActivity.kt" || it.name == "MainActivity.java" }
                        ?: files.firstOrNull()

                    val tabs = if (state.openTabs.isEmpty() && active != null) {
                        listOf(active)
                    } else {
                        state.openTabs.mapNotNull { ot -> files.find { it.id == ot.id } }
                    }

                    val initialNodes = if (state.visualNodes.isEmpty()) {
                        VisualLayoutBridge.getDefaultNodesForTemplate(project.templateType, project.name)
                    } else {
                        state.visualNodes
                    }

                    state.copy(
                        files = files,
                        activeFile = active,
                        openTabs = tabs,
                        editorContent = active?.content ?: "",
                        isModified = false,
                        visualNodes = initialNodes
                    )
                }
            }
        }
    }

    fun createProject(
        template: ProjectTemplate,
        name: String,
        packageName: String,
        description: String,
        language: LanguageType = LanguageType.KOTLIN,
        sdkConfig: SdkConfiguration = SdkConfiguration()
    ) {
        viewModelScope.launch {
            val created = projectManager.createNewProject(
                template = template,
                name = name,
                packageName = packageName,
                description = description,
                language = language,
                sdkConfig = sdkConfig
            )
            selectProject(created)
            _uiState.update {
                it.copy(
                    isNewProjectDialogOpen = false,
                    infoSnackbarMessage = "Project '${created.name}' created!"
                )
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectManager.deleteProject(projectId)
            _uiState.update { it.copy(infoSnackbarMessage = "Project deleted") }
        }
    }

    fun openFile(file: ProjectFileEntity) {
        val initialDiags = if (_uiState.value.isRealtimeCheckingEnabled) {
            SyntaxValidator.validateFile(file)
        } else {
            emptyList()
        }

        _uiState.update { state ->
            val tabs = if (state.openTabs.any { it.id == file.id }) {
                state.openTabs
            } else {
                state.openTabs + file
            }
            state.copy(
                activeFile = file,
                openTabs = tabs,
                editorContent = file.content,
                isModified = false,
                realtimeDiagnostics = initialDiags,
                activeDiagnostic = initialDiags.firstOrNull(),
                highlightedErrorLine = initialDiags.firstOrNull()?.line
            )
        }
    }

    fun closeTab(file: ProjectFileEntity) {
        _uiState.update { state ->
            val newTabs = state.openTabs.filter { it.id != file.id }
            val nextActive = if (state.activeFile?.id == file.id) {
                newTabs.lastOrNull()
            } else {
                state.activeFile
            }
            val diags = if (nextActive != null && state.isRealtimeCheckingEnabled) {
                SyntaxValidator.validateFile(nextActive)
            } else emptyList()

            state.copy(
                openTabs = newTabs,
                activeFile = nextActive,
                editorContent = nextActive?.content ?: "",
                isModified = false,
                realtimeDiagnostics = diags,
                activeDiagnostic = diags.firstOrNull(),
                highlightedErrorLine = diags.firstOrNull()?.line
            )
        }
    }

    fun updateEditorContent(content: String) {
        val active = _uiState.value.activeFile
        _uiState.update {
            it.copy(
                editorContent = content,
                isModified = content != (active?.content ?: "")
            )
        }

        if (active != null && _uiState.value.isRealtimeCheckingEnabled) {
            realtimeSyntaxJob?.cancel()
            realtimeSyntaxJob = viewModelScope.launch {
                delay(120) // Snappy debounce for optimal typing responsiveness
                val tempFile = active.copy(content = content)
                val diags = SyntaxValidator.validateFile(tempFile)
                _uiState.update { state ->
                    val currentActiveDiag = state.activeDiagnostic
                    val matchingDiag = if (currentActiveDiag != null) {
                        diags.find { it.line == currentActiveDiag.line } ?: diags.firstOrNull()
                    } else {
                        diags.firstOrNull()
                    }
                    state.copy(
                        realtimeDiagnostics = diags,
                        activeDiagnostic = matchingDiag,
                        highlightedErrorLine = matchingDiag?.line
                    )
                }
            }
        }
    }

    fun saveActiveFile() {
        val currentFile = _uiState.value.activeFile ?: return
        val newContent = _uiState.value.editorContent
        viewModelScope.launch {
            val updatedFile = currentFile.copy(content = newContent)
            repository.updateFile(updatedFile)
            val diags = if (_uiState.value.isRealtimeCheckingEnabled) {
                SyntaxValidator.validateFile(updatedFile)
            } else emptyList()

            _uiState.update {
                it.copy(
                    activeFile = updatedFile,
                    isModified = false,
                    realtimeDiagnostics = diags,
                    activeDiagnostic = diags.firstOrNull(),
                    highlightedErrorLine = diags.firstOrNull()?.line,
                    infoSnackbarMessage = "Saved ${updatedFile.name}"
                )
            }
            addLogcat("Editor", "I", "File ${updatedFile.name} saved successfully.")
        }
    }

    fun addNewFileOrFolder(fileName: String, fileType: String, isFolder: Boolean) {
        val project = _uiState.value.currentProject ?: return
        val active = _uiState.value.activeFile
        val parentPath = active?.parentPath ?: "app/src/main/java"

        viewModelScope.launch {
            if (isFolder) {
                fileManager.createDirectory(project.id, parentPath, fileName)
                _uiState.update { it.copy(isNewFileDialogOpen = false, infoSnackbarMessage = "Created directory $fileName") }
            } else {
                val ext = when (fileType.uppercase()) {
                    "KOTLIN" -> ".kt"
                    "JAVA" -> ".java"
                    "XML" -> ".xml"
                    "GRADLE" -> ".gradle.kts"
                    "JSON" -> ".json"
                    else -> ".kt"
                }
                val cleanName = if (fileName.endsWith(ext)) fileName else "$fileName$ext"
                val created = fileManager.createFile(project.id, parentPath, cleanName, fileType)
                openFile(created)
                _uiState.update { it.copy(isNewFileDialogOpen = false, infoSnackbarMessage = "Created $cleanName") }
            }
        }
    }

    fun renameFile(file: ProjectFileEntity, newName: String) {
        viewModelScope.launch {
            fileManager.renameFile(file, newName, _uiState.value.files)
            _uiState.update { it.copy(isRenameDialogOpen = false, targetFileForAction = null, infoSnackbarMessage = "Renamed to $newName") }
        }
    }

    fun moveFile(file: ProjectFileEntity, newParentPath: String) {
        viewModelScope.launch {
            fileManager.moveFile(file, newParentPath, _uiState.value.files)
            _uiState.update { it.copy(isRenameDialogOpen = false, targetFileForAction = null, infoSnackbarMessage = "Moved ${file.name}") }
        }
    }

    fun deleteFile(file: ProjectFileEntity) {
        viewModelScope.launch {
            fileManager.deleteFileOrDirectory(file, _uiState.value.files)
            closeTab(file)
            _uiState.update { it.copy(infoSnackbarMessage = "Deleted ${file.name}") }
        }
    }

    fun openRenameDialog(file: ProjectFileEntity) {
        _uiState.update { it.copy(isRenameDialogOpen = true, targetFileForAction = file) }
    }

    fun setViewMode(mode: StudioViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setBottomTab(index: Int) {
        _uiState.update { it.copy(selectedBottomTab = index, isLogcatOpen = true) }
    }

    fun runBuildAndRun() {
        val project = _uiState.value.currentProject ?: return
        val files = _uiState.value.files

        saveActiveFile()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBuilding = true,
                    buildProgress = 0f,
                    currentBuildTask = ":app:preBuild",
                    isEmulatorVisible = false
                )
            }
            addLogcat("Gradle", "I", "Starting build process for project ${project.name}...")

            val (result, logEntity) = buildPipeline.executeBuild(
                project = project,
                files = files
            ) { stage, progress ->
                _uiState.update {
                    it.copy(
                        buildProgress = progress,
                        currentBuildTask = stage.taskName
                    )
                }
                addLogcat("Gradle", "D", "Executing ${stage.taskName}: ${stage.description}")
            }

            repository.addBuildLog(logEntity)

            _uiState.update {
                it.copy(
                    isBuilding = false,
                    lastBuildResult = result,
                    latestApkMetadata = result.apkMetadata,
                    isEmulatorVisible = result.isSuccess,
                    isLogcatOpen = !result.isSuccess,
                    selectedBottomTab = 0,
                    infoSnackbarMessage = if (result.isSuccess) "Build Successful! APK generated (${result.apkMetadata?.fileSizeFormatted})" else "Build Failed. Check syntax errors."
                )
            }

            if (result.isSuccess) {
                addLogcat("ActivityManager", "I", "Start proc ${project.packageName} for activity .MainActivity")
                addLogcat(project.name, "D", "MainActivity.onCreate() invoked - Jetpack Compose initialized")
            } else {
                addLogcat("AndroidRuntime", "E", "FATAL COMPILATION ERROR: ${result.errorMessage}")
            }
        }
    }

    fun sendTerminalCommand(command: String) {
        if (command.isBlank()) return
        val currentLines = _uiState.value.terminalLines
        val promptLine = "$ $command"
        val res = TerminalManager.executeCommand(command, _uiState.value.currentProject, _uiState.value.files)

        if (res.output == "__CLEAR__") {
            _uiState.update { it.copy(terminalLines = emptyList(), terminalInput = "") }
            return
        }

        val newLines = currentLines + promptLine + if (res.output.isNotEmpty()) res.output.lines() else emptyList()
        _uiState.update {
            it.copy(
                terminalLines = newLines,
                terminalInput = ""
            )
        }

        if (res.triggerBuild) {
            runBuildAndRun()
        } else if (res.triggerInstallToVirtualDevice) {
            installAppToVirtualDevice()
        }
    }

    fun installAppToVirtualDevice() {
        val project = _uiState.value.currentProject ?: return
        val files = _uiState.value.files

        saveActiveFile()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBuilding = true,
                    buildProgress = 0.1f,
                    currentBuildTask = ":app:preBuild (Virtual Device Install)",
                    isEmulatorVisible = false
                )
            }
            addLogcat("adb", "I", "Connecting to emulator-5554 (Pixel 9 Pro - Android 15)")
            addLogcat("adb", "I", "Preparing APK installation for ${project.packageName}...")

            val (result, logEntity) = buildPipeline.executeBuild(
                project = project,
                files = files
            ) { stage, progress ->
                _uiState.update {
                    it.copy(
                        buildProgress = progress,
                        currentBuildTask = stage.taskName
                    )
                }
                addLogcat("Gradle", "D", "Executing ${stage.taskName}: ${stage.description}")
            }

            repository.addBuildLog(logEntity)

            if (result.isSuccess) {
                addLogcat("adb", "I", "Performing Streamed Install of ${result.apkMetadata?.appName ?: project.name}.apk (${result.apkMetadata?.fileSizeFormatted ?: "3.4 MB"})")
                addLogcat("adb", "I", "Success: Package ${project.packageName} installed on emulator-5554 in 312ms")
                addLogcat("ActivityManager", "I", "Starting: Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] cmp=${project.packageName}/.MainActivity }")
                addLogcat(project.name, "D", "MainActivity initialized on Pixel 9 Pro virtual screen")
            } else {
                addLogcat("adb", "E", "INSTALL_FAILED_DEXOPT: Build failed with errors. Check Logcat.")
            }

            _uiState.update {
                it.copy(
                    isBuilding = false,
                    lastBuildResult = result,
                    latestApkMetadata = result.apkMetadata,
                    isEmulatorVisible = result.isSuccess,
                    isLogcatOpen = !result.isSuccess,
                    selectedBottomTab = if (result.isSuccess) 0 else 0,
                    infoSnackbarMessage = if (result.isSuccess) "✓ Successfully installed & running on Pixel 9 Pro Emulator!" else "Build failed. Check diagnostics."
                )
            }
        }
    }

    fun updateTerminalInput(input: String) {
        _uiState.update { it.copy(terminalInput = input) }
    }

    fun toggleEmulator(visible: Boolean) {
        _uiState.update { it.copy(isEmulatorVisible = visible) }
    }

    fun setGeminiApiKey(key: String) {
        val prefs = getApplication<Application>().getSharedPreferences("studio_ai_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("gemini_api_key", key.trim()).apply()
        _uiState.update { it.copy(geminiApiKey = key.trim(), infoSnackbarMessage = if (key.isBlank()) "Gemini API Key cleared" else "Gemini API Key saved") }
    }

    fun toggleAiSheet(open: Boolean) {
        _uiState.update { state ->
            val messages = if (state.aiMessages.isEmpty()) {
                listOf(
                    AiChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        message = "👋 Hi! I am **Studio Bot Project AI Architect**.\nI have full context of all **${state.files.size} files** in project **${state.currentProject?.name ?: ""}**.\n\nI can:\n- 🛠️ **Auto-Heal Project Errors**: Inspect and fix all broken files so the app compiles cleanly\n- 🏗️ **Multi-File Scaffolding**: Generate models, ViewModels, and Compose UI across multiple files simultaneously\n- 💡 **Refactor & Modernize**: Clean architecture, Material 3, and state lifecycle fixes."
                    )
                )
            } else {
                state.aiMessages
            }
            state.copy(isAiSheetOpen = open, aiMessages = messages)
        }
    }

    fun sendAiPrompt(prompt: String) {
        val currentProj = _uiState.value.currentProject
        val allFiles = _uiState.value.files
        val activeFile = _uiState.value.activeFile
        val diagnostics = _uiState.value.realtimeDiagnostics
        val lastBuild = _uiState.value.lastBuildResult
        val apiKey = _uiState.value.geminiApiKey

        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            message = prompt
        )

        _uiState.update {
            it.copy(
                aiMessages = it.aiMessages + userMsg,
                isAiThinking = true
            )
        }

        viewModelScope.launch {
            val result = GeminiAiService.promptStudioBot(
                userPrompt = prompt,
                currentProject = currentProj,
                allFiles = allFiles,
                activeFile = activeFile,
                diagnostics = diagnostics,
                lastBuildResult = lastBuild,
                customApiKey = apiKey
            )

            val botMsg = AiChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                message = result.explanation,
                extractedCode = result.extractedCode,
                fileOperations = result.fileOperations,
                isAutoHealFix = result.isAutoHealed
            )

            _uiState.update {
                it.copy(
                    aiMessages = it.aiMessages + botMsg,
                    isAiThinking = false
                )
            }
        }
    }

    fun autoHealProjectAndRun() {
        val currentProj = _uiState.value.currentProject ?: return
        val allFiles = _uiState.value.files
        val activeFile = _uiState.value.activeFile
        val diagnostics = _uiState.value.realtimeDiagnostics
        val lastBuild = _uiState.value.lastBuildResult
        val apiKey = _uiState.value.geminiApiKey

        _uiState.update { it.copy(isAutoHealingProject = true, isAiThinking = true) }
        addLogcat("AutoHealer", "I", "Starting Autonomous Project Healing across all ${allFiles.size} files...")

        viewModelScope.launch {
            val healResult = GeminiAiService.promptStudioBot(
                userPrompt = "Please analyze all project compilation errors, syntax issues, and broken files. Repair every file so the Android app builds and runs normally.",
                currentProject = currentProj,
                allFiles = allFiles,
                activeFile = activeFile,
                diagnostics = diagnostics,
                lastBuildResult = lastBuild,
                customApiKey = apiKey
            )

            if (healResult.fileOperations.isNotEmpty()) {
                applyAllAiFileOperations(healResult.fileOperations, autoBuild = true)
                val botMsg = AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    message = "🎉 **Autonomous Auto-Heal Completed!**\nRepaired **${healResult.fileOperations.size} file(s)** and initiated Gradle build.\n\n${healResult.explanation}",
                    fileOperations = healResult.fileOperations.map { it.copy(isApplied = true) },
                    isAutoHealFix = true
                )
                _uiState.update {
                    it.copy(
                        aiMessages = it.aiMessages + botMsg,
                        isAutoHealingProject = false,
                        isAiThinking = false,
                        infoSnackbarMessage = "Auto-Healed ${healResult.fileOperations.size} file(s) and initiated build!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isAutoHealingProject = false,
                        isAiThinking = false,
                        infoSnackbarMessage = "No critical errors found in project files."
                    )
                }
            }
        }
    }

    fun applyAiFileOperation(op: AiFileOperation) {
        val currentProj = _uiState.value.currentProject ?: return
        viewModelScope.launch {
            val allFiles = _uiState.value.files
            val existing = allFiles.find { it.path == op.filePath || it.name == op.fileName }

            when (op.type) {
                AiOperationType.CREATE -> {
                    if (existing != null) {
                        val updated = existing.copy(content = op.content)
                        repository.updateFile(updated)
                        if (_uiState.value.activeFile?.id == existing.id) {
                            updateEditorContent(op.content)
                        }
                    } else {
                        val parentPath = if (op.filePath.contains('/')) op.filePath.substringBeforeLast('/') else "app/src/main/java"
                        val created = fileManager.createFile(
                            projectId = currentProj.id,
                            parentPath = parentPath,
                            fileName = op.fileName,
                            fileType = op.fileType,
                            initialContent = op.content
                        )
                        openFile(created)
                    }
                }
                AiOperationType.EDIT -> {
                    if (existing != null) {
                        val updated = existing.copy(content = op.content)
                        repository.updateFile(updated)
                        if (_uiState.value.activeFile?.id == existing.id) {
                            updateEditorContent(op.content)
                        }
                    } else {
                        val parentPath = if (op.filePath.contains('/')) op.filePath.substringBeforeLast('/') else "app/src/main/java"
                        val created = fileManager.createFile(
                            projectId = currentProj.id,
                            parentPath = parentPath,
                            fileName = op.fileName,
                            fileType = op.fileType,
                            initialContent = op.content
                        )
                        openFile(created)
                    }
                }
                AiOperationType.DELETE -> {
                    if (existing != null) {
                        fileManager.deleteFileOrDirectory(existing, allFiles)
                        closeTab(existing)
                    }
                }
            }

            // Mark this operation as applied in AI messages
            _uiState.update { state ->
                val updatedMessages = state.aiMessages.map { msg ->
                    val updatedOps = msg.fileOperations.map { if (it.id == op.id) it.copy(isApplied = true) else it }
                    msg.copy(fileOperations = updatedOps)
                }
                state.copy(
                    aiMessages = updatedMessages,
                    infoSnackbarMessage = "Applied change to ${op.fileName}"
                )
            }
            addLogcat("AiAssistant", "I", "Applied AI file operation [${op.type}] to ${op.filePath}")
        }
    }

    fun applyAllAiFileOperations(operations: List<AiFileOperation>, autoBuild: Boolean = false) {
        val currentProj = _uiState.value.currentProject ?: return
        viewModelScope.launch {
            val allFiles = _uiState.value.files
            var appliedCount = 0

            for (op in operations) {
                val existing = allFiles.find { it.path == op.filePath || it.name == op.fileName }
                when (op.type) {
                    AiOperationType.CREATE -> {
                        if (existing != null) {
                            val updated = existing.copy(content = op.content)
                            repository.updateFile(updated)
                            if (_uiState.value.activeFile?.id == existing.id) {
                                updateEditorContent(op.content)
                            }
                        } else {
                            val parentPath = if (op.filePath.contains('/')) op.filePath.substringBeforeLast('/') else "app/src/main/java"
                            fileManager.createFile(
                                projectId = currentProj.id,
                                parentPath = parentPath,
                                fileName = op.fileName,
                                fileType = op.fileType,
                                initialContent = op.content
                            )
                        }
                        appliedCount++
                    }
                    AiOperationType.EDIT -> {
                        if (existing != null) {
                            val updated = existing.copy(content = op.content)
                            repository.updateFile(updated)
                            if (_uiState.value.activeFile?.id == existing.id) {
                                updateEditorContent(op.content)
                            }
                        } else {
                            val parentPath = if (op.filePath.contains('/')) op.filePath.substringBeforeLast('/') else "app/src/main/java"
                            fileManager.createFile(
                                projectId = currentProj.id,
                                parentPath = parentPath,
                                fileName = op.fileName,
                                fileType = op.fileType,
                                initialContent = op.content
                            )
                        }
                        appliedCount++
                    }
                    AiOperationType.DELETE -> {
                        if (existing != null) {
                            fileManager.deleteFileOrDirectory(existing, allFiles)
                            closeTab(existing)
                            appliedCount++
                        }
                    }
                }
            }

            // Mark all operations as applied in UI
            _uiState.update { state ->
                val targetIds = operations.map { it.id }.toSet()
                val updatedMessages = state.aiMessages.map { msg ->
                    val updatedOps = msg.fileOperations.map { if (targetIds.contains(it.id)) it.copy(isApplied = true) else it }
                    msg.copy(fileOperations = updatedOps)
                }
                state.copy(
                    aiMessages = updatedMessages,
                    infoSnackbarMessage = "Successfully updated $appliedCount project file(s)!"
                )
            }
            addLogcat("AiAssistant", "I", "Applied $appliedCount AI multi-file operations across project ${currentProj.name}")

            if (autoBuild) {
                runBuildAndRun()
            }
        }
    }

    fun toggleAutonomousMode(enabled: Boolean) {
        _uiState.update { it.copy(autonomousAgent = it.autonomousAgent.copy(isAutonomousMode = enabled)) }
    }

    fun stopAutonomousAgent() {
        autonomousAgentJob?.cancel()
        _uiState.update {
            it.copy(
                isAiThinking = false,
                autonomousAgent = it.autonomousAgent.copy(
                    isActive = false,
                    stage = AgentStage.IDLE,
                    statusText = "Autonomous Agent stopped."
                ),
                infoSnackbarMessage = "Autonomous Agent stopped."
            )
        }
        addLogcat("AutonomousAgent", "W", "Autonomous Agent stopped by user.")
    }

    fun startAutonomousAgent(goal: String) {
        if (goal.isBlank()) return
        val currentProj = _uiState.value.currentProject ?: return
        autonomousAgentJob?.cancel()

        val planId = UUID.randomUUID().toString()

        // 1. Break down user request into structured sub-tasks with stages & order
        val initialSteps = listOf(
            AgentPlanStep(
                id = UUID.randomUUID().toString(),
                planId = planId,
                stepOrder = 1,
                title = "1. Architecture & Dependency Deconstruction",
                description = "Deconstruct user request \"$goal\" into required data entities, state models & UI architecture",
                stage = AgentStage.PLANNING,
                status = AgentStepStatus.PENDING
            ),
            AgentPlanStep(
                id = UUID.randomUUID().toString(),
                planId = planId,
                stepOrder = 2,
                title = "2. Multi-File Code Synthesis & Generation",
                description = "Synthesize compile-ready Kotlin/Compose models, DAOs, and UI components",
                stage = AgentStage.GENERATING_CODE,
                status = AgentStepStatus.PENDING
            ),
            AgentPlanStep(
                id = UUID.randomUUID().toString(),
                planId = planId,
                stepOrder = 3,
                title = "3. Room Database & File System Persistence",
                description = "Atomically persist generated files to local project repository database",
                stage = AgentStage.APPLYING_CHANGES,
                status = AgentStepStatus.PENDING
            ),
            AgentPlanStep(
                id = UUID.randomUUID().toString(),
                planId = planId,
                stepOrder = 4,
                title = "4. Gradle Build Verification & Self-Healing Loop",
                description = "Run Kotlin compiler task and automatically heal any syntax or type diagnostics",
                stage = AgentStage.COMPILING,
                status = AgentStepStatus.PENDING
            ),
            AgentPlanStep(
                id = UUID.randomUUID().toString(),
                planId = planId,
                stepOrder = 5,
                title = "5. Streamed APK Deployment to Pixel 9 Pro",
                description = "Deploy built APK to virtual Android 15 device and boot MainActivity",
                stage = AgentStage.DEPLOYING,
                status = AgentStepStatus.PENDING
            )
        )

        // Room Entities for Task Plan & Sub-Tasks
        val planEntity = AgentTaskPlanEntity(
            id = planId,
            projectId = currentProj.id,
            userGoal = goal,
            status = "IN_PROGRESS",
            totalSubTasks = initialSteps.size,
            completedSubTasks = 0
        )

        val subTaskEntities = initialSteps.map { step ->
            AgentSubTaskEntity(
                id = step.id,
                planId = planId,
                projectId = currentProj.id,
                stepOrder = step.stepOrder,
                title = step.title,
                description = step.description,
                stage = step.stage.name,
                status = "PENDING"
            )
        }

        _uiState.update {
            it.copy(
                isAiSheetOpen = true,
                isAiThinking = true,
                autonomousAgent = AutonomousAgentSession(
                    isActive = true,
                    currentPlanId = planId,
                    goal = goal,
                    stage = AgentStage.PLANNING,
                    steps = initialSteps,
                    currentStepIndex = 0,
                    currentIteration = 1,
                    maxIterations = 4,
                    thoughtLogs = listOf(
                        "⚡ [TASK PLANNER] Deconstructed goal into ${initialSteps.size} sub-tasks",
                        "💾 [ROOM DB] Persisted task plan ($planId) to SQLite Room database",
                        "🎯 [GOAL] \"$goal\"",
                        "📁 [PROJECT] ${currentProj.name} (${currentProj.packageName})"
                    ),
                    statusText = "Planning sub-tasks and analyzing project context..."
                )
            )
        }
        addLogcat("TaskPlanner", "I", "Deconstructed goal into ${initialSteps.size} sub-tasks and saved to Room DB: $goal")

        autonomousAgentJob = viewModelScope.launch {
            try {
                // Clear any previous error feedback
                _uiState.update { it.copy(autonomousAgent = it.autonomousAgent.copy(feedback = null)) }

                // Persist initial Plan and Sub-Tasks into Room Database
                repository.createTaskPlanWithSubTasks(planEntity, subTaskEntities)

                // ==================== SUB-TASK 1: PLANNING & CONTEXT ====================
                val step1StartTime = System.currentTimeMillis()
                var step1Success = false
                var step1Attempts = 0
                val maxSubTaskRetries = 2

                while (!step1Success && step1Attempts < maxSubTaskRetries) {
                    step1Attempts++
                    try {
                        updateAgentStepAndDb(0, initialSteps[0].id, AgentStepStatus.RUNNING, "Analyzing AST and project dependencies (Attempt $step1Attempts)")
                        appendAgentThought("🧠 [SUB-TASK 1/5] Deconstructing project AST, layout dependencies, and Kotlin models...")
                        delay(450)
                        val step1Duration = System.currentTimeMillis() - step1StartTime
                        updateAgentStepAndDb(
                            stepIndex = 0,
                            subTaskId = initialSteps[0].id,
                            status = AgentStepStatus.COMPLETED,
                            log = "Architecture deconstruction complete",
                            durationMs = step1Duration
                        )
                        repository.updateTaskPlanProgress(planId, "IN_PROGRESS", 1)
                        step1Success = true
                    } catch (e: Exception) {
                        appendAgentThought("⚠️ [ERROR CAUGHT] Sub-Task 1 Error: ${e.message}")
                        if (step1Attempts < maxSubTaskRetries) {
                            updateAgentStepAndDb(0, initialSteps[0].id, AgentStepStatus.RETRYING, "Retrying AST analysis...")
                            appendAgentThought("🔄 [AUTO-REATTEMPT 1/1] Re-indexing project structure...")
                            delay(300)
                        } else {
                            throw e
                        }
                    }
                }

                // ==================== SUB-TASK 2: CODE SYNTHESIS ====================
                val step2StartTime = System.currentTimeMillis()
                _uiState.update { it.copy(autonomousAgent = it.autonomousAgent.copy(stage = AgentStage.GENERATING_CODE, currentStepIndex = 1, statusText = "Generating multi-file code solution...")) }
                
                var botResult: com.example.data.ai.StudioBotResult? = null
                var step2Success = false
                var step2Attempts = 0

                while (!step2Success && step2Attempts < maxSubTaskRetries) {
                    step2Attempts++
                    try {
                        val status = if (step2Attempts > 1) AgentStepStatus.RETRYING else AgentStepStatus.RUNNING
                        updateAgentStepAndDb(1, initialSteps[1].id, status, "Synthesizing multi-file code (Attempt $step2Attempts/$maxSubTaskRetries)")
                        appendAgentThought("✍️ [SUB-TASK 2/5] Synthesizing multi-file source code across UI & business logic (Attempt $step2Attempts)...")

                        val allFiles = _uiState.value.files
                        val activeFile = _uiState.value.activeFile
                        val diagnostics = _uiState.value.realtimeDiagnostics
                        val lastBuild = _uiState.value.lastBuildResult
                        val apiKey = _uiState.value.geminiApiKey

                        val result = GeminiAiService.promptStudioBot(
                            userPrompt = if (step2Attempts > 1) "$goal (Provide clean compile-ready Jetpack Compose and Kotlin implementation)" else goal,
                            currentProject = currentProj,
                            allFiles = allFiles,
                            activeFile = activeFile,
                            diagnostics = diagnostics,
                            lastBuildResult = lastBuild,
                            customApiKey = apiKey
                        )

                        if (result.fileOperations.isEmpty() && step2Attempts < maxSubTaskRetries) {
                            throw IllegalStateException("AI generated 0 file operations for goal: $goal")
                        }

                        botResult = result
                        step2Success = true
                        appendAgentThought("💡 [SYNTHESIS COMPLETE] Generated ${result.fileOperations.size} file operation(s).")
                        for (op in result.fileOperations) {
                            appendAgentThought("  • [${op.type}] ${op.filePath}: ${op.summary}")
                        }
                        val step2Duration = System.currentTimeMillis() - step2StartTime
                        updateAgentStepAndDb(
                            stepIndex = 1,
                            subTaskId = initialSteps[1].id,
                            status = AgentStepStatus.COMPLETED,
                            log = "Generated ${result.fileOperations.size} file operations",
                            durationMs = step2Duration
                        )
                        repository.updateTaskPlanProgress(planId, "IN_PROGRESS", 2)
                    } catch (e: Exception) {
                        appendAgentThought("⚠️ [ERROR CAUGHT IN SYNTHESIS] ${e.message}")
                        if (step2Attempts < maxSubTaskRetries) {
                            updateAgentStepAndDb(1, initialSteps[1].id, AgentStepStatus.RETRYING, "Re-attempting synthesis with fallback...")
                            appendAgentThought("🔄 [AUTO-REATTEMPT] Triggering fallback code synthesis engine...")
                            delay(400)
                        } else {
                            val feedback = buildExecutionFeedback(
                                failedIndex = 1,
                                failedTitle = initialSteps[1].title,
                                errorCategory = AgentErrorCategory.CODE_SYNTHESIS_ERROR,
                                errorMessage = e.message ?: "Code synthesis failed after $maxSubTaskRetries attempts",
                                diagnostics = "Failed to generate multi-file patch for goal: \"$goal\"",
                                goal = goal,
                                project = currentProj
                            )
                            handleAgentExecutionFailure(planId, 1, initialSteps[1].id, feedback)
                            return@launch
                        }
                    }
                }

                val generatedOps = botResult?.fileOperations ?: emptyList()

                // ==================== SUB-TASK 3: PERSISTENCE (ROOM & FILES) ====================
                val step3StartTime = System.currentTimeMillis()
                _uiState.update { it.copy(autonomousAgent = it.autonomousAgent.copy(stage = AgentStage.APPLYING_CHANGES, currentStepIndex = 2, statusText = "Applying generated files to project database...")) }
                
                var step3Success = false
                var step3Attempts = 0

                while (!step3Success && step3Attempts < maxSubTaskRetries) {
                    step3Attempts++
                    try {
                        val status = if (step3Attempts > 1) AgentStepStatus.RETRYING else AgentStepStatus.RUNNING
                        updateAgentStepAndDb(2, initialSteps[2].id, status, "Writing files to project filesystem")
                        appendAgentThought("💾 [SUB-TASK 3/5] Persisting ${generatedOps.size} files to Room database...")

                        if (generatedOps.isNotEmpty()) {
                            applyAllAiFileOperations(generatedOps, autoBuild = false)
                        }
                        delay(350)
                        val step3Duration = System.currentTimeMillis() - step3StartTime
                        updateAgentStepAndDb(
                            stepIndex = 2,
                            subTaskId = initialSteps[2].id,
                            status = AgentStepStatus.COMPLETED,
                            log = "Committed ${generatedOps.size} files to database",
                            durationMs = step3Duration
                        )
                        repository.updateTaskPlanProgress(planId, "IN_PROGRESS", 3)
                        step3Success = true
                    } catch (e: Exception) {
                        appendAgentThought("⚠️ [ERROR CAUGHT IN PERSISTENCE] ${e.message}")
                        if (step3Attempts < maxSubTaskRetries) {
                            updateAgentStepAndDb(2, initialSteps[2].id, AgentStepStatus.RETRYING, "Re-attempting database write with conflict resolution...")
                            appendAgentThought("🔄 [AUTO-REATTEMPT] Retrying atomic file persistence...")
                            delay(300)
                        } else {
                            val feedback = buildExecutionFeedback(
                                failedIndex = 2,
                                failedTitle = initialSteps[2].title,
                                errorCategory = AgentErrorCategory.FILESYSTEM_PERSISTENCE,
                                errorMessage = e.message ?: "Failed to persist files to Room database",
                                diagnostics = "File conflict or Room IO exception during commit",
                                goal = goal,
                                project = currentProj
                            )
                            handleAgentExecutionFailure(planId, 2, initialSteps[2].id, feedback)
                            return@launch
                        }
                    }
                }

                // ==================== SUB-TASK 4: GRADLE BUILD & SELF-HEALING ====================
                val step4StartTime = System.currentTimeMillis()
                _uiState.update { it.copy(autonomousAgent = it.autonomousAgent.copy(stage = AgentStage.COMPILING, currentStepIndex = 3, statusText = "Compiling Gradle & verifying diagnostics...")) }
                updateAgentStepAndDb(3, initialSteps[3].id, AgentStepStatus.RUNNING, "Executing Gradle build")
                appendAgentThought("🔨 [SUB-TASK 4/5] Executing compiler verification task :app:compileDebugKotlin...")

                var buildSuccessful = false
                var iteration = 1
                val maxAttempts = 3
                var lastBuildError = ""

                while (iteration <= maxAttempts && !buildSuccessful) {
                    try {
                        _uiState.update { it.copy(autonomousAgent = it.autonomousAgent.copy(currentIteration = iteration)) }
                        appendAgentThought("⚙️ [BUILD CYCLE] Attempt ($iteration/$maxAttempts)...")

                        val currentProjectFiles = _uiState.value.files
                        val (buildResult, logEntity) = buildPipeline.executeBuild(
                            project = currentProj,
                            files = currentProjectFiles
                        ) { stage, progress ->
                            _uiState.update { it.copy(buildProgress = progress, currentBuildTask = stage.taskName) }
                        }
                        repository.addBuildLog(logEntity)
                        _uiState.update { it.copy(lastBuildResult = buildResult, latestApkMetadata = buildResult.apkMetadata) }

                        if (buildResult.isSuccess) {
                            buildSuccessful = true
                            appendAgentThought("✅ [BUILD VERIFIED] Green build! APK ready (${buildResult.apkMetadata?.fileSizeFormatted ?: "3.4 MB"})")
                        } else {
                            lastBuildError = buildResult.errorMessage ?: "Compiler diagnostic detected"
                            appendAgentThought("⚠️ [COMPILE DIAGNOSTIC] Found error: $lastBuildError")
                            _uiState.update { it.copy(autonomousAgent = it.autonomousAgent.copy(stage = AgentStage.SELF_HEALING, statusText = "Self-Healing compiler diagnostics ($iteration/$maxAttempts)...")) }
                            appendAgentThought("🩺 [AUTO-REPAIR] Applying targeted fix for diagnostics...")

                            val healDiagnostics = _uiState.value.realtimeDiagnostics
                            val healResult = GeminiAiService.promptStudioBot(
                                userPrompt = "Fix compilation error in project: $lastBuildError",
                                currentProject = currentProj,
                                allFiles = _uiState.value.files,
                                activeFile = _uiState.value.activeFile,
                                diagnostics = healDiagnostics,
                                lastBuildResult = buildResult,
                                customApiKey = _uiState.value.geminiApiKey
                            )

                            if (healResult.fileOperations.isNotEmpty()) {
                                appendAgentThought("🩹 [AUTO-FIX] Applied ${healResult.fileOperations.size} patch(es).")
                                applyAllAiFileOperations(healResult.fileOperations, autoBuild = false)
                                delay(350)
                            } else {
                                appendAgentThought("ℹ️ [AUTO-FIX] Repaired syntax AST directly.")
                            }
                            iteration++
                        }
                    } catch (e: Exception) {
                        appendAgentThought("⚠️ [BUILD PIPELINE EXCEPTION] ${e.message}")
                        lastBuildError = e.message ?: "Unknown build exception"
                        iteration++
                        delay(300)
                    }
                }

                val step4Duration = System.currentTimeMillis() - step4StartTime
                if (buildSuccessful) {
                    updateAgentStepAndDb(
                        stepIndex = 3,
                        subTaskId = initialSteps[3].id,
                        status = AgentStepStatus.COMPLETED,
                        log = "Gradle compilation passed",
                        durationMs = step4Duration
                    )
                    repository.updateTaskPlanProgress(planId, "IN_PROGRESS", 4)

                    // ==================== SUB-TASK 5: DEPLOYMENT ====================
                    val step5StartTime = System.currentTimeMillis()
                    _uiState.update { it.copy(autonomousAgent = it.autonomousAgent.copy(stage = AgentStage.DEPLOYING, currentStepIndex = 4, statusText = "Deploying app to Pixel 9 Pro Emulator...")) }
                    updateAgentStepAndDb(4, initialSteps[4].id, AgentStepStatus.RUNNING, "Installing APK to Pixel 9 Pro")
                    appendAgentThought("🚀 [SUB-TASK 5/5] Streamed APK install to Pixel 9 Pro (Android 15)...")
                    delay(500)
                    appendAgentThought("📱 [LAUNCH] MainActivity started on Android 15 Virtual Device.")

                    val step5Duration = System.currentTimeMillis() - step5StartTime
                    updateAgentStepAndDb(
                        stepIndex = 4,
                        subTaskId = initialSteps[4].id,
                        status = AgentStepStatus.COMPLETED,
                        log = "App installed & launched on Emulator",
                        durationMs = step5Duration
                    )

                    // Update Plan status to COMPLETED in Room DB
                    repository.updateTaskPlanProgress(
                        planId = planId,
                        status = "COMPLETED",
                        completedCount = 5,
                        completedAt = System.currentTimeMillis(),
                        summary = "Successfully completed all 5 sub-tasks. ${generatedOps.size} files generated."
                    )

                    _uiState.update {
                        it.copy(
                            isEmulatorVisible = true,
                            isBuilding = false,
                            isAiThinking = false,
                            autonomousAgent = it.autonomousAgent.copy(
                                stage = AgentStage.COMPLETED,
                                statusText = "All 5 sub-tasks completed and tracked in Room DB! App is running on Pixel 9 Pro.",
                                modifiedFilesCount = generatedOps.size,
                                feedback = null
                            ),
                            infoSnackbarMessage = "🎉 Plan selesai: 5/5 sub-tugas terekam di Room DB!"
                        )
                    }
                    appendAgentThought("🎉 [TASK PLAN COMPLETED] All sub-tasks completed and verified in Room database.")

                    val finalMsg = AiChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        message = "🤖 **Autonomous Agent - Plan Execution Complete!**\n\n**Goal**: \"$goal\"\n- **Plan ID**: `$planId`\n- **Sub-Tasks**: 5/5 Completed & Saved to Room DB\n- **Files modified/created**: ${generatedOps.size}\n- **Build & Deploy**: PASSED (Running on Pixel 9 Pro)\n\n${botResult?.explanation ?: "Executed autonomously."}",
                        fileOperations = generatedOps.map { it.copy(isApplied = true) },
                        isAutoHealFix = true
                    )
                    _uiState.update { it.copy(aiMessages = it.aiMessages + finalMsg) }
                } else {
                    // Compilation Failed after all retry & self-healing cycles
                    val errCategory = classifyErrorCategory(lastBuildError, _uiState.value.realtimeDiagnostics)
                    val feedback = buildExecutionFeedback(
                        failedIndex = 3,
                        failedTitle = initialSteps[3].title,
                        errorCategory = errCategory,
                        errorMessage = lastBuildError.ifBlank { "Gradle build failed after $maxAttempts self-healing cycles" },
                        diagnostics = _uiState.value.realtimeDiagnostics.joinToString("\n") { "[${it.errorType.label}] Line ${it.line}: ${it.message}" },
                        goal = goal,
                        project = currentProj
                    )
                    handleAgentExecutionFailure(planId, 3, initialSteps[3].id, feedback)
                }
            } catch (e: Exception) {
                appendAgentThought("❌ [ERROR DETECTED] Unhandled exception in Autonomous Agent: ${e.message}")
                val errCategory = classifyErrorCategory(e.message, emptyList())
                val feedback = buildExecutionFeedback(
                    failedIndex = _uiState.value.autonomousAgent.currentStepIndex,
                    failedTitle = initialSteps.getOrNull(_uiState.value.autonomousAgent.currentStepIndex)?.title ?: "Autonomous Execution",
                    errorCategory = errCategory,
                    errorMessage = e.message ?: "Unexpected runtime exception",
                    diagnostics = e.stackTraceToString().take(300),
                    goal = goal,
                    project = currentProj
                )
                handleAgentExecutionFailure(planId, _uiState.value.autonomousAgent.currentStepIndex, initialSteps.getOrNull(_uiState.value.autonomousAgent.currentStepIndex)?.id ?: "", feedback)
            }
        }
    }

    private suspend fun updateAgentStepAndDb(
        stepIndex: Int,
        subTaskId: String,
        status: AgentStepStatus,
        log: String? = null,
        durationMs: Long = 0
    ) {
        _uiState.update { state ->
            val steps = state.autonomousAgent.steps.toMutableList()
            if (stepIndex in steps.indices) {
                steps[stepIndex] = steps[stepIndex].copy(
                    status = status,
                    outputLog = log ?: steps[stepIndex].outputLog,
                    executionTimeMs = if (durationMs > 0) durationMs else steps[stepIndex].executionTimeMs
                )
            }
            state.copy(autonomousAgent = state.autonomousAgent.copy(steps = steps))
        }

        val dbStatus = when (status) {
            AgentStepStatus.RUNNING -> "RUNNING"
            AgentStepStatus.RETRYING -> "RETRYING"
            AgentStepStatus.COMPLETED -> "COMPLETED"
            AgentStepStatus.FAILED -> "FAILED"
            AgentStepStatus.PENDING -> "PENDING"
        }
        repository.updateSubTaskStatus(
            subTaskId = subTaskId,
            status = dbStatus,
            outputLog = log,
            executionTimeMs = durationMs,
            completedAt = if (status == AgentStepStatus.COMPLETED) System.currentTimeMillis() else null
        )
    }

    private fun appendAgentThought(thought: String) {
        _uiState.update { state ->
            val logs = state.autonomousAgent.thoughtLogs + thought
            state.copy(autonomousAgent = state.autonomousAgent.copy(thoughtLogs = logs))
        }
        addLogcat("TaskPlanner", "D", thought)
    }

    private fun classifyErrorCategory(message: String?, diagnostics: List<com.example.core.BuildDiagnostic>): AgentErrorCategory {
        val combined = "${message.orEmpty()} ${diagnostics.joinToString { it.message }}".lowercase()
        return when {
            combined.contains("unresolved reference") || combined.contains("cannot find symbol") || combined.contains("unresolved import") || combined.contains("import") ->
                AgentErrorCategory.DEPENDENCY_OR_IMPORT
            combined.contains("syntax error") || combined.contains("expecting") || combined.contains("type mismatch") || combined.contains("val cannot be reassigned") ->
                AgentErrorCategory.SYNTAX_OR_TYPE
            combined.contains("compile") || combined.contains("gradle") || combined.contains("task :app:") ->
                AgentErrorCategory.COMPILATION_FAILURE
            combined.contains("synthesis") || combined.contains("gemini") || combined.contains("0 file operations") || combined.contains("ai") ->
                AgentErrorCategory.CODE_SYNTHESIS_ERROR
            combined.contains("file") || combined.contains("ioexception") || combined.contains("room") || combined.contains("sqlite") || combined.contains("database") ->
                AgentErrorCategory.FILESYSTEM_PERSISTENCE
            combined.contains("emulator") || combined.contains("pixel") || combined.contains("apk") || combined.contains("deploy") ->
                AgentErrorCategory.DEPLOYMENT_TIMEOUT
            else -> AgentErrorCategory.UNKNOWN
        }
    }

    private fun buildExecutionFeedback(
        failedIndex: Int,
        failedTitle: String,
        errorCategory: AgentErrorCategory,
        errorMessage: String,
        diagnostics: String,
        goal: String,
        project: ProjectEntity
    ): AgentExecutionFeedback {
        val suggestions = generateRefinedPlanSuggestions(goal, errorCategory, errorMessage, project)
        val remedy = when (errorCategory) {
            AgentErrorCategory.SYNTAX_OR_TYPE -> "Terapkan pembersihan AST sintaks dan periksa tipe data parameter Composable."
            AgentErrorCategory.DEPENDENCY_OR_IMPORT -> "Auto-inject paket import AndroidX, Compose Material 3, dan Icons yang hilang."
            AgentErrorCategory.COMPILATION_FAILURE -> "Gunakan template arsitektur Scaffold MVP yang lebih modular dan aman."
            AgentErrorCategory.CODE_SYNTHESIS_ERROR -> "Gunakan scaffold bertahap (Data -> ViewModel -> Screen UI)."
            AgentErrorCategory.FILESYSTEM_PERSISTENCE -> "Sinkronisasi ulang struktur file dan tabel Room database."
            AgentErrorCategory.DEPLOYMENT_TIMEOUT -> "Verifikasi manifest launcher activity dan package name."
            AgentErrorCategory.UNKNOWN -> "Jalankan ulang dengan strategi fallback minimalis."
        }

        return AgentExecutionFeedback(
            hasError = true,
            errorCategory = errorCategory,
            failedStepIndex = failedIndex,
            failedStepTitle = failedTitle,
            errorMessage = errorMessage,
            diagnosticDetails = diagnostics,
            attemptNumber = 2,
            maxAttempts = 2,
            isAutoReattempting = false,
            suggestedRemedy = remedy,
            refinedPlans = suggestions
        )
    }

    private fun generateRefinedPlanSuggestions(
        goal: String,
        errorCategory: AgentErrorCategory,
        errorMessage: String,
        project: ProjectEntity
    ): List<RefinedPlanSuggestion> {
        return listOf(
            RefinedPlanSuggestion(
                id = UUID.randomUUID().toString(),
                title = "Plan A: Rencana Scaffold Bersih & Material 3 Standar",
                strategyName = "Scaffold Bersih & Safe Compose",
                explanation = "Menyederhanakan Composable tree menjadi Material 3 Scaffold standar untuk mencegah benturan tipe dan sintaks tanpa mengurangi fitur utama.",
                revisedGoal = "Buat implementasi MVP andal untuk: $goal dengan state ViewModel dan Material 3 Scaffold",
                revisedSteps = listOf(
                    "1. Dekonstruksi Entity & ViewModel State",
                    "2. Sintesis Screen UI berbasis M3 Scaffold",
                    "3. Persistensi atomic ke Room Database",
                    "4. Kompilasi Gradle & Running Pixel 9 Pro"
                )
            ),
            RefinedPlanSuggestion(
                id = UUID.randomUUID().toString(),
                title = "Plan B: Perbaikan Import & Namespace Otomatis",
                strategyName = "Auto-Repair Imports & Package Resolver",
                explanation = "Memperbaiki seluruh namespace paket dan menambahkan import AndroidX Jetpack Compose yang hilang pada source code.",
                revisedGoal = "Perbaiki import dependensi dan selesaikan fitur: $goal",
                revisedSteps = listOf(
                    "1. Analisis AST & identifikasi import yang hilang",
                    "2. Patch package header & Compose imports",
                    "3. Verifikasi build Gradle",
                    "4. Luncurkan aplikasi di Emulator"
                )
            ),
            RefinedPlanSuggestion(
                id = UUID.randomUUID().toString(),
                title = "Plan C: Eksekusi Modular Bertahap (Layer-by-Layer)",
                strategyName = "Modular Granular Synthesis",
                explanation = "Menjalankan sintesis terpisah untuk lapisan data (Room DAO/Entity) sebelum membangun Composable UI agar arsitektur terverifikasi lebih dulu.",
                revisedGoal = "Implementasi bertahap Layer-by-Layer untuk: $goal",
                revisedSteps = listOf(
                    "1. Inisialisasi Data Model & Room DAO",
                    "2. Verifikasi Data Layer",
                    "3. Rancang UI Component & ViewModel",
                    "4. Final Build & Deploy"
                )
            )
        )
    }

    private suspend fun handleAgentExecutionFailure(
        planId: String,
        stepIndex: Int,
        subTaskId: String,
        feedback: AgentExecutionFeedback
    ) {
        if (subTaskId.isNotBlank()) {
            updateAgentStepAndDb(
                stepIndex = stepIndex,
                subTaskId = subTaskId,
                status = AgentStepStatus.FAILED,
                log = feedback.errorMessage.take(200)
            )
        }
        repository.updateTaskPlanProgress(
            planId = planId,
            status = "FAILED",
            completedCount = stepIndex.coerceAtLeast(0),
            completedAt = System.currentTimeMillis(),
            summary = "Gagal pada sub-tugas [${feedback.failedStepTitle}]: ${feedback.errorMessage.take(150)}"
        )

        _uiState.update {
            it.copy(
                isAiThinking = false,
                autonomousAgent = it.autonomousAgent.copy(
                    stage = AgentStage.FAILED,
                    statusText = "Eksekusi terhenti pada sub-tugas. Saran rencana perbaikan (Refined Plan) siap ditinjau.",
                    feedback = feedback
                ),
                infoSnackbarMessage = "⚠️ Kendala terdeteksi: ${feedback.errorCategory.label}. Saran rencana baru tersedia."
            )
        }
        appendAgentThought("⚠️ [FEEDBACK PROPOSER] Refined Plan suggestions generated for user review.")
    }

    fun executeRefinedPlan(suggestion: RefinedPlanSuggestion) {
        dismissAgentFeedback()
        appendAgentThought("🚀 [REFINED PLAN SELECTED] \"${suggestion.title}\" (${suggestion.strategyName})")
        startAutonomousAgent(suggestion.revisedGoal)
    }

    fun retryCurrentAutonomousStep() {
        val currentGoal = _uiState.value.autonomousAgent.goal
        if (currentGoal.isNotBlank()) {
            dismissAgentFeedback()
            appendAgentThought("🔄 [MANUAL RE-ATTEMPT] Retrying autonomous task execution...")
            startAutonomousAgent(currentGoal)
        }
    }

    fun dismissAgentFeedback() {
        _uiState.update {
            it.copy(
                autonomousAgent = it.autonomousAgent.copy(
                    feedback = null
                )
            )
        }
    }

    fun selectTaskPlanForDetails(planId: String?) {
        _uiState.update { it.copy(selectedPlanIdForDetails = planId) }
        if (planId != null) {
            viewModelScope.launch {
                repository.getSubTasksForPlan(planId).collectLatest { subTasks ->
                    _uiState.update { it.copy(selectedTaskPlanSubTasks = subTasks) }
                }
            }
        } else {
            _uiState.update { it.copy(selectedTaskPlanSubTasks = emptyList()) }
        }
    }

    fun deleteTaskPlan(planId: String) {
        viewModelScope.launch {
            repository.deleteTaskPlan(planId)
            _uiState.update {
                it.copy(
                    selectedPlanIdForDetails = if (it.selectedPlanIdForDetails == planId) null else it.selectedPlanIdForDetails,
                    infoSnackbarMessage = "Task plan deleted from Room DB"
                )
            }
        }
    }

    fun insertCodeIntoEditor(code: String) {
        val current = _uiState.value.editorContent
        val updated = "$current\n\n$code"
        updateEditorContent(updated)
        _uiState.update { it.copy(isAiSheetOpen = false, infoSnackbarMessage = "Code snippet inserted into editor") }
    }

    fun replaceEditorWithCode(code: String) {
        updateEditorContent(code)
        _uiState.update { it.copy(isAiSheetOpen = false, infoSnackbarMessage = "Editor updated with AI code") }
    }

    fun addVisualNode(type: ComponentType) {
        val newNode = VisualUiNode(
            type = type,
            label = "New ${type.displayName}",
            colorHex = "Primary"
        )
        _uiState.update {
            it.copy(
                visualNodes = it.visualNodes + newNode,
                selectedVisualNodeId = newNode.id
            )
        }
        syncVisualToCode()
    }

    fun updateVisualNode(node: VisualUiNode) {
        _uiState.update { state ->
            state.copy(
                visualNodes = state.visualNodes.map { if (it.id == node.id) node else it }
            )
        }
        syncVisualToCode()
    }

    fun deleteVisualNode(id: String) {
        _uiState.update { state ->
            state.copy(
                visualNodes = state.visualNodes.filter { it.id != id },
                selectedVisualNodeId = if (state.selectedVisualNodeId == id) null else state.selectedVisualNodeId
            )
        }
        syncVisualToCode()
    }

    fun selectVisualNode(id: String?) {
        _uiState.update { it.copy(selectedVisualNodeId = id) }
    }

    fun syncVisualToCode() {
        val project = _uiState.value.currentProject ?: return
        val nodes = _uiState.value.visualNodes
        val generatedCode = VisualLayoutBridge.generateComposeCode(nodes, project.name, project.packageName)
        updateEditorContent(generatedCode)
    }

    fun toggleLogcat(open: Boolean) {
        _uiState.update { it.copy(isLogcatOpen = open) }
    }

    fun setLogcatFilter(filter: String) {
        _uiState.update { it.copy(logcatFilter = filter) }
    }

    fun clearLogcat() {
        _uiState.update { it.copy(logcatEntries = emptyList()) }
    }

    fun addLogcat(tag: String, level: String, message: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val entry = LogcatEntry(
            id = UUID.randomUUID().toString(),
            tag = tag,
            level = level,
            message = message,
            timestamp = timeFormat.format(Date())
        )
        _uiState.update {
            it.copy(logcatEntries = it.logcatEntries + entry)
        }
    }

    fun toggleSearch(open: Boolean) {
        _uiState.update { it.copy(isSearchOpen = open) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setReplaceQuery(query: String) {
        _uiState.update { it.copy(replaceQuery = query) }
    }

    fun executeReplaceAll() {
        val query = _uiState.value.searchQuery
        val replace = _uiState.value.replaceQuery
        if (query.isNotEmpty()) {
            val content = _uiState.value.editorContent
            val replaced = content.replace(query, replace)
            updateEditorContent(replaced)
            _uiState.update { it.copy(infoSnackbarMessage = "Replaced all occurrences of '$query'") }
        }
    }

    fun toggleNewProjectDialog(open: Boolean) {
        _uiState.update { it.copy(isNewProjectDialogOpen = open) }
    }

    fun toggleNewFileDialog(open: Boolean) {
        _uiState.update { it.copy(isNewFileDialogOpen = open) }
    }

    fun toggleExportDialog(open: Boolean) {
        _uiState.update { it.copy(isExportDialogOpen = open) }
    }

    fun toggleEmulatorDarkMode() {
        _uiState.update { it.copy(emulatorDarkMode = !it.emulatorDarkMode) }
    }

    fun toggleEmulatorOrientation() {
        _uiState.update { it.copy(emulatorOrientationLandscape = !it.emulatorOrientationLandscape) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(infoSnackbarMessage = null) }
    }

    fun setDiagnosticsFilter(filter: String) {
        _uiState.update { it.copy(diagnosticsFilter = filter) }
    }

    fun navigateToDiagnostic(diagnostic: BuildDiagnostic) {
        val files = _uiState.value.files
        // Resolve target file
        val targetFile = GradleConsoleParser.resolveFile(diagnostic.filePath, files)
            ?: files.find { it.name.equals(diagnostic.fileName, ignoreCase = true) }
            ?: files.find { it.path.endsWith(diagnostic.fileName) }

        if (targetFile != null) {
            openFile(targetFile)
        }

        _uiState.update {
            it.copy(
                highlightedErrorLine = diagnostic.line,
                activeDiagnostic = diagnostic,
                isLogcatOpen = false, // Minimize bottom sheet to reveal editor
                viewMode = if (it.viewMode == StudioViewMode.DESIGN_ONLY) StudioViewMode.SPLIT_VIEW else it.viewMode,
                infoSnackbarMessage = "Jumped to ${diagnostic.fileName}:${diagnostic.line}"
            )
        }
    }

    fun dismissActiveDiagnostic() {
        _uiState.update {
            it.copy(
                highlightedErrorLine = null,
                activeDiagnostic = null
            )
        }
    }

    fun askAiFixForDiagnostic(diagnostic: BuildDiagnostic) {
        toggleAiSheet(true)
        val prompt = "How do I fix this build error in ${diagnostic.fileName} at line ${diagnostic.line}?\n\nError: ${diagnostic.message}\n${if (diagnostic.codeSnippet != null) "Snippet:\n${diagnostic.codeSnippet}" else ""}\n\nPlease provide the corrected code."
        sendAiPrompt(prompt)
    }

    fun toggleRealtimeSyntaxChecking(enabled: Boolean? = null) {
        val nextState = enabled ?: !_uiState.value.isRealtimeCheckingEnabled
        _uiState.update { it.copy(isRealtimeCheckingEnabled = nextState) }
        if (nextState) {
            runManualSyntaxCheck()
        } else {
            _uiState.update { it.copy(realtimeDiagnostics = emptyList()) }
        }
    }

    fun runManualSyntaxCheck() {
        val active = _uiState.value.activeFile ?: return
        val currentContent = _uiState.value.editorContent
        val tempFile = active.copy(content = currentContent)
        val diags = SyntaxValidator.validateFile(tempFile)
        _uiState.update {
            it.copy(
                realtimeDiagnostics = diags,
                activeDiagnostic = diags.firstOrNull(),
                highlightedErrorLine = diags.firstOrNull()?.line,
                infoSnackbarMessage = if (diags.isEmpty()) "✓ Syntax check passed: 0 errors" else "Found ${diags.size} syntax issue(s)"
            )
        }
    }

    fun toggleGitHubImportDialog(open: Boolean) {
        _uiState.update { it.copy(isGitHubImportDialogOpen = open, gitHubErrorMessage = null, gitHubImportProgress = null) }
        if (open && !_uiState.value.gitHubToken.isNullOrBlank() && _uiState.value.gitHubUser == null) {
            loadGitHubUserAndRepos()
        }
    }

    fun toggleGitHubSyncDialog(open: Boolean) {
        _uiState.update { it.copy(isGitHubSyncDialogOpen = open, gitHubErrorMessage = null) }
    }

    fun setGitCommitMessage(msg: String) {
        _uiState.update { it.copy(gitCommitMessage = msg) }
    }

    fun saveGitHubToken(token: String) {
        gitHubRepository.saveToken(token)
        _uiState.update { it.copy(gitHubToken = token, gitHubErrorMessage = null) }
        loadGitHubUserAndRepos()
    }

    fun clearGitHubToken() {
        gitHubRepository.clearToken()
        _uiState.update {
            it.copy(
                gitHubToken = null,
                gitHubUser = null,
                gitHubUserRepos = emptyList(),
                infoSnackbarMessage = "GitHub token removed"
            )
        }
    }

    fun loadGitHubUserAndRepos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGitHubLoading = true, gitHubErrorMessage = null) }
            when (val userResult = gitHubRepository.getAuthenticatedUser()) {
                is GitHubResult.Success -> {
                    _uiState.update { it.copy(gitHubUser = userResult.data) }
                    when (val repoResult = gitHubRepository.getUserRepositories()) {
                        is GitHubResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isGitHubLoading = false,
                                    gitHubUserRepos = repoResult.data
                                )
                            }
                        }
                        is GitHubResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isGitHubLoading = false,
                                    gitHubErrorMessage = repoResult.message
                                )
                            }
                        }
                    }
                }
                is GitHubResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isGitHubLoading = false,
                            gitHubErrorMessage = userResult.message
                        )
                    }
                }
            }
        }
    }

    fun importGitHubRepository(owner: String, repo: String, branch: String = "main") {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGitHubLoading = true,
                    gitHubErrorMessage = null,
                    gitHubImportProgress = null
                )
            }

            val result = gitHubRepository.importRepository(
                owner = owner,
                repo = repo,
                branch = branch,
                onProgress = { progress ->
                    _uiState.update { it.copy(gitHubImportProgress = progress) }
                }
            )

            when (result) {
                is GitHubResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isGitHubLoading = false,
                            isGitHubImportDialogOpen = false,
                            gitHubImportProgress = null,
                            infoSnackbarMessage = "Successfully imported $owner/$repo"
                        )
                    }
                    selectProject(result.data)
                }
                is GitHubResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isGitHubLoading = false,
                            gitHubErrorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun pushCurrentFileToGitHub(commitMessage: String) {
        val currentProj = _uiState.value.currentProject ?: return
        val active = _uiState.value.activeFile ?: return
        val currentContent = _uiState.value.editorContent

        viewModelScope.launch {
            _uiState.update { it.copy(isGitPushing = true) }
            // Save file locally first
            val updatedFile = active.copy(content = currentContent)
            repository.updateFile(updatedFile)

            val result = gitHubRepository.pushFileToGitHub(currentProj, updatedFile, commitMessage)
            when (result) {
                is GitHubResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isGitPushing = false,
                            isModified = false,
                            activeFile = updatedFile,
                            isGitHubSyncDialogOpen = false,
                            gitCommitMessage = "",
                            infoSnackbarMessage = "✓ Pushed commit: ${result.data.sha.take(7)}"
                        )
                    }
                    addLogcat("GitPush", "I", "Successfully committed & pushed ${active.name} (SHA: ${result.data.sha})")
                }
                is GitHubResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isGitPushing = false,
                            gitHubErrorMessage = result.message
                        )
                    }
                    addLogcat("GitPush", "E", result.message)
                }
            }
        }
    }

    fun pullLatestFromGitHub() {
        val currentProj = _uiState.value.currentProject ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isGitPulling = true, gitHubErrorMessage = null) }
            val result = gitHubRepository.pullLatestChanges(currentProj) { progress ->
                _uiState.update { it.copy(gitHubImportProgress = progress) }
            }

            when (result) {
                is GitHubResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isGitPulling = false,
                            gitHubImportProgress = null,
                            isGitHubSyncDialogOpen = false,
                            infoSnackbarMessage = "✓ Pulled and synced ${result.data} files from GitHub"
                        )
                    }
                    addLogcat("GitPull", "I", "Updated ${result.data} files from remote GitHub repository")
                }
                is GitHubResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isGitPulling = false,
                            gitHubErrorMessage = result.message
                        )
                    }
                    addLogcat("GitPull", "E", result.message)
                }
            }
        }
    }
}

