package com.alhasanah.alhasanahmedia.ui.qibla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.qibla.QiblaData
import com.alhasanah.alhasanahmedia.data.repository.QiblaRepository
import com.alhasanah.alhasanahmedia.util.QiblaCompassAccuracy
import com.alhasanah.alhasanahmedia.util.QiblaDeviceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

data class QiblaUiState(
    val isLoading: Boolean = true,
    val qiblaData: QiblaData? = null,
    val deviceHeading: Float = 0f,
    val compassAccuracy: QiblaCompassAccuracy = QiblaCompassAccuracy.Medium,
    val locationLabel: String = "",
    val locationAccuracyMeters: Float? = null,
    val usingFallbackLocation: Boolean = false,
    val sensorAvailable: Boolean = true,
    val errorMessage: String? = null
) {
    val needleRotation: Float
        get() = normalizeSignedDegrees((qiblaData?.direction ?: 0.0).toFloat() - deviceHeading)

    val alignmentDelta: Float
        get() = abs(needleRotation)

    val isAligned: Boolean
        get() = qiblaData != null && sensorAvailable && alignmentDelta <= 5f

    val needsCompassCalibration: Boolean
        get() = sensorAvailable &&
            compassAccuracy in setOf(QiblaCompassAccuracy.Unreliable, QiblaCompassAccuracy.Low)

    val hasPreciseUserLocation: Boolean
        get() = !usingFallbackLocation && qiblaData != null
}

class QiblaViewModel(
    private val repository: QiblaRepository,
    private val deviceManager: QiblaDeviceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    init {
        observeHeading()
        refreshQiblaDirection()
    }

    fun refreshQiblaDirection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val location = deviceManager.getCurrentLocation()

            repository.getQiblaDirection(location.latitude, location.longitude).collect { result ->
                result.onSuccess { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        qiblaData = data,
                        locationLabel = location.label,
                        locationAccuracyMeters = location.accuracyMeters,
                        usingFallbackLocation = location.isFallback,
                        errorMessage = null
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        locationLabel = location.label,
                        locationAccuracyMeters = location.accuracyMeters,
                        usingFallbackLocation = location.isFallback,
                        errorMessage = error.message ?: "Gagal mengambil arah kiblat"
                    )
                }
            }
        }
    }

    private fun observeHeading() {
        viewModelScope.launch {
            deviceManager.observeHeading().collect { heading ->
                if (heading.degrees.isNaN()) {
                    _uiState.value = _uiState.value.copy(
                        sensorAvailable = false,
                        compassAccuracy = heading.accuracy
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        deviceHeading = heading.degrees,
                        sensorAvailable = true,
                        compassAccuracy = heading.accuracy
                    )
                }
            }
        }
    }
}

private fun normalizeSignedDegrees(value: Float): Float {
    var normalized = (value + 540f) % 360f - 180f
    if (normalized < -180f) normalized += 360f
    return normalized
}
