package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AgentSubTaskEntity
import com.example.data.local.AgentTaskPlanEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AgentStepStatus
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.AiFileOperation
import com.example.ui.viewmodel.AiOperationType
import com.example.ui.viewmodel.RefinedPlanSuggestion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantSheet(
    isOpen: Boolean,
    isThinking: Boolean,
    messages: List<AiChatMessage>,
    projectName: String? = null,
    projectFileCount: Int = 0,
    diagnosticCount: Int = 0,
    geminiApiKey: String? = null,
    autonomousSession: com.example.ui.viewmodel.AutonomousAgentSession = com.example.ui.viewmodel.AutonomousAgentSession(),
    savedTaskPlans: List<AgentTaskPlanEntity> = emptyList(),
    selectedPlanSubTasks: List<AgentSubTaskEntity> = emptyList(),
    selectedPlanIdForDetails: String? = null,
    onSelectPlanForDetails: (String?) -> Unit = {},
    onDeletePlan: (String) -> Unit = {},
    onExecuteRefinedPlan: (RefinedPlanSuggestion) -> Unit = {},
    onRetryAutonomousStep: () -> Unit = {},
    onDismissAgentFeedback: () -> Unit = {},
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit,
    onStartAutonomousAgent: (String) -> Unit = {},
    onStopAutonomousAgent: () -> Unit = {},
    onToggleAutonomousMode: (Boolean) -> Unit = {},
    onAutoHealProject: () -> Unit = {},
    onApplyFileOperation: (AiFileOperation) -> Unit = {},
    onApplyAllFileOperations: (List<AiFileOperation>) -> Unit = {},
    onInsertCode: (String) -> Unit,
    onReplaceFile: (String) -> Unit,
    onSaveApiKey: (String) -> Unit = {},
    selectedModel: String = com.example.data.ai.GeminiAiService.MODEL_FLASH,
    onSelectModel: (String) -> Unit = {},
    onOpenCodeReview: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val clipboardManager = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    var autonomousGoalInput by remember { mutableStateOf("") }
    var selectedSheetTab by remember { mutableIntStateOf(if (autonomousSession.isActive) 0 else 0) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey ?: "") }

    val autonomousPresets = listOf(
        "🧮 Bangun Aplikasi Kalkulator Lengkap Material 3",
        "⏱️ Bangun Aplikasi Stopwatch & Lap Tracker",
        "📝 Buat Todo List Lengkap dengan Filter & Animasi Jetpack Compose",
        "🏗️ Bangun Full App Catatan Keuangan (Model, Room DAO, UI & Chart)",
        "🛠️ Auto-Heal & Perbaiki Semua Error Proyek lalu Jalankan",
        "🎨 Refactor & Modernisasi seluruh layar ke Material Design 3",
        "⚡ Tambahkan Dark Theme Switcher & Visual Navigation State"
    )

    val quickPrompts = listOf(
        "📖 Jelaskan Arsitektur File Aktif",
        "✨ Refactor ke Clean Architecture",
        "🧪 Buat Robolectric Unit Test",
        "🧮 Buat Aplikasi Kalkulator Material 3",
        "⏱️ Buat Aplikasi Stopwatch Presisi",
        "📝 Scaffold Todo List (3 Files: Model, UI, Main)",
        "🛠️ Auto-Heal Project Errors & Build",
        "🎨 Modernize UI with Material 3 Theme"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            // Header with Project Context & Agent Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (autonomousSession.isActive) StudioGreen.copy(alpha = 0.25f) else StudioCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (autonomousSession.isActive) Icons.Default.SmartToy else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (autonomousSession.isActive) StudioGreen else StudioCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Studio Autonomous Agent", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF00E676).copy(alpha = 0.2f)
                        ) {
                            Text("AUTONOMY 99%", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = StudioGreen, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        text = if (!projectName.isNullOrBlank()) "Project: $projectName • $projectFileCount files indexed" else "Full Multi-File Android Autonomy Engine",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = { showApiKeyDialog = true }) {
                    Icon(Icons.Default.Key, contentDescription = "API Key", tint = if (geminiApiKey.isNullOrBlank()) Color.Gray else StudioGreen)
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            // Enhanced Model Selector & AI Doctor Bar
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StudioSurfaceVariant,
                border = BorderStroke(0.5.dp, StudioBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Model:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            FilterChip(
                                selected = selectedModel == com.example.data.ai.GeminiAiService.MODEL_FLASH,
                                onClick = { onSelectModel(com.example.data.ai.GeminiAiService.MODEL_FLASH) },
                                label = { Text("⚡ 3.5 Flash", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioGreen.copy(alpha = 0.25f),
                                    selectedLabelColor = StudioGreen
                                ),
                                modifier = Modifier.height(26.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedModel == com.example.data.ai.GeminiAiService.MODEL_PRO,
                                onClick = { onSelectModel(com.example.data.ai.GeminiAiService.MODEL_PRO) },
                                label = { Text("🧠 3.1 Pro", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = StudioCyan
                                ),
                                modifier = Modifier.height(26.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedModel == com.example.data.ai.GeminiAiService.MODEL_FLASH_LITE,
                                onClick = { onSelectModel(com.example.data.ai.GeminiAiService.MODEL_FLASH_LITE) },
                                label = { Text("🚀 3.1 Lite", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFB74D).copy(alpha = 0.25f),
                                    selectedLabelColor = Color(0xFFFFB74D)
                                ),
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    FilledTonalButton(
                        onClick = onOpenCodeReview,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StudioGreen.copy(alpha = 0.2f),
                            contentColor = StudioGreen
                        ),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dokter", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tab Bar: Agent Otonom vs Task Plans vs Chat Architect
            TabRow(
                selectedTabIndex = selectedSheetTab,
                containerColor = StudioBackground,
                contentColor = StudioGreen,
                divider = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .padding(bottom = 8.dp)
            ) {
                Tab(
                    selected = selectedSheetTab == 0,
                    onClick = { selectedSheetTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agent Otonom", fontWeight = if (selectedSheetTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
                            if (autonomousSession.isActive) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StudioGreen))
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedSheetTab == 1,
                    onClick = { selectedSheetTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Room Plans (${savedTaskPlans.size})", fontWeight = if (selectedSheetTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedSheetTab == 2,
                    onClick = { selectedSheetTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chat AI", fontWeight = if (selectedSheetTab == 2) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
                        }
                    }
                )
            }

            if (selectedSheetTab == 0) {
                // ==================== TAB 0: AGENT OTONOM (99% AUTONOMY DASHBOARD) ====================
                AutonomousAgentPanel(
                    session = autonomousSession,
                    diagnosticCount = diagnosticCount,
                    onStartGoal = { goal ->
                        onStartAutonomousAgent(goal)
                    },
                    onStopAgent = onStopAutonomousAgent,
                    onToggleAutonomousMode = onToggleAutonomousMode,
                    onExecuteRefinedPlan = onExecuteRefinedPlan,
                    onRetryCurrentStep = onRetryAutonomousStep,
                    onDismissFeedback = onDismissAgentFeedback,
                    autonomousPresets = autonomousPresets,
                    modifier = Modifier.weight(1f)
                )
            } else if (selectedSheetTab == 1) {
                // ==================== TAB 1: ROOM TASK PLANS & SUB-TASK TRACKER ====================
                RoomTaskPlansPanel(
                    plans = savedTaskPlans,
                    selectedPlanId = selectedPlanIdForDetails,
                    subTasks = selectedPlanSubTasks,
                    onSelectPlan = onSelectPlanForDetails,
                    onDeletePlan = onDeletePlan,
                    onStartGoal = { goal ->
                        selectedSheetTab = 0
                        onStartAutonomousAgent(goal)
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // ==================== TAB 2: INTERACTIVE CHAT & MULTI-FILE ARCHITECT ====================
                Column(modifier = Modifier.weight(1f)) {
                    // Project Context Status & 1-Tap Auto-Heal Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (diagnosticCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (diagnosticCount > 0) StudioOrange else StudioGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (diagnosticCount > 0) "$diagnosticCount compile issue(s) detected" else "All $projectFileCount project files indexed",
                                    fontSize = 11.sp,
                                    color = if (diagnosticCount > 0) StudioOrange else Color.LightGray
                                )
                            }

                            Button(
                                onClick = onAutoHealProject,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (diagnosticCount > 0) StudioOrange else StudioGreen,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto-Heal & Run", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = StudioBorder)

                    // Message List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (msg.isUser) StudioGreen else StudioSurfaceVariant,
                                    modifier = Modifier.widthIn(max = 340.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = msg.message,
                                            color = if (msg.isUser) Color(0xFF003919) else Color(0xFFE3E2E6),
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )

                                        // MULTI-FILE OPERATIONS CARD
                                        if (msg.fileOperations.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            MultiFileOperationsCard(
                                                operations = msg.fileOperations,
                                                onApplyAll = { onApplyAllFileOperations(msg.fileOperations) },
                                                onApplySingle = { onApplyFileOperation(it) },
                                                onCopyCode = { clipboardManager.setText(AnnotatedString(it)) }
                                            )
                                        } else if (msg.extractedCode != null) {
                                            // Single File Code Preview Fallback
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Surface(
                                                color = StudioBackground,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Kotlin / Compose Code", fontSize = 10.sp, color = StudioCyan, fontWeight = FontWeight.Bold)
                                                        IconButton(
                                                            onClick = { clipboardManager.setText(AnnotatedString(msg.extractedCode)) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                    Text(
                                                        text = msg.extractedCode.take(200) + if (msg.extractedCode.length > 200) "\n..." else "",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF98C379)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        FilledTonalButton(
                                                            onClick = { onInsertCode(msg.extractedCode) },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Insert at Bottom", fontSize = 10.sp)
                                                        }
                                                        Button(
                                                            onClick = { onReplaceFile(msg.extractedCode) },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color.Black),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Replace File", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Transparent Token Usage Metric
                                        if (!msg.isUser && msg.tokenUsage != null && msg.tokenUsage.totalTokens > 0) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color.Black.copy(alpha = 0.25f),
                                                border = BorderStroke(0.5.dp, StudioGreen.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Analytics,
                                                        contentDescription = null,
                                                        tint = StudioGreen,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Penggunaan: ${msg.tokenUsage.totalTokens} token (Prompt: ${msg.tokenUsage.promptTokens} • Respon: ${msg.tokenUsage.candidateTokens})",
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = StudioGreen
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isThinking) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = StudioGreen, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Studio Bot is inspecting project files & generating solutions...", fontSize = 12.sp, color = StudioGreen)
                                }
                            }
                        }
                    }

                    // Quick suggestions carousel
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(quickPrompts) { prompt ->
                            SuggestionChip(
                                onClick = { onSendMessage(prompt) },
                                label = { Text(prompt, fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = StudioSurfaceVariant,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask Studio Bot: create feature, fix errors...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StudioGreen,
                                unfocusedBorderColor = StudioBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = StudioGreen)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF003919))
                        }
                    }
                }
            }
        }
    }

    var isTestingKey by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestSuccessful by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = StudioGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini AI API Key")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Masukkan Google Gemini API Key Anda untuk mengaktifkan Agent AI Autonomous resmi (gemini-3.5-flash / gemini-3.1-pro). Agent akan menulis kode Android lengkap dari nol sampai selesai, mengelola Room DB, Jetpack Compose UI, serta melakukan Self-Healing otomatis.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            testResultText = null
                        },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (apiKeyInput.isNotBlank()) {
                                    isTestingKey = true
                                    testResultText = null
                                    coroutineScope.launch {
                                        val (success, msg) = com.example.data.ai.GeminiAiService.testApiKeyConnection(apiKeyInput)
                                        isTestingKey = false
                                        isTestSuccessful = success
                                        testResultText = msg
                                    }
                                } else {
                                    testResultText = "Masukkan API Key terlebih dahulu"
                                    isTestSuccessful = false
                                }
                            },
                            enabled = !isTestingKey
                        ) {
                            if (isTestingKey) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Connection", fontSize = 11.sp)
                            }
                        }

                        if (!geminiApiKey.isNullOrBlank()) {
                            TextButton(onClick = {
                                apiKeyInput = ""
                                onSaveApiKey("")
                                testResultText = "Key berhasil dihapus"
                                isTestSuccessful = true
                            }) {
                                Text("Clear Key", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    if (testResultText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isTestSuccessful) StudioGreen.copy(alpha = 0.15f) else Color(0xFFFF5252).copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = testResultText ?: "",
                                fontSize = 11.sp,
                                color = if (isTestSuccessful) StudioGreen else Color(0xFFFF5252),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveApiKey(apiKeyInput.trim())
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color.Black)
                ) {
                    Text("Save Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AutonomousAgentPanel(
    session: com.example.ui.viewmodel.AutonomousAgentSession,
    diagnosticCount: Int,
    onStartGoal: (String) -> Unit,
    onStopAgent: () -> Unit,
    onToggleAutonomousMode: (Boolean) -> Unit,
    onExecuteRefinedPlan: (RefinedPlanSuggestion) -> Unit = {},
    onRetryCurrentStep: () -> Unit = {},
    onDismissFeedback: () -> Unit = {},
    autonomousPresets: List<String>,
    modifier: Modifier = Modifier
) {
    var customGoal by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // Agent Status & Autonomy Control Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (session.isActive) Color(0xFF142C20) else StudioBackground,
            border = BorderStroke(1.dp, if (session.isActive) StudioGreen else StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (session.isActive) StudioGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (session.isActive) "AGENT OTONOM: ${session.stage.label}" else "Agent Otonom Siap (Autonomy 99%)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (session.isActive) StudioGreen else Color.White
                        )
                    }

                    if (session.isActive) {
                        OutlinedButton(
                            onClick = onStopAgent,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                            border = BorderStroke(1.dp, Color(0xFFFF5252)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop Agent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (session.isActive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🎯 Goal: \"${session.goal}\"",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = {
                            val activeSteps = session.steps.count { it.status == com.example.ui.viewmodel.AgentStepStatus.COMPLETED }
                            (activeSteps.toFloat() / maxOf(session.steps.size, 1).toFloat()).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = StudioGreen,
                        trackColor = StudioSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Scrollable Area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Interactive Error Detection Feedback & Refined Plan Suggestions Card
            if (session.feedback != null && session.feedback.hasError) {
                item {
                    val feedback = session.feedback
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2B1114),
                        border = BorderStroke(1.dp, Color(0xFFFF5252)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Kendala: ${feedback.errorCategory.label}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF8A80)
                                        )
                                        Text(
                                            text = "Sub-Tugas #${feedback.failedStepIndex + 1}: ${feedback.failedStepTitle}",
                                            fontSize = 10.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onDismissFeedback,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            // Diagnostic message box
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1B0B0D),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = feedback.errorMessage,
                                        fontSize = 11.sp,
                                        color = Color(0xFFFFCDD2),
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    )
                                    if (feedback.suggestedRemedy.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "💡 Remediasi: ${feedback.suggestedRemedy}",
                                            fontSize = 10.sp,
                                            color = StudioGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            // Refined Plans List
                            Text(
                                text = "SARAN RENCANA KERJA BARU (REFINED PLANS):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            feedback.refinedPlans.forEach { plan ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E1517),
                                    border = BorderStroke(0.5.dp, Color(0xFF6B2D33)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = plan.title,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = StudioGreen.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = plan.strategyName,
                                                    fontSize = 8.sp,
                                                    color = StudioGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = plan.explanation,
                                            fontSize = 10.sp,
                                            color = Color(0xFFCFD8DC),
                                            lineHeight = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        plan.revisedSteps.forEach { stepText ->
                                            Text(
                                                text = "  • $stepText",
                                                fontSize = 9.sp,
                                                color = Color.Gray,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { onExecuteRefinedPlan(plan) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = StudioGreen,
                                                contentColor = Color.Black
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(28.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Eksekusi Rencana Ini (Auto-Retry)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = onRetryCurrentStep,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioCyan),
                                    border = BorderStroke(1.dp, StudioCyan),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Coba Lagi Sekarang", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // If Agent is Active: Show Step-by-Step Checklist
            if (session.steps.isNotEmpty()) {
                item {
                    Text("ALUR EKSEKUSI OTONOM (PIPELINE):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            session.steps.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when (step.status) {
                                        com.example.ui.viewmodel.AgentStepStatus.COMPLETED -> {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(18.dp))
                                        }
                                        com.example.ui.viewmodel.AgentStepStatus.RUNNING -> {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = StudioCyan, strokeWidth = 2.dp)
                                        }
                                        com.example.ui.viewmodel.AgentStepStatus.RETRYING -> {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = StudioOrange, strokeWidth = 2.dp)
                                        }
                                        com.example.ui.viewmodel.AgentStepStatus.FAILED -> {
                                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                                        }
                                        else -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, Color.Gray, CircleShape)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (step.status == com.example.ui.viewmodel.AgentStepStatus.RETRYING) "${step.title} (Re-attempting...)" else step.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (step.status == com.example.ui.viewmodel.AgentStepStatus.RUNNING || step.status == com.example.ui.viewmodel.AgentStepStatus.RETRYING) FontWeight.Bold else FontWeight.Normal,
                                            color = when (step.status) {
                                                com.example.ui.viewmodel.AgentStepStatus.COMPLETED -> StudioGreen
                                                com.example.ui.viewmodel.AgentStepStatus.RUNNING -> Color.White
                                                com.example.ui.viewmodel.AgentStepStatus.RETRYING -> StudioOrange
                                                com.example.ui.viewmodel.AgentStepStatus.FAILED -> Color(0xFFFF5252)
                                                else -> Color.Gray
                                            }
                                        )
                                        if (step.description.isNotBlank()) {
                                            Text(step.description, fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Real-Time Thought Stream Box
            if (session.thoughtLogs.isNotEmpty()) {
                item {
                    Text("LIVE REASONING & THOUGHT LOGS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0D1117),
                        border = BorderStroke(1.dp, Color(0xFF30363D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            session.thoughtLogs.takeLast(10).forEach { log ->
                                Text(
                                    text = log,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = if (log.contains("SUCCESS") || log.contains("COMPLETED")) StudioGreen else if (log.contains("ERROR")) Color(0xFFFF5252) else if (log.contains("THOUGHT")) StudioCyan else Color(0xFFC9D1D9),
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 1-Click Autonomous Presets
            item {
                Text("REKOMENDASI GOAL OTONOM (1-CLICK EXECUTION):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    autonomousPresets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StudioSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    customGoal = preset
                                    onStartGoal(preset)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(preset, fontSize = 11.sp, color = Color.White, lineHeight = 15.sp)
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Goal Launcher Input
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = StudioBackground,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Beri Instruksi / Goal pada Agent Otonom:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customGoal,
                        onValueChange = { customGoal = it },
                        placeholder = { Text("Contoh: Bangun UI profil pengguna dengan Room DB & Compose...", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioGreen,
                            unfocusedBorderColor = StudioBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (customGoal.isNotBlank()) {
                                onStartGoal(customGoal)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mulai", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiFileOperationsCard(
    operations: List<AiFileOperation>,
    onApplyAll: () -> Unit,
    onApplySingle: (AiFileOperation) -> Unit,
    onCopyCode: (String) -> Unit
) {
    var expandedOpId by remember { mutableStateOf<String?>(null) }
    val allApplied = operations.all { it.isApplied }

    Surface(
        color = StudioBackground,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${operations.size} Multi-File Project Changes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (allApplied) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StudioGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "✓ Applied All",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onApplyAll,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply All Changes", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File items list
            operations.forEach { op ->
                val isExpanded = expandedOpId == op.id
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedOpId = if (isExpanded) null else op.id },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = Color(op.type.badgeColor).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = op.type.label,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(op.type.badgeColor),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = op.fileName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = op.filePath,
                                        fontSize = 9.sp,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (op.isApplied) {
                                    Text("✓ Applied", fontSize = 9.sp, color = StudioGreen, fontWeight = FontWeight.Bold)
                                } else {
                                    FilledTonalButton(
                                        onClick = { onApplySingle(op) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                        modifier = Modifier.height(22.dp)
                                    ) {
                                        Text("Apply", fontSize = 9.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (!op.summary.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(op.summary, fontSize = 10.sp, color = Color.LightGray)
                        }

                        // Expanded Code Preview
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Source Preview (${op.content.lines().size} lines)", fontSize = 9.sp, color = StudioCyan)
                                    IconButton(
                                        onClick = { onCopyCode(op.content) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    }
                                }
                                Surface(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = op.content.take(300) + if (op.content.length > 300) "\n..." else "",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color(0xFF98C379),
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomTaskPlansPanel(
    plans: List<AgentTaskPlanEntity>,
    selectedPlanId: String?,
    subTasks: List<AgentSubTaskEntity>,
    onSelectPlan: (String?) -> Unit,
    onDeletePlan: (String) -> Unit,
    onStartGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // Summary Header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = StudioBackground,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Room Database Persistence", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${plans.size} task plan(s) recorded in SQLite", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioSurfaceVariant
                ) {
                    Text(
                        text = "ROOM v3",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (plans.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Belum ada Task Plan yang tersimpan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Jalankan instruksi pada Agent Otonom untuk merekam perincian sub-tugas ke Room DB.", fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(plans, key = { it.id }) { plan ->
                    val isExpanded = selectedPlanId == plan.id
                    val isCompleted = plan.status == "COMPLETED"
                    val isFailed = plan.status == "FAILED"

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isExpanded) StudioSurfaceVariant else StudioBackground,
                        border = BorderStroke(1.dp, if (isExpanded) StudioGreen else StudioBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isExpanded) onSelectPlan(null) else onSelectPlan(plan.id)
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = when {
                                            isCompleted -> Icons.Default.CheckCircle
                                            isFailed -> Icons.Default.Cancel
                                            else -> Icons.Default.Pending
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            isCompleted -> StudioGreen
                                            isFailed -> Color(0xFFFF5252)
                                            else -> StudioCyan
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = plan.userGoal,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = if (isExpanded) 4 else 1
                                        )
                                        Text(
                                            text = "${dateFormat.format(java.util.Date(plan.createdAt))} • ${plan.completedSubTasks}/${plan.totalSubTasks} sub-tugas selesai",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when {
                                            isCompleted -> StudioGreen.copy(alpha = 0.2f)
                                            isFailed -> Color(0xFFFF5252).copy(alpha = 0.2f)
                                            else -> StudioCyan.copy(alpha = 0.2f)
                                        }
                                    ) {
                                        Text(
                                            text = plan.status,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isCompleted -> StudioGreen
                                                isFailed -> Color(0xFFFF5252)
                                                else -> StudioCyan
                                            },
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onDeletePlan(plan.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Plan", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Progress indicator
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = {
                                    (plan.completedSubTasks.toFloat() / maxOf(plan.totalSubTasks, 1).toFloat()).coerceIn(0f, 1f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (isCompleted) StudioGreen else StudioCyan,
                                trackColor = StudioSurfaceVariant
                            )

                            // Expanded Sub-Tasks Checklist from Room DB
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    HorizontalDivider(color = StudioBorder, modifier = Modifier.padding(bottom = 8.dp))
                                    Text("RINCIAN SUB-TUGAS (ROOM DB TRACKING):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioGreen)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    if (subTasks.isEmpty()) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp).align(Alignment.CenterHorizontally), strokeWidth = 2.dp, color = StudioGreen)
                                    } else {
                                        subTasks.forEach { st ->
                                            val isStDone = st.status == "COMPLETED"
                                            val isStRunning = st.status == "RUNNING"
                                            val isStFailed = st.status == "FAILED"

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF0D1117),
                                                border = BorderStroke(0.5.dp, Color(0xFF30363D)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = when {
                                                            isStDone -> Icons.Default.CheckCircle
                                                            isStFailed -> Icons.Default.Cancel
                                                            isStRunning -> Icons.Default.PlayCircleFilled
                                                            else -> Icons.Default.RadioButtonUnchecked
                                                        },
                                                        contentDescription = null,
                                                        tint = when {
                                                            isStDone -> StudioGreen
                                                            isStFailed -> Color(0xFFFF5252)
                                                            isStRunning -> StudioCyan
                                                            else -> Color.Gray
                                                        },
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "${st.stepOrder}. ${st.title}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color.White
                                                        )
                                                        if (st.description.isNotBlank()) {
                                                            Text(st.description, fontSize = 10.sp, color = Color.LightGray)
                                                        }
                                                        if (!st.outputLog.isNullOrBlank()) {
                                                            Text("Output: ${st.outputLog}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = StudioGreen)
                                                        }
                                                    }
                                                    if (st.executionTimeMs > 0) {
                                                        Text(
                                                            text = "${st.executionTimeMs}ms",
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = { onStartGoal(plan.userGoal) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp),
                                            border = BorderStroke(1.dp, StudioGreen)
                                        ) {
                                            Icon(Icons.Default.Replay, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Jalankan Ulang", fontSize = 10.sp, color = StudioGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

