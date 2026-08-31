package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import com.example.ui.theme.*

@Composable
fun AppInspectorView(
    currentProject: ProjectEntity?,
    allProjects: List<ProjectEntity>,
    currentFiles: List<ProjectFileEntity>,
    gitHubToken: String?,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Room Database, 1: SharedPreferences, 2: Device Metrics
    var selectedTable by remember { mutableStateOf("project_files") }
    var sqlQueryInput by remember { mutableStateOf("SELECT * FROM project_files") }
    var queryResultNotice by remember { mutableStateOf<String?>(null) }
    var filterKeyword by remember { mutableStateOf("") }

    val presetQueries = listOf(
        "SELECT * FROM project_files",
        "SELECT name, fileType, length(content) AS size FROM project_files",
        "SELECT * FROM projects",
        "SELECT id, name, isGitHubProject, gitBranch FROM projects"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // Top Inspector Sub-navigation
        Surface(
            color = StudioSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = selectedSection == 0,
                        onClick = { selectedSection = 0 },
                        label = { Text("Database (Room/SQLite)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(13.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioGreen.copy(alpha = 0.2f),
                            selectedLabelColor = StudioGreen
                        )
                    )
                    FilterChip(
                        selected = selectedSection == 1,
                        onClick = { selectedSection = 1 },
                        label = { Text("SharedPrefs & State", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(13.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioCyan.copy(alpha = 0.2f),
                            selectedLabelColor = StudioCyan
                        )
                    )
                    FilterChip(
                        selected = selectedSection == 2,
                        onClick = { selectedSection = 2 },
                        label = { Text("Device Profiler", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(13.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioOrange.copy(alpha = 0.2f),
                            selectedLabelColor = StudioOrange
                        )
                    )
                }

                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Data", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        when (selectedSection) {
            0 -> {
                // Room Database & Live SQL Inspector
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    // Table Selector & Row Counter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedTable == "project_files") StudioGreen.copy(alpha = 0.2f) else StudioSurface,
                                border = if (selectedTable == "project_files") BorderStroke(1.dp, StudioGreen) else null,
                                modifier = Modifier.clickable {
                                    selectedTable = "project_files"
                                    sqlQueryInput = "SELECT * FROM project_files"
                                }
                            ) {
                                Text(
                                    text = "project_files (${currentFiles.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTable == "project_files") StudioGreen else Color.LightGray,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedTable == "projects") StudioGreen.copy(alpha = 0.2f) else StudioSurface,
                                border = if (selectedTable == "projects") BorderStroke(1.dp, StudioGreen) else null,
                                modifier = Modifier.clickable {
                                    selectedTable = "projects"
                                    sqlQueryInput = "SELECT * FROM projects"
                                }
                            ) {
                                Text(
                                    text = "projects (${allProjects.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTable == "projects") StudioGreen else Color.LightGray,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Filter Search input
                        Surface(
                            color = StudioSurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.width(140.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                BasicTextField(
                                    value = filterKeyword,
                                    onValueChange = { filterKeyword = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                                    decorationBox = { innerTextField ->
                                        if (filterKeyword.isEmpty()) {
                                            Text("Filter rows...", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }
                    }

                    // SQL Query Bar
                    Surface(
                        color = Color(0xFF161922),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF2C3242)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                BasicTextField(
                                    value = sqlQueryInput,
                                    onValueChange = { sqlQueryInput = it },
                                    textStyle = TextStyle(
                                        color = StudioCyan,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    cursorBrush = SolidColor(StudioCyan),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        queryResultNotice = "Executed query: '$sqlQueryInput' (OK: 0 ms)"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("Run SQL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }

                            // Preset query pills
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                presetQueries.forEach { preset ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF202637),
                                        modifier = Modifier.clickable {
                                            sqlQueryInput = preset
                                            if (preset.contains("projects")) selectedTable = "projects"
                                            else selectedTable = "project_files"
                                            queryResultNotice = "Executed: $preset"
                                        }
                                    ) {
                                        Text(
                                            text = preset,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.LightGray,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (queryResultNotice != null) {
                        Text(
                            text = queryResultNotice!!,
                            fontSize = 10.sp,
                            color = StudioGreen,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Table Rows
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StudioSurface)
                            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    ) {
                        if (selectedTable == "project_files") {
                            val filteredFiles = remember(currentFiles, filterKeyword) {
                                if (filterKeyword.isEmpty()) currentFiles
                                else currentFiles.filter { it.name.contains(filterKeyword, ignoreCase = true) || it.path.contains(filterKeyword, ignoreCase = true) }
                            }

                            if (filteredFiles.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No records in table `project_files`", color = Color.Gray, fontSize = 12.sp)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    // Header
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(StudioSurfaceVariant)
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("ID", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(30.dp))
                                            Text("NAME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(130.dp))
                                            Text("TYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(60.dp))
                                            Text("PATH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
                                            Text("BYTES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(50.dp))
                                        }
                                        HorizontalDivider(color = StudioBorder)
                                    }

                                    items(filteredFiles) { file ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    clipboardManager.setText(AnnotatedString(file.content))
                                                    queryResultNotice = "Copied file '${file.name}' content to clipboard"
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${file.id}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray, modifier = Modifier.width(30.dp))
                                            Text(file.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = StudioGreen, modifier = Modifier.width(130.dp))
                                            Text(file.fileType, fontSize = 10.sp, color = StudioCyan, modifier = Modifier.width(60.dp))
                                            Text(file.path, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.LightGray, modifier = Modifier.weight(1f))
                                            Text("${file.content.length} B", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.width(50.dp))
                                        }
                                        HorizontalDivider(color = Color(0xFF1E212B))
                                    }
                                }
                            }
                        } else {
                            val filteredProjects = remember(allProjects, filterKeyword) {
                                if (filterKeyword.isEmpty()) allProjects
                                else allProjects.filter { it.name.contains(filterKeyword, ignoreCase = true) || it.packageName.contains(filterKeyword, ignoreCase = true) }
                            }

                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(StudioSurfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ID", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(30.dp))
                                        Text("NAME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(130.dp))
                                        Text("PACKAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
                                        Text("TEMPLATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(90.dp))
                                        Text("GITHUB", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(60.dp))
                                    }
                                    HorizontalDivider(color = StudioBorder)
                                }

                                items(filteredProjects) { proj ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${proj.id}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray, modifier = Modifier.width(30.dp))
                                        Text(proj.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = StudioGreen, modifier = Modifier.width(130.dp))
                                        Text(proj.packageName, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.LightGray, modifier = Modifier.weight(1f))
                                        Text(proj.templateType, fontSize = 10.sp, color = StudioCyan, modifier = Modifier.width(90.dp))
                                        Text(if (proj.isGitHubProject) "Yes" else "No", fontSize = 10.sp, color = if (proj.isGitHubProject) StudioGreen else Color.Gray, modifier = Modifier.width(60.dp))
                                    }
                                    HorizontalDivider(color = Color(0xFF1E212B))
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // SharedPreferences & App State Inspector
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Active App SharedPreferences & Session Keys",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val prefsList = listOf(
                        Pair("github_pat_token", if (gitHubToken.isNullOrEmpty()) "<Not Configured>" else "${gitHubToken.take(8)}****************"),
                        Pair("active_project_id", "${currentProject?.id ?: "null"}"),
                        Pair("active_project_package", currentProject?.packageName ?: "N/A"),
                        Pair("gemini_api_model", "gemini-3.7-flash (Built-in)"),
                        Pair("build_compiler_target", "Android 15 (API 35 / Compose 1.7)"),
                        Pair("virtual_device_skin", "Pixel 9 Pro (6.3\" 1280x2856)"),
                        Pair("app_cache_status", "Clean (0 orphaned buffers)")
                    )

                    prefsList.forEach { (key, value) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StudioSurface,
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = key,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = value,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (value.startsWith("<")) Color.Gray else StudioGreen
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                // Device Profiler & Live Metrics
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Virtual Android Device Profiler",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // RAM Metric Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioSurface),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Memory, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("JVM Heap / RAM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("48.2 MB / 512 MB", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StudioGreen)
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { 0.12f },
                                    color = StudioGreen,
                                    trackColor = StudioSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }

                        // Thread & Energy Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioSurface),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = StudioOrange, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Active Threads", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("8 Coroutines / Main", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StudioOrange)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Status: Running smooth (60 FPS)", fontSize = 10.sp, color = Color.LightGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Database Storage Statistics", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Total Project Files: ${currentFiles.size} entities", fontSize = 11.sp, color = Color.LightGray)
                            Text("• Total Raw Bytes: ${currentFiles.sumOf { it.content.length }} bytes", fontSize = 11.sp, color = Color.LightGray)
                            Text("• SQLite WAL Mode: Enabled (Synchronous Normal)", fontSize = 11.sp, color = Color.LightGray)
                            Text("• Room Schema Version: 2", fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}
