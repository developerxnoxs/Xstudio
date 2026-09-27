package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectFileEntity
import com.example.ui.theme.*

data class FileSearchResult(
    val file: ProjectFileEntity,
    val line: Int,
    val lineContent: String,
    val matchSnippet: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchDialog(
    isOpen: Boolean,
    files: List<ProjectFileEntity>,
    onOpenFileAtLine: (ProjectFileEntity, Int) -> Unit,
    onReplaceAllInProject: (searchQuery: String, replaceQuery: String, isRegex: Boolean, isCaseSensitive: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }
    var isCaseSensitive by remember { mutableStateOf(false) }
    var showReplaceRow by remember { mutableStateOf(false) }

    val searchResults = remember(searchQuery, files, isRegex, isCaseSensitive) {
        if (searchQuery.isBlank()) return@remember emptyList<FileSearchResult>()
        val results = mutableListOf<FileSearchResult>()

        val pattern = try {
            if (isRegex) {
                if (isCaseSensitive) Regex(searchQuery) else Regex(searchQuery, RegexOption.IGNORE_CASE)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

        for (file in files) {
            if (file.isDirectory) continue
            val lines = file.content.lines()
            for ((index, line) in lines.withIndex()) {
                val matches = if (pattern != null) {
                    pattern.containsMatchIn(line)
                } else {
                    line.contains(searchQuery, ignoreCase = !isCaseSensitive)
                }

                if (matches) {
                    results.add(
                        FileSearchResult(
                            file = file,
                            line = index + 1,
                            lineContent = line.trim(),
                            matchSnippet = line.trim()
                        )
                    )
                }
            }
        }
        results
    }

    val groupedResults = remember(searchResults) {
        searchResults.groupBy { it.file }
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
                // Title and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FindInPage, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Global Find & Replace", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari teks di seluruh file proyek...", color = Color.Gray, fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = StudioGreen) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioGreen,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Options Row: Regex, Match Case, Toggle Replace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = isCaseSensitive,
                            onClick = { isCaseSensitive = !isCaseSensitive },
                            label = { Text("Aa", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioGreen.copy(alpha = 0.2f),
                                selectedLabelColor = StudioGreen
                            )
                        )
                        FilterChip(
                            selected = isRegex,
                            onClick = { isRegex = !isRegex },
                            label = { Text(".*", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioGreen.copy(alpha = 0.2f),
                                selectedLabelColor = StudioGreen
                            )
                        )
                    }

                    TextButton(onClick = { showReplaceRow = !showReplaceRow }) {
                        Icon(
                            if (showReplaceRow) Icons.Default.ExpandLess else Icons.Default.FindReplace,
                            contentDescription = null,
                            tint = StudioCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showReplaceRow) "Sembunyikan Replace" else "Buka Replace", color = StudioCyan, fontSize = 12.sp)
                    }
                }

                // Replace Row (Expandable)
                if (showReplaceRow) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = replaceQuery,
                        onValueChange = { replaceQuery = it },
                        placeholder = { Text("Teks pengganti...", color = Color.Gray, fontSize = 13.sp) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.ChangeCircle, contentDescription = null, tint = StudioOrange) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioOrange,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (searchQuery.isNotEmpty()) {
                                onReplaceAllInProject(searchQuery, replaceQuery, isRegex, isCaseSensitive)
                            }
                        },
                        enabled = searchQuery.isNotEmpty() && searchResults.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioOrange),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ganti Semua (${searchResults.size} matches)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Results Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "Ketik kata kunci untuk mulai mencari." else "Ditemukan ${searchResults.size} hasil di ${groupedResults.size} file",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Results List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedResults.forEach { (file, matches) ->
                        item(key = file.id) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                                border = BorderStroke(0.5.dp, StudioBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    // File Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Description,
                                                contentDescription = null,
                                                tint = StudioCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(file.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = StudioGreen.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                "${matches.size} match",
                                                color = StudioGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Matches inside this file
                                    matches.forEach { match ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onOpenFileAtLine(file, match.line)
                                                    onDismiss()
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "L${match.line}: ",
                                                color = StudioOrange,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = match.lineContent,
                                                color = Color(0xFFDDDDDD),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 2
                                            )
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
