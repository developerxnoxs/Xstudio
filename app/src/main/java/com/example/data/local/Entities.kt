package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val packageName: String,
    val description: String,
    val templateType: String,
    val minSdk: Int = 24,
    val targetSdk: Int = 36,
    val composeVersion: String = "1.7.0",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isGitHubProject: Boolean = false,
    val githubOwner: String? = null,
    val githubRepo: String? = null,
    val githubBranch: String? = "main",
    val githubUrl: String? = null,
    val lastSyncedSha: String? = null
)

@Entity(tableName = "project_files")
data class ProjectFileEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val path: String, // e.g. "app/src/main/java/com/example/MainActivity.kt"
    val name: String, // e.g. "MainActivity.kt"
    val fileType: String, // "KOTLIN", "XML", "GRADLE", "JSON", "MANIFEST"
    val content: String,
    val isDirectory: Boolean = false,
    val parentPath: String = ""
)

@Entity(tableName = "build_logs")
data class BuildLogEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SUCCESS", "FAILED", "WARNING"
    val durationMs: Long,
    val logOutput: String,
    val apkSize: String = "14.2 MB"
)

@Entity(tableName = "agent_task_plans")
data class AgentTaskPlanEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val userGoal: String,
    val status: String, // "PLANNING", "IN_PROGRESS", "COMPLETED", "FAILED", "STOPPED"
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val totalSubTasks: Int = 0,
    val completedSubTasks: Int = 0,
    val summaryResult: String = ""
)

@Entity(tableName = "agent_sub_tasks")
data class AgentSubTaskEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val projectId: String,
    val stepOrder: Int,
    val title: String,
    val description: String = "",
    val stage: String, // "PLANNING", "GENERATING_CODE", "APPLYING_CHANGES", "COMPILING", "SELF_HEALING", "DEPLOYING"
    val status: String = "PENDING", // "PENDING", "RUNNING", "COMPLETED", "FAILED", "SKIPPED"
    val targetFilePath: String? = null,
    val outputLog: String? = null,
    val executionTimeMs: Long = 0,
    val completedAt: Long? = null
)

