package com.alhasanah.alhasanahmedia.ui.laporanmasalah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.LaporanKategori
import com.alhasanah.alhasanahmedia.data.model.LaporanMasalah
import com.alhasanah.alhasanahmedia.data.model.LaporanMasalahInsert
import com.alhasanah.alhasanahmedia.data.model.LaporanPrioritas
import com.alhasanah.alhasanahmedia.data.repository.DeviceInfo
import com.alhasanah.alhasanahmedia.data.repository.LaporanMasalahRepository
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LaporanMasalahUiState {
    object Idle : LaporanMasalahUiState()
    object Loading : LaporanMasalahUiState()
    object Success : LaporanMasalahUiState()
    data class Error(val message: String) : LaporanMasalahUiState()
}

class LaporanMasalahViewModel(
    private val repository: LaporanMasalahRepository,
    private val auth: Auth
) : ViewModel() {

    private val _uiState = MutableStateFlow<LaporanMasalahUiState>(LaporanMasalahUiState.Idle)
    val uiState: StateFlow<LaporanMasalahUiState> = _uiState.asStateFlow()

    private val _laporans = MutableStateFlow<List<LaporanMasalah>>(emptyList())
    val laporans: StateFlow<List<LaporanMasalah>> = _laporans.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    private val _selectedJudul = MutableStateFlow("")
    val selectedJudul: StateFlow<String> = _selectedJudul.asStateFlow()

    private val _selectedDeskripsi = MutableStateFlow("")
    val selectedDeskripsi: StateFlow<String> = _selectedDeskripsi.asStateFlow()

    private val _selectedKategori = MutableStateFlow(LaporanKategori.BUG)
    val selectedKategori: StateFlow<LaporanKategori> = _selectedKategori.asStateFlow()

    private val _selectedPrioritas = MutableStateFlow(LaporanPrioritas.MEDIUM)
    val selectedPrioritas: StateFlow<LaporanPrioritas> = _selectedPrioritas.asStateFlow()

    init {
        loadDeviceInfo()
        loadUserLaporans()
    }

    private fun loadDeviceInfo() {
        viewModelScope.launch {
            _deviceInfo.value = repository.getDeviceInfo()
        }
    }

    private fun loadUserLaporans() {
        viewModelScope.launch {
            val userId = auth.currentUserOrNull()?.id ?: return@launch
            repository.getUserLaporans(userId).collect { laporans ->
                _laporans.value = laporans
            }
        }
    }

    fun updateJudul(judul: String) {
        _selectedJudul.value = judul
    }

    fun updateDeskripsi(deskripsi: String) {
        _selectedDeskripsi.value = deskripsi
    }

    fun updateKategori(kategori: LaporanKategori) {
        _selectedKategori.value = kategori
    }

    fun updatePrioritas(prioritas: LaporanPrioritas) {
        _selectedPrioritas.value = prioritas
    }

    fun submitLaporan() {
        val judul = _selectedJudul.value.trim()
        val deskripsi = _selectedDeskripsi.value.trim()

        if (judul.isEmpty() || deskripsi.isEmpty()) {
            _uiState.value = LaporanMasalahUiState.Error("Judul dan deskripsi harus diisi")
            return
        }

        viewModelScope.launch {
            _uiState.value = LaporanMasalahUiState.Loading

            val userId = auth.currentUserOrNull()?.id
            if (userId == null) {
                _uiState.value = LaporanMasalahUiState.Error("Anda harus login terlebih dahulu")
                return@launch
            }

            val device = _deviceInfo.value

            val laporan = LaporanMasalahInsert(
                userId = userId,
                judul = judul,
                deskripsi = deskripsi,
                kategori = _selectedKategori.value.name,
                prioritas = _selectedPrioritas.value.name,
                appVersion = device?.appVersion,
                androidVersion = device?.androidVersion,
                deviceBrand = device?.deviceBrand,
                deviceModel = device?.deviceModel,
                deviceManufacturer = device?.deviceManufacturer,
                deviceSdk = device?.deviceSdk,
                locale = device?.locale,
                timezone = device?.timezone,
                source = "android"
            )

            val result = repository.insertLaporan(laporan)

            if (result != null) {
                _uiState.value = LaporanMasalahUiState.Success
                resetForm()
                loadUserLaporans()
            } else {
                _uiState.value = LaporanMasalahUiState.Error("Gagal mengirim laporan. Silakan coba lagi.")
            }
        }
    }

    private fun resetForm() {
        _selectedJudul.value = ""
        _selectedDeskripsi.value = ""
        _selectedKategori.value = LaporanKategori.BUG
        _selectedPrioritas.value = LaporanPrioritas.MEDIUM
    }

    fun resetUiState() {
        _uiState.value = LaporanMasalahUiState.Idle
    }
}
