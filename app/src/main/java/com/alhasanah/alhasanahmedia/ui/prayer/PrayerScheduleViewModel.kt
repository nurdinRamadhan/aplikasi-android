package com.alhasanah.alhasanahmedia.ui.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerLocation
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerScheduleData
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerScheduleEntry
import com.alhasanah.alhasanahmedia.data.repository.PrayerScheduleRepository
import com.alhasanah.alhasanahmedia.util.PrayerLocationManager
import com.alhasanah.alhasanahmedia.util.PrayerReminderMode
import com.alhasanah.alhasanahmedia.util.PrayerReminderScheduler
import com.alhasanah.alhasanahmedia.util.PrayerReminderSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrayerScheduleUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val detectedLocationLabel: String? = null,
    val locations: List<PrayerLocation> = emptyList(),
    val selectedLocation: PrayerLocation? = null,
    val scheduleData: PrayerScheduleData? = null,
    val isOfflineData: Boolean = false,
    val lastUpdatedAt: Long? = null,
    val cacheNotice: String? = null,
    val errorMessage: String? = null
) {
    val todaySchedule: PrayerScheduleEntry?
        get() = scheduleData?.jadwal?.values?.firstOrNull()
}

class PrayerScheduleViewModel(
    private val repository: PrayerScheduleRepository,
    private val locationManager: PrayerLocationManager,
    private val reminderScheduler: PrayerReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerScheduleUiState(isLoading = true))
    val uiState: StateFlow<PrayerScheduleUiState> = _uiState.asStateFlow()

    private val _reminderSettings = MutableStateFlow(PrayerReminderSettings())
    val reminderSettings: StateFlow<PrayerReminderSettings> = _reminderSettings.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeReminderSettings()
        detectLocationAndLoad()
    }

    private fun observeReminderSettings() {
        viewModelScope.launch {
            reminderScheduler.settingsFlow.collect { settings ->
                _reminderSettings.value = settings
                rescheduleReminderIfPossible(settings)
            }
        }
    }

    fun detectLocationAndLoad() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, cacheNotice = null, errorMessage = null)
            locationManager.getSavedLocation()?.let { savedLocation ->
                _uiState.value = _uiState.value.copy(
                    searchQuery = savedLocation.lokasi,
                    selectedLocation = savedLocation
                )
                loadSchedule(savedLocation, persistSelection = false)
                return@launch
            }

            val hint = locationManager.getLocationHint()
            if (hint == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Lokasi belum tersedia. Cari kota atau kabupaten secara manual."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                detectedLocationLabel = hint.label,
                searchQuery = hint.keyword
            )
            searchLocations(hint.keyword, autoSelectFirst = true)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()

        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.value = _uiState.value.copy(locations = emptyList(), errorMessage = null)
            return
        }

        searchJob = viewModelScope.launch {
            delay(350)
            searchLocations(trimmed, autoSelectFirst = false)
        }
    }

    fun searchLocations(keyword: String = _uiState.value.searchQuery, autoSelectFirst: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, cacheNotice = null, errorMessage = null)
            repository.searchLocations(keyword).collect { result ->
                result.onSuccess { resource ->
                    val locations = resource.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        locations = locations,
                        isOfflineData = resource.isFromCache,
                        lastUpdatedAt = resource.updatedAt,
                        cacheNotice = resource.notice,
                        errorMessage = if (locations.isEmpty()) "Lokasi tidak ditemukan" else null
                    )
                    if (autoSelectFirst && locations.isNotEmpty()) {
                        selectLocation(locations.first())
                    }
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        locations = emptyList(),
                        errorMessage = error.message ?: "Lokasi tidak ditemukan"
                    )
                }
            }
        }
    }

    fun selectLocation(location: PrayerLocation) {
        loadSchedule(location, persistSelection = true)
    }

    private fun loadSchedule(location: PrayerLocation, persistSelection: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedLocation = location,
                cacheNotice = null,
                errorMessage = null
            )
            if (persistSelection) {
                locationManager.saveSelectedLocation(location)
            }
            repository.getTodaySchedule(location.id).collect { result ->
                result.onSuccess { resource ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        scheduleData = resource.data,
                        isOfflineData = resource.isFromCache,
                        lastUpdatedAt = resource.updatedAt,
                        cacheNotice = resource.notice,
                        errorMessage = null
                    )
                    rescheduleReminderIfPossible(_reminderSettings.value)
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Jadwal sholat tidak ditemukan"
                    )
                }
            }
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        updateReminderSettings(_reminderSettings.value.copy(enabled = enabled))
    }

    fun setReminderMode(mode: PrayerReminderMode) {
        updateReminderSettings(_reminderSettings.value.copy(mode = mode, enabled = true))
    }

    fun setReminderOffset(minutes: Int) {
        updateReminderSettings(_reminderSettings.value.copy(minutesBefore = minutes.coerceIn(0, 30)))
    }

    fun toggleReminderPrayer(name: String) {
        val current = _reminderSettings.value
        val nextPrayers = if (name in current.enabledPrayers) {
            current.enabledPrayers - name
        } else {
            current.enabledPrayers + name
        }
        updateReminderSettings(current.copy(enabledPrayers = nextPrayers))
    }

    private fun updateReminderSettings(settings: PrayerReminderSettings) {
        viewModelScope.launch {
            reminderScheduler.saveSettings(settings)
            _reminderSettings.value = settings
            rescheduleReminderIfPossible(settings)
        }
    }

    private fun rescheduleReminderIfPossible(settings: PrayerReminderSettings) {
        val state = _uiState.value
        val schedule = state.todaySchedule ?: return
        val location = listOf(state.scheduleData?.kabko, state.scheduleData?.prov)
            .filterNot { it.isNullOrBlank() }
            .joinToString(", ")
            .ifBlank { state.selectedLocation?.lokasi.orEmpty() }
        reminderScheduler.rescheduleToday(schedule, settings, location)
    }
}
