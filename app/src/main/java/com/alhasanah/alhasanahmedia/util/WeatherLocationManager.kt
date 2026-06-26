package com.alhasanah.alhasanahmedia.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.core.content.ContextCompat
import com.alhasanah.alhasanahmedia.data.model.weather.BmkgRegion
import com.alhasanah.alhasanahmedia.data.model.weather.WeatherLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.Locale

private val Context.weatherRegionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "weather_region_preferences"
)

class WeatherLocationManager(context: Context) {
    private val appContext = context.applicationContext
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    private val fallbackLatitude = -7.3333
    private val fallbackLongitude = 108.2167
    private val fallbackAdm4 = "32.78.04.1003"

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): WeatherLocation {
        getSavedRegion()?.let { region ->
            return WeatherLocation(
                latitude = fallbackLatitude,
                longitude = fallbackLongitude,
                label = region.fullLabel,
                isFallback = false,
                adm4 = region.adm4,
                isSavedRegion = true
            )
        }

        if (!hasLocationPermission()) {
            return fallbackLocation("Izin lokasi belum diberikan")
        }

        return try {
            val location = fusedLocationClient.lastLocation.await()
                ?: fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()

            if (location != null) {
                WeatherLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    label = resolveLocationLabel(location.latitude, location.longitude),
                    isFallback = false,
                    adm4 = resolveAdm4(location.latitude, location.longitude),
                    isSavedRegion = false
                )
            } else {
                fallbackLocation("Lokasi belum tersedia")
            }
        } catch (e: Exception) {
            fallbackLocation("Lokasi belum tersedia")
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun fallbackLocation(reason: String): WeatherLocation =
        WeatherLocation(
            latitude = fallbackLatitude,
            longitude = fallbackLongitude,
            label = "$reason. Menggunakan koordinat cadangan Tasikmalaya.",
            isFallback = true,
            adm4 = fallbackAdm4,
            isSavedRegion = false
        )

    suspend fun saveRegion(region: BmkgRegion) {
        appContext.weatherRegionDataStore.edit { preferences ->
            preferences[ADM4_KEY] = region.adm4
            preferences[REGION_LABEL_KEY] = region.fullLabel
        }
    }

    fun searchRegions(query: String): List<BmkgRegion> =
        BmkgAdm4Catalog.search(query)

    suspend fun getSavedRegion(): BmkgRegion? {
        val preferences = appContext.weatherRegionDataStore.data.first()
        val adm4 = preferences[ADM4_KEY]
        return adm4?.let { code ->
            BmkgAdm4Catalog.findByAdm4(code)
                ?: BmkgRegion(code, "", "", "", preferences[REGION_LABEL_KEY] ?: "Wilayah tersimpan")
        }
    }

    private fun resolveLocationLabel(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(appContext, Locale.getDefault())
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            val city = address?.subAdminArea ?: address?.locality
            val province = address?.adminArea
            listOfNotNull(city, province).distinct().joinToString(", ").ifBlank {
                "%.4f, %.4f".format(Locale.US, latitude, longitude)
            }
        } catch (e: Exception) {
            "%.4f, %.4f".format(Locale.US, latitude, longitude)
        }
    }

    private fun resolveAdm4(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(appContext, Locale.getDefault())
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            val text = listOfNotNull(
                address?.subLocality,
                address?.locality,
                address?.subAdminArea,
                address?.adminArea
            ).joinToString(" ").lowercase(Locale.getDefault())

            when {
                "kemayoran" in text || "jakarta pusat" in text -> "31.71.03.1001"
                "tasikmalaya" in text || "indihiang" in text -> "32.78.04.1003"
                else -> fallbackAdm4
            }
        } catch (e: Exception) {
            fallbackAdm4
        }
    }

    private companion object {
        val ADM4_KEY = stringPreferencesKey("selected_weather_adm4")
        val REGION_LABEL_KEY = stringPreferencesKey("selected_weather_region_label")
    }
}
