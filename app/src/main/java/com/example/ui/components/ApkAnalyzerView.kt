package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analyzer.ApkAnalysisReport
import com.example.ui.theme.*

@Composable
fun ApkAnalyzerView(
    report: ApkAnalysisReport?,
    isAnalyzing: Boolean,
    onRefreshAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isAnalyzing) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = StudioGreen)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Menganalisis struktur file APK & DEX...", color = Color.White, fontSize = 13.sp)
            }
        }
        return
    }

    if (report == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Belum ada data analisis APK.", color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRefreshAnalysis, colors = ButtonDefaults.buttonColors(containerColor = StudioGreen)) {
                    Text("Jalankan Analisis Sekarang", color = Color.Black)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                border = BorderStroke(1.dp, StudioBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Android, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(report.apkName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(report.packageName, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        IconButton(onClick = onRefreshAnalysis, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = StudioGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricItem("Ukuran APK", report.formattedTotalSize, StudioGreen)
                        MetricItem("Total File", "${report.totalEntries}", StudioCyan)
                        MetricItem("Min SDK", "API ${report.minSdk}", StudioOrange)
                        MetricItem("Target SDK", "API ${report.targetSdk}", StudioPurple)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Proportional Size Bar
                    Text("Distribusi Ukuran Komponen:", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    val totalBytes = maxOf(1L, report.totalSizeBytes)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    ) {
                        val dexWeight = maxOf(0.05f, report.dexSizeBytes.toFloat() / totalBytes)
                        val resWeight = maxOf(0.05f, report.resourcesSizeBytes.toFloat() / totalBytes)
                        val assetsWeight = maxOf(0.02f, report.assetsSizeBytes.toFloat() / totalBytes)
                        val otherWeight = maxOf(0.02f, report.otherSizeBytes.toFloat() / totalBytes)

                        Box(modifier = Modifier.weight(dexWeight).fillMaxHeight().background(StudioCyan))
                        Box(modifier = Modifier.weight(resWeight).fillMaxHeight().background(StudioGreen))
                        Box(modifier = Modifier.weight(assetsWeight).fillMaxHeight().background(StudioOrange))
                        Box(modifier = Modifier.weight(otherWeight).fillMaxHeight().background(StudioPurple))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendChip("classes.dex", StudioCyan, formatBytes(report.dexSizeBytes))
                        LegendChip("res / arsc", StudioGreen, formatBytes(report.resourcesSizeBytes))
                        LegendChip("assets", StudioOrange, formatBytes(report.assetsSizeBytes))
                        LegendChip("lainnya", StudioPurple, formatBytes(report.otherSizeBytes + report.metaSizeBytes))
                    }
                }
            }
        }

        // Optimization Recommendations Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                border = BorderStroke(1.dp, StudioGreen.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = StudioOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rekomendasi Optimasi Ukuran (Size Insights)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    report.optimizationTips.forEach { tip ->
                        Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            Text("• ", color = StudioGreen, fontWeight = FontWeight.Bold)
                            Text(tip, color = Color(0xFFDDDDDD), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Permissions Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                border = BorderStroke(1.dp, StudioBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Declared Permissions (${report.permissions.size}):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        report.permissions.take(6).forEach { perm ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StudioSurface,
                                border = BorderStroke(0.5.dp, StudioBorder)
                            ) {
                                Text(
                                    text = perm,
                                    color = StudioCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Largest Files Inside APK
        item {
            Text("10 File Terbesar di Dalam APK:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        items(report.largestFiles) { fileEntry ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StudioSurfaceVariant,
                border = BorderStroke(0.5.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(fileEntry.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(fileEntry.path, color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (fileEntry.category) {
                            "DEX" -> StudioCyan.copy(alpha = 0.2f)
                            "RESOURCES" -> StudioGreen.copy(alpha = 0.2f)
                            "ASSETS" -> StudioOrange.copy(alpha = 0.2f)
                            else -> StudioSurface
                        }
                    ) {
                        Text(
                            text = formatBytes(fileEntry.sizeBytes),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
private fun LegendChip(label: String, color: Color, size: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text("$label ($size)", color = Color.Gray, fontSize = 10.sp)
    }
}

private fun formatBytes(bytes: Long): String {
    return if (bytes >= 1024 * 1024) {
        String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    } else {
        String.format("%.1f KB", bytes / 1024.0)
    }
}
