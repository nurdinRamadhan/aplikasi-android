package com.alhasanah.alhasanahmedia.ui.wallet

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.CorePaymentMethod
import com.alhasanah.alhasanahmedia.data.model.WalletTransactionDto
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import com.alhasanah.alhasanahmedia.ui.payment.PaymentInstructionData
import com.alhasanah.alhasanahmedia.ui.payment.PaymentMethodPickerDialog
import com.alhasanah.alhasanahmedia.util.formatRupiah
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════
// 🎨  DESIGN TOKENS — Dark Fintech × Gold × White
// ═══════════════════════════════════════════════════════════════

/** Dark-mode deep backgrounds — sama seperti screenshot referensi */
private val DarkBg         = Color(0xFF0D0D18)
private val DarkSurface    = Color(0xFF14141F)
private val DarkCard       = Color(0xFF1A1A2E)
private val DarkCardBorder = Color(0xFF2A2A45)

/** Gold brand accent */
private val GoldPrimary = Color(0xFFD4A017)
private val GoldLight   = Color(0xFFF0C040)
private val GoldDark    = Color(0xFF9A7A00)
private val GoldGlow    = Color(0x33D4A017)

/** Semantic colors */
private val CreditGreen = Color(0xFF22C55E)
private val DebitRed    = Color(0xFFEF4444)
private val PurpleAccent= Color(0xFF8B5CF6)
private val BlueAccent  = Color(0xFF3B82F6)
private val AmberAccent = Color(0xFFF59E0B)

/** Gradient definitions */
private val GoldGradient = Brush.linearGradient(listOf(GoldDark, GoldPrimary, GoldLight))
private val DarkCardGradient = Brush.linearGradient(
    listOf(Color(0xFF1E1E35), Color(0xFF12121F))
)
private val PurpleGoldGradient = Brush.linearGradient(
    listOf(PurpleAccent.copy(0.7f), GoldPrimary.copy(0.9f))
)

// ═══════════════════════════════════════════════════════════════

private enum class WalletTxFilter(val label: String) {
    SEMUA("Semua"), MASUK("Masuk"), KELUAR("Keluar")
}

private enum class WalletPeriodFilter(val label: String, val shortLabel: String) {
    SEMUA("Semua Waktu", "Semua"),
    HARI_INI("Hari Ini", "Hari ini"),
    KEMARIN("Kemarin", "Kemarin"),
    MINGGU_INI("Minggu Ini", "Minggu ini"),
    MINGGU_LALU("Minggu Lalu", "Minggu lalu"),
    BULAN_INI("Bulan Ini", "Bulan ini"),
    BULAN_LALU("Bulan Lalu", "Bulan lalu"),
    TIGA_BULAN("3 Bulan Terakhir", "3 bulan");

    fun contains(date: LocalDate?, today: LocalDate): Boolean {
        if (this == SEMUA) return true
        if (date == null) return false

        val thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastWeekStart = thisWeekStart.minusWeeks(1)
        val thisMonth = YearMonth.from(today)

        return when (this) {
            SEMUA -> true
            HARI_INI -> date == today
            KEMARIN -> date == today.minusDays(1)
            MINGGU_INI -> !date.isBefore(thisWeekStart) && !date.isAfter(today)
            MINGGU_LALU -> !date.isBefore(lastWeekStart) && date.isBefore(thisWeekStart)
            BULAN_INI -> YearMonth.from(date) == thisMonth
            BULAN_LALU -> YearMonth.from(date) == thisMonth.minusMonths(1)
            TIGA_BULAN -> !date.isBefore(today.minusMonths(3)) && !date.isAfter(today)
        }
    }
}

private fun WalletTopUpLaunch.toInstructionData(): PaymentInstructionData =
    PaymentInstructionData(
        orderId = orderId,
        transactionId = transactionId,
        methodCode = methodCode,
        methodLabel = methodLabel,
        amount = amount,
        expiresAt = expiresAt,
        qrUrl = qrUrl,
        deeplinkUrl = deeplinkUrl,
        vaNumber = vaNumber,
        bank = bank,
        billerCode = billerCode,
        billKey = billKey,
        paymentCode = paymentCode,
        store = store,
        message = "Top up akan masuk ke saldo Dompet Santri setelah pembayaran dikonfirmasi Midtrans."
    )

private fun WalletTransactionDto.walletLocalDateOrNull(): LocalDate? {
    return try {
        OffsetDateTime.parse(createdAt).toLocalDate()
    } catch (_: DateTimeParseException) {
        runCatching { LocalDate.parse(createdAt.take(10)) }.getOrNull()
    }
}

