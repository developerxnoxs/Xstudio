package com.example.data.github

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val id: Long,
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "public_repos") val publicRepos: Int = 0,
    val bio: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val id: Long,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val owner: GitHubOwner,
    val description: String? = null,
    val private: Boolean = false,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "stargazers_count") val stargazersCount: Int = 0,
    val language: String? = null,
    val size: Long = 0
)

@JsonClass(generateAdapter = true)
data class GitHubOwner(
    val login: String,
    val id: Long,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubBranch(
    val name: String,
    val commit: GitHubBranchCommit
)

@JsonClass(generateAdapter = true)
data class GitHubBranchCommit(
    val sha: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class GitHubTreeResponse(
    val sha: String,
    val url: String,
    val tree: List<GitHubTreeItem>,
    val truncated: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GitHubTreeItem(
    val path: String,
    val mode: String,
    val type: String, // "blob" (file) or "tree" (dir)
    val sha: String,
    val size: Long? = null,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubContentResponse(
    val name: String,
    val path: String,
    val sha: String,
    val size: Long = 0,
    val url: String,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "git_url") val gitUrl: String? = null,
    @Json(name = "download_url") val downloadUrl: String? = null,
    val type: String,
    val content: String? = null,
    val encoding: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubCommitRequest(
    val message: String,
    val content: String, // Base64 encoded file content
    val sha: String? = null, // Required if updating an existing file
    val branch: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubCommitResponse(
    val content: GitHubContentResponse?,
    val commit: GitHubCommitInfo?
)

@JsonClass(generateAdapter = true)
data class GitHubCommitInfo(
    val sha: String,
    val message: String,
    val html_url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubCommitItem(
    val sha: String,
    val commit: GitHubCommitDetail,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubCommitDetail(
    val message: String,
    val author: GitHubAuthor? = null
)

@JsonClass(generateAdapter = true)
data class GitHubAuthor(
    val name: String,
    val email: String? = null,
    val date: String? = null
)
