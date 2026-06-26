package com.alhasanah.alhasanahmedia.data.remote.islamiccalendar

import com.alhasanah.alhasanahmedia.data.model.islamiccalendar.CalendarResponse
import com.alhasanah.alhasanahmedia.data.model.islamiccalendar.HolidayResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface IslamicCalendarApiService {
    @GET("cal/today")
    suspend fun getTodayCalendar(
        @Query("adj") adjustment: Int = 0,
        @Query("method") method: String = "standar",
        @Query("tz") timezone: String = "Asia/Jakarta"
    ): CalendarResponse

    @GET("cal/hijr/{date}")
    suspend fun convertCeToHijri(
        @Path("date") date: String,
        @Query("adj") adjustment: Int = 0,
        @Query("method") method: String = "standar",
        @Query("tz") timezone: String = "Asia/Jakarta"
    ): CalendarResponse

    @GET("cal/holidays")
    suspend fun getHolidays(
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null,
        @Query("day") day: Int? = null
    ): HolidayResponse
}
