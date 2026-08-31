package com.example.data.repository

import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import java.util.UUID

enum class ProjectTemplate(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val iconName: String
) {
    COMPOSE_M3_APP(
        id = "compose_m3",
        title = "Material 3 Task & Counter App",
        description = "Modern Jetpack Compose app featuring Scaffold, TopAppBar, Floating Action Button, and dynamic lists.",
        category = "Jetpack Compose",
        iconName = "dashboard"
    ),
    AI_GEMINI_CHAT(
        id = "ai_gemini",
        title = "AI Chat Assistant (Gemini)",
        description = "Intelligent AI assistant app with conversational UI, preset prompts, and live streaming response simulation.",
        category = "AI & ML",
        iconName = "smart_toy"
    ),
    CANVAS_GAME(
        id = "canvas_game",
        title = "Arcade Star Catcher (Game)",
        description = "Interactive 2D Canvas game with real-time touch controls, physics collision detection, and score HUD.",
        category = "Canvas & Games",
        iconName = "sports_esports"
    ),
    FINANCE_CONVERTER(
        id = "finance_calc",
        title = "Currency & Unit Converter",
        description = "Clean utility app with reactive exchange rate calculations, quick presets, and conversion history.",
        category = "Utility",
        iconName = "currency_exchange"
    ),
    STOREFRONT_M3(
        id = "storefront",
        title = "E-Commerce Storefront",
        description = "Polished retail app with catalog grid, filter chips, cart drawer, and checkout confirmation.",
        category = "E-Commerce",
        iconName = "shopping_bag"
    ),
    EMPTY_COMPOSE(
        id = "empty_compose",
        title = "Empty Compose Activity",
        description = "Clean minimal starter template with a standard Greeting composable and Material 3 theme.",
        category = "Starter",
        iconName = "code"
    )
}

object TemplatesProvider {

    fun createProject(
        template: ProjectTemplate,
        name: String,
        packageName: String,
        description: String
    ): Pair<ProjectEntity, List<ProjectFileEntity>> {
        val projectId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val project = ProjectEntity(
            id = projectId,
            name = name,
            packageName = packageName,
            description = description.ifBlank { template.description },
            templateType = template.id,
            minSdk = 24,
            targetSdk = 36,
            createdAt = now,
            updatedAt = now
        )

        val files = generateFilesForTemplate(projectId, packageName, name, template)
        return Pair(project, files)
    }

    private fun generateFilesForTemplate(
        projectId: String,
        packageName: String,
        appName: String,
        template: ProjectTemplate
    ): List<ProjectFileEntity> {
        val packagePath = "app/src/main/java/" + packageName.replace(".", "/")
        val files = mutableListOf<ProjectFileEntity>()

        // 1. AndroidManifest.xml
        files.add(
            ProjectFileEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                path = "app/src/main/AndroidManifest.xml",
                name = "AndroidManifest.xml",
                fileType = "MANIFEST",
                content = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="$appName"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.App">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="$appName">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>""".trimIndent()
            )
        )

        // 2. build.gradle.kts
        files.add(
            ProjectFileEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                path = "app/build.gradle.kts",
                name = "build.gradle.kts",
                fileType = "GRADLE",
                content = """plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "$packageName"
    compileSdk = 36

    defaultConfig {
        applicationId = "$packageName"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.lifecycle.runtime.compose)
}""".trimIndent()
            )
        )

        // 3. res/values/strings.xml
        files.add(
            ProjectFileEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                path = "app/src/main/res/values/strings.xml",
                name = "strings.xml",
                fileType = "XML",
                content = """<resources>
    <string name="app_name">$appName</string>
    <string name="welcome_message">Welcome to $appName</string>
</resources>""".trimIndent()
            )
        )

        // 4. res/values/colors.xml
        files.add(
            ProjectFileEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                path = "app/src/main/res/values/colors.xml",
                name = "colors.xml",
                fileType = "XML",
                content = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="primary">#3DDC84</color>
    <color name="primary_dark">#2B9B5C</color>
    <color name="accent">#388BFD</color>
    <color name="background">#121212</color>
    <color name="surface">#1E1E1E</color>
</resources>""".trimIndent()
            )
        )

        // 5. ui/theme/Theme.kt
        files.add(
            ProjectFileEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                path = "$packagePath/ui/theme/Theme.kt",
                name = "Theme.kt",
                fileType = "KOTLIN",
                content = """package $packageName.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GreenPrimary = Color(0xFF3DDC84)
val BlueSecondary = Color(0xFF388BFD)
val DarkBackground = Color(0xFF131314)
val DarkSurface = Color(0xFF1E1F20)

private val DarkScheme = darkColorScheme(
    primary = GreenPrimary,
    secondary = BlueSecondary,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF006D38),
    secondary = Color(0xFF0F62FE),
    background = Color(0xFFF9F9FB),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content
    )
}""".trimIndent()
            )
        )

        // 6. MainActivity.kt (Template-specific)
        val mainActivityContent = when (template) {
            ProjectTemplate.COMPOSE_M3_APP -> getMainForComposeM3(packageName, appName)
            ProjectTemplate.AI_GEMINI_CHAT -> getMainForAiChat(packageName, appName)
            ProjectTemplate.CANVAS_GAME -> getMainForCanvasGame(packageName, appName)
            ProjectTemplate.FINANCE_CONVERTER -> getMainForFinance(packageName, appName)
            ProjectTemplate.STOREFRONT_M3 -> getMainForStorefront(packageName, appName)
            ProjectTemplate.EMPTY_COMPOSE -> getMainForEmpty(packageName, appName)
        }

        files.add(
            ProjectFileEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                path = "$packagePath/MainActivity.kt",
                name = "MainActivity.kt",
                fileType = "KOTLIN",
                content = mainActivityContent
            )
        )

        return files
    }

    private fun getMainForComposeM3(packageName: String, appName: String): String = """package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import $packageName.ui.theme.AppTheme

