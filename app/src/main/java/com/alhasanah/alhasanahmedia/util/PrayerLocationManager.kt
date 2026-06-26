package com.alhasanah.alhasanahmedia.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.core.content.ContextCompat
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.Locale

private val Context.prayerLocationDataStore by preferencesDataStore(name = "prayer_location_preferences")

data class PrayerLocationHint(
    val keyword: String,
    val label: String
)

data class PrayerCoordinateSnapshot(
    val latitude: Double,
    val longitude: Double,
    val keyword: String?,
    val label: String?
)

data class SavedPrayerLocation(
    val location: PrayerLocation,
    val latitude: Double?,
    val longitude: Double?
) {
    fun distanceTo(snapshot: PrayerCoordinateSnapshot): Float? {
        val lat = latitude ?: return null
        val lon = longitude ?: return null
        val result = FloatArray(1)
        Location.distanceBetween(lat, lon, snapshot.latitude, snapshot.longitude, result)
        return result.firstOrNull()
    }
}

class PrayerLocationManager(context: Context) {
    private val appContext = context.applicationContext
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    suspend fun getSavedLocation(): PrayerLocation? {
        return getSavedPrayerLocation()?.location
    }

    suspend fun getSavedPrayerLocation(): SavedPrayerLocation? {
        val preferences = appContext.prayerLocationDataStore.data.first()
        val id = preferences[SELECTED_LOCATION_ID].orEmpty()
        val label = preferences[SELECTED_LOCATION_LABEL].orEmpty()
        if (id.isBlank() || label.isBlank()) return null
        return SavedPrayerLocation(
            location = PrayerLocation(id = id, lokasi = label),
            latitude = preferences[SELECTED_LOCATION_LATITUDE],
            longitude = preferences[SELECTED_LOCATION_LONGITUDE]
        )
    }

    suspend fun saveSelectedLocation(
        location: PrayerLocation,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        appContext.prayerLocationDataStore.edit { preferences ->
            preferences[SELECTED_LOCATION_ID] = location.id
            preferences[SELECTED_LOCATION_LABEL] = location.lokasi
            if (latitude != null && longitude != null) {
                preferences[SELECTED_LOCATION_LATITUDE] = latitude
                preferences[SELECTED_LOCATION_LONGITUDE] = longitude
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLocationHint(): PrayerLocationHint? {
        val snapshot = getCurrentLocationSnapshot() ?: return null
        val keyword = snapshot.keyword ?: return null
        return PrayerLocationHint(keyword = keyword, label = snapshot.label ?: keyword)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationSnapshot(): PrayerCoordinateSnapshot? {
        if (!hasLocationPermission()) return null
        return try {
            val location = fusedLocationClient.lastLocation.await()
                ?: fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                ?: return null

            val geocoder = Geocoder(appContext, Locale.getDefault())
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()

            val keyword = address?.subAdminArea ?: address?.locality ?: address?.adminArea
            val label = listOfNotNull(address?.subAdminArea ?: address?.locality, address?.adminArea)
                .distinct()
                .joinToString(", ")
                .ifBlank { keyword.orEmpty() }

            PrayerCoordinateSnapshot(
                latitude = location.latitude,
                longitude = location.longitude,
                keyword = keyword,
                label = label.ifBlank { null }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        val SELECTED_LOCATION_ID = stringPreferencesKey("selected_location_id")
        val SELECTED_LOCATION_LABEL = stringPreferencesKey("selected_location_label")
        val SELECTED_LOCATION_LATITUDE = doublePreferencesKey("selected_location_latitude")
        val SELECTED_LOCATION_LONGITUDE = doublePreferencesKey("selected_location_longitude")
    }
}
