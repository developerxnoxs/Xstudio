package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.ProjectFileEntity
import com.example.ui.theme.*

@Composable
fun RenameMoveDialog(
    isOpen: Boolean,
    file: ProjectFileEntity?,
    onDismiss: () -> Unit,
    onRename: (file: ProjectFileEntity, newName: String) -> Unit,
    onMove: (file: ProjectFileEntity, newParentPath: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen || file == null) return

    var newName by remember(file) { mutableStateOf(file.name) }
    var newParentPath by remember(file) { mutableStateOf(file.parentPath) }
    var isRenameMode by remember { mutableStateOf(true) }

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
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = StudioCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRenameMode) "Rename ${if (file.isDirectory) "Directory" else "File"}" else "Move Location", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isRenameMode,
                        onClick = { isRenameMode = true },
                        label = { Text("Rename") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isRenameMode,
                        onClick = { isRenameMode = false },
                        label = { Text("Move Path") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isRenameMode) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = newParentPath,
                        onValueChange = { newParentPath = it },
                        label = { Text("New Parent Directory Path") },
                        placeholder = { Text("e.g. app/src/main/java/com/example") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
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
                            if (isRenameMode && newName.isNotBlank()) {
                                onRename(file, newName.trim())
                            } else if (!isRenameMode) {
                                onMove(file, newParentPath.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = Color(0xFF00373D))
                    ) {
                        Text(if (isRenameMode) "Rename" else "Move", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
