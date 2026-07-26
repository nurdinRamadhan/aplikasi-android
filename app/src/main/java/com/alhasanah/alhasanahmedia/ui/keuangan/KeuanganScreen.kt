package com.alhasanah.alhasanahmedia.ui.keuangan

// ─────────────────────────────────────────────────────────────────────────────
// Imports
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.CorePaymentData
import com.alhasanah.alhasanahmedia.data.model.CorePaymentMethod
import com.alhasanah.alhasanahmedia.data.model.PembayaranTagihanDto
import com.alhasanah.alhasanahmedia.data.model.TagihanCache
import com.alhasanah.alhasanahmedia.data.model.TagihanStatus
import com.alhasanah.alhasanahmedia.data.model.TagihanWithDetail
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.ui.components.ComingSoonDialog
import com.alhasanah.alhasanahmedia.ui.payment.PaymentInstructionData
import com.alhasanah.alhasanahmedia.ui.payment.PaymentMethodPickerDialog
import com.alhasanah.alhasanahmedia.util.formatDate
import com.alhasanah.alhasanahmedia.util.formatRupiah
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// ─────────────────────────────────────────────────────────────────────────────
// Filter State
// ─────────────────────────────────────────────────────────────────────────────

enum class TagihanFilter(val label: String) {
    SEMUA("Semua"),
    BELUM_LUNAS("Belum Lunas"),
    CICILAN("Cicilan"),
    LUNAS("Lunas")
}

private enum class RiwayatPembayaranFilter(val label: String) {
    SEMUA("Semua"),
    BERHASIL("Berhasil"),
    MENUNGGU("Menunggu"),
    GAGAL("Gagal"),
    CICILAN("Cicilan"),
    PELUNASAN("Pelunasan")
}

