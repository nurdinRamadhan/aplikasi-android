package com.alhasanah.alhasanahmedia.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.weather.BmkgRegion
import com.alhasanah.alhasanahmedia.data.model.weather.CurrentWeather
import com.alhasanah.alhasanahmedia.data.model.weather.DailyForecastItem
import com.alhasanah.alhasanahmedia.data.model.weather.HourlyForecastItem
import com.alhasanah.alhasanahmedia.data.model.weather.WeatherForecastResponse
import com.alhasanah.alhasanahmedia.data.model.weather.WeatherAlertItem
import com.alhasanah.alhasanahmedia.data.repository.WeatherRepository
import com.alhasanah.alhasanahmedia.util.WeatherLocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeatherUiState(
    val isLoading: Boolean = true,
    val locationLabel: String = "",
    val usingFallbackLocation: Boolean = false,
    val current: CurrentWeather? = null,
    val hourly: List<HourlyForecastItem> = emptyList(),
    val daily: List<DailyForecastItem> = emptyList(),
    val alerts: List<WeatherAlertItem> = emptyList(),
    val sourceLabel: String = "BMKG",
    val adm4: String = "",
    val isSavedRegion: Boolean = false,
    val isRegionPickerVisible: Boolean = false,
    val regionQuery: String = "",
    val regionResults: List<BmkgRegion> = emptyList(),
    val errorMessage: String? = null
)

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val locationManager: WeatherLocationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        refreshWeather()
    }

    fun refreshWeather() {
        viewModelScope.launch {
            runCatching {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                val location = locationManager.getCurrentLocation()
                var alerts: List<WeatherAlertItem> = emptyList()
                repository.getNowcastAlerts().collect { result ->
                    alerts = result.getOrNull().orEmpty().take(3)
                }
                repository.getForecast(location.adm4).collect { result ->
                    result.onSuccess { forecast ->
                        val bmkgLocation = forecast.location ?: forecast.data.firstOrNull()?.location
                        val locationName = listOfNotNull(
                            bmkgLocation?.village,
                            bmkgLocation?.district,
                            bmkgLocation?.city
                        ).distinct().joinToString(", ")
                        _uiState.value = WeatherUiState(
                            isLoading = false,
                            locationLabel = locationName.ifBlank { location.label },
                            usingFallbackLocation = location.isFallback,
                            current = forecast.toCurrentWeather(),
                            hourly = forecast.toHourlyItems().take(12),
                            daily = forecast.toDailyItems(),
                            alerts = alerts,
                            adm4 = location.adm4,
                            isSavedRegion = location.isSavedRegion,
                            regionResults = locationManager.searchRegions(""),
                            errorMessage = null
                        )
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            locationLabel = location.label,
                            usingFallbackLocation = location.isFallback,
                            alerts = alerts,
                            adm4 = location.adm4,
                            isSavedRegion = location.isSavedRegion,
                            regionResults = locationManager.searchRegions(""),
                            errorMessage = error.message ?: "Gagal mengambil data cuaca"
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    regionResults = locationManager.searchRegions(""),
                    errorMessage = error.message ?: "Cuaca belum bisa dibuka"
                )
            }
        }
    }

    fun showRegionPicker() {
        _uiState.value = _uiState.value.copy(
            isRegionPickerVisible = true,
            regionResults = locationManager.searchRegions(_uiState.value.regionQuery)
        )
    }

    fun hideRegionPicker() {
        _uiState.value = _uiState.value.copy(isRegionPickerVisible = false)
    }

    fun onRegionQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            regionQuery = query,
            regionResults = locationManager.searchRegions(query)
        )
    }

    fun selectRegion(region: BmkgRegion) {
        viewModelScope.launch {
            locationManager.saveRegion(region)
            _uiState.value = _uiState.value.copy(
                isRegionPickerVisible = false,
                regionQuery = "",
                regionResults = locationManager.searchRegions(""),
                locationLabel = region.fullLabel,
                adm4 = region.adm4,
                isSavedRegion = true
            )
            refreshWeather()
        }
    }
}

private fun WeatherForecastResponse.flattenPoints() =
    data.flatMap { forecastData -> forecastData.weatherGroups.flatten() }
        .sortedBy { it.localDatetime ?: it.utcDatetime ?: it.datetime.orEmpty() }

private fun WeatherForecastResponse.toCurrentWeather(): CurrentWeather? {
    val point = flattenPoints().firstOrNull() ?: return null
    return CurrentWeather(
        time = point.localDatetime ?: point.utcDatetime ?: point.datetime.orEmpty(),
        temperature = point.temperature,
        humidity = point.humidity,
        apparentTemperature = point.temperature,
        weatherCode = point.weatherCode,
        windSpeed = point.windSpeed,
        precipitation = point.precipitation,
        description = point.weatherDescription,
        visibility = point.visibilityText,
        analysisDate = point.analysisDate,
        imageUrl = point.imageUrl
    )
}

private fun WeatherForecastResponse.toHourlyItems(): List<HourlyForecastItem> {
    return flattenPoints().map { point ->
        HourlyForecastItem(
            time = point.localDatetime ?: point.utcDatetime ?: point.datetime.orEmpty(),
            temperature = point.temperature,
            precipitationProbability = point.precipitation?.times(100)?.toInt(),
            weatherCode = point.weatherCode,
            description = point.weatherDescription,
            humidity = point.humidity,
            windSpeed = point.windSpeed,
            imageUrl = point.imageUrl
        )
    }
}

private fun WeatherForecastResponse.toDailyItems(): List<DailyForecastItem> {
    return flattenPoints()
        .groupBy { (it.localDatetime ?: it.utcDatetime ?: it.datetime.orEmpty()).take(10) }
        .filterKeys { it.isNotBlank() }
        .map { (date, points) ->
            val main = points.maxByOrNull { (it.precipitation ?: 0.0) + ((it.cloudCover ?: 0) / 100.0) }
                ?: points.first()
        DailyForecastItem(
            date = date,
            weatherCode = main.weatherCode,
            temperatureMax = points.mapNotNull { it.temperature }.maxOrNull(),
            temperatureMin = points.mapNotNull { it.temperature }.minOrNull(),
            precipitationProbability = points.mapNotNull { it.precipitation?.times(100)?.toInt() }.maxOrNull(),
            description = main.weatherDescription,
            imageUrl = main.imageUrl
        )
    }.take(3)
}
