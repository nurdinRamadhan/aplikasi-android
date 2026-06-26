package com.alhasanah.alhasanahmedia.data.remote.prayer

import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerLocationResponse
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerScheduleResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PrayerScheduleApiService {
    @GET("sholat/kabkota/cari/{keyword}")
    suspend fun searchLocations(
        @Path("keyword") keyword: String
    ): PrayerLocationResponse

    @GET("sholat/jadwal/{id}/today")
    suspend fun getTodaySchedule(
        @Path("id") id: String,
        @Query("tz") timezone: String = "Asia/Jakarta"
    ): PrayerScheduleResponse

    @GET("sholat/jadwal/{id}/{period}")
    suspend fun getScheduleByPeriod(
        @Path("id") id: String,
        @Path("period") period: String
    ): PrayerScheduleResponse
}
