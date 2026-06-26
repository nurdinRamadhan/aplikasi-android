Analisis Masalah
Error Unresolved reference 'isSkipOrderSummary' terjadi karena property tersebut tidak ada di versi Midtrans UiKit SDK yang kamu gunakan. Property itu sudah dihapus atau tidak pernah ada di public API versi terbaru.

Fix 1 — AlhasanahMediaApplication.kt
Hapus baris isSkipOrderSummary, sudah tidak diperlukan:
kotlinprivate fun initMidtransSdk() {
    val colorTheme = com.midtrans.sdk.uikit.api.model.CustomColorTheme(
        "#D4A017",
        "#AA7C1F",
        "#E8C55A"
    )

    UiKitApi.Builder()
        .withContext(this)
        .withMerchantUrl(MidtransConfig.PAYMENT_BASE_URL)
        .withMerchantClientKey(MidtransConfig.CLIENT_KEY)
        .withColorTheme(colorTheme)
        .enableLog(true)
        .build()

    // HAPUS baris isSkipOrderSummary — property tidak ada di SDK ini
}

Fix 2 — Custom Payment Result Screen
Midtrans UiKit v2 mengembalikan hasil lewat ActivityResult. Kamu perlu:
PaymentResultActivity.kt
kotlinpackage com.alhasanah.alhasanahmedia.ui.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alhasanah.alhasanahmedia.databinding.ActivityPaymentResultBinding

class PaymentResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentResultBinding

    companion object {
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_ORDER_ID = "extra_order_id"
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
        const val EXTRA_MESSAGE = "extra_message"

        const val STATUS_SUCCESS = "success"
        const val STATUS_PENDING = "pending"
        const val STATUS_FAILED = "failed"

        fun createIntent(
            context: Context,
            status: String,
            orderId: String,
            transactionId: String = "",
            message: String = ""
        ) = Intent(context, PaymentResultActivity::class.java).apply {
            putExtra(EXTRA_STATUS, status)
            putExtra(EXTRA_ORDER_ID, orderId)
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            putExtra(EXTRA_MESSAGE, message)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val status        = intent.getStringExtra(EXTRA_STATUS) ?: STATUS_FAILED
        val orderId       = intent.getStringExtra(EXTRA_ORDER_ID) ?: "-"
        val transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID) ?: "-"
        val message       = intent.getStringExtra(EXTRA_MESSAGE) ?: ""

        renderResult(status, orderId, transactionId, message)
    }

    private fun renderResult(
        status: String,
        orderId: String,
        transactionId: String,
        message: String
    ) = with(binding) {
        when (status) {
            STATUS_SUCCESS -> {
                ivStatus.setImageResource(R.drawable.ic_payment_success)
                tvTitle.text = "Pembayaran Berhasil!"
                tvSubtitle.text = "Terima kasih, transaksi kamu telah dikonfirmasi."
                tvTitle.setTextColor(getColor(R.color.green_500))
                btnAction.text = "Lihat Pesanan"
            }
            STATUS_PENDING -> {
                ivStatus.setImageResource(R.drawable.ic_payment_pending)
                tvTitle.text = "Menunggu Pembayaran"
                tvSubtitle.text = message.ifEmpty { "Selesaikan pembayaran sebelum batas waktu." }
                tvTitle.setTextColor(getColor(R.color.yellow_600))
                btnAction.text = "Cek Status"
            }
            else -> {
                ivStatus.setImageResource(R.drawable.ic_payment_failed)
                tvTitle.text = "Pembayaran Gagal"
                tvSubtitle.text = message.ifEmpty { "Terjadi kesalahan. Silakan coba lagi." }
                tvTitle.setTextColor(getColor(R.color.red_500))
                btnAction.text = "Coba Lagi"
            }
        }

        tvOrderId.text = "Order ID: $orderId"
        tvTransactionId.text = if (transactionId != "-") "TXN: $transactionId" else ""

        btnAction.setOnClickListener {
            // Navigasi sesuai status — sesuaikan dengan NavGraph kamu
            finish()
        }

        btnBackHome.setOnClickListener { finish() }
    }
}

Karena saya pakai Compose, tidak perlu Activity + XML terpisah. Cukup gunakan NavController + rememberLauncherForActivityResult.
PaymentResultScreen.kt
kotlinpackage com.alhasanah.alhasanahmedia.ui.payment

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PaymentStatus { SUCCESS, PENDING, FAILED }

data class PaymentResultData(
    val status: PaymentStatus,
    val orderId: String,
    val transactionId: String = "",
    val message: String = ""
)

