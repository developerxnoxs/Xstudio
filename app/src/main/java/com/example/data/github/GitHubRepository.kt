package com.example.data.github

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class ImportProgress(
    val stage: String,
    val progress: Float, // 0.0 to 1.0
    val currentFile: String = ""
)

sealed class GitHubResult<out T> {
    data class Success<out T>(val data: T) : GitHubResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : GitHubResult<Nothing>()
}

class GitHubRepository(
    private val context: Context,
    private val projectRepository: ProjectRepository
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("github_auth_prefs", Context.MODE_PRIVATE)
    private val api: GitHubApiService = GitHubClient.apiService

    companion object {
        private const val KEY_GITHUB_TOKEN = "github_personal_access_token"
        private const val KEY_CACHED_USERNAME = "github_cached_username"
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_GITHUB_TOKEN, token.trim()).apply()
    }

    fun getToken(): String? {
        val t = prefs.getString(KEY_GITHUB_TOKEN, null)
        return if (t.isNullOrBlank()) null else t
    }

    fun clearToken() {
        prefs.edit().remove(KEY_GITHUB_TOKEN).remove(KEY_CACHED_USERNAME).apply()
    }

    private fun getAuthHeader(): String? {
        val token = getToken() ?: return null
        return if (token.startsWith("Bearer ") || token.startsWith("token ")) token else "Bearer $token"
    }

    suspend fun getAuthenticatedUser(): GitHubResult<GitHubUser> = withContext(Dispatchers.IO) {
        val auth = getAuthHeader() ?: return@withContext GitHubResult.Error("No GitHub Token provided. Please enter a Personal Access Token.")
        try {
            val user = api.getCurrentUser(auth)
            prefs.edit().putString(KEY_CACHED_USERNAME, user.login).apply()
            GitHubResult.Success(user)
        } catch (e: Exception) {
            GitHubResult.Error(e.localizedMessage ?: "Failed to authenticate with GitHub", e)
        }
    }

    suspend fun getUserRepositories(): GitHubResult<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        val auth = getAuthHeader() ?: return@withContext GitHubResult.Error("Please configure a GitHub token to view your repositories.")
        try {
            val repos = api.getUserRepos(auth)
            GitHubResult.Success(repos)
        } catch (e: Exception) {
            GitHubResult.Error(e.localizedMessage ?: "Failed to load GitHub repositories", e)
        }
    }

    suspend fun getRepositoryInfo(owner: String, repo: String): GitHubResult<GitHubRepo> = withContext(Dispatchers.IO) {
        try {
            val repoInfo = api.getRepository(getAuthHeader(), owner, repo)
            GitHubResult.Success(repoInfo)
        } catch (e: Exception) {
            GitHubResult.Error(e.localizedMessage ?: "Failed to fetch repository $owner/$repo", e)
        }
    }

    suspend fun getBranches(owner: String, repo: String): GitHubResult<List<GitHubBranch>> = withContext(Dispatchers.IO) {
        try {
            val branches = api.getBranches(getAuthHeader(), owner, repo)
            GitHubResult.Success(branches)
        } catch (e: Exception) {
            GitHubResult.Error(e.localizedMessage ?: "Failed to fetch branches", e)
        }
    }

    suspend fun getCommits(owner: String, repo: String, branch: String? = null): GitHubResult<List<GitHubCommitItem>> = withContext(Dispatchers.IO) {
        try {
            val commits = api.getCommits(getAuthHeader(), owner, repo, branch)
            GitHubResult.Success(commits)
        } catch (e: Exception) {
            GitHubResult.Error(e.localizedMessage ?: "Failed to fetch commits", e)
        }
    }

    /**
     * Imports a GitHub repository tree and files into a new Android Studio project.
     */
    suspend fun importRepository(
        owner: String,
        repo: String,
        branch: String = "main",
        onProgress: (ImportProgress) -> Unit = {}
    ): GitHubResult<ProjectEntity> = withContext(Dispatchers.IO) {
        try {
            onProgress(ImportProgress("Connecting to GitHub...", 0.05f))

            val auth = getAuthHeader()
            val repoInfo = api.getRepository(auth, owner, repo)
            val targetBranch = if (branch.isBlank()) repoInfo.defaultBranch else branch

            onProgress(ImportProgress("Fetching repository file tree for '$targetBranch'...", 0.15f))

            // Get branch commit to find tree SHA
            val treeResponse = try {
                api.getTreeRecursive(auth, owner, repo, targetBranch, recursive = 1)
            } catch (e: Exception) {
                // Fallback to default branch if custom branch failed
                api.getTreeRecursive(auth, owner, repo, repoInfo.defaultBranch, recursive = 1)
            }

            val validFiles = treeResponse.tree.filter { item ->
                item.type == "blob" && isSupportedProjectFile(item.path)
            }.take(120) // Limit to top 120 key source files to maintain swift import

            if (validFiles.isEmpty()) {
                return@withContext GitHubResult.Error("No valid Android/code source files found in repository '$owner/$repo'.")
            }

            val projectId = UUID.randomUUID().toString()
            val downloadedFiles = mutableListOf<ProjectFileEntity>()

            var processedCount = 0
            val totalFiles = validFiles.size

            for (treeItem in validFiles) {
                processedCount++
                val percentage = 0.20f + (0.70f * (processedCount.toFloat() / totalFiles.toFloat()))
                val fileName = treeItem.path.substringAfterLast("/")
                onProgress(ImportProgress("Downloading ($processedCount/$totalFiles): $fileName", percentage, treeItem.path))

                val content = try {
                    val contentResponse = api.getFileContent(auth, owner, repo, treeItem.path, ref = targetBranch)
                    if (contentResponse.content != null) {
                        decodeBase64(contentResponse.content)
                    } else if (contentResponse.downloadUrl != null) {
                        api.getRawContent(contentResponse.downloadUrl, auth).string()
                    } else {
                        ""
                    }
                } catch (e: Exception) {
                    // Fallback to raw github content
                    try {
                        val rawUrl = "https://raw.githubusercontent.com/$owner/$repo/$targetBranch/${treeItem.path}"
                        api.getRawContent(rawUrl, auth).string()
                    } catch (e2: Exception) {
                        "// Failed to load content: ${e2.message}"
                    }
                }

                val fileType = detectFileType(fileName)
                val parentPath = if (treeItem.path.contains("/")) treeItem.path.substringBeforeLast("/") else ""

                downloadedFiles.add(
                    ProjectFileEntity(
                        id = UUID.randomUUID().toString(),
                        projectId = projectId,
                        path = treeItem.path,
                        name = fileName,
                        fileType = fileType,
                        content = content,
                        isDirectory = false,
                        parentPath = parentPath
                    )
                )
            }

            onProgress(ImportProgress("Setting up project database...", 0.95f))

            // Derive package name or default
            val packageName = downloadedFiles.find { it.name == "AndroidManifest.xml" }?.let { manifest ->
                Regex("""package=["']([^"']+)["']""").find(manifest.content)?.groupValues?.get(1)
            } ?: downloadedFiles.find { it.fileType == "KOTLIN" && it.content.startsWith("package ") }?.let { kt ->
                kt.content.lines().firstOrNull { it.startsWith("package ") }?.removePrefix("package ")?.trim()?.removeSuffix(";")
            } ?: "com.example.${repo.lowercase().replace(Regex("[^a-z0-9]"), "")}"

            val projectEntity = ProjectEntity(
                id = projectId,
                name = repo,
                packageName = packageName,
                description = repoInfo.description ?: "Imported from GitHub: $owner/$repo ($targetBranch)",
                templateType = "GITHUB_CLONE",
                minSdk = 24,
                targetSdk = 36,
                isGitHubProject = true,
                githubOwner = owner,
                githubRepo = repo,
                githubBranch = targetBranch,
                githubUrl = repoInfo.htmlUrl,
                lastSyncedSha = treeResponse.sha
            )

            projectRepository.insertProjectWithFiles(projectEntity, downloadedFiles)

            onProgress(ImportProgress("Ready!", 1.0f))
            GitHubResult.Success(projectEntity)

        } catch (e: Exception) {
            GitHubResult.Error(e.localizedMessage ?: "Failed to import project from GitHub", e)
        }
    }

    /**
     * Commits and pushes a modified file directly to GitHub.
     */
    suspend fun pushFileToGitHub(
        project: ProjectEntity,
        file: ProjectFileEntity,
        commitMessage: String
    ): GitHubResult<GitHubCommitInfo> = withContext(Dispatchers.IO) {
        val owner = project.githubOwner ?: return@withContext GitHubResult.Error("Project is not linked to a GitHub repository owner.")
        val repo = project.githubRepo ?: return@withContext GitHubResult.Error("Project is not linked to a GitHub repository name.")
        val branch = project.githubBranch ?: "main"
        val auth = getAuthHeader() ?: return@withContext GitHubResult.Error("GitHub token required to push changes.")

        try {
            // Get current file SHA on remote repo
            val currentSha = try {
                val currentRemote = api.getFileContent(auth, owner, repo, file.path, ref = branch)
                currentRemote.sha
            } catch (e: Exception) {
                null // New file creation on remote
            }

            val base64Content = Base64.encodeToString(file.content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val request = GitHubCommitRequest(
                message = commitMessage.ifBlank { "Update ${file.name} via Android Studio Mobile" },
                content = base64Content,
                sha = currentSha,
                branch = branch
            )

            val response = api.createOrUpdateFile(auth, owner, repo, file.path, request)
            val commitInfo = response.commit ?: GitHubCommitInfo(sha = "head", message = commitMessage)

            // Update project timestamp and lastSyncedSha
            projectRepository.updateProject(
                project.copy(
                    updatedAt = System.currentTimeMillis(),
                    lastSyncedSha = commitInfo.sha
                )
            )

            GitHubResult.Success(commitInfo)
        } catch (e: Exception) {
            GitHubResult.Error("Push failed: ${e.localizedMessage}", e)
        }
    }

    /**
     * Pulls latest file contents from GitHub and syncs to local project.
     */
    suspend fun pullLatestChanges(
        project: ProjectEntity,
        onProgress: (ImportProgress) -> Unit = {}
    ): GitHubResult<Int> = withContext(Dispatchers.IO) {
        val owner = project.githubOwner ?: return@withContext GitHubResult.Error("Project has no GitHub owner.")
        val repo = project.githubRepo ?: return@withContext GitHubResult.Error("Project has no GitHub repo.")
        val branch = project.githubBranch ?: "main"
        val auth = getAuthHeader()

        try {
            onProgress(ImportProgress("Fetching remote tree...", 0.2f))
            val tree = api.getTreeRecursive(auth, owner, repo, branch, recursive = 1)
            val validBlobs = tree.tree.filter { it.type == "blob" && isSupportedProjectFile(it.path) }

            var updatedCount = 0
            val total = validBlobs.size

            for ((idx, item) in validBlobs.withIndex()) {
                val progress = 0.2f + 0.7f * ((idx + 1).toFloat() / total.toFloat())
                onProgress(ImportProgress("Syncing: ${item.path}", progress, item.path))

                val content = try {
                    val remote = api.getFileContent(auth, owner, repo, item.path, ref = branch)
                    if (remote.content != null) decodeBase64(remote.content) else ""
                } catch (e: Exception) {
                    continue
                }

                val fileName = item.path.substringAfterLast("/")
                val fileType = detectFileType(fileName)

                val fileEntity = ProjectFileEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = project.id,
                    path = item.path,
                    name = fileName,
                    fileType = fileType,
                    content = content,
                    isDirectory = false,
                    parentPath = if (item.path.contains("/")) item.path.substringBeforeLast("/") else ""
                )
                projectRepository.insertFileEntity(fileEntity)
                updatedCount++
            }

            projectRepository.updateProject(
                project.copy(
                    updatedAt = System.currentTimeMillis(),
                    lastSyncedSha = tree.sha
                )
            )

            onProgress(ImportProgress("Sync complete!", 1.0f))
            GitHubResult.Success(updatedCount)
        } catch (e: Exception) {
            GitHubResult.Error("Pull failed: ${e.localizedMessage}", e)
        }
    }

    private fun decodeBase64(encoded: String): String {
        return try {
            val clean = encoded.replace("\n", "").replace("\r", "")
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encoded
        }
    }

    private fun isSupportedProjectFile(path: String): Boolean {
        if (path.startsWith(".git/") || path.contains("/build/") || path.startsWith("build/")) return false
        if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".webp")) return false
        if (path.endsWith(".jar") || path.endsWith(".aar") || path.endsWith(".class") || path.endsWith(".apk")) return false
        if (path.endsWith(".zip") || path.endsWith(".tar") || path.endsWith(".so") || path.endsWith(".keystore")) return false

        val ext = path.substringAfterLast(".", "").lowercase()
        return ext in listOf(
            "kt", "java", "xml", "gradle", "kts", "json", "properties",
            "md", "txt", "pro", "toml", "yaml", "yml", "gitignore"
        )
    }

    private fun detectFileType(fileName: String): String {
        return when {
            fileName.endsWith(".kt") -> "KOTLIN"
            fileName.endsWith(".java") -> "JAVA"
            fileName.endsWith(".xml") -> if (fileName.equals("AndroidManifest.xml", ignoreCase = true)) "MANIFEST" else "XML"
            fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts") -> "GRADLE"
            fileName.endsWith(".json") -> "JSON"
            else -> "KOTLIN"
        }
    }
}
