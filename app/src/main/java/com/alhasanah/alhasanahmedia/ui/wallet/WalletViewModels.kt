package com.alhasanah.alhasanahmedia.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.CorePaymentMethod
import com.alhasanah.alhasanahmedia.data.model.KantinAuthorizationDto
import com.alhasanah.alhasanahmedia.data.model.KantinCardLookupDto
import com.alhasanah.alhasanahmedia.data.model.WalletAccountDto
import com.alhasanah.alhasanahmedia.data.model.WalletLimitUpdateRequest
import com.alhasanah.alhasanahmedia.data.model.WalletMerchantContext
import com.alhasanah.alhasanahmedia.data.model.WalletTransactionDto
import com.alhasanah.alhasanahmedia.data.repository.KantinDeviceLocalInfo
import com.alhasanah.alhasanahmedia.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WalletWaliUiState(
    val loading: Boolean = true,
    val account: WalletAccountDto? = null,
    val transactions: List<WalletTransactionDto> = emptyList(),
    val error: String? = null,
    val info: String? = null
)

data class WalletTopUpLaunch(
    val orderId: String,
    val transactionId: String,
    val methodCode: String,
    val methodLabel: String,
    val amount: Long,
    val expiresAt: String,
    val qrUrl: String,
    val deeplinkUrl: String,
    val vaNumber: String,
    val bank: String,
    val billerCode: String,
    val billKey: String,
    val paymentCode: String,
    val store: String
)

