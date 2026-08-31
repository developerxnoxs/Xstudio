package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Android, contentDescription = null, tint = Color(0xFF003919), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Android Studio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Mobile Edition · Android 16 (API 36)", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = onOpenGitHubImport,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GitHub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    FilledTonalButton(
                        onClick = onNewProjectClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = StudioGreen, contentColor = Color(0xFF003919)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioSurface)
            )
        },
        containerColor = StudioBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Status Banner
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StudioSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ready to Code, Build & Sync", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Create native Android apps with Kotlin & Compose, or clone and edit repositories directly from GitHub.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onOpenGitHubImport,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292E), contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clone from GitHub", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(StudioGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }

            // Quick Starter Templates Carousel
            item {
                Column {
                    Text("Quick Starter Templates", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = StudioGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(ProjectTemplate.values()) { template ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StudioSurface,
                                modifier = Modifier
                                    .width(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onCreateFromTemplate(template) }
                                    .padding(12.dp)
                            ) {
                                Column {
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
                                        tint = StudioGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(template.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, maxLines = 1)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(template.description, fontSize = 10.sp, color = Color.Gray, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }

            // Recent Projects Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Projects (${projects.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
            }

            // Projects List
            items(projects, key = { it.id }) { project ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectProject(project) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = StudioSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (project.isGitHubProject) Color(0xFF24292E) else StudioSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (project.isGitHubProject) Icons.Default.CloudDownload else Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = if (project.isGitHubProject) Color.White else StudioGreen
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(project.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                if (project.isGitHubProject) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = StudioGreen.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            "GitHub @ ${project.githubBranch ?: "main"}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StudioGreen,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                if (project.isGitHubProject && !project.githubOwner.isNullOrBlank()) "${project.githubOwner}/${project.githubRepo}" else project.packageName,
                                fontSize = 11.sp,
                                color = if (project.isGitHubProject) Color.LightGray else StudioGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Updated: ${dateFormat.format(Date(project.updatedAt))}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }

                        IconButton(onClick = { onDeleteProject(project.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }

                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
        }
    }
}
