package com.alhasanah.alhasanahmedia.ui.santri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.AbsensiHarianItem
import com.alhasanah.alhasanahmedia.data.model.RingkasanAbsensiMingguan
import com.alhasanah.alhasanahmedia.data.repository.SantriActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

sealed interface AbsensiUiState {
    data class Success(val summary: RingkasanAbsensiMingguan) : AbsensiUiState
    object Loading : AbsensiUiState
    data class Error(val message: String) : AbsensiUiState
}

class AbsensiViewModel(
    private val repository: SantriActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AbsensiUiState>(AbsensiUiState.Loading)
    val uiState: StateFlow<AbsensiUiState> = _uiState

    private var currentNis: String? = null
    private var currentWeekStart: String? = null

    fun loadWeeklySummary(nis: String, weekStart: LocalDate? = null) {
        val startDate = weekStart ?: getCurrentWeekStart()
        currentNis = nis
        currentWeekStart = startDate.toString()
        
        viewModelScope.launch {
            _uiState.value = AbsensiUiState.Loading
            repository.getRingkasanAbsensiMingguan(nis, startDate.toString())
                .catch { e ->
                    _uiState.value = AbsensiUiState.Error("Gagal memuat ringkasan absensi: ${e.message}")
                }
                .collect { summary ->
                    _uiState.value = AbsensiUiState.Success(summary)
                }
        }
    }

    fun refresh() {
        currentNis?.let { nis ->
            currentWeekStart?.let { weekStart ->
                loadWeeklySummary(nis, LocalDate.parse(weekStart))
            }
        }
    }

    fun getCurrentWeekStart(): LocalDate {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
    }

    fun getWeekStartForDisplay(weekStart: LocalDate): String {
        val endDate = weekStart.plusDays(5)
        return "${weekStart.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"))} - ${endDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
    }

    fun getStatusColor(status: String): Long {
        return when (status.uppercase()) {
            "HADIR" -> 0xFF2E7D32L
            "ALPHA" -> 0xFFBA1A1AL
            "SAKIT" -> 0xFFF57C00L
            "IZIN" -> 0xFF1565C0L
            "SEKOLAH" -> 0xFF6A1B9AL
            "PULANG" -> 0xFF00695CL
            else -> 0xFF757575L
        }
    }

    fun getStatusLabel(status: String): String {
        return when (status.uppercase()) {
            "HADIR" -> "Hadir"
            "ALPHA" -> "Alpha"
            "SAKIT" -> "Sakit"
            "IZIN" -> "Izin"
            "SEKOLAH" -> "Sekolah"
            "PULANG" -> "Pulang"
            else -> status
        }
    }
}