package com.alhasanah.alhasanahmedia.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.alhasanah.alhasanahmedia.domain.falak.MarkazFalak
import com.alhasanah.alhasanahmedia.domain.falak.ZonaWaktuFalak
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

enum class SumberMarkazFalak {
    GPS,
    Peta,
    Manual,
}

data class MarkazFalakTerdeteksi(
    val markaz: MarkazFalak,
    val sumber: SumberMarkazFalak,
    val akurasiMeter: Float? = null,
    val elevasiOtomatis: Boolean,
    val catatan: String? = null,
)

class FalakMarkazProvider(
    context: Context,
    private val httpClient: OkHttpClient,
) {
    private val appContext = context.applicationContext
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val json = Json { ignoreUnknownKeys = true }

    @SuppressLint("MissingPermission")
    suspend fun deteksiMarkaz(
        namaFallback: String = "Lokasi saat ini",
        elevasiManualMeter: Double? = null,
    ): Result<MarkazFalakTerdeteksi> = withContext(Dispatchers.IO) {
        runCatching {
            check(hasLocationPermission()) { "Izin lokasi belum diberikan." }
            val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                ?: fusedLocationClient.lastLocation.await()
                ?: error("Lokasi belum tersedia.")
            val elevasi = elevasiManualMeter
                ?: elevasiDariLokasi(location.latitude, location.longitude)
                ?: if (location.hasAltitude()) location.altitude else 0.0
            MarkazFalakTerdeteksi(
                markaz = MarkazFalak(
                    nama = labelLokasi(location.latitude, location.longitude).ifBlank { namaFallback },
                    lintangDerajat = location.latitude,
                    bujurDerajat = location.longitude,
                    elevasiMeter = elevasi,
                    zonaWaktu = zonaWaktuIndonesia(location.longitude),
                ),
                sumber = SumberMarkazFalak.GPS,
                akurasiMeter = if (location.hasAccuracy()) location.accuracy else null,
                elevasiOtomatis = elevasiManualMeter == null,
                catatan = "Koordinat dari GPS/Fused Location. Elevasi dapat dikoreksi manual sebelum hisab.",
            )
        }
    }

    suspend fun markazDariPeta(
        nama: String,
        lintangDerajat: Double,
        bujurDerajat: Double,
        elevasiManualMeter: Double? = null,
    ): Result<MarkazFalakTerdeteksi> = withContext(Dispatchers.IO) {
        runCatching {
            validasiKoordinat(lintangDerajat, bujurDerajat)
            val elevasi = elevasiManualMeter ?: elevasiDariLokasi(lintangDerajat, bujurDerajat) ?: 0.0
            MarkazFalakTerdeteksi(
                markaz = MarkazFalak(
                    nama = nama.ifBlank { labelLokasi(lintangDerajat, bujurDerajat).ifBlank { "Titik peta" } },
                    lintangDerajat = lintangDerajat,
                    bujurDerajat = bujurDerajat,
                    elevasiMeter = elevasi,
                    zonaWaktu = zonaWaktuIndonesia(bujurDerajat),
                ),
                sumber = SumberMarkazFalak.Peta,
                elevasiOtomatis = elevasiManualMeter == null,
                catatan = "Koordinat dari titik peta. Elevasi dapat dikoreksi manual sebelum hisab.",
            )
        }
    }

    fun markazManual(
        nama: String,
        lintangDerajat: Double,
        bujurDerajat: Double,
        elevasiMeter: Double,
        zonaWaktu: ZonaWaktuFalak = zonaWaktuIndonesia(bujurDerajat),
    ): Result<MarkazFalakTerdeteksi> =
        runCatching {
            validasiKoordinat(lintangDerajat, bujurDerajat)
            check(elevasiMeter >= -500.0) { "Elevasi tidak wajar." }
            MarkazFalakTerdeteksi(
                markaz = MarkazFalak(
                    nama = nama.ifBlank { "Markaz manual" },
                    lintangDerajat = lintangDerajat,
                    bujurDerajat = bujurDerajat,
                    elevasiMeter = elevasiMeter,
                    zonaWaktu = zonaWaktu,
                ),
                sumber = SumberMarkazFalak.Manual,
                elevasiOtomatis = false,
                catatan = "Semua nilai markaz diisi manual.",
            )
        }

    suspend fun elevasiDariLokasi(lintangDerajat: Double, bujurDerajat: Double): Double? = withContext(Dispatchers.IO) {
        runCatching {
            validasiKoordinat(lintangDerajat, bujurDerajat)
            val url = "https://api.open-meteo.com/v1/elevation?latitude=$lintangDerajat&longitude=$bujurDerajat"
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body == null) {
                        null
                    } else {
                        val root = json.parseToJsonElement(body).jsonObject
                        root["elevation"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.doubleOrNull
                    }
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun labelLokasi(lintangDerajat: Double, bujurDerajat: Double): String {
        return try {
            val geocoder = Geocoder(appContext, Locale.getDefault())
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocation(lintangDerajat, bujurDerajat, 1)?.firstOrNull()
            listOfNotNull(
                address?.featureName,
                address?.subLocality,
                address?.locality ?: address?.subAdminArea,
                address?.adminArea
            ).distinct().joinToString(", ")
        } catch (e: Exception) {
            ""
        }
    }

    private fun validasiKoordinat(lintangDerajat: Double, bujurDerajat: Double) {
        check(lintangDerajat in -90.0..90.0) { "Lintang harus berada di antara -90° sampai 90°." }
        check(bujurDerajat in -180.0..180.0) { "Bujur harus berada di antara -180° sampai 180°." }
    }

    companion object {
        fun zonaWaktuIndonesia(bujurDerajat: Double): ZonaWaktuFalak =
            when {
                bujurDerajat >= 127.5 -> ZonaWaktuFalak.WIT
                bujurDerajat >= 112.5 -> ZonaWaktuFalak.WITA
                else -> ZonaWaktuFalak.WIB
            }
    }
}