// ─────────────────────────────────────────────────────────────────────────────
// KeuanganScreen — Root
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeuanganScreen(
    santriNis: String,
    navController: NavController,
    viewModel: KeuanganViewModel = koinViewModel { parametersOf(santriNis) }
) {
    val tagihanState     by viewModel.tagihanState.collectAsState()
    val santriInfoState  by viewModel.santriInfoState.collectAsState()
    val pembayaranTagihanState by viewModel.pembayaranTagihanState.collectAsState()
    val riwayatPembayaranState by viewModel.riwayatPembayaranState.collectAsState()
    var showDetailSheet  by remember { mutableStateOf<TagihanWithDetail?>(null) }
    var amountPickerTagihan by remember { mutableStateOf<TagihanWithDetail?>(null) }
    var pendingPaymentTagihan by remember { mutableStateOf<TagihanWithDetail?>(null) }
    var pendingPaymentAmount by remember { mutableStateOf<Long?>(null) }
    var paymentSuccessMessage by remember { mutableStateOf<String?>(null) }
    var activeFilter     by remember { mutableStateOf(TagihanFilter.SEMUA) }
    var selectedPaymentMethod by remember { mutableStateOf(CorePaymentMethod.QRIS) }

    LaunchedEffect(Unit) {
        viewModel.launchCorePayment.collect { payment ->
            navController.navigate(Screen.PaymentInstruction.createRoute(payment.toInstructionData()))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.paymentSuccessEvent.collect { message ->
            paymentSuccessMessage = message
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Subtle diagonal finance grid
        FinanceDiagonalPattern()

        LazyColumn(
            modifier             = Modifier.fillMaxSize(),
            contentPadding       = PaddingValues(bottom = 128.dp),
            verticalArrangement  = Arrangement.spacedBy(0.dp)
        ) {

            // ── 1. Finance Header (TopBar + Summary Card) ─────────────────
            item {
                FinanceHeader(
                    santriInfoState = santriInfoState,
                    tagihanState    = tagihanState,
                    onBack          = { navController.popBackStack() }
                )
            }

            // ── 2. Filter Chips ───────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                FilterChipRow(
                    activeFilter = activeFilter,
                    onFilterChange = { activeFilter = it }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ── 3. Section Label ──────────────────────────────────────────
            item {
                Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text  = "DAFTAR TAGIHAN",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight    = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                    color         = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }

                    // Count badge
                    val tagihanSuccess = tagihanState as? TagihanUiState.Success
                    if (tagihanSuccess != null) {
                        val count = tagihanSuccess.cache.items.count { it.matchesFilter(activeFilter) }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text     = "$count item",
                                style    = MaterialTheme.typography.labelSmall.copy(
                                    color      = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── 4. Tagihan List ───────────────────────────────────────────
            when (val state = tagihanState) {
                is TagihanUiState.Loading -> {
                    item {
                        Box(
                            modifier            = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment    = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                is TagihanUiState.Error -> {
                    item {
                        FinanceErrorState(message = state.message)
                    }
                }
                is TagihanUiState.Success -> {
                    val cache = state.cache
                    val filtered = cache.items.filter { it.matchesFilter(activeFilter) }
                    val lastPaymentsByTagihan = (riwayatPembayaranState as? RiwayatPembayaranUiState.Success)
                        ?.items
                        ?.filter { it.status.equals("posted", ignoreCase = true) }
                        ?.groupBy { it.tagihanId }
                        ?.mapValues { (_, payments) -> payments.firstOrNull() }
                        .orEmpty()

                    // Stale indicator
                    if (cache.isStale) {
                        item {
                            StaleIndicator(cache)
                        }
                    }

                    if (filtered.isEmpty()) {
                        item { FinanceEmptyState(filter = activeFilter) }
                    } else {
                        items(filtered, key = { it.id }) { tagihan ->
                            AnimatedVisibility(
                                visible    = true,
                                enter      = fadeIn() + slideInVertically { it / 3 }
                            ) {
                                TagihanCard(
                                    tagihan  = tagihan,
                                    lastPayment = lastPaymentsByTagihan[tagihan.id],
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 7.dp),
                                    onClick  = { showDetailSheet = tagihan }
                                )
                    }

                    // Refresh button
                    IconButton(onClick = { viewModel.refreshData() }) {
                        val rotation by animateFloatAsState(
                            targetValue = if (tagihanState is TagihanUiState.Loading) 360f else 0f,
                            animationSpec = if (tagihanState is TagihanUiState.Loading) {
                                infiniteRepeatable(animation = tween(1000, easing = LinearEasing))
                            } else {
                                tween(durationMillis = 300)
                            },
                            label = "refreshRotation"
                        )
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = rotation }
                        )
                    }
                }
            }
                    item {
                        GlobalPaymentHistorySection(
                            riwayatState = riwayatPembayaranState,
                            tagihanById = state.cache.items.associateBy { it.id },
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
                        )
                    }
                }
            }
        }
    }

    // ── Detail Bottom Sheet ─────────────────────────────────────────────────
    showDetailSheet?.let { tagihan ->
        LaunchedEffect(tagihan.id) {
            viewModel.loadPembayaranTagihan(tagihan.id)
        }
        ModalBottomSheet(
            onDismissRequest = { showDetailSheet = null },
            containerColor   = MaterialTheme.colorScheme.surface,
            tonalElevation   = 8.dp
        ) {
            TagihanDetailSheet(
                tagihan     = tagihan,
                pembayaranState = pembayaranTagihanState[tagihan.id] ?: PembayaranTagihanUiState.Idle,
                onBayarClick = {
                    showDetailSheet = null
                    amountPickerTagihan = it
                }
            )
        }
    }

    amountPickerTagihan?.let { tagihan ->
        PaymentAmountDialog(
            tagihan = tagihan,
            onDismiss = { amountPickerTagihan = null },
            onConfirm = { amount ->
                pendingPaymentAmount = amount
                pendingPaymentTagihan = tagihan
                amountPickerTagihan = null
            }
        )
    }

    pendingPaymentTagihan?.let { tagihan ->
        PaymentMethodPickerDialog(
            selected = selectedPaymentMethod,
            onSelected = { selectedPaymentMethod = it },
            onDismiss = {
                pendingPaymentTagihan = null
                pendingPaymentAmount = null
            },
            onConfirm = {
                pendingPaymentTagihan = null
                viewModel.bayarTagihan(
                    tagihan = tagihan,
                    amount = pendingPaymentAmount ?: (tagihan.sisaTagihan ?: 0L),
                    paymentMethod = selectedPaymentMethod
                )
                pendingPaymentAmount = null
            },
            title = if ((pendingPaymentAmount ?: 0L) < (tagihan.sisaTagihan ?: 0L)) {
                "Pilih Metode Cicilan"
            } else {
                "Pilih Pembayaran Tagihan"
            },
            confirmText = if ((pendingPaymentAmount ?: 0L) < (tagihan.sisaTagihan ?: 0L)) {
                "Bayar Cicilan ${formatRupiah(pendingPaymentAmount ?: 0L)}"
            } else {
                "Lunasi ${formatRupiah(tagihan.sisaTagihan ?: 0L)}"
            }
        )
    }

    paymentSuccessMessage?.let { message ->
        SuccessPaymentDialog(
            title = if (message.contains("cicilan", ignoreCase = true)) {
                "Cicilan Berhasil"
            } else {
                "Pembayaran Lunas"
            },
            message = message,
            buttonText = "LIHAT RIWAYAT",
            onDismiss = { paymentSuccessMessage = null }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter Helper
// ─────────────────────────────────────────────────────────────────────────────

private fun TagihanWithDetail.matchesFilter(filter: TagihanFilter): Boolean = when (filter) {
    TagihanFilter.SEMUA       -> true
    TagihanFilter.LUNAS       -> status == TagihanStatus.LUNAS
    TagihanFilter.CICILAN     -> status == TagihanStatus.CICILAN
    TagihanFilter.BELUM_LUNAS -> status != TagihanStatus.LUNAS
}

private fun CorePaymentData.toInstructionData(): PaymentInstructionData =
    PaymentInstructionData(
        orderId = orderId,
        transactionId = transactionId.orEmpty(),
        methodCode = methodCode,
        methodLabel = methodLabel,
        amount = amount,
        expiresAt = expiresAt.orEmpty(),
        qrUrl = qrUrl.orEmpty(),
        deeplinkUrl = deeplinkUrl.orEmpty(),
        vaNumber = vaNumber ?: permataVaNumber.orEmpty(),
        bank = bank.orEmpty(),
        billerCode = billerCode.orEmpty(),
        billKey = billKey.orEmpty(),
        paymentCode = paymentCode.orEmpty(),
        store = store.orEmpty(),
        message = "Silakan selesaikan pembayaran. Status tagihan akan otomatis berubah setelah pembayaran terkonfirmasi."
    )

// ─────────────────────────────────────────────────────────────────────────────
// Urgency helper — returns true if due date is within 7 days or overdue
// ─────────────────────────────────────────────────────────────────────────────

private fun isUrgent(date: java.time.LocalDate?): Boolean {
    if (date == null) return false
    val now = java.time.LocalDate.now()
    val diff = java.time.temporal.ChronoUnit.DAYS.between(now, date)
    return diff in 0..7
}

private fun isOverdue(date: java.time.LocalDate?): Boolean {
    if (date == null) return false
    return date.isBefore(java.time.LocalDate.now())
}

// ─────────────────────────────────────────────────────────────────────────────
// Background — Diagonal Finance Grid (bukan kotak biasa)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FinanceDiagonalPattern() {
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.04f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 56.dp.toPx()
        // Diagonal lines NW→SE
        var x = -size.height
        while (x < size.width + size.height) {
            drawLine(
                color       = lineColor,
                start       = Offset(x, 0f),
                end         = Offset(x + size.height, size.height),
                strokeWidth = 0.5.dp.toPx()
            )
            x += step
        }
        // Horizontal fine lines — very subtle
        var y = 0f
        while (y < size.height) {
            drawLine(
                color       = lineColor.copy(alpha = 0.025f),
                start       = Offset(0f, y),
                end         = Offset(size.width, y),
                strokeWidth = 0.3.dp.toPx()
            )
            y += step * 2
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Finance Header — TopBar + Santri Summary Financial Card
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceHeader(
    santriInfoState: SantriInfoState,
    tagihanState: TagihanUiState,
    onBack: () -> Unit
) {
    val isDark  = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val titleColor = if (isDark) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isDark) primary.copy(alpha = 0.90f) else MaterialTheme.colorScheme.onSurfaceVariant

    val headerBrush = if (isDark) {
        Brush.verticalGradient(
            0.0f to Color(0xFF10100D),
            0.52f to Color(0xFF171A1A),
            0.88f to MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
            1.0f to MaterialTheme.colorScheme.background
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color(0xFFFFFCF7),
            0.55f to Color(0xFFFBF5E9),
            0.88f to MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
            1.0f to MaterialTheme.colorScheme.background
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        AppPageHeaderBackground(isDark = isDark, modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxWidth()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 34.dp, start = 24.dp, end = 24.dp)
            ) {
                Surface(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.10f else 0.55f),
                    border = BorderStroke(1.dp, primary.copy(alpha = 0.35f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = titleColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 54.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = "KEUANGAN SANTRI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = primary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text  = "Informasi Tagihan & Pembayaran",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = subtitleColor,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(150.dp))

            // ── Financial Summary Card ──────────────────────────────────────
            when (val infoState = santriInfoState) {
                is SantriInfoState.Loading -> {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primary, strokeWidth = 2.dp)
                    }
                }
                is SantriInfoState.Error -> {
                    Text(
                        text     = infoState.message,
                        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }
                is SantriInfoState.Success -> {
                    FinancialSummaryCard(
                        santriInfo   = infoState.santriInfo,
                        tagihanState = tagihanState
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FinanceHeroOrnament(isDark: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val lineColor = primary.copy(alpha = if (isDark) 0.18f else 0.11f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        val centerX = size.width / 2f
        val archTop = 48.dp.toPx()
        val archWidth = size.width * 1.02f
        val archHeight = 260.dp.toPx()

        repeat(4) { index ->
            val inset = index * 18.dp.toPx()
            drawArc(
                color = lineColor.copy(alpha = lineColor.alpha * (1f - index * 0.16f)),
                startAngle = 204f,
                sweepAngle = 132f,
                useCenter = false,
                topLeft = Offset(centerX - archWidth / 2f + inset, archTop + inset * 0.55f),
                size = androidx.compose.ui.geometry.Size(archWidth - inset * 2f, archHeight - inset),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        repeat(9) { index ->
            val angle = Math.toRadians((220 + index * 12).toDouble())
            val radius = archWidth * 0.34f
            val x = centerX + kotlin.math.cos(angle).toFloat() * radius
            val y = archTop + archHeight * 0.52f + kotlin.math.sin(angle).toFloat() * radius
            drawCircle(
                color = lineColor.copy(alpha = lineColor.alpha * 0.65f),
                radius = 2.2.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Financial Summary Card — Replaces HolographicSantriCard
// Shows: Nama, NIS, Total Tagihan, Total Terbayar, Sisa, Progress Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FinancialSummaryCard(
    santriInfo: SantriInfo,
    tagihanState: TagihanUiState
) {
    val isDark = isAppInDarkTheme()
    // Shimmer animation
    val shimmerTrans = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by shimmerTrans.animateFloat(
        initialValue  = -400f,
        targetValue   = 800f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    // Compute financial aggregates from tagihan
    val (totalTagihan, totalTerbayar, sisaTotal) = remember(tagihanState) {
        if (tagihanState is TagihanUiState.Success) {
            val items = tagihanState.cache.items
            val total    = items.sumOf { it.nominalTagihan ?: 0L }
            val sisa     = items
                .filter { it.status != TagihanStatus.LUNAS }
                .sumOf { it.sisaTagihan ?: 0L }
            val terbayar = total - sisa
            Triple(total, terbayar, sisa)
        } else Triple(0L, 0L, 0L)
    }

    val progressFraction = if (totalTagihan > 0) {
        (totalTerbayar.toFloat() / totalTagihan.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue   = progressFraction,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label         = "progressAnim"
    )

    val primary = MaterialTheme.colorScheme.primary
    val cardContent = MaterialTheme.colorScheme.onSurface
    val mutedContent = MaterialTheme.colorScheme.onSurfaceVariant
    val cardContainer = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.54f else 0.78f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardContainer)
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            primary.copy(alpha = if (isDark) 0.08f else 0.05f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerX, 0f),
                        end   = Offset(shimmerX + 200f, size.height)
                    )
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.55f),
                        primary.copy(alpha = 0.18f),
                        primary.copy(alpha = 0.38f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {

            // ── Row 1: Identity ─────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = "DIGITAL SANTRI ID",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color         = if (isDark) primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight    = FontWeight.Black,
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text     = santriInfo.nama.uppercase(),
                        style    = MaterialTheme.typography.titleLarge.copy(
                            fontWeight    = FontWeight.Black,
                            color         = cardContent
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector  = Icons.Outlined.Badge,
                            contentDescription = null,
                            tint         = primary.copy(alpha = 0.78f),
                            modifier     = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text  = "NIS: ${santriInfo.nis}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color      = mutedContent,
                                fontWeight = FontWeight.Bold,
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Status chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = primary.copy(alpha = if (isDark) 0.12f else 0.10f)
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(primary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text  = "AKTIF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color      = cardContent,
                                fontWeight = FontWeight.Black,
                                fontSize   = 9.sp,
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)))
            Spacer(modifier = Modifier.height(20.dp))

            // ── Row 2: 3-Column Financial Summary ──────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FinanceSummaryMetric(
                    label  = "TOTAL TAGIHAN",
                    value  = if (totalTagihan > 0) formatRupiah(totalTagihan) else "—",
                    icon   = Icons.Outlined.Receipt,
                    accent = mutedContent
                )
                // Vertical divider
                Box(modifier = Modifier.width(0.5.dp).height(52.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)))
                FinanceSummaryMetric(
                    label  = "TERBAYAR",
                    value  = if (totalTerbayar > 0) formatRupiah(totalTerbayar) else "—",
                    icon   = Icons.Outlined.CheckCircle,
                    accent = primary
                )
                Box(modifier = Modifier.width(0.5.dp).height(52.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)))
                FinanceSummaryMetric(
                    label  = "SISA TAGIHAN",
                    value  = if (sisaTotal > 0) formatRupiah(sisaTotal) else "Lunas ✓",
                    icon   = Icons.Outlined.PendingActions,
                    accent = if (sisaTotal > 0) MaterialTheme.colorScheme.error else primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Row 3: Payment Progress Bar ─────────────────────────────────
            Column {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = "Progres Pembayaran",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color  = mutedContent,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    Text(
                        text  = "${(progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color      = cardContent,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                ) {
                    // Fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(primary.copy(alpha = 0.82f), primary)
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceSummaryMetric(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector  = icon,
            contentDescription = null,
            tint         = accent,
            modifier     = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text      = value,
            style     = MaterialTheme.typography.labelMedium.copy(
                fontWeight    = FontWeight.Black,
                color         = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.sp
            ),
            textAlign = TextAlign.Center,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis
        )
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall.copy(
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight    = FontWeight.Normal,
                letterSpacing = 0.5.sp,
                fontSize      = 8.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter Chip Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FilterChipRow(
    activeFilter: TagihanFilter,
    onFilterChange: (TagihanFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TagihanFilter.entries.forEach { filter ->
            val isActive = filter == activeFilter
            val primary  = MaterialTheme.colorScheme.primary

            Surface(
                shape  = CircleShape,
                color  = if (isActive) primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                border = if (!isActive) BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
                ) else null,
                shadowElevation = if (isActive) 4.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null
                    ) { onFilterChange(filter) }
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isActive) {
                        Icon(
                            imageVector  = Icons.Default.Check,
                            contentDescription = null,
                            tint         = MaterialTheme.colorScheme.onPrimary,
                            modifier     = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text  = filter.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color      = if (isActive) MaterialTheme.colorScheme.onPrimary
                                         else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stale Indicator — Shows when data is from cache and not recently synced
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StaleIndicator(cache: TagihanCache?) {
    val isStale = cache?.isStale == true
    if (!isStale) return

    val syncedAt = cache?.serverSyncedAt
    val timeAgo = syncedAt?.let { System.currentTimeMillis() - it }
    val label = when {
        timeAgo == null -> "Data offline"
        timeAgo < 60_000 -> "Data offline • Baru saja"
        timeAgo < 3_600_000 -> "Data offline • ${(timeAgo / 60_000)} menit lalu"
        timeAgo < 86_400_000 -> "Data offline • ${(timeAgo / 3_600_000)} jam lalu"
        else -> "Data offline • ${(timeAgo / 86_400_000)} hari lalu"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(    imageVector = Icons.Filled.Info,
                 contentDescription = "Info",
                 modifier = Modifier.size(16.dp),
                 tint = MaterialTheme.colorScheme.onSecondaryContainer
        )

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TagihanCard — Premium with Urgency Indicator & Press Animation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TagihanCard(
    tagihan: TagihanWithDetail,
    lastPayment: PembayaranTagihanDto? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isLunas   = tagihan.status == TagihanStatus.LUNAS
    val isCicilan = tagihan.status == TagihanStatus.CICILAN
    val overdue   = !isLunas && isOverdue(tagihan.tanggalJatuhTempo)
    val urgent    = !isLunas && !overdue && isUrgent(tagihan.tanggalJatuhTempo)
    val terbayar  = (tagihan.nominalTagihan ?: 0L) - (tagihan.sisaTagihan ?: 0L)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue   = if (isPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "cardScale"
    )

    val primary = MaterialTheme.colorScheme.primary
    val error   = MaterialTheme.colorScheme.error
    val isDark  = isAppInDarkTheme()

    // Left accent color
    val accentColor = when {
        isLunas  -> primary
        isCicilan -> MaterialTheme.colorScheme.secondary
        overdue  -> error
        urgent   -> Color(0xFFF9A825)     // amber warning
        else     -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.58f else 0.82f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        border    = BorderStroke(1.dp, primary.copy(alpha = if (isDark) 0.34f else 0.20f)),
        shape     = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

                // ── Top row: Icon + Name + Status Chip ─────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        listOf(accentColor.copy(alpha = 0.22f), accentColor.copy(alpha = 0.05f))
                                    ),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = accentColor.copy(alpha = 0.28f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector  = when {
                                    isLunas -> Icons.Default.CheckCircle
                                    isCicilan -> Icons.Outlined.PendingActions
                                    overdue -> Icons.Default.Warning
                                    else    -> Icons.Outlined.Receipt
                                },
                                contentDescription = null,
                                tint         = accentColor,
                                modifier     = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text     = (tagihan.refJenisPembayaran?.namaPembayaran
                                    ?: tagihan.deskripsiTagihan).uppercase(),
                                style    = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight    = FontWeight.ExtraBold,
                                    letterSpacing = 0.3.sp,
                                    color         = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text  = tagihan.deskripsiTagihan,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Normal
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Status badge
                    StatusBadge(isLunas = isLunas, isCicilan = isCicilan, overdue = overdue, urgent = urgent)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 0.5.dp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // ── Bottom row: Financial details ───────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Bottom
                ) {
                    // Left: Jatuh tempo
                    Column {
                        Text(
                            text  = "Jatuh Tempo",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (overdue) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint     = error,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text  = formatDate(tagihan.tanggalJatuhTempo),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color      = if (overdue) error
                                                 else if (urgent) Color(0xFFF9A825)
                                                 else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        if (overdue) {
                            Text(
                                text  = "Melewati Batas",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color    = error,
                                    fontSize = 8.sp
                                )
                            )
                        } else if (urgent) {
                            Text(
                                text  = "Segera Bayar",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color    = Color(0xFFF9A825),
                                    fontSize = 8.sp
                                )
                            )
                        }
                    }

                    // Right: Nominal & sisa
                    Column(horizontalAlignment = Alignment.End) {
                        if (!isLunas && terbayar > 0) {
                            Text(
                                text  = "Dibayar: ${formatRupiah(terbayar)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize   = 9.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }
                        if (isCicilan && lastPayment != null) {
                            Text(
                                text  = "Terakhir: ${formatPaymentDate(lastPayment.paidAt?.toString() ?: lastPayment.createdAt?.toString())}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color      = MaterialTheme.colorScheme.secondary,
                                    fontSize   = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Text(
                            text  = formatRupiah(if (isLunas) tagihan.nominalTagihan else tagihan.sisaTagihan),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color      = if (isLunas) primary
                                             else if (overdue) error
                                             else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text  = if (isLunas) "Total Tagihan" else "Sisa Tagihan",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                        )
                    }
                }

                // ── Mini progress bar (only for partial payment) ─────────────
                if (!isLunas && terbayar > 0 && (tagihan.nominalTagihan ?: 0L) > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val progress = (terbayar.toFloat() / (tagihan.nominalTagihan ?: 0L).toFloat()).coerceIn(0f, 1f)
                    val animProgress by animateFloatAsState(
                        targetValue   = progress,
                        animationSpec = tween(800, easing = FastOutSlowInEasing),
                        label         = "itemProgress"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animProgress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                }

                // ── Tap hint ───────────────────────────────────────────────
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = when {
                            isLunas -> "Lihat Rincian"
                            isCicilan -> "Lanjutkan Cicilan"
                            else -> "Bayar Sekarang"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color      = if (isLunas) MaterialTheme.colorScheme.onSurfaceVariant
                                         else primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector  = Icons.Default.ChevronRight,
                        contentDescription = null,
                    tint         = primary,
                    modifier     = Modifier.size(14.dp)
                )
            }
        }
}
}

@Composable
private fun StatusBadge(isLunas: Boolean, isCicilan: Boolean = false, overdue: Boolean, urgent: Boolean) {
    val (label, bgColor, textColor) = when {
        isLunas -> Triple("LUNAS",       MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), MaterialTheme.colorScheme.primary)
        isCicilan -> Triple("CICILAN",   MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), MaterialTheme.colorScheme.secondary)
        overdue -> Triple("TERLAMBAT",   MaterialTheme.colorScheme.error.copy(alpha = 0.12f),   MaterialTheme.colorScheme.error)
        urgent  -> Triple("SEGERA",      Color(0xFFF9A825).copy(alpha = 0.15f),                 Color(0xFFF9A825))
        else    -> Triple("BELUM LUNAS", MaterialTheme.colorScheme.surfaceVariant,              MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelSmall.copy(
                color         = textColor,
                fontWeight    = FontWeight.Black,
                letterSpacing = 0.5.sp,
                fontSize      = 8.sp
            ),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TagihanDetailSheet — Complete Financial Detail
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TagihanDetailSheet(
    tagihan: TagihanWithDetail,
    pembayaranState: PembayaranTagihanUiState,
    onBayarClick: (TagihanWithDetail) -> Unit
) {
    val isLunas  = tagihan.status == TagihanStatus.LUNAS
    val isCicilan = tagihan.status == TagihanStatus.CICILAN
    var showComingSoonDialog by remember { mutableStateOf(false) }
    val terbayar = (tagihan.nominalTagihan ?: 0L) - (tagihan.sisaTagihan ?: 0L)
    val progress = if ((tagihan.nominalTagihan ?: 0L) > 0)
        (terbayar.toFloat() / (tagihan.nominalTagihan ?: 0L).toFloat()).coerceIn(0f, 1f)
    else 0f
    val animProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label         = "sheetProgress"
    )
    val overdue = !isLunas && isOverdue(tagihan.tanggalJatuhTempo)
    val primary = MaterialTheme.colorScheme.primary
    val error   = MaterialTheme.colorScheme.error

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {

        // ── Sheet Handle (already provided by ModalBottomSheet) ─────────────
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(primary, primary.copy(alpha = 0.25f))
                                ),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text  = "RINCIAN TAGIHAN",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color         = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = tagihan.refJenisPembayaran?.namaPembayaran ?: tagihan.deskripsiTagihan,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.padding(start = 13.dp)
                )
            }
            StatusBadge(isLunas = isLunas, isCicilan = isCicilan, overdue = overdue, urgent = false)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Amount Breakdown Card ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    RoundedCornerShape(16.dp)
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Total tagihan
                SheetDetailRow(
                    label = "Total Tagihan",
                    value = formatRupiah(tagihan.nominalTagihan),
                    icon  = Icons.Outlined.Receipt
                )

                if (terbayar > 0) {
                    SheetDetailRow(
                        label     = "Sudah Dibayar",
                        value     = formatRupiah(terbayar),
                        icon      = Icons.Outlined.CheckCircle,
                        valueColor = primary
                    )
                }

                Divider(
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                    thickness = 0.5.dp
                )

                // Sisa / Lunas — most prominent
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = if (isLunas) "Status Pembayaran" else "Sisa yang Harus Dibayar",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text  = if (isLunas) "✓ LUNAS" else formatRupiah(tagihan.sisaTagihan),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color      = if (isLunas) primary
                                         else if (overdue) error
                                         else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                // Progress bar
                if ((tagihan.nominalTagihan ?: 0L) > 0) {
                    Column {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text  = "Progres Pembayaran",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text  = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color      = primary,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animProgress)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.secondary,
                                                primary
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PaymentHistorySection(tagihan = tagihan, pembayaranState = pembayaranState)

        Spacer(modifier = Modifier.height(16.dp))

        // ── Info Detail Card ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                    RoundedCornerShape(16.dp)
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text  = "INFORMASI TAGIHAN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color         = MaterialTheme.colorScheme.primary,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                )
                Divider(
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.5.dp
                )
                SheetDetailRow(
                    label = "Deskripsi",
                    value = tagihan.deskripsiTagihan,
                    icon  = Icons.Outlined.Description
                )
                SheetDetailRow(
                    label = "Jatuh Tempo",
                    value = formatDate(tagihan.tanggalJatuhTempo),
                    icon  = Icons.Outlined.CalendarToday,
                    valueColor = if (overdue) error else MaterialTheme.colorScheme.onSurface
                )
                if (!tagihan.midtransOrderId.isNullOrBlank()) {
                    SheetDetailRow(
                        label = "Order ID",
                        value = tagihan.midtransOrderId,
                        icon  = Icons.Outlined.Tag
                    )
                }
                if (overdue) {
                    Divider(
                        color     = error.copy(alpha = 0.25f),
                        thickness = 0.5.dp
                    )
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(error.copy(alpha = 0.08f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint     = error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text  = "Tagihan ini telah melewati tanggal jatuh tempo. Segera lakukan pembayaran untuk menghindari denda.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color      = error,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── CTA Button ───────────────────────────────────────────────────────
        if (isLunas) {
            // Lunas — read-only confirmation state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(primary.copy(alpha = 0.12f), primary.copy(alpha = 0.06f))
                        )
                    )
                    .border(1.dp, primary.copy(alpha = 0.30f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint     = primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text  = "TAGIHAN INI TELAH DILUNASI",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color         = primary,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        } else {
            // Belum Lunas — payment button with gradient
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val pressScale by animateFloatAsState(
                targetValue   = if (isPressed) 0.97f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                label         = "btnScale"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(pressScale)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                primary,
                                primary.copy(green = (primary.green + 0.08f).coerceAtMost(1f))
                            )
                        )
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication        = null
                    ) { showComingSoonDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Payment,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                            text  = if (isCicilan) "LANJUTKAN CICILAN" else "PROSES PEMBAYARAN",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color         = MaterialTheme.colorScheme.onPrimary,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text      = "Pembayaran diproses melalui Midtrans dengan enkripsi SSL.",
                style     = MaterialTheme.typography.labelSmall.copy(
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 10.sp
                ),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        }
    }

    if (showComingSoonDialog) {
        ComingSoonDialog(
            title = "Pembayaran Tagihan",
            onDismiss = { showComingSoonDialog = false }
        )
    }
}

@Composable
private fun PaymentHistorySection(
    tagihan: TagihanWithDetail,
    pembayaranState: PembayaranTagihanUiState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                RoundedCornerShape(16.dp)
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text  = "RIWAYAT CICILAN",
                style = MaterialTheme.typography.labelSmall.copy(
                    color         = MaterialTheme.colorScheme.primary,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            )
            Divider(
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.5.dp
            )
            when (pembayaranState) {
                PembayaranTagihanUiState.Idle,
                PembayaranTagihanUiState.Loading -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Memuat riwayat pembayaran",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                is PembayaranTagihanUiState.Error -> Text(
                    text = pembayaranState.message,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.error,
                        lineHeight = 16.sp
                    )
                )
                is PembayaranTagihanUiState.Success -> {
                    val postedPayments = pembayaranState.items
                        .filter { it.status.equals("posted", ignoreCase = true) }
                        .sortedBy { it.paidAt ?: it.createdAt }
                    val remainingAfterById = mutableMapOf<String, Long>()
                    var remaining = tagihan.nominalTagihan ?: 0L
                    postedPayments.forEach { payment ->
                        remaining = (remaining - payment.amount).coerceAtLeast(0L)
                        remainingAfterById[payment.id] = remaining
                    }
                    if (pembayaranState.items.isEmpty()) {
                        Text(
                            text = "Belum ada pembayaran yang tercatat.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    } else {
                        pembayaranState.items.take(5).forEach { payment ->
                            PaymentHistoryRow(
                                payment = payment,
                                remainingAfter = remainingAfterById[payment.id],
                                showTagihanLabel = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryRow(
    payment: PembayaranTagihanDto,
    remainingAfter: Long? = null,
    tagihanLabel: String? = null,
    showTagihanLabel: Boolean = true
) {
    val isPosted = payment.status.equals("posted", ignoreCase = true)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPosted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPosted) Icons.Outlined.CheckCircle else Icons.Outlined.PendingActions,
                    contentDescription = null,
                    tint = if (isPosted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                if (showTagihanLabel && !tagihanLabel.isNullOrBlank()) {
                    Text(
                        text = tagihanLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Text(
                    text = "${payment.metodePembayaran.uppercase()} • ${payment.status.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPosted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = formatPaymentDate(payment.paidAt?.toString() ?: payment.createdAt?.toString()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
                if (remainingAfter != null) {
                    Text(
                        text = "Sisa setelah pembayaran: ${formatRupiah(remainingAfter)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                if (!payment.providerOrderId.isNullOrBlank()) {
                    Text(
                        text = payment.providerOrderId,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    )
                }
                if (!payment.keterangan.isNullOrBlank()) {
                    Text(
                        text = payment.keterangan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
        Text(
            text = formatRupiah(payment.amount),
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (isPosted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black
            )
        )
    }
}

@Composable
private fun GlobalPaymentHistorySection(
    riwayatState: RiwayatPembayaranUiState,
    tagihanById: Map<String, TagihanWithDetail>,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(RiwayatPembayaranFilter.SEMUA) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(18.dp)
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(16.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    )
                                ),
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "RIWAYAT PEMBAYARAN",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                val total = (riwayatState as? RiwayatPembayaranUiState.Success)?.items?.size ?: 0
                if (total > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = "$total transaksi",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            when (riwayatState) {
                RiwayatPembayaranUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Memuat riwayat pembayaran",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                is RiwayatPembayaranUiState.Error -> Text(
                    text = riwayatState.message,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.error,
                        lineHeight = 16.sp
                    )
                )
                is RiwayatPembayaranUiState.Success -> {
                    val filteredPayments = riwayatState.items.filter { payment ->
                        val tagihan = tagihanById[payment.tagihanId]
                        payment.matchesRiwayatFilter(activeFilter, tagihan)
                    }
                    CompactHistoryFilterRow(
                        activeFilter = activeFilter,
                        onFilterChange = { activeFilter = it }
                    )
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                        thickness = 0.5.dp
                    )
                    if (filteredPayments.isEmpty()) {
                        Text(
                            text = "Belum ada transaksi pada filter ini.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    } else {
                        filteredPayments.take(8).forEach { payment ->
                            val tagihan = tagihanById[payment.tagihanId]
                            val label = tagihan?.refJenisPembayaran?.namaPembayaran
                                ?: tagihan?.deskripsiTagihan
                                ?: "Tagihan"
                            PaymentHistoryRow(
                                payment = payment,
                                tagihanLabel = label,
                                showTagihanLabel = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactHistoryFilterRow(
    activeFilter: RiwayatPembayaranFilter,
    onFilterChange: (RiwayatPembayaranFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RiwayatPembayaranFilter.entries.chunked(3).forEach { rowFilters ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowFilters.forEach { filter ->
                    val selected = filter == activeFilter
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clickable { onFilterChange(filter) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = filter.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun PembayaranTagihanDto.matchesRiwayatFilter(
    filter: RiwayatPembayaranFilter,
    tagihan: TagihanWithDetail?
): Boolean {
    val posted = status.equals("posted", ignoreCase = true)
    val failed = status.equals("failed", ignoreCase = true) ||
        status.equals("cancelled", ignoreCase = true)
    val pending = status.equals("pending", ignoreCase = true)
    val isPelunasan = posted && tagihan?.status == TagihanStatus.LUNAS
    val isCicilan = posted && tagihan?.status == TagihanStatus.CICILAN

    return when (filter) {
        RiwayatPembayaranFilter.SEMUA -> true
        RiwayatPembayaranFilter.BERHASIL -> posted
        RiwayatPembayaranFilter.MENUNGGU -> pending
        RiwayatPembayaranFilter.GAGAL -> failed
        RiwayatPembayaranFilter.CICILAN -> isCicilan
        RiwayatPembayaranFilter.PELUNASAN -> isPelunasan
    }
}

@Composable
private fun PaymentAmountDialog(
    tagihan: TagihanWithDetail,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val remaining = tagihan.sisaTagihan ?: 0L
    var amountText by remember(tagihan.id) { mutableStateOf(remaining.toString()) }
    var payFull by remember(tagihan.id) { mutableStateOf(true) }
    val parsedAmount = amountText.filter(Char::isDigit).toLongOrNull() ?: 0L
    val isOverLimit = parsedAmount > remaining
    val isValid = parsedAmount in 1..remaining

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Nominal Pembayaran",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                SheetDetailRow(
                    label = "Sisa Tagihan",
                    value = formatRupiah(remaining),
                    icon = Icons.Outlined.Receipt,
                    valueColor = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AmountModeButton(
                        label = "Penuh",
                        selected = payFull,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            payFull = true
                            amountText = remaining.toString()
                        }
                    )
                    AmountModeButton(
                        label = "Cicilan",
                        selected = !payFull,
                        modifier = Modifier.weight(1f),
                        onClick = { payFull = false }
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { value ->
                        payFull = false
                        amountText = value.filter(Char::isDigit).take(12)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !payFull,
                    singleLine = true,
                    label = { Text("Nominal cicilan") },
                    prefix = { Text("Rp") },
                    supportingText = {
                        Text(
                            text = if (parsedAmount > remaining) {
                                "Nominal melebihi sisa tagihan yang harus dibayar."
                            } else {
                                "Minimal Rp1 dan maksimal ${formatRupiah(remaining)}."
                            }
                        )
                    },
                    isError = !isValid || isOverLimit,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = { onConfirm(parsedAmount) },
                        modifier = Modifier.weight(1f),
                        enabled = isValid
                    ) {
                        Text(if (parsedAmount < remaining) "Cicil" else "Bayar")
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountModeButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

private fun formatPaymentDate(value: String?): String =
    value?.substringBefore('T')?.takeIf { it.isNotBlank() } ?: "-"

@Composable
private fun SheetDetailRow(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.weight(0.45f)
        ) {
            Icon(
                imageVector  = icon,
                contentDescription = null,
                tint         = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier     = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text  = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            )
        }
        Text(
            text      = value,
            style     = MaterialTheme.typography.bodySmall.copy(
                color      = valueColor,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.End,
            modifier  = Modifier.weight(0.55f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty & Error States
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FinanceEmptyState(filter: TagihanFilter) {
    Column(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        Icon(
            imageVector  = if (filter == TagihanFilter.LUNAS) Icons.Default.CheckCircle
                           else Icons.Outlined.Receipt,
            contentDescription = null,
            tint         = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            modifier     = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text      = if (filter == TagihanFilter.LUNAS)
                "Belum ada tagihan yang dilunasi"
            else
                "Semua tagihan telah terbayar",
            style     = MaterialTheme.typography.bodyMedium.copy(
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text      = "Tidak ada data untuk kategori \"${filter.label}\".",
            style     = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FinanceErrorState(message: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text  = message,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.error
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SuccessPaymentDialog — Premium Animated
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SuccessPaymentDialog(
    title: String = "Pembayaran Berhasil!",
    message: String = "Terima kasih. Pembayaran Anda telah berhasil diproses dan tercatat dalam sistem Al-Hasanah Media.",
    buttonText: String = "MENGERTI",
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    // Entry animation
    val scale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "dialogScale"
    )

    // Pulse ring animation
    val pulseTrans = rememberInfiniteTransition(label = "successPulse")
    val pulseAlpha by pulseTrans.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
        label         = "pulseAlpha"
    )
    val pulseRadius by pulseTrans.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1.2f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
        label         = "pulseRadius"
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .scale(scale)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    primary.copy(alpha = 0.15f),
                    RoundedCornerShape(32.dp)
                )
        ) {
            // Decorative Background
            Canvas(modifier = Modifier.matchParentSize().alpha(0.05f)) {
                drawCircle(
                    color = primary,
                    radius = size.maxDimension / 2f,
                    center = androidx.compose.ui.geometry.Offset(size.width, 0f)
                )
            }

            Column(
                modifier            = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Premium Success Icon ────────────────────────────────────
                Box(
                    modifier         = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated pulse rings
                    repeat(2) { index ->
                        val delay = index * 300
                        val alpha by pulseTrans.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, delayMillis = delay),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "ringAlpha$index"
                        )
                        val radius by pulseTrans.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, delayMillis = delay),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "ringRadius$index"
                        )
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = primary.copy(alpha = alpha),
                                radius = (size.minDimension / 2f) * radius,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }

                    // Inner Glow
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    0.0f to primary.copy(alpha = 0.2f),
                                    1.0f to Color.Transparent
                                ),
                                shape = CircleShape
                            )
                    )

                    // Main Icon Circle
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = primary,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Al-Hasanah Media • Terpercaya",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
