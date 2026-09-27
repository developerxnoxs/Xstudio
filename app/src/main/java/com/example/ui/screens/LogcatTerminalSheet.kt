package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.builder.BuildResult
import com.example.core.BuildDiagnostic
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import com.example.ui.components.AppInspectorView
import com.example.ui.components.DependencyItem
import com.example.ui.components.DependencyUpgradeView
import com.example.ui.components.GitVisualDiffView
import com.example.ui.components.NetworkInspectorView
import com.example.ui.components.PerformanceProfilerView
import com.example.ui.components.VectorAssetStudioView
import com.example.ui.components.ApkAnalyzerView
import com.example.ui.components.StringLocalizationView
import com.example.ui.components.MavenDependencyCatalogView
import com.example.ui.components.MavenLibraryDef
import com.example.ui.theme.*
import com.example.ui.viewmodel.LogcatEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatTerminalSheet(
    isOpen: Boolean,
    selectedTab: Int,
    filter: String,
    logs: List<LogcatEntry>,
    terminalLines: List<String>,
    terminalInput: String,
    lastBuildResult: BuildResult?,
    realtimeDiagnostics: List<BuildDiagnostic> = emptyList(),
    currentProject: ProjectEntity? = null,
    allProjects: List<ProjectEntity> = emptyList(),
    currentFiles: List<ProjectFileEntity> = emptyList(),
    gitHubToken: String? = null,
    activeFile: ProjectFileEntity? = null,
    editorContent: String = "",
    apkReport: com.example.analyzer.ApkAnalysisReport? = null,
    isAnalyzingApk: Boolean = false,
    onTabChange: (Int) -> Unit,
    onFilterChange: (String) -> Unit,
    onClearLogs: () -> Unit,
    onTerminalInputChange: (String) -> Unit,
    onSendTerminalCommand: (String) -> Unit,
    onDiagnosticClick: (BuildDiagnostic) -> Unit,
    onAskAiDiagnostic: (BuildDiagnostic) -> Unit = {},
    onAutoHealProject: () -> Unit = {},
    onSaveVectorAsset: (String, String) -> Unit = { _, _ -> },
    onApplyDependencyUpgrade: (DependencyItem) -> Unit = {},
    onUpgradeAllDependencies: () -> Unit = {},
    onTriggerGc: () -> Unit = {},
    onAcceptAllDiffIncoming: () -> Unit = {},
    onAcceptAllDiffCurrent: () -> Unit = {},
    onLogNetworkEvent: (String, String, String) -> Unit = { _, _, _ -> },
    onRefreshApkAnalysis: () -> Unit = {},
    onSaveStringTranslations: (String, String, String) -> Unit = { _, _, _ -> },
    onAddMavenLibrary: (MavenLibraryDef) -> Unit = {},
    onCaptureDeviceLogs: () -> Unit = {},
    onExportLogs: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var diagnosticCategoryFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, filter) {
        if (filter == "ALL") logs else logs.filter { it.level == filter }
    }

    val diagnosticsList = remember(lastBuildResult, realtimeDiagnostics) {
        val buildDiags = lastBuildResult?.diagnostics ?: emptyList()
        if (buildDiags.isNotEmpty()) {
            buildDiags
        } else {
            realtimeDiagnostics
        }
    }
    val filteredDiagnostics = remember(diagnosticsList, diagnosticCategoryFilter) {
        when (diagnosticCategoryFilter) {
            "ERRORS" -> diagnosticsList.filter { !it.isWarning }
            "WARNINGS" -> diagnosticsList.filter { it.isWarning }
            else -> diagnosticsList
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .padding(horizontal = 16.dp)
        ) {
            // Header Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    edgePadding = 0.dp,
                    divider = {},
                    modifier = Modifier.weight(1f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { onTabChange(0) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (lastBuildResult != null && !lastBuildResult.isSuccess) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = StudioRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("Build Output", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { onTabChange(1) },
                        text = { Text("Logcat (${logs.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { onTabChange(2) },
                        text = { Text("Terminal", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { onTabChange(3) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("App Inspection", fontSize = 12.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { onTabChange(4) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Network", fontSize = 12.sp, fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 5,
                        onClick = { onTabChange(5) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = StudioOrange, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Profiler", fontSize = 12.sp, fontWeight = if (selectedTab == 5) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 6,
                        onClick = { onTabChange(6) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, tint = StudioPurple, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dependencies", fontSize = 12.sp, fontWeight = if (selectedTab == 6) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 7,
                        onClick = { onTabChange(7) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Vector Studio", fontSize = 12.sp, fontWeight = if (selectedTab == 7) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 8,
                        onClick = { onTabChange(8) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Difference, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Git Diff", fontSize = 12.sp, fontWeight = if (selectedTab == 8) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 9,
                        onClick = { onTabChange(9) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = StudioOrange, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("APK Analyzer", fontSize = 12.sp, fontWeight = if (selectedTab == 9) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 10,
                        onClick = { onTabChange(10) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Translate, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Localization", fontSize = 12.sp, fontWeight = if (selectedTab == 10) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 11,
                        onClick = { onTabChange(11) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Maven Library", fontSize = 12.sp, fontWeight = if (selectedTab == 11) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedTab == 1) {
                        IconButton(onClick = onClearLogs) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
            }

            HorizontalDivider(color = StudioBorder)

            when (selectedTab) {
                0 -> {
                    // Build Output & Diagnostics
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // Diagnostic Category Filter chips
                        if (diagnosticsList.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = diagnosticCategoryFilter == "ALL",
                                    onClick = { diagnosticCategoryFilter = "ALL" },
                                    label = { Text("All (${diagnosticsList.size})", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StudioCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = StudioCyan
                                    )
                                )
                                FilterChip(
                                    selected = diagnosticCategoryFilter == "ERRORS",
                                    onClick = { diagnosticCategoryFilter = "ERRORS" },
                                    label = { Text("Errors (${diagnosticsList.count { !it.isWarning }})", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StudioRed.copy(alpha = 0.2f),
                                        selectedLabelColor = StudioRed
                                    )
                                )
                                FilterChip(
                                    selected = diagnosticCategoryFilter == "WARNINGS",
                                    onClick = { diagnosticCategoryFilter = "WARNINGS" },
                                    label = { Text("Warnings (${diagnosticsList.count { it.isWarning }})", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFD54F).copy(alpha = 0.2f),
                                        selectedLabelColor = Color(0xFFFFD54F)
                                    )
                                )
                            }
                        }

                        Surface(
                            color = StudioBackground,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (lastBuildResult == null) {
                                    item {
                                        Text(
                                            "No recent build executed. Press the Run / Build button in the top bar or run 'gradle assembleDebug' in Terminal.",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                } else {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (lastBuildResult.isSuccess) StudioSurfaceVariant else StudioRed.copy(alpha = 0.15f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    if (lastBuildResult.isSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                    contentDescription = null,
                                                    tint = if (lastBuildResult.isSuccess) StudioGreen else StudioRed,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        if (lastBuildResult.isSuccess) "BUILD SUCCESSFUL (${lastBuildResult.durationMs}ms)" else "BUILD FAILED (${lastBuildResult.durationMs}ms)",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (lastBuildResult.isSuccess) StudioGreen else StudioRed
                                                    )
                                                    if (lastBuildResult.isSuccess && lastBuildResult.apkMetadata != null) {
                                                        Text(
                                                            "Output: ${lastBuildResult.apkMetadata.fileSizeFormatted} APK generated at ${lastBuildResult.apkMetadata.apkFilePath}",
                                                            fontSize = 10.sp,
                                                            color = Color.LightGray
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (filteredDiagnostics.isNotEmpty()) {
                                        item {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Parsed Diagnostics & Line Mapping:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StudioRed)
                                                Button(
                                                    onClick = onAutoHealProject,
                                                    colors = ButtonDefaults.buttonColors(containerColor = StudioOrange, contentColor = Color.Black),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(11.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Auto-Heal Project", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        items(filteredDiagnostics, key = { it.id }) { diag ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = StudioSurfaceVariant,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(diag.errorType.badgeColorHex).copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                text = diag.errorType.label,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(diag.errorType.badgeColorHex),
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.width(6.dp))

                                                        Text(
                                                            text = "${diag.fileName}:${diag.line}:${diag.column}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = StudioGreen,
                                                            fontFamily = FontFamily.Monospace,
                                                            modifier = Modifier.weight(1f)
                                                        )

                                                        TextButton(
                                                            onClick = { onDiagnosticClick(diag) },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            colors = ButtonDefaults.textButtonColors(contentColor = StudioCyan)
                                                        ) {
                                                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Jump to Line ${diag.line}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }

                                                    Text(
                                                        text = diag.message,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFFFF8A80),
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    )

                                                    if (diag.codeSnippet != null) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFF101014),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = diag.codeSnippet,
                                                                fontSize = 10.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = Color(0xFFE0E0E0),
                                                                modifier = Modifier.padding(6.dp)
                                                            )
                                                        }
                                                    }

                                                    if (diag.suggestion != null) {
                                                        Row(
                                                            modifier = Modifier.padding(top = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(12.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = diag.suggestion,
                                                                fontSize = 10.sp,
                                                                color = Color(0xFFA5D6A7),
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            IconButton(
                                                                onClick = { onAskAiDiagnostic(diag) },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(Icons.Default.AutoAwesome, contentDescription = "Ask AI", tint = StudioCyan, modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Console Execution Logs:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StudioCyan)
                                    }

                                    items(lastBuildResult.logs) { line ->
                                        Text(
                                            text = line,
                                            color = if (line.startsWith("e:") || line.startsWith("FAILURE")) StudioRed
                                            else if (line.startsWith("w:")) Color(0xFFFFD54F)
                                            else if (line.startsWith("> Task")) StudioGreen
                                            else if (line.startsWith("BUILD SUCCESSFUL")) StudioCyan
                                            else Color(0xFFABB2BF),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Enhanced Logcat Stream with Filter, Tag, Capture & Export
                    var tagFilter by remember { mutableStateOf("") }
                    var searchLogQuery by remember { mutableStateOf("") }

                    val activeFilteredLogs = remember(filteredLogs, tagFilter, searchLogQuery) {
                        filteredLogs.filter { log ->
                            val matchTag = tagFilter.isBlank() || log.tag.contains(tagFilter, ignoreCase = true)
                            val matchQuery = searchLogQuery.isBlank() || log.message.contains(searchLogQuery, ignoreCase = true)
                            matchTag && matchQuery
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(listOf("ALL", "V", "D", "I", "W", "E")) { level ->
                                    FilterChip(
                                        selected = filter == level,
                                        onClick = { onFilterChange(level) },
                                        label = { Text(if (level == "ALL") "All" else level, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = when (level) {
                                                "E" -> StudioRed.copy(alpha = 0.2f)
                                                "W" -> StudioOrange.copy(alpha = 0.2f)
                                                "I" -> StudioGreen.copy(alpha = 0.2f)
                                                "D" -> StudioCyan.copy(alpha = 0.2f)
                                                else -> StudioSurfaceVariant
                                            },
                                            selectedLabelColor = when (level) {
                                                "E" -> StudioRed
                                                "W" -> StudioOrange
                                                "I" -> StudioGreen
                                                "D" -> StudioCyan
                                                else -> Color.White
                                            }
                                        )
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalButton(
                                    onClick = onCaptureDeviceLogs,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = StudioGreen.copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Capture", color = StudioGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                FilledTonalButton(
                                    onClick = onExportLogs,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = StudioCyan.copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export", color = StudioCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Tag & Search Inputs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = tagFilter,
                                onValueChange = { tagFilter = it },
                                placeholder = { Text("Filter Tag...", fontSize = 10.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StudioGreen,
                                    unfocusedBorderColor = StudioBorder
                                )
                            )
                            OutlinedTextField(
                                value = searchLogQuery,
                                onValueChange = { searchLogQuery = it },
                                placeholder = { Text("Cari pesan log...", fontSize = 10.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.weight(1.5f),
                                textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StudioCyan,
                                    unfocusedBorderColor = StudioBorder
                                )
                            )
                        }

                        Surface(
                            color = StudioBackground,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (activeFilteredLogs.isEmpty()) {
                                    item {
                                        Text(
                                            if (logs.isEmpty()) "Belum ada log. Klik 'Capture' atau jalankan aplikasi." else "Tidak ada pesan yang cocok dengan filter.",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                items(activeFilteredLogs, key = { it.id }) { log ->
                                    val color = when (log.level) {
                                        "E" -> StudioRed
                                        "W" -> StudioOrange
                                        "I" -> StudioGreen
                                        "D" -> StudioBlue
                                        else -> Color(0xFFABB2BF)
                                    }
                                    Text(
                                        text = "${log.timestamp}  ${log.level}/${log.tag}: ${log.message}",
                                        color = color,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Terminal Console
                    val terminalListState = rememberLazyListState()
                    LaunchedEffect(terminalLines.size) {
                        if (terminalLines.isNotEmpty()) {
                            terminalListState.animateScrollToItem(terminalLines.size - 1)
                        }
                    }

                    val quickCommands = listOf("gradle build", "gradle test", "ls", "adb logcat", "git status", "clear", "help")

                    Surface(
                        color = StudioBackground,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Output Window
                            LazyColumn(
                                state = terminalListState,
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(terminalLines) { line ->
                                    Text(
                                        text = line,
                                        color = if (line.startsWith("$")) StudioGreen else Color(0xFFABB2BF),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Quick command chips
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(quickCommands) { cmd ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = StudioSurfaceVariant,
                                        modifier = Modifier.clickable {
                                            onSendTerminalCommand(cmd)
                                        }
                                    ) {
                                        Text(
                                            text = cmd,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = StudioCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = StudioBorder, modifier = Modifier.padding(vertical = 4.dp))

                            // Interactive Command Line with Enter Key Handler
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$ ", color = StudioGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                BasicTextField(
                                    value = terminalInput,
                                    onValueChange = { newText ->
                                        if (newText.contains('\n')) {
                                            val cleanCmd = newText.replace("\n", "").trim()
                                            if (cleanCmd.isNotBlank()) {
                                                onSendTerminalCommand(cleanCmd)
                                            }
                                        } else {
                                            onTerminalInputChange(newText)
                                        }
                                    },
                                    singleLine = true,
                                    maxLines = 1,
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done,
                                        keyboardType = KeyboardType.Ascii,
                                        autoCorrect = false
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (terminalInput.isNotBlank()) {
                                                onSendTerminalCommand(terminalInput.trim())
                                            }
                                        }
                                    ),
                                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                    cursorBrush = SolidColor(StudioGreen),
                                    modifier = Modifier
                                        .weight(1f)
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyUp) {
                                                if (terminalInput.isNotBlank()) {
                                                    onSendTerminalCommand(terminalInput.trim())
                                                    true
                                                } else false
                                            } else false
                                        }
                                )
                                IconButton(
                                    onClick = {
                                        if (terminalInput.isNotBlank()) {
                                            onSendTerminalCommand(terminalInput.trim())
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardReturn, contentDescription = "Execute", tint = StudioGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // App Inspection & Database Inspector
                    AppInspectorView(
                        currentProject = currentProject,
                        allProjects = allProjects,
                        currentFiles = currentFiles,
                        gitHubToken = gitHubToken,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                4 -> {
                    // Network & API Traffic Inspector
                    NetworkInspectorView(
                        onLogEvent = onLogNetworkEvent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                5 -> {
                    // Runtime Profiler (CPU, RAM, FPS, GC)
                    PerformanceProfilerView(
                        onTriggerGc = onTriggerGc,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                6 -> {
                    // Dependency & Version Catalog Assistant
                    DependencyUpgradeView(
                        files = currentFiles,
                        onApplyUpgrade = onApplyDependencyUpgrade,
                        onUpgradeAll = onUpgradeAllDependencies,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                7 -> {
                    // Vector Asset Studio & SVG-to-XML
                    VectorAssetStudioView(
                        onSaveToDrawable = onSaveVectorAsset,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                8 -> {
                    // Git Visual Diff & Merge Tool
                    GitVisualDiffView(
                        activeFile = activeFile,
                        currentEditorContent = editorContent,
                        onAcceptAllIncoming = onAcceptAllDiffIncoming,
                        onAcceptAllCurrent = onAcceptAllDiffCurrent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                9 -> {
                    // 1. APK Analyzer & Package Inspector
                    ApkAnalyzerView(
                        report = apkReport,
                        isAnalyzing = isAnalyzingApk,
                        onRefreshAnalysis = onRefreshApkAnalysis,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                10 -> {
                    // 3. String & Localization Manager
                    StringLocalizationView(
                        files = currentFiles,
                        onSaveTranslations = onSaveStringTranslations,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                11 -> {
                    // 4. Maven Library Catalog & Adder
                    MavenDependencyCatalogView(
                        files = currentFiles,
                        onAddDependencyToGradle = onAddMavenLibrary,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
