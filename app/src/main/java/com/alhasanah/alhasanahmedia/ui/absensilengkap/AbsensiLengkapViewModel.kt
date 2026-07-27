package com.alhasanah.alhasanahmedia.ui.absensilengkap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.AbsensiLengkapResponse
import com.alhasanah.alhasanahmedia.data.model.HariAbsensi
import com.alhasanah.alhasanahmedia.data.model.KegiatanHarian
import com.alhasanah.alhasanahmedia.data.model.QuickFilter
import com.alhasanah.alhasanahmedia.data.model.ViewMode
import com.alhasanah.alhasanahmedia.data.repository.AbsensiLengkapRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed interface AbsensiLengkapUiState {
    data object Loading : AbsensiLengkapUiState
    data class Success(val data: AbsensiLengkapResponse) : AbsensiLengkapUiState
    data class Error(val message: String) : AbsensiLengkapUiState
}

class AbsensiLengkapViewModel(
    private val repository: AbsensiLengkapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AbsensiLengkapUiState>(AbsensiLengkapUiState.Loading)
    val uiState: StateFlow<AbsensiLengkapUiState> = _uiState.asStateFlow()

    private var rawResponse: AbsensiLengkapResponse? = null

    private val _currentNis = MutableStateFlow("")
    private val _currentStartDate = MutableStateFlow(LocalDate.now().minusDays(6))
    private val _currentEndDate = MutableStateFlow(LocalDate.now())
    private val _quickFilter = MutableStateFlow(QuickFilter._7_HARI)
    private val _selectedKegiatanFilters = MutableStateFlow<Set<String>>(emptySet())
    private val _selectedStatusFilters = MutableStateFlow<Set<String>>(emptySet())
    private val _selectedSourceFilters = MutableStateFlow<Set<String>>(emptySet())
    private val _viewMode = MutableStateFlow(ViewMode.HARIAN)

    val currentNis: StateFlow<String> = _currentNis.asStateFlow()
    val currentStartDate: StateFlow<LocalDate> = _currentStartDate.asStateFlow()
    val currentEndDate: StateFlow<LocalDate> = _currentEndDate.asStateFlow()
    val quickFilter: StateFlow<QuickFilter> = _quickFilter.asStateFlow()
    val selectedKegiatanFilters: StateFlow<Set<String>> = _selectedKegiatanFilters.asStateFlow()
    val selectedStatusFilters: StateFlow<Set<String>> = _selectedStatusFilters.asStateFlow()
    val selectedSourceFilters: StateFlow<Set<String>> = _selectedSourceFilters.asStateFlow()
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun loadAbsensi(nis: String) {
        _currentNis.value = nis
        _currentStartDate.value = LocalDate.now().minusDays(6)
        _currentEndDate.value = LocalDate.now()
        _quickFilter.value = QuickFilter._7_HARI
        _selectedKegiatanFilters.value = emptySet()
        _selectedStatusFilters.value = emptySet()
        _selectedSourceFilters.value = emptySet()
        _viewMode.value = ViewMode.HARIAN
        fetchFromApi()
    }

    fun setQuickFilter(filter: QuickFilter) {
        _quickFilter.value = filter
        val today = LocalDate.now()

        when (filter) {
            QuickFilter.HARI_INI -> {
                _currentStartDate.value = today
                _currentEndDate.value = today
            }
            QuickFilter.KEMARIN -> {
                _currentStartDate.value = today.minusDays(1)
                _currentEndDate.value = today.minusDays(1)
            }
            QuickFilter._7_HARI -> {
                _currentStartDate.value = today.minusDays(6)
                _currentEndDate.value = today
            }
            QuickFilter._30_HARI -> {
                _currentStartDate.value = today.minusDays(29)
                _currentEndDate.value = today
            }
            QuickFilter.CUSTOM -> {
                // Do nothing, use existing dates
            }
        }

        fetchFromApi()
    }

    fun setCustomDateRange(startDate: LocalDate, endDate: LocalDate) {
        _currentStartDate.value = startDate
        _currentEndDate.value = endDate
        _quickFilter.value = QuickFilter.CUSTOM
        fetchFromApi()
    }

    fun navigateWeek(isNext: Boolean) {
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
            _currentStartDate.value, _currentEndDate.value
        ) + 1
        val offset = if (isNext) daysBetween else -daysBetween
        _currentStartDate.value = _currentStartDate.value.plusDays(offset)
        _currentEndDate.value = _currentEndDate.value.plusDays(offset)
        _quickFilter.value = QuickFilter.CUSTOM
        fetchFromApi()
    }

    fun setKegiatanFilter(kegiatan: String, isSelected: Boolean) {
        val current = _selectedKegiatanFilters.value.toMutableSet()
        if (isSelected) {
            current.add(kegiatan)
        } else {
            current.remove(kegiatan)
        }
        _selectedKegiatanFilters.value = current
        applyClientSideFilter()
    }

    fun setAllKegiatanFilter(kegiatanList: List<String>, isSelected: Boolean) {
        if (isSelected) {
            _selectedKegiatanFilters.value = kegiatanList.toSet()
        } else {
            _selectedKegiatanFilters.value = emptySet()
        }
        applyClientSideFilter()
    }

    fun setStatusFilter(status: String, isSelected: Boolean) {
        val current = _selectedStatusFilters.value.toMutableSet()
        if (isSelected) {
            current.add(status)
        } else {
            current.remove(status)
        }
        _selectedStatusFilters.value = current
        applyClientSideFilter()
    }

    fun setSourceFilter(source: String, isSelected: Boolean) {
        val current = _selectedSourceFilters.value.toMutableSet()
        if (isSelected) {
            current.add(source)
        } else {
            current.remove(source)
        }
        _selectedSourceFilters.value = current
        applyClientSideFilter()
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
        applyClientSideFilter()
    }

    fun applyFilters() {
        fetchFromApi()
    }

    fun applyKegiatanFilter() {
        applyClientSideFilter()
    }

    fun resetAllFilters() {
        _selectedKegiatanFilters.value = emptySet()
        _selectedStatusFilters.value = emptySet()
        _selectedSourceFilters.value = emptySet()
        _quickFilter.value = QuickFilter._7_HARI
        val today = LocalDate.now()
        _currentStartDate.value = today.minusDays(6)
        _currentEndDate.value = today
        fetchFromApi()
    }

    private fun fetchFromApi() {
        viewModelScope.launch {
            _uiState.value = AbsensiLengkapUiState.Loading

            val nis = _currentNis.value
            val startDate = _currentStartDate.value
            val endDate = _currentEndDate.value

            repository.getAbsensiLengkap(
                nis = nis,
                startDate = startDate.format(dateFormatter),
                endDate = endDate.format(dateFormatter)
            )
                .catch { e ->
                    _uiState.value = AbsensiLengkapUiState.Error(
                        e.message ?: "Gagal memuat data absensi"
                    )
                }
                .collect { response ->
                    rawResponse = response
                    applyClientSideFilter()
                }
        }
    }

    private fun applyClientSideFilter() {
        val response = rawResponse ?: return
        val selectedKegiatan = _selectedKegiatanFilters.value
        val selectedStatus = _selectedStatusFilters.value
        val selectedSource = _selectedSourceFilters.value

        // If no filters selected, show all data
        if (selectedKegiatan.isEmpty() && selectedStatus.isEmpty() && selectedSource.isEmpty()) {
            _uiState.value = AbsensiLengkapUiState.Success(response)
            return
        }

        // Filter perHari by selected filters
        val filteredPerHari = response.perHari.map { hari ->
            val filteredKegiatan = hari.kegiatan.filter { kegiatan ->
                val matchesKegiatan = selectedKegiatan.isEmpty() || selectedKegiatan.contains(kegiatan.nama)
                val matchesStatus = selectedStatus.isEmpty() || selectedStatus.contains(kegiatan.status.uppercase())
                val matchesSource = selectedSource.isEmpty() || selectedSource.contains(kegiatan.sumber)
                matchesKegiatan && matchesStatus && matchesSource
            }
            hari.copy(kegiatan = filteredKegiatan)
        }.filter { it.kegiatan.isNotEmpty() }

        // Recalculate ringkasan based on filtered data
        // Sekolah dihitung sebagai Izin untuk ringkasan
        var totalHadir = 0
        var totalIzin = 0
        var totalSakit = 0
        var totalAlpha = 0
        var totalPulang = 0
        var totalAll = 0

        filteredPerHari.forEach { hari ->
            hari.kegiatan.forEach { kegiatan ->
                totalAll++
                when (kegiatan.status.uppercase()) {
                    "HADIR" -> totalHadir++
                    "IZIN", "SEKOLAH" -> totalIzin++
                    "SAKIT" -> totalSakit++
                    "ALFA", "GHAIB" -> totalAlpha++
                    "PULANG" -> totalPulang++
                }
            }
        }

        val persentase = if (totalAll > 0) (totalHadir.toDouble() / totalAll) * 100.0 else 0.0

        val filteredResponse = response.copy(
            perHari = filteredPerHari,
            ringkasan = response.ringkasan.copy(
                hadir = totalHadir,
                izin = totalIzin,
                sakit = totalSakit,
                alpha = totalAlpha,
                pulang = totalPulang,
                total = totalAll,
                persentase = persentase
            )
        )

        _uiState.value = AbsensiLengkapUiState.Success(filteredResponse)
    }

    fun refresh() {
        fetchFromApi()
    }

    fun getDisplayDateRange(): String {
        val start = _currentStartDate.value
        val end = _currentEndDate.value
        val bulanIndo = listOf(
            "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        return "${start.dayOfMonth} - ${end.dayOfMonth} ${bulanIndo[end.monthValue]} ${end.year}"
    }

    fun getStatusColor(status: String): Long {
        return when (status.uppercase()) {
            "HADIR" -> 0xFF16A34A
            "IZIN" -> 0xFFF59E0B
            "SAKIT" -> 0xFFEF4444
            "ALFA", "GHAIB" -> 0xFF6B7280
            "SEKOLAH" -> 0xFF8B5CF6
            "PULANG" -> 0xFF0891B2
            else -> 0xFF6B7280
        }
    }

    fun getStatusLabel(status: String): String {
        return when (status.uppercase()) {
            "HADIR" -> "Hadir"
            "IZIN" -> "Izin"
            "SAKIT" -> "Sakit"
            "ALFA", "GHAIB" -> "Alpha"
            "SEKOLAH" -> "Sekolah"
            "PULANG" -> "Pulang"
            else -> status
        }
    }

    fun getDaySummary(kegiatanList: List<KegiatanHarian>): Map<String, Int> {
        val summary = mutableMapOf(
            "HADIR" to 0,
            "IZIN" to 0,
            "SAKIT" to 0,
            "ALPHA" to 0
        )
        kegiatanList.forEach { kegiatan ->
            when (kegiatan.status.uppercase()) {
                "HADIR" -> summary["HADIR"] = (summary["HADIR"] ?: 0) + 1
                "IZIN" -> summary["IZIN"] = (summary["IZIN"] ?: 0) + 1
                "SAKIT" -> summary["SAKIT"] = (summary["SAKIT"] ?: 0) + 1
                "ALFA", "GHAIB" -> summary["ALPHA"] = (summary["ALPHA"] ?: 0) + 1
            }
        }
        return summary
    }

    fun getDayPercentage(kegiatanList: List<KegiatanHarian>): Double {
        if (kegiatanList.isEmpty()) return 0.0
        val hadirCount = kegiatanList.count { it.status.uppercase() == "HADIR" }
        return (hadirCount.toDouble() / kegiatanList.size) * 100.0
    }

    fun getSesiGrouped(kegiatanList: List<KegiatanHarian>): Map<String, List<KegiatanHarian>> {
        val result = mutableMapOf<String, MutableList<KegiatanHarian>>()

        kegiatanList.forEach { kegiatan ->
            val sumber = kegiatan.sumber
            // Hanya Tahfidz yang punya grouping sesi (Pagi/Siang)
            val sesiKey = if (sumber == "tahfidz") {
                kegiatan.sesi.ifEmpty { "Lainnya" }
            } else {
                "Lainnya"
            }

            if (!result.containsKey(sesiKey)) {
                result[sesiKey] = mutableListOf()
            }
            result[sesiKey]?.add(kegiatan)
        }

        // Sort: Pagi first, then Siang, then others
        val sortedKeys = result.keys.sortedWith(
            compareBy<String> {
                when (it.lowercase()) {
                    "pagi" -> 0
                    "siang" -> 1
                    "sore" -> 2
                    "malam" -> 3
                    else -> 4
                }
            }
        )

        return sortedKeys.associateWith { result[it] ?: emptyList() }
    }
}
