package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

enum class DiffLineType {
    UNCHANGED,
    ADDED,
    DELETED,
    MODIFIED
}

data class DiffLine(
    val lineNumberOriginal: Int?,
    val lineNumberModified: Int?,
    val originalText: String,
    val modifiedText: String,
    val type: DiffLineType
)

@Composable
fun GitVisualDiffView(
    activeFile: ProjectFileEntity?,
    currentEditorContent: String,
    onAcceptAllIncoming: () -> Unit = {},
    onAcceptAllCurrent: () -> Unit = {},
    onResolveMerge: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf("SIDE_BY_SIDE") } // "SIDE_BY_SIDE" or "UNIFIED"
    var diffResultNotice by remember { mutableStateOf<String?>(null) }

    // Generate line-by-line diff between original file content in Room DB vs current editor buffer
    val originalLines = remember(activeFile) {
        activeFile?.content?.lines() ?: listOf("// Original committed code base")
    }
    val modifiedLines = remember(currentEditorContent) {
        currentEditorContent.lines()
    }

    val diffLines = remember(originalLines, modifiedLines) {
        val list = mutableListOf<DiffLine>()
        val maxLines = maxOf(originalLines.size, modifiedLines.size)
        for (i in 0 until maxLines) {
            val orig = originalLines.getOrNull(i)
            val mod = modifiedLines.getOrNull(i)

            when {
                orig == null && mod != null -> {
                    list.add(DiffLine(null, i + 1, "", mod, DiffLineType.ADDED))
                }
                orig != null && mod == null -> {
                    list.add(DiffLine(i + 1, null, orig, "", DiffLineType.DELETED))
                }
                orig != mod && orig != null && mod != null -> {
                    list.add(DiffLine(i + 1, i + 1, orig, mod, DiffLineType.MODIFIED))
                }
                else -> {
                    list.add(DiffLine(i + 1, i + 1, orig ?: "", mod ?: "", DiffLineType.UNCHANGED))
                }
            }
        }
        list
    }

    val additionsCount = remember(diffLines) { diffLines.count { it.type == DiffLineType.ADDED } }
    val deletionsCount = remember(diffLines) { diffLines.count { it.type == DiffLineType.DELETED } }
    val modifiedCount = remember(diffLines) { diffLines.count { it.type == DiffLineType.MODIFIED } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .padding(10.dp)
    ) {
        // Top Toolbar
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = StudioSurface,
            border = BorderStroke(0.5.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Difference, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Git Visual Diff & Merge Viewer: ${activeFile?.name ?: "Active File"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("+$additionsCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("-$deletionsCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioRed)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("~$modifiedCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioOrange)
                            }
                        }
                    }

                    // Conflict actions
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                onAcceptAllIncoming()
                                diffResultNotice = "Accepted all HEAD/committed changes!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioCyan.copy(alpha = 0.2f)),
                            border = BorderStroke(0.5.dp, StudioCyan),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Accept HEAD", fontSize = 10.sp, color = StudioCyan)
                        }

                        Button(
                            onClick = {
                                onAcceptAllCurrent()
                                diffResultNotice = "Kept all local working copy changes!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioGreen),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Keep Local", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003919))
                        }
                    }
                }

                if (diffResultNotice != null) {
                    Spacer(modifier = Modifier.height(6.dp))
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
                            Text(diffResultNotice!!, fontSize = 10.sp, color = StudioGreen)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Diff Viewer Canvas
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0E1117),
            border = BorderStroke(0.5.dp, StudioBorder),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Diff Column Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Original (HEAD / Base)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
                    Text("Modified (Working Copy)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioGreen, modifier = Modifier.weight(1f))
                }

                HorizontalDivider(color = StudioBorder)

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(diffLines) { idx, line ->
                        val bg = when (line.type) {
                            DiffLineType.ADDED -> StudioGreen.copy(alpha = 0.15f)
                            DiffLineType.DELETED -> StudioRed.copy(alpha = 0.15f)
                            DiffLineType.MODIFIED -> StudioOrange.copy(alpha = 0.15f)
                            DiffLineType.UNCHANGED -> Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            // Left side (Original)
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = line.lineNumberOriginal?.toString() ?: "-",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray,
                                    modifier = Modifier.width(24.dp)
                                )
                                Text(
                                    text = line.originalText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (line.type == DiffLineType.DELETED) StudioRed else Color.LightGray,
                                    maxLines = 1
                                )
                            }

                            // Right side (Modified)
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = line.lineNumberModified?.toString() ?: "-",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray,
                                    modifier = Modifier.width(24.dp)
                                )
                                Text(
                                    text = line.modifiedText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (line.type == DiffLineType.ADDED) StudioGreen else if (line.type == DiffLineType.MODIFIED) StudioOrange else Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
