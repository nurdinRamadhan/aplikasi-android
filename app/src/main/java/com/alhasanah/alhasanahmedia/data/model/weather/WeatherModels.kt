package com.alhasanah.alhasanahmedia.data.model.weather

import com.google.gson.annotations.SerializedName

data class WeatherForecastResponse(
    @SerializedName("lokasi")
    val location: BmkgLocation? = null,
    @SerializedName("data")
    val data: List<BmkgForecastData> = emptyList()
)

data class CurrentWeather(
    @SerializedName("time")
    val time: String = "",
    @SerializedName("temperature_2m")
    val temperature: Double? = null,
    @SerializedName("relative_humidity_2m")
    val humidity: Int? = null,
    @SerializedName("apparent_temperature")
    val apparentTemperature: Double? = null,
    @SerializedName("weather_code")
    val weatherCode: Int? = null,
    @SerializedName("wind_speed_10m")
    val windSpeed: Double? = null,
    @SerializedName("precipitation")
    val precipitation: Double? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("visibility")
    val visibility: String? = null,
    @SerializedName("analysis_date")
    val analysisDate: String? = null,
    @SerializedName("image")
    val imageUrl: String? = null
)

data class BmkgLocation(
    @SerializedName("adm1")
    val adm1: String? = null,
    @SerializedName("adm2")
    val adm2: String? = null,
    @SerializedName("adm3")
    val adm3: String? = null,
    @SerializedName("adm4")
    val adm4: String? = null,
    @SerializedName("provinsi")
    val province: String? = null,
    @SerializedName("kotkab")
    val city: String? = null,
    @SerializedName("kecamatan")
    val district: String? = null,
    @SerializedName("desa")
    val village: String? = null,
    @SerializedName("lon")
    val longitude: Double? = null,
    @SerializedName("lat")
    val latitude: Double? = null,
    @SerializedName("timezone")
    val timezone: String? = null
)

data class BmkgForecastData(
    @SerializedName("lokasi")
    val location: BmkgLocation? = null,
    @SerializedName("cuaca")
    val weatherGroups: List<List<BmkgWeatherPoint>> = emptyList()
)

data class BmkgWeatherPoint(
    @SerializedName("datetime")
    val datetime: String? = null,
    @SerializedName("utc_datetime")
    val utcDatetime: String? = null,
    @SerializedName("local_datetime")
    val localDatetime: String? = null,
    @SerializedName("t")
    val temperature: Double? = null,
    @SerializedName("tcc")
    val cloudCover: Int? = null,
    @SerializedName("tp")
    val precipitation: Double? = null,
    @SerializedName("weather")
    val weatherCode: Int? = null,
    @SerializedName("weather_desc")
    val weatherDescription: String? = null,
    @SerializedName("weather_desc_en")
    val weatherDescriptionEn: String? = null,
    @SerializedName("wd")
    val windDirection: String? = null,
    @SerializedName("wd_to")
    val windTo: String? = null,
    @SerializedName("ws")
    val windSpeed: Double? = null,
    @SerializedName("hu")
    val humidity: Int? = null,
    @SerializedName("vs_text")
    val visibilityText: String? = null,
    @SerializedName("analysis_date")
    val analysisDate: String? = null,
    @SerializedName("image")
    val imageUrl: String? = null
)

data class WeatherLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val isFallback: Boolean,
    val adm4: String,
    val isSavedRegion: Boolean = false
)

data class BmkgRegion(
    val adm4: String,
    val province: String,
    val city: String,
    val district: String,
    val village: String
) {
    val label: String
        get() = "$village, $district, $city"

    val fullLabel: String
        get() = "$village, $district, $city, $province"
}

data class HourlyForecastItem(
    val time: String,
    val temperature: Double?,
    val precipitationProbability: Int?,
    val weatherCode: Int?,
    val description: String? = null,
    val humidity: Int? = null,
    val windSpeed: Double? = null,
    val imageUrl: String? = null
)

data class DailyForecastItem(
    val date: String,
    val weatherCode: Int?,
    val temperatureMax: Double?,
    val temperatureMin: Double?,
    val precipitationProbability: Int?,
    val description: String? = null,
    val imageUrl: String? = null
)

data class WeatherAlertItem(
    val title: String,
    val description: String,
    val pubDate: String,
    val link: String
)