data class TaskItem(val id: Int, val title: String, val isCompleted: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                TasksAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksAppScreen() {
    var count by remember { mutableIntStateOf(0) }
    var taskText by remember { mutableStateOf("") }
    var tasks by remember {
        mutableStateOf(
            listOf(
                TaskItem(1, "Build Android app directly from mobile", true),
                TaskItem(2, "Design UI with Visual Compose Editor", false),
                TaskItem(3, "Compile and run on built-in device emulator", false)
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$appName", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { count = 0 }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { count++ },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Tap Count: ${'$'}count") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Hero Counter Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Projects Counter", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${'$'}count",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Button(onClick = { count += 5 }) {
                        Text("+5 Boost")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Task Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    label = { Text("New Task Item") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = {
                        if (taskText.isNotBlank()) {
                            tasks = tasks + TaskItem(tasks.size + 1, taskText, false)
                            taskText = ""
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Project Tasks (${'$'}{tasks.count { it.isCompleted }}/${'$'}{tasks.size})", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { checked ->
                                    tasks = tasks.map { if (it.id == item.id) it.copy(isCompleted = checked) else it }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            IconButton(onClick = { tasks = tasks.filter { it.id != item.id } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}
""".trimIndent()

    private fun getMainForAiChat(packageName: String, appName: String): String = """package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import $packageName.ui.theme.AppTheme

data class ChatMessage(val id: Int, val isUser: Boolean, val text: String, val timestamp: Long = System.currentTimeMillis())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                AiChatScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen() {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(1, false, "Hello! I am your mobile Android Studio AI assistant. How can I help you write code or design your app today?")
            )
        )
    }

    val quickPrompts = listOf(
        "Generate a Compose Card",
        "Add Dark Mode Support",
        "Create Room Database DAO",
        "Explain Recomposition"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Gemini Studio AI", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("gemini-3.5-flash", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { messages = listOf(ChatMessage(1, false, "Chat cleared. Ready for your prompt!")) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                items(messages, key = { it.id }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                                bottomEnd = if (msg.isUser) 4.dp else 16.dp
                            ),
                            color = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = 2.dp,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.text,
                                modifier = Modifier.padding(14.dp),
                                color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (isThinking) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Studio Bot is thinking...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Quick Prompt Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPrompts.take(2).forEach { prompt ->
                    SuggestionChip(
                        onClick = { inputText = prompt },
                        label = { Text(prompt, fontSize = 12.sp) }
                    )
                }
            }

            // Input Bar
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask Gemini AI coding question...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val userQuery = inputText
                                val userMsg = ChatMessage(messages.size + 1, true, userQuery)
                                messages = messages + userMsg
                                inputText = ""
                                isThinking = true

                                coroutineScope.launch {
                                    listState.animateScrollToItem(messages.size - 1)
                                    delay(1200)
                                    val aiResponse = when {
                                        userQuery.contains("Card", ignoreCase = true) ->
                                            "Here is a Compose Card snippet:\n\nCard(shape = RoundedCornerShape(16.dp)) {\n  Text('Dynamic Card Content', modifier = Modifier.padding(16.dp))\n}"
                                        userQuery.contains("Dark", ignoreCase = true) ->
                                            "To support Dark Mode in Compose, use `isSystemInDarkTheme()` inside your Theme.kt to switch between `darkColorScheme` and `lightColorScheme`."
                                        else -> "I analyzed your project '$appName'. To implement '${'$'}userQuery', declare your state with `remember { mutableStateOf() }` and compose responsive M3 components!"
                                    }
                                    messages = messages + ChatMessage(messages.size + 1, false, aiResponse)
                                    isThinking = false
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
""".trimIndent()

    private fun getMainForCanvasGame(packageName: String, appName: String): String = """package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random
import $packageName.ui.theme.AppTheme

data class Star(var x: Float, var y: Float, val radius: Float, val speed: Float, val color: Color)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0E14)) {
                    StarCatcherGame()
                }
            }
        }
    }
}

@Composable
fun StarCatcherGame() {
    var playerX by remember { mutableFloatStateOf(300f) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var isGameOver by remember { mutableStateOf(false) }
    var stars by remember { mutableStateOf(listOf<Star>()) }

    // Game loop tick
    LaunchedEffect(isGameOver) {
        if (isGameOver) return@LaunchedEffect
        while (true) {
            delay(16) // ~60fps
            // Spawn star occasionally
            if (Random.nextFloat() < 0.08f && stars.size < 12) {
                stars = stars + Star(
                    x = Random.nextFloat() * 700f + 50f,
                    y = 0f,
                    radius = Random.nextFloat() * 14f + 10f,
                    speed = Random.nextFloat() * 6f + 4f,
                    color = if (Random.nextBoolean()) Color(0xFFFFD166) else Color(0xFF06D6A0)
                )
            }

            // Update stars
            val updated = mutableListOf<Star>()
            for (s in stars) {
                s.y += s.speed
                // Catch check (player at bottom around y=1100f)
                if (kotlin.math.abs(s.x - playerX) < 70f && s.y in 1050f..1180f) {
                    score += 10
                } else if (s.y > 1300f) {
                    lives -= 1
                    if (lives <= 0) {
                        isGameOver = true
                    }
                } else {
                    updated.add(s)
                }
            }
            stars = updated
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    playerX = (playerX + dragAmount.x).coerceIn(60f, 750f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw player ship / paddle
            drawCircle(
                color = Color(0xFF388BFD),
                radius = 36f,
                center = Offset(playerX, 1120f)
            )
            drawCircle(
                color = Color(0xFF3DDC84),
                radius = 20f,
                center = Offset(playerX, 1120f)
            )

            // Draw falling stars
            for (s in stars) {
                drawCircle(
                    color = s.color,
                    radius = s.radius,
                    center = Offset(s.x, s.y)
                )
            }
        }

        // HUD Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("SCORE", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${'$'}score", color = Color(0xFFFFD166), fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("LIVES", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("❤️".repeat(lives.coerceAtLeast(0)), fontSize = 20.sp)
            }
        }

        if (isGameOver) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222D)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("GAME OVER", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF476F))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Final Score: ${'$'}score", fontSize = 18.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            score = 0
                            lives = 3
                            stars = emptyList()
                            isGameOver = false
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play Again")
                    }
                }
            }
        }
    }
}
""".trimIndent()

    private fun getMainForFinance(packageName: String, appName: String): String = """package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import $packageName.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                CurrencyConverterScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen() {
    var amountText by remember { mutableStateOf("100") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("IDR") }

    val rates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "JPY" to 154.5,
        "IDR" to 16200.0,
        "SGD" to 1.35
    )

    val inputAmount = amountText.toDoubleOrNull() ?: 0.0
    val fromRate = rates[fromCurrency] ?: 1.0
    val toRate = rates[toCurrency] ?: 1.0
    val result = (inputAmount / fromRate) * toRate

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$appName", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Conversion Result", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${'$'}{String.format("%,.2f", result)} ${'$'}toCurrency",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text("1 ${'$'}fromCurrency = ${'$'}{String.format("%,.4f", toRate / fromRate)} ${'$'}toCurrency", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount to convert") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CurrencySelector("From", fromCurrency, rates.keys.toList()) { fromCurrency = it }
                IconButton(onClick = {
                    val temp = fromCurrency
                    fromCurrency = toCurrency
                    toCurrency = temp
                }) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Swap")
                }
                CurrencySelector("To", toCurrency, rates.keys.toList()) { toCurrency = it }
            }
        }
    }
}

