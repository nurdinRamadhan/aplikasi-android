package com.alhasanah.alhasanahmedia.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.alhasanah.alhasanahmedia.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object UpdateChecker {

    private const val GITHUB_API =
        "https://api.github.com/repos/nurdinRamadhan/aplikasi-android/releases/latest"
    private const val USER_AGENT = "AlhasanahMedia-Android"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ────────────────────────────────────────────────────────────────────────
    // Public API
    // ────────────────────────────────────────────────────────────────────────

    /** Cek update async, non-blocking. Panggil dari UI scope. */
    fun checkUpdate(onResult: (UpdateResult) -> Unit) {
        scope.launch { onResult(checkUpdateAsync()) }
    }

    suspend fun checkUpdateAsync(): UpdateResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(GITHUB_API)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@use UpdateResult.Error("Gagal cek update: ${response.code}")
            }

            response.body?.use { body ->
                val release = json.decodeFromString<GitHubRelease>(body.string())
                parseRelease(release)
            } ?: UpdateResult.Error("Response body kosong")
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Internal
    // ────────────────────────────────────────────────────────────────────────

    private fun parseRelease(release: GitHubRelease): UpdateResult {
        val tag = release.tag_name.removePrefix("v")
        val parts = tag.split(".").map { it.toIntOrNull() ?: 0 }
        val (major, minor, patch) = (parts + listOf(0, 0, 0)).take(3)
        val remoteVersionCode = major * 10000 + minor * 100 + patch

        // Parse local versionName (misal "1.0.4") → versionCode yang sama
        val localParts = BuildConfig.VERSION_NAME.split(".").map { it.toIntOrNull() ?: 0 }
        val (lMajor, lMinor, lPatch) = (localParts + listOf(0, 0, 0)).take(3)
        val localVersionCode = lMajor * 10000 + lMinor * 100 + lPatch

        if (remoteVersionCode <= localVersionCode) {
            return UpdateResult.UpToDate
        }

        val apkAsset = release.assets.find { it.name.endsWith(".apk") }
            ?: return UpdateResult.Error("Asset APK tidak ditemukan di release")

        val info = UpdateInfo(
            versionName = tag,
            versionCode = remoteVersionCode,
            changelog = release.body.ifBlank { "Tidak ada catatan rilis" },
            downloadUrl = apkAsset.browser_download_url,
            fileSize = apkAsset.size,
            releaseDate = release.published_at,
        )
        return UpdateResult.Available(info)
    }

    // ────────────────────────────────────────────────────────────────────────
    // Download & Install
    // ────────────────────────────────────────────────────────────────────────

    /** Unduh APK dengan progress throttling (max 10 callback/detik). */
    suspend fun downloadApk(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "updates/AlhasanahMedia-v${info.versionName}.apk")
        file.parentFile?.mkdirs()
        val tmpFile = File(file.path + ".part")

        var lastProgressTime = 0L
        var lastProgressValue = -1f

        val request = Request.Builder()
            .url(info.downloadUrl)
            .header("User-Agent", USER_AGENT)
            .build()

        return@withContext client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w("UpdateChecker", "Download gagal: ${response.code}")
                tmpFile.delete()
                return@use null
            }

            response.body?.use { responseBody ->
                val inputStream = responseBody.byteStream()
                val outputStream = FileOutputStream(tmpFile)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                val contentLength = responseBody.contentLength()

                try {
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        if (contentLength > 0) {
                            val now = System.currentTimeMillis()
                            val progress = totalRead.toFloat() / contentLength
                            // Throttle: max 10x/detik & min 1% change
                            if (now - lastProgressTime >= 100 && progress - lastProgressValue >= 0.01f) {
                                lastProgressTime = now
                                lastProgressValue = progress
                                withContext(Dispatchers.Main) { onProgress(progress) }
                            }
                        }
                    }
                } finally {
                    inputStream.close()
                    outputStream.close()
                }

                // Final progress = 1.0
                withContext(Dispatchers.Main) { onProgress(1f) }

                // Rename .part → .apk
                if (tmpFile.renameTo(file)) file else null
            } ?: run {
                Log.w("UpdateChecker", "Response body null")
                tmpFile.delete()
                null
            }
        }
    }

    fun installApk(context: Context, file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /** Check if a downloaded APK exists for a given version. */
    fun getDownloadedApk(context: Context, versionName: String): File? {
        val file = File(context.filesDir, "updates/AlhasanahMedia-v$versionName.apk")
        return if (file.exists()) file else null
    }

    /** Find any pending downloaded APK file. */
    fun findDownloadedApk(context: Context): File? {
        val updatesDir = File(context.filesDir, "updates")
        if (!updatesDir.exists()) return null
        return updatesDir.listFiles()?.find { it.name.endsWith(".apk") && it.length() > 0 }
    }

    fun cancelCheck() {
        scope.cancel()
    }
}