package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import com.example.filemanager.FileTreeBuilder
import com.example.filemanager.FileTreeNode
import com.example.ui.theme.*

@Composable
fun FileTreeDrawerContent(
    project: ProjectEntity?,
    files: List<ProjectFileEntity>,
    activeFile: ProjectFileEntity?,
    onFileClick: (ProjectFileEntity) -> Unit,
    onAddNewFileClick: () -> Unit,
    onRenameFileClick: (ProjectFileEntity) -> Unit,
    onDeleteFileClick: (ProjectFileEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var isProjectViewMode by remember { mutableStateOf(true) } // true: Android view, false: Project filesystem tree

    // Grouping for Android View
    var isManifestExpanded by remember { mutableStateOf(true) }
    var isJavaExpanded by remember { mutableStateOf(true) }
    var isResExpanded by remember { mutableStateOf(true) }
    var isGradleExpanded by remember { mutableStateOf(true) }

    val manifestFile = files.find { it.fileType == "MANIFEST" || it.name == "AndroidManifest.xml" }
    val javaFiles = files.filter { it.fileType == "KOTLIN" || it.fileType == "JAVA" }
    val resFiles = files.filter { it.fileType == "XML" && it.name != "AndroidManifest.xml" }
    val gradleFiles = files.filter { it.fileType == "GRADLE" || it.name.endsWith(".gradle.kts") }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(290.dp)
            .background(StudioSurface)
            .padding(12.dp)
    ) {
        // Project Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Android, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project?.name ?: "Project Explorer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = project?.packageName ?: "com.example.app",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onAddNewFileClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.NoteAdd, contentDescription = "Add File", tint = StudioGreen, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // View Mode Switch: Android View vs Full Tree View
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurfaceVariant, RoundedCornerShape(8.dp))
                .padding(2.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { isProjectViewMode = true },
                color = if (isProjectViewMode) StudioGreen else Color.Transparent
            ) {
                Text(
                    text = "Android View",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isProjectViewMode) Color(0xFF003919) else Color.LightGray,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
                    maxLines = 1
                )
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { isProjectViewMode = false },
                color = if (!isProjectViewMode) StudioGreen else Color.Transparent
            ) {
                Text(
                    text = "Project Files",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (!isProjectViewMode) Color(0xFF003919) else Color.LightGray,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
                    maxLines = 1
                )
            }
        }

        HorizontalDivider(color = StudioBorder, modifier = Modifier.padding(vertical = 8.dp))

        if (isProjectViewMode) {
            // Android View (Manifests, java, res, Gradle Scripts)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // AndroidManifest
                if (manifestFile != null) {
                    item {
                        FolderHeader(
                            title = "manifests",
                            isExpanded = isManifestExpanded,
                            onToggle = { isManifestExpanded = !isManifestExpanded }
                        )
                    }
                    if (isManifestExpanded) {
                        item {
                            FileItemRow(
                                file = manifestFile,
                                icon = Icons.Default.Description,
                                iconTint = StudioGreen,
                                isActive = activeFile?.id == manifestFile.id,
                                indent = 16,
                                onClick = { onFileClick(manifestFile) },
                                onRename = { onRenameFileClick(manifestFile) },
                                onDelete = null
                            )
                        }
                    }
                }

                // java / kotlin sources
                item {
                    FolderHeader(
                        title = "java / kotlin",
                        isExpanded = isJavaExpanded,
                        onToggle = { isJavaExpanded = !isJavaExpanded }
                    )
                }

                if (isJavaExpanded) {
                    items(javaFiles, key = { it.id }) { file ->
                        FileItemRow(
                            file = file,
                            icon = if (file.fileType == "KOTLIN") Icons.Default.Code else Icons.Default.Terminal,
                            iconTint = if (file.fileType == "KOTLIN") StudioPurple else StudioCyan,
                            isActive = activeFile?.id == file.id,
                            indent = 16,
                            onClick = { onFileClick(file) },
                            onRename = { onRenameFileClick(file) },
                            onDelete = { onDeleteFileClick(file) }
                        )
                    }
                }

                // res resources
                item {
                    FolderHeader(
                        title = "res (resources)",
                        isExpanded = isResExpanded,
                        onToggle = { isResExpanded = !isResExpanded }
                    )
                }

                if (isResExpanded) {
                    items(resFiles, key = { it.id }) { file ->
                        FileItemRow(
                            file = file,
                            icon = Icons.Default.Palette,
                            iconTint = StudioOrange,
                            isActive = activeFile?.id == file.id,
                            indent = 16,
                            onClick = { onFileClick(file) },
                            onRename = { onRenameFileClick(file) },
                            onDelete = { onDeleteFileClick(file) }
                        )
                    }
                }

                // Gradle Scripts
                item {
                    FolderHeader(
                        title = "Gradle Scripts",
                        isExpanded = isGradleExpanded,
                        onToggle = { isGradleExpanded = !isGradleExpanded }
                    )
                }

                if (isGradleExpanded) {
                    items(gradleFiles, key = { it.id }) { file ->
                        FileItemRow(
                            file = file,
                            icon = Icons.Default.Build,
                            iconTint = StudioCyan,
                            isActive = activeFile?.id == file.id,
                            indent = 16,
                            onClick = { onFileClick(file) },
                            onRename = { onRenameFileClick(file) },
                            onDelete = null
                        )
                    }
                }
            }
        } else {
            // Full Filesystem Tree View
            val fileTree = remember(files) { FileTreeBuilder.buildTree(files) }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(fileTree, key = { it.path }) { node ->
                    FileTreeNodeView(
                        node = node,
                        activeFile = activeFile,
                        onFileClick = onFileClick,
                        onRename = onRenameFileClick,
                        onDelete = onDeleteFileClick
                    )
                }
            }
        }
    }
}

