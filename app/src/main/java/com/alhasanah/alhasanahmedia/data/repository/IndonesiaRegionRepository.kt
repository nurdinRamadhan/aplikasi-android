package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.IndonesiaRegionItem
import com.alhasanah.alhasanahmedia.data.model.IndonesiaRegionResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

interface IndonesiaRegionRepository {
    suspend fun getProvinces(): List<IndonesiaRegionItem>
    suspend fun getRegencies(provinceCode: String): List<IndonesiaRegionItem>
    suspend fun getDistricts(regencyCode: String): List<IndonesiaRegionItem>
    suspend fun getVillages(districtCode: String): List<IndonesiaRegionItem>
}

class IndonesiaRegionRepositoryImpl : IndonesiaRegionRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client = HttpClient(Android) {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 20_000
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val cache = mutableMapOf<String, List<IndonesiaRegionItem>>()

    override suspend fun getProvinces(): List<IndonesiaRegionItem> {
        return getCached("provinces") { "$BASE_URL/provinces.json" }
    }

    override suspend fun getRegencies(provinceCode: String): List<IndonesiaRegionItem> {
        return getCached("regencies:$provinceCode") { "$BASE_URL/regencies/$provinceCode.json" }
    }

    override suspend fun getDistricts(regencyCode: String): List<IndonesiaRegionItem> {
        return getCached("districts:$regencyCode") { "$BASE_URL/districts/$regencyCode.json" }
    }

    override suspend fun getVillages(districtCode: String): List<IndonesiaRegionItem> {
        return getCached("villages:$districtCode") { "$BASE_URL/villages/$districtCode.json" }
    }

    private suspend fun getCached(key: String, url: () -> String): List<IndonesiaRegionItem> {
        cache[key]?.let { return it }
        val items = client.get(url()).body<IndonesiaRegionResponse>().data
            .sortedBy { it.name.lowercase() }
        cache[key] = items
        return items
    }

    private companion object {
        const val BASE_URL = "https://wilayah.id/api"
    }
}
