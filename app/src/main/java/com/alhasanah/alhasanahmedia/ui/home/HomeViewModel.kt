package com.alhasanah.alhasanahmedia.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.BeritaModel
import com.alhasanah.alhasanahmedia.data.model.HafalanTahfidz
import com.alhasanah.alhasanahmedia.data.model.KesehatanSantri
import com.alhasanah.alhasanahmedia.data.model.PelanggaranSantri
import com.alhasanah.alhasanahmedia.data.model.PerizinanSantri
import com.alhasanah.alhasanahmedia.data.model.SantriModel
import com.alhasanah.alhasanahmedia.data.model.TagihanStatus
import com.alhasanah.alhasanahmedia.data.model.TagihanWithDetail
import com.alhasanah.alhasanahmedia.data.model.WalletTransactionDto
import com.alhasanah.alhasanahmedia.data.model.quran.SurahDetail
import com.alhasanah.alhasanahmedia.data.repository.BeritaRepository
import com.alhasanah.alhasanahmedia.data.repository.KeuanganRepository
import com.alhasanah.alhasanahmedia.data.repository.QuranRepository
import com.alhasanah.alhasanahmedia.data.repository.SantriActivityRepository
import com.alhasanah.alhasanahmedia.data.repository.WaliSantriRepository
import com.alhasanah.alhasanahmedia.data.repository.WalletRepository
import com.alhasanah.alhasanahmedia.util.PrayerManager
import com.alhasanah.alhasanahmedia.util.PrayerTimeInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

data class HomeSummaryChartItem(
    val label: String,
    val amount: Long
)