@Composable
fun CurrencySelector(label: String, selected: String, currencies: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        Button(onClick = { expanded = true }) {
            Text(selected)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            currencies.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c) },
                    onClick = {
                        onSelect(c)
                        expanded = false
                    }
                )
            }
        }
    }
}
""".trimIndent()

    private fun getMainForStorefront(packageName: String, appName: String): String = """package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import $packageName.ui.theme.AppTheme

data class Product(val id: Int, val name: String, val price: Double, val category: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                StorefrontScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorefrontScreen() {
    var cartCount by remember { mutableIntStateOf(0) }
    val products = remember {
        listOf(
            Product(1, "Pixel 9 Pro", 999.0, "Phones"),
            Product(2, "Pixel Watch 3", 349.0, "Wearables"),
            Product(3, "Pixel Buds Pro 2", 229.0, "Audio"),
            Product(4, "Pixel Tablet", 499.0, "Tablets"),
            Product(5, "Titan Security Key", 35.0, "Accessories"),
            Product(6, "Fast Wireless Charger", 79.0, "Accessories")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$appName Store", fontWeight = FontWeight.Bold) },
                actions = {
                    BadgedBox(badge = { if (cartCount > 0) Badge { Text("${'$'}cartCount") } }) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = "Cart")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products, key = { it.id }) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(product.name.take(2).uppercase(), fontWeight = FontWeight.Black, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(product.name, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("${'$'}${'$'}{product.price}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { cartCount++ },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
""".trimIndent()

    private fun getMainForEmpty(packageName: String, appName: String): String = """package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import $packageName.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hello from $appName!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
""".trimIndent()
}
