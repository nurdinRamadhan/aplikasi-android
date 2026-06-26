package com.alhasanah.alhasanahmedia.ui.payment

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alhasanah.alhasanahmedia.ui.components.AppGradientBackground
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// ─────────────────────────────────────────────────────────────
// Data class — tidak diubah agar kompatibel dengan caller
// ─────────────────────────────────────────────────────────────
data class PaymentInstructionData(
    val orderId        : String,
    val transactionId  : String = "",
    val methodCode     : String,
    val methodLabel    : String,
    val amount         : Long,
    val expiresAt      : String = "",
    val qrUrl          : String = "",
    val deeplinkUrl    : String = "",
    val vaNumber       : String = "",
    val bank           : String = "",
    val billerCode     : String = "",
    val billKey        : String = "",
    val paymentCode    : String = "",
    val store          : String = "",
    val message        : String = ""
)

// ─────────────────────────────────────────────────────────────
// 🎨  BRAND TOKENS — sama persis dengan WalletScreens
// ─────────────────────────────────────────────────────────────
private val GoldPrimary  = Color(0xFFD4A017)
private val GoldLight    = Color(0xFFF0C040)
private val GoldDark     = Color(0xFF9A7A00)
private val GoldGlow     = Color(0x33D4A017)
private val DarkBg       = Color(0xFF0D0D18)
private val DarkSurface  = Color(0xFF14141F)
private val DarkCard     = Color(0xFF1A1A2E)
private val DarkCardBorder = Color(0xFF2A2A45)
private val CreditGreen  = Color(0xFF22C55E)
private val AmberAccent  = Color(0xFFF59E0B)
private val BlueAccent   = Color(0xFF3B82F6)
private val PurpleAccent = Color(0xFF8B5CF6)

private val GoldGradient = Brush.linearGradient(listOf(GoldDark, GoldPrimary, GoldLight))
private val DarkCardGradient = Brush.linearGradient(
    listOf(Color(0xFF1E1E35), Color(0xFF12121F))
)

private fun fmtRupiah(v: Long): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(v).replace(",00", "")

