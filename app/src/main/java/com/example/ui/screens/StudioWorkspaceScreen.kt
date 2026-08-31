package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectFileEntity
import com.example.editor.CodeEditorView
import com.example.ui.components.DeviceEmulatorView
import com.example.ui.components.FileTreeDrawerContent
import com.example.ui.components.VisualDesignerScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.StudioViewModel
import com.example.ui.viewmodel.StudioViewMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioWorkspaceScreen(
    viewModel: StudioViewModel,
    uiState: StudioUiState,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val tabScrollState = rememberScrollState()

    var showOverflowMenu by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = StudioSurface,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.width(300.dp)
            ) {
                FileTreeDrawerContent(
                    project = uiState.currentProject,
                    files = uiState.files,
                    activeFile = uiState.activeFile,
                    onFileClick = { file ->
                        viewModel.openFile(file)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onAddNewFileClick = {
                        viewModel.toggleNewFileDialog(true)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onRenameFileClick = { file ->
                        viewModel.openRenameDialog(file)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onDeleteFileClick = { file ->
                        viewModel.deleteFile(file)
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open File Explorer", tint = Color.White)
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = uiState.currentProject?.name ?: "Android Studio Mobile",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = uiState.activeFile?.name ?: "No file open",
                                fontSize = 11.sp,
                                color = StudioGreen
                            )
                        }
                    },
                    actions = {
                        // View Mode Switcher
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StudioSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                IconButton(
                                    onClick = { viewModel.setViewMode(StudioViewMode.CODE_ONLY) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Code,
                                        contentDescription = "Code",
                                        tint = if (uiState.viewMode == StudioViewMode.CODE_ONLY) StudioGreen else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.setViewMode(StudioViewMode.SPLIT_VIEW) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.VerticalSplit,
                                        contentDescription = "Split",
                                        tint = if (uiState.viewMode == StudioViewMode.SPLIT_VIEW) StudioGreen else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.setViewMode(StudioViewMode.DESIGN_ONLY) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = "Design",
                                        tint = if (uiState.viewMode == StudioViewMode.DESIGN_ONLY) StudioGreen else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Git & GitHub Sync Action
                        IconButton(
                            onClick = {
                                if (uiState.currentProject?.isGitHubProject == true) {
                                    viewModel.toggleGitHubSyncDialog(true)
                                } else {
                                    viewModel.toggleGitHubImportDialog(true)
                                }
                            }
                        ) {
                            Icon(
                                if (uiState.currentProject?.isGitHubProject == true) Icons.Default.CloudSync else Icons.Default.CloudDownload,
                                contentDescription = "Git & GitHub",
                                tint = if (uiState.currentProject?.isGitHubProject == true) StudioGreen else Color.LightGray
                            )
                        }

                        // Run App Button
                        FilledIconButton(
                            onClick = { viewModel.runBuildAndRun() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (uiState.isBuilding) Color.Gray else StudioGreen
                            ),
                            modifier = Modifier.size(34.dp),
                            enabled = !uiState.isBuilding
                        ) {
                            if (uiState.isBuilding) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run App", tint = Color(0xFF003919), modifier = Modifier.size(20.dp))
                            }
                        }

                        // Autonomous Agent 99% Quick Launcher
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (uiState.autonomousAgent.isActive) StudioGreen.copy(alpha = 0.25f) else StudioSurfaceVariant,
                            border = BorderStroke(1.dp, if (uiState.autonomousAgent.isActive) StudioGreen else StudioBorder),
                            modifier = Modifier
                                .height(32.dp)
                                .clickable { viewModel.toggleAiSheet(true) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "Agent Otonom",
                                    tint = if (uiState.autonomousAgent.isActive) StudioGreen else StudioCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.autonomousAgent.isActive) "Running 99%" else "Agent 99%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.autonomousAgent.isActive) StudioGreen else Color.White
                                )
                            }
                        }

                        // Studio Bot (Gemini)
                        IconButton(onClick = { viewModel.toggleAiSheet(true) }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Studio Bot", tint = StudioGreen)
                        }

                        // More options
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Git & GitHub Sync") },
                                    leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = StudioGreen) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.toggleGitHubSyncDialog(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import from GitHub") },
                                    leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = StudioGreen) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.toggleGitHubImportDialog(true)
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Find & Replace") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.toggleSearch(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Format Code (Ctrl+Alt+L)") },
                                    leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = StudioGreen) },
                                    onClick = {
                                        showOverflowMenu = false
                                        val activeContent = uiState.activeFile?.content ?: ""
                                        val fileType = uiState.activeFile?.fileType ?: "KOTLIN"
                                        val formatted = com.example.editor.IntelliSenseEngine.formatCode(activeContent, fileType)
                                        viewModel.updateEditorContent(formatted)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Build Output & Diagnostics") },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.setBottomTab(0)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Logcat Console") },
                                    leadingIcon = { Icon(Icons.Default.Article, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.setBottomTab(1)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Terminal Shell") },
                                    leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.setBottomTab(2)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("App Inspection & Database") },
                                    leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, tint = StudioCyan) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.setBottomTab(3)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Auto-Heal Project (Fix Errors)") },
                                    leadingIcon = { Icon(Icons.Default.Healing, contentDescription = null, tint = StudioOrange) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.autoHealProjectAndRun()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Install to Virtual Device (Emulator)") },
                                    leadingIcon = { Icon(Icons.Default.InstallMobile, contentDescription = null, tint = StudioGreen) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.installAppToVirtualDevice()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Virtual Android Emulator") },
                                    leadingIcon = { Icon(Icons.Default.Smartphone, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.toggleEmulator(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Install / Export APK") },
                                    leadingIcon = { Icon(Icons.Default.Android, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.toggleExportDialog(true)
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Projects Dashboard") },
                                    leadingIcon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onNavigateToDashboard()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioSurface)
                )
            },
            bottomBar = {
                // Bottom Status Bar
                Surface(
                    color = StudioSurface,
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp
                ) {
                    Column {
                        if (uiState.isBuilding) {
                            LinearProgressIndicator(
                                progress = { uiState.buildProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = StudioGreen,
                                trackColor = StudioSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.toggleLogcat(true) }
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isBuilding) uiState.currentBuildTask else "Gradle: Ready",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("UTF-8", fontSize = 10.sp, color = Color.Gray)
                                Text("Kotlin 2.2", fontSize = 10.sp, color = StudioPurple, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            snackbarHost = {
                if (uiState.infoSnackbarMessage != null) {
                    Snackbar(
                        action = {
                            TextButton(onClick = { viewModel.clearSnackbar() }) {
                                Text("OK", color = StudioGreen)
                            }
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(uiState.infoSnackbarMessage)
                    }
                }
            },
            containerColor = StudioBackground
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Find and Replace Bar (when open)
                if (uiState.isSearchOpen) {
                    Surface(
                        color = StudioSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text("Find...", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { viewModel.toggleSearch(false) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FindReplace, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedTextField(
                                    value = uiState.replaceQuery,
                                    onValueChange = { viewModel.setReplaceQuery(it) },
                                    placeholder = { Text("Replace with...", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { viewModel.executeReplaceAll() },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Replace All", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Open Files Tab Bar
                if (uiState.openTabs.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StudioSurface)
                            .horizontalScroll(tabScrollState)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        uiState.openTabs.forEach { tab ->
                            val isActive = tab.id == uiState.activeFile?.id
                            Surface(
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                color = if (isActive) StudioBackground else StudioSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .clickable { viewModel.openFile(tab) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        when (tab.fileType.uppercase()) {
                                            "KOTLIN" -> Icons.Default.Code
                                            "JAVA" -> Icons.Default.Terminal
                                            "XML" -> Icons.Default.Palette
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = null,
                                        tint = if (isActive) StudioGreen else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tab.name + if (isActive && uiState.isModified) " *" else "",
                                        fontSize = 11.sp,
                                        color = if (isActive) Color.White else Color.Gray,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { viewModel.closeTab(tab) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Tab", tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Workspace Main Area
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.isEmulatorVisible) {
                        DeviceEmulatorView(
                            project = uiState.currentProject,
                            isDarkMode = uiState.emulatorDarkMode,
                            isLandscape = uiState.emulatorOrientationLandscape,
                            onToggleDarkMode = { viewModel.toggleEmulatorDarkMode() },
                            onToggleLandscape = { viewModel.toggleEmulatorOrientation() },
                            onReload = { viewModel.runBuildAndRun() },
                            onOpenLogcat = { viewModel.toggleLogcat(true) },
                            onClose = { viewModel.toggleEmulator(false) },
                            onLogEvent = { tag, level, msg -> viewModel.addLogcat(tag, level, msg) }
                        )
                    } else {
                        val activeFileDiagnostics = remember(uiState.realtimeDiagnostics, uiState.lastBuildResult, uiState.activeFile) {
                            val buildDiags = uiState.lastBuildResult?.diagnostics?.filter { d ->
                                uiState.activeFile != null && (d.fileName.equals(uiState.activeFile.name, ignoreCase = true) || d.filePath == uiState.activeFile.path)
                            } ?: emptyList()

                            if (uiState.realtimeDiagnostics.isNotEmpty()) {
                                uiState.realtimeDiagnostics
                            } else {
                                buildDiags
                            }
                        }

                        when (uiState.viewMode) {
                            StudioViewMode.CODE_ONLY -> {
                                CodeEditorView(
                                    content = uiState.editorContent,
                                    fileType = uiState.activeFile?.fileType ?: "KOTLIN",
                                    onContentChange = { viewModel.updateEditorContent(it) },
                                    diagnostics = activeFileDiagnostics,
                                    targetLineToHighlight = uiState.highlightedErrorLine,
                                    activeDiagnostic = uiState.activeDiagnostic,
                                    isRealtimeCheckingEnabled = uiState.isRealtimeCheckingEnabled,
                                    onToggleRealtimeChecking = { viewModel.toggleRealtimeSyntaxChecking() },
                                    onRunManualCheck = { viewModel.runManualSyntaxCheck() },
                                    onAskAiFix = { viewModel.askAiFixForDiagnostic(it) },
                                    onDismissDiagnostic = { viewModel.dismissActiveDiagnostic() },
                                    searchQuery = uiState.searchQuery
                                )
                            }
                            StudioViewMode.DESIGN_ONLY -> {
                                VisualDesignerScreen(
                                    nodes = uiState.visualNodes,
                                    selectedNodeId = uiState.selectedVisualNodeId,
                                    onSelectNode = { viewModel.selectVisualNode(it) },
                                    onAddNode = { viewModel.addVisualNode(it) },
                                    onUpdateNode = { viewModel.updateVisualNode(it) },
                                    onDeleteNode = { viewModel.deleteVisualNode(it) },
                                    onSyncCode = { viewModel.syncVisualToCode() }
                                )
                            }
                            StudioViewMode.SPLIT_VIEW -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        CodeEditorView(
                                            content = uiState.editorContent,
                                            fileType = uiState.activeFile?.fileType ?: "KOTLIN",
                                            onContentChange = { viewModel.updateEditorContent(it) },
                                            diagnostics = activeFileDiagnostics,
                                            targetLineToHighlight = uiState.highlightedErrorLine,
                                            activeDiagnostic = uiState.activeDiagnostic,
                                            isRealtimeCheckingEnabled = uiState.isRealtimeCheckingEnabled,
                                            onToggleRealtimeChecking = { viewModel.toggleRealtimeSyntaxChecking() },
                                            onRunManualCheck = { viewModel.runManualSyntaxCheck() },
                                            onAskAiFix = { viewModel.askAiFixForDiagnostic(it) },
                                            onDismissDiagnostic = { viewModel.dismissActiveDiagnostic() },
                                            searchQuery = uiState.searchQuery
                                        )
                                    }
                                    HorizontalDivider(color = StudioGreen, thickness = 2.dp)
                                    Box(modifier = Modifier.weight(1f)) {
                                        VisualDesignerScreen(
                                            nodes = uiState.visualNodes,
                                            selectedNodeId = uiState.selectedVisualNodeId,
                                            onSelectNode = { viewModel.selectVisualNode(it) },
                                            onAddNode = { viewModel.addVisualNode(it) },
                                            onUpdateNode = { viewModel.updateVisualNode(it) },
                                            onDeleteNode = { viewModel.deleteVisualNode(it) },
                                            onSyncCode = { viewModel.syncVisualToCode() }
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

    // AI Assistant Sheet & Autonomous Agent 99%
    AiAssistantSheet(
        isOpen = uiState.isAiSheetOpen,
        isThinking = uiState.isAiThinking,
        messages = uiState.aiMessages,
        projectName = uiState.currentProject?.name,
        projectFileCount = uiState.files.size,
        diagnosticCount = uiState.realtimeDiagnostics.size,
        geminiApiKey = uiState.geminiApiKey,
        autonomousSession = uiState.autonomousAgent,
        savedTaskPlans = uiState.savedTaskPlans,
        selectedPlanSubTasks = uiState.selectedTaskPlanSubTasks,
        selectedPlanIdForDetails = uiState.selectedPlanIdForDetails,
        onSelectPlanForDetails = { viewModel.selectTaskPlanForDetails(it) },
        onDeletePlan = { viewModel.deleteTaskPlan(it) },
        onExecuteRefinedPlan = { viewModel.executeRefinedPlan(it) },
        onRetryAutonomousStep = { viewModel.retryCurrentAutonomousStep() },
        onDismissAgentFeedback = { viewModel.dismissAgentFeedback() },
        onDismiss = { viewModel.toggleAiSheet(false) },
        onSendMessage = { viewModel.sendAiPrompt(it) },
        onStartAutonomousAgent = { viewModel.startAutonomousAgent(it) },
        onStopAutonomousAgent = { viewModel.stopAutonomousAgent() },
        onToggleAutonomousMode = { viewModel.toggleAutonomousMode(it) },
        onAutoHealProject = { viewModel.autoHealProjectAndRun() },
        onApplyFileOperation = { viewModel.applyAiFileOperation(it) },
        onApplyAllFileOperations = { viewModel.applyAllAiFileOperations(it) },
        onInsertCode = { viewModel.insertCodeIntoEditor(it) },
        onReplaceFile = { viewModel.replaceEditorWithCode(it) },
        onSaveApiKey = { viewModel.setGeminiApiKey(it) }
    )

    // Logcat & Terminal Sheet
    LogcatTerminalSheet(
        isOpen = uiState.isLogcatOpen,
        selectedTab = uiState.selectedBottomTab,
        filter = uiState.logcatFilter,
        logs = uiState.logcatEntries,
        terminalLines = uiState.terminalLines,
        terminalInput = uiState.terminalInput,
        lastBuildResult = uiState.lastBuildResult,
        realtimeDiagnostics = uiState.realtimeDiagnostics,
        currentProject = uiState.currentProject,
        allProjects = uiState.projects,
        currentFiles = uiState.files,
        gitHubToken = uiState.gitHubToken,
        onTabChange = { viewModel.setBottomTab(it) },
        onFilterChange = { viewModel.setLogcatFilter(it) },
        onClearLogs = { viewModel.clearLogcat() },
        onTerminalInputChange = { viewModel.updateTerminalInput(it) },
        onSendTerminalCommand = { viewModel.sendTerminalCommand(it) },
        onDiagnosticClick = { diag ->
            viewModel.navigateToDiagnostic(diag)
        },
        onAskAiDiagnostic = { diag ->
            viewModel.askAiFixForDiagnostic(diag)
        },
        onAutoHealProject = {
            viewModel.autoHealProjectAndRun()
        },
        onDismiss = { viewModel.toggleLogcat(false) }
    )

    // New Project Dialog
    NewProjectDialog(
        isOpen = uiState.isNewProjectDialogOpen,
        onDismiss = { viewModel.toggleNewProjectDialog(false) },
        onCreateProject = { template, name, pkg, desc, language, sdkConfig ->
            viewModel.createProject(template, name, pkg, desc, language, sdkConfig)
        }
    )

    // Export APK Dialog
    ExportApkDialog(
        isOpen = uiState.isExportDialogOpen,
        project = uiState.currentProject,
        files = uiState.files,
        cachedApkMetadata = uiState.latestApkMetadata,
        onInstallToVirtualDevice = { viewModel.installAppToVirtualDevice() },
        onDismiss = { viewModel.toggleExportDialog(false) }
    )

    // New File / Folder Dialog
    NewFileDialog(
        isOpen = uiState.isNewFileDialogOpen,
        onDismiss = { viewModel.toggleNewFileDialog(false) },
        onCreateFile = { fileName, fileType, isFolder ->
            viewModel.addNewFileOrFolder(fileName, fileType, isFolder)
        }
    )

    // Rename / Move Dialog
    RenameMoveDialog(
        isOpen = uiState.isRenameDialogOpen,
        file = uiState.targetFileForAction,
        onDismiss = { viewModel.renameFile(uiState.targetFileForAction ?: return@RenameMoveDialog, uiState.targetFileForAction!!.name) },
        onRename = { file, newName -> viewModel.renameFile(file, newName) },
        onMove = { file, newParentPath -> viewModel.moveFile(file, newParentPath) }
    )

    // GitHub Import Dialog
    GitHubImportDialog(
        isOpen = uiState.isGitHubImportDialogOpen,
        token = uiState.gitHubToken,
        user = uiState.gitHubUser,
        repos = uiState.gitHubUserRepos,
        isLoading = uiState.isGitHubLoading,
        progress = uiState.gitHubImportProgress,
        errorMessage = uiState.gitHubErrorMessage,
        onDismiss = { viewModel.toggleGitHubImportDialog(false) },
        onSaveToken = { token -> viewModel.saveGitHubToken(token) },
        onClearToken = { viewModel.clearGitHubToken() },
        onRefreshRepos = { viewModel.loadGitHubUserAndRepos() },
        onImportRepo = { owner, repo, branch ->
            viewModel.importGitHubRepository(owner, repo, branch)
        }
    )

    // GitHub Sync & Push/Pull Dialog
    GitHubSyncDialog(
        isOpen = uiState.isGitHubSyncDialogOpen,
        project = uiState.currentProject,
        activeFile = uiState.activeFile,
        isModified = uiState.isModified,
        token = uiState.gitHubToken,
        commitMessage = uiState.gitCommitMessage,
        isPushing = uiState.isGitPushing,
        isPulling = uiState.isGitPulling,
        errorMessage = uiState.gitHubErrorMessage,
        onDismiss = { viewModel.toggleGitHubSyncDialog(false) },
        onCommitMessageChange = { viewModel.setGitCommitMessage(it) },
        onPush = { commitMsg -> viewModel.pushCurrentFileToGitHub(commitMsg) },
        onPull = { viewModel.pullLatestFromGitHub() },
        onConfigureToken = {
            viewModel.toggleGitHubSyncDialog(false)
            viewModel.toggleGitHubImportDialog(true)
        }
    )
}
