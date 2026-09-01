package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectEntity
import com.example.data.repository.ProjectTemplate
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsDashboardScreen(
    projects: List<ProjectEntity>,
    onSelectProject: (ProjectEntity) -> Unit,
    onNewProjectClick: () -> Unit,
    onOpenGitHubImport: () -> Unit,
    onCreateFromTemplate: (ProjectTemplate) -> Unit,
    onDeleteProject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "LOCAL", "GITHUB"

    val filteredProjects = remember(projects, searchQuery, selectedFilter) {
        projects.filter { proj ->
            val matchesQuery = searchQuery.isBlank() ||
                    proj.name.contains(searchQuery, ignoreCase = true) ||
                    proj.packageName.contains(searchQuery, ignoreCase = true) ||
                    (proj.githubRepo?.contains(searchQuery, ignoreCase = true) == true)
            val matchesFilter = when (selectedFilter) {
                "LOCAL" -> !proj.isGitHubProject
                "GITHUB" -> proj.isGitHubProject
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    val gitHubCount = remember(projects) { projects.count { it.isGitHubProject } }

    Scaffold(
        topBar = {
            Surface(
                color = StudioSurface,
                tonalElevation = 6.dp,
                border = BorderStroke(0.5.dp, StudioBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(StudioGreen, Color(0xFF00BFA5))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Android,
                                contentDescription = null,
                                tint = Color(0xFF002914),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Android Studio",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StudioGreen.copy(alpha = 0.15f),
                                    border = BorderStroke(0.5.dp, StudioGreen.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        "v2026.1",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioGreen,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                "Mobile Workspace · Kotlin 2.2 · Compose M3",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onOpenGitHubImport,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, StudioBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = StudioGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GitHub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onNewProjectClick,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioGreen,
                                contentColor = Color(0xFF003919)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = StudioBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Modern Hero Banner with Gradient & Quick Stats
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF22252A),
                    border = BorderStroke(1.dp, Brush.linearGradient(listOf(StudioBorder, StudioGreen.copy(alpha = 0.3f)))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Native Jetpack Compose Engine",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Build, test with Pixel 9 Pro Emulator, or sync Git repos seamlessly.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFB0BEC5),
                                        lineHeight = 16.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(StudioGreen.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Code,
                                        contentDescription = null,
                                        tint = StudioGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick Stats Metric Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StudioSurface,
                                    border = BorderStroke(0.5.dp, StudioBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("${projects.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("Projects", fontSize = 9.sp, color = Color.Gray)
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StudioSurface,
                                    border = BorderStroke(0.5.dp, StudioBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("$gitHubCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("GitHub Synced", fontSize = 9.sp, color = Color.Gray)
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StudioSurface,
                                    border = BorderStroke(0.5.dp, StudioBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = StudioPurple, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("AI Agent", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("Gemini 2.5", fontSize = 9.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Starter Templates Section with Polished Layout
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Quick Starter Templates",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            "${ProjectTemplate.values().size} Presets",
                            fontSize = 11.sp,
                            color = StudioCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(ProjectTemplate.values()) { template ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = StudioSurface,
                                border = BorderStroke(0.8.dp, StudioBorder),
                                modifier = Modifier
                                    .width(190.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onCreateFromTemplate(template) }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when (template) {
                                                        ProjectTemplate.COMPOSE_M3_APP -> StudioGreen.copy(alpha = 0.15f)
                                                        ProjectTemplate.AI_GEMINI_CHAT -> StudioPurple.copy(alpha = 0.15f)
                                                        ProjectTemplate.CANVAS_GAME -> StudioOrange.copy(alpha = 0.15f)
                                                        ProjectTemplate.FINANCE_CONVERTER -> StudioCyan.copy(alpha = 0.15f)
                                                        ProjectTemplate.STOREFRONT_M3 -> Color(0xFFE91E63).copy(alpha = 0.15f)
                                                        ProjectTemplate.EMPTY_COMPOSE -> Color.Gray.copy(alpha = 0.15f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                when (template) {
                                                    ProjectTemplate.COMPOSE_M3_APP -> Icons.Default.Dashboard
                                                    ProjectTemplate.AI_GEMINI_CHAT -> Icons.Default.SmartToy
                                                    ProjectTemplate.CANVAS_GAME -> Icons.Default.SportsEsports
                                                    ProjectTemplate.FINANCE_CONVERTER -> Icons.Default.CurrencyExchange
                                                    ProjectTemplate.STOREFRONT_M3 -> Icons.Default.ShoppingBag
                                                    ProjectTemplate.EMPTY_COMPOSE -> Icons.Default.Code
                                                },
                                                contentDescription = null,
                                                tint = when (template) {
                                                    ProjectTemplate.COMPOSE_M3_APP -> StudioGreen
                                                    ProjectTemplate.AI_GEMINI_CHAT -> StudioPurple
                                                    ProjectTemplate.CANVAS_GAME -> StudioOrange
                                                    ProjectTemplate.FINANCE_CONVERTER -> StudioCyan
                                                    ProjectTemplate.STOREFRONT_M3 -> Color(0xFFFF4081)
                                                    ProjectTemplate.EMPTY_COMPOSE -> Color.LightGray
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.DarkGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        template.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        template.description,
                                        fontSize = 11.sp,
                                        color = Color(0xFF90A4AE),
                                        lineHeight = 15.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search and Category Filter Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Projects (${filteredProjects.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    // Search input & Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search projects...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = StudioSurface,
                                unfocusedContainerColor = StudioSurface,
                                focusedBorderColor = StudioGreen,
                                unfocusedBorderColor = StudioBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )

                        // Filter Chips (All / Local / GitHub)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = selectedFilter == "ALL",
                                onClick = { selectedFilter = "ALL" },
                                label = { Text("All", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioGreen,
                                    selectedLabelColor = Color(0xFF003919),
                                    containerColor = StudioSurface
                                )
                            )
                            FilterChip(
                                selected = selectedFilter == "GITHUB",
                                onClick = { selectedFilter = "GITHUB" },
                                label = { Text("Git", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioGreen,
                                    selectedLabelColor = Color(0xFF003919),
                                    containerColor = StudioSurface
                                )
                            )
                        }
                    }
                }
            }

            // Empty Projects State
            if (filteredProjects.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StudioSurface,
                        border = BorderStroke(0.5.dp, StudioBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Tidak ada proyek ditemukan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Mulai proyek baru dari template di atas atau impor dari GitHub.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Projects List with Modern Cards
            items(filteredProjects, key = { it.id }) { project ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectProject(project) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = StudioSurface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (project.isGitHubProject)
                                        Brush.linearGradient(listOf(Color(0xFF24292E), Color(0xFF333842)))
                                    else
                                        Brush.linearGradient(listOf(StudioSurfaceVariant, Color(0xFF32363D)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (project.isGitHubProject) Icons.Default.CloudDownload else Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = if (project.isGitHubProject) StudioGreen else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    project.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                if (project.isGitHubProject) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = StudioGreen.copy(alpha = 0.15f),
                                        border = BorderStroke(0.5.dp, StudioGreen.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            "GitHub @ ${project.githubBranch ?: "main"}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StudioGreen,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (project.isGitHubProject && !project.githubOwner.isNullOrBlank())
                                    "${project.githubOwner}/${project.githubRepo}"
                                else
                                    project.packageName,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (project.isGitHubProject) StudioCyan else StudioGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Diperbarui: ${dateFormat.format(Date(project.updatedAt))}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteProject(project.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Hapus",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = StudioGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
