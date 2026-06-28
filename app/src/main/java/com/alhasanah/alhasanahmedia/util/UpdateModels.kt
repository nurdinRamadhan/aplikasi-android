package com.alhasanah.alhasanahmedia.util

import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val body: String,
    val assets: List<GitHubAsset>,
    val published_at: String,
)

@Serializable
data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long,
)

@Serializable
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val changelog: String,
    val downloadUrl: String,
    val fileSize: Long,
    val releaseDate: String,
)

sealed interface UpdateResult {
    data class Available(val info: UpdateInfo) : UpdateResult
    object UpToDate : UpdateResult
    data class Error(val message: String) : UpdateResult
}