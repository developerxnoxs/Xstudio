package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import com.example.ui.components.DiffLine
import com.example.ui.components.DiffLineType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubSyncDialog(
    isOpen: Boolean,
    project: ProjectEntity?,
    activeFile: ProjectFileEntity?,
    currentFiles: List<ProjectFileEntity> = emptyList(),
    editorContent: String = "",
    isModified: Boolean,
    token: String?,
    commitMessage: String,
    isPushing: Boolean,
    isPulling: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCommitMessageChange: (String) -> Unit,
    onPush: (commitMessage: String) -> Unit,
    onPull: () -> Unit,
    onConfigureToken: () -> Unit,
    onRevertToHead: (ProjectFileEntity) -> Unit = {},
    onKeepWorkingCopy: () -> Unit = {}
) {
    if (!isOpen) return

    var selectedTab by remember { mutableStateOf(0) } // 0: Commit & Push, 1: Visual Diff, 2: Pull & Merge, 3: Remote Info
    var selectedDiffFile by remember(activeFile, currentFiles) {
        mutableStateOf(activeFile ?: currentFiles.firstOrNull())
    }
    var diffMode by remember { mutableStateOf("SPLIT") } // "SPLIT" (Side-by-Side) or "UNIFIED" (Inline)
    var compareBranch by remember { mutableStateOf("origin/${project?.githubBranch ?: "main"}") }
    var showBranchDropdown by remember { mutableStateOf(false) }
    var diffNoticeMessage by remember { mutableStateOf<String?>(null) }

    val isGitHubLinked = project?.isGitHubProject == true && !project.githubOwner.isNullOrBlank()

    // Calculate diff lines
    val originalLines = remember(selectedDiffFile) {
        selectedDiffFile?.content?.lines() ?: listOf("// Base committed version from HEAD")
    }
    val modifiedLines = remember(selectedDiffFile, editorContent, activeFile) {
        if (selectedDiffFile?.id == activeFile?.id) {
            editorContent.lines()
        } else {
            selectedDiffFile?.content?.lines() ?: listOf("")
        }
    }

    val diffLines = remember(originalLines, modifiedLines) {
        val list = mutableListOf<DiffLine>()
        val maxLines = maxOf(originalLines.size, modifiedLines.size)
        for (i in 0 until maxLines) {
            val orig = originalLines.getOrNull(i)
            val mod = modifiedLines.getOrNull(i)

            when {
                orig == null && mod != null -> {
                    list.add(DiffLine(null, i + 1, "", mod, DiffLineType.ADDED))
                }
                orig != null && mod == null -> {
                    list.add(DiffLine(i + 1, null, orig, "", DiffLineType.DELETED))
                }
                orig != mod && orig != null && mod != null -> {
                    list.add(DiffLine(i + 1, i + 1, orig, mod, DiffLineType.MODIFIED))
                }
                else -> {
                    list.add(DiffLine(i + 1, i + 1, orig ?: "", mod ?: "", DiffLineType.UNCHANGED))
                }
            }
        }
        list
    }

    val additionsCount = remember(diffLines) { diffLines.count { it.type == DiffLineType.ADDED } }
    val deletionsCount = remember(diffLines) { diffLines.count { it.type == DiffLineType.DELETED } }
    val modifiedCount = remember(diffLines) { diffLines.count { it.type == DiffLineType.MODIFIED } }

    Dialog(
        onDismissRequest = { if (!isPushing && !isPulling) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioSurfaceVariant)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF24292E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Git & GitHub Integration", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Text(
                            text = if (isGitHubLinked) "${project?.githubOwner}/${project?.githubRepo} @ ${project?.githubBranch}" else "Local Project (Not linked to GitHub)",
                            fontSize = 11.sp,
                            color = if (isGitHubLinked) StudioGreen else Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isPushing && !isPulling) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                // Error Message Banner
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = StudioRed.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = StudioRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = errorMessage, color = StudioRed, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (!isGitHubLinked) {
                    // Not a GitHub Project UI
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("This project was created locally", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "To enable Git push, pull, branch merging and visual diffing, import a project directly from your GitHub account or clone an existing repository.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                onConfigureToken()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color(0xFF003919))
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open GitHub Importer")
                        }
                    }
                } else {
                    // Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = StudioSurface,
                        contentColor = StudioGreen
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Commit & Push", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Visual Diff", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                                    if (additionsCount > 0 || deletionsCount > 0 || modifiedCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = StudioGreen.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                "+$additionsCount -$deletionsCount",
                                                fontSize = 8.sp,
                                                color = StudioGreen,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            icon = { Icon(Icons.Default.Difference, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Pull & Merge", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.CallMerge, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Repo Info", fontSize = 11.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // Commit & Push with Visual Diff Preview Summary
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Target file info card
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = StudioSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Description,
                                                contentDescription = null,
                                                tint = StudioGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = activeFile?.name ?: "No file selected",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = activeFile?.path ?: "",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isModified) StudioYellow.copy(alpha = 0.2f) else StudioGreen.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = if (isModified) "Modified (${diffLines.count { it.type != DiffLineType.UNCHANGED }} lines)" else "Clean (No Changes)",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isModified) StudioYellow else StudioGreen,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Quick visual diff summary button
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StudioBackground,
                                        border = BorderStroke(0.5.dp, StudioBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedTab = 1 }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CompareArrows, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Inspect File Diff Before Commit", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("+$additionsCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioGreen)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("-$deletionsCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioRed)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("~$modifiedCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioOrange)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    // Commit message field
                                    OutlinedTextField(
                                        value = commitMessage,
                                        onValueChange = onCommitMessageChange,
                                        label = { Text("Commit Message") },
                                        placeholder = { Text("Update ${activeFile?.name ?: "code"} via Android Studio Mobile") },
                                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = StudioGreen) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = StudioGreen,
                                            unfocusedBorderColor = StudioBorder
                                        ),
                                        maxLines = 3
                                    )

                                    if (token.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = StudioYellow.copy(alpha = 0.15f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Lock, contentDescription = null, tint = StudioYellow, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "GitHub Token required to commit changes to GitHub.",
                                                    fontSize = 11.sp,
                                                    color = StudioYellow,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                TextButton(onClick = onConfigureToken) {
                                                    Text("Set Token", fontSize = 11.sp, color = StudioGreen, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    Button(
                                        onClick = { onPush(commitMessage) },
                                        enabled = activeFile != null && !isPushing && !isPulling && !token.isNullOrBlank(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = StudioGreen,
                                            contentColor = Color(0xFF003919)
                                        )
                                    ) {
                                        if (isPushing) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF003919), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Pushing to GitHub...", fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Commit & Push to ${project?.githubBranch ?: "main"}", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            1 -> {
                                // Visual Diff Viewer Tab
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Diff Toolbar: File selector + Branch selector + View mode toggle
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StudioSurfaceVariant,
                                        border = BorderStroke(0.5.dp, StudioBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                // File Selector
                                                var showFileDropdown by remember { mutableStateOf(false) }
                                                Box {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = StudioBackground,
                                                        border = BorderStroke(0.5.dp, StudioBorder),
                                                        modifier = Modifier.clickable { showFileDropdown = true }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = selectedDiffFile?.name ?: "Select File",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                        }
                                                    }

                                                    DropdownMenu(
                                                        expanded = showFileDropdown,
                                                        onDismissRequest = { showFileDropdown = false }
                                                    ) {
                                                        currentFiles.forEach { file ->
                                                            DropdownMenuItem(
                                                                text = { Text(file.name, fontSize = 12.sp) },
                                                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(16.dp)) },
                                                                onClick = {
                                                                    selectedDiffFile = file
                                                                    showFileDropdown = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                                // Stats badges
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("+$additionsCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioGreen)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("-$deletionsCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioRed)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("~$modifiedCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioOrange)
                                                }

                                                // Split vs Unified View Switcher
                                                Row(
                                                    modifier = Modifier
                                                        .background(StudioBackground, RoundedCornerShape(6.dp))
                                                        .padding(2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (diffMode == "SPLIT") StudioGreen.copy(alpha = 0.2f) else Color.Transparent,
                                                        modifier = Modifier.clickable { diffMode = "SPLIT" }
                                                    ) {
                                                        Text("Split", fontSize = 10.sp, color = if (diffMode == "SPLIT") StudioGreen else Color.Gray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (diffMode == "UNIFIED") StudioGreen.copy(alpha = 0.2f) else Color.Transparent,
                                                        modifier = Modifier.clickable { diffMode = "UNIFIED" }
                                                    ) {
                                                        Text("Unified", fontSize = 10.sp, color = if (diffMode == "UNIFIED") StudioGreen else Color.Gray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }

                                            if (diffNoticeMessage != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(diffNoticeMessage!!, fontSize = 10.sp, color = StudioGreen)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Diff View Canvas
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0E1117),
                                        border = BorderStroke(0.5.dp, StudioBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            // Diff Column Header
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(StudioSurfaceVariant)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                if (diffMode == "SPLIT") {
                                                    Text("HEAD / Remote Base", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
                                                    Text("Working Copy (Local Edits)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioGreen, modifier = Modifier.weight(1f))
                                                } else {
                                                    Text("Unified Diff (Line-by-Line)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioCyan, modifier = Modifier.fillMaxWidth())
                                                }
                                            }

                                            HorizontalDivider(color = StudioBorder)

                                            if (diffLines.isEmpty() || diffLines.all { it.type == DiffLineType.UNCHANGED }) {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(32.dp))
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text("No Differences Detected", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                        Text("Working copy matches HEAD perfectly.", fontSize = 10.sp, color = Color.Gray)
                                                    }
                                                }
                                            } else {
                                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                    itemsIndexed(diffLines) { idx, line ->
                                                        val bg = when (line.type) {
                                                            DiffLineType.ADDED -> StudioGreen.copy(alpha = 0.15f)
                                                            DiffLineType.DELETED -> StudioRed.copy(alpha = 0.15f)
                                                            DiffLineType.MODIFIED -> StudioOrange.copy(alpha = 0.15f)
                                                            DiffLineType.UNCHANGED -> Color.Transparent
                                                        }

                                                        if (diffMode == "SPLIT") {
                                                            // Split (Side-by-side)
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(bg)
                                                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                                            ) {
                                                                // Left side (Base)
                                                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(
                                                                        text = line.lineNumberOriginal?.toString() ?: " ",
                                                                        fontSize = 9.sp,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        color = Color.Gray,
                                                                        modifier = Modifier.width(22.dp)
                                                                    )
                                                                    Text(
                                                                        text = line.originalText,
                                                                        fontSize = 10.sp,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        color = if (line.type == DiffLineType.DELETED) StudioRed else Color.LightGray,
                                                                        maxLines = 1
                                                                    )
                                                                }

                                                                // Right side (Modified)
                                                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(
                                                                        text = line.lineNumberModified?.toString() ?: " ",
                                                                        fontSize = 9.sp,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        color = Color.Gray,
                                                                        modifier = Modifier.width(22.dp)
                                                                    )
                                                                    Text(
                                                                        text = line.modifiedText,
                                                                        fontSize = 10.sp,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        color = if (line.type == DiffLineType.ADDED) StudioGreen else if (line.type == DiffLineType.MODIFIED) StudioOrange else Color.White,
                                                                        maxLines = 1
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            // Unified (Inline)
                                                            val prefix = when (line.type) {
                                                                DiffLineType.ADDED -> "+"
                                                                DiffLineType.DELETED -> "-"
                                                                DiffLineType.MODIFIED -> "~"
                                                                DiffLineType.UNCHANGED -> " "
                                                            }
                                                            val text = if (line.type == DiffLineType.DELETED) line.originalText else line.modifiedText
                                                            val textColor = when (line.type) {
                                                                DiffLineType.ADDED -> StudioGreen
                                                                DiffLineType.DELETED -> StudioRed
                                                                DiffLineType.MODIFIED -> StudioOrange
                                                                DiffLineType.UNCHANGED -> Color.LightGray
                                                            }
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(bg)
                                                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "$prefix ${line.lineNumberModified ?: line.lineNumberOriginal ?: " "}",
                                                                    fontSize = 9.sp,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    color = textColor,
                                                                    modifier = Modifier.width(36.dp)
                                                                )
                                                                Text(
                                                                    text = text,
                                                                    fontSize = 10.sp,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    color = textColor,
                                                                    maxLines = 1
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Action bar in Diff tab
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedDiffFile?.let { onRevertToHead(it) }
                                                diffNoticeMessage = "Reverted to HEAD committed version"
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioRed),
                                            border = BorderStroke(0.5.dp, StudioRed)
                                        ) {
                                            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Revert to HEAD", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                onKeepWorkingCopy()
                                                selectedTab = 0 // Go to commit tab
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color(0xFF003919))
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Stage & Commit Changes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // Pull, Merge & Branch Diff
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = StudioSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CallMerge, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Branch Comparison & Merge Sync", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "Compare incoming branch changes against local workspace before pulling or merging.",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    // Branch comparator selector
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StudioBackground,
                                        border = BorderStroke(0.5.dp, StudioBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Compare Target Branch:", fontSize = 11.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.AccountTree, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "origin/${project?.githubBranch ?: "main"}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }

                                                Button(
                                                    onClick = { selectedTab = 1 },
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan.copy(alpha = 0.2f)),
                                                    border = BorderStroke(0.5.dp, StudioCyan),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Difference, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("View Merge Diff", fontSize = 10.sp, color = StudioCyan)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    Button(
                                        onClick = onPull,
                                        enabled = !isPushing && !isPulling,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = StudioGreen,
                                            contentColor = Color(0xFF003919)
                                        )
                                    ) {
                                        if (isPulling) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF003919), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Merging & Pulling Changes...", fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Pull & Merge Latest from Remote", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            3 -> {
                                // Remote Info
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    InfoRow(label = "Repository Owner", value = project?.githubOwner ?: "-")
                                    InfoRow(label = "Repository Name", value = project?.githubRepo ?: "-")
                                    InfoRow(label = "Tracking Branch", value = project?.githubBranch ?: "main")
                                    InfoRow(label = "Remote URL", value = project?.githubUrl ?: "https://github.com/${project?.githubOwner}/${project?.githubRepo}")
                                    if (!project?.lastSyncedSha.isNullOrBlank()) {
                                        InfoRow(label = "Last Synced SHA", value = project?.lastSyncedSha?.take(10) ?: "-")
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

@Composable
private fun InfoRow(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StudioSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
        }
    }
}
