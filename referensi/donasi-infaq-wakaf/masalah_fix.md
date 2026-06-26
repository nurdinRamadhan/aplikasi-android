Kendala unresolved reference yang Anda alami terjadi karena properti orderId dan statusMessage memang tidak tersedia di dalam data class TransactionResult pada Midtrans SDK (UI Kit) versi 2.5.0.

Pada versi ini, class com.midtrans.sdk.uikit.api.model.TransactionResult hanya membawa beberapa properti dasar, yaitu:

    transactionId (ID Transaksi dari Midtrans)

    status (Status pembayaran seperti success, pending, failed, canceled)

    paymentType (Metode pembayaran yang digunakan)

Midtrans berasumsi bahwa aplikasi Anda sudah mengetahui orderId sejak awal (karena orderId di-generate oleh backend/aplikasi Anda sebelum memanggil Snap Token), sehingga mereka tidak mengembalikannya lagi di dalam callback hasil pembayaran.

Berikut adalah solusi dan cara memperbaiki penulisan kode di Jetpack Compose Anda:
1. Cara Menangani orderId

Anda harus mengambil orderId dari state aplikasi Anda (misalnya dari ViewModel atau variabel lokal tempat Anda menyimpan data pesanan sebelum membuka halaman Midtrans), bukan memanggilnya dari objek tx.
2. Cara Menangani statusMessage

Karena properti statusMessage tidak ada, Anda bisa menggunakan properti status bawaan dari Midtrans sebagai pesan, atau membuat mapping fungsi sendiri untuk mengubah status menjadi pesan yang lebih ramah pengguna.
Contoh Implementasi Kode yang Benar

Pastikan Anda sudah menyimpan orderId di state (misal: currentOrderId) sebelum memanggil blok navigasi ini:
Kotlin

// Asumsi: Anda mengambil orderId dari ViewModel atau State yang sudah ada sebelum payment
val currentOrderId = viewModel.orderId.value // Contoh pengambilan dari state

navController.navigate(
    Screen.PaymentResult.createRoute(
        // Gunakan properti tx?.status bawaan Midtrans
        status = tx?.status ?: "unknown", 
        
        // Gunakan orderId dari State aplikasi, BUKAN dari TransactionResult
        orderId = currentOrderId ?: "-", 
        
        // transactionId tersedia di TransactionResult, jadi ini sudah benar
        transactionId = tx?.transactionId ?: "", 
        
        // Gunakan tx?.status sebagai message, atau buat helper function
        message = getCustomMessage(tx?.status) 
    )
)

Opsional: Fungsi Mapping Pesan (Helper Function)
Jika Anda ingin pesannya lebih deskriptif daripada sekadar "success" atau "pending", Anda bisa membuat fungsi sederhana seperti ini:
Kotlin

fun getCustomMessage(status: String?): String {
    return when (status?.lowercase()) {
        "success" -> "Pembayaran berhasil diterima."
        "pending" -> "Menunggu pembayaran diselesaikan."
        "failed" -> "Pembayaran gagal."
        "canceled" -> "Pembayaran dibatalkan."
        else -> "Status pembayaran tidak diketahui."
    }
}

Dengan penyesuaian ini, error unresolved reference akan hilang karena Anda hanya memanggil properti yang benar-benar ada di Midtrans SDK versi 2.5.0 (tx?.transactionId dan tx?.status), dan mengambil sisanya dari state lokal aplikasi Anda.