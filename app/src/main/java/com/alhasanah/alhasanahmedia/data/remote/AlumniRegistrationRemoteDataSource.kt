package com.alhasanah.alhasanahmedia.data.remote

import com.alhasanah.alhasanahmedia.BuildConfig
import com.alhasanah.alhasanahmedia.data.model.RegisterAlumniErrorResponse
import com.alhasanah.alhasanahmedia.data.model.RegisterAlumniRequest
import com.alhasanah.alhasanahmedia.data.model.RegisterAlumniResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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

class AlumniRegistrationException(
    val statusCode: Int,
    override val message: String
) : Exception(message)

class AlumniRegistrationRemoteDataSource {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client = HttpClient(Android) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun register(request: RegisterAlumniRequest): RegisterAlumniResponse {
        val response = client.post("${BuildConfig.SUPABASE_URL}/functions/v1/register-alumni") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setBody(request)
        }
        return decodeOrThrow(response)
    }

    private suspend inline fun <reified T> decodeOrThrow(response: HttpResponse): T {
        if (response.status.value in 200..299) return response.body()

        val body = response.bodyAsText()
        val backendMessage = runCatching {
            json.decodeFromString<RegisterAlumniErrorResponse>(body).error
        }.getOrNull()

        throw AlumniRegistrationException(
            statusCode = response.status.value,
            message = backendMessage ?: response.status.description
        )
    }
}
