package com.alhasanah.alhasanahmedia.ui.falak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.repository.GerhanaBulanRepository
import com.alhasanah.alhasanahmedia.domain.falak.HasilGerhanaBulanEphemeris
import com.alhasanah.alhasanahmedia.domain.falak.KonteksGerhanaBulan
import com.alhasanah.alhasanahmedia.domain.falak.ModeDataGerhanaBulan
import com.alhasanah.alhasanahmedia.domain.falak.ZonaWaktuFalak
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AcuanGerhanaBulanKemenag(
    val nama: String,
    val bulanHijriah: String,
    val tanggalKemungkinanMasehi: String,
    val jenis: String,
)

enum class ModeTanggalGerhanaBulan {
    ACUAN_KEMENAG,
    INPUT_MANUAL,
}

data class GerhanaBulanUiState(
    val bulanHijriah: String = "Pertengahan Ramadan 1447 H",
    val tanggalKemungkinan: String = "2026-03-04",
    val zona: String = "WIB",
    val modeTanggal: ModeTanggalGerhanaBulan = ModeTanggalGerhanaBulan.ACUAN_KEMENAG,
    val rentangPencarianManualHari: Int = 3,
    val loading: Boolean = false,
    val error: String? = null,
    val hasil: HasilGerhanaBulanEphemeris? = null,
)

class GerhanaBulanViewModel(
    private val gerhanaBulanRepository: GerhanaBulanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GerhanaBulanUiState())
    val uiState: StateFlow<GerhanaBulanUiState> = _uiState.asStateFlow()

    fun pilihAcuan(acuan: AcuanGerhanaBulanKemenag) {
        _uiState.update {
            it.copy(
                bulanHijriah = acuan.bulanHijriah,
                tanggalKemungkinan = acuan.tanggalKemungkinanMasehi,
                modeTanggal = ModeTanggalGerhanaBulan.ACUAN_KEMENAG,
                error = null,
                hasil = null,
            )
        }
    }

    fun ubahModeTanggal(mode: ModeTanggalGerhanaBulan) {
        _uiState.update {
            it.copy(
                modeTanggal = mode,
                error = null,
                hasil = null,
            )
        }
    }

    fun ubahBulanHijriah(value: String) {
        _uiState.update { it.copy(bulanHijriah = value, error = null, hasil = null) }
    }

    fun ubahTanggalKemungkinan(value: String) {
        _uiState.update {
            it.copy(
                tanggalKemungkinan = value,
                modeTanggal = ModeTanggalGerhanaBulan.INPUT_MANUAL,
                error = null,
                hasil = null,
            )
        }
    }

    fun ubahZona(value: String) {
        _uiState.update { it.copy(zona = value, error = null, hasil = null) }
    }

    fun ubahRentangPencarianManual(value: Int) {
        _uiState.update {
            it.copy(
                rentangPencarianManualHari = value.coerceIn(1, 5),
                error = null,
                hasil = null,
            )
        }
    }

    fun hitung() {
        viewModelScope.launch {
            val konteks = runCatching { buildKonteks(_uiState.value) }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "Parameter gerhana bulan belum valid.") }
                }
                .getOrNull() ?: return@launch

            _uiState.update { it.copy(loading = true, error = null) }
            gerhanaBulanRepository.hitung(konteks)
                .onSuccess { hasil ->
                    _uiState.update { it.copy(loading = false, hasil = hasil, error = null) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = pesanErrorGerhana(error.message, it)
                        )
                    }
                }
        }
    }

    private fun pesanErrorGerhana(message: String?, state: GerhanaBulanUiState): String {
        val teks = message ?: return "Perhitungan gerhana bulan gagal."
        return when {
            "Data ephemeris tanggal" in teks -> {
                if (state.modeTanggal == ModeTanggalGerhanaBulan.INPUT_MANUAL) {
                    "$teks Mode input manual membutuhkan data ephemeris di sekitar tanggal input sampai jam interpolasi. Coba perluas rentang pencarian atau sinkronkan paket ephemeris tahun terkait."
                } else {
                    "$teks Mode acuan Kemenag membutuhkan data tanggal acuan dan tanggal pendampingnya. Sinkronkan paket ephemeris tahun terkait."
                }
            }
            "jam" in teks && "GMT/UT" in teks -> {
                "$teks Data tanggal ada, tetapi baris jam yang dibutuhkan belum lengkap. Sinkronkan ulang paket ephemeris dan pastikan tabel harian tidak rusak."
            }
            "FIB Bulan" in teks -> {
                "$teks Mode input manual mencari purnama/FIB terbesar di rentang yang dipilih. Perluas rentang atau gunakan tanggal yang lebih dekat dengan pertengahan bulan Hijriah."
            }
            else -> teks
        }
    }

    private fun buildKonteks(state: GerhanaBulanUiState): KonteksGerhanaBulan {
        val tanggal = LocalDate.parse(state.tanggalKemungkinan.trim())
        val zona = when (state.zona.trim().uppercase()) {
            "WIB" -> ZonaWaktuFalak.WIB
            "WITA" -> ZonaWaktuFalak.WITA
            "WIT" -> ZonaWaktuFalak.WIT
            else -> ZonaWaktuFalak.WIB
        }
        return KonteksGerhanaBulan(
            bulanHijriah = state.bulanHijriah.ifBlank { "Pertengahan bulan Hijriah" },
            tanggalKemungkinanGerhanaMasehi = tanggal,
            zonaWaktu = zona,
            modeData = if (state.modeTanggal == ModeTanggalGerhanaBulan.ACUAN_KEMENAG) {
                ModeDataGerhanaBulan.AcuanKemenag
            } else {
                ModeDataGerhanaBulan.InputManual
            },
            rentangPencarianManualHari = state.rentangPencarianManualHari,
        )
    }

    companion object {
        val acuanKemenag2026 = listOf(
            AcuanGerhanaBulanKemenag(
                nama = "Gerhana Bulan Total Maret 2026",
                bulanHijriah = "Pertengahan Ramadan 1447 H",
                tanggalKemungkinanMasehi = "2026-03-04",
                jenis = "Total",
            ),
            AcuanGerhanaBulanKemenag(
                nama = "Gerhana Bulan Sebagian Agustus 2026",
                bulanHijriah = "Pertengahan Rabi'ul Awal 1448 H",
                tanggalKemungkinanMasehi = "2026-08-28",
                jenis = "Sebagian",
            ),
        )
    }
}
