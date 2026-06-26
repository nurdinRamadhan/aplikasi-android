package com.alhasanah.alhasanahmedia.data.remote.quran

import com.alhasanah.alhasanahmedia.data.model.quran.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface QuranApiService {
    @GET("quran")
    suspend fun getAllSurah(): SurahListResponse

    @GET("quran/{nomor}")
    suspend fun getSurahDetail(
        @Path("nomor") nomor: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): SurahDetailResponse

    @GET("quran/juz/{nomor}")
    suspend fun getJuz(
        @Path("nomor") nomor: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): JuzResponse

    @GET("quran/{nomor_surah}/{nomor_ayat}")
    suspend fun getAyatDetail(
        @Path("nomor_surah") nomorSurah: Int,
        @Path("nomor_ayat") nomorAyat: Int
    ): AyahDetailResponse
}