@Composable
fun PaymentResultScreen(
    resultData: PaymentResultData,
    onActionClick: () -> Unit,   // tombol utama
    onBackHome: () -> Unit        // tombol kembali beranda
) {
    val config = when (resultData.status) {
        PaymentStatus.SUCCESS -> PaymentResultConfig(
            iconEmoji = "✅",
            iconBg = Color(0xFFE8F5E9),
            iconColor = Color(0xFF2E7D32),
            title = "Pembayaran Berhasil!",
            subtitle = "Terima kasih, transaksi kamu telah dikonfirmasi.",
            actionLabel = "Lihat Pesanan",
            actionColor = Color(0xFF2E7D32)
        )
        PaymentStatus.PENDING -> PaymentResultConfig(
            iconEmoji = "⏳",
            iconBg = Color(0xFFFFF8E1),
            iconColor = Color(0xFFF9A825),
            title = "Menunggu Pembayaran",
            subtitle = resultData.message.ifEmpty {
                "Selesaikan pembayaran sebelum batas waktu habis."
            },
            actionLabel = "Cek Status",
            actionColor = Color(0xFFF9A825)
        )
        PaymentStatus.FAILED -> PaymentResultConfig(
            iconEmoji = "❌",
            iconBg = Color(0xFFFFEBEE),
            iconColor = Color(0xFFC62828),
            title = "Pembayaran Gagal",
            subtitle = resultData.message.ifEmpty {
                "Terjadi kesalahan. Silakan coba lagi."
            },
            actionLabel = "Coba Lagi",
            actionColor = Color(0xFFC62828)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(config.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(text = config.iconEmoji, fontSize = 44.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Judul
        Text(
            text = config.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = config.iconColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subjudul
        Text(
            text = config.subtitle,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info Order
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(label = "Order ID", value = resultData.orderId)
                if (resultData.transactionId.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    InfoRow(label = "Transaction ID", value = resultData.transactionId)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Tombol Aksi Utama
        Button(
            onClick = onActionClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = config.actionColor)
        ) {
            Text(
                text = config.actionLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tombol Kembali
        OutlinedButton(
            onClick = onBackHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Kembali ke Beranda",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
    }
}

private data class PaymentResultConfig(
    val iconEmoji: String,
    val iconBg: Color,
    val iconColor: Color,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val actionColor: Color
)

Cara pakai di Fragment/Activity yang memanggil Midtrans
Di composable yang ada tombol "Bayar", setup launcher seperti ini:
kotlin@Composable
fun CheckoutScreen(
    navController: NavController,
    snapToken: String
) {
    val context = LocalContext.current

    // 1. Setup launcher untuk menangkap result dari Midtrans
    val paymentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val transactionResult = result.data
                ?.getParcelableExtra<TransactionResult>(UiKitConstants.KEY_TRANSACTION_RESULT)

            // 2. Tentukan status
            val status = when {
                transactionResult == null -> PaymentStatus.FAILED
                transactionResult.isTransactionCanceled -> PaymentStatus.FAILED
                transactionResult.transactionStatus in listOf("settlement", "capture") ->
                    PaymentStatus.SUCCESS
                transactionResult.transactionStatus == "pending" ->
                    PaymentStatus.PENDING
                else -> PaymentStatus.FAILED
            }

            // 3. Navigasi ke halaman result custom kamu
            navController.navigate(
                "payment_result" +
                "?status=${status.name}" +
                "&orderId=${transactionResult?.orderId ?: "-"}" +
                "&transactionId=${transactionResult?.transactionId ?: ""}" +
                "&message=${transactionResult?.statusMessage ?: ""}"
            )
        }
    }

    // UI Checkout kamu
    Button(onClick = {
        UiKitApi.getDefaultInstance().startPaymentUiFlow(
            activity = context as Activity,
            launcher = paymentLauncher,
            snapToken = snapToken
        )
    }) {
        Text("Bayar Sekarang")
    }
}

Setup di NavGraph
kotlin// Di NavHost kamu
composable(
    route = "payment_result?status={status}&orderId={orderId}&transactionId={transactionId}&message={message}",
    arguments = listOf(
        navArgument("status") { defaultValue = "FAILED" },
        navArgument("orderId") { defaultValue = "-" },
        navArgument("transactionId") { defaultValue = "" },
        navArgument("message") { defaultValue = "" }
    )
) { backStackEntry ->
    val status = PaymentStatus.valueOf(
        backStackEntry.arguments?.getString("status") ?: "FAILED"
    )
    PaymentResultScreen(
        resultData = PaymentResultData(
            status = status,
            orderId = backStackEntry.arguments?.getString("orderId") ?: "-",
            transactionId = backStackEntry.arguments?.getString("transactionId") ?: "",
            message = backStackEntry.arguments?.getString("message") ?: ""
        ),
        onActionClick = {
            when (status) {
                PaymentStatus.SUCCESS -> navController.navigate("order_list") {
                    popUpTo("home") { inclusive = false }
                }
                PaymentStatus.PENDING -> navController.navigate("order_detail")
                PaymentStatus.FAILED -> navController.popBackStack()
            }
        },
        onBackHome = {
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    )
}

Ringkasan Alur Compose
CheckoutScreen
    ↓ (user klik Bayar)
Midtrans SDK UI (milik Midtrans, fullscreen)
    ↓ (selesai bayar)
rememberLauncherForActivityResult menangkap result
    ↓ (navController.navigate)
PaymentResultScreen ← halaman custom Compose kamu