package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.github.GitHubRepo
import com.example.data.github.GitHubUser
import com.example.data.github.ImportProgress
import com.example.ui.theme.*

data class CuratedGitHubSample(
    val title: String,
    val owner: String,
    val repo: String,
    val branch: String,
    val description: String,
    val tag: String
)

val CURATED_GITHUB_SAMPLES = listOf(
    CuratedGitHubSample(
        title = "Android Sunflower",
        owner = "android",
        repo = "sunflower",
        branch = "main",
        description = "A gardening app illustrating Android development best practices with Compose.",
        tag = "Best Practices"
    ),
    CuratedGitHubSample(
        title = "Compose Jetpack Samples",
        owner = "android",
        repo = "compose-samples",
        branch = "main",
        description = "Official Jetpack Compose showcases including JetChat and JetSnack.",
        tag = "Official"
    ),
    CuratedGitHubSample(
        title = "Now in Android",
        owner = "android",
        repo = "nowinandroid",
        branch = "main",
        description = "A fully functional Android app built entirely with Kotlin and Jetpack Compose.",
        tag = "Architecture"
    ),
    CuratedGitHubSample(
        title = "Architecture Templates",
        owner = "android",
        repo = "architecture-templates",
        branch = "main",
        description = "App templates to quickly start new Android projects with modern architecture.",
        tag = "Template"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubImportDialog(
    isOpen: Boolean,
    token: String?,
    user: GitHubUser?,
    repos: List<GitHubRepo>,
    isLoading: Boolean,
    progress: ImportProgress?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSaveToken: (String) -> Unit,
    onClearToken: () -> Unit,
    onRefreshRepos: () -> Unit,
    onImportRepo: (owner: String, repo: String, branch: String) -> Unit
) {
    if (!isOpen) return

    var selectedTab by remember { mutableStateOf(if (token.isNullOrBlank()) 1 else 0) } // 0: User Repos, 1: Direct URL/Clone, 2: Curated, 3: Token Settings
    var inputUrlOrSlug by remember { mutableStateOf("") }
    var inputBranch by remember { mutableStateOf("main") }
    var tokenInput by remember { mutableStateOf(token ?: "") }
    var isTokenVisible by remember { mutableStateOf(false) }
    var repoSearchQuery by remember { mutableStateOf("") }

    val filteredRepos = remember(repos, repoSearchQuery) {
        if (repoSearchQuery.isBlank()) repos
        else repos.filter { it.name.contains(repoSearchQuery, ignoreCase = true) || (it.description?.contains(repoSearchQuery, ignoreCase = true) == true) }
    }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
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
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF24292E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Import from GitHub", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text(
                            text = if (user != null) "Logged in as @${user.login}" else "Clone & Edit GitHub repositories",
                            fontSize = 11.sp,
                            color = if (user != null) StudioGreen else Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isLoading) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                // Progress Bar if Loading/Importing
                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StudioSurfaceVariant.copy(alpha = 0.5f))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (progress != null) {
                            LinearProgressIndicator(
                                progress = { progress.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = StudioGreen,
                                trackColor = StudioBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = progress.stage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioGreen
                            )
                            if (progress.currentFile.isNotBlank()) {
                                Text(
                                    text = progress.currentFile,
                                    fontSize = 10.sp,
                                    color = Color.LightGray,
                                    maxLines = 1
                                )
                            }
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = StudioGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Communicating with GitHub API...", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // Error Banner
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = StudioRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StudioRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = errorMessage, color = StudioRed, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Tabs Navigation
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = StudioSurface,
                    contentColor = StudioGreen
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Repos (${repos.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("URL / Clone", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Showcase", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.StarOutline, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Auth Token", fontSize = 12.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                // Content for selected tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // My Repos Tab
                            if (user == null && token.isNullOrBlank()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("GitHub Token Required", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Authenticate with a Personal Access Token (PAT) to view and clone your private & public repositories.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(horizontal = 24.dp),
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { selectedTab = 3 },
                                        colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color(0xFF003919))
                                    ) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Set GitHub Token")
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Search Bar & Refresh
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = repoSearchQuery,
                                            onValueChange = { repoSearchQuery = it },
                                            placeholder = { Text("Filter your repositories...", fontSize = 12.sp) },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = StudioBorder,
                                                focusedBorderColor = StudioGreen
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = onRefreshRepos,
                                            enabled = !isLoading,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(StudioSurfaceVariant, RoundedCornerShape(10.dp))
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = StudioGreen)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (filteredRepos.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(if (repos.isEmpty()) "No repositories found for @${user?.login}" else "No matching repositories", color = Color.Gray, fontSize = 13.sp)
                                        }
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(filteredRepos, key = { it.id }) { repo ->
                                                RepoItemCard(
                                                    repo = repo,
                                                    onImport = { branch ->
                                                        onImportRepo(repo.owner.login, repo.name, branch)
                                                    },
                                                    isEnabled = !isLoading
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Direct URL / Clone Tab
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text("Clone Any Public or Accessible Repository", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                Text(
                                    "Enter a GitHub URL (e.g., https://github.com/owner/repository) or shorthand 'owner/repo'.",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                OutlinedTextField(
                                    value = inputUrlOrSlug,
                                    onValueChange = { inputUrlOrSlug = it },
                                    label = { Text("Repository URL or owner/repo") },
                                    placeholder = { Text("e.g. android/sunflower or https://github.com/...") },
                                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = StudioGreen) },
                                    trailingIcon = {
                                        if (inputUrlOrSlug.isNotBlank()) {
                                            IconButton(onClick = { inputUrlOrSlug = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = StudioGreen,
                                        unfocusedBorderColor = StudioBorder
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = inputBranch,
                                        onValueChange = { inputBranch = it },
                                        label = { Text("Branch") },
                                        placeholder = { Text("main") },
                                        leadingIcon = { Icon(Icons.Default.CallSplit, contentDescription = null, tint = StudioGreen) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = StudioGreen,
                                            unfocusedBorderColor = StudioBorder
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Button(
                                    onClick = {
                                        val cleaned = inputUrlOrSlug.trim()
                                            .removePrefix("https://github.com/")
                                            .removePrefix("http://github.com/")
                                            .removeSuffix(".git")
                                        val parts = cleaned.split("/")
                                        if (parts.size >= 2) {
                                            onImportRepo(parts[0], parts[1], inputBranch.ifBlank { "main" })
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    enabled = inputUrlOrSlug.isNotBlank() && !isLoading,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StudioGreen,
                                        contentColor = Color(0xFF003919)
                                    )
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Clone & Import into Studio", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        2 -> {
                            // Curated Showcase Tab
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Text("Official & Recommended Android Repositories", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("One-click clone and edit top Android Jetpack Compose open source projects.", fontSize = 11.sp, color = Color.Gray)
                                }

                                items(CURATED_GITHUB_SAMPLES) { sample ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = StudioSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(StudioGreen.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Android, contentDescription = null, tint = StudioGreen)
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(sample.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = StudioGreen.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(sample.tag, fontSize = 9.sp, color = StudioGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                                Text("${sample.owner}/${sample.repo} (${sample.branch})", fontSize = 11.sp, color = StudioGreen)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(sample.description, fontSize = 11.sp, color = Color.Gray, lineHeight = 14.sp)
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            FilledTonalButton(
                                                onClick = { onImportRepo(sample.owner, sample.repo, sample.branch) },
                                                enabled = !isLoading,
                                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = StudioGreen, contentColor = Color(0xFF003919)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Clone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Auth Token Settings Tab
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("GitHub Authentication (PAT)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                Text(
                                    "A Personal Access Token allows you to clone private repositories and push commits directly from the editor.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    lineHeight = 15.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = StudioSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("How to generate a token:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StudioGreen)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "1. Go to github.com -> Settings -> Developer Settings\n" +
                                                    "2. Personal Access Tokens (Classic)\n" +
                                                    "3. Generate new token with 'repo' scope\n" +
                                                    "4. Paste below (starts with ghp_...)",
                                            fontSize = 11.sp,
                                            color = Color.LightGray,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = tokenInput,
                                    onValueChange = { tokenInput = it },
                                    label = { Text("Personal Access Token (PAT)") },
                                    placeholder = { Text("ghp_xxxxxxxxxxxxxx") },
                                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    trailingIcon = {
                                        IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                            Icon(
                                                if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle Visibility"
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = StudioGreen,
                                        unfocusedBorderColor = StudioBorder
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onSaveToken(tokenInput.trim())
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = tokenInput.isNotBlank() && !isLoading,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color(0xFF003919))
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save & Validate")
                                    }

                                    if (!token.isNullOrBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                tokenInput = ""
                                                onClearToken()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioRed)
                                        ) {
                                            Text("Disconnect")
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

@Composable
private fun RepoItemCard(
    repo: GitHubRepo,
    onImport: (branch: String) -> Unit,
    isEnabled: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = StudioSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF24292E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (repo.private) Icons.Default.Lock else Icons.Default.FolderZip,
                    contentDescription = null,
                    tint = if (repo.private) StudioYellow else StudioGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(repo.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    if (repo.private) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = StudioYellow.copy(alpha = 0.2f)) {
                            Text("Private", fontSize = 8.sp, color = StudioYellow, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
                if (!repo.description.isNullOrBlank()) {
                    Text(repo.description, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Branch: ${repo.defaultBranch}", fontSize = 9.sp, color = StudioGreen)
                    if (repo.stargazersCount > 0) {
                        Text("★ ${repo.stargazersCount}", fontSize = 9.sp, color = StudioYellow)
                    }
                    if (repo.language != null) {
                        Text("• ${repo.language}", fontSize = 9.sp, color = Color.LightGray)
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            FilledTonalButton(
                onClick = { onImport(repo.defaultBranch) },
                enabled = isEnabled,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = StudioGreen, contentColor = Color(0xFF003919)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
