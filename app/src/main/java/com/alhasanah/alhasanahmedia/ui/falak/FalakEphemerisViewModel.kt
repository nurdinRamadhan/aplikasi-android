package com.alhasanah.alhasanahmedia.ui.falak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.falak.FalakDataLengkap
import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarian
import com.alhasanah.alhasanahmedia.data.model.falak.FalakHalamanPdf
import com.alhasanah.alhasanahmedia.data.model.falak.FalakHilalTable
import com.alhasanah.alhasanahmedia.data.model.falak.FalakIndeksItem
import com.alhasanah.alhasanahmedia.data.repository.FalakRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class FalakEphemerisUiState(
    val loading: Boolean = false,
    val syncing: Boolean = false,
    val error: String? = null,
    val data: FalakDataLengkap? = null,
    val query: String = "",
    val tipeFilter: String? = null,
    val hasilIndeks: List<FalakIndeksItem> = emptyList(),
    val tanggalDipilih: String = "2026-01-01",
    val ephemerisDipilih: FalakEphemerisHarian? = null,
    val hilalIndexDipilih: Int = 0,
    val hilalDipilih: FalakHilalTable? = null,
    val lokasiQuery: String = "",
    val lokasiTerdeteksi: String? = null,
    val nomorHalaman: String = "50",
    val halamanPdf: FalakHalamanPdf? = null,
)

class FalakEphemerisViewModel(
    private val repository: FalakRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FalakEphemerisUiState(loading = true))
    val uiState: StateFlow<FalakEphemerisUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = repository.loadDataLengkap().recoverCatching {
                repository.refreshPaketKemenag().getOrThrow()
            }
            result
                .onSuccess { data ->
                    val firstDate = data.ephemerisHarian.firstOrNull()?.date ?: "2026-01-01"
                    val firstHilal = data.hilalLokasi.firstOrNull()
                    _uiState.update {
                        it.copy(
                            loading = false,
                            data = data,
                            tanggalDipilih = firstDate,
                            ephemerisDipilih = data.ephemerisHarian.firstOrNull(),
                            hilalDipilih = firstHilal,
                            hasilIndeks = data.indeks.take(200),
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(loading = false, error = error.message ?: "Gagal memuat data Falak.") }
                }
        }
    }

    fun sinkronkan() {
        viewModelScope.launch {
            _uiState.update { it.copy(syncing = true, error = null) }
            repository.refreshPaketKemenag()
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            syncing = false,
                            loading = false,
                            data = data,
                            hasilIndeks = data.indeks.take(200),
                            ephemerisDipilih = data.ephemerisHarian.firstOrNull { block -> block.date == it.tanggalDipilih }
                                ?: data.ephemerisHarian.firstOrNull(),
                            hilalDipilih = data.hilalLokasi.getOrNull(it.hilalIndexDipilih) ?: data.hilalLokasi.firstOrNull(),
                            error = null,
                        )
                    }
                    cari()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(syncing = false, error = error.message ?: "Sinkronisasi gagal.") }
                }
        }
    }

    fun ubahQuery(value: String) {
        _uiState.update { it.copy(query = value) }
        cari()
    }

    fun ubahFilterTipe(value: String?) {
        _uiState.update { it.copy(tipeFilter = value) }
        cari()
    }

    fun pilihTanggal(value: String) {
        _uiState.update { it.copy(tanggalDipilih = value) }
        viewModelScope.launch {
            val item = repository.getEphemerisTanggal(value)
            _uiState.update { it.copy(ephemerisDipilih = item) }
        }
    }

    fun pilihHilal(index: Int) {
        _uiState.update { it.copy(hilalIndexDipilih = index) }
        viewModelScope.launch {
            _uiState.update { it.copy(hilalDipilih = repository.getHilalTable(index)) }
        }
    }

    fun ubahLokasiQuery(value: String) {
        _uiState.update { it.copy(lokasiQuery = value) }
    }

    fun cocokkanLokasiTerdeteksi(candidates: List<String>) {
        val table = _uiState.value.hilalDipilih ?: return
        val normalizedCandidates = candidates
            .flatMap { it.split(" ", "-", ",") }
            .map { it.trim().lowercase() }
            .filter { it.length >= 4 }
        val best = table.rows.firstOrNull { row ->
            val lokasi = row["location"]?.jsonPrimitive?.contentOrNull.orEmpty().lowercase()
            normalizedCandidates.any { token -> lokasi.contains(token) || token.contains(lokasi) }
        }?.get("location")?.jsonPrimitive?.contentOrNull

        _uiState.update {
            it.copy(
                lokasiQuery = best ?: candidates.firstOrNull().orEmpty(),
                lokasiTerdeteksi = candidates.filter { item -> item.isNotBlank() }.distinct().joinToString(", ")
            )
        }
    }

    fun ubahNomorHalaman(value: String) {
        _uiState.update { it.copy(nomorHalaman = value.filter { char -> char.isDigit() }) }
    }

    fun bukaHalamanPdf() {
        val nomor = _uiState.value.nomorHalaman.toIntOrNull() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(halamanPdf = repository.getHalamanPdf(nomor)) }
        }
    }

    private fun cari() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val state = _uiState.value
            val result = repository.cariIndeks(state.query, state.tipeFilter, 200)
            _uiState.update { it.copy(hasilIndeks = result) }
        }
    }
}
