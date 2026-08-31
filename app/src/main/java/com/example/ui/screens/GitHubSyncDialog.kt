package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubSyncDialog(
    isOpen: Boolean,
    project: ProjectEntity?,
    activeFile: ProjectFileEntity?,
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
    onConfigureToken: () -> Unit
) {
    if (!isOpen) return

    var selectedTab by remember { mutableStateOf(0) } // 0: Push & Commit, 1: Pull / Sync, 2: Remote Info

    val isGitHubLinked = project?.isGitHubProject == true && !project.githubOwner.isNullOrBlank()

    Dialog(
        onDismissRequest = { if (!isPushing && !isPulling) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                        Text("Git & GitHub Sync", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
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
                            "To enable Git push and pull operations, import a project directly from your GitHub account or clone an existing repository.",
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
                            text = { Text("Commit & Push", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Pull & Sync", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Repository Info", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // Push & Commit
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                                    text = if (isModified) "Modified" else "Unchanged",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isModified) StudioYellow else StudioGreen,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
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
                                // Pull & Sync
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Synchronize with Remote Repository",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        "Fetch and pull the latest changes and updates from the remote '${project?.githubBranch ?: "main"}' branch.",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        lineHeight = 15.sp
                                    )

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
                                            Text("Pulling from GitHub...", fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Pull Latest Changes", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            2 -> {
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
