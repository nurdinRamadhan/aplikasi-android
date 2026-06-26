package com.alhasanah.alhasanahmedia.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerDetails(
    @SerialName("first_name")
    val firstName: String,
    val email: String,
    val phone: String
)

@Serializable
data class ItemDetail(
    val id: String? = null,
    val name: String,
    val price: Long,
    val quantity: Int = 1
)

@Serializable
data class CorePaymentRequest(
    @SerialName("transaction_type")
    val transactionType: String,
    @SerialName("payment_method")
    val paymentMethod: String,
    @SerialName("order_id")
    val orderId: String? = null,
    @SerialName("gross_amount")
    val grossAmount: Long,
    @SerialName("customer_details")
    val customerDetails: CustomerDetails,
    @SerialName("item_details")
    val itemDetails: List<ItemDetail> = emptyList(),
    @SerialName("santri_nis")
    val santriNis: String? = null,
    @SerialName("notes")
    val notes: String? = null
)

@Serializable
data class CorePaymentResponse(
    val data: CorePaymentData? = null,
    val error: String? = null,
    val details: MidtransErrorDetails? = null,
    @SerialName("error_messages")
    val errorMessages: List<String>? = null
)

@Serializable
data class CorePaymentData(
    @SerialName("order_id")
    val orderId: String,
    @SerialName("transaction_id")
    val transactionId: String? = null,
    @SerialName("payment_type")
    val paymentType: String,
    @SerialName("method_code")
    val methodCode: String,
    @SerialName("method_label")
    val methodLabel: String,
    val amount: Long,
    val status: String = "pending",
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

enum class CorePaymentMethod(val code: String, val label: String, val helper: String) {
    QRIS("qris", "QRIS", "GoPay, OVO, DANA, LinkAja, dan mobile banking"),
    GOPAY("gopay", "GoPay", "Buka aplikasi GoPay/Gojek atau bayar dengan QR"),
    BCA_VA("bca_va", "BCA Virtual Account", "Transfer dari BCA atau bank lain"),
    BNI_VA("bni_va", "BNI Virtual Account", "Transfer dari BNI atau bank lain"),
    BRI_VA("bri_va", "BRI Virtual Account", "Transfer dari BRI atau bank lain"),
    PERMATA_VA("permata_va", "Permata Virtual Account", "Transfer ke nomor VA Permata"),
    MANDIRI_BILL("mandiri_bill", "Mandiri Bill Payment", "Bayar lewat Mandiri Bill Payment"),
    ALFAMART("alfamart", "Alfamart", "Bayar di Alfamart, Alfamidi, atau DAN+DAN"),
    INDOMARET("indomaret", "Indomaret", "Bayar di kasir Indomaret dengan kode pembayaran");

    companion object {
        fun fromCode(code: String?): CorePaymentMethod =
            entries.firstOrNull { it.code == code } ?: QRIS
    }
}
