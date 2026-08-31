package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: String)
}

@Dao
interface ProjectFileDao {
    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY isDirectory DESC, name ASC")
    fun getFilesForProject(projectId: String): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM project_files WHERE projectId = :projectId AND path = :path LIMIT 1")
    suspend fun getFileByPath(projectId: String, path: String): ProjectFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<ProjectFileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFileEntity)

    @Update
    suspend fun updateFile(file: ProjectFileEntity)

    @Query("DELETE FROM project_files WHERE id = :fileId")
    suspend fun deleteFile(fileId: String)

    @Query("DELETE FROM project_files WHERE projectId = :projectId")
    suspend fun deleteFilesForProject(projectId: String)
}

@Dao
interface BuildLogDao {
    @Query("SELECT * FROM build_logs WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getBuildLogs(projectId: String): Flow<List<BuildLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildLog(log: BuildLogEntity)

    @Query("DELETE FROM build_logs WHERE projectId = :projectId")
    suspend fun clearLogsForProject(projectId: String)
}

@Dao
interface AgentTaskPlanDao {
    @Query("SELECT * FROM agent_task_plans WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getTaskPlansForProject(projectId: String): Flow<List<AgentTaskPlanEntity>>

    @Query("SELECT * FROM agent_task_plans WHERE id = :planId LIMIT 1")
    suspend fun getTaskPlanById(planId: String): AgentTaskPlanEntity?

    @Query("SELECT * FROM agent_task_plans WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT 1")
    fun getLatestTaskPlanForProject(projectId: String): Flow<AgentTaskPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskPlan(plan: AgentTaskPlanEntity)

    @Update
    suspend fun updateTaskPlan(plan: AgentTaskPlanEntity)

    @Query("UPDATE agent_task_plans SET status = :status, completedAt = :completedAt, summaryResult = :summary, completedSubTasks = :completedCount WHERE id = :planId")
    suspend fun updatePlanCompletion(planId: String, status: String, completedAt: Long, summary: String, completedCount: Int)

    @Query("DELETE FROM agent_task_plans WHERE id = :planId")
    suspend fun deleteTaskPlan(planId: String)

    @Query("DELETE FROM agent_task_plans WHERE projectId = :projectId")
    suspend fun clearPlansForProject(projectId: String)
}

@Dao
interface AgentSubTaskDao {
    @Query("SELECT * FROM agent_sub_tasks WHERE planId = :planId ORDER BY stepOrder ASC")
    fun getSubTasksForPlan(planId: String): Flow<List<AgentSubTaskEntity>>

    @Query("SELECT * FROM agent_sub_tasks WHERE projectId = :projectId ORDER BY completedAt DESC, stepOrder ASC")
    fun getAllSubTasksForProject(projectId: String): Flow<List<AgentSubTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTasks(subTasks: List<AgentSubTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: AgentSubTaskEntity)

    @Update
    suspend fun updateSubTask(subTask: AgentSubTaskEntity)

    @Query("UPDATE agent_sub_tasks SET status = :status, outputLog = :outputLog, executionTimeMs = :executionTimeMs, completedAt = :completedAt WHERE id = :subTaskId")
    suspend fun updateSubTaskStatus(subTaskId: String, status: String, outputLog: String?, executionTimeMs: Long, completedAt: Long?)

    @Query("DELETE FROM agent_sub_tasks WHERE planId = :planId")
    suspend fun deleteSubTasksForPlan(planId: String)
}

