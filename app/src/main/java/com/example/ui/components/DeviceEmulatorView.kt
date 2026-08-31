package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.data.local.ProjectEntity
import com.example.ui.theme.*

@Composable
fun DeviceEmulatorView(
    project: ProjectEntity?,
    isDarkMode: Boolean,
    isLandscape: Boolean,
    onToggleDarkMode: () -> Unit,
    onToggleLandscape: () -> Unit,
    onReload: () -> Unit,
    onOpenLogcat: () -> Unit,
    onClose: () -> Unit,
    onLogEvent: (tag: String, level: String, msg: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var deviceScale by remember { mutableFloatStateOf(1.0f) }
    var isInHomeScreen by remember { mutableStateOf(false) }
    var showInstallBanner by remember { mutableStateOf(true) }
    var navMode3Button by remember { mutableStateOf(true) }

    LaunchedEffect(project?.id) {
        showInstallBanner = true
        onLogEvent("adb", "I", "Virtual device emulator-5554 online. Package ${project?.packageName} mounted.")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Emulator Top Toolbar
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(StudioGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Pixel 9 Pro (Android 15)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isInHomeScreen) "Home Launcher" else "${project?.name ?: "App"} (Running)",
                            fontSize = 10.sp,
                            color = if (isInHomeScreen) Color.Gray else StudioGreen
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Nav Mode Switch
                    IconButton(
                        onClick = { navMode3Button = !navMode3Button },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (navMode3Button) Icons.Default.SmartButton else Icons.Default.HorizontalRule,
                            contentDescription = "Toggle Nav Bar",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Dark Mode Toggle
                    IconButton(onClick = onToggleDarkMode, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = if (isDarkMode) StudioCyan else StudioOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Rotate
                    IconButton(onClick = onToggleLandscape, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.ScreenRotation,
                            contentDescription = "Rotate",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Hot Reload / Reinstall
                    IconButton(
                        onClick = {
                            isInHomeScreen = false
                            showInstallBanner = true
                            onReload()
                            onLogEvent("adb", "I", "Streamed hot-reinstalled ${project?.name} APK in 280ms")
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Hot Reload / Reinstall",
                            tint = StudioGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Logcat Quick Button
                    IconButton(onClick = onOpenLogcat, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = "Logcat",
                            tint = StudioBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Close
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close Emulator",
                            tint = StudioRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Phone Frame Bezel
        Box(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = if (isLandscape) 640.dp else 360.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF1B1D22))
                .border(4.dp, Color(0xFF32363F), RoundedCornerShape(32.dp))
                .padding(6.dp)
        ) {
            // Inside Phone Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (isInHomeScreen) Color(0xFF0F1B29) else if (isDarkMode) Color(0xFF121316) else Color(0xFFFBFBFE))
            ) {
                // Virtual Status Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "12:30",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isInHomeScreen || isDarkMode) Color.White else Color.Black
                    )

                    // Punch-hole Camera
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isInHomeScreen || isDarkMode) Color.White else Color.Black)
                        Icon(Icons.Default.BatteryFull, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isInHomeScreen || isDarkMode) Color.White else Color.Black)
                    }
                }

                // Installation Status Banner
                AnimatedVisibility(visible = showInstallBanner && !isInHomeScreen) {
                    Surface(
                        color = Color(0xFF1B382B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Installed: ${project?.name ?: "App"} (${project?.packageName ?: "com.example"})",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(onClick = { showInstallBanner = false }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                // Interactive Canvas Runner or Home Screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isInHomeScreen) {
                        AndroidHomeScreenView(
                            project = project,
                            onLaunchApp = {
                                isInHomeScreen = false
                                onLogEvent("ActivityManager", "I", "Start proc ${project?.packageName} from Launcher")
                            }
                        )
                    } else {
                        RenderProjectApp(
                            project = project,
                            isDarkMode = isDarkMode,
                            onLogEvent = onLogEvent
                        )
                    }
                }

                // Virtual Android Navigation
                if (navMode3Button) {
                    // 3-Button Navigation Bar (Back, Home, Recents)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(if (isInHomeScreen || isDarkMode) Color(0xFF090A0D) else Color(0xFFEEEEF2)),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (!isInHomeScreen) {
                                    isInHomeScreen = true
                                    onLogEvent("ActivityManager", "D", "Back pressed -> navigating to Home")
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = if (isInHomeScreen || isDarkMode) Color.LightGray else Color.DarkGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                isInHomeScreen = true
                                onLogEvent("ActivityManager", "I", "Home pressed -> Launcher displayed")
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, if (isInHomeScreen || isDarkMode) Color.LightGray else Color.DarkGray, CircleShape)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isInHomeScreen) {
                                    isInHomeScreen = false
                                    onLogEvent("ActivityManager", "D", "Switched back to foreground activity")
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .border(2.dp, if (isInHomeScreen || isDarkMode) Color.LightGray else Color.DarkGray, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                } else {
                    // Gesture Navigation Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clickable {
                                isInHomeScreen = !isInHomeScreen
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isInHomeScreen || isDarkMode) Color.Gray else Color.DarkGray)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AndroidHomeScreenView(
    project: ProjectEntity?,
    onLaunchApp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Clock Widget
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text("12:30", fontSize = 48.sp, fontWeight = FontWeight.Light, color = Color.White)
            Text("Monday, Aug 31 • 26°C Sunny", fontSize = 12.sp, color = Color.LightGray)
        }

        // Google Search Pill
        Surface(
            color = Color(0x33FFFFFF),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search apps & web...", fontSize = 12.sp, color = Color.LightGray)
                }
                Icon(Icons.Default.Mic, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
            }
        }

        // Installed Apps Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("INSTALLED APPS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // User Project App Icon (Highlight)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onLaunchApp() }
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StudioGreen,
                        modifier = Modifier.size(54.dp),
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Android, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = project?.name ?: "My App",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0x5500FF88)
                    ) {
                        Text("INSTALLED", fontSize = 8.sp, color = StudioGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                }

                // Chrome
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = RoundedCornerShape(16.dp), color = StudioSurfaceVariant, modifier = Modifier.size(54.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = StudioBlue, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Browser", fontSize = 11.sp, color = Color.LightGray)
                }

                // Camera
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = RoundedCornerShape(16.dp), color = StudioSurfaceVariant, modifier = Modifier.size(54.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = StudioOrange, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Camera", fontSize = 11.sp, color = Color.LightGray)
                }

                // Settings
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = RoundedCornerShape(16.dp), color = StudioSurfaceVariant, modifier = Modifier.size(54.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Settings", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
private fun RenderProjectApp(
    project: ProjectEntity?,
    isDarkMode: Boolean,
    onLogEvent: (tag: String, level: String, msg: String) -> Unit
) {
    val template = project?.templateType ?: "compose_m3"
    val appName = project?.name ?: "App"

    when (template) {
        "compose_m3" -> InteractiveComposeM3Runner(appName = appName, onLog = onLogEvent)
        "ai_gemini" -> InteractiveAiChatRunner(appName = appName, onLog = onLogEvent)
        "canvas_game" -> InteractiveCanvasGameRunner(appName = appName, onLog = onLogEvent)
        "finance_calc" -> InteractiveFinanceRunner(appName = appName, onLog = onLogEvent)
        "storefront" -> InteractiveStorefrontRunner(appName = appName, onLog = onLogEvent)
        else -> InteractiveComposeM3Runner(appName = appName, onLog = onLogEvent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InteractiveComposeM3Runner(appName: String, onLog: (String, String, String) -> Unit) {
    var count by remember { mutableIntStateOf(0) }
    var taskText by remember { mutableStateOf("") }
    var tasks by remember {
        mutableStateOf(
            listOf(
                "Build Android app directly from mobile",
                "Design UI with Visual Compose Editor",
                "Compile and run on built-in device emulator"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appName, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                actions = {
                    IconButton(onClick = {
                        count = 0
                        onLog(appName, "D", "Reset counter to 0")
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    count++
                    onLog(appName, "I", "FAB Clicked: Counter incremented to $count")
                },
                containerColor = StudioGreen,
                contentColor = Color(0xFF003919),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Projects Counter", fontSize = 11.sp)
                        Text("$count", fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                    FilledTonalButton(
                        onClick = {
                            count += 5
                            onLog(appName, "D", "Boost +5 applied: count = $count")
                        }
                    ) {
                        Text("+5 Boost", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    label = { Text("New Task", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        if (taskText.isNotBlank()) {
                            tasks = tasks + taskText
                            onLog(appName, "I", "Added new task: '$taskText'")
                            taskText = ""
                        }
                    }
                ) {
                    Text("Add", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Tasks (${tasks.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tasks.forEachIndexed { index, task ->
                    var isChecked by remember { mutableStateOf(index == 0) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    isChecked = it
                                    onLog(appName, "D", "Task '$task' checked: $it")
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(task, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InteractiveAiChatRunner(appName: String, onLog: (String, String, String) -> Unit) {
    var input by remember { mutableStateOf("") }
    var chatList by remember {
        mutableStateOf(
            listOf(
                "Studio Bot" to "Hello! What Android feature should we code today?"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(appName, fontWeight = FontWeight.Bold, fontSize = 16.sp) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chatList.forEach { (sender, text) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (sender == "Me") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(sender, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(text, fontSize = 12.sp)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Type prompt...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            val userText = input
                            chatList = chatList + ("Me" to userText) + ("Studio Bot" to "Generated code for: '$userText'")
                            onLog(appName, "I", "AI Request: $userText")
                            input = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = StudioGreen)
                }
            }
        }
    }
}

@Composable
private fun InteractiveCanvasGameRunner(appName: String, onLog: (String, String, String) -> Unit) {
    var score by remember { mutableIntStateOf(120) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🚀 Star Runner Game", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("SCORE: $score", color = Color(0xFFFFD166), fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    score += 50
                    onLog(appName, "D", "Star captured! Score = $score")
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.TouchApp, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tap to Catch Star (+50)")
            }
        }
    }
}

@Composable
private fun InteractiveFinanceRunner(appName: String, onLog: (String, String, String) -> Unit) {
    var amount by remember { mutableStateOf("100") }
    val usd = (amount.toDoubleOrNull() ?: 0.0) * 16200.0
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Currency Converter", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("IDR Result", fontSize = 11.sp, color = Color.Gray)
                Text("Rp ${String.format("%,.0f", usd)}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = StudioGreen)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = {
                amount = it
                onLog(appName, "D", "Recalculated exchange for USD: $it")
            },
            label = { Text("USD Amount") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun InteractiveStorefrontRunner(appName: String, onLog: (String, String, String) -> Unit) {
    var cartCount by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Store Catalog", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("🛒 Cart: $cartCount", fontWeight = FontWeight.Bold, color = StudioGreen)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                cartCount++
                onLog(appName, "I", "Added item to cart (Total: $cartCount)")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AddShoppingCart, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Pixel 9 Pro to Cart ($999)")
        }
    }
}
