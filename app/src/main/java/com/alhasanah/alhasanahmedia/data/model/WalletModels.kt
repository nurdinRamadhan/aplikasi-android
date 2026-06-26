package com.alhasanah.alhasanahmedia.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class WalletAccountDto(
    @SerialName("santri_nis")
    val santriNis: String,
    @SerialName("wallet_public_id")
    val walletPublicId: String,
    val saldo: Long,
    val status: String,
    @SerialName("low_balance_threshold")
    val lowBalanceThreshold: Long = 10_000,
    @SerialName("daily_spend_limit")
    val dailySpendLimit: Long? = null,
    @SerialName("single_transaction_limit")
    val singleTransactionLimit: Long? = null,
    @SerialName("monthly_spend_limit")
    val monthlySpendLimit: Long? = null,
    @SerialName("large_transaction_threshold")
    val largeTransactionThreshold: Long? = null,
    @SerialName("low_balance_warning_threshold")
    val lowBalanceWarningThreshold: Long = 30_000,
    @SerialName("low_balance_critical_threshold")
    val lowBalanceCriticalThreshold: Long = 10_000,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class WalletTransactionDto(
    val id: Long,
    @SerialName("public_id")
    val publicId: String,
    @SerialName("santri_nis")
    val santriNis: String,
    val direction: String,
    val category: String,
    val amount: Long,
    @SerialName("balance_after")
    val balanceAfter: Long,
    val status: String,
    @SerialName("counterparty_role")
    val counterpartyRole: String? = null,
    val keterangan: String? = null,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class WalletRegisterRequest(
    @SerialName("santri_nis")
    val santriNis: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("device_name")
    val deviceName: String,
    @SerialName("public_key")
    val publicKey: String,
    @SerialName("student_pin_salt")
    val studentPinSalt: String,
    @SerialName("student_pin_verifier")
    val studentPinVerifier: String,
    @SerialName("student_pin_kdf")
    val studentPinKdf: WalletPinKdfDto,
    @SerialName("key_algorithm")
    val keyAlgorithm: String = "Ed25519"
)

@Serializable
data class WalletPinKdfDto(
    val algorithm: String = "Argon2id",
    @SerialName("memory_kib")
    val memoryKib: Int = 19_456,
    val iterations: Int = 3,
    val parallelism: Int = 1,
    @SerialName("hash_length")
    val hashLength: Int = 32
)

@Serializable
data class WalletRegisterResponse(
    val data: WalletRegisterData
)

@Serializable
data class WalletRegisterData(
    val wallet: WalletAccountDto,
    val santri: WalletSantriDto,
    val device: WalletDeviceStatusDto
)

@Serializable
data class WalletSantriDto(
    val nis: String,
    val nama: String? = null,
    val kelas: String? = null,
    val jurusan: String? = null,
    @SerialName("status_santri")
    val statusSantri: String? = null
)

@Serializable
data class WalletDeviceStatusDto(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("key_algorithm")
    val keyAlgorithm: String,
    val status: String
)

@Serializable
data class WalletLimitUpdateRequest(
    @SerialName("santri_nis")
    val santriNis: String,
    @SerialName("low_balance_threshold")
    val lowBalanceThreshold: Long?,
    @SerialName("single_transaction_limit")
    val singleTransactionLimit: Long?,
    @SerialName("daily_spend_limit")
    val dailySpendLimit: Long?,
    @SerialName("monthly_spend_limit")
    val monthlySpendLimit: Long?,
    @SerialName("large_transaction_threshold")
    val largeTransactionThreshold: Long? = 75_000,
    @SerialName("allowed_merchant_categories")
    val allowedMerchantCategories: List<String> = listOf("kantin"),
    @SerialName("spending_schedule")
    val spendingSchedule: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class WalletTopUpCreateRequest(
    @SerialName("santri_nis")
    val santriNis: String,
    val amount: Long,
    @SerialName("payment_method")
    val paymentMethod: String,
    @SerialName("idempotency_key")
    val idempotencyKey: String
)

@Serializable
data class WalletTopUpCreateResponse(
    val data: WalletTopUpDto
)

@Serializable
data class WalletTopUpDto(
    @SerialName("payment_intent_id")
    val paymentIntentId: String,
    @SerialName("order_id")
    val orderId: String? = null,
    @SerialName("snap_token")
    val snapToken: String? = null,
    @SerialName("transaction_id")
    val transactionId: String? = null,
    @SerialName("payment_type")
    val paymentType: String? = null,
    @SerialName("method_code")
    val methodCode: String? = null,
    @SerialName("method_label")
    val methodLabel: String? = null,
    val amount: Long,
    val status: String,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    @SerialName("qr_url")
    val qrUrl: String? = null,
    @SerialName("deeplink_url")
    val deeplinkUrl: String? = null,
    @SerialName("va_number")
    val vaNumber: String? = null,
    val bank: String? = null,
    @SerialName("biller_code")
    val billerCode: String? = null,
    @SerialName("bill_key")
    val billKey: String? = null,
    @SerialName("permata_va_number")
    val permataVaNumber: String? = null,
    @SerialName("payment_code")
    val paymentCode: String? = null,
    val store: String? = null
)

@Serializable
data class KantinAuthorizeRequest(
    val media: String = "qr",
    @SerialName("qr_payload")
    val qrPayload: String,
    val amount: Long,
    @SerialName("merchant_id")
    val merchantId: String? = null,
    @SerialName("outlet_id")
    val outletId: String? = null,
    @SerialName("idempotency_key")
    val idempotencyKey: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("device_signature")
    val deviceSignature: String,
    @SerialName("device_nonce")
    val deviceNonce: String,
    @SerialName("signature_encoding")
    val signatureEncoding: String = "base64",
    @SerialName("public_key_encoding")
    val publicKeyEncoding: String = "base64"
)

@Serializable
data class KantinAuthorizeResponse(
    val data: KantinAuthorizationDto
)

@Serializable
data class KantinCardLookupRequest(
    @SerialName("qr_payload")
    val qrPayload: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("device_signature")
    val deviceSignature: String,
    @SerialName("device_nonce")
    val deviceNonce: String,
    @SerialName("signature_encoding")
    val signatureEncoding: String = "base64",
    @SerialName("public_key_encoding")
    val publicKeyEncoding: String = "base64"
)

@Serializable
data class KantinCardLookupResponse(
    val data: KantinCardLookupDto
)

@Serializable
data class KantinCardLookupDto(
    @SerialName("wallet_public_id")
    val walletPublicId: String,
    @SerialName("student_name")
    val studentName: String,
    @SerialName("student_class")
    val studentClass: String? = null,
    @SerialName("student_major")
    val studentMajor: String? = null,
    @SerialName("wallet_status")
    val walletStatus: String,
    @SerialName("student_status")
    val studentStatus: String? = null
)

@Serializable
data class KantinAuthorizationDto(
    val status: String,
    @SerialName("authorization_session_id")
    val authorizationSessionId: String,
    @SerialName("payment_intent_id")
    val paymentIntentId: String,
    @SerialName("santri_nis")
    val santriNis: String? = null,
    val amount: Long,
    val challenge: String,
    val nonce: String? = null,
    @SerialName("expires_at")
    val expiresAt: String,
    @SerialName("authorization_mode")
    val authorizationMode: String? = null,
    @SerialName("pin_kdf")
    val pinKdf: WalletPinChallengeKdfDto? = null
)

@Serializable
data class WalletPinChallengeKdfDto(
    val salt: String,
    val version: Int = 1,
    val algorithm: String = "Argon2id",
    @SerialName("memory_kib")
    val memoryKib: Int = 19_456,
    val iterations: Int = 3,
    val parallelism: Int = 1,
    @SerialName("hash_length")
    val hashLength: Int = 32
)

@Serializable
data class WalletConfirmRequest(
    @SerialName("authorization_session_id")
    val authorizationSessionId: String,
    @SerialName("device_id")
    val deviceId: String,
    val signature: String,
    @SerialName("signature_encoding")
    val signatureEncoding: String = "base64",
    @SerialName("public_key_encoding")
    val publicKeyEncoding: String = "base64"
)

@Serializable
data class WalletStudentPinConfirmRequest(
    @SerialName("authorization_session_id")
    val authorizationSessionId: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("pin_proof")
    val pinProof: String,
    @SerialName("idempotency_key")
    val idempotencyKey: String
)

@Serializable
data class KantinDeviceRegisterRequest(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("device_fingerprint")
    val deviceFingerprint: String,
    @SerialName("public_key")
    val publicKey: String
)

@Serializable
data class KantinDeviceRegisterResponse(
    val data: KantinDeviceDto
)

@Serializable
data class KantinDeviceDto(
    @SerialName("device_id")
    val deviceId: String,
    val status: String,
    @SerialName("registered_at")
    val registeredAt: String? = null,
    @SerialName("approved_at")
    val approvedAt: String? = null
)

@Serializable
data class WalletMerchantUserDto(
    val id: String,
    @SerialName("merchant_id")
    val merchantId: String,
    @SerialName("profile_id")
    val profileId: String,
    @SerialName("outlet_id")
    val outletId: String? = null,
    @SerialName("merchant_role")
    val merchantRole: String,
    val status: String
)

@Serializable
data class WalletMerchantDto(
    val id: String,
    val name: String,
    @SerialName("ownership_model")
    val ownershipModel: String,
    @SerialName("settlement_mode")
    val settlementMode: String,
    val status: String
)

@Serializable
data class WalletMerchantOutletDto(
    val id: String,
    @SerialName("merchant_id")
    val merchantId: String,
    val name: String,
    val location: String? = null,
    val status: String
)

@Serializable
data class WalletMerchantBalanceDto(
    @SerialName("merchant_id")
    val merchantId: String,
    @SerialName("outlet_id")
    val outletId: String? = null,
    @SerialName("saldo_available")
    val saldoAvailable: Long = 0,
    @SerialName("saldo_pending_settlement")
    val saldoPendingSettlement: Long = 0,
    @SerialName("total_sales")
    val totalSales: Long = 0,
    @SerialName("total_settled")
    val totalSettled: Long = 0,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class WalletMerchantContext(
    val assignment: WalletMerchantUserDto,
    val merchant: WalletMerchantDto,
    val outlet: WalletMerchantOutletDto? = null,
    val balance: WalletMerchantBalanceDto? = null,
    val device: KantinDeviceDto? = null
)

@Serializable
data class WalletSettlementRequestDto(
    val id: String,
    @SerialName("merchant_id")
    val merchantId: String,
    @SerialName("outlet_id")
    val outletId: String? = null,
    val amount: Long,
    val status: String,
    @SerialName("destination_note")
    val destinationNote: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class WalletSettlementRequestCreate(
    @SerialName("merchant_id")
    val merchantId: String,
    @SerialName("outlet_id")
    val outletId: String? = null,
    val amount: Long,
    @SerialName("destination_note")
    val destinationNote: String? = null
)

@Serializable
data class WalletSettlementRequestResponse(
    val data: WalletSettlementRequestDto
)

@Serializable
data class WalletDisputeDto(
    val id: String,
    @SerialName("ledger_id")
    val ledgerId: Long,
    @SerialName("santri_nis")
    val santriNis: String,
    val status: String,
    val reason: String,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class WalletDisputeCreate(
    @SerialName("ledger_id")
    val ledgerId: Long,
    @SerialName("santri_nis")
    val santriNis: String,
    val reason: String,
    val evidence: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class WalletDisputeResponse(
    val data: WalletDisputeDto
)

@Serializable
data class WalletApiError(
    val error: String? = null
)
