package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.BuildConfig
import com.alhasanah.alhasanahmedia.data.model.KantinAuthorizationDto
import com.alhasanah.alhasanahmedia.data.model.KantinAuthorizeRequest
import com.alhasanah.alhasanahmedia.data.model.KantinAuthorizeResponse
import com.alhasanah.alhasanahmedia.data.model.KantinCardLookupDto
import com.alhasanah.alhasanahmedia.data.model.KantinCardLookupRequest
import com.alhasanah.alhasanahmedia.data.model.KantinCardLookupResponse
import com.alhasanah.alhasanahmedia.data.model.CorePaymentMethod
import com.alhasanah.alhasanahmedia.data.model.KantinDeviceDto
import com.alhasanah.alhasanahmedia.data.model.KantinDeviceRegisterRequest
import com.alhasanah.alhasanahmedia.data.model.KantinDeviceRegisterResponse
import com.alhasanah.alhasanahmedia.data.model.WalletAccountDto
import com.alhasanah.alhasanahmedia.data.model.WalletApiError
import com.alhasanah.alhasanahmedia.data.model.WalletConfirmRequest
import com.alhasanah.alhasanahmedia.data.model.WalletDisputeDto
import com.alhasanah.alhasanahmedia.data.model.WalletDisputeCreate
import com.alhasanah.alhasanahmedia.data.model.WalletDisputeResponse
import com.alhasanah.alhasanahmedia.data.model.WalletLimitUpdateRequest
import com.alhasanah.alhasanahmedia.data.model.WalletMerchantBalanceDto
import com.alhasanah.alhasanahmedia.data.model.WalletMerchantContext
import com.alhasanah.alhasanahmedia.data.model.WalletMerchantDto
import com.alhasanah.alhasanahmedia.data.model.WalletMerchantOutletDto
import com.alhasanah.alhasanahmedia.data.model.WalletMerchantUserDto
import com.alhasanah.alhasanahmedia.data.model.WalletPinKdfDto
import com.alhasanah.alhasanahmedia.data.model.WalletPinChallengeKdfDto
import com.alhasanah.alhasanahmedia.data.model.WalletRegisterRequest
import com.alhasanah.alhasanahmedia.data.model.WalletRegisterResponse
import com.alhasanah.alhasanahmedia.data.model.WalletSettlementRequestCreate
import com.alhasanah.alhasanahmedia.data.model.WalletSettlementRequestDto
import com.alhasanah.alhasanahmedia.data.model.WalletSettlementRequestResponse
import com.alhasanah.alhasanahmedia.data.model.WalletStudentPinConfirmRequest
import com.alhasanah.alhasanahmedia.data.model.WalletTransactionDto
import com.alhasanah.alhasanahmedia.data.model.WalletTopUpCreateRequest
import com.alhasanah.alhasanahmedia.data.model.WalletTopUpCreateResponse
import com.alhasanah.alhasanahmedia.data.model.WalletTopUpDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject

interface WalletRepository {
    fun getKantinDeviceInfo(): KantinDeviceLocalInfo
    suspend fun getCurrentKantinDevice(): KantinDeviceDto?
    suspend fun registerKantinDevice(): KantinDeviceDto
    suspend fun getKantinMerchantContext(): WalletMerchantContext?
    suspend fun getAccount(santriNis: String): WalletAccountDto?
    suspend fun getTransactions(santriNis: String): List<WalletTransactionDto>
    suspend fun registerWallet(santriNis: String, deviceName: String, pin: CharArray): WalletAccountDto
    suspend fun updateLimits(request: WalletLimitUpdateRequest): WalletAccountDto
    suspend fun createTopUp(santriNis: String, amount: Long, paymentMethod: CorePaymentMethod): WalletTopUpDto
    suspend fun lookupKantinCard(qrPayload: String): KantinCardLookupDto
    suspend fun createKantinAuthorization(qrPayload: String, amount: Long): KantinAuthorizationDto
    suspend fun confirmStudentPin(session: KantinAuthorizationDto, pin: CharArray)
    suspend fun confirmParentApproval(session: KantinAuthorizationDto)
    suspend fun getKantinHistory(): List<WalletTransactionDto>
    suspend fun requestMerchantSettlement(merchantId: String, outletId: String?, amount: Long, note: String?): WalletSettlementRequestDto
    suspend fun createDispute(ledgerId: Long, santriNis: String, reason: String): WalletDisputeDto
}

class WalletRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val crypto: WalletDeviceCrypto,
    private val securityGuard: WalletSecurityGuard
) : WalletRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client = HttpClient(Android) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    override fun getKantinDeviceInfo(): KantinDeviceLocalInfo {
        val publicKey = crypto.ensureWalletKey(KANTIN_KEY_ALIAS)
        return KantinDeviceLocalInfo(
            deviceId = crypto.deviceId(),
            deviceFingerprint = "android:${android.os.Build.MANUFACTURER}:${android.os.Build.MODEL}:${crypto.deviceId()}",
            publicKey = publicKey.publicKeyBase64
        )
    }

    override suspend fun getCurrentKantinDevice(): KantinDeviceDto? {
        supabaseClient.auth.awaitInitialization()
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return null
        return supabaseClient.from("kantin_devices")
            .select {
                filter {
                    eq("kantin_user_id", userId)
                    eq("device_id", crypto.deviceId())
                }
                limit(1)
            }
            .decodeList<KantinDeviceDto>()
            .firstOrNull()
    }

    override suspend fun registerKantinDevice(): KantinDeviceDto {
        securityGuard.assertSensitiveWalletOperationAllowed()
        val info = getKantinDeviceInfo()
        val response: KantinDeviceRegisterResponse = callFunction(
            "wallet-kantin-register-device",
            KantinDeviceRegisterRequest(
                deviceId = info.deviceId,
                deviceFingerprint = info.deviceFingerprint,
                publicKey = info.publicKey
            )
        )
        return response.data
    }

    override suspend fun getKantinMerchantContext(): WalletMerchantContext? {
        supabaseClient.auth.awaitInitialization()
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return null
        val assignment = supabaseClient.from("wallet_merchant_users")
            .select {
                filter {
                    eq("profile_id", userId)
                    eq("status", "active")
                }
                limit(1)
            }
            .decodeList<WalletMerchantUserDto>()
            .firstOrNull() ?: return null

        val merchant = supabaseClient.from("wallet_merchants")
            .select {
                filter {
                    eq("id", assignment.merchantId)
                    eq("status", "active")
                }
                limit(1)
            }
            .decodeList<WalletMerchantDto>()
            .firstOrNull() ?: return null

        val outlet = assignment.outletId?.let { outletId ->
            supabaseClient.from("wallet_merchant_outlets")
                .select {
                    filter {
                        eq("id", outletId)
                        eq("status", "active")
                    }
                    limit(1)
                }
                .decodeList<WalletMerchantOutletDto>()
                .firstOrNull()
        }

        val balance = supabaseClient.from("wallet_merchant_balances")
            .select {
                filter { eq("merchant_id", assignment.merchantId) }
                limit(20)
            }
            .decodeList<WalletMerchantBalanceDto>()
            .firstOrNull { it.outletId == assignment.outletId }

        val device = supabaseClient.from("kantin_devices")
            .select {
                filter {
                    eq("kantin_user_id", userId)
                    eq("device_id", crypto.deviceId())
                }
                limit(1)
            }
            .decodeList<KantinDeviceDto>()
            .firstOrNull()

        return WalletMerchantContext(
            assignment = assignment,
            merchant = merchant,
            outlet = outlet,
            balance = balance,
            device = device
        )
    }

    override suspend fun getAccount(santriNis: String): WalletAccountDto? {
        supabaseClient.auth.awaitInitialization()
        return supabaseClient.from("dompet_santri")
            .select {
                filter { eq("santri_nis", santriNis) }
                limit(1)
            }
            .decodeList<WalletAccountDto>()
            .firstOrNull()
    }

    override suspend fun getTransactions(santriNis: String): List<WalletTransactionDto> {
        supabaseClient.auth.awaitInitialization()
        return supabaseClient.from("transaksi_dompet")
            .select(
                Columns.raw("id,public_id,santri_nis,direction,category,amount,balance_after,status,counterparty_role,keterangan,created_at")
            ) {
                filter { eq("santri_nis", santriNis) }
                order("created_at", Order.DESCENDING)
                limit(50)
            }
            .decodeList<WalletTransactionDto>()
    }

    override suspend fun registerWallet(santriNis: String, deviceName: String, pin: CharArray): WalletAccountDto {
        securityGuard.assertSensitiveWalletOperationAllowed()
        val publicKey = crypto.ensureWalletKey(walletAlias(santriNis))
        val pinMaterial = crypto.createPinVerifier(pin)
        callFunction<WalletRegisterRequest, WalletRegisterResponse>(
            "wallet-register",
            WalletRegisterRequest(
                santriNis = santriNis,
                deviceId = crypto.deviceId(),
                deviceName = deviceName,
                publicKey = publicKey.publicKeyBase64,
                studentPinSalt = pinMaterial.saltBase64,
                studentPinVerifier = pinMaterial.verifierBase64,
                studentPinKdf = WalletPinKdfDto(
                    memoryKib = pinMaterial.params.memoryKib,
                    iterations = pinMaterial.params.iterations,
                    parallelism = pinMaterial.params.parallelism,
                    hashLength = pinMaterial.params.hashLength
                ),
                keyAlgorithm = publicKey.keyAlgorithm
            )
        )
        return getAccount(santriNis)
            ?: throw WalletApiException(500, "Dompet berhasil dibuat, tetapi data akun belum bisa dimuat. Muat ulang halaman.")
    }

    override suspend fun updateLimits(request: WalletLimitUpdateRequest): WalletAccountDto {
        securityGuard.assertSensitiveWalletOperationAllowed()
        callFunction<WalletLimitUpdateRequest, WalletLimitUpdateEnvelope>("wallet-update-limits", request)
        return getAccount(request.santriNis)
            ?: throw WalletApiException(404, "Limit tersimpan, tetapi data dompet belum ditemukan. Muat ulang halaman.")
    }

    override suspend fun createTopUp(santriNis: String, amount: Long, paymentMethod: CorePaymentMethod): WalletTopUpDto {
        securityGuard.assertSensitiveWalletOperationAllowed()
        val response: WalletTopUpCreateResponse = callFunction(
            "wallet-topup-create",
            WalletTopUpCreateRequest(
                santriNis = santriNis,
                amount = amount,
                paymentMethod = paymentMethod.code,
                idempotencyKey = "wallet-topup:${santriNis}:${java.util.UUID.randomUUID()}"
            )
        )
        return response.data
    }

    override suspend fun confirmStudentPin(session: KantinAuthorizationDto, pin: CharArray) {
        securityGuard.assertSensitiveWalletOperationAllowed()
        val pinKdf = session.pinKdf
            ?: throw WalletApiException(400, "Parameter PIN belum tersedia. Scan ulang kartu santri.")
        val santriNis = session.santriNis
            ?: throw WalletApiException(400, "Data santri belum tersedia.")
        val message = studentPinMessage(session, santriNis, crypto.deviceId())
        val proof = crypto.createStudentPinProof(
            pin = pin,
            saltBase64 = pinKdf.salt,
            params = pinKdf.toParams(),
            message = message
        )
        callFunction<WalletStudentPinConfirmRequest, EmptyWalletResponse>(
            "wallet-kantin-student-confirm",
            WalletStudentPinConfirmRequest(
                authorizationSessionId = session.authorizationSessionId,
                deviceId = crypto.deviceId(),
                pinProof = proof,
                idempotencyKey = "wallet-kantin-student-confirm:${session.authorizationSessionId}"
            )
        )
    }

    override suspend fun createKantinAuthorization(qrPayload: String, amount: Long): KantinAuthorizationDto {
        securityGuard.assertSensitiveWalletOperationAllowed()
        val merchantContext = getKantinMerchantContext()
            ?: throw WalletApiException(403, "Akun kantin belum siap. Minta admin membuka Manajemen Kantin dan menekan Siapkan Otomatis.")
        return runCatching {
            createKantinAuthorizationSigned(qrPayload, amount, merchantContext)
        }.recoverCatching { error ->
            if (error is WalletApiException && error.isKantinSignatureInvalid()) {
                val device = registerKantinDevice()
                if (device.status != "active") {
                    throw WalletApiException(403, "Kunci perangkat diperbarui. Minta admin mengaktifkan ulang perangkat kantin.")
                }
                createKantinAuthorizationSigned(qrPayload, amount, merchantContext)
            } else {
                throw error
            }
        }.getOrThrow()
    }

    override suspend fun lookupKantinCard(qrPayload: String): KantinCardLookupDto {
        securityGuard.assertSensitiveWalletOperationAllowed()
        val walletPublicId = extractWalletPublicId(qrPayload)
        val nonce = crypto.randomNonce()
        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: throw WalletApiException(401, "Sesi kantin tidak valid. Silakan login ulang.")
        val signature = crypto.signKantinCardLookupMessage(
            alias = KANTIN_KEY_ALIAS,
            walletPublicId = walletPublicId,
            kantinUserId = userId,
            nonce = nonce
        )
        val response: KantinCardLookupResponse = callFunction(
            "wallet-kantin-card-lookup",
            KantinCardLookupRequest(
                qrPayload = qrPayload,
                deviceId = crypto.deviceId(),
                deviceSignature = signature,
                deviceNonce = nonce
            )
        )
        return response.data
    }

    private suspend fun createKantinAuthorizationSigned(
        qrPayload: String,
        amount: Long,
        merchantContext: WalletMerchantContext
    ): KantinAuthorizationDto {
        val walletPublicId = extractWalletPublicId(qrPayload)
        val idempotencyKey = "kantin:${crypto.deviceId()}:${java.util.UUID.randomUUID()}"
        val nonce = crypto.randomNonce()
        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: throw WalletApiException(401, "Sesi kantin tidak valid. Silakan login ulang.")
        val signature = crypto.signKantinAuthorizeMessage(
            alias = KANTIN_KEY_ALIAS,
            walletPublicId = walletPublicId,
            amount = amount,
            kantinUserId = userId,
            idempotencyKey = idempotencyKey,
            media = "qr",
            merchantId = merchantContext.assignment.merchantId,
            outletId = merchantContext.assignment.outletId,
            nonce = nonce
        )
        val response: KantinAuthorizeResponse = callFunction(
            "wallet-kantin-authorize",
            KantinAuthorizeRequest(
                qrPayload = qrPayload,
                amount = amount,
                merchantId = merchantContext.assignment.merchantId,
                outletId = merchantContext.assignment.outletId,
                idempotencyKey = idempotencyKey,
                deviceId = crypto.deviceId(),
                deviceSignature = signature,
                deviceNonce = nonce
            )
        )
        return response.data
    }

    override suspend fun confirmParentApproval(session: KantinAuthorizationDto) {
        securityGuard.assertSensitiveWalletOperationAllowed()
        val santriNis = session.santriNis
            ?: throw WalletApiException(400, "Data santri belum tersedia untuk approval.")
        val message = listOf(
            "DOMPET_SANTRI_KANTIN_V1",
            session.authorizationSessionId,
            session.paymentIntentId,
            santriNis,
            session.amount.toString(),
            session.challenge,
            session.nonce.orEmpty(),
            session.expiresAt
        ).joinToString("\n")
        callFunction<WalletConfirmRequest, EmptyWalletResponse>(
            "wallet-kantin-confirm",
            WalletConfirmRequest(
                authorizationSessionId = session.authorizationSessionId,
                deviceId = crypto.deviceId(),
                signature = crypto.signWalletMessage(walletAlias(santriNis), message)
            )
        )
    }

    override suspend fun getKantinHistory(): List<WalletTransactionDto> {
        supabaseClient.auth.awaitInitialization()
        return supabaseClient.from("view_kantin_transaction_history")
            .select {
                order("created_at", Order.DESCENDING)
                limit(50)
            }
            .decodeList<WalletTransactionDto>()
    }

    override suspend fun requestMerchantSettlement(
        merchantId: String,
        outletId: String?,
        amount: Long,
        note: String?
    ): WalletSettlementRequestDto {
        securityGuard.assertSensitiveWalletOperationAllowed()
        val response: WalletSettlementRequestResponse = callFunction(
            "wallet-merchant-settlement-request",
            WalletSettlementRequestCreate(
                merchantId = merchantId,
                outletId = outletId,
                amount = amount,
                destinationNote = note
            )
        )
        return response.data
    }

    override suspend fun createDispute(ledgerId: Long, santriNis: String, reason: String): WalletDisputeDto {
        securityGuard.assertSensitiveWalletOperationAllowed()
        val response: WalletDisputeResponse = callFunction(
            "wallet-dispute-create",
            WalletDisputeCreate(
                ledgerId = ledgerId,
                santriNis = santriNis,
                reason = reason
            )
        )
        return response.data
    }

    private suspend inline fun <reified Req : Any, reified Res : Any> callFunction(
        name: String,
        request: Req
    ): Res {
        supabaseClient.auth.awaitInitialization()
        val token = supabaseClient.auth.currentAccessTokenOrNull()
            ?: throw WalletApiException(401, "Sesi login tidak valid. Silakan login ulang.")

        val response = client.post("${BuildConfig.SUPABASE_URL}/functions/v1/$name") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setBody(request)
        }
        return decodeOrThrow(response)
    }

    private suspend inline fun <reified T> decodeOrThrow(response: HttpResponse): T {
        if (response.status.value in 200..299) return response.body()
        val body = response.bodyAsText()
        val message = runCatching { json.decodeFromString<WalletApiError>(body).error }.getOrNull()
        throw WalletApiException(response.status.value, message ?: "Permintaan dompet gagal.")
    }

    private fun walletAlias(santriNis: String): String =
        "wallet-wali-${santriNis.hashCode()}"

    private fun extractWalletPublicId(payload: String): String =
        runCatching {
            val obj = json.parseToJsonElement(payload).jsonObject
            obj["wallet_public_id"]?.jsonPrimitive?.content.orEmpty()
        }.getOrDefault(payload).ifBlank { payload }

    companion object {
        private const val KANTIN_KEY_ALIAS = "wallet-kantin-device"
    }
}