data class HomeSantriSummaryUiState(
    val shouldShow: Boolean = false,
    val isLoading: Boolean = false,
    val isExpanded: Boolean = false,
    val santriName: String = "",
    val santriMeta: String = "",
    val unpaidCount: Int = 0,
    val unpaidAmount: Long = 0L,
    val latestHafalan: String = "Belum ada data",
    val activePermit: String = "Aman",
    val violationSummary: String = "Aman",
    val healthSummary: String = "Aman",
    val chartTitle: String = "Komposisi tagihan",
    val chartItems: List<HomeSummaryChartItem> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val beritaRepository: BeritaRepository,
    private val prayerManager: PrayerManager,
    private val quranRepository: QuranRepository,
    private val waliSantriRepository: WaliSantriRepository,
    private val santriActivityRepository: SantriActivityRepository,
    private val keuanganRepository: KeuanganRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _beritaState = MutableStateFlow<List<BeritaModel>>(emptyList())
    val beritaState: StateFlow<List<BeritaModel>> = _beritaState.asStateFlow()

    private val _isLoadingBerita = MutableStateFlow(true)
    val isLoadingBerita: StateFlow<Boolean> = _isLoadingBerita.asStateFlow()

    private val _prayerState = MutableStateFlow<PrayerTimeInfo?>(null)
    val prayerState: StateFlow<PrayerTimeInfo?> = _prayerState.asStateFlow()

    private val _ayatOfTheDay = MutableStateFlow<SurahDetail?>(null)
    val ayatOfTheDay: StateFlow<SurahDetail?> = _ayatOfTheDay.asStateFlow()

    private val _santriSummary = MutableStateFlow(HomeSantriSummaryUiState())
    val santriSummary: StateFlow<HomeSantriSummaryUiState> = _santriSummary.asStateFlow()

    private var summaryJob: Job? = null
    private var loadedSummaryKey: String? = null

    init {
        loadLatestBerita()
        startPrayerUpdates()
        fetchAyatOfTheDay()
    }

    private fun fetchAyatOfTheDay() {
        viewModelScope.launch {
            // Pick a random surah (1-114)
            val randomSurah = Random.nextInt(1, 115)
            quranRepository.getSurahDetail(randomSurah).collect { result ->
                result.onSuccess {
                    _ayatOfTheDay.value = it
                }
            }
        }
    }

    private fun startPrayerUpdates() {
        viewModelScope.launch {
            while (isActive) {
                _prayerState.value = prayerManager.getNextPrayer()
                delay(1000) // Update every second for countdown
            }
        }
    }

    private fun loadLatestBerita() {
        viewModelScope.launch {
            _isLoadingBerita.value = true
            beritaRepository.getLatestBerita()
                .catch { 
                    _isLoadingBerita.value = false 
                }
                .collect { beritaList ->
                    _beritaState.value = beritaList
                    _isLoadingBerita.value = false
                }
        }
    }

    fun loadSantriSummary(nis: String?, role: String?) {
        val normalizedRole = role?.lowercase().orEmpty()
        val shouldHide = nis.isNullOrBlank() || normalizedRole == "alumni"
        if (shouldHide) {
            summaryJob?.cancel()
            loadedSummaryKey = null
            _santriSummary.value = HomeSantriSummaryUiState()
            return
        }

        val key = "$nis:$normalizedRole"
        if (loadedSummaryKey == key && _santriSummary.value.shouldShow) return
        loadedSummaryKey = key

        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            _santriSummary.value = HomeSantriSummaryUiState(shouldShow = true, isLoading = true)
            runCatching {
                val santriDeferred = async { withTimeoutOrNull(6_000) { waliSantriRepository.getSantriByNis(nis) } }
                val tagihanDeferred = async { loadLatestTagihanOrEmpty(nis) }
                val hafalanDeferred = async { loadFirstOrEmpty { santriActivityRepository.getHafalan(nis).first() } }
                val perizinanDeferred = async { loadFirstOrEmpty { santriActivityRepository.getPerizinan(nis).first() } }
                val pelanggaranDeferred = async { loadFirstOrEmpty { santriActivityRepository.getPelanggaran(nis).first() } }
                val kesehatanDeferred = async { loadFirstOrEmpty { santriActivityRepository.getKesehatan(nis).first() } }
                val walletDeferred = async {
                    withTimeoutOrNull(5_000) {
                        runCatching { walletRepository.getTransactions(nis) }.getOrDefault(emptyList())
                    }.orEmpty()
                }

                buildSummaryState(
                    santri = santriDeferred.await(),
                    tagihan = tagihanDeferred.await(),
                    hafalan = hafalanDeferred.await(),
                    perizinan = perizinanDeferred.await(),
                    pelanggaran = pelanggaranDeferred.await(),
                    kesehatan = kesehatanDeferred.await(),
                    walletTransactions = walletDeferred.await(),
                    keepExpanded = _santriSummary.value.isExpanded
                )
            }.onSuccess { state ->
                _santriSummary.value = state
            }.onFailure { error ->
                _santriSummary.value = HomeSantriSummaryUiState(
                    shouldShow = true,
                    isLoading = false,
                    errorMessage = error.message ?: "Ringkasan santri belum tersedia"
                )
            }
        }
    }

    fun setSantriSummaryExpanded(expanded: Boolean) {
        _santriSummary.value = _santriSummary.value.copy(isExpanded = expanded)
    }

    private suspend fun <T> loadFirstOrEmpty(block: suspend () -> List<T>): List<T> =
        withTimeoutOrNull(6_000) { runCatching { block() }.getOrDefault(emptyList()) }.orEmpty()

    private suspend fun loadLatestTagihanOrEmpty(nis: String): List<TagihanWithDetail> =
        withTimeoutOrNull(6_000) {
            runCatching {
                var latest = emptyList<TagihanWithDetail>()
                keuanganRepository.getTagihanByNis(nis).collect { latest = it }
                latest
            }.getOrDefault(emptyList())
        }.orEmpty()

    private fun buildSummaryState(
        santri: SantriModel?,
        tagihan: List<TagihanWithDetail>,
        hafalan: List<HafalanTahfidz>,
        perizinan: List<PerizinanSantri>,
        pelanggaran: List<PelanggaranSantri>,
        kesehatan: List<KesehatanSantri>,
        walletTransactions: List<WalletTransactionDto>,
        keepExpanded: Boolean
    ): HomeSantriSummaryUiState {
        val unpaidBills = tagihan.filter { it.isUnpaidBill() }
        val unpaidAmount = unpaidBills.sumOf { it.remainingBillAmount() }
        val latestHafalan = hafalan.firstOrNull()?.let { item ->
            listOfNotNull(item.surat, item.juz?.let { "Juz $it" }, item.predikat ?: item.status)
                .joinToString(" • ")
                .ifBlank { "Hafalan terbaru tercatat" }
        } ?: "Belum ada data"
        val activePermit = perizinan.firstOrNull()?.let { item ->
            listOfNotNull(
                item.tanggal?.toCompactDateLabel(),
                item.jenis_izin,
                item.status
            ).joinToString(" • ").ifBlank { "Catatan izin terakhir" }
        } ?: "Tidak ada catatan"
        val violationSummary = pelanggaran.firstOrNull()?.let { item ->
            listOfNotNull(
                item.tanggal?.toCompactDateLabel(),
                item.jenis_pelanggaran,
                item.poin?.let { "$it poin" }
            ).joinToString(" • ").ifBlank { "Catatan terakhir" }
        } ?: "Tidak ada catatan"
        val healthSummary = kesehatan.firstOrNull()?.let { item ->
            listOfNotNull(
                item.tanggal?.toCompactDateLabel(),
                item.keluhan
            ).joinToString(" • ").ifBlank { "Catatan kesehatan terakhir" }
        } ?: "Tidak ada catatan"
        val walletExpenses = walletTransactions
            .filter { it.direction.equals("debit", ignoreCase = true) }
            .groupBy { it.category.ifBlank { "Pengeluaran" } }
            .map { (category, items) -> HomeSummaryChartItem(category.toDisplayLabel(), items.sumOf { it.amount }) }
            .filter { it.amount > 0L }
            .sortedByDescending { it.amount }
            .take(4)
        val billChart = unpaidBills
            .groupBy { it.refJenisPembayaran?.namaPembayaran ?: it.deskripsiTagihan }
            .map { (label, items) -> HomeSummaryChartItem(label.toDisplayLabel(), items.sumOf { it.remainingBillAmount() }) }
            .filter { it.amount > 0L }
            .sortedByDescending { it.amount }
            .take(4)
        return HomeSantriSummaryUiState(
            shouldShow = true,
            isLoading = false,
            isExpanded = keepExpanded,
            santriName = santri?.namaLengkap.orEmpty().ifBlank { "Santri aktif" },
            santriMeta = listOfNotNull(santri?.kelas?.let { "Kelas $it" }, santri?.jurusan).joinToString(" • "),
            unpaidCount = unpaidBills.size,
            unpaidAmount = unpaidAmount,
            latestHafalan = latestHafalan,
            activePermit = activePermit,
            violationSummary = violationSummary,
            healthSummary = healthSummary,
            chartTitle = if (walletExpenses.isNotEmpty()) "Pengeluaran dompet" else "Komposisi tagihan",
            chartItems = if (walletExpenses.isNotEmpty()) walletExpenses else billChart
        )
    }

    private fun String.toDisplayLabel(): String =
        replace("_", " ")
            .lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            .take(18)

    private fun String.toCompactDateLabel(): String =
        runCatching {
            val parts = take(10).split("-")
            if (parts.size == 3) "${parts[2]}/${parts[1]}" else take(10)
        }.getOrDefault(take(10))

    private fun TagihanWithDetail.remainingBillAmount(): Long =
        when {
            sisaTagihan != null -> sisaTagihan
            status == TagihanStatus.LUNAS -> 0L
            else -> nominalTagihan ?: 0L
        }

    private fun TagihanWithDetail.isUnpaidBill(): Boolean =
        status != TagihanStatus.LUNAS && remainingBillAmount() > 0L
}
