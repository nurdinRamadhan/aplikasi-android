package com.alhasanah.alhasanahmedia.ui.donasi

// ─────────────────────────────────────────────────────────────────────────────
// Imports
// ─────────────────────────────────────────────────────────────────────────────

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.CorePaymentData
import com.alhasanah.alhasanahmedia.data.model.CorePaymentMethod
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import com.alhasanah.alhasanahmedia.ui.components.ComingSoonDialog
import com.alhasanah.alhasanahmedia.ui.keuangan.KeuanganViewModel
import com.alhasanah.alhasanahmedia.ui.keuangan.SantriInfoState
import com.alhasanah.alhasanahmedia.ui.keuangan.TagihanUiState
import com.alhasanah.alhasanahmedia.ui.payment.PaymentInstructionData
import com.alhasanah.alhasanahmedia.ui.payment.PaymentMethodPickerDialog
import com.alhasanah.alhasanahmedia.ui.theme.AmiriFontFamily
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Gold Brand Palette — Fixed, consistent with all screens
// ─────────────────────────────────────────────────────────────────────────────

private val GoldPrimary = Color(0xFFD4A017)
private val GoldLight   = Color(0xFFE8C55A)
private val GoldDeep    = Color(0xFFAA7C1F)
private val GoldShimmer = Color(0xFFFAF0C0)
private val WarmIvory   = Color(0xFFFFFCF7)
private val WarmParchment = Color(0xFFFBF3E6)
private val WarmInk     = Color(0xFF2A2318)
private val DarkInk     = Color(0xFF0D171B)
private val DarkSurface = Color(0xFF111D21)

private val GoldGradientBrush = Brush.linearGradient(
    colors = listOf(GoldDeep, GoldPrimary, GoldLight, GoldPrimary, GoldDeep)
)

// ─────────────────────────────────────────────────────────────────────────────
// Donation Category Model
// ─────────────────────────────────────────────────────────────────────────────

private data class DonasiCategory(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val description: String,
    val arabicText: String
)

private val donasiCategories = listOf(
    DonasiCategory(
        id          = "Infaq",
        label       = "Infaq",
        icon        = Icons.Outlined.Favorite,
        description = "Harta di jalan Allah",
        arabicText  = "إِنْفَاق"
    ),
    DonasiCategory(
        id          = "Wakaf",
        label       = "Wakaf",
        icon        = Icons.Outlined.AccountBalance,
        description = "Amal jariyah abadi",
        arabicText  = "وَقْف"
    ),
    DonasiCategory(
        id          = "Shadaqah",
        label       = "Shadaqah",
        icon        = Icons.Outlined.VolunteerActivism,
        description = "Pemberian ikhlas",
        arabicText  = "صَدَقَة"
    )
)

// Quick nominal presets
private val nominalPresets = listOf(
    25_000L   to "25K",
    50_000L   to "50K",
    100_000L  to "100K",
    250_000L  to "250K",
    500_000L  to "500K",
    1_000_000L to "1 Jt"
)

