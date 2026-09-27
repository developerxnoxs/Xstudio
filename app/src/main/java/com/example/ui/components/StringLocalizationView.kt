package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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

data class StringItemEntry(
    val key: String,
    var defaultValue: String,
    var idValue: String,
    var enValue: String
)

@Composable
fun StringLocalizationView(
    files: List<ProjectFileEntity>,
    onSaveTranslations: (defaultStringsXml: String, idStringsXml: String, enStringsXml: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Find strings.xml files
    val defaultStringsFile = files.find { it.path == "app/src/main/res/values/strings.xml" }
    val idStringsFile = files.find { it.path == "app/src/main/res/values-id/strings.xml" }
    val enStringsFile = files.find { it.path == "app/src/main/res/values-en/strings.xml" }

    // Parse XML into structured items
    val initialEntries = remember(defaultStringsFile?.content, idStringsFile?.content, enStringsFile?.content) {
        val defaultMap = parseStringsXml(defaultStringsFile?.content ?: "")
        val idMap = parseStringsXml(idStringsFile?.content ?: "")
        val enMap = parseStringsXml(enStringsFile?.content ?: "")

        val allKeys = (defaultMap.keys + idMap.keys + enMap.keys).distinct().sorted()
        allKeys.map { key ->
            StringItemEntry(
                key = key,
                defaultValue = defaultMap[key] ?: "",
                idValue = idMap[key] ?: "",
                enValue = enMap[key] ?: ""
            )
        }
    }

    var entries by remember(initialEntries) { mutableStateOf(initialEntries) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    val filteredEntries = remember(entries, searchQuery) {
        if (searchQuery.isBlank()) entries
        else entries.filter {
            it.key.contains(searchQuery, ignoreCase = true) ||
                    it.defaultValue.contains(searchQuery, ignoreCase = true) ||
                    it.idValue.contains(searchQuery, ignoreCase = true) ||
                    it.enValue.contains(searchQuery, ignoreCase = true)
        }
    }

    val missingIdCount = remember(entries) { entries.count { it.idValue.isBlank() } }
    val missingEnCount = remember(entries) { entries.count { it.enValue.isBlank() } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Translate, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("String & Localization Manager", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${entries.size} keys • ID missing: $missingIdCount • EN missing: $missingEnCount", color = Color.Gray, fontSize = 11.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = StudioSurfaceVariant)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = StudioGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Key", fontSize = 11.sp, color = StudioGreen)
                }

                Button(
                    onClick = {
                        val defaultXml = generateStringsXml(entries.associate { it.key to it.defaultValue })
                        val idXml = generateStringsXml(entries.associate { it.key to if (it.idValue.isNotBlank()) it.idValue else it.defaultValue })
                        val enXml = generateStringsXml(entries.associate { it.key to if (it.enValue.isNotBlank()) it.enValue else it.defaultValue })
                        onSaveTranslations(defaultXml, idXml, enXml)
                        hasUnsavedChanges = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioGreen)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simpan XML", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter key atau teks terjemahan...", color = Color.Gray, fontSize = 12.sp) },
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

        Spacer(modifier = Modifier.height(10.dp))

        // String List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredEntries, key = { it.key }) { entry ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                    border = BorderStroke(0.5.dp, StudioBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.key,
                                color = StudioCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = {
                                    entries = entries.filterNot { it.key == entry.key }
                                    hasUnsavedChanges = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Default Value
                        OutlinedTextField(
                            value = entry.defaultValue,
                            onValueChange = { newVal ->
                                entry.defaultValue = newVal
                                entries = entries.map { if (it.key == entry.key) it.copy(defaultValue = newVal) else it }
                                hasUnsavedChanges = true
                            },
                            label = { Text("Default (values/strings.xml)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StudioGreen,
                                unfocusedBorderColor = StudioBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Indonesian Value
                            OutlinedTextField(
                                value = entry.idValue,
                                onValueChange = { newVal ->
                                    entry.idValue = newVal
                                    entries = entries.map { if (it.key == entry.key) it.copy(idValue = newVal) else it }
                                    hasUnsavedChanges = true
                                },
                                label = { Text("🇮🇩 ID (values-id)", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StudioCyan,
                                    unfocusedBorderColor = if (entry.idValue.isBlank()) StudioOrange else StudioBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            // English Value
                            OutlinedTextField(
                                value = entry.enValue,
                                onValueChange = { newVal ->
                                    entry.enValue = newVal
                                    entries = entries.map { if (it.key == entry.key) it.copy(enValue = newVal) else it }
                                    hasUnsavedChanges = true
                                },
                                label = { Text("🇬🇧 EN (values-en)", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StudioCyan,
                                    unfocusedBorderColor = if (entry.enValue.isBlank()) StudioOrange else StudioBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Key Dialog
    if (showAddDialog) {
        var newKeyName by remember { mutableStateOf("") }
        var newDefaultVal by remember { mutableStateOf("") }
        var newIdVal by remember { mutableStateOf("") }
        var newEnVal by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah String Baru", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newKeyName,
                        onValueChange = { newKeyName = it.replace(" ", "_").lowercase() },
                        label = { Text("Key Name (e.g. btn_confirm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDefaultVal,
                        onValueChange = { newDefaultVal = it },
                        label = { Text("Default Value") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newIdVal,
                        onValueChange = { newIdVal = it },
                        label = { Text("🇮🇩 Indonesian") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newEnVal,
                        onValueChange = { newEnVal = it },
                        label = { Text("🇬🇧 English") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKeyName.isNotBlank()) {
                            entries = entries + StringItemEntry(
                                key = newKeyName,
                                defaultValue = newDefaultVal,
                                idValue = if (newIdVal.isNotBlank()) newIdVal else newDefaultVal,
                                enValue = if (newEnVal.isNotBlank()) newEnVal else newDefaultVal
                            )
                            hasUnsavedChanges = true
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioGreen)
                ) {
                    Text("Tambahkan", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            },
            containerColor = StudioSurface
        )
    }
}

private fun parseStringsXml(xmlContent: String): Map<String, String> {
    if (xmlContent.isBlank()) return emptyMap()
    val map = mutableMapOf<String, String>()
    val regex = Regex("<string[^>]*name=[\"']([^\"']+)[\"'][^>]*>([\\s\\S]*?)</string>")
    regex.findAll(xmlContent).forEach { match ->
        val key = match.groupValues[1]
        val value = match.groupValues[2].trim()
        map[key] = value
    }
    return map
}

private fun generateStringsXml(map: Map<String, String>): String {
    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
    sb.append("<resources>\n")
    map.forEach { (key, value) ->
        val escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        sb.append("    <string name=\"$key\">$escaped</string>\n")
    }
    sb.append("</resources>\n")
    return sb.toString()
}
