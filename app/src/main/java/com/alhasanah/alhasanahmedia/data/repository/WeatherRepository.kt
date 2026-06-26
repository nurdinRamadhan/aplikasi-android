package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.weather.WeatherForecastResponse
import com.alhasanah.alhasanahmedia.data.model.weather.WeatherAlertItem
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    fun getForecast(adm4: String): Flow<Result<WeatherForecastResponse>>
    fun getNowcastAlerts(): Flow<Result<List<WeatherAlertItem>>>
}
