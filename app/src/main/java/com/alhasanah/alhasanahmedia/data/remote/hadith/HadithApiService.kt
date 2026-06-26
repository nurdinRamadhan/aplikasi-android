package com.alhasanah.alhasanahmedia.data.remote.hadith

import com.alhasanah.alhasanahmedia.data.model.hadith.HadithDetailResponse
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithExploreResponse
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HadithApiService {
    @GET("hadis/enc/explore")
    suspend fun exploreHadith(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): HadithExploreResponse

    @GET("hadis/enc/cari/{keyword}")
    suspend fun searchHadith(
        @Path("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): HadithSearchResponse

    @GET("hadis/enc/show/{id}")
    suspend fun getHadithDetail(
        @Path("id") id: Int
    ): HadithDetailResponse

    @GET("hadis/enc/next/{id}")
    suspend fun getNextHadith(
        @Path("id") id: Int
    ): HadithDetailResponse

    @GET("hadis/enc/prev/{id}")
    suspend fun getPreviousHadith(
        @Path("id") id: Int
    ): HadithDetailResponse
}
