package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewFileDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onCreateFile: (fileName: String, fileType: String, isFolder: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var fileName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("KOTLIN") }
    var isFolder by remember { mutableStateOf(false) }

    val fileTypes = listOf(
        "KOTLIN" to "Kotlin (.kt)",
        "JAVA" to "Java (.java)",
        "XML" to "Layout / Values XML (.xml)",
        "GRADLE" to "Gradle Script (.gradle.kts)",
        "JSON" to "JSON (.json)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null, tint = StudioGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isFolder) "New Directory" else "New File", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isFolder,
                        onClick = { isFolder = false },
                        label = { Text("File") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isFolder,
                        onClick = { isFolder = true },
                        label = { Text("Folder") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text(if (isFolder) "Directory Name (e.g. models)" else "File Name (e.g. DetailScreen)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (!isFolder) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("File Type", fontSize = 12.sp, color = StudioGreen, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        fileTypes.forEach { (type, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type }
                                )
                                Text(label, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (fileName.isNotBlank()) {
                                onCreateFile(fileName.trim(), selectedType, isFolder)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color(0xFF003919))
                    ) {
                        Text(if (isFolder) "Create Directory" else "Create File", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
