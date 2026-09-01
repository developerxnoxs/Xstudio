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
                Surface(
                    color = StudioSurface,
                    tonalElevation = 4.dp,
                    border = BorderStroke(0.5.dp, StudioBorder)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Drawer button and Project name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Buka File Explorer", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = uiState.currentProject?.name ?: "Android Studio",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                        if (uiState.currentProject?.isGitHubProject == true) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = StudioGreen.copy(alpha = 0.15f),
                                                border = BorderStroke(0.5.dp, StudioGreen.copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    "Git",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StudioGreen,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = uiState.activeFile?.name ?: "No file open",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = StudioGreen,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Right Action Dock
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // View Mode Segmented Pill
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StudioBackground,
                                    border = BorderStroke(0.5.dp, StudioBorder)
                                ) {
                                    Row(modifier = Modifier.padding(2.dp)) {
                                        IconButton(
                                            onClick = { viewModel.setViewMode(StudioViewMode.CODE_ONLY) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    if (uiState.viewMode == StudioViewMode.CODE_ONLY) StudioSurfaceVariant else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                        ) {
                                            Icon(
                                                Icons.Default.Code,
                                                contentDescription = "Code",
                                                tint = if (uiState.viewMode == StudioViewMode.CODE_ONLY) StudioGreen else Color.Gray,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.setViewMode(StudioViewMode.SPLIT_VIEW) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    if (uiState.viewMode == StudioViewMode.SPLIT_VIEW) StudioSurfaceVariant else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                        ) {
                                            Icon(
                                                Icons.Default.VerticalSplit,
                                                contentDescription = "Split",
                                                tint = if (uiState.viewMode == StudioViewMode.SPLIT_VIEW) StudioGreen else Color.Gray,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.setViewMode(StudioViewMode.DESIGN_ONLY) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    if (uiState.viewMode == StudioViewMode.DESIGN_ONLY) StudioSurfaceVariant else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                        ) {
                                            Icon(
                                                Icons.Default.Palette,
                                                contentDescription = "Design",
                                                tint = if (uiState.viewMode == StudioViewMode.DESIGN_ONLY) StudioGreen else Color.Gray,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                // Quick Run Button with High Contrast
                                FilledIconButton(
                                    onClick = { viewModel.runBuildAndRun() },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (uiState.isBuilding) StudioSurfaceVariant else StudioGreen,
                                        contentColor = if (uiState.isBuilding) Color.White else Color(0xFF003919)
                                    ),
                                    modifier = Modifier.size(32.dp),
                                    enabled = !uiState.isBuilding,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (uiState.isBuilding) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = StudioGreen, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Run App", modifier = Modifier.size(18.dp))
                                    }
                                }

                                // Autonomous Agent 99% Quick Pill
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (uiState.autonomousAgent.isActive) StudioGreen.copy(alpha = 0.2f) else StudioSurfaceVariant,
                                    border = BorderStroke(0.8.dp, if (uiState.autonomousAgent.isActive) StudioGreen else StudioBorder),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
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
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (uiState.autonomousAgent.isActive) "Running..." else "AI Bot",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.autonomousAgent.isActive) StudioGreen else Color.White
                                        )
                                    }
                                }

                                // Overflow Menu
                                Box {
                                    IconButton(
                                        onClick = { showOverflowMenu = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White, modifier = Modifier.size(18.dp))
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
                                            text = { Text("Network & Traffic Inspector") },
                                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = StudioGreen) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.setBottomTab(4)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Performance Profiler (CPU/RAM)") },
                                            leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = StudioOrange) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.setBottomTab(5)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Dependency Upgrade Assistant") },
                                            leadingIcon = { Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, tint = StudioPurple) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.setBottomTab(6)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Vector Asset Studio & SVG") },
                                            leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, tint = StudioCyan) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.setBottomTab(7)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Git Visual Diff & Conflicts") },
                                            leadingIcon = { Icon(Icons.Default.Difference, contentDescription = null, tint = StudioGreen) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.setBottomTab(8)
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
                                            text = { Text("Pixel 9 Pro Emulator") },
                                            leadingIcon = { Icon(Icons.Default.Smartphone, contentDescription = null, tint = StudioGreen) },
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
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // Bottom Status Bar
                Surface(
                    color = StudioSurface,
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 6.dp,
                    border = BorderStroke(0.5.dp, StudioBorder)
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        if (uiState.isBuilding) {
                            LinearProgressIndicator(
                                progress = { uiState.buildProgress },
                                modifier = Modifier.fillMaxWidth().height(3.dp),
                                color = StudioGreen,
                                trackColor = StudioSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Gradle & Terminal trigger
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { viewModel.toggleLogcat(true) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = if (uiState.isBuilding) StudioOrange else StudioGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isBuilding) uiState.currentBuildTask else "Gradle: Ready",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (uiState.isBuilding) StudioOrange else Color.LightGray
                                )
                            }

                            // Middle: Diagnostics status pills
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val errorCount = uiState.realtimeDiagnostics.count { it.errorType.name.contains("ERROR") }
                                val warnCount = uiState.realtimeDiagnostics.size - errorCount

                                if (errorCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = StudioRed.copy(alpha = 0.2f),
                                        modifier = Modifier.clickable { viewModel.setBottomTab(0) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Cancel, contentDescription = null, tint = StudioRed, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("$errorCount", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StudioRed)
                                        }
                                    }
                                }

                                if (warnCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = StudioOrange.copy(alpha = 0.2f),
                                        modifier = Modifier.clickable { viewModel.setBottomTab(0) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = StudioOrange, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("$warnCount", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StudioOrange)
                                        }
                                    }
                                }

                                Text("UTF-8", fontSize = 9.sp, color = Color.Gray)
                                Text("Kotlin 2.2", fontSize = 9.sp, color = StudioPurple, fontWeight = FontWeight.Bold)
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
                                    onDuplicateNode = { viewModel.duplicateVisualNode(it) },
                                    onReorderNode = { from, to -> viewModel.reorderVisualNode(from, to) },
                                    designerMode = uiState.visualDesignerMode,
                                    onToggleDesignerMode = { viewModel.setVisualDesignerMode(it) },
                                    isRealtimeSyncEnabled = uiState.isRealtimeVisualSyncEnabled,
                                    onToggleRealtimeSync = { viewModel.toggleRealtimeVisualSync(it) },
                                    onSaveXmlLayout = { viewModel.saveVisualLayoutAsXmlFile(it) },
                                    onLoadFromActiveXml = { viewModel.loadFromActiveXmlFile() },
                                    onSyncCode = { viewModel.syncVisualToCode() },
                                    activeFileName = uiState.activeFile?.name
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
                                            onDuplicateNode = { viewModel.duplicateVisualNode(it) },
                                            onReorderNode = { from, to -> viewModel.reorderVisualNode(from, to) },
                                            designerMode = uiState.visualDesignerMode,
                                            onToggleDesignerMode = { viewModel.setVisualDesignerMode(it) },
                                            isRealtimeSyncEnabled = uiState.isRealtimeVisualSyncEnabled,
                                            onToggleRealtimeSync = { viewModel.toggleRealtimeVisualSync(it) },
                                            onSaveXmlLayout = { viewModel.saveVisualLayoutAsXmlFile(it) },
                                            onLoadFromActiveXml = { viewModel.loadFromActiveXmlFile() },
                                            onSyncCode = { viewModel.syncVisualToCode() },
                                            activeFileName = uiState.activeFile?.name
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

    // Logcat & Developer Tools Sheet
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
        activeFile = uiState.activeFile,
        editorContent = uiState.editorContent,
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
        onSaveVectorAsset = { name, xml ->
            viewModel.saveVectorDrawable(name, xml)
        },
        onApplyDependencyUpgrade = { dep ->
            viewModel.applyDependencyUpgrade(dep.artifact, dep.latestVersion)
        },
        onUpgradeAllDependencies = {
            viewModel.applyDependencyUpgrade("libs.versions.toml", "latest")
        },
        onTriggerGc = {
            viewModel.triggerGarbageCollection()
        },
        onAcceptAllDiffIncoming = {
            uiState.activeFile?.let { viewModel.replaceEditorWithCode(it.content) }
        },
        onAcceptAllDiffCurrent = {
            viewModel.saveActiveFile()
        },
        onLogNetworkEvent = { tag, level, msg ->
            viewModel.addLogcat(tag, level, msg)
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
        currentFiles = uiState.files,
        editorContent = uiState.editorContent,
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
        },
        onRevertToHead = { file ->
            viewModel.replaceEditorWithCode(file.content)
        },
        onKeepWorkingCopy = {
            viewModel.saveActiveFile()
        }
    )
}
