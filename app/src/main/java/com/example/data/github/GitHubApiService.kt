package com.example.data.github

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface GitHubApiService {

    @GET("user")
    suspend fun getCurrentUser(
        @Header("Authorization") auth: String
    ): GitHubUser

    @GET("user/repos")
    suspend fun getUserRepos(
        @Header("Authorization") auth: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 50,
        @Query("affiliation") affiliation: String = "owner,collaborator"
    ): List<GitHubRepo>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Header("Authorization") auth: String? = null,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRepo

    @GET("repos/{owner}/{repo}/branches")
    suspend fun getBranches(
        @Header("Authorization") auth: String? = null,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<GitHubBranch>

    @GET("repos/{owner}/{repo}/git/trees/{tree_sha}")
    suspend fun getTreeRecursive(
        @Header("Authorization") auth: String? = null,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("tree_sha") treeSha: String,
        @Query("recursive") recursive: Int = 1
    ): GitHubTreeResponse

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Header("Authorization") auth: String? = null,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String? = null
    ): GitHubContentResponse

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: GitHubCommitRequest
    ): GitHubCommitResponse

    @HTTP(method = "DELETE", path = "repos/{owner}/{repo}/contents/{path}", hasBody = true)
    suspend fun deleteFile(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getCommits(
        @Header("Authorization") auth: String? = null,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("sha") branch: String? = null,
        @Query("per_page") perPage: Int = 20
    ): List<GitHubCommitItem>

    @GET
    suspend fun getRawContent(
        @Url url: String,
        @Header("Authorization") auth: String? = null
    ): ResponseBody
}
