package com.alhasanah.alhasanahmedia.data.remote.weather

import com.alhasanah.alhasanahmedia.data.model.weather.WeatherForecastResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface WeatherApiService {
    @GET("publik/prakiraan-cuaca")
    suspend fun getForecast(
        @Query("adm4") adm4: String
    ): WeatherForecastResponse

    @GET
    suspend fun getNowcastAlerts(
        @Url url: String = "https://www.bmkg.go.id/alerts/nowcast/id"
    ): ResponseBody
}
