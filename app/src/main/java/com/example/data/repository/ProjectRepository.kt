package com.example.data.repository

import com.example.data.local.AgentSubTaskEntity
import com.example.data.local.AgentTaskPlanEntity
import com.example.data.local.AppDatabase
import com.example.data.local.BuildLogEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class ProjectRepository(private val db: AppDatabase) {

    val allProjects: Flow<List<ProjectEntity>> = db.projectDao().getAllProjects()

    suspend fun ensureDefaultProjects() {
        val current = db.projectDao().getAllProjects().first()
        if (current.isEmpty()) {
            // Seed first starter projects
            val (p1, f1) = TemplatesProvider.createProject(
                template = ProjectTemplate.COMPOSE_M3_APP,
                name = "MyAwesomeApp",
                packageName = "com.example.myawesomeapp",
                description = "Modern Material 3 Compose Task & Counter application"
            )
            db.projectDao().insertProject(p1)
            db.projectFileDao().insertFiles(f1)

            val (p2, f2) = TemplatesProvider.createProject(
                template = ProjectTemplate.AI_GEMINI_CHAT,
                name = "GeminiStudioAssistant",
                packageName = "com.example.geminiassistant",
                description = "Conversational AI Studio Assistant powered by Gemini"
            )
            db.projectDao().insertProject(p2)
            db.projectFileDao().insertFiles(f2)

            val (p3, f3) = TemplatesProvider.createProject(
                template = ProjectTemplate.CANVAS_GAME,
                name = "SpaceStarRunner",
                packageName = "com.example.stargame",
                description = "High-speed arcade 2D Canvas space game"
            )
            db.projectDao().insertProject(p3)
            db.projectFileDao().insertFiles(f3)
        }
    }

    suspend fun getProject(projectId: String): ProjectEntity? {
        return db.projectDao().getProjectById(projectId)
    }

    fun getFilesForProject(projectId: String): Flow<List<ProjectFileEntity>> {
        return db.projectFileDao().getFilesForProject(projectId)
    }

    suspend fun insertProjectWithFiles(project: ProjectEntity, files: List<ProjectFileEntity>) {
        db.projectDao().insertProject(project)
        db.projectFileDao().insertFiles(files)
    }

    suspend fun createProject(
        template: ProjectTemplate,
        name: String,
        packageName: String,
        description: String
    ): String {
        val (project, files) = TemplatesProvider.createProject(
            template = template,
            name = name,
            packageName = packageName,
            description = description
        )
        db.projectDao().insertProject(project)
        db.projectFileDao().insertFiles(files)
        return project.id
    }

    suspend fun updateProject(project: ProjectEntity) {
        db.projectDao().updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(projectId: String) {
        db.projectFileDao().deleteFilesForProject(projectId)
        db.buildLogDao().clearLogsForProject(projectId)
        db.projectDao().deleteProject(projectId)
    }

    suspend fun saveFileContent(fileId: String, newContent: String) {
        val files = db.projectFileDao().getFilesForProject("").first() // or update directly
        // Update query
    }

    suspend fun updateFile(file: ProjectFileEntity) {
        db.projectFileDao().updateFile(file)
        // Also update project's updatedAt
        val project = db.projectDao().getProjectById(file.projectId)
        if (project != null) {
            db.projectDao().updateProject(project.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun addNewFile(
        projectId: String,
        path: String,
        name: String,
        fileType: String,
        content: String
    ): ProjectFileEntity {
        val entity = ProjectFileEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            path = path,
            name = name,
            fileType = fileType,
            content = content,
            isDirectory = false
        )
        db.projectFileDao().insertFile(entity)
        return entity
    }

    suspend fun insertFileEntity(file: ProjectFileEntity) {
        db.projectFileDao().insertFile(file)
    }

    suspend fun deleteFile(fileId: String) {
        db.projectFileDao().deleteFile(fileId)
    }

    fun getBuildLogs(projectId: String): Flow<List<BuildLogEntity>> {
        return db.buildLogDao().getBuildLogs(projectId)
    }

    suspend fun addBuildLog(log: BuildLogEntity) {
        db.buildLogDao().insertBuildLog(log)
    }

    // Task Planning Module (Room-backed)
    fun getTaskPlansForProject(projectId: String): Flow<List<AgentTaskPlanEntity>> {
        return db.agentTaskPlanDao().getTaskPlansForProject(projectId)
    }

    fun getSubTasksForPlan(planId: String): Flow<List<AgentSubTaskEntity>> {
        return db.agentSubTaskDao().getSubTasksForPlan(planId)
    }

    suspend fun createTaskPlanWithSubTasks(plan: AgentTaskPlanEntity, subTasks: List<AgentSubTaskEntity>) {
        db.agentTaskPlanDao().insertTaskPlan(plan)
        db.agentSubTaskDao().insertSubTasks(subTasks)
    }

    suspend fun updateSubTaskStatus(
        subTaskId: String,
        status: String,
        outputLog: String? = null,
        executionTimeMs: Long = 0,
        completedAt: Long? = null
    ) {
        db.agentSubTaskDao().updateSubTaskStatus(
            subTaskId = subTaskId,
            status = status,
            outputLog = outputLog,
            executionTimeMs = executionTimeMs,
            completedAt = completedAt
        )
    }

    suspend fun updateTaskPlanProgress(
        planId: String,
        status: String,
        completedCount: Int,
        completedAt: Long = System.currentTimeMillis(),
        summary: String = ""
    ) {
        db.agentTaskPlanDao().updatePlanCompletion(
            planId = planId,
            status = status,
            completedAt = completedAt,
            summary = summary,
            completedCount = completedCount
        )
    }

    suspend fun deleteTaskPlan(planId: String) {
        db.agentSubTaskDao().deleteSubTasksForPlan(planId)
        db.agentTaskPlanDao().deleteTaskPlan(planId)
    }
}
