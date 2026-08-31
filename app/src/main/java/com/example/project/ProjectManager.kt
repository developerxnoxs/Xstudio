package com.example.project

import com.example.core.LanguageType
import com.example.core.SdkConfiguration
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import com.example.data.repository.ProjectRepository
import com.example.data.repository.ProjectTemplate
import kotlinx.coroutines.flow.Flow

class ProjectManager(private val repository: ProjectRepository) {

    val allProjects: Flow<List<ProjectEntity>> = repository.allProjects

    suspend fun getProject(projectId: String): ProjectEntity? {
        return repository.getProject(projectId)
    }

    fun getFilesForProject(projectId: String): Flow<List<ProjectFileEntity>> {
        return repository.getFilesForProject(projectId)
    }

    suspend fun createNewProject(
        template: ProjectTemplate,
        name: String,
        packageName: String,
        description: String,
        language: LanguageType = LanguageType.KOTLIN,
        sdkConfig: SdkConfiguration = SdkConfiguration()
    ): ProjectEntity {
        val (project, files) = AndroidProjectGenerator.generateProject(
            template = template,
            name = name,
            packageName = packageName,
            description = description,
            language = language,
            sdkConfig = sdkConfig
        )
        // Store project and files
        repository.insertProjectWithFiles(project, files)
        return project
    }

    suspend fun updateProject(project: ProjectEntity) {
        repository.updateProject(project)
    }

    suspend fun deleteProject(projectId: String) {
        repository.deleteProject(projectId)
    }
}
