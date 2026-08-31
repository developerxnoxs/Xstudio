package com.example.filemanager

import com.example.data.local.ProjectFileEntity
import com.example.data.repository.ProjectRepository
import java.util.UUID

class FileManager(private val repository: ProjectRepository) {

    suspend fun createFile(
        projectId: String,
        parentPath: String,
        fileName: String,
        fileType: String,
        initialContent: String = ""
    ): ProjectFileEntity {
        val normalizedParent = parentPath.trim().removeSuffix("/")
        val fullPath = if (normalizedParent.isEmpty()) fileName else "$normalizedParent/$fileName"

        val defaultContent = if (initialContent.isNotEmpty()) {
            initialContent
        } else {
            generateDefaultContentForFile(fileName, fileType)
        }

        val entity = ProjectFileEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            path = fullPath,
            name = fileName,
            fileType = fileType.uppercase(),
            content = defaultContent,
            isDirectory = false,
            parentPath = normalizedParent
        )
        repository.addNewFile(
            projectId = projectId,
            path = fullPath,
            name = fileName,
            fileType = fileType.uppercase(),
            content = defaultContent
        )
        return entity
    }

    suspend fun createDirectory(
        projectId: String,
        parentPath: String,
        dirName: String
    ): ProjectFileEntity {
        val normalizedParent = parentPath.trim().removeSuffix("/")
        val fullPath = if (normalizedParent.isEmpty()) dirName else "$normalizedParent/$dirName"

        val entity = ProjectFileEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            path = fullPath,
            name = dirName,
            fileType = "DIRECTORY",
            content = "",
            isDirectory = true,
            parentPath = normalizedParent
        )
        repository.insertFileEntity(entity)
        return entity
    }

    suspend fun renameFile(
        file: ProjectFileEntity,
        newName: String,
        allProjectFiles: List<ProjectFileEntity>
    ) {
        val oldPath = file.path
        val lastSlash = oldPath.lastIndexOf('/')
        val newPath = if (lastSlash != -1) {
            oldPath.substring(0, lastSlash + 1) + newName
        } else {
            newName
        }

        if (file.isDirectory) {
            // Rename directory and update all descendant paths
            val updatedDir = file.copy(name = newName, path = newPath)
            repository.updateFile(updatedDir)

            for (descendant in allProjectFiles) {
                if (descendant.path.startsWith("$oldPath/")) {
                    val relPath = descendant.path.removePrefix("$oldPath/")
                    val updatedDescendantPath = "$newPath/$relPath"
                    repository.updateFile(descendant.copy(path = updatedDescendantPath))
                }
            }
        } else {
            val updatedFile = file.copy(
                name = newName,
                path = newPath,
                fileType = inferFileTypeFromName(newName)
            )
            repository.updateFile(updatedFile)
        }
    }

    suspend fun moveFile(
        file: ProjectFileEntity,
        newParentPath: String,
        allProjectFiles: List<ProjectFileEntity>
    ) {
        val normalizedParent = newParentPath.trim().removeSuffix("/")
        val newPath = if (normalizedParent.isEmpty()) file.name else "$normalizedParent/${file.name}"
        val oldPath = file.path

        if (file.isDirectory) {
            val updatedDir = file.copy(path = newPath, parentPath = normalizedParent)
            repository.updateFile(updatedDir)

            for (descendant in allProjectFiles) {
                if (descendant.path.startsWith("$oldPath/")) {
                    val relPath = descendant.path.removePrefix("$oldPath/")
                    val updatedDescendantPath = "$newPath/$relPath"
                    repository.updateFile(descendant.copy(path = updatedDescendantPath))
                }
            }
        } else {
            val updatedFile = file.copy(path = newPath, parentPath = normalizedParent)
            repository.updateFile(updatedFile)
        }
    }

    suspend fun deleteFileOrDirectory(
        file: ProjectFileEntity,
        allProjectFiles: List<ProjectFileEntity>
    ) {
        if (file.isDirectory) {
            repository.deleteFile(file.id)
            for (descendant in allProjectFiles) {
                if (descendant.path.startsWith("${file.path}/")) {
                    repository.deleteFile(descendant.id)
                }
            }
        } else {
            repository.deleteFile(file.id)
        }
    }

    private fun inferFileTypeFromName(name: String): String {
        return when {
            name.endsWith(".kt") -> "KOTLIN"
            name.endsWith(".java") -> "JAVA"
            name == "AndroidManifest.xml" -> "MANIFEST"
            name.endsWith(".xml") -> "XML"
            name.endsWith(".gradle.kts") || name.endsWith(".gradle") -> "GRADLE"
            name.endsWith(".json") -> "JSON"
            else -> "OTHER"
        }
    }

    private fun generateDefaultContentForFile(name: String, type: String): String {
        return when (type.uppercase()) {
            "KOTLIN" -> """package com.example
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier

@Composable
fun ${name.removeSuffix(".kt")}(modifier: Modifier = Modifier) {
    Text("Component ${name.removeSuffix(".kt")}")
}
"""
            "JAVA" -> """package com.example;

public class ${name.removeSuffix(".java")} {
    public ${name.removeSuffix(".java")}() {
        // Constructor
    }
}
"""
            "XML" -> """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="New Layout" />
</LinearLayout>
"""
            "GRADLE" -> """// Configuration for $name
plugins {
    alias(libs.plugins.android.application)
}
"""
            "JSON" -> "{\n  \"version\": 1,\n  \"name\": \"$name\"\n}"
            else -> "// $name"
        }
    }
}
