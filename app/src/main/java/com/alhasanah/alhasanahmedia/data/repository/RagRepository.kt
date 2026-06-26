package com.alhasanah.alhasanahmedia.data.repository

import android.content.Context
import com.alhasanah.alhasanahmedia.data.model.PublicRagRequest
import com.alhasanah.alhasanahmedia.data.model.PublicRagResponse
import com.alhasanah.alhasanahmedia.data.model.WaliRagRequest
import com.alhasanah.alhasanahmedia.data.model.WaliRagResponse
import com.alhasanah.alhasanahmedia.data.remote.RagApiException
import com.alhasanah.alhasanahmedia.data.remote.RagRemoteDataSource
import java.util.UUID

interface RagRepository {
    suspend fun askPublicPesantren(query: String): Result<PublicRagResponse>
    suspend fun askPublicKitab(query: String): Result<PublicRagResponse>
    suspend fun askWali(query: String, childRef: String? = null): Result<WaliRagResponse>
}

class RagRepositoryImpl(
    private val context: Context,
    private val remoteDataSource: RagRemoteDataSource
) : RagRepository {

    override suspend fun askPublicPesantren(query: String): Result<PublicRagResponse> {
        return askPublic(source = "pesantren", query = query)
    }

    override suspend fun askPublicKitab(query: String): Result<PublicRagResponse> {
        return askPublic(source = "kitab", query = query)
    }

    override suspend fun askWali(query: String, childRef: String?): Result<WaliRagResponse> {
        return runCatching {
            remoteDataSource.askWali(
                WaliRagRequest(
                    query = query,
                    childRef = childRef,
                    includePublicKnowledge = true
                )
            )
        }.mapFailureMessage()
    }

    private suspend fun askPublic(source: String, query: String): Result<PublicRagResponse> {
        return runCatching {
            remoteDataSource.askPublic(
                PublicRagRequest(
                    source = source,
                    query = query,
                    sessionId = getOrCreatePublicSessionId()
                )
            )
        }.mapFailureMessage()
    }

    private fun getOrCreatePublicSessionId(): String {
        val preferences = context.getSharedPreferences("rag_chat", Context.MODE_PRIVATE)
        val existing = preferences.getString(KEY_PUBLIC_SESSION_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val sessionId = "android-public-${UUID.randomUUID()}"
        preferences.edit().putString(KEY_PUBLIC_SESSION_ID, sessionId).apply()
        return sessionId
    }

    private fun <T> Result<T>.mapFailureMessage(): Result<T> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { throwable ->
                Result.failure(Exception(throwable.toUserMessage()))
            }
        )
    }

    private fun Throwable.toUserMessage(): String {
        val apiException = this as? RagApiException
        return when (apiException?.statusCode) {
            401, 403 -> "Sesi login tidak valid. Silakan login ulang."
            429 -> "Terlalu banyak pertanyaan. Coba lagi sebentar lagi."
            in 500..599 -> "AI belum dapat menjawab saat ini. Coba lagi nanti."
            else -> apiException?.message?.takeIf { it.isNotBlank() }
                ?: localizedMessage
                ?: "Layanan AI belum dapat digunakan. Coba lagi nanti."
        }
    }

    private companion object {
        const val KEY_PUBLIC_SESSION_ID = "public_session_id"
    }
}
