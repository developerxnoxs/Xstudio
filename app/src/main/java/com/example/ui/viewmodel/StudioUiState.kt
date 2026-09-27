package com.example.ui.viewmodel

import com.example.builder.BuildResult
import com.example.core.ApkMetadata
import com.example.core.BuildDiagnostic
import com.example.data.github.GitHubRepo
import com.example.data.github.GitHubUser
import com.example.data.github.ImportProgress
import com.example.data.local.AgentSubTaskEntity
import com.example.data.local.AgentTaskPlanEntity
import com.example.data.local.BuildLogEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import com.example.engine.VisualUiNode

enum class StudioViewMode {
    CODE_ONLY,
    DESIGN_ONLY,
    SPLIT_VIEW
}

enum class AiOperationType(val label: String, val badgeColor: Long) {
    CREATE("+ CREATE", 0xFF81C784),
    EDIT("~ EDIT", 0xFF4DD0E1),
    DELETE("- DELETE", 0xFFE57373)
}

enum class AgentStage(val label: String, val iconColor: Long) {
    IDLE("Idle", 0xFF9E9E9E),
    PLANNING("Planning Architecture...", 0xFF64B5F6),
    GENERATING_CODE("Generating Project Files...", 0xFFFFB74D),
    APPLYING_CHANGES("Applying Multi-File Changes...", 0xFF81C784),
    COMPILING("Compiling Gradle & Checking Diagnostics...", 0xFFBA68C8),
    SELF_HEALING("Self-Healing Compiler Errors...", 0xFFFF8A65),
    DEPLOYING("Deploying to Pixel 9 Pro Emulator...", 0xFF4DD0E1),
    COMPLETED("Autonomous Goal Completed!", 0xFF00E676),
    PAUSED("Agent Paused", 0xFFFFD54F),
    FAILED("Agent Halted (Max Retries Exceeded)", 0xFFE57373)
}

enum class AgentStepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    RETRYING
}

enum class AgentErrorCategory(val label: String, val colorHex: Long) {
    SYNTAX_OR_TYPE("Syntax & Type Diagnostics", 0xFFFF5252),
    DEPENDENCY_OR_IMPORT("Unresolved Dependencies / Imports", 0xFFFFB74D),
    COMPILATION_FAILURE("Gradle Build Failure", 0xFFFF1744),
    CODE_SYNTHESIS_ERROR("AI Synthesis Fault", 0xFFBA68C8),
    FILESYSTEM_PERSISTENCE("File System / DB Conflict", 0xFFFF8A65),
    DEPLOYMENT_TIMEOUT("Virtual Device Deploy Issue", 0xFF4DD0E1),
    UNKNOWN("Runtime Exception", 0xFFE57373)
}

data class RefinedPlanSuggestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val strategyName: String,
    val explanation: String,
    val revisedGoal: String,
    val revisedSteps: List<String>
)

data class AgentExecutionFeedback(
    val hasError: Boolean = false,
    val errorCategory: AgentErrorCategory = AgentErrorCategory.UNKNOWN,
    val failedStepIndex: Int = -1,
    val failedStepTitle: String = "",
    val errorMessage: String = "",
    val diagnosticDetails: String = "",
    val attemptNumber: Int = 1,
    val maxAttempts: Int = 2,
    val isAutoReattempting: Boolean = false,
    val suggestedRemedy: String = "",
    val refinedPlans: List<RefinedPlanSuggestion> = emptyList()
)

data class AgentPlanStep(
    val id: String = java.util.UUID.randomUUID().toString(),
    val planId: String = "",
    val stepOrder: Int = 0,
    val title: String,
    val description: String = "",
    val stage: AgentStage = AgentStage.PLANNING,
    val status: AgentStepStatus = AgentStepStatus.PENDING,
    val targetFilePath: String? = null,
    val outputLog: String? = null,
    val executionTimeMs: Long = 0
)

data class AutonomousAgentSession(
    val isActive: Boolean = false,
    val currentPlanId: String = "",
    val goal: String = "",
    val stage: AgentStage = AgentStage.IDLE,
    val steps: List<AgentPlanStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val currentIteration: Int = 0,
    val maxIterations: Int = 4,
    val thoughtLogs: List<String> = emptyList(),
    val statusText: String = "",
    val isAutonomousMode: Boolean = true,
    val modifiedFilesCount: Int = 0,
    val fixedDiagnosticsCount: Int = 0,
    val isPaused: Boolean = false,
    val feedback: AgentExecutionFeedback? = null
)

data class AiFileOperation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: AiOperationType,
    val filePath: String,
    val fileName: String,
    val fileType: String = "KOTLIN",
    val content: String = "",
    val summary: String = "",
    val isApplied: Boolean = false
)

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val message: String,
    val extractedCode: String? = null,
    val fileOperations: List<AiFileOperation> = emptyList(),
    val isAutoHealFix: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class LogcatEntry(
    val id: String,
    val tag: String,
    val level: String, // "V", "D", "I", "W", "E"
    val message: String,
    val timestamp: String
)

data class StudioUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val currentProject: ProjectEntity? = null,
    val files: List<ProjectFileEntity> = emptyList(),
    val activeFile: ProjectFileEntity? = null,
    val openTabs: List<ProjectFileEntity> = emptyList(),
    val editorContent: String = "",
    val isModified: Boolean = false,
    val viewMode: StudioViewMode = StudioViewMode.CODE_ONLY,
    val isBuilding: Boolean = false,
    val buildProgress: Float = 0f,
    val currentBuildTask: String = "",
    val lastBuildResult: BuildResult? = null,
    val latestApkMetadata: ApkMetadata? = null,
    val buildLogs: List<BuildLogEntity> = emptyList(),
    val isEmulatorVisible: Boolean = false,
    val isAiSheetOpen: Boolean = false,
    val isAiThinking: Boolean = false,
    val aiMessages: List<AiChatMessage> = emptyList(),
    val visualNodes: List<VisualUiNode> = emptyList(),
    val selectedVisualNodeId: String? = null,
    val visualDesignerMode: String = "XML", // "XML" or "COMPOSE"
    val isRealtimeVisualSyncEnabled: Boolean = true,
    val isLogcatOpen: Boolean = false,
    val selectedBottomTab: Int = 0, // 0: Build & Errors, 1: Logcat, 2: Terminal
    val logcatFilter: String = "ALL",
    val logcatEntries: List<LogcatEntry> = emptyList(),
    val terminalLines: List<String> = listOf("Android Studio Mobile IDE Terminal [Version 2026.1]", "Type 'help' for available commands."),
    val terminalInput: String = "",
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val isNewProjectDialogOpen: Boolean = false,
    val isNewFileDialogOpen: Boolean = false,
    val isRenameDialogOpen: Boolean = false,
    val targetFileForAction: ProjectFileEntity? = null,
    val isExportDialogOpen: Boolean = false,
    val emulatorDarkMode: Boolean = false,
    val emulatorOrientationLandscape: Boolean = false,
    val highlightedErrorLine: Int? = null,
    val activeDiagnostic: BuildDiagnostic? = null,
    val diagnosticsFilter: String = "ALL",
    val realtimeDiagnostics: List<BuildDiagnostic> = emptyList(),
    val isRealtimeCheckingEnabled: Boolean = true,
    // GitHub Integration States
    val isGitHubImportDialogOpen: Boolean = false,
    val isGitHubSyncDialogOpen: Boolean = false,
    val gitHubToken: String? = null,
    val gitHubUser: GitHubUser? = null,
    val gitHubUserRepos: List<GitHubRepo> = emptyList(),
    val isGitHubLoading: Boolean = false,
    val gitHubImportProgress: ImportProgress? = null,
    val gitHubErrorMessage: String? = null,
    val isGitPushing: Boolean = false,
    val isGitPulling: Boolean = false,
    val gitCommitMessage: String = "",
    val geminiApiKey: String? = null,
    val isAutoHealingProject: Boolean = false,
    val autonomousAgent: AutonomousAgentSession = AutonomousAgentSession(),
    val savedTaskPlans: List<AgentTaskPlanEntity> = emptyList(),
    val selectedTaskPlanSubTasks: List<AgentSubTaskEntity> = emptyList(),
    val selectedPlanIdForDetails: String? = null,
    val infoSnackbarMessage: String? = null,
    // New Feature States
    val isApkAnalyzerOpen: Boolean = false,
    val apkAnalysisReport: com.example.analyzer.ApkAnalysisReport? = null,
    val isAnalyzingApk: Boolean = false,
    val isGlobalSearchOpen: Boolean = false,
    val isStringManagerOpen: Boolean = false,
    val isDependencyCatalogOpen: Boolean = false,
    val isSnippetGeneratorOpen: Boolean = false,
    val isKeystoreSignerOpen: Boolean = false,
    val signedApkResult: com.example.signing.SignedApkResult? = null,
    val isSigningApk: Boolean = false,
    val savedKeystoreDetails: com.example.signing.KeystoreDetails? = null
)
