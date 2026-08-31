package com.example.filemanager

import com.example.core.FileCategory
import com.example.data.local.ProjectFileEntity

data class FileTreeNode(
    val id: String,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val fileType: String,
    val children: MutableList<FileTreeNode> = mutableListOf(),
    val isExpanded: Boolean = true,
    val fileEntity: ProjectFileEntity? = null
) {
    val category: FileCategory
        get() = when {
            isDirectory -> FileCategory.OTHER
            name.endsWith(".kt") -> FileCategory.KOTLIN
            name.endsWith(".java") -> FileCategory.JAVA
            name == "AndroidManifest.xml" -> FileCategory.MANIFEST
            name.endsWith(".xml") -> FileCategory.XML
            name.endsWith(".gradle.kts") -> FileCategory.GRADLE
            name.endsWith(".gradle") -> FileCategory.GROOVY
            name.endsWith(".json") -> FileCategory.JSON
            name.endsWith(".properties") -> FileCategory.PROPERTIES
            name.endsWith(".toml") -> FileCategory.TOML
            else -> FileCategory.OTHER
        }
}

object FileTreeBuilder {

    fun buildTree(files: List<ProjectFileEntity>): List<FileTreeNode> {
        val rootNodes = mutableListOf<FileTreeNode>()
        val nodeMap = mutableMapOf<String, FileTreeNode>()

        // 1. Create nodes for all entities
        for (file in files) {
            val node = FileTreeNode(
                id = file.id,
                name = file.name,
                path = file.path,
                isDirectory = file.isDirectory,
                fileType = file.fileType,
                fileEntity = file
            )
            nodeMap[file.path] = node
        }

        // 2. Build synthetic folders if paths are segmented e.g. "app/src/main/java/..."
        for (file in files) {
            val segments = file.path.split("/").filter { it.isNotEmpty() }
            if (segments.size > 1) {
                var currentPath = ""
                for (i in 0 until segments.size - 1) {
                    val seg = segments[i]
                    val parentPath = currentPath
                    currentPath = if (currentPath.isEmpty()) seg else "$currentPath/$seg"
                    if (!nodeMap.containsKey(currentPath)) {
                        val syntheticFolder = FileTreeNode(
                            id = "folder_$currentPath",
                            name = seg,
                            path = currentPath,
                            isDirectory = true,
                            fileType = "DIRECTORY",
                            children = mutableListOf()
                        )
                        nodeMap[currentPath] = syntheticFolder
                    }
                }
            }
        }

        // 3. Link children to their parent directories
        for ((path, node) in nodeMap) {
            val lastSlash = path.lastIndexOf('/')
            if (lastSlash != -1) {
                val parentPath = path.substring(0, lastSlash)
                val parent = nodeMap[parentPath]
                if (parent != null) {
                    if (parent.children.none { it.path == node.path }) {
                        parent.children.add(node)
                    }
                } else {
                    if (rootNodes.none { it.path == node.path }) {
                        rootNodes.add(node)
                    }
                }
            } else {
                if (rootNodes.none { it.path == node.path }) {
                    rootNodes.add(node)
                }
            }
        }

        // 4. Sort recursively: directories first, then alphabetical
        fun sortNode(n: FileTreeNode) {
            n.children.sortWith(compareBy<FileTreeNode> { !it.isDirectory }.thenBy { it.name.lowercase() })
            n.children.forEach { sortNode(it) }
        }

        rootNodes.sortWith(compareBy<FileTreeNode> { !it.isDirectory }.thenBy { it.name.lowercase() })
        rootNodes.forEach { sortNode(it) }

        return rootNodes
    }
}
