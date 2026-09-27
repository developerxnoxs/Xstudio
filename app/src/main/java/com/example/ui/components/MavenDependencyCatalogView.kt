package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectFileEntity
import com.example.ui.theme.*

data class MavenLibraryDef(
    val id: String,
    val name: String,
    val group: String,
    val artifact: String,
    val version: String,
    val description: String,
    val category: String // "COMPOSE", "ARCHITECTURE", "NETWORKING", "STORAGE", "IMAGE", "TESTING"
) {
    val coordinate: String get() = "$group:$artifact:$version"
    val gradleImplementation: String get() = "implementation(\"$coordinate\")"
}

@Composable
fun MavenDependencyCatalogView(
    files: List<ProjectFileEntity>,
    onAddDependencyToGradle: (MavenLibraryDef) -> Unit,
    modifier: Modifier = Modifier
) {
    val gradleFile = files.find { it.name == "build.gradle.kts" && it.path.contains("app") }
        ?: files.find { it.name == "build.gradle.kts" }
    val gradleContent = gradleFile?.content ?: ""

    val catalog = remember {
        listOf(
            MavenLibraryDef(
                id = "compose-m3",
                name = "Material 3 Compose",
                group = "androidx.compose.material3",
                artifact = "material3",
                version = "1.3.1",
                description = "Komponen desain Material 3 modern untuk Jetpack Compose.",
                category = "COMPOSE"
            ),
            MavenLibraryDef(
                id = "compose-navigation",
                name = "Navigation Compose",
                group = "androidx.navigation",
                artifact = "navigation-compose",
                version = "2.8.5",
                description = "Navigasi type-safe antar layar di Jetpack Compose.",
                category = "COMPOSE"
            ),
            MavenLibraryDef(
                id = "compose-icons-extended",
                name = "Material Icons Extended",
                group = "androidx.compose.material",
                artifact = "material-icons-extended",
                version = "1.7.6",
                description = "Koleksi ribuan icon Material Design lengkap.",
                category = "COMPOSE"
            ),
            MavenLibraryDef(
                id = "lifecycle-viewmodel-compose",
                name = "ViewModel Compose",
                group = "androidx.lifecycle",
                artifact = "lifecycle-viewmodel-compose",
                version = "2.8.7",
                description = "Integrasi Lifecycle ViewModel dan state flow dengan Compose.",
                category = "ARCHITECTURE"
            ),
            MavenLibraryDef(
                id = "room-runtime",
                name = "Room Database Runtime",
                group = "androidx.room",
                artifact = "room-runtime",
                version = "2.6.1",
                description = "Abstraksi SQLite lokal dengan type-safety dan query compile-time.",
                category = "STORAGE"
            ),
            MavenLibraryDef(
                id = "room-ktx",
                name = "Room Coroutines & KTX",
                group = "androidx.room",
                artifact = "room-ktx",
                version = "2.6.1",
                description = "Dukungan Coroutines Flow untuk Room Database.",
                category = "STORAGE"
            ),
            MavenLibraryDef(
                id = "datastore-preferences",
                name = "DataStore Preferences",
                group = "androidx.datastore",
                artifact = "datastore-preferences",
                version = "1.1.1",
                description = "Penyimpanan key-value modern berbasis Coroutines menggantikan SharedPreferences.",
                category = "STORAGE"
            ),
            MavenLibraryDef(
                id = "retrofit",
                name = "Retrofit 2 REST Client",
                group = "com.squareup.retrofit2",
                artifact = "retrofit",
                version = "2.11.0",
                description = "HTTP REST Client deklaratif berbasis interface untuk Android.",
                category = "NETWORKING"
            ),
            MavenLibraryDef(
                id = "converter-moshi",
                name = "Retrofit Moshi Converter",
                group = "com.squareup.retrofit2",
                artifact = "converter-moshi",
                version = "2.11.0",
                description = "Konversi JSON otomatis dari Retrofit menggunakan Moshi.",
                category = "NETWORKING"
            ),
            MavenLibraryDef(
                id = "okhttp-logging",
                name = "OkHttp Logging Interceptor",
                group = "com.squareup.okhttp3",
                artifact = "logging-interceptor",
                version = "4.12.0",
                description = "Mencatat URL, Header, Request & Response HTTP ke logcat.",
                category = "NETWORKING"
            ),
            MavenLibraryDef(
                id = "coil-compose",
                name = "Coil Compose (Image Loader)",
                group = "io.coil-kt",
                artifact = "coil-compose",
                version = "2.7.0",
                description = "Pemuat gambar asinkron cepat berbasis Kotlin Coroutines untuk Compose.",
                category = "IMAGE"
            ),
            MavenLibraryDef(
                id = "kotlinx-serialization-json",
                name = "Kotlinx Serialization JSON",
                group = "org.jetbrains.kotlinx",
                artifact = "kotlinx-serialization-json",
                version = "1.7.3",
                description = "Serialisasi multiplatform JSON cepat tanpa refleksi dari JetBrains.",
                category = "ARCHITECTURE"
            ),
            MavenLibraryDef(
                id = "kotlinx-coroutines-android",
                name = "Kotlinx Coroutines Android",
                group = "org.jetbrains.kotlinx",
                artifact = "kotlinx-coroutines-android",
                version = "1.9.0",
                description = "Dukungan Dispatchers.Main dan UI loop untuk Android.",
                category = "ARCHITECTURE"
            ),
            MavenLibraryDef(
                id = "robolectric",
                name = "Robolectric Unit Testing",
                group = "org.robolectric",
                artifact = "robolectric",
                version = "4.14.1",
                description = "Menjalankan Android framework unit test cepat di JVM tanpa emulator.",
                category = "TESTING"
            )
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val categories = listOf("ALL", "COMPOSE", "ARCHITECTURE", "NETWORKING", "STORAGE", "IMAGE", "TESTING")

    val filteredLibraries = remember(catalog, searchQuery, selectedCategory) {
        catalog.filter { lib ->
            val matchCat = selectedCategory == "ALL" || lib.category == selectedCategory
            val matchQuery = searchQuery.isBlank() ||
                    lib.name.contains(searchQuery, ignoreCase = true) ||
                    lib.artifact.contains(searchQuery, ignoreCase = true) ||
                    lib.description.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Maven Library Search & Adder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Pustaka Android & Jetpack resmi siap ditambahkan ke build.gradle.kts", color = Color.Gray, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari library (mis: Room, Retrofit, Coil)...", color = Color.Gray, fontSize = 12.sp) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StudioGreen,
                unfocusedBorderColor = StudioBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioGreen.copy(alpha = 0.2f),
                        selectedLabelColor = StudioGreen
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Library Items List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredLibraries, key = { it.id }) { lib ->
                val isAlreadyAdded = remember(gradleContent, lib.artifact) {
                    gradleContent.contains(lib.artifact)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                    border = BorderStroke(0.5.dp, if (isAlreadyAdded) StudioGreen.copy(alpha = 0.5f) else StudioBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(lib.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = StudioSurface
                                    ) {
                                        Text(
                                            lib.version,
                                            color = StudioCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    lib.coordinate,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (isAlreadyAdded) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StudioGreen.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Terpasang", color = StudioGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onAddDependencyToGradle(lib) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Tambah", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(lib.description, color = Color(0xFFCCCCCC), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
