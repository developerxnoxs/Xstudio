package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class CodeSnippetTemplate(
    val id: String,
    val title: String,
    val category: String, // "COMPOSE_UI", "MVVM_STATE", "DATABASE", "NETWORKING"
    val description: String,
    val defaultName: String,
    val generateCode: (packageName: String, componentName: String) -> String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeSnippetGeneratorDialog(
    isOpen: Boolean,
    defaultPackageName: String,
    onInsertToActiveFile: (code: String) -> Unit,
    onCreateAsNewFile: (fileName: String, code: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val templates = remember {
        listOf(
            CodeSnippetTemplate(
                id = "scaffold_m3",
                title = "M3 Scaffold + TopAppBar + FAB",
                category = "COMPOSE_UI",
                description = "Layar lengkap dengan Scaffold Material 3, TopAppBar, Floating Action Button, dan Snackbar host.",
                defaultName = "HomeScreen",
                generateCode = { pkg, name ->
                    """package $pkg

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun $name(
    onNavigateBack: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$name") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch { snackbarHostState.showSnackbar("Aksi FAB diklik!") }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Konten $name",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}
"""
                }
            ),
            CodeSnippetTemplate(
                id = "lazy_cards_list",
                title = "LazyColumn List + Search Bar",
                category = "COMPOSE_UI",
                description = "Daftar item interaktif dengan kolom pencarian, Card elevation, dan tag status.",
                defaultName = "ItemListView",
                generateCode = { pkg, name ->
                    """package $pkg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ListEntryItem(val id: Int, val title: String, val subtitle: String, val category: String)

@Composable
fun $name(
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val allItems = remember {
        (1..20).map {
            ListEntryItem(it, "Item #${'$'}it", "Deskripsi lengkap untuk elemen #${'$'}it", if (it % 2 == 0) "Aktif" else "Pending")
        }
    }

    val filteredList = remember(allItems, searchQuery) {
        if (searchQuery.isBlank()) allItems
        else allItems.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari item...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredList, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SuggestionChip(
                            onClick = {},
                            label = { Text(item.category) }
                        )
                    }
                }
            }
        }
    }
}
"""
                }
            ),
            CodeSnippetTemplate(
                id = "mvvm_viewmodel",
                title = "Clean MVVM ViewModel + UiState",
                category = "MVVM_STATE",
                description = "Pola arsitektur Clean ViewModel dengan StateFlow reaktif, Sealed UiState, dan Coroutines.",
                defaultName = "MainViewModel",
                generateCode = { pkg, name ->
                    """package $pkg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ${name}UiState {
    data object Loading : ${name}UiState
    data class Success(val data: List<String>) : ${name}UiState
    data class Error(val message: String) : ${name}UiState
}

class $name : ViewModel() {

    private val _uiState = MutableStateFlow<${name}UiState>(${name}UiState.Loading)
    val uiState: StateFlow<${name}UiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { ${name}UiState.Loading }
            try {
                delay(800) // Simulasi loading asinkron
                val sampleData = listOf("Kotlin Coroutines", "Jetpack Compose", "Clean Architecture", "Room Database")
                _uiState.update { ${name}UiState.Success(sampleData) }
            } catch (e: Exception) {
                _uiState.update { ${name}UiState.Error(e.localizedMessage ?: "Terjadi kesalahan") }
            }
        }
    }
}
"""
                }
            ),
            CodeSnippetTemplate(
                id = "room_boilerplate",
                title = "Room Entity + DAO + Database",
                category = "DATABASE",
                description = "Template database SQLite lokal lengkap dengan Entity, DAO (CRUD + Flow), dan AppDatabase singleton.",
                defaultName = "AppDatabaseSetup",
                generateCode = { pkg, name ->
                    """package $pkg

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Delete
    suspend fun deleteItem(item: ItemEntity)
}

@Database(entities = [ItemEntity::class], version = 1, exportSchema = false)
abstract class AppLocalDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppLocalDatabase? = null

        fun getInstance(context: Context): AppLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppLocalDatabase::class.java,
                    "app_local.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
"""
                }
            ),
            CodeSnippetTemplate(
                id = "retrofit_service",
                title = "Retrofit REST API + OkHttp Client",
                category = "NETWORKING",
                description = "Interface Retrofit 2 lengkap dengan suspend functions, Moshi converter, dan Logging Interceptor.",
                defaultName = "ApiService",
                generateCode = { pkg, name ->
                    """package $pkg

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class ApiResponseItem(val id: Int, val title: String, val body: String)

interface $name {
    @GET("posts")
    suspend fun getPosts(@Query("limit") limit: Int = 20): List<ApiResponseItem>

    @GET("posts/{id}")
    suspend fun getPostById(@Path("id") id: Int): ApiResponseItem

    companion object {
        private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

        fun create(): $name {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create($name::class.java)
        }
    }
}
"""
                }
            )
        )
    }

    var selectedTemplate by remember { mutableStateOf(templates.first()) }
    var componentName by remember(selectedTemplate) { mutableStateOf(selectedTemplate.defaultName) }
    var packageName by remember { mutableStateOf(defaultPackageName) }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val categories = listOf("ALL", "COMPOSE_UI", "MVVM_STATE", "DATABASE", "NETWORKING")

    val filteredTemplates = remember(templates, selectedCategory) {
        if (selectedCategory == "ALL") templates else templates.filter { it.category == selectedCategory }
    }

    val generatedCode = remember(selectedTemplate, packageName, componentName) {
        selectedTemplate.generateCode(packageName, componentName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Widgets, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compose & Architecture Generator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Categories Row
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

                Spacer(modifier = Modifier.height(8.dp))

                // Template selector list
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredTemplates) { tmpl ->
                        val isSelected = tmpl.id == selectedTemplate.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) StudioGreen.copy(alpha = 0.15f) else StudioSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) StudioGreen else StudioBorder),
                            modifier = Modifier
                                .clickable {
                                    selectedTemplate = tmpl
                                    componentName = tmpl.defaultName
                                }
                                .padding(2.dp)
                        ) {
                            Text(
                                text = tmpl.title,
                                color = if (isSelected) StudioGreen else Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Name & Package Input Fields
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = componentName,
                        onValueChange = { componentName = it },
                        label = { Text("Nama Komponen / Kelas", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioGreen,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package Name", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioCyan,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Code Preview Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioBackground,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        item {
                            Text(
                                text = generatedCode,
                                color = Color(0xFF80CBC4),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onInsertToActiveFile(generatedCode)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Input, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sisipkan ke File Aktif", color = StudioCyan, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val newFileName = "$componentName.kt"
                            onCreateAsNewFile(newFileName, generatedCode)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioGreen)
                    ) {
                        Icon(Icons.Default.AddBox, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buat File Baru ($componentName.kt)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
