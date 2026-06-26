package com.alhasanah.alhasanahmedia.data.remote.devotion

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface AhmadSanusiApiService {
    @GET("v1/doa/kategori")
    suspend fun getDevotionCategories(
        @Header("X-API-Key") apiKey: String
    ): JsonObject

    @GET("v1/doa/kategori/{slug}")
    suspend fun getDevotionsByCategory(
        @Header("X-API-Key") apiKey: String,
        @Path("slug") slug: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): JsonObject

    @GET("v1/doa/search")
    suspend fun searchDevotions(
        @Header("X-API-Key") apiKey: String,
        @Query("q") keyword: String,
        @Query("limit") limit: Int = 50
    ): JsonObject

    @GET("v1/doa/{doa_id}")
    suspend fun getDevotionDetail(
        @Header("X-API-Key") apiKey: String,
        @Path("doa_id") id: Int
    ): JsonObject

    @GET("v1/kitab")
    suspend fun getKitabBooks(
        @Header("X-API-Key") apiKey: String,
        @Query("q") keyword: String? = null,
        @Query("kategori") category: String? = null
    ): JsonObject

    @GET("v1/kitab/kategori")
    suspend fun getKitabCategories(
        @Header("X-API-Key") apiKey: String
    ): JsonObject

    @GET("v1/kitab/{slug}")
    suspend fun getKitabChapters(
        @Header("X-API-Key") apiKey: String,
        @Path("slug") slug: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 200
    ): JsonObject

    @GET("v1/kitab/{slug}/bab/{nomor}")
    suspend fun getKitabChapterDetail(
        @Header("X-API-Key") apiKey: String,
        @Path("slug") slug: String,
        @Path("nomor") number: Int
    ): JsonObject

    @GET("v1/kitab/{slug}/search")
    suspend fun searchKitabChapters(
        @Header("X-API-Key") apiKey: String,
        @Path("slug") slug: String,
        @Query("q") keyword: String
    ): JsonObject
}