// ─────────────────────────────────────────────────────────────────────────────
// DonasiScreen — Root (logika ViewModel tidak berubah)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DonasiScreen(
    santriNis: String,
    onBack: () -> Unit,
    navController: NavController,
    viewModel: KeuanganViewModel = koinViewModel { parametersOf(santriNis) }
) {
    val santriInfoState by viewModel.santriInfoState.collectAsState()
    val tagihanState    by viewModel.tagihanState.collectAsState()
    val context         = LocalContext.current
    var selectedPaymentMethod by remember { mutableStateOf(CorePaymentMethod.QRIS) }

    LaunchedEffect(tagihanState) {
        if (tagihanState is TagihanUiState.Error) {
            Toast.makeText(
                context,
                (tagihanState as TagihanUiState.Error).message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.launchCorePayment.collect { payment ->
            navController.navigate(Screen.PaymentInstruction.createRoute(payment.toInstructionData()))
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppSolidBackground(isDark = isAppInDarkTheme())

        when (val state = tagihanState) {
            is TagihanUiState.Loading -> DonasiLoadingState()
            is TagihanUiState.Error   -> DonasiErrorState(
                message = state.message,
                onRetry = { viewModel.refreshData() }
            )
            else -> {
                val defaultName = (santriInfoState as? SantriInfoState.Success)
                    ?.santriInfo?.nama ?: ""
                DonasiContent(
                    defaultDonorName = defaultName,
                    selectedPaymentMethod = selectedPaymentMethod,
                    onPaymentMethodSelected = { selectedPaymentMethod = it },
                    onBack           = onBack,
                    onBayarClick     = { nominal, jenis, nama, pesan ->
                        viewModel.bayarDonasi(nominal, jenis, nama, pesan, selectedPaymentMethod)
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Background — Islamic Star Pattern
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DonasiBackground() {
    val primary = MaterialTheme.colorScheme.primary
    val isDark  = isAppInDarkTheme()
    val infiniteTransition = rememberInfiniteTransition(label = "donasiBg")
    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(140_000, easing = LinearEasing)),
        label         = "donasiBgRot"
    )
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isDark) {
                        listOf(Color(0xFF0B1519), Color(0xFF101C20), Color(0xFF0C1417))
                    } else {
                        listOf(WarmIvory, WarmParchment, MaterialTheme.colorScheme.background)
                    }
                )
            )
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = if (isDark) 0.12f else 0.10f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.18f, size.height * 0.08f),
                radius = size.width * 0.70f
            ),
            radius = size.width * 0.70f,
            center = Offset(size.width * 0.18f, size.height * 0.08f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDark) 0.05f else 0.20f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.86f, size.height * 0.26f),
                radius = size.width * 0.56f
            ),
            radius = size.width * 0.56f,
            center = Offset(size.width * 0.86f, size.height * 0.26f)
        )
        val spacing = 96.dp.toPx()
        val starR   = 14.dp.toPx()
        val cols    = (size.width  / spacing).toInt() + 2
        val rows    = (size.height / spacing).toInt() + 2
        val c       = primary.copy(alpha = if (isDark) 0.028f else 0.020f)
        for (col in -1..cols) {
            for (row in -1..rows) {
                val stagger = if (col % 2 == 0) spacing / 2f else 0f
                val center  = Offset(col * spacing, row * spacing + stagger)
                val localR  = if ((col + row) % 2 == 0) rotation else -rotation
                rotate(degrees = localR, pivot = center) {
                    val path  = Path()
                    val inner = starR * 0.55f
                    for (i in 0 until 16) {
                        val r   = if (i % 2 == 0) starR else inner
                        val ang = (i * PI / 8 - PI / 2).toFloat()
                        val px  = center.x + r * cos(ang.toDouble()).toFloat()
                        val py  = center.y + r * sin(ang.toDouble()).toFloat()
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    drawPath(path, c)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DonasiContent — Main scrollable content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DonasiContent(
    defaultDonorName: String,
    selectedPaymentMethod: CorePaymentMethod,
    onPaymentMethodSelected: (CorePaymentMethod) -> Unit,
    onBack: () -> Unit,
    onBayarClick: (Long, String, String, String) -> Unit
) {
    var nominal      by remember { mutableStateOf("") }
    var jenis        by remember { mutableStateOf("Infaq") }
    var namaDonatur  by remember { mutableStateOf(defaultDonorName) }
    var pesan        by remember { mutableStateOf("") }
    var isAnonymous  by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf<Long?>(null) }
    var showPaymentMethodDialog by remember { mutableStateOf(false) }
    var showComingSoonDialog by remember { mutableStateOf(false) }

    val isDark   = isAppInDarkTheme()
    val primary  = MaterialTheme.colorScheme.primary
    val scrollState = rememberScrollState()

    // Sync nominal when anonymous toggled
    LaunchedEffect(isAnonymous) {
        if (isAnonymous) namaDonatur = "Hamba Allah"
    }

    // Compute impact message from nominal
    val impact = remember(nominal) {
        val n = nominal.toLongOrNull() ?: 0L
        when {
            n >= 1_000_000 -> "≈ 1 bulan beasiswa santri"
            n >= 500_000   -> "≈ 2 minggu kebutuhan santri"
            n >= 250_000   -> "≈ 1 minggu kebutuhan santri"
            n >= 100_000   -> "Membantu operasional harian"
            n >= 50_000    -> "Kontribusi buku & alat tulis"
            n >= 25_000    -> "Secangkir semangat untuk santri"
            n > 0          -> "Setiap rupiah bernilai ibadah"
            else           -> null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {

        // ── 1. Gradient Header + Hero Card ────────────────────────────────
        DonasiGradientHeader(onBack = onBack)

        // ── 2. Form Content ───────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Spacer(modifier = Modifier.height(4.dp))

            // ── Section: Kategori ────────────────────────────────────────
            DonasiSectionLabel(
                text     = "PILIH KATEGORI DONASI",
                subtitle = "Setiap kategori memiliki keutamaan tersendiri"
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                donasiCategories.forEach { category ->
                    DonasiCategoryCard(
                        category   = category,
                        isSelected = jenis == category.id,
                        isDark     = isDark,
                        modifier   = Modifier.weight(1f),
                        onClick    = { jenis = category.id }
                    )
                }
            }

            // ── Section: Nominal ─────────────────────────────────────────
            DonasiSectionLabel(
                text     = "NOMINAL DONASI",
                subtitle = "Pilih nominal atau masukkan sendiri"
            )

            // Quick amount grid
            DonasiQuickAmounts(
                selectedPreset = selectedPreset,
                isDark         = isDark,
                onSelect       = { amount ->
                    selectedPreset = amount
                    nominal        = amount.toString()
                }
            )

            // Manual input field
            OutlinedTextField(
                value         = nominal,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) {
                        nominal        = it
                        selectedPreset = it.toLongOrNull()
                    }
                },
                modifier      = Modifier.fillMaxWidth(),
                label         = { Text("Atau masukkan nominal lain") },
                prefix        = {
                    Text(
                        "Rp  ",
                        fontWeight = FontWeight.ExtraBold,
                        color      = if (isDark) GoldLight else GoldDeep
                    )
                },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape         = RoundedCornerShape(14.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = primary,
                    unfocusedBorderColor = primary.copy(alpha = 0.24f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.28f else 0.60f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.18f else 0.54f),
                    cursorColor          = primary
                )
            )

            // Impact Statement — appears when nominal > 0
            if (impact != null) {
                DonasiImpactBanner(text = impact, isDark = isDark)
            }

            // ── Section: Identitas ───────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                DonasiSectionLabel(
                    text     = "IDENTITAS DONATUR",
                    subtitle = "Opsional — bisa anonim",
                    modifier = Modifier.weight(1f)
                )
                // Anonymous toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            if (isAnonymous) primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                        )
                        .border(
                            0.5.dp,
                            if (isAnonymous) primary.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            RoundedCornerShape(50.dp)
                        )
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { isAnonymous = !isAnonymous }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector        = if (isAnonymous) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint               = if (isAnonymous) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text  = if (isAnonymous) "Anonim" else "Tampilkan",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color      = if (isAnonymous) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 10.sp
                        )
                    )
                }
            }

            OutlinedTextField(
                value         = if (isAnonymous) "Hamba Allah" else namaDonatur,
                onValueChange = { if (!isAnonymous) namaDonatur = it },
                modifier      = Modifier.fillMaxWidth(),
                label         = { Text("Nama Donatur") },
                singleLine    = true,
                enabled       = !isAnonymous,
                leadingIcon   = {
                    Icon(
                        if (isAnonymous) Icons.Default.VisibilityOff else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isAnonymous) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                               else primary
                    )
                },
                shape         = RoundedCornerShape(14.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = primary,
                    unfocusedBorderColor = primary.copy(alpha = 0.24f),
                    disabledBorderColor  = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                    disabledTextColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.28f else 0.60f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.18f else 0.54f),
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.12f else 0.38f),
                    cursorColor          = primary
                )
            )

            // ── Section: Pesan / Doa ──────────────────────────────────────
            DonasiSectionLabel(
                text     = "PESAN & DOA",
                subtitle = "Titipkan harapan Anda bersama donasi"
            )

            OutlinedTextField(
                value         = pesan,
                onValueChange = { pesan = it },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = {
                    Text(
                        "Contoh: Semoga Allah melipatgandakan kebaikan pesantren ini…",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            fontStyle = FontStyle.Italic
                        )
                    )
                },
                minLines      = 3,
                maxLines      = 5,
                leadingIcon   = {
                    Icon(
                        Icons.Outlined.FormatQuote,
                        contentDescription = null,
                        tint = primary
                    )
                },
                shape         = RoundedCornerShape(14.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = primary,
                    unfocusedBorderColor = primary.copy(alpha = 0.24f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.28f else 0.60f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.18f else 0.54f),
                    cursorColor          = primary
                )
            )

            // ── Trust signals block ──────────────────────────────────────
            DonasiTrustBlock(isDark = isDark)

            Spacer(modifier = Modifier.height(4.dp))

            // ── CTA Button ────────────────────────────────────────────────
            DonasiCtaButton(
                enabled   = nominal.isNotBlank() &&
                            (nominal.toLongOrNull() ?: 0L) > 0L &&
                            (namaDonatur.isNotBlank() || isAnonymous),
                jenis     = jenis,
                nominal   = nominal,
                isDark    = isDark,
                onClick   = {
                    showComingSoonDialog = true
                }
            )

            Spacer(modifier = Modifier.height(128.dp))
        }
    }

    if (showPaymentMethodDialog) {
        PaymentMethodPickerDialog(
            selected = selectedPaymentMethod,
            onSelected = onPaymentMethodSelected,
            onDismiss = { showPaymentMethodDialog = false },
            onConfirm = {
                showPaymentMethodDialog = false
                val amount = nominal.toLongOrNull() ?: 0L
                val donorName = if (isAnonymous) "Hamba Allah" else namaDonatur
                onBayarClick(amount, jenis, donorName, pesan)
            },
            title = "Pilih Pembayaran Infaq",
            confirmText = "Lanjutkan Donasi",
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

    if (showComingSoonDialog) {
        ComingSoonDialog(
            title = "Pembayaran Donasi",
            onDismiss = { showComingSoonDialog = false }
        )
    }
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
        message = "Donasi menunggu pembayaran. Bukti penerimaan akan tercatat setelah Midtrans mengirim konfirmasi."
    )

// ─────────────────────────────────────────────────────────────────────────────
// DonasiGradientHeader — Unified with all app screens
// Contains: TopBar + Dark Mushaf-style Hero Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DonasiGradientHeader(onBack: () -> Unit) {
    val isDark  = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val titleColor = if (isDark) GoldLight else GoldDeep
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.72f) else WarmInk.copy(alpha = 0.68f)
    val buttonBg = if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.62f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(458.dp)
    ) {
        AppPageHeaderBackground(isDark = isDark, modifier = Modifier.matchParentSize())

        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── TopBar Row ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 26.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .border(
                            1.dp,
                            primary.copy(alpha = 0.42f),
                            CircleShape
                        )
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onBack() }
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint               = if (isDark) Color.White.copy(alpha = 0.86f) else WarmInk,
                        modifier           = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier            = Modifier
                        .align(Alignment.Center)
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text          = "DONASI & INFAQ",
                        style         = MaterialTheme.typography.titleLarge.copy(
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 1.4.sp,
                            color         = titleColor
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text  = "Al-Hasanah Media",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color      = subtitleColor,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }

            // ── Hero Islamic Card ──────────────────────────────────────────
            DonasiHeroCard(isDark = isDark)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DonasiHeroCard — Dark Mushaf-style emotional anchor