// ─────────────────────────────────────────────────────────────
// 🚀  MAIN SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun PaymentInstructionScreen(
    data          : PaymentInstructionData,
    onBack        : () -> Unit,
    onCheckStatus : () -> Unit,
    onBackHome    : () -> Unit
) {
    val isDark    = isSystemInDarkTheme()
    val snackbar  = remember { SnackbarHostState() }
    val scope     = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context   = LocalContext.current

    val isQr      = data.qrUrl.isNotBlank()
    val isRetail  = data.methodCode in listOf("alfamart", "indomaret")
    val isMandiri = data.methodCode == "mandiri_bill"

    val mainValue = data.vaNumber.ifBlank {
        when {
            isMandiri -> data.billKey
            data.paymentCode.isNotBlank() -> data.paymentCode
            else -> data.orderId
        }
    }

    val onCopy: (String) -> Unit = { value ->
        clipboard.setText(AnnotatedString(value))
        scope.launch { snackbar.showSnackbar("✓ Disalin ke clipboard") }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbar) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AppSolidBackground(isDark = isDark)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── TOP BAR ──────────────────────────────────
                AppPageHeader(
                    title = "INSTRUKSI PEMBAYARAN",
                    subtitle = data.methodLabel.ifBlank { "Selesaikan pembayaran" },
                    isDark = isDark,
                    onBack = onBack,
                    size = AppPageHeaderSize.Compact
                )

                // ── HEADER CARD ───────────────────────────────
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PiHeaderCard(data = data, isDark = isDark)

                    // ── QR OR VA CARD ─────────────────────────────
                    if (isQr) {
                        PiQrCard(
                            qrUrl       = data.qrUrl,
                            deeplinkUrl = data.deeplinkUrl,
                            isDark      = isDark,
                            onOpen      = {
                                val url = data.deeplinkUrl.ifBlank { data.qrUrl }
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }.onFailure {
                                    scope.launch { snackbar.showSnackbar("Aplikasi pembayaran belum bisa dibuka.") }
                                }
                            }
                        )
                    } else {
                        PiVaCard(
                            data       = data,
                            mainValue  = mainValue,
                            isDark     = isDark,
                            onCopy     = onCopy
                        )
                    }

                    // ── STEPS CARD ────────────────────────────────
                    PiStepsCard(methodCode = data.methodCode, isDark = isDark)

                    // ── COUNTDOWN (jika ada expiry) ───────────────
                    if (data.expiresAt.isNotBlank()) {
                        PiCountdownBanner(expiresAt = data.expiresAt, isDark = isDark)
                    }

                    // ── CTA BUTTONS ───────────────────────────────
                    // Check status — gold gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GoldGradient)
                            .clickable { onCheckStatus() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.Refresh, null,
                                tint = Color(0xFF151108), modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Cek Status Pembayaran",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color      = Color(0xFF151108)
                                )
                            )
                        }
                    }

                    // Back home — outlined
                    OutlinedButton(
                        onClick = onBackHome,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape   = RoundedCornerShape(14.dp),
                        border  = BorderStroke(1.dp, GoldPrimary.copy(if (isDark) 0.45f else 0.35f))
                    ) {
                        Text(
                            "Kembali ke Beranda",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 🔝  TOP BAR
// ─────────────────────────────────────────────────────────────

@Composable
private fun PiTopBar(title: String, onBack: () -> Unit, isDark: Boolean) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Gold accent line top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .background(GoldGradient)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, null, tint = GoldPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 💳  HEADER CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun PiHeaderCard(data: PaymentInstructionData, isDark: Boolean) {
    val isQr = data.qrUrl.isNotBlank()

    // Infinite glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "headerGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) DarkCardGradient else Brush.linearGradient(
                listOf(Color(0xFFFDF6DC), Color(0xFFFFFBF0))
            ))
            .border(
                1.dp,
                Brush.linearGradient(listOf(
                    GoldPrimary.copy(glowAlpha),
                    GoldLight.copy(glowAlpha * 0.45f),
                    GoldPrimary.copy(glowAlpha)
                )),
                RoundedCornerShape(22.dp)
            )
    ) {
        // Background glow orb (dark only)
        if (isDark) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-20).dp)
                    .background(
                        Brush.radialGradient(listOf(GoldGlow, Color.Transparent)),
                        CircleShape
                    )
            )
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Method label row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(GoldPrimary.copy(.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, GoldPrimary.copy(.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isQr) Icons.Outlined.QrCode2 else Icons.Outlined.AccountBalance,
                            null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        data.methodLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                // Status chip
                Surface(shape = CircleShape, color = AmberAccent.copy(.14f)) {
                    Text(
                        "MENUNGGU",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = AmberAccent,
                            letterSpacing = .8.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Gold separator
            Box(Modifier.fillMaxWidth().height(1.dp).background(GoldPrimary.copy(if (isDark) .18f else .12f)))

            // Amount — large
            Text(
                "TAGIHAN",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    color = if (isDark) GoldLight.copy(.6f) else GoldDark.copy(.7f),
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                fmtRupiah(data.amount),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 30.sp
                )
            )

            // Meta info lines
            PiInfoLine("Order ID", data.orderId, isDark)
            if (data.expiresAt.isNotBlank()) PiInfoLine("Batas Waktu", data.expiresAt, isDark)
            if (data.transactionId.isNotBlank()) PiInfoLine("Transaksi ID", data.transactionId, isDark)

            // Notice
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BlueAccent.copy(if (isDark) .12f else .08f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info, null,
                        tint = BlueAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        data.message.ifBlank {
                            "Selesaikan pembayaran sebelum batas waktu. Status diperbarui otomatis setelah webhook Midtrans diterima."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isDark) BlueAccent.copy(.85f) else BlueAccent
                        )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 📱  QR CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun PiQrCard(
    qrUrl      : String,
    deeplinkUrl: String,
    isDark     : Boolean,
    onOpen     : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) DarkCard else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isDark) DarkCardBorder else GoldPrimary.copy(.15f),
                RoundedCornerShape(22.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(GoldPrimary.copy(.14f), RoundedCornerShape(10.dp))
                        .border(1.dp, GoldPrimary.copy(.28f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.QrCode2, null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Kode QR Pembayaran",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // QR image — always on white background
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(
                        2.dp,
                        Brush.linearGradient(listOf(GoldDark, GoldPrimary, GoldLight)),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = qrUrl,
                    contentDescription = "QR pembayaran",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                "Scan QR menggunakan aplikasi e-wallet\natau mobile banking Anda",
                style = MaterialTheme.typography.bodySmall.copy(
                    textAlign = TextAlign.Center,
                    color = if (isDark) Color.White.copy(.45f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // Open app button
            if (deeplinkUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GoldGradient)
                        .clickable { onOpen() }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.OpenInNew, null,
                            tint = Color(0xFF151108), modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Buka Aplikasi Pembayaran",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF151108)
                            )
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 🏦  VA / CODE CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun PiVaCard(
    data      : PaymentInstructionData,
    mainValue : String,
    isDark    : Boolean,
    onCopy    : (String) -> Unit
) {
    val vaLabel = when {
        data.methodCode == "mandiri_bill"    -> "Bill Key"
        data.paymentCode.isNotBlank()        -> "Kode Pembayaran"
        else                                  -> "Nomor Virtual Account"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) DarkCard else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isDark) DarkCardBorder else GoldPrimary.copy(.15f),
                RoundedCornerShape(22.dp)
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(GoldPrimary.copy(.14f), RoundedCornerShape(10.dp))
                        .border(1.dp, GoldPrimary.copy(.28f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AccountBalance, null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Detail Pembayaran",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(GoldPrimary.copy(if (isDark) .15f else .1f)))

            // Info lines
            if (data.bank.isNotBlank())  PiInfoLine("Bank",       data.bank.uppercase(),  isDark)
            if (data.store.isNotBlank()) PiInfoLine("Gerai",      data.store.uppercase(), isDark)

            // Biller code (if mandiri)
            if (data.billerCode.isNotBlank()) {
                PiCopyLine("Biller Code", data.billerCode, isDark, onCopy)
                Box(Modifier.fillMaxWidth().height(1.dp).background(GoldPrimary.copy(if (isDark) .1f else .08f)))
            }

            // Main value — hero copy field
            PiHeroCopyField(label = vaLabel, value = mainValue, isDark = isDark, onCopy = onCopy)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 📋  STEP-BY-STEP CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun PiStepsCard(methodCode: String, isDark: Boolean) {
    val steps = when (methodCode) {
        "mandiri_bill"            -> listOf(
            "Buka aplikasi Livin' by Mandiri atau ATM Mandiri",
            "Pilih menu Bayar / Beli → Multi Payment",
            "Masukkan Biller Code dan Bill Key",
            "Cek nama merchant dan nominal, lalu konfirmasi"
        )
        "alfamart", "indomaret"   -> listOf(
            "Datangi kasir gerai ${methodCode.replaceFirstChar { it.uppercase() }}",
            "Beritahu kasir bahwa Anda ingin bayar Midtrans",
            "Tunjukkan atau sebutkan kode pembayaran",
            "Cek nominal di struk, lalu simpan sebagai bukti"
        )
        "qris", "gopay",
        "shopeepay", "dana"       -> listOf(
            "Buka aplikasi e-wallet atau mobile banking Anda",
            "Pilih Scan QR atau buka dari tombol di atas",
            "Pastikan nama merchant dan nominal sesuai",
            "Konfirmasi pembayaran dengan PIN / biometrik"
        )
        else                      -> listOf(
            "Buka aplikasi mobile banking atau ATM",
            "Pilih menu Transfer / Virtual Account",
            "Masukkan nomor VA yang tertera di atas",
            "Cek nama penerima & nominal, lalu konfirmasi"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) DarkCard else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isDark) DarkCardBorder else GoldPrimary.copy(.13f),
                RoundedCornerShape(22.dp)
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(GoldPrimary.copy(.14f), RoundedCornerShape(10.dp))
                        .border(1.dp, GoldPrimary.copy(.28f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Payments, null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Langkah Pembayaran",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(GoldPrimary.copy(if (isDark) .15f else .1f)))

            steps.forEachIndexed { i, step ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Step number bubble
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(GoldGradient, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF151108)
                            )
                        )
                    }
                    Text(
                        step,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDark) Color.White.copy(.8f)
                                    else MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ⏱  COUNTDOWN BANNER
// ─────────────────────────────────────────────────────────────

@Composable
private fun PiCountdownBanner(expiresAt: String, isDark: Boolean) {
    // Pulsing amber animation
    val inf = rememberInfiniteTransition(label = "cd")
    val alpha by inf.animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "cdAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AmberAccent.copy(if (isDark) .12f else .09f))
            .border(1.dp, AmberAccent.copy(if (isDark) .38f else .28f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Outlined.Timer, null,
                tint = AmberAccent.copy(alpha),
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    "Batas Waktu Pembayaran",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent
                    )
                )
                Text(
                    expiresAt,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 🧩  REUSABLE ATOMS
// ─────────────────────────────────────────────────────────────

@Composable
private fun PiInfoLine(label: String, value: String, isDark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isDark) Color.White.copy(.4f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White.copy(.8f)
                        else MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun PiCopyLine(label: String, value: String, isDark: Boolean, onCopy: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDark) Color.White.copy(.4f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                )
            )
        }
        IconButton(onClick = { onCopy(value) }) {
            Icon(Icons.Outlined.ContentCopy, null, tint = GoldPrimary)
        }
    }
}

/** Hero copy field — large number, full-width copy button */
@Composable
private fun PiHeroCopyField(
    label  : String,
    value  : String,
    isDark : Boolean,
    onCopy : (String) -> Unit
) {
    var justCopied by remember { mutableStateOf(false) }
    val scope      = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) GoldGlow else GoldPrimary.copy(.07f))
            .border(1.dp, GoldPrimary.copy(if (isDark) .38f else .3f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight    = FontWeight.Black,
                    color         = GoldPrimary
                )
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = if (isDark) GoldLight else GoldDark
                ),
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            // Copy button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (justCopied) CreditGreen.copy(.15f)
                        else GoldPrimary.copy(.15f)
                    )
                    .border(
                        1.dp,
                        if (justCopied) CreditGreen.copy(.45f) else GoldPrimary.copy(.4f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        onCopy(value)
                        justCopied = true
                        scope.launch {
                            delay(1800)
                            justCopied = false
                        }
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = justCopied,
                        label = "copyIcon",
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                    ) { copied ->
                        Icon(
                            if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            null,
                            tint     = if (copied) CreditGreen else GoldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (justCopied) "Tersalin!" else "Salin Nomor",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (justCopied) CreditGreen else GoldPrimary
                        )
                    )
                }
            }
        }
    }
}
