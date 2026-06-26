package com.alhasanah.alhasanahmedia.ui.keuangan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.BuildConfig
import com.alhasanah.alhasanahmedia.data.model.CorePaymentData
import com.alhasanah.alhasanahmedia.data.model.CorePaymentMethod
import com.alhasanah.alhasanahmedia.data.model.CorePaymentRequest
import com.alhasanah.alhasanahmedia.data.model.CorePaymentResponse
import com.alhasanah.alhasanahmedia.data.model.CustomerDetails
import com.alhasanah.alhasanahmedia.data.model.ItemDetail
import com.alhasanah.alhasanahmedia.data.model.PembayaranTagihanDto
import com.alhasanah.alhasanahmedia.data.model.TagihanDto
import com.alhasanah.alhasanahmedia.data.model.TagihanStatus
import com.alhasanah.alhasanahmedia.data.model.TagihanWithDetail
import com.alhasanah.alhasanahmedia.data.repository.AuthRepository
import com.alhasanah.alhasanahmedia.data.repository.KeuanganRepository
import com.alhasanah.alhasanahmedia.data.repository.WaliSantriRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface TagihanUiState {
    data object Loading : TagihanUiState
    data class Success(val tagihan: List<TagihanWithDetail>) : TagihanUiState
    data class Error(val message: String) : TagihanUiState
}

sealed interface SantriInfoState {
    data object Loading : SantriInfoState

    data class Success(val santriInfo: SantriInfo) : SantriInfoState
    data class Error(val message: String) : SantriInfoState
}

sealed interface PembayaranTagihanUiState {
    data object Idle : PembayaranTagihanUiState
    data object Loading : PembayaranTagihanUiState
    data class Success(val items: List<PembayaranTagihanDto>) : PembayaranTagihanUiState
    data class Error(val message: String) : PembayaranTagihanUiState
}

sealed interface RiwayatPembayaranUiState {
    data object Loading : RiwayatPembayaranUiState
    data class Success(val items: List<PembayaranTagihanDto>) : RiwayatPembayaranUiState
    data class Error(val message: String) : RiwayatPembayaranUiState
}

data class SantriInfo(
    val nis: String,
    val nama: String,
    val kelas: String,
    val noKontakWali: String? = null
)

