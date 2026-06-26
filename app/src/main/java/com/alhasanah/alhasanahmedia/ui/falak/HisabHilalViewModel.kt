package com.alhasanah.alhasanahmedia.ui.falak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.repository.HisabHilalRepository
import com.alhasanah.alhasanahmedia.domain.falak.HasilHisabHilalEphemeris
import com.alhasanah.alhasanahmedia.domain.falak.KonteksHisabHilal
import com.alhasanah.alhasanahmedia.domain.falak.KriteriaAwalBulanFalak
import com.alhasanah.alhasanahmedia.domain.falak.MarkazFalak
import com.alhasanah.alhasanahmedia.domain.falak.ZonaWaktuFalak
import com.alhasanah.alhasanahmedia.util.FalakMarkazProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AcuanAwalBulanKemenag(
    val bulanHijriah: String,
    val tanggalRukyatMasehi: String,
    val ijtimakWib: String,
    val prediksiAwalBulanMasehi: String,
)

enum class ModeTanggalHisab {
    ACUAN_KEMENAG,
    INPUT_MANUAL,
}

data class MarkazInput(
    val nama: String = "Markaz Al Hasanah",
    val lintang: String = "-7.3333",
    val bujur: String = "108.2167",
    val elevasi: String = "350",
    val zona: String = "WIB",
)

data class HisabHilalUiState(
    val bulanHijriah: String = "Ramadan 1447 H",
    val tanggalSituasiHilal: String = "2026-02-17",
    val modeTanggal: ModeTanggalHisab = ModeTanggalHisab.ACUAN_KEMENAG,
    val markazInput: MarkazInput = MarkazInput(),
    val kriteria: KriteriaAwalBulanFalak = KriteriaAwalBulanFalak.KemenagMabimsTerbaru,
    val loading: Boolean = false,
    val detectingLocation: Boolean = false,
    val error: String? = null,
    val sumberMarkaz: String? = null,
    val hasil: HasilHisabHilalEphemeris? = null,
)