// ═══════════════════════════════════════════════════════════════
// 🚀  WALI WALLET SCREEN — UTAMA
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletWaliScreen(
    santriNis: String,
    navController: NavController,
    viewModel: WalletWaliViewModel = koinViewModel { parametersOf(santriNis) }
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context  = LocalContext.current
    var showPinSetup    by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var activeFilter    by remember { mutableStateOf(WalletTxFilter.SEMUA) }
    var activePeriod    by remember { mutableStateOf(WalletPeriodFilter.SEMUA) }
    var topUpAmount     by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(CorePaymentMethod.QRIS) }
    var showTopUpPaymentMethodDialog by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme()

    SecureWalletWindow()

    LaunchedEffect(state.error, state.info) {
        state.error?.let { snackbar.showSnackbar(it) }
        state.info?.let  { snackbar.showSnackbar(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.launchTopUp.collect { topUp ->
            topUpAmount = ""
            navController.navigate(Screen.PaymentInstruction.createRoute(topUp.toInstructionData()))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Color.Transparent
    ) { padding ->
        val today = remember { LocalDate.now() }
        val filteredTransactions = remember(state.transactions, activeFilter, activePeriod, today) {
            state.transactions.filter { tx ->
                val directionMatch = when (activeFilter) {
                    WalletTxFilter.SEMUA  -> true
                    WalletTxFilter.MASUK  -> tx.direction == "credit"
                    WalletTxFilter.KELUAR -> tx.direction != "credit"
                }
                directionMatch && activePeriod.contains(tx.walletLocalDateOrNull(), today)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AppSolidBackground(isDark = isDark)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
            // ── Header ──
            item {
                FintechPageHeader(
                    title    = "Dompet Santri",
                    subtitle = "Saldo, limit & riwayat kantin",
                    onBack   = { navController.popBackStack() },
                    onRefresh= viewModel::refresh,
                    isDark   = isDark
                )
            }

            // ── Balance / Activation ──
            item {
                Spacer(Modifier.height(8.dp))
                if (state.account == null) {
                    WalletActivationCard(
                        loading  = state.loading,
                        onActivate = { showPinSetup = true },
                        isDark   = isDark
                    )
                } else {
                    PremiumBalanceCard(
                        account     = state.account!!,
                        isDark      = isDark,
                        onEditLimit = { showLimitDialog = true }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Quick Action Buttons ──
            state.account?.let { account ->
                item {
                    QuickActionRow(
                        account  = account,
                        isDark   = isDark,
                        onTopUp  = { /* scroll down or show sheet */ }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ── Spending Chart ──
                item {
                    SpendingMiniChart(
                        transactions = state.transactions,
                        isDark       = isDark
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ── Top Up Card ──
                item {
                    PremiumTopUpCard(
                        amount         = topUpAmount,
                        onAmountChange = { topUpAmount = it.filter(Char::isDigit) },
                        enabled        = !state.loading && account.status == "active",
                        onSubmit       = { showTopUpPaymentMethodDialog = true },
                        isDark         = isDark
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Filter + History Header ──
            item {
                WalletSectionLabel(
                    title  = "RIWAYAT TRANSAKSI",
                    count  = "${filteredTransactions.size} item",
                    isDark = isDark
                )
                Spacer(Modifier.height(10.dp))
                PremiumFilterRow(
                    activeFilter  = activeFilter,
                    onFilterChange= { activeFilter = it },
                    activePeriod  = activePeriod,
                    onPeriodChange= { activePeriod = it },
                    visibleCount   = filteredTransactions.size,
                    totalCount     = state.transactions.size,
                    isDark        = isDark
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── Transactions ──
            if (filteredTransactions.isEmpty()) {
                item { WalletEmptyState("Belum ada transaksi pada filter ini.", isDark) }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    PremiumTransactionRow(tx, isDark)
                }
            }
            }
        }
    }

    // ── Pin Setup Dialog ──
    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = { showPinSetup = false },
            onConfirm = { pin ->
                showPinSetup = false
                viewModel.registerWallet(
                    "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", pin
                )
            },
            isDark = isDark
        )
    }

    if (showTopUpPaymentMethodDialog) {
        PaymentMethodPickerDialog(
            selected = selectedPaymentMethod,
            onSelected = { selectedPaymentMethod = it },
            onDismiss = { showTopUpPaymentMethodDialog = false },
            onConfirm = {
                showTopUpPaymentMethodDialog = false
                viewModel.createTopUp(topUpAmount, selectedPaymentMethod)
            },
            title = "Pilih Pembayaran Top Up",
            confirmText = "Buat Instruksi Top Up",
            methods = listOf(
                CorePaymentMethod.QRIS,
                CorePaymentMethod.GOPAY,
                CorePaymentMethod.BCA_VA,
                CorePaymentMethod.BNI_VA,
                CorePaymentMethod.BRI_VA,
                CorePaymentMethod.PERMATA_VA,
                CorePaymentMethod.MANDIRI_BILL,
                CorePaymentMethod.ALFAMART,
                CorePaymentMethod.INDOMARET
            )
        )
    }

    // ── Premium Limit Bottom Sheet Dialog ──
    if (showLimitDialog && state.account != null) {
        PremiumLimitDialog(
            account   = state.account!!,
            onDismiss = { showLimitDialog = false },
            onSave    = { low, single, daily, monthly ->
                viewModel.updateLimits(low, single, daily, monthly)
                showLimitDialog = false
            },
            isDark    = isDark
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// 🎨  FINTECH PAGE HEADER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FintechPageHeader(
    title: String, subtitle: String,
    onBack: () -> Unit, onRefresh: (() -> Unit)? = null,
    isDark: Boolean
) {
    AppPageHeader(
        title = title.uppercase(),
        subtitle = subtitle,
        isDark = isDark,
        onBack = onBack,
        size = AppPageHeaderSize.Compact,
        titleTopPadding = 50.dp,
        rightAction = {
            if (onRefresh != null) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Muat ulang",
                        tint = if (isDark) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
// 💳  PREMIUM BALANCE CARD — Fintech Dark Card Style
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PremiumBalanceCard(
    account: com.alhasanah.alhasanahmedia.data.model.WalletAccountDto,
    isDark: Boolean,
    onEditLimit: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f, targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isDark) DarkCardGradient
                else Brush.linearGradient(listOf(Color(0xFFFDF6DC), Color(0xFFFFF9EE)))
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        GoldPrimary.copy(glowAlpha),
                        GoldLight.copy(glowAlpha * 0.5f),
                        GoldPrimary.copy(glowAlpha)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        // Background glow orb
        if (isDark) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-30).dp)
                    .background(
                        Brush.radialGradient(listOf(GoldGlow, Color.Transparent)),
                        CircleShape
                    )
            )
        }

        Column(Modifier.padding(22.dp)) {
            // ── Card Header Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "DIGITAL SANTRI WALLET",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.8.sp,
                            color = GoldPrimary
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Saldo Tersedia",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDark) Color.White.copy(0.55f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatRupiah(account.saldo),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 32.sp
                        )
                    )
                }
                PremiumStatusChip(account.status, isDark)
            }

            Spacer(Modifier.height(20.dp))

            // ── Divider ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        if (isDark) GoldPrimary.copy(0.18f)
                        else GoldPrimary.copy(0.15f)
                    )
            )

            Spacer(Modifier.height(18.dp))

            // ── Limit Metrics Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LimitMetricItem(
                    label = "SEKALI",
                    value = formatRupiah(account.singleTransactionLimit),
                    icon  = Icons.Outlined.CreditCard,
                    isDark= isDark
                )
                Box(Modifier.width(1.dp).height(44.dp).background(
                    if (isDark) GoldPrimary.copy(0.18f) else GoldPrimary.copy(0.12f)
                ))
                LimitMetricItem(
                    label = "HARIAN",
                    value = formatRupiah(account.dailySpendLimit),
                    icon  = Icons.Outlined.History,
                    isDark= isDark
                )
                Box(Modifier.width(1.dp).height(44.dp).background(
                    if (isDark) GoldPrimary.copy(0.18f) else GoldPrimary.copy(0.12f)
                ))
                LimitMetricItem(
                    label = "BULANAN",
                    value = formatRupiah(account.monthlySpendLimit),
                    icon  = Icons.Outlined.PendingActions,
                    isDark= isDark
                )
            }

            Spacer(Modifier.height(18.dp))

            // ── Action Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Edit Limit Button — premium pill
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEditLimit() },
                    shape     = RoundedCornerShape(12.dp),
                    color     = Color.Transparent,
                    border    = BorderStroke(
                        1.dp,
                        GoldPrimary.copy(if (isDark) 0.45f else 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = null,
                            tint   = GoldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Atur Limit",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color      = GoldPrimary
                            )
                        )
                    }
                }

                // Security info pill
                Surface(
                    modifier = Modifier.weight(1.4f),
                    shape    = RoundedCornerShape(12.dp),
                    color    = if (isDark) GoldGlow else GoldPrimary.copy(0.07f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.Security,
                            contentDescription = null,
                            tint   = GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "PIN terenkripsi backend",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) GoldLight else GoldDark,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 🔢  LIMIT METRIC ITEM
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LimitMetricItem(label: String, value: String, icon: ImageVector, isDark: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(94.dp)
    ) {
        Icon(
            icon, contentDescription = null,
            tint     = GoldPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.height(5.dp))
        Text(
            value,
            style    = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign= TextAlign.Center
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color    = if (isDark) GoldLight.copy(0.6f) else GoldDark.copy(0.7f),
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// ⚡  QUICK ACTION ROW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun QuickActionRow(
    account: com.alhasanah.alhasanahmedia.data.model.WalletAccountDto,
    isDark: Boolean,
    onTopUp: () -> Unit
) {
    val actions = listOf(
        Triple(Icons.Outlined.Payments,             "Top Up",   onTopUp),
        Triple(Icons.Outlined.History,              "Riwayat",  {}),
        Triple(Icons.Outlined.QrCodeScanner,        "Scan QR",  {}),
        Triple(Icons.Outlined.AccountBalanceWallet, "Kantin",   {})
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        actions.forEach { (icon, label, action) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { action() }
                    .padding(vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (isDark) DarkCard else GoldPrimary.copy(0.08f),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            1.dp,
                            GoldPrimary.copy(if (isDark) 0.3f else 0.2f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint     = GoldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize   = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isDark) Color.White.copy(0.75f)
                                     else MaterialTheme.colorScheme.onSurface.copy(0.75f)
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 📈  SPENDING MINI CHART — Canvas-drawn
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SpendingMiniChart(
    transactions: List<WalletTransactionDto>,
    isDark: Boolean
) {
    // Group last 7 days spending
    val last7Data = remember(transactions) {
        val dayMap = mutableMapOf<Int, Long>()
        for (i in 0..6) dayMap[i] = 0L
        transactions.filter { it.direction != "credit" }.forEach { tx ->
            runCatching {
                val dayAgo = ((System.currentTimeMillis() -
                    java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .parse(tx.createdAt.take(10))!!.time) / 86_400_000L).toInt()
                if (dayAgo in 0..6) dayMap[dayAgo] = (dayMap[dayAgo] ?: 0L) + tx.amount
            }
        }
        (0..6).map { dayMap[6 - it] ?: 0L }
    }
    val maxVal = last7Data.maxOrNull()?.takeIf { it > 0 } ?: 1L
    val totalSpent  = transactions.filter { it.direction != "credit" }.sumOf { it.amount }
    val totalCredit = transactions.filter { it.direction == "credit"  }.sumOf { it.amount }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isDark) DarkCard
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                if (isDark) DarkCardBorder else GoldPrimary.copy(0.14f),
                RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
    ) {
        Column {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "PENGELUARAN 7 HARI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight    = FontWeight.Black,
                            color         = GoldPrimary
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatRupiah(totalSpent),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "PEMASUKAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp, fontSize = 8.sp,
                            color = if (isDark) Color.White.copy(0.45f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        formatRupiah(totalCredit),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color      = CreditGreen
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                val w    = size.width
                val h    = size.height
                val pad  = 8.dp.toPx()
                val pts  = last7Data.size
                val step = (w - pad * 2) / (pts - 1).coerceAtLeast(1)

                // Compute points
                val points = last7Data.mapIndexed { i, v ->
                    val x = pad + i * step
                    val y = h - pad - (v.toFloat() / maxVal * (h - pad * 2))
                    Offset(x, y)
                }

                // Fill gradient under line
                val fillPath = Path().apply {
                    moveTo(points.first().x, h)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, h)
                    close()
                }
                drawPath(
                    path  = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(
                            GoldPrimary.copy(0.35f),
                            GoldPrimary.copy(0.0f)
                        )
                    )
                )

                // Line
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path   = linePath,
                    color  = GoldPrimary,
                    style  = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Dots
                points.forEach { pt ->
                    drawCircle(color = GoldPrimary, radius = 4.dp.toPx(), center = pt)
                    drawCircle(
                        color  = if (isDark) DarkCard else Color.White,
                        radius = 2.5.dp.toPx(),
                        center = pt
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Day labels
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val days = listOf("Sen","Sel","Rab","Kam","Jum","Sab","Min")
                val todayIdx = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
                val startDay = (todayIdx - 8 + 7) % 7
                days.indices.forEach { i ->
                    Text(
                        days[(startDay + i) % 7],
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = if (isDark) Color.White.copy(0.35f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                        )
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 🏧  PREMIUM TOP UP CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PremiumTopUpCard(
    amount: String,
    onAmountChange: (String) -> Unit,
    enabled: Boolean,
    onSubmit: () -> Unit,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) DarkCard else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isDark) DarkCardBorder else GoldPrimary.copy(0.18f),
                RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(GoldPrimary.copy(0.14f), RoundedCornerShape(12.dp))
                        .border(1.dp, GoldPrimary.copy(0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Payments, contentDescription = null, tint = GoldPrimary)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Top Up Saldo",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        "Diproses via Midtrans · Saldo aktif setelah konfirmasi",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color.White.copy(0.45f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                }
            }

            // Amount field
            FintechMoneyField(
                label = "Nominal Top Up",
                value = amount,
                onValueChange = onAmountChange,
                isDark = isDark
            )

            // Quick preset chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(25_000L, 50_000L, 100_000L, 150_000L, 200_000L, 500_000L)
                items(presets) { preset ->
                    val selected = amount == preset.toString()
                    Surface(
                        shape  = CircleShape,
                        color  = if (selected) GoldPrimary else Color.Transparent,
                        border = if (!selected) BorderStroke(
                            1.dp, GoldPrimary.copy(if (isDark) 0.38f else 0.3f)
                        ) else null,
                        modifier = Modifier.clickable(enabled = enabled) {
                            onAmountChange(preset.toString())
                        }
                    ) {
                        Text(
                            formatRupiah(preset),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White
                                        else if (isDark) GoldLight else GoldDark
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // Submit button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (enabled) GoldGradient
                        else Brush.linearGradient(listOf(Color.Gray.copy(0.3f), Color.Gray.copy(0.2f)))
                    )
                    .clickable(enabled = enabled && (amount.toLongOrNull() ?: 0L) >= 10_000L) {
                        onSubmit()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.Payments,
                        contentDescription = null,
                        tint     = if (enabled) Color(0xFF1A1200) else Color.White.copy(0.45f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Top Up Sekarang",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = if (enabled) Color(0xFF1A1200) else Color.White.copy(0.45f)
                        )
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 🔒  PREMIUM LIMIT DIALOG (Popup — bukan inline form)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PremiumLimitDialog(
    account: com.alhasanah.alhasanahmedia.data.model.WalletAccountDto,
    onDismiss: () -> Unit,
    onSave: (Long?, Long?, Long?, Long?) -> Unit,
    isDark: Boolean
) {
    var lowValue    by remember(account.lowBalanceThreshold)   { mutableStateOf(account.lowBalanceThreshold?.toString().orEmpty()) }
    var singleValue by remember(account.singleTransactionLimit){ mutableStateOf(account.singleTransactionLimit?.toString().orEmpty()) }
    var dailyValue  by remember(account.dailySpendLimit)       { mutableStateOf(account.dailySpendLimit?.toString().orEmpty()) }
    var monthlyValue by remember(account.monthlySpendLimit)    { mutableStateOf(account.monthlySpendLimit?.toString().orEmpty()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (isDark) DarkCard
                    else MaterialTheme.colorScheme.surface
                )
                .border(1.dp, GoldPrimary.copy(if (isDark) 0.35f else 0.2f), RoundedCornerShape(28.dp))
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                // Dialog header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(GoldGradient, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Tune, contentDescription = null,
                            tint = Color(0xFF1A1200), modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Atur Limit Dompet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            "Batas pengeluaran harian & bulanan santri",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) GoldLight.copy(0.6f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Limit fields
                listOf(
                    Triple("Ambang Saldo Rendah",      lowValue,     { v: String -> lowValue = v    }),
                    Triple("Maks. Sekali Transaksi",   singleValue,  { v: String -> singleValue = v }),
                    Triple("Limit Harian",             dailyValue,   { v: String -> dailyValue = v  }),
                    Triple("Limit Bulanan",            monthlyValue, { v: String -> monthlyValue= v }),
                ).forEachIndexed { i, (label, value, setter) ->
                    FintechMoneyField(
                        label      = label,
                        value      = value,
                        onValueChange = setter,
                        isDark     = isDark
                    )
                    if (i < 3) Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(20.dp))

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape  = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(0.4f))
                    ) {
                        Text(
                            "Batal",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldGradient)
                            .clickable {
                                onSave(
                                    lowValue.toLongOrNull(),
                                    singleValue.toLongOrNull(),
                                    dailyValue.toLongOrNull(),
                                    monthlyValue.toLongOrNull()
                                )
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Simpan Limit",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                color      = Color(0xFF1A1200)
                            )
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 📋  PREMIUM TRANSACTION ROW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PremiumTransactionRow(tx: WalletTransactionDto, isDark: Boolean) {
    val isCredit = tx.direction == "credit"
    val accent   = if (isCredit) CreditGreen else DebitRed
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .scale(if (isPressed) 0.975f else 1f)
            .background(if (isDark) DarkCard else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isDark) DarkCardBorder else GoldPrimary.copy(0.10f),
                RoundedCornerShape(18.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) {}
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + info
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(accent.copy(0.12f), RoundedCornerShape(14.dp))
                        .border(1.dp, accent.copy(0.25f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isCredit) Icons.Outlined.Payments else Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint     = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        tx.category.replace('_',' ').replaceFirstChar { it.uppercase() }
                            .uppercase(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        tx.keterangan ?: tx.status,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color.White.copy(0.45f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right: Amount + time
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isCredit) "+" else "-"}${formatRupiah(tx.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        color      = accent
                    )
                )
                Text(
                    tx.createdAt.take(10),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = if (isDark) Color.White.copy(0.35f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 🧩  REUSABLE COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PremiumStatusChip(status: String, isDark: Boolean) {
    val isActive = status == "active"
    Surface(
        shape = CircleShape,
        color = if (isActive) CreditGreen.copy(0.14f) else AmberAccent.copy(0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dotColor = if (isActive) CreditGreen else AmberAccent
            Box(
                Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                status.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    color      = if (isActive) CreditGreen else AmberAccent,
                    letterSpacing = 0.8.sp
                )
            )
        }
    }
}

@Composable
private fun WalletActivationCard(loading: Boolean, onActivate: () -> Unit, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) DarkCard else MaterialTheme.colorScheme.surface)
            .border(1.dp, AmberAccent.copy(0.35f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.CreditCard, null, tint = AmberAccent, modifier = Modifier.size(22.dp))
                Text(
                    "Dompet Belum Aktif",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            Text(
                "Aktivasi hanya dari aplikasi wali. Buat PIN transaksi untuk dipakai santri saat belanja kantin.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDark) Color.White.copy(0.55f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GoldGradient)
                    .clickable(enabled = !loading) { onActivate() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aktifkan Dompet",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        color      = Color(0xFF1A1200)
                    )
                )
            }
        }
    }
}

@Composable
private fun FintechMoneyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isDark: Boolean
) {
    OutlinedTextField(
        value         = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        label         = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon   = {
            Text(
                "Rp",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    color      = GoldPrimary
                ),
                modifier = Modifier.padding(start = 4.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine      = true,
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(12.dp),
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = GoldPrimary,
            unfocusedBorderColor = GoldPrimary.copy(if (isDark) 0.28f else 0.22f),
            focusedLabelColor    = GoldPrimary,
            cursorColor          = GoldPrimary,
            focusedContainerColor   = if (isDark) GoldGlow else GoldPrimary.copy(0.04f),
            unfocusedContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun PremiumFilterRow(
    activeFilter: WalletTxFilter,
    onFilterChange: (WalletTxFilter) -> Unit,
    activePeriod: WalletPeriodFilter,
    onPeriodChange: (WalletPeriodFilter) -> Unit,
    visibleCount: Int,
    totalCount: Int,
    isDark: Boolean
) {
    val hasActiveFilter = activeFilter != WalletTxFilter.SEMUA || activePeriod != WalletPeriodFilter.SEMUA
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WalletTxFilter.entries.forEach { filter ->
                WalletFilterChip(
                    label = filter.label,
                    active = activeFilter == filter,
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterChange(filter) }
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(WalletPeriodFilter.entries, key = { it.name }) { period ->
                WalletFilterChip(
                    label = period.shortLabel,
                    active = activePeriod == period,
                    isDark = isDark,
                    onClick = { onPeriodChange(period) }
                )
            }
        }

        AnimatedVisibility(
            visible = hasActiveFilter,
            enter = fadeIn(tween(180)) + expandVertically(tween(180)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(140))
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) GoldGlow else GoldPrimary.copy(0.08f),
                border = BorderStroke(1.dp, GoldPrimary.copy(if (isDark) 0.24f else 0.18f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Outlined.FilterList,
                            contentDescription = null,
                            tint = if (isDark) GoldLight else GoldDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "${activeFilter.label} • ${activePeriod.label} • $visibleCount/$totalCount",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White.copy(0.82f)
                                else MaterialTheme.colorScheme.onSurface.copy(0.78f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            onFilterChange(WalletTxFilter.SEMUA)
                            onPeriodChange(WalletPeriodFilter.SEMUA)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Reset filter",
                            tint = if (isDark) Color.White.copy(0.72f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletFilterChip(
    label: String,
    active: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (active) GoldPrimary else Color.Transparent,
        animationSpec = tween(180),
        label = "walletFilterBackground"
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) Color.Transparent else GoldPrimary.copy(if (isDark) 0.35f else 0.25f),
        animationSpec = tween(180),
        label = "walletFilterBorder"
    )

    Surface(
        shape = CircleShape,
        color = background,
        border = if (!active) BorderStroke(1.dp, borderColor) else null,
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = active,
                enter = fadeIn(tween(120)) + expandHorizontally(tween(120)),
                exit = fadeOut(tween(90)) + shrinkHorizontally(tween(90))
            ) {
                Row {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF1A1200),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (active) Color(0xFF1A1200)
                    else if (isDark) GoldLight else GoldDark
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WalletEmptyState(message: String, isDark: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(GoldPrimary.copy(0.1f), CircleShape)
                .border(1.dp, GoldPrimary.copy(0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.AccountBalanceWallet, null,
                tint = GoldPrimary, modifier = Modifier.size(28.dp)
            )
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isDark) Color.White.copy(0.45f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WalletSectionLabel(title: String, count: String? = null, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp).height(16.dp)
                    .background(GoldGradient, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = if (isDark) Color.White.copy(0.85f)
                            else MaterialTheme.colorScheme.onBackground
                )
            )
        }
        count?.let {
            Surface(shape = CircleShape, color = GoldPrimary.copy(0.14f)) {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GoldPrimary, fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 📱  KANTIN WALLET SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KantinWalletScreen(
    navController: NavController,
    viewModel: KantinWalletViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context  = LocalContext.current
    var showStudentPin by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme()
    SecureWalletWindow()
    WalletNfcReader(enabled = true, onPayload = viewModel::setQrPayload, onError = {})

    LaunchedEffect(state.error, state.info) {
        state.error?.let { snackbar.showSnackbar(it) }
        state.info?.let  { snackbar.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost    = { SnackbarHost(snackbar) },
        containerColor  = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AppSolidBackground(isDark = isDark)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
            item {
                FintechPageHeader(
                    title    = "Kantin Al-Hasanah",
                    subtitle = "Scan QR atau tempel kartu NFC",
                    onBack   = { navController.popBackStack() },
                    isDark   = isDark
                )
            }
            item { KantinMerchantSummaryCard(state, isDark) }
            item {
                Spacer(Modifier.height(12.dp))
                WalletSectionLabel("PEMBAYARAN KANTIN", null, isDark)
            }
            item {
                KantinPaymentCard(
                    state           = state,
                    loading         = state.loading,
                    isDark          = isDark,
                    onRegisterDevice= viewModel::registerDevice,
                    onQrPayloadChange = viewModel::setQrPayload,
                    onScanQr        = {
                        val options = GmsBarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .enableAutoZoom().build()
                        GmsBarcodeScanning.getClient(context, options)
                            .startScan()
                            .addOnSuccessListener { viewModel.setQrPayload(it.rawValue.orEmpty()) }
                            .addOnFailureListener { viewModel.setQrPayload(state.qrPayload) }
                    },
                    onAmountChange  = viewModel::setAmount,
                    onAuthorize     = viewModel::authorize,
                    onStudentPin    = { showStudentPin = true }
                )
            }
            item {
                state.merchantContext?.let { ctx ->
                    Spacer(Modifier.height(10.dp))
                    WalletSectionLabel("PENCAIRAN SALDO", ctx.assignment.merchantRole, isDark)
                    KantinSettlementCard(
                        state         = state,
                        canRequest    = ctx.assignment.merchantRole in listOf("owner","manager","cashier"),
                        onAmountChange= viewModel::setSettlementAmount,
                        onNoteChange  = viewModel::setSettlementNote,
                        onSubmit      = viewModel::requestSettlement,
                        isDark        = isDark
                    )
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                WalletSectionLabel("RIWAYAT KANTIN", "${state.history.size} item", isDark)
                Spacer(Modifier.height(6.dp))
            }
            items(state.history, key = { it.id }) { tx ->
                PremiumTransactionRow(tx, isDark)
            }
            }
        }
    }

    if (showStudentPin) {
        PinEntryDialog(
            title      = "PIN Santri",
            buttonText = "Konfirmasi Pembayaran",
            onDismiss  = { showStudentPin = false },
            onConfirm  = { pin ->
                showStudentPin = false
                viewModel.confirmWithStudentPin(pin)
            },
            isDark     = isDark
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// 🏪  KANTIN MERCHANT SUMMARY CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun KantinMerchantSummaryCard(state: KantinWalletUiState, isDark: Boolean) {
    val mc          = state.merchantContext
    val balance     = mc?.balance
    val device      = state.registeredDevice ?: mc?.device
    val deviceReady = device?.status == "active"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isDark) DarkCardGradient else Brush.linearGradient(
                listOf(Color(0xFFFDF6DC), Color(0xFFFFFBF0))
            ))
            .border(
                1.dp,
                Brush.linearGradient(listOf(GoldPrimary.copy(0.4f), GoldLight.copy(0.2f), GoldPrimary.copy(0.4f))),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        if (isDark) {
            Box(
                modifier = Modifier
                    .size(120.dp).align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-20).dp)
                    .background(Brush.radialGradient(listOf(GoldGlow, Color.Transparent)), CircleShape)
            )
        }
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "MERCHANT KANTIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black, letterSpacing = 1.6.sp, color = GoldPrimary
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        mc?.merchant?.name?.uppercase() ?: "KANTIN BELUM DIPILIH ADMIN",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        mc?.outlet?.name?.let { "Outlet: $it" } ?: "Outlet belum dipilih",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isDark) GoldLight.copy(0.55f) else GoldDark.copy(0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                PremiumStatusChip(if (deviceReady) "aktif" else kantinDeviceStatusText(device?.status ?: "baru"), isDark)
            }

            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(GoldPrimary.copy(if (isDark) 0.18f else 0.12f)))
            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LimitMetricItem("SALDO", formatRupiah(balance?.saldoAvailable ?: 0), Icons.Outlined.AccountBalanceWallet, isDark)
                Box(Modifier.width(1.dp).height(44.dp).background(GoldPrimary.copy(if (isDark) 0.18f else 0.12f)))
                LimitMetricItem("PENDING", formatRupiah(balance?.saldoPendingSettlement ?: 0), Icons.Outlined.PendingActions, isDark)
                Box(Modifier.width(1.dp).height(44.dp).background(GoldPrimary.copy(if (isDark) 0.18f else 0.12f)))
                LimitMetricItem("TERJUAL", formatRupiah(balance?.totalSales ?: 0), Icons.Outlined.CreditCard, isDark)
            }

            if (mc == null) {
                Spacer(Modifier.height(14.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = DebitRed.copy(0.1f)) {
                    Text(
                        if (deviceReady) {
                            "Perangkat sudah aktif. Minta admin membuka Manajemen Kantin dan menekan Siapkan Otomatis."
                        } else {
                            "Akun kantin belum siap. Minta admin mengaktifkan perangkat lalu menekan Siapkan Otomatis."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = DebitRed),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 💳  KANTIN PAYMENT CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun KantinPaymentCard(
    state: KantinWalletUiState, loading: Boolean, isDark: Boolean,
    onRegisterDevice: () -> Unit, onQrPayloadChange: (String) -> Unit,
    onScanQr: () -> Unit, onAmountChange: (String) -> Unit,
    onAuthorize: () -> Unit, onStudentPin: () -> Unit
) {
    val registered  = state.registeredDevice ?: state.merchantContext?.device
    val activeDevice= registered?.status == "active"
    val hasRegisteredDevice = registered != null
    val hasAmount = (state.amount.toLongOrNull() ?: 0L) > 0L
    val hasCardCode = state.qrPayload.isNotBlank()
    val hasMerchant = state.merchantContext != null
    val canProcessPayment = !loading && activeDevice && hasAmount && hasCardCode

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) DarkCard else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isDark) DarkCardBorder else GoldPrimary.copy(0.18f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.size(46.dp)
                            .background(GoldPrimary.copy(0.14f), RoundedCornerShape(14.dp))
                            .border(1.dp, GoldPrimary.copy(0.28f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.QrCodeScanner, null, tint = GoldPrimary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Terima Pembayaran",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            when {
                                activeDevice -> "Perangkat aktif · siap transaksi"
                                hasRegisteredDevice -> "Menunggu admin mengaktifkan perangkat"
                                else -> "Daftarkan perangkat ini satu kali"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (activeDevice) CreditGreen else AmberAccent
                            )
                        )
                    }
                }
                PremiumStatusChip(if (activeDevice) "aktif" else kantinDeviceStatusText(registered?.status ?: "baru"), isDark)
            }

            state.deviceInfo?.let { device ->
                Box(Modifier.fillMaxWidth().height(1.dp).background(GoldPrimary.copy(if (isDark) 0.14f else 0.1f)))
                WalletInfoLine("Perangkat", device.deviceId.takeLast(10), isDark)
                if (!hasRegisteredDevice) {
                    OutlinedButton(
                        onClick = onRegisterDevice, enabled = !loading,
                        border = BorderStroke(1.dp, GoldPrimary.copy(0.4f)),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Text("Daftarkan Perangkat Ini", color = GoldPrimary, fontWeight = FontWeight.Bold)
                    }
                } else if (!activeDevice) {
                    Surface(shape = RoundedCornerShape(12.dp), color = AmberAccent.copy(0.12f)) {
                        Text(
                            if (registered?.status == "pending") {
                                "Perangkat sudah didaftarkan. Minta admin mengaktifkan perangkat ini."
                            } else {
                                "Perangkat belum bisa dipakai. Hubungi admin pesantren."
                            },
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(color = AmberAccent, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            FintechMoneyField("Nominal Belanja", state.amount, onAmountChange, isDark)

            OutlinedTextField(
                value = state.cardLookup?.studentName ?: compactWalletCardCode(state.qrPayload),
                onValueChange = {},
                label = { Text("Kode Kartu Santri") },
                placeholder = { Text("Scan QR atau tempel kartu NFC") },
                singleLine = true,
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            when {
                state.cardLookupLoading -> {
                    Surface(shape = RoundedCornerShape(12.dp), color = GoldPrimary.copy(0.1f)) {
                        Text(
                            "Mengecek kartu santri...",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDark) GoldLight else GoldDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
                state.cardLookup != null -> {
                    val card = state.cardLookup
                    Surface(shape = RoundedCornerShape(14.dp), color = CreditGreen.copy(0.1f)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(
                                card.studentName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Black
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                listOfNotNull(card.studentClass?.let { "Kelas $it" }, card.studentMajor, walletStatusText(card.walletStatus))
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CreditGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                "ID kartu: ${compactWalletCardCode(card.walletPublicId)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color.White.copy(0.55f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
                state.qrPayload.isNotBlank() -> {
                    Surface(shape = RoundedCornerShape(12.dp), color = AmberAccent.copy(0.12f)) {
                        Text(
                            "Kartu terbaca. Data santri belum tampil, tetapi pembayaran tetap akan divalidasi server.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(color = AmberAccent, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onScanQr, modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, GoldPrimary.copy(0.4f)),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.QrCodeScanner, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Scan Kartu", color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (canProcessPayment) GoldGradient else Brush.linearGradient(listOf(Color.Gray.copy(0.3f), Color.Gray.copy(0.2f))))
                        .clickable(enabled = canProcessPayment) { onAuthorize() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Proses Bayar",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = if (canProcessPayment) Color(0xFF1A1200) else Color.White.copy(0.45f)
                        )
                    )
                }
            }

            if (!canProcessPayment) {
                Text(
                    when {
                        !activeDevice -> "Perangkat harus aktif sebelum menerima pembayaran."
                        !hasAmount -> "Isi nominal belanja terlebih dahulu."
                        !hasCardCode -> "Scan kartu santri terlebih dahulu."
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color.White.copy(alpha = 0.58f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            if (activeDevice && !hasMerchant) {
                Text(
                    "Perangkat aktif, tetapi akun kantin belum siap. Minta admin membuka Manajemen Kantin dan menekan Siapkan Otomatis.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AmberAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            state.authorization?.let { auth ->
                Box(Modifier.fillMaxWidth().height(1.dp).background(GoldPrimary.copy(0.14f)))
                WalletInfoLine("Status",        statusPembayaranKantin(auth.status), isDark)
                WalletInfoLine("Nominal",       formatRupiah(auth.amount), isDark)
                WalletInfoLine("Batas waktu",   auth.expiresAt, isDark)
                if (auth.amount > 75_000L || auth.authorizationMode == "parent_approval") {
                    Surface(shape = RoundedCornerShape(10.dp), color = AmberAccent.copy(0.12f)) {
                        Text(
                            "Nominal di atas Rp75.000. Tunggu wali menyetujui transaksi ini.",
                            style    = MaterialTheme.typography.bodySmall.copy(color = AmberAccent, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(GoldGradient)
                            .clickable(enabled = !loading) { onStudentPin() }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Badge, null, tint = Color(0xFF1A1200), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Minta PIN Santri",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black, color = Color(0xFF1A1200)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 🏦  KANTIN SETTLEMENT CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun KantinSettlementCard(
    state: KantinWalletUiState, canRequest: Boolean, isDark: Boolean,
    onAmountChange: (String) -> Unit, onNoteChange: (String) -> Unit, onSubmit: () -> Unit
) {
    val balance = state.merchantContext?.balance
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) DarkCard else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isDark) DarkCardBorder else GoldPrimary.copy(0.18f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp)
                        .background(PurpleAccent.copy(0.14f), RoundedCornerShape(12.dp))
                        .border(1.dp, PurpleAccent.copy(0.28f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AccountBalanceWallet, null, tint = PurpleAccent)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Pencairan Saldo Kantin",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        "Dana dicatat di ledger internal · Dibayar manual bendahara",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color.White.copy(0.45f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    WalletInfoLine("Saldo dapat dicairkan", formatRupiah(balance?.saldoAvailable ?: 0), isDark)
                }
                Column(Modifier.weight(1f)) {
                    WalletInfoLine("Menunggu proses", formatRupiah(balance?.saldoPendingSettlement ?: 0), isDark)
                }
            }

            FintechMoneyField("Nominal pencairan", state.settlementAmount, onAmountChange, isDark)

            OutlinedTextField(
                value         = state.settlementNote,
                onValueChange = onNoteChange,
                label         = { Text("Catatan rekening / tujuan") },
                minLines      = 2,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = GoldPrimary,
                    unfocusedBorderColor = GoldPrimary.copy(if (isDark) 0.28f else 0.2f),
                    focusedLabelColor    = GoldPrimary,
                    cursorColor          = GoldPrimary
                )
            )

            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(
                        if (canRequest && !state.loading) GoldGradient
                        else Brush.linearGradient(listOf(Color.Gray.copy(0.3f), Color.Gray.copy(0.2f)))
                    )
                    .clickable(enabled = !state.loading && canRequest) { onSubmit() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Ajukan Pencairan",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = if (canRequest && !state.loading) Color(0xFF1A1200) else Color.White.copy(0.4f)
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 🔍  WALLET INFO LINE
// ═══════════════════════════════════════════════════════════════

@Composable
private fun WalletInfoLine(label: String, value: String, isDark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color   = if (isDark) Color.White.copy(0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize= 9.sp
            )
        )
        Text(
            value,
            style     = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White.copy(0.85f) else MaterialTheme.colorScheme.onSurface
            ),
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier  = Modifier.weight(1f).padding(start = 8.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// 🔐  DIALOGS (PIN)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit, isDark: Boolean
) {
    var pin     by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid   = pin.length in 4..12 && pin.all(Char::isDigit) && pin == confirm

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) DarkCard else MaterialTheme.colorScheme.surface)
                .border(1.dp, GoldPrimary.copy(if (isDark) 0.35f else 0.2f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(44.dp).background(GoldGradient, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Security, null, tint = Color(0xFF1A1200), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            "Buat PIN Dompet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            "PIN dipakai santri untuk transaksi kantin ≤ Rp75.000",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) GoldLight.copy(0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                PinTextField("PIN baru", pin, { pin = it }, isDark)
                PinTextField("Ulangi PIN", confirm, { confirm = it }, isDark)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss, modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, GoldPrimary.copy(0.4f)),
                        shape  = RoundedCornerShape(12.dp)
                    ) { Text("Batal", color = GoldPrimary, fontWeight = FontWeight.Bold) }

                    Box(
                        modifier = Modifier
                            .weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (valid) GoldGradient else Brush.linearGradient(listOf(Color.Gray.copy(0.3f), Color.Gray.copy(0.2f))))
                            .clickable(enabled = valid) {
                                val submitted = pin.toCharArray()
                                pin = ""; confirm = ""
                                onConfirm(submitted)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Aktifkan",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = if (valid) Color(0xFF1A1200) else Color.White.copy(0.4f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinEntryDialog(
    title: String, buttonText: String,
    onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit, isDark: Boolean
) {
    var pin by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) DarkCard else MaterialTheme.colorScheme.surface)
                .border(1.dp, GoldPrimary.copy(if (isDark) 0.35f else 0.2f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )
                PinTextField("PIN", pin, { pin = it }, isDark)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss, modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, GoldPrimary.copy(0.4f)),
                        shape  = RoundedCornerShape(12.dp)
                    ) { Text("Batal", color = GoldPrimary, fontWeight = FontWeight.Bold) }
                    Box(
                        modifier = Modifier
                            .weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (pin.length >= 4) GoldGradient else Brush.linearGradient(listOf(Color.Gray.copy(0.3f), Color.Gray.copy(0.2f))))
                            .clickable(enabled = pin.length in 4..12) {
                                val submitted = pin.toCharArray(); pin = ""
                                onConfirm(submitted)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            buttonText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = if (pin.length >= 4) Color(0xFF1A1200) else Color.White.copy(0.4f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinTextField(label: String, value: String, onValueChange: (String) -> Unit, isDark: Boolean) {
    OutlinedTextField(
        value           = value,
        onValueChange   = { onValueChange(it.filter(Char::isDigit).take(12)) },
        label           = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine      = true,
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(12.dp),
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = GoldPrimary,
            unfocusedBorderColor = GoldPrimary.copy(if (isDark) 0.28f else 0.22f),
            focusedLabelColor    = GoldPrimary,
            cursorColor          = GoldPrimary
        )
    )
}

// ═══════════════════════════════════════════════════════════════
// 📄  DISPUTE SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDisputeScreen(
    ledgerId: Long,
    navController: NavController,
    viewModel: WalletDisputeViewModel = koinViewModel { parametersOf(ledgerId) }
) {
    val state   = viewModel.uiState.collectAsState().value
    val snackbar= remember { SnackbarHostState() }
    val isDark  = isAppInDarkTheme()
    SecureWalletWindow()

    LaunchedEffect(state.error, state.info) {
        state.error?.let { snackbar.showSnackbar(it) }
        state.info?.let  { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            AppPageHeader(
                title = "LAPORKAN TRANSAKSI",
                subtitle = "Kirim koreksi transaksi dompet",
                isDark = isDark,
                onBack = { navController.popBackStack() },
                size = AppPageHeaderSize.Compact,
                titleTopPadding = 50.dp
            )
        },
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            Surface(shape = RoundedCornerShape(12.dp), color = GoldPrimary.copy(0.1f)) {
                Text(
                    "ID Transaksi: $ledgerId",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold, color = GoldPrimary
                    ),
                    modifier = Modifier.padding(10.dp)
                )
            }
            OutlinedTextField(
                value = state.santriNis, onValueChange = viewModel::setSantriNis,
                label = { Text("NIS Santri") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary, unfocusedBorderColor = GoldPrimary.copy(0.3f),
                    focusedLabelColor = GoldPrimary, cursorColor = GoldPrimary
                )
            )
            OutlinedTextField(
                value = state.reason, onValueChange = viewModel::setReason,
                label = { Text("Alasan laporan") }, minLines = 4,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary, unfocusedBorderColor = GoldPrimary.copy(0.3f),
                    focusedLabelColor = GoldPrimary, cursorColor = GoldPrimary
                )
            )
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (!state.loading) GoldGradient else Brush.linearGradient(listOf(Color.Gray.copy(0.3f), Color.Gray.copy(0.2f))))
                    .clickable(enabled = !state.loading) { viewModel.submit() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Kirim Laporan",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = if (!state.loading) Color(0xFF1A1200) else Color.White.copy(0.4f)
                    )
                )
            }
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, GoldPrimary.copy(0.4f)),
                shape  = RoundedCornerShape(14.dp)
            ) { Text("Batal", color = GoldPrimary, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 🔧  HELPERS
// ═══════════════════════════════════════════════════════════════

private fun walletStatusText(status: String): String = when (status.lowercase()) {
    "active", "aktif" -> "Dompet aktif"
    "locked", "terkunci" -> "Dompet terkunci"
    "suspended" -> "Dompet ditahan"
    "closed" -> "Dompet ditutup"
    else -> status.replace('_',' ').replaceFirstChar { it.uppercase() }
}

private fun compactWalletCardCode(value: String): String {
    val clean = value.trim()
    if (clean.isBlank()) return ""
    return if (clean.length <= 14) clean else "${clean.take(6)}...${clean.takeLast(6)}"
}

private fun kantinDeviceStatusText(status: String): String = when (status.lowercase()) {
    "active" -> "aktif"
    "pending" -> "menunggu admin"
    "suspended" -> "ditahan"
    "revoked" -> "dicabut"
    "baru" -> "belum daftar"
    "belum terdaftar" -> "belum daftar"
    else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun statusPembayaranKantin(status: String): String = when (status.lowercase()) {
    "requires_authorization" -> "Menunggu PIN atau persetujuan wali"
    "authorized" -> "Disetujui"
    "processing" -> "Diproses"
    "posted" -> "Berhasil"
    "pending" -> "Menunggu"
    "expired" -> "Kedaluwarsa"
    "failed" -> "Gagal"
    "cancelled" -> "Dibatalkan"
    else -> walletStatusText(status)
}

@Composable
private fun WalletNfcReader(enabled: Boolean, onPayload: (String) -> Unit, onError: (Throwable) -> Unit) {
    val activity = LocalActivity.current
    DisposableEffect(activity, enabled) {
        val adapter = activity?.let { NfcAdapter.getDefaultAdapter(it) }
        if (activity != null && adapter != null && enabled) {
            adapter.enableReaderMode(
                activity,
                { tag ->
                    runCatching { readNfcPayload(tag) }
                        .onSuccess { if (it.isNotBlank()) activity.runOnUiThread { onPayload(it) } }
                        .onFailure { e -> activity.runOnUiThread { onError(e) } }
                },
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
        }
        onDispose { if (activity != null && adapter != null) adapter.disableReaderMode(activity) }
    }
}

private fun readNfcPayload(tag: Tag): String {
    val ndef = Ndef.get(tag) ?: return tag.id.joinToString("") { "%02x".format(it) }
    return runCatching {
        ndef.connect()
        val message: NdefMessage = ndef.cachedNdefMessage ?: return@runCatching ""
        message.records.firstNotNullOfOrNull(::decodeNdefRecord).orEmpty()
    }.also { runCatching { ndef.close() } }.getOrThrow()
}

private fun decodeNdefRecord(record: NdefRecord): String? {
    val payload = record.payload ?: return null
    if (payload.isEmpty()) return null
    return when {
        record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
            val ll = payload[0].toInt() and 0x3F
            payload.copyOfRange(1 + ll, payload.size).toString(Charsets.UTF_8)
        }
        record.tnf == NdefRecord.TNF_MIME_MEDIA -> payload.toString(Charsets.UTF_8)
        else -> null
    }
}

@Composable
private fun SecureWalletWindow() {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

@Composable
private fun isSystemInDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.05f
