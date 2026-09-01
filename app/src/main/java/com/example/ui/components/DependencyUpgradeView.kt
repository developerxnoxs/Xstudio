package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.theme.*

data class DependencyItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val artifact: String,
    val currentVersion: String,
    val latestVersion: String,
    val category: String, // "Compose", "Kotlin", "Architecture", "Networking", "Database"
    val isUpgradable: Boolean = true
)

@Composable
fun DependencyUpgradeView(
    files: List<ProjectFileEntity>,
    onApplyUpgrade: (DependencyItem) -> Unit = {},
    onUpgradeAll: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var filterCategory by remember { mutableStateOf("ALL") }
    var upgradeStatusMessage by remember { mutableStateOf<String?>(null) }

    val dependenciesList = remember {
        mutableStateListOf(
            DependencyItem("1", "Jetpack Compose UI", "androidx.compose.ui:ui", "1.7.0", "1.7.5", "Compose"),
            DependencyItem("2", "Compose Material 3", "androidx.compose.material3:material3", "1.3.0", "1.3.1", "Compose"),
            DependencyItem("3", "Kotlin Compiler & Stdlib", "org.jetbrains.kotlin:kotlin-stdlib", "2.1.0", "2.1.10", "Kotlin"),
            DependencyItem("4", "Kotlinx Coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-android", "1.8.0", "1.9.0", "Kotlin"),
            DependencyItem("5", "Room Local Database", "androidx.room:room-runtime", "2.6.1", "2.7.0", "Database"),
            DependencyItem("6", "Navigation Compose", "androidx.navigation:navigation-compose", "2.8.0", "2.8.4", "Architecture"),
            DependencyItem("7", "Lifecycle ViewModel Compose", "androidx.lifecycle:lifecycle-viewmodel-compose", "2.8.4", "2.8.7", "Architecture"),
            DependencyItem("8", "Coil Image Loader", "io.coil-kt:coil-compose", "2.7.0", "2.7.0", "Networking", isUpgradable = false)
        )
    }

    val filteredList = remember(dependenciesList, filterCategory) {
        if (filterCategory == "ALL") dependenciesList else dependenciesList.filter { it.category == filterCategory }
    }

    val upgradableCount = remember(dependenciesList) {
        dependenciesList.count { it.isUpgradable }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .padding(10.dp)
    ) {
        // Header card
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = StudioSurface,
            border = BorderStroke(0.5.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Gradle & Version Catalog Assistant", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                if (upgradableCount > 0) "$upgradableCount updates available for libs.versions.toml" else "All dependencies are up to date",
                                fontSize = 10.sp,
                                color = if (upgradableCount > 0) StudioOrange else StudioGreen
                            )
                        }
                    }

                    if (upgradableCount > 0) {
                        Button(
                            onClick = {
                                dependenciesList.replaceAll { it.copy(currentVersion = it.latestVersion, isUpgradable = false) }
                                upgradeStatusMessage = "All $upgradableCount libraries upgraded to stable releases in build config!"
                                onUpgradeAll()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioGreen),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Upgrade All", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003919))
                        }
                    }
                }

                if (upgradeStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StudioGreen.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, StudioGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(upgradeStatusMessage!!, fontSize = 10.sp, color = StudioGreen)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter chips
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "Compose", "Kotlin", "Architecture", "Database").forEach { cat ->
                FilterChip(
                    selected = filterCategory == cat,
                    onClick = { filterCategory = cat },
                    label = { Text(cat, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioGreen.copy(alpha = 0.2f),
                        selectedLabelColor = StudioGreen
                    )
                )
            }
        }

        // Dependency Items List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredList) { dep ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioSurface,
                    border = BorderStroke(0.5.dp, if (dep.isUpgradable) StudioOrange.copy(alpha = 0.4f) else StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dep.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StudioSurfaceVariant
                                ) {
                                    Text(
                                        dep.category,
                                        fontSize = 8.sp,
                                        color = Color.LightGray,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                dep.artifact,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "v${dep.currentVersion}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (dep.isUpgradable) StudioOrange else StudioGreen
                                )
                                if (dep.isUpgradable) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(11.dp).padding(horizontal = 2.dp))
                                    Text(
                                        "v${dep.latestVersion}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioGreen
                                    )
                                }
                            }
                        }

                        if (dep.isUpgradable) {
                            Button(
                                onClick = {
                                    val idx = dependenciesList.indexOfFirst { it.id == dep.id }
                                    if (idx != -1) {
                                        dependenciesList[idx] = dep.copy(currentVersion = dep.latestVersion, isUpgradable = false)
                                        upgradeStatusMessage = "Updated ${dep.name} to v${dep.latestVersion}"
                                        onApplyUpgrade(dep)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioOrange.copy(alpha = 0.2f)),
                                border = BorderStroke(0.5.dp, StudioOrange),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Upgrade", fontSize = 10.sp, color = StudioOrange, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StudioGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "Up to date",
                                    fontSize = 9.sp,
                                    color = StudioGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
