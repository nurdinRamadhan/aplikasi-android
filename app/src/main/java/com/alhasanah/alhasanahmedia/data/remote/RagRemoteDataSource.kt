package com.alhasanah.alhasanahmedia.data.remote

import com.alhasanah.alhasanahmedia.BuildConfig
import com.alhasanah.alhasanahmedia.data.model.PublicRagRequest
import com.alhasanah.alhasanahmedia.data.model.PublicRagResponse
import com.alhasanah.alhasanahmedia.data.model.RagErrorResponse
import com.alhasanah.alhasanahmedia.data.model.WaliRagRequest
import com.alhasanah.alhasanahmedia.data.model.WaliRagResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class RagApiException(
    val statusCode: Int,
    override val message: String
) : Exception(message)

class RagRemoteDataSource(
    private val supabaseClient: SupabaseClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client = HttpClient(Android) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun askPublic(request: PublicRagRequest): PublicRagResponse {
        val response = client.post(functionUrl("rag-query-public")) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setBody(request)
        }
        return decodeOrThrow(response)
    }

    suspend fun askWali(request: WaliRagRequest): WaliRagResponse {
        supabaseClient.auth.awaitInitialization()
        val accessToken = supabaseClient.auth.currentAccessTokenOrNull()
            ?: throw RagApiException(401, "Sesi login tidak valid. Silakan login ulang.")

        val response = client.post(functionUrl("rag-query-wali")) {
            contentType(ContentType.Application.Json)
            bearerAuth(accessToken)
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setBody(request)
        }
        return decodeOrThrow(response)
    }

    private fun functionUrl(functionName: String): String {
        return "${BuildConfig.SUPABASE_URL}/functions/v1/$functionName"
    }

    private suspend inline fun <reified T> decodeOrThrow(response: HttpResponse): T {
        if (response.status.value in 200..299) return response.body()

        val body = response.bodyAsText()
        val backendMessage = runCatching {
            json.decodeFromString<RagErrorResponse>(body).error
        }.getOrNull()

        throw RagApiException(
            statusCode = response.status.value,
            message = backendMessage ?: response.status.description
        )
    }
}