class HisabHilalViewModel(
    private val hisabHilalRepository: HisabHilalRepository,
    private val markazProvider: FalakMarkazProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HisabHilalUiState())
    val uiState: StateFlow<HisabHilalUiState> = _uiState.asStateFlow()

    fun ubahBulanHijriah(value: String) {
        val acuan = acuanKemenag2026.firstOrNull { it.bulanHijriah.equals(value.trim(), ignoreCase = true) }
        _uiState.update {
            it.copy(
                bulanHijriah = value,
                tanggalSituasiHilal = if (it.modeTanggal == ModeTanggalHisab.ACUAN_KEMENAG) {
                    acuan?.tanggalRukyatMasehi ?: it.tanggalSituasiHilal
                } else {
                    it.tanggalSituasiHilal
                },
                error = null,
                hasil = null,
            )
        }
    }

    fun pilihAcuanKemenag(acuan: AcuanAwalBulanKemenag) {
        _uiState.update {
            it.copy(
                bulanHijriah = acuan.bulanHijriah,
                tanggalSituasiHilal = acuan.tanggalRukyatMasehi,
                modeTanggal = ModeTanggalHisab.ACUAN_KEMENAG,
                error = null,
                hasil = null,
            )
        }
    }

    fun ubahModeTanggal(mode: ModeTanggalHisab) {
        _uiState.update {
            val acuan = acuanKemenag2026.firstOrNull { item ->
                item.bulanHijriah.equals(it.bulanHijriah.trim(), ignoreCase = true)
            }
            it.copy(
                modeTanggal = mode,
                tanggalSituasiHilal = if (mode == ModeTanggalHisab.ACUAN_KEMENAG && acuan != null) {
                    acuan.tanggalRukyatMasehi
                } else {
                    it.tanggalSituasiHilal
                },
                error = null,
                hasil = null,
            )
        }
    }

    fun ubahTanggalSituasi(value: String) {
        _uiState.update {
            it.copy(
                tanggalSituasiHilal = value,
                modeTanggal = ModeTanggalHisab.INPUT_MANUAL,
                error = null,
                hasil = null
            )
        }
    }

    fun ubahMarkaz(value: MarkazInput) {
        _uiState.update { it.copy(markazInput = value, error = null, sumberMarkaz = "Markaz diubah manual.") }
    }

    fun ubahKriteria(value: KriteriaAwalBulanFalak) {
        _uiState.update { it.copy(kriteria = value, error = null) }
    }

    fun deteksiMarkaz() {
        viewModelScope.launch {
            _uiState.update { it.copy(detectingLocation = true, error = null) }
            markazProvider.deteksiMarkaz()
                .onSuccess { detected ->
                    val markaz = detected.markaz
                    _uiState.update {
                        it.copy(
                            detectingLocation = false,
                            markazInput = MarkazInput(
                                nama = markaz.nama,
                                lintang = markaz.lintangDerajat.toString(),
                                bujur = markaz.bujurDerajat.toString(),
                                elevasi = markaz.elevasiMeter.toString(),
                                zona = markaz.zonaWaktu.nama,
                            ),
                            sumberMarkaz = listOfNotNull(
                                "Sumber ${detected.sumber}",
                                detected.akurasiMeter?.let { accuracy -> "akurasi ${accuracy.toInt()} m" },
                                if (detected.elevasiOtomatis) "elevasi otomatis" else "elevasi manual",
                                detected.catatan
                            ).joinToString(". "),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            detectingLocation = false,
                            error = error.message ?: "Gagal mendeteksi markaz."
                        )
                    }
                }
        }
    }

    fun hitung() {
        viewModelScope.launch {
            val konteks = runCatching { buildKonteks(_uiState.value) }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "Parameter hisab belum valid.") }
                }
                .getOrNull() ?: return@launch

            _uiState.update { it.copy(loading = true, error = null) }
            hisabHilalRepository.hitung(konteks)
                .onSuccess { hasil ->
                    _uiState.update { it.copy(loading = false, hasil = hasil, error = null) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = error.message ?: "Perhitungan hisab hilal gagal."
                        )
                    }
                }
        }
    }

    private fun buildKonteks(state: HisabHilalUiState): KonteksHisabHilal {
        val tanggal = LocalDate.parse(state.tanggalSituasiHilal.trim())
        val input = state.markazInput
        val lintang = input.lintang.toDoubleOrNull() ?: error("Lintang belum valid.")
        val bujur = input.bujur.toDoubleOrNull() ?: error("Bujur belum valid.")
        val elevasi = input.elevasi.toDoubleOrNull() ?: error("Elevasi belum valid.")
        val zona = when (input.zona.trim().uppercase()) {
            "WIB" -> ZonaWaktuFalak.WIB
            "WITA" -> ZonaWaktuFalak.WITA
            "WIT" -> ZonaWaktuFalak.WIT
            else -> FalakMarkazProvider.zonaWaktuIndonesia(bujur)
        }
        return KonteksHisabHilal(
            bulanHijriah = state.bulanHijriah.ifBlank { "Bulan Hijriah" },
            tanggalSituasiHilalMasehi = tanggal,
            markaz = MarkazFalak(
                nama = input.nama.ifBlank { "Markaz" },
                lintangDerajat = lintang,
                bujurDerajat = bujur,
                elevasiMeter = elevasi,
                zonaWaktu = zona,
            ),
            kriteriaAwalBulan = state.kriteria,
        )
    }

    companion object {
        val acuanKemenag2026 = listOf(
            AcuanAwalBulanKemenag("Syaban 1447 H", "2026-01-19", "19 Januari 2026 02:52 WIB", "2026-01-20"),
            AcuanAwalBulanKemenag("Ramadan 1447 H", "2026-02-17", "17 Februari 2026 19:01 WIB", "2026-02-19"),
            AcuanAwalBulanKemenag("Syawal 1447 H", "2026-03-19", "19 Maret 2026 08:23 WIB", "2026-03-21"),
            AcuanAwalBulanKemenag("Zulqa'dah 1447 H", "2026-04-17", "17 April 2026 18:51 WIB", "2026-04-19"),
            AcuanAwalBulanKemenag("Zulhijjah 1447 H", "2026-05-17", "17 Mei 2026 03:00 WIB", "2026-05-18"),
            AcuanAwalBulanKemenag("Muharram 1448 H", "2026-06-15", "15 Juni 2026 09:54 WIB", "2026-06-16"),
            AcuanAwalBulanKemenag("Shafar 1448 H", "2026-07-14", "14 Juli 2026 16:43 WIB", "2026-07-16"),
            AcuanAwalBulanKemenag("Rabi'ul Awal 1448 H", "2026-08-13", "13 Agustus 2026 00:36 WIB", "2026-08-14"),
            AcuanAwalBulanKemenag("Rabi'ul Akhir 1448 H", "2026-09-11", "11 September 2026 10:26 WIB", "2026-09-13"),
            AcuanAwalBulanKemenag("Jumadal Ula 1448 H", "2026-10-10", "10 Oktober 2026 22:49 WIB", "2026-10-12"),
            AcuanAwalBulanKemenag("Jumadal Akhirah 1448 H", "2026-11-09", "09 November 2026 14:01 WIB", "2026-11-11"),
            AcuanAwalBulanKemenag("Rajab 1448 H", "2026-12-09", "09 Desember 2026 07:51 WIB", "2026-12-10"),
        )
    }
}
