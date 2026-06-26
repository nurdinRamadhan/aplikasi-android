package com.alhasanah.alhasanahmedia.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class QiblaLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val isFallback: Boolean,
    val accuracyMeters: Float? = null
)

enum class QiblaCompassAccuracy {
    Unavailable,
    Unreliable,
    Low,
    Medium,
    High
}

data class QiblaHeading(
    val degrees: Float,
    val accuracy: QiblaCompassAccuracy
)

class QiblaDeviceManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val fallbackLatitude = -7.3333
    private val fallbackLongitude = 108.2167

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): QiblaLocation {
        if (!hasLocationPermission()) {
            return fallbackLocation("Izin lokasi belum diberikan")
        }

        return try {
            val location = fusedLocationClient.lastLocation.await()
                ?: fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()

            if (location != null) {
                QiblaLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    label = resolveLocationLabel(location),
                    isFallback = false,
                    accuracyMeters = if (location.hasAccuracy()) location.accuracy else null
                )
            } else {
                fallbackLocation("Lokasi belum tersedia")
            }
        } catch (e: Exception) {
            fallbackLocation("Lokasi belum tersedia")
        }
    }

    fun observeHeading(): Flow<QiblaHeading> = callbackFlow {
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationVector == null && (accelerometer == null || magnetometer == null)) {
            trySend(QiblaHeading(Float.NaN, QiblaCompassAccuracy.Unavailable))
            close()
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeomagnetic = false
        var compassAccuracy = QiblaCompassAccuracy.Medium
        var smoothedHeading: Float? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                compassAccuracy = when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR,
                    Sensor.TYPE_MAGNETIC_FIELD -> event.accuracy.toQiblaCompassAccuracy()
                    else -> compassAccuracy
                }

                val heading = when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        azimuthToDegrees(orientation[0])
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        lowPass(event.values, gravity)
                        hasGravity = true
                        headingFromFallbackSensors(
                            hasGravity = hasGravity,
                            hasGeomagnetic = hasGeomagnetic,
                            gravity = gravity,
                            geomagnetic = geomagnetic,
                            rotationMatrix = rotationMatrix,
                            orientation = orientation
                        )
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        lowPass(event.values, geomagnetic)
                        hasGeomagnetic = true
                        headingFromFallbackSensors(
                            hasGravity = hasGravity,
                            hasGeomagnetic = hasGeomagnetic,
                            gravity = gravity,
                            geomagnetic = geomagnetic,
                            rotationMatrix = rotationMatrix,
                            orientation = orientation
                        )
                    }
                    else -> null
                }

                if (heading != null) {
                    smoothedHeading = smoothHeading(
                        previous = smoothedHeading,
                        raw = heading,
                        accuracy = compassAccuracy
                    )
                    smoothedHeading?.let {
                        trySend(QiblaHeading(it, compassAccuracy))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR || sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    compassAccuracy = accuracy.toQiblaCompassAccuracy()
                }
            }
        }

        if (rotationVector != null) {
            sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun fallbackLocation(reason: String): QiblaLocation =
        QiblaLocation(
            latitude = fallbackLatitude,
            longitude = fallbackLongitude,
            label = "$reason. Menggunakan koordinat cadangan Tasikmalaya.",
            isFallback = true,
            accuracyMeters = null
        )

    private fun resolveLocationLabel(location: Location): String {
        return try {
            val geocoder = Geocoder(appContext, Locale.getDefault())
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
            val city = address?.subAdminArea ?: address?.locality
            val country = address?.countryName
            listOfNotNull(city, country).joinToString(", ").ifBlank {
                "${location.latitude.formatCoordinate()}, ${location.longitude.formatCoordinate()}"
            }
        } catch (e: Exception) {
            "${location.latitude.formatCoordinate()}, ${location.longitude.formatCoordinate()}"
        }
    }

    private fun headingFromFallbackSensors(
        hasGravity: Boolean,
        hasGeomagnetic: Boolean,
        gravity: FloatArray,
        geomagnetic: FloatArray,
        rotationMatrix: FloatArray,
        orientation: FloatArray
    ): Float? {
        if (!hasGravity || !hasGeomagnetic) return null
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
        if (!success) return null
        SensorManager.getOrientation(rotationMatrix, orientation)
        return azimuthToDegrees(orientation[0])
    }

    private fun azimuthToDegrees(azimuthRadians: Float): Float =
        ((Math.toDegrees(azimuthRadians.toDouble()).toFloat() + 360f) % 360f)

    private fun smoothHeading(previous: Float?, raw: Float, accuracy: QiblaCompassAccuracy): Float {
        if (previous == null) return raw
        val delta = shortestAngleDelta(previous, raw)
        val absDelta = abs(delta)

        if (absDelta < HEADING_DEADBAND_DEGREES) return previous
        if (accuracy in setOf(QiblaCompassAccuracy.Unreliable, QiblaCompassAccuracy.Low) && absDelta > LOW_ACCURACY_JUMP_LIMIT_DEGREES) {
            return previous
        }

        val alpha = when (accuracy) {
            QiblaCompassAccuracy.High -> 0.18f
            QiblaCompassAccuracy.Medium -> 0.13f
            QiblaCompassAccuracy.Low -> 0.08f
            QiblaCompassAccuracy.Unreliable -> 0.05f
            QiblaCompassAccuracy.Unavailable -> 0.05f
        }
        return normalizeDegrees(previous + delta * alpha)
    }

    private fun shortestAngleDelta(from: Float, to: Float): Float {
        var delta = (to - from + 540f) % 360f - 180f
        if (delta < -180f) delta += 360f
        return delta
    }

    private fun normalizeDegrees(value: Float): Float {
        var normalized = value % 360f
        if (normalized < 0f) normalized += 360f
        return normalized
    }

    private fun lowPass(input: FloatArray, output: FloatArray) {
        val alpha = 0.12f
        for (i in output.indices) {
            output[i] += alpha * (input[i] - output[i])
        }
    }

    private fun Double.formatCoordinate(): String =
        (this * 100000.0).roundToInt().div(100000.0).toString()

    private fun Int.toQiblaCompassAccuracy(): QiblaCompassAccuracy =
        when (this) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> QiblaCompassAccuracy.High
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> QiblaCompassAccuracy.Medium
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> QiblaCompassAccuracy.Low
            SensorManager.SENSOR_STATUS_UNRELIABLE -> QiblaCompassAccuracy.Unreliable
            else -> QiblaCompassAccuracy.Medium
        }

    private companion object {
        const val HEADING_DEADBAND_DEGREES = 0.65f
        const val LOW_ACCURACY_JUMP_LIMIT_DEGREES = 38f
    }
}
