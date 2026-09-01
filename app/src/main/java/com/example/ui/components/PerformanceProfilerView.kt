package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PerformanceProfilerView(
    onTriggerGc: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isRecording by remember { mutableStateOf(true) }
    var currentCpuPercent by remember { mutableFloatStateOf(14.5f) }
    var currentRamMb by remember { mutableFloatStateOf(52.4f) }
    var currentFps by remember { mutableIntStateOf(60) }
    var jankFrameCount by remember { mutableIntStateOf(0) }
    var gcNotice by remember { mutableStateOf<String?>(null) }

    val cpuHistory = remember { mutableStateListOf(12f, 15f, 18f, 14f, 22f, 19f, 16f, 28f, 14f, 13f, 15f, 20f, 14f) }
    val memoryHistory = remember { mutableStateListOf(48f, 49f, 50f, 51f, 53f, 52f, 54f, 55f, 52f, 53f, 54f, 52f, 52f) }

    // Dynamic metrics simulator loop
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1200)
            val newCpu = (10f + (Math.random() * 25f)).toFloat()
            currentCpuPercent = newCpu
            cpuHistory.add(newCpu)
            if (cpuHistory.size > 20) cpuHistory.removeAt(0)

            val ramDelta = ((Math.random() * 3f) - 1.2f).toFloat()
            currentRamMb = (currentRamMb + ramDelta).coerceIn(38f, 180f)
            memoryHistory.add(currentRamMb)
            if (memoryHistory.size > 20) memoryHistory.removeAt(0)

            currentFps = if (newCpu > 30f) 57 else 60
            if (currentFps < 60) jankFrameCount++
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Profiler Status Bar
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = StudioSurface,
            border = BorderStroke(0.5.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (isRecording) StudioGreen else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Android Runtime Studio Profiler", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (isRecording) "Live Recording (Sampling @ 1.2s)" else "Profiler Paused",
                            fontSize = 10.sp,
                            color = if (isRecording) StudioGreen else Color.Gray
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            currentRamMb = 38.6f
                            gcNotice = "Explicit GC invoked: Freed 16.8 MB (Native + ART Heap)"
                            onTriggerGc()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioOrange),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Force GC", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = { isRecording = !isRecording },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle",
                            tint = if (isRecording) StudioOrange else StudioGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (gcNotice != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = StudioOrange.copy(alpha = 0.15f),
                border = BorderStroke(0.5.dp, StudioOrange.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioOrange, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(gcNotice!!, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = StudioOrange)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // CPU & Rendering Chart Card
        Card(
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
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
                        Icon(Icons.Default.Memory, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CPU Usage (All Threads)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(
                        String.format("%.1f%%", currentCpuPercent),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (currentCpuPercent > 25f) StudioOrange else StudioCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // CPU Sparkline Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Color(0xFF10131B), RoundedCornerShape(6.dp))
                        .padding(4.dp)
                ) {
                    if (cpuHistory.size > 1) {
                        val maxVal = 50f
                        val stepX = size.width / (cpuHistory.size - 1)
                        val path = Path()
                        val fillPath = Path()

                        fillPath.moveTo(0f, size.height)

                        cpuHistory.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = size.height - (value / maxVal * size.height).coerceIn(0f, size.height)
                            if (index == 0) {
                                path.moveTo(x, y)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }

                        fillPath.lineTo(size.width, size.height)
                        fillPath.close()

                        drawPath(
                            fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(StudioCyan.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                        drawPath(
                            path,
                            color = StudioCyan,
                            style = Stroke(width = 2.5f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Memory Heap & Allocation Breakdown
        Card(
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
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
                        Icon(Icons.Default.Storage, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ART JVM Memory Heap", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(
                        String.format("%.1f MB / 512 MB", currentRamMb),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = StudioGreen
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (currentRamMb / 512f).coerceIn(0.05f, 1f) },
                    color = StudioGreen,
                    trackColor = StudioSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Breakdown Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Java Heap: 26.4 MB", fontSize = 10.sp, color = Color.LightGray)
                        Text("Native Heap: 18.2 MB", fontSize = 10.sp, color = Color.LightGray)
                    }
                    Column {
                        Text("Compose Graphics: 7.8 MB", fontSize = 10.sp, color = Color.LightGray)
                        Text("Code & Stack: 4.1 MB", fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Frame Rendering (FPS) & Energy Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                border = BorderStroke(0.5.dp, StudioBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Frame Rate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$currentFps FPS", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StudioGreen)
                    Text("Jank Count: $jankFrameCount frames", fontSize = 9.sp, color = Color.Gray)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                border = BorderStroke(0.5.dp, StudioBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = StudioOrange, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Battery Drain", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Low (0.8% / hr)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StudioOrange)
                    Text("Thermal: Nominal (31°C)", fontSize = 9.sp, color = Color.Gray)
                }
            }
        }
    }
}
