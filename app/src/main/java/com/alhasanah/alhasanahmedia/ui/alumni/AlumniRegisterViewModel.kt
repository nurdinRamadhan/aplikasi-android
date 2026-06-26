package com.alhasanah.alhasanahmedia.ui.alumni

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.IndonesiaRegionItem
import com.alhasanah.alhasanahmedia.data.model.RegisterAlumniRequest
import com.alhasanah.alhasanahmedia.data.repository.AlumniRegistrationRepository
import com.alhasanah.alhasanahmedia.data.repository.IndonesiaRegionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AlumniRegisterState {
    object Idle : AlumniRegisterState()
    object Loading : AlumniRegisterState()
    data class Success(val message: String) : AlumniRegisterState()
    data class Error(val message: String) : AlumniRegisterState()
}

data class AlumniRegisterRegionState(
    val provinces: List<IndonesiaRegionItem> = emptyList(),
    val regencies: List<IndonesiaRegionItem> = emptyList(),
    val districts: List<IndonesiaRegionItem> = emptyList(),
    val villages: List<IndonesiaRegionItem> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

class AlumniRegisterViewModel(
    private val repository: AlumniRegistrationRepository,
    private val regionRepository: IndonesiaRegionRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AlumniRegisterState>(AlumniRegisterState.Idle)
    val state: StateFlow<AlumniRegisterState> = _state.asStateFlow()

    private val _regionState = MutableStateFlow(AlumniRegisterRegionState())
    val regionState: StateFlow<AlumniRegisterRegionState> = _regionState.asStateFlow()

    init {
        loadProvinces()
    }

    fun register(
        email: String,
        password: String,
        fullName: String,
        tahunLulus: String,
        noWa: String,
        profesiSekarang: String,
        instansiKerja: String,
        alamatDomisili: String,
        province: IndonesiaRegionItem?,
        regency: IndonesiaRegionItem?,
        district: IndonesiaRegionItem?,
        village: IndonesiaRegionItem?,
        postalCode: String,
        addressDetail: String
    ) {
        val year = tahunLulus.trim().toIntOrNull()
        when {
            fullName.trim().length < 3 -> {
                _state.value = AlumniRegisterState.Error("Nama lengkap wajib diisi.")
                return
            }
            !email.trim().contains("@") -> {
                _state.value = AlumniRegisterState.Error("Email tidak valid.")
                return
            }
            password.length < 8 -> {
                _state.value = AlumniRegisterState.Error("Password minimal 8 karakter.")
                return
            }
            year == null -> {
                _state.value = AlumniRegisterState.Error("Tahun lulus wajib berupa angka.")
                return
            }
        }

        viewModelScope.launch {
            _state.value = AlumniRegisterState.Loading
            runCatching {
                repository.register(
                    RegisterAlumniRequest(
                        email = email.trim(),
                        password = password,
                        fullName = fullName.trim(),
                        tahunLulus = year,
                        noWa = noWa.trim().ifBlank { null },
                        profesiSekarang = profesiSekarang.trim().ifBlank { null },
                        instansiKerja = instansiKerja.trim().ifBlank { null },
                        alamatDomisili = alamatDomisili.trim().ifBlank {
                            buildAddressDisplay(village, district, regency, province).ifBlank { null }
                        },
                        provinceCode = province?.code,
                        provinceName = province?.name,
                        regencyCode = regency?.code,
                        regencyName = regency?.name,
                        districtCode = district?.code,
                        districtName = district?.name,
                        villageCode = village?.code,
                        villageName = village?.name,
                        postalCode = postalCode.trim().ifBlank { null },
                        addressDetail = addressDetail.trim().ifBlank { null }
                    )
                )
            }.onSuccess { message ->
                _state.value = AlumniRegisterState.Success(message)
            }.onFailure { error ->
                Log.e("AlumniRegisterVM", "Gagal daftar alumni", error)
                _state.value = AlumniRegisterState.Error(
                    error.localizedMessage ?: "Pendaftaran alumni gagal diproses."
                )
            }
        }
    }

    fun loadProvinces() {
        viewModelScope.launch {
            _regionState.value = _regionState.value.copy(isLoading = true, message = null)
            runCatching { regionRepository.getProvinces() }
                .onSuccess { provinces ->
                    _regionState.value = _regionState.value.copy(
                        provinces = provinces,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _regionState.value = _regionState.value.copy(
                        isLoading = false,
                        message = error.localizedMessage ?: "Data wilayah belum dapat dimuat."
                    )
                }
        }
    }

    fun loadRegencies(provinceCode: String) {
        viewModelScope.launch {
            _regionState.value = _regionState.value.copy(
                regencies = emptyList(),
                districts = emptyList(),
                villages = emptyList(),
                isLoading = true,
                message = null
            )
            runCatching { regionRepository.getRegencies(provinceCode) }
                .onSuccess { items -> _regionState.value = _regionState.value.copy(regencies = items, isLoading = false) }
                .onFailure { error ->
                    _regionState.value = _regionState.value.copy(
                        isLoading = false,
                        message = error.localizedMessage ?: "Kabupaten/kota belum dapat dimuat."
                    )
                }
        }
    }

    fun loadDistricts(regencyCode: String) {
        viewModelScope.launch {
            _regionState.value = _regionState.value.copy(
                districts = emptyList(),
                villages = emptyList(),
                isLoading = true,
                message = null
            )
            runCatching { regionRepository.getDistricts(regencyCode) }
                .onSuccess { items -> _regionState.value = _regionState.value.copy(districts = items, isLoading = false) }
                .onFailure { error ->
                    _regionState.value = _regionState.value.copy(
                        isLoading = false,
                        message = error.localizedMessage ?: "Kecamatan belum dapat dimuat."
                    )
                }
        }
    }

    fun loadVillages(districtCode: String) {
        viewModelScope.launch {
            _regionState.value = _regionState.value.copy(villages = emptyList(), isLoading = true, message = null)
            runCatching { regionRepository.getVillages(districtCode) }
                .onSuccess { items -> _regionState.value = _regionState.value.copy(villages = items, isLoading = false) }
                .onFailure { error ->
                    _regionState.value = _regionState.value.copy(
                        isLoading = false,
                        message = error.localizedMessage ?: "Desa/kelurahan belum dapat dimuat."
                    )
                }
        }
    }

    fun resetError() {
        if (_state.value is AlumniRegisterState.Error) {
            _state.value = AlumniRegisterState.Idle
        }
    }

    private fun buildAddressDisplay(
        village: IndonesiaRegionItem?,
        district: IndonesiaRegionItem?,
        regency: IndonesiaRegionItem?,
        province: IndonesiaRegionItem?
    ): String {
        return listOfNotNull(village?.name, district?.name, regency?.name, province?.name).joinToString(", ")
    }
}