class WalletWaliViewModel(
    private val santriNis: String,
    private val repository: WalletRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WalletWaliUiState())
    val uiState: StateFlow<WalletWaliUiState> = _uiState.asStateFlow()

    private val _launchTopUp = MutableSharedFlow<WalletTopUpLaunch>()
    val launchTopUp: SharedFlow<WalletTopUpLaunch> = _launchTopUp.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, info = null) }
            runCatching {
                val account = repository.getAccount(santriNis)
                val transactions = repository.getTransactions(santriNis)
                _uiState.update {
                    it.copy(loading = false, account = account, transactions = transactions)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(loading = false, error = error.userMessage())
                }
            }
        }
    }

    fun registerWallet(deviceName: String, pin: CharArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, info = null) }
            runCatching {
                repository.registerWallet(santriNis, deviceName, pin)
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(loading = false, account = it, info = "Dompet santri berhasil diaktifkan.")
                }
                refresh()
            }.onFailure { error ->
                _uiState.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun updateLimits(
        lowBalanceThreshold: Long?,
        singleTransactionLimit: Long?,
        dailySpendLimit: Long?,
        monthlySpendLimit: Long?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, info = null) }
            runCatching {
                repository.updateLimits(
                    WalletLimitUpdateRequest(
                        santriNis = santriNis,
                        lowBalanceThreshold = lowBalanceThreshold,
                        singleTransactionLimit = singleTransactionLimit,
                        dailySpendLimit = dailySpendLimit,
                        monthlySpendLimit = monthlySpendLimit
                    )
                )
            }.onSuccess { account ->
                _uiState.update {
                    it.copy(loading = false, account = account, info = "Limit dompet berhasil disimpan.")
                }
            }.onFailure { error ->
                _uiState.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun createTopUp(amountText: String, paymentMethod: CorePaymentMethod = CorePaymentMethod.QRIS) {
        val amount = amountText.filter(Char::isDigit).toLongOrNull()
        if (amount == null || amount < 10_000L) {
            _uiState.update { it.copy(error = "Nominal top up minimal Rp10.000.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, info = null) }
            runCatching {
                repository.createTopUp(santriNis, amount, paymentMethod)
            }.onSuccess { topUp ->
                val orderId = topUp.orderId
                val methodCode = topUp.methodCode ?: paymentMethod.code
                val methodLabel = topUp.methodLabel ?: paymentMethod.label
                if (orderId.isNullOrBlank()) {
                    _uiState.update { it.copy(loading = false, error = "Data pembayaran top up belum tersedia.") }
                    return@onSuccess
                }
                _uiState.update {
                    it.copy(loading = false, info = "Instruksi pembayaran top up sudah dibuat.")
                }
                _launchTopUp.emit(
                    WalletTopUpLaunch(
                        orderId = orderId,
                        transactionId = topUp.transactionId.orEmpty(),
                        methodCode = methodCode,
                        methodLabel = methodLabel,
                        amount = topUp.amount,
                        expiresAt = topUp.expiresAt.orEmpty(),
                        qrUrl = topUp.qrUrl.orEmpty(),
                        deeplinkUrl = topUp.deeplinkUrl.orEmpty(),
                        vaNumber = topUp.vaNumber ?: topUp.permataVaNumber.orEmpty(),
                        bank = topUp.bank.orEmpty(),
                        billerCode = topUp.billerCode.orEmpty(),
                        billKey = topUp.billKey.orEmpty(),
                        paymentCode = topUp.paymentCode.orEmpty(),
                        store = topUp.store.orEmpty()
                    )
                )
            }.onFailure { error ->
                _uiState.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }
}

data class KantinWalletUiState(
    val loading: Boolean = false,
    val deviceInfo: KantinDeviceLocalInfo? = null,
    val registeredDevice: com.alhasanah.alhasanahmedia.data.model.KantinDeviceDto? = null,
    val merchantContext: WalletMerchantContext? = null,
    val qrPayload: String = "",
    val cardLookup: KantinCardLookupDto? = null,
    val cardLookupLoading: Boolean = false,
    val amount: String = "",
    val settlementAmount: String = "",
    val settlementNote: String = "",
    val authorization: KantinAuthorizationDto? = null,
    val history: List<WalletTransactionDto> = emptyList(),
    val error: String? = null,
    val info: String? = null
)

class KantinWalletViewModel(
    private val repository: WalletRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(KantinWalletUiState())
    val uiState: StateFlow<KantinWalletUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(deviceInfo = repository.getKantinDeviceInfo()) }
        refreshAll()
    }

    fun refreshAll() {
        refreshMerchantContext()
        loadHistory()
    }

    fun refreshMerchantContext() {
        viewModelScope.launch {
            runCatching {
                val device = repository.getCurrentKantinDevice()
                val context = repository.getKantinMerchantContext()
                device to context
            }
                .onSuccess { (device, context) ->
                    _uiState.update { it.copy(registeredDevice = device, merchantContext = context) }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.userMessage()) } }
        }
    }

    fun registerDevice() {
        val currentDevice = _uiState.value.registeredDevice ?: _uiState.value.merchantContext?.device
        if (currentDevice?.status == "active") {
            _uiState.update { it.copy(info = "Perangkat ini sudah aktif dan siap dipakai.") }
            return
        }
        if (currentDevice?.status == "pending") {
            _uiState.update { it.copy(info = "Perangkat sudah didaftarkan. Tunggu admin mengaktifkan perangkat ini.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, info = null) }
            runCatching { repository.registerKantinDevice() }
                .onSuccess { device ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            registeredDevice = device,
                            info = if (device.status == "active") "Perangkat aktif dan siap transaksi." else "Perangkat berhasil didaftarkan. Tunggu admin mengaktifkan perangkat ini."
                        )
                    }
                    refreshMerchantContext()
                }
                .onFailure { error -> _uiState.update { it.copy(loading = false, error = error.userMessage()) } }
        }
    }

    fun setQrPayload(value: String) {
        val normalized = value.trim()
        _uiState.update {
            it.copy(
                qrPayload = normalized,
                cardLookup = null,
                cardLookupLoading = normalized.isNotBlank(),
                error = null,
                info = null
            )
        }
        if (normalized.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.lookupKantinCard(normalized) }
                .onSuccess { lookup ->
                    _uiState.update { current ->
                        if (current.qrPayload == normalized) {
                            current.copy(cardLookup = lookup, cardLookupLoading = false)
                        } else {
                            current
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { current ->
                        if (current.qrPayload == normalized) {
                            current.copy(cardLookupLoading = false, error = error.userMessage())
                        } else {
                            current
                        }
                    }
                }
        }
    }

    fun setAmount(value: String) {
        _uiState.update { it.copy(amount = value.filter(Char::isDigit), error = null, info = null) }
    }

    fun setSettlementAmount(value: String) {
        _uiState.update { it.copy(settlementAmount = value.filter(Char::isDigit), error = null, info = null) }
    }

    fun setSettlementNote(value: String) {
        _uiState.update { it.copy(settlementNote = value, error = null, info = null) }
    }

    fun authorize() {
        val state = _uiState.value
        val amount = state.amount.toLongOrNull()
        if (state.qrPayload.isBlank() || amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Scan kartu santri dan isi nominal belanja terlebih dahulu.") }
            return
        }
        val currentDevice = state.registeredDevice ?: state.merchantContext?.device
        if (currentDevice?.status != "active") {
            _uiState.update { it.copy(error = "Perangkat ini belum aktif. Minta admin mengaktifkan perangkat kantin.") }
            return
        }
        if (state.merchantContext == null) {
            _uiState.update { it.copy(error = "Akun kantin belum siap. Minta admin membuka Manajemen Kantin dan menekan Siapkan Otomatis.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, info = null) }
            runCatching {
                repository.createKantinAuthorization(state.qrPayload, amount)
            }.onSuccess { authorization ->
                val message = if (authorization.amount > 75_000L) {
                    "Menunggu approval wali untuk transaksi di atas Rp75.000."
                } else {
                    "Transaksi siap. Minta santri memasukkan PIN."
                }
                _uiState.update {
                    it.copy(loading = false, authorization = authorization, info = message)
                }
                loadHistory()
                refreshMerchantContext()
            }.onFailure { error ->
                _uiState.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun confirmWithStudentPin(pin: CharArray) {
        val authorization = _uiState.value.authorization
        if (authorization == null) {
            _uiState.update { it.copy(error = "Buat otorisasi transaksi terlebih dahulu.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, info = null) }
            runCatching {
                repository.confirmStudentPin(authorization, pin)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        loading = false,
                        info = "Pembayaran berhasil diproses.",
                        amount = "",
                        qrPayload = "",
                        cardLookup = null,
                        cardLookupLoading = false,
                        authorization = null
                    )
                }
                loadHistory()
                refreshMerchantContext()
            }.onFailure { error ->
                _uiState.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            runCatching { repository.getKantinHistory() }
                .onSuccess { history -> _uiState.update { it.copy(history = history) } }
        }
    }

    fun requestSettlement() {
        val state = _uiState.value
        val context = state.merchantContext
        val amount = state.settlementAmount.filter(Char::isDigit).toLongOrNull()
        if (context == null || amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Nominal pencairan wajib diisi dengan benar.") }
            return
        }
        val available = context.balance?.saldoAvailable ?: 0L
        if (amount > available) {
            _uiState.update {
                it.copy(
                    error = "Nominal pencairan melebihi saldo kantin tersedia (${formatRupiahText(available)}).",
                    info = null
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, info = null) }
            runCatching {
                repository.requestMerchantSettlement(
                    merchantId = context.assignment.merchantId,
                    outletId = context.assignment.outletId,
                    amount = amount,
                    note = state.settlementNote.ifBlank { null }
                )
            }.onSuccess {
                _uiState.update { ui ->
                    ui.copy(
                        loading = false,
                        settlementAmount = "",
                        settlementNote = "",
                        info = "Pengajuan pencairan dikirim ke bendahara pesantren."
                    )
                }
                refreshMerchantContext()
            }.onFailure { error ->
                _uiState.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    private fun formatRupiahText(value: Long): String {
        val raw = value.toString()
        val grouped = raw.reversed().chunked(3).joinToString(".").reversed()
        return "Rp$grouped"
    }
}

data class WalletDisputeUiState(
    val loading: Boolean = false,
    val santriNis: String = "",
    val reason: String = "",
    val error: String? = null,
    val info: String? = null
)

class WalletDisputeViewModel(
    private val ledgerId: Long,
    private val repository: WalletRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WalletDisputeUiState())
    val uiState: StateFlow<WalletDisputeUiState> = _uiState.asStateFlow()

    fun setSantriNis(value: String) {
        _uiState.update { it.copy(santriNis = value.trim(), error = null) }
    }

    fun setReason(value: String) {
        _uiState.update { it.copy(reason = value, error = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.santriNis.isBlank() || state.reason.trim().length < 10) {
            _uiState.update { it.copy(error = "Isi NIS santri dan alasan minimal 10 karakter.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, info = null) }
            runCatching {
                repository.createDispute(ledgerId, state.santriNis, state.reason)
            }.onSuccess {
                _uiState.update {
                    it.copy(loading = false, info = "Laporan dikirim. Pesantren akan memeriksa transaksi ini.")
                }
            }.onFailure { error ->
                _uiState.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }
}

private fun Throwable.userMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Terjadi kendala. Silakan coba lagi."