@Composable
private fun FileTreeNodeView(
    node: FileTreeNode,
    activeFile: ProjectFileEntity?,
    onFileClick: (ProjectFileEntity) -> Unit,
    onRename: (ProjectFileEntity) -> Unit,
    onDelete: (ProjectFileEntity) -> Unit,
    indent: Int = 0
) {
    var isExpanded by remember { mutableStateOf(true) }

    if (node.isDirectory) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = indent.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = node.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFC4C7C5),
                    maxLines = 1
                )
            }
            if (isExpanded) {
                node.children.forEach { child ->
                    FileTreeNodeView(
                        node = child,
                        activeFile = activeFile,
                        onFileClick = onFileClick,
                        onRename = onRename,
                        onDelete = onDelete,
                        indent = indent + 12
                    )
                }
            }
        }
    } else {
        val fileEntity = node.fileEntity
        if (fileEntity != null) {
            val icon = when (fileEntity.fileType.uppercase()) {
                "KOTLIN" -> Icons.Default.Code
                "JAVA" -> Icons.Default.Terminal
                "XML", "MANIFEST" -> Icons.Default.Palette
                "GRADLE" -> Icons.Default.Build
                else -> Icons.Default.Description
            }
            val iconTint = when (fileEntity.fileType.uppercase()) {
                "KOTLIN" -> StudioPurple
                "JAVA" -> StudioCyan
                "XML", "MANIFEST" -> StudioOrange
                "GRADLE" -> StudioGreen
                else -> Color.Gray
            }
            FileItemRow(
                file = fileEntity,
                icon = icon,
                iconTint = iconTint,
                isActive = activeFile?.id == fileEntity.id,
                indent = indent + 12,
                onClick = { onFileClick(fileEntity) },
                onRename = { onRename(fileEntity) },
                onDelete = { onDelete(fileEntity) }
            )
        }
    }
}

@Composable
private fun FolderHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onToggle() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFC4C7C5),
            maxLines = 1
        )
    }
}

@Composable
private fun FileItemRow(
    file: ProjectFileEntity,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isActive: Boolean,
    indent: Int = 0,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) StudioSurfaceVariant else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = file.name,
            fontSize = 11.sp,
            color = if (isActive) StudioGreen else Color(0xFFE3E2E6),
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray, modifier = Modifier.size(12.dp))
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Rename / Move") },
                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showMenu = false
                        onRename()
                    }
                )
                if (onDelete != null) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = StudioRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StudioRed, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