// Always dark in both modes — same philosophy as SurahHeaderCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DonasiHeroCard(isDark: Boolean) {
    val bgBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF111E23), Color(0xFF0D171B)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.82f), WarmIvory.copy(alpha = 0.94f)))
    }
    val arabicColor = if (isDark) GoldLight else GoldDeep
    val bodyColor = if (isDark) Color.White.copy(alpha = 0.70f) else WarmInk.copy(alpha = 0.68f)
    val sourceColor = if (isDark) GoldLight.copy(alpha = 0.78f) else GoldDeep.copy(alpha = 0.82f)

    // Shimmer animation
    val infiniteTransition = rememberInfiniteTransition(label = "heroShimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue  = -600f,
        targetValue   = 800f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label         = "heroShimmerX"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        GoldPrimary.copy(alpha = if (isDark) 0.52f else 0.42f),
                        GoldLight.copy(alpha = if (isDark) 0.14f else 0.18f),
                        GoldPrimary.copy(alpha = if (isDark) 0.52f else 0.42f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .drawBehind {
                // Gold top line
                drawLine(
                    brush       = GoldGradientBrush,
                    start       = Offset(20f, 0f),
                    end         = Offset(size.width - 20f, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                // Gold bottom line
                drawLine(
                    brush       = GoldGradientBrush,
                    start       = Offset(48f, size.height),
                    end         = Offset(size.width - 48f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                // Radial center glow
                drawCircle(
                    brush  = Brush.radialGradient(
                        colors = listOf(GoldPrimary.copy(alpha = if (isDark) 0.12f else 0.06f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
                // Shimmer sweep
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerX, 0f),
                        end   = Offset(shimmerX + 200f, size.height)
                    )
                )
                // Concentric rings — top right corner ornament
                for (i in 0..2) {
                    val r     = 14f + i * 12f
                    val alpha = (0.07f - i * 0.02f).coerceAtLeast(0f)
                    drawArc(
                        color      = GoldPrimary.copy(alpha = alpha),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter  = false,
                        topLeft    = Offset(size.width - r * 2 - 12f, 10f),
                        size       = Size(r * 2, r * 2),
                        style      = Stroke(width = 0.7f)
                    )
                }
            }
            .padding(horizontal = 22.dp, vertical = 22.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arabic verse
            Text(
                text       = "وَمَا تُنفِقُوا مِنْ خَيْرٍ فَلِأَنفُسِكُمْ",
                fontFamily = AmiriFontFamily,
                fontSize   = 22.sp,
                color      = arabicColor,
                textAlign  = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ornamental divider
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(0.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, GoldPrimary.copy(alpha = 0.55f))
                            )
                        )
                )
                Text(
                    "  ✦  ",
                    color    = GoldPrimary.copy(alpha = 0.60f),
                    fontSize = 8.sp
                )
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(0.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(GoldPrimary.copy(alpha = 0.55f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Translation
            Text(
                text      = "\"Dan apa saja kebaikan yang kamu infakkan, maka itu untuk dirimu sendiri.\"",
                fontSize  = 11.sp,
                color     = bodyColor,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text     = "— QS. Al-Baqarah: 272",
                fontSize = 9.sp,
                color    = sourceColor,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DonasiCategoryCard — Rich card with Arabic name + description
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DonasiCategoryCard(
    category: DonasiCategory,
    isSelected: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    val cardBg by animateColorAsState(
        targetValue   = if (isSelected)
                            primary.copy(alpha = if (isDark) 0.18f else 0.10f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.16f else 0.58f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "categoryCardBg"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) primary.copy(alpha = 0.65f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
        label         = "categoryBorder"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "catScale"
    )

    Column(
        modifier = modifier
            .scale(pressScale)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(
                width = if (isSelected) 1.5.dp else 0.8.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null
            ) { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Icon orb
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            primary.copy(alpha = if (isSelected) 0.25f else 0.10f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    0.5.dp,
                    primary.copy(alpha = if (isSelected) 0.40f else 0.18f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = category.icon,
                contentDescription = category.label,
                tint               = primary,
                modifier           = Modifier.size(20.dp)
            )
        }

        // Arabic label
        Text(
            text       = category.arabicText,
            fontFamily = AmiriFontFamily,
            fontSize   = 15.sp,
            color      = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign  = TextAlign.Center
        )

        // Latin label
        Text(
            text      = category.label,
            style     = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                color      = if (isSelected) primary else MaterialTheme.colorScheme.onSurface
            ),
            textAlign = TextAlign.Center
        )

        // Description
        Text(
            text      = category.description,
            style     = MaterialTheme.typography.labelSmall.copy(
                fontSize   = 8.sp,
                color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                fontWeight = FontWeight.Normal
            ),
            textAlign = TextAlign.Center,
            maxLines  = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DonasiQuickAmounts — 6 preset buttons in 3x2 grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DonasiQuickAmounts(
    selectedPreset: Long?,
    isDark: Boolean,
    onSelect: (Long) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        nominalPresets.chunked(3).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (amount, label) ->
                    val isSelected = selectedPreset == amount
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed         by interactionSource.collectIsPressedAsState()
                    val pressScale        by animateFloatAsState(
                        targetValue   = if (isPressed) 0.94f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessHigh),
                        label         = "preset_$amount"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(pressScale)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected)
                                    Brush.linearGradient(listOf(GoldDeep, GoldPrimary))
                                else
                                    Brush.linearGradient(
                                        listOf(
                                            primary.copy(alpha = if (isDark) 0.15f else 0.09f),
                                            primary.copy(alpha = if (isDark) 0.08f else 0.04f)
                                        )
                                    )
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.7.dp,
                                color = if (isSelected) GoldLight.copy(alpha = 0.40f)
                                        else primary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication        = null
                            ) { onSelect(amount) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "Rp $label",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color      = if (isSelected) Color(0xFF12100A)
                                             else if (isDark) GoldLight else GoldDeep,
                                letterSpacing = 0.3.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DonasiImpactBanner — Real-time emotional feedback
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DonasiImpactBanner(text: String, isDark: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        primary.copy(alpha = if (isDark) 0.15f else 0.08f),
                        primary.copy(alpha = if (isDark) 0.08f else 0.04f)
                    )
                )
            )
            .border(
                0.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        primary.copy(alpha = 0.40f),
                        primary.copy(alpha = 0.12f),
                        primary.copy(alpha = 0.40f)
                    )
                ),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint               = GoldPrimary,
            modifier           = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text  = "DAMPAK DONASI ANDA",
                style = MaterialTheme.typography.labelSmall.copy(
                    color         = if (isDark) GoldLight else GoldDeep,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontSize      = 8.sp
                )
            )
            Text(
                text  = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DonasiTrustBlock — Trust signals (transparency, security, legitimacy)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DonasiTrustBlock(isDark: Boolean) {
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.20f else 0.56f))
            .border(
                0.8.dp,
                primary.copy(alpha = if (isDark) 0.26f else 0.22f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(14.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(GoldPrimary, GoldPrimary.copy(alpha = 0.25f))
                            ),
                            RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text  = "KEAMANAN & TRANSPARANSI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color         = if (isDark) GoldLight else GoldDeep,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        fontSize      = 8.sp
                    )
                )
            }

            HorizontalDivider(
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.5.dp
            )

            TrustItem(
                icon  = Icons.Outlined.Shield,
                text  = "Transaksi diproses via Midtrans dan di enkripsi"
            )
            TrustItem(
                icon  = Icons.Outlined.VerifiedUser,
                text  = "Pemberian Anda dicatat permanen di sistem kami"
            )
            TrustItem(
                icon  = Icons.Outlined.Mosque,
                text  = "Dana digunakan untuk operasional & pembangunan pesantren"
            )
        }
    }
}

@Composable
private fun TrustItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            modifier           = Modifier.size(14.dp).padding(top = 1.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text  = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                fontSize   = 10.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DonasiCtaButton — Gold gradient, shows jenis + nominal in button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DonasiCtaButton(
    enabled: Boolean,
    jenis: String,
    nominal: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "ctaScale"
    )

    val formattedNominal = remember(nominal) {
        val n = nominal.toLongOrNull() ?: 0L
        when {
            n >= 1_000_000 -> "Rp ${n / 1_000_000} Jt"
            n >= 1_000     -> "Rp ${n / 1_000}K"
            else           -> "Rp $n"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .scale(pressScale)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (enabled)
                        Brush.horizontalGradient(listOf(GoldDeep, GoldPrimary, GoldLight))
                    else
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                )
                .border(
                    width = 1.dp,
                    color = if (enabled) GoldLight.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(
                    enabled           = enabled,
                    interactionSource = interactionSource,
                    indication        = null
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (enabled) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Favorite,
                        contentDescription = null,
                        tint               = Color(0xFF12100A),
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = "DONASI $jenis SEKARANG",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color         = Color(0xFF12100A),
                                fontWeight    = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                        if (nominal.isNotBlank() && (nominal.toLongOrNull() ?: 0L) > 0L) {
                            Text(
                                text  = formattedNominal,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color      = Color(0xFF12100A).copy(alpha = 0.65f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 9.sp
                                )
                            )
                        }
                    }
                }
            } else {
                Text(
                    text  = "Lengkapi form terlebih dahulu",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        // Trust signal below button
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Outlined.Lock,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
                modifier           = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text  = "Pembayaran aman & terenkripsi via Midtrans",
                style = MaterialTheme.typography.labelSmall.copy(
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    fontSize = 9.sp
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DonasiSectionLabel — Consistent with app design system
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DonasiSectionLabel(
    text: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier          = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
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
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text  = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color         = if (isAppInDarkTheme()) GoldLight else GoldDeep
                )
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 10.sp
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DonasiSuccessDialog — Dedicated to donasi (NOT reuse from KeuanganScreen)
// Animated rings, Jazakallah message, impact note
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DonasiSuccessDialog(
    donasiInfo: Triple<Long, String, String>?,   // <nominal, jenis, nama>
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary

    // Multi-ring pulse — more celebratory than keuangan
    val infiniteTransition = rememberInfiniteTransition(label = "donasiSuccess")
    val ring1 by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label         = "ring1"
    )
    
    // Confetti particles simulation
    val particles = remember { List(15) { (0..360).random() } }

    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing)),
        label = "particles"
    )

    // Gold shimmer on icon
    val shimmerTrans = rememberInfiniteTransition(label = "donasiIconShimmer")
    val shimmerX by shimmerTrans.animateFloat(
        initialValue  = -150f,
        targetValue   = 150f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label         = "donasiShimX"
    )

    // Entry scale bounce
    val scale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "dialogEntryScale"
    )

    val formattedNominal = donasiInfo?.let { (nominal, _, _) ->
        "Rp " + "%,d".format(nominal).replace(',', '.')
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .scale(scale)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            GoldPrimary.copy(alpha = 0.5f),
                            GoldPrimary.copy(alpha = 0.1f),
                            GoldPrimary.copy(alpha = 0.5f)
                        )
                    ),
                    RoundedCornerShape(32.dp)
                )
        ) {
            // Decorative Corner
            Canvas(modifier = Modifier.size(100.dp).align(Alignment.TopEnd).alpha(0.1f)) {
                drawCircle(GoldPrimary, radius = size.width)
            }

            Column(
                modifier            = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Animated Gold Icon ─────────────────────────────────────
                Box(
                    modifier         = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Confetti/Star Burst
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        particles.forEach { angle ->
                            val rad = angle * PI / 180f
                            val dist = 40.dp.toPx() + (particleOffset * 30.dp.toPx())
                            val alpha = (1f - particleOffset).coerceIn(0f, 1f)
                            drawCircle(
                                color = GoldPrimary.copy(alpha = alpha),
                                radius = 2.dp.toPx(),
                                center = Offset(
                                    x = (size.width / 2) + cos(rad).toFloat() * dist,
                                    y = (size.height / 2) + sin(rad).toFloat() * dist
                                )
                            )
                        }
                    }

                    // Ripple ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val p = ring1
                        drawCircle(
                            color = GoldPrimary.copy(alpha = (1f - p) * 0.4f),
                            radius = (size.minDimension / 2f) * p,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Gold icon orb with shimmer
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = GoldPrimary,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().drawBehind {
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.3f), Color.Transparent),
                                        start = Offset(shimmerX, 0f),
                                        end   = Offset(shimmerX + 100f, size.height)
                                    )
                                )
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFF12100A),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Arabic jazakallah
                Text(
                    text       = "جَزَاكَ اللَّهُ خَيْرًا",
                    fontFamily = AmiriFontFamily,
                    fontSize   = 32.sp,
                    color      = GoldDeep,
                    textAlign  = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text      = "Donasi Diterima!",
                    style     = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight    = FontWeight.ExtraBold,
                        color         = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Info Card
                if (donasiInfo != null) {
                    val (_, jenis, nama) = donasiInfo
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formattedNominal ?: "",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = GoldDeep
                                )
                            )
                            Text(
                                text = "$jenis dari $nama",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Text(
                    text      = "Infaq/Shadaqah Anda telah kami terima dan akan disalurkan sesuai amanah. Semoga menjadi amal jariyah yang terus mengalir.",
                    style     = MaterialTheme.typography.bodyMedium.copy(
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // CTA Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Share Button
                    OutlinedButton(
                        onClick = {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "Alhamdulillah, saya baru saja berdonasi $formattedNominal ke Al-Hasanah Media. Mari ikut berkontribusi untuk pendidikan santri!")
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Bagikan Kebaikan"))
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp), tint = GoldDeep)
                        Spacer(Modifier.width(8.dp))
                        Text("SHARE", style = MaterialTheme.typography.labelLarge.copy(color = GoldDeep, fontWeight = FontWeight.Bold))
                    }

                    // Main Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1.5f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text  = "AAMIIN",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color         = Color(0xFF12100A),
                                fontWeight    = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading & Error States — theme-adaptive
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DonasiLoadingState() {
    val primary = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "donasiLoading")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.30f,
        targetValue   = 0.85f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "donasiLoadGlow"
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color       = GoldPrimary.copy(alpha = glowAlpha),
                trackColor  = GoldPrimary.copy(alpha = 0.08f),
                strokeWidth = 2.5.dp,
                modifier    = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text          = "Memuat…",
                fontSize      = 12.sp,
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun DonasiErrorState(message: String, onRetry: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "retryBtnScale"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier            = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.09f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Outlined.WifiOff,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.error,
                    modifier           = Modifier.size(32.dp)
                )
            }

            Text(
                text      = "Gagal Memuat",
                style     = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text      = message,
                style     = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.60f)
                    .height(48.dp)
                    .scale(pressScale)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(listOf(GoldDeep, GoldPrimary))
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication        = null
                    ) { onRetry() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint     = Color(0xFF12100A),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text  = "COBA LAGI",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color         = Color(0xFF12100A),
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    )
                }
            }
        }
    }
}
