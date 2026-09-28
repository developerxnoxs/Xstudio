package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.ai.CodeReviewIssue
import com.example.data.ai.CodeReviewResult
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCodeReviewSheet(
    isOpen: Boolean,
    isLoading: Boolean,
    reviewResult: CodeReviewResult?,
    activeFileName: String,
    onDismiss: () -> Unit,
    onReAnalyze: () -> Unit,
    onApplyPatch: (CodeReviewIssue) -> Unit,
    onNavigateToLine: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var selectedFilter by remember { mutableStateOf("ALL") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StudioGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.HealthAndSafety,
                        contentDescription = "Code Doctor",
                        tint = StudioGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "AI Code Doctor & Linter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StudioCyan.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "Gemini 3.1 Pro",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioCyan,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "File: $activeFileName",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = onReAnalyze, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = StudioGreen, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Ulang Analisis", tint = StudioGreen)
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = StudioGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Gemini 3.1 Pro sedang menganalisis arsitektur & performa...",
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Memeriksa recomposition traps, memory leaks, dan standar M3",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else if (reviewResult != null) {
                // Score Banner
                val scoreColor = when {
                    reviewResult.overallScore >= 85 -> StudioGreen
                    reviewResult.overallScore >= 70 -> StudioYellow
                    else -> StudioRed
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                    border = BorderStroke(0.5.dp, StudioBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(scoreColor.copy(alpha = 0.2f))
                                .border(2.dp, scoreColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${reviewResult.overallScore}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = scoreColor
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Skor Kualitas Kode: ${if (reviewResult.overallScore >= 85) "Sangat Baik" else if (reviewResult.overallScore >= 70) "Cukup Baik" else "Perlu Perbaikan"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                reviewResult.summary,
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                // Filter Chips
                val issues = reviewResult.issues
                val criticalCount = issues.count { it.severity == "CRITICAL" }
                val warningCount = issues.count { it.severity == "WARNING" }
                val suggestionCount = issues.count { it.severity == "SUGGESTION" }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Semua (${issues.size})", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioGreen.copy(alpha = 0.2f),
                            selectedLabelColor = StudioGreen
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "CRITICAL",
                        onClick = { selectedFilter = "CRITICAL" },
                        label = { Text("Kritis ($criticalCount)", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioRed.copy(alpha = 0.2f),
                            selectedLabelColor = StudioRed
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "WARNING",
                        onClick = { selectedFilter = "WARNING" },
                        label = { Text("Warning ($warningCount)", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioYellow.copy(alpha = 0.2f),
                            selectedLabelColor = StudioYellow
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "SUGGESTION",
                        onClick = { selectedFilter = "SUGGESTION" },
                        label = { Text("Saran ($suggestionCount)", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioCyan.copy(alpha = 0.2f),
                            selectedLabelColor = StudioCyan
                        )
                    )
                }

                val filteredIssues = when (selectedFilter) {
                    "CRITICAL" -> issues.filter { it.severity == "CRITICAL" }
                    "WARNING" -> issues.filter { it.severity == "WARNING" }
                    "SUGGESTION" -> issues.filter { it.severity == "SUGGESTION" }
                    else -> issues
                }

                if (filteredIssues.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tidak ada masalah pada kategori ini", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Kode Anda mengikuti best practice yang baik.", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        items(filteredIssues) { issue ->
                            IssueCard(
                                issue = issue,
                                onNavigateToLine = { onNavigateToLine(issue.line) },
                                onApplyPatch = { onApplyPatch(issue) }
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Biotech, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Belum Ada Analisis", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tekan tombol di bawah untuk meminta Gemini Pro meninjau kode.", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onReAnalyze,
                            colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color(0xFF003919))
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mulai Code Review", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueCard(
    issue: CodeReviewIssue,
    onNavigateToLine: () -> Unit,
    onApplyPatch: () -> Unit
) {
    val (badgeColor, badgeText) = when (issue.severity) {
        "CRITICAL" -> StudioRed to "KRITIS"
        "WARNING" -> StudioYellow to "PERINGATAN"
        else -> StudioCyan to "SARAN"
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
        border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier.clickable { onNavigateToLine() }
                ) {
                    Text(
                        "Baris ${issue.line}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (!issue.suggestedPatch.isNullOrBlank()) {
                    FilledTonalButton(
                        onClick = onApplyPatch,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StudioGreen.copy(alpha = 0.2f),
                            contentColor = StudioGreen
                        ),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terapkan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                issue.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White
            )

            if (issue.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    issue.description,
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }

            if (issue.recommendation.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = StudioYellow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            issue.recommendation,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }
            }

            if (!issue.suggestedPatch.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(0.5.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "Saran Kode Perbaikan:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioCyan
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            issue.suggestedPatch,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = StudioGreen
                        )
                    }
                }
            }
        }
    }
}