class KeuanganViewModel(
    private val santriNis: String,
    private val keuanganRepository: KeuanganRepository,
    private val authRepository: AuthRepository,
    private val supabaseClient: SupabaseClient,
    private val waliSantriRepository: WaliSantriRepository
) : ViewModel() {

    companion object {
        private const val EDGE_FUNCTION_URL = "https://sldobkbolvrahlnowrga.supabase.co/functions/v1/midtrans-core-charge"
    }

    private val _tagihanState = MutableStateFlow<TagihanUiState>(TagihanUiState.Loading)
    val tagihanState: StateFlow<TagihanUiState> = _tagihanState.asStateFlow()

    private val _santriInfoState = MutableStateFlow<SantriInfoState>(SantriInfoState.Loading)
    val santriInfoState: StateFlow<SantriInfoState> = _santriInfoState.asStateFlow()

    private val _pembayaranTagihanState =
        MutableStateFlow<Map<String, PembayaranTagihanUiState>>(emptyMap())
    val pembayaranTagihanState: StateFlow<Map<String, PembayaranTagihanUiState>> =
        _pembayaranTagihanState.asStateFlow()

    private val _riwayatPembayaranState =
        MutableStateFlow<RiwayatPembayaranUiState>(RiwayatPembayaranUiState.Loading)
    val riwayatPembayaranState: StateFlow<RiwayatPembayaranUiState> =
        _riwayatPembayaranState.asStateFlow()

    private val _launchCorePayment = MutableSharedFlow<CorePaymentData>(replay = 1)
    val launchCorePayment: SharedFlow<CorePaymentData> = _launchCorePayment.asSharedFlow()

    private val _currentOrderId = MutableStateFlow<String?>(null)
    val currentOrderId: StateFlow<String?> = _currentOrderId.asStateFlow()

    private val _paymentSuccessEvent = MutableSharedFlow<String>()
    val paymentSuccessEvent: SharedFlow<String> = _paymentSuccessEvent.asSharedFlow()

    private val client = HttpClient(Android) { 
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true 
                isLenient = true
                encodeDefaults = true // PENTING: Agar field dengan default value (seperti quantity=1) tetap dikirim
            })
        }
    }
    private var jobMonitoring: Job? = null
    private val isPublicDonation = santriNis.isBlank() || santriNis.equals("public", ignoreCase = true)

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _tagihanState.value = TagihanUiState.Loading
            
            if (isPublicDonation) {
                // Untuk akses publik (Donasi), jangan tampilkan error, cukup set success kosong
                _tagihanState.value = TagihanUiState.Success(emptyList())
                _santriInfoState.value = SantriInfoState.Error("Akses Publik") // Diabaikan oleh DonasiScreen
                return@launch
            }

            getSantriInfo(santriNis)
            loadTagihan(santriNis)
            loadRiwayatPembayaran(santriNis)
        }
    }
    
    private fun getSantriInfo(nis: String) {
        viewModelScope.launch {
            _santriInfoState.value = SantriInfoState.Loading
            try {
                val profile = waliSantriRepository.getSantriByNis(nis)
                _santriInfoState.value = SantriInfoState.Success(
                    SantriInfo(
                        nis = profile.id,
                        nama = profile.namaLengkap,
                        kelas = profile.kelas.orEmpty(),
                        noKontakWali = profile.noKontakWali
                    )
                )
            } catch (e: Exception) {
                _santriInfoState.value = SantriInfoState.Error("Gagal mengambil data santri.")
            }
        }
    }
    
    private fun loadTagihan(nis: String) {
        viewModelScope.launch {
            keuanganRepository.getTagihanByNis(nis)
                .catch { e -> _tagihanState.value = TagihanUiState.Error(e.message ?: "Gagal memuat tagihan") }
                .collect { tagihanList -> _tagihanState.value = TagihanUiState.Success(tagihanList) }
        }
    }

    private fun loadRiwayatPembayaran(nis: String) {
        viewModelScope.launch {
            _riwayatPembayaranState.value = RiwayatPembayaranUiState.Loading
            try {
                _riwayatPembayaranState.value = RiwayatPembayaranUiState.Success(
                    keuanganRepository.getPembayaranTagihanByNis(nis)
                )
            } catch (e: Exception) {
                _riwayatPembayaranState.value =
                    RiwayatPembayaranUiState.Error(e.message ?: "Gagal memuat riwayat pembayaran")
            }
        }
    }
    
    fun loadPembayaranTagihan(tagihanId: String) {
        viewModelScope.launch {
            _pembayaranTagihanState.value =
                _pembayaranTagihanState.value + (tagihanId to PembayaranTagihanUiState.Loading)
            try {
                val payments = keuanganRepository.getPembayaranTagihan(tagihanId)
                _pembayaranTagihanState.value =
                    _pembayaranTagihanState.value + (tagihanId to PembayaranTagihanUiState.Success(payments))
            } catch (e: Exception) {
                _pembayaranTagihanState.value =
                    _pembayaranTagihanState.value + (
                        tagihanId to PembayaranTagihanUiState.Error(e.message ?: "Gagal memuat riwayat cicilan")
                    )
            }
        }
    }

    fun bayarTagihan(
        tagihan: TagihanWithDetail,
        amount: Long,
        paymentMethod: CorePaymentMethod = CorePaymentMethod.QRIS
    ) {
         viewModelScope.launch {
            _tagihanState.value = TagihanUiState.Loading
            try {
                val remaining = tagihan.sisaTagihan ?: 0L
                val paymentAmount = amount.coerceIn(0L, remaining)
                if (paymentAmount <= 0L) {
                    _tagihanState.value = TagihanUiState.Error("Nominal pembayaran tidak valid.")
                    return@launch
                }

                // --- 1. LOGIKA ANTI-NULL NAME & PHONE ---
                val santriInfo = (santriInfoState.value as? SantriInfoState.Success)?.santriInfo
                val safeName = if (santriInfo?.nama.isNullOrBlank()) "Wali Santri" else santriInfo!!.nama
                val safePhone = if (santriInfo?.noKontakWali.isNullOrBlank()) "081234567890" else santriInfo!!.noKontakWali!!

                val userEmail = authRepository.getCurrentUser().firstOrNull()?.email ?: "pembayaran@santri.com"

                // --- 2. LOGIKA ORDER ID ---
                // Cukup kirim tagihan.id, karena Edge Function midtrans-snap 
                // akan menambahkan timestamp (_123456) secara otomatis.
                val uniqueOrderId = tagihan.id

                // --- 3. SUSUN BODY REQUEST ---
                val isPartial = paymentAmount < remaining
                val requestPayload = CorePaymentRequest(
                    transactionType = "tagihan",
                    paymentMethod = paymentMethod.code,
                    orderId = uniqueOrderId,
                    grossAmount = paymentAmount,
                    customerDetails = CustomerDetails(
                        firstName = safeName,
                        email = userEmail,
                        phone = safePhone
                    ),
                    itemDetails = listOf(
                        ItemDetail(
                            id = tagihan.id,
                            name = if (isPartial) "Cicilan ${tagihan.deskripsiTagihan}" else tagihan.deskripsiTagihan,
                            price = paymentAmount
                        )
                    ),
                    santriNis = tagihan.santriNis
                )

                Log.d("KeuanganViewModel", "Kirim Tagihan ID ke Midtrans: ${tagihan.id}")
                Log.d("KeuanganViewModel", "Order ID: $uniqueOrderId")
                _currentOrderId.value = uniqueOrderId

                // --- 4. KIRIM KE EDGE FUNCTION ---
                val response: CorePaymentResponse = client.post(EDGE_FUNCTION_URL) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(currentBearerToken())
                    header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    setBody(requestPayload)
                }.body()

                if (response.data != null) {
                    _launchCorePayment.emit(response.data)
                    _currentOrderId.value = response.data.orderId
                    mulaiPantauStatus(tagihan.id, response.data.orderId)
                } else {
                    val errorMessage = response.errorMessages?.joinToString() ?: response.error ?: "Gagal membuat pembayaran."
                    _tagihanState.value = TagihanUiState.Error(errorMessage)
                }

            } catch (e: Exception) {
                Log.e("KeuanganViewModel", "Error Bayar: ${e.message}", e)
                _tagihanState.value = TagihanUiState.Error("Gagal menghubungi server: ${e.message}")
            }
        }
    }

    fun bayarDonasi(
        nominal: Long,
        jenis: String,
        namaDonatur: String,
        pesan: String = "",
        paymentMethod: CorePaymentMethod = CorePaymentMethod.QRIS
    ) {
        viewModelScope.launch {
            _tagihanState.value = TagihanUiState.Loading
            try {
                Log.d("KeuanganViewModel", "Memulai proses donasi")
                val user = authRepository.getCurrentUser().firstOrNull()
                val userEmail = user?.email ?: "donatur@alhasanah.com"
                
                // Santri info untuk metadata jika ada
                val santriInfo = (santriInfoState.value as? SantriInfoState.Success)?.santriInfo

                val request = CorePaymentRequest(
                    transactionType = "donation",
                    paymentMethod = paymentMethod.code,
                    grossAmount = nominal,
                    notes = pesan,
                    itemDetails = listOf(
                        ItemDetail(
                            id = "DONASI_${jenis.uppercase()}",
                            name = jenis, // Cukup kirim jenis (Infaq/Wakaf) agar Order ID tidak terlalu panjang
                            price = nominal
                        )
                    ),
                    customerDetails = CustomerDetails(
                        firstName = if (namaDonatur.length > 20) namaDonatur.take(20) else namaDonatur,
                        email = userEmail,
                        phone = santriInfo?.noKontakWali ?: "081234567890"
                    ),
                    santriNis = if (!isPublicDonation) santriNis else null
                )

                Log.d("KeuanganViewModel", "Kirim request donasi ke Edge Function...")
                val response: CorePaymentResponse = client.post(EDGE_FUNCTION_URL) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(currentBearerToken(allowAnon = true))
                    header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    setBody(request)
                }.body()

                Log.d("KeuanganViewModel", "Response donasi diterima")

                if (response.data != null) {
                    _launchCorePayment.emit(response.data)
                    _tagihanState.value = TagihanUiState.Success(emptyList()) 
                } else {
                    // Coba ambil detail error jika ada
                    val midtransError = response.details?.error_messages?.joinToString()
                    val errorMessage = midtransError ?: response.errorMessages?.joinToString() ?: response.error ?: "Gagal memproses donasi."
                    _tagihanState.value = TagihanUiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                Log.e("KeuanganViewModel", "Error Donasi: ${e.message}", e)
                _tagihanState.value = TagihanUiState.Error("Gagal menghubungi server: ${e.message}")
            }
        }
    }

    fun mulaiPantauStatus(tagihanId: String, orderId: String? = null) {
        jobMonitoring?.cancel()
        jobMonitoring = viewModelScope.launch {
            var paymentPosted = false
            while (isActive && !paymentPosted) {
                try {
                    val transactionStatus = orderId?.let { currentOrderId ->
                        supabaseClient.from("transaksi_keuangan")
                            .select {
                                filter {
                                    eq("midtrans_order_id", currentOrderId)
                                }
                                order("tanggal_transaksi", Order.DESCENDING)
                            }
                            .decodeList<TagihanTransactionStatusRow>()
                            .firstOrNull()
                    }

                    val result = supabaseClient.from("tagihan_santri")
                        .select {
                           filter {
                                eq("id", tagihanId)
                            }
                        }
                        .decodeSingle<TagihanDto>()

                    val transactionSuccess = transactionStatus?.status.equals("success", ignoreCase = true) ||
                        transactionStatus?.statusTransaksi?.lowercase() in setOf("settlement", "capture", "paid")
                    if (result.status == TagihanStatus.LUNAS || (transactionSuccess && result.status == TagihanStatus.CICILAN)) {
                        paymentPosted = true
                        val message = if (result.status == TagihanStatus.CICILAN) {
                            "Pembayaran cicilan berhasil. Sisa tagihan ${result.sisaTagihan ?: 0}."
                        } else {
                            "Pembayaran tagihan lunas."
                        }
                        Log.d("PAYMENT_MONITOR", "Pembayaran terkonfirmasi: ${result.status}")
                        _paymentSuccessEvent.emit(message)
                        refreshData()
                        if (!isPublicDonation) loadRiwayatPembayaran(santriNis)
                        loadPembayaranTagihan(tagihanId)
                        hentikanPantauan()
                    } else {
                        Log.d("PAYMENT_MONITOR", "Status saat ini: ${result.status}. Masih menunggu pembayaran...")
                    }
                } catch (e: Exception) {
                    Log.e("PAYMENT_MONITOR", "Gagal cek status: ${e.message}")
                    hentikanPantauan()
                }
                delay(5000)
            }
        }
    }

    fun hentikanPantauan() { jobMonitoring?.cancel() }

    override fun onCleared() {
        super.onCleared()
        client.close()
        hentikanPantauan()
    }

    private suspend fun currentBearerToken(allowAnon: Boolean = false): String {
        supabaseClient.auth.awaitInitialization()
        return supabaseClient.auth.currentAccessTokenOrNull()
            ?: if (allowAnon) BuildConfig.SUPABASE_ANON_KEY else throw IllegalStateException("Sesi login tidak valid.")
    }
}

@Serializable
private data class TagihanTransactionStatusRow(
    @SerialName("status")
    val status: String? = null,
    @SerialName("status_transaksi")
    val statusTransaksi: String? = null
)