private fun studentPinMessage(session: KantinAuthorizationDto, santriNis: String, deviceId: String): String =
    listOf(
        "DOMPET_SANTRI_STUDENT_PIN_V1",
        session.authorizationSessionId,
        session.paymentIntentId,
        santriNis,
        session.amount.toString(),
        session.challenge,
        session.nonce.orEmpty(),
        session.expiresAt,
        deviceId
    ).joinToString("\n")

private fun WalletPinChallengeKdfDto.toParams(): WalletPinKdfParams =
    WalletPinKdfParams(
        memoryKib = memoryKib,
        iterations = iterations,
        parallelism = parallelism,
        hashLength = hashLength
    )

data class KantinDeviceLocalInfo(
    val deviceId: String,
    val deviceFingerprint: String,
    val publicKey: String
)

class WalletApiException(
    val statusCode: Int,
    override val message: String
) : Exception(message)

private fun WalletApiException.isKantinSignatureInvalid(): Boolean =
    statusCode == 403 && message.contains("signature perangkat kantin tidak valid", ignoreCase = true)

@Serializable
private data class WalletLimitUpdateEnvelope(
    val data: kotlinx.serialization.json.JsonObject
)

@Serializable
private data class EmptyWalletResponse(
    val data: kotlinx.serialization.json.JsonElement? = null
)
