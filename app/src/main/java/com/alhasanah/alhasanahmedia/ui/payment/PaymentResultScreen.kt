package com.alhasanah.alhasanahmedia.ui.payment

// ─────────────────────────────────────────────────────────────────────────────
// Imports
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhasanah.alhasanahmedia.ui.components.AppGradientBackground
import com.alhasanah.alhasanahmedia.ui.theme.AmiriFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Public API — Unchanged (logika pemanggil tidak berubah)
// ─────────────────────────────────────────────────────────────────────────────

enum class PaymentStatus { SUCCESS, PENDING, FAILED }

data class PaymentResultData(
    val status: PaymentStatus,
    val orderId: String,
    val transactionId: String = "",
    val message: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
// Brand Color Constants — Gold consistent with all screens
// ─────────────────────────────────────────────────────────────────────────────

private val GoldPrimary  = Color(0xFFD4A017)
private val GoldLight    = Color(0xFFE8C55A)
private val GoldDeep     = Color(0xFFAA7C1F)
private val GoldShimmer  = Color(0xFFFAF0C0)

private val AmberPrimary = Color(0xFFF9A825)
private val AmberLight   = Color(0xFFFFD54F)
private val AmberDeep    = Color(0xFFF57F17)

private val ErrorPrimary = Color(0xFFBA1A1A)
private val ErrorLight   = Color(0xFFFF8A80)

private val GoldGradientBrush = Brush.linearGradient(
    listOf(GoldDeep, GoldPrimary, GoldLight, GoldPrimary, GoldDeep)
)

// ─────────────────────────────────────────────────────────────────────────────
// Internal Config — extended with Islamic content per status
// ─────────────────────────────────────────────────────────────────────────────

private data class PaymentResultConfig(
    // Visual
    val accentPrimary: Color,
    val accentLight: Color,
    val accentDeep: Color,
    val statusIcon: ImageVector,
    val headerGradient: Brush,
    // Text
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val actionGradient: Brush,
    val actionTextColor: Color,
    // Islamic content
    val arabicText: String,
    val arabicTranslation: String,
    val arabicSource: String,
    val duaText: String,
    val duaTranslation: String
)

// ─────────────────────────────────────────────────────────────────────────────
// PaymentResultScreen — Root (signature tidak berubah)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PaymentResultScreen(
    resultData: PaymentResultData,
    onActionClick: () -> Unit,
    onBackHome: () -> Unit
) {
    val isDark  = isSystemInDarkTheme()
    val config  = rememberConfig(resultData = resultData, message = resultData.message)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppGradientBackground(isDark = isDark)
        PaymentResultBackground(accentColor = config.accentPrimary)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // ── 1. Animated Status Orb ─────────────────────────────────────
                StatusOrb(config = config, status = resultData.status)

                Spacer(modifier = Modifier.height(28.dp))

                // ── 2. Arabic Content Block ────────────────────────────────────
                ArabicContentBlock(config = config, isDark = isDark)

                Spacer(modifier = Modifier.height(22.dp))

                // ── 3. Status Title + Subtitle ─────────────────────────────────
                Text(
                    text          = config.title,
                    style         = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight    = FontWeight.Black,
                        color         = config.accentPrimary,
                        letterSpacing = 0.3.sp
                    ),
                    textAlign     = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text      = config.subtitle,
                    style     = MaterialTheme.typography.bodyMedium.copy(
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── 4. Transaction Info Card ───────────────────────────────────
                TransactionInfoCard(
                    resultData = resultData,
                    config     = config,
                    isDark     = isDark
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── 5. Dua Card — only on SUCCESS & PENDING ────────────────────
                if (resultData.status != PaymentStatus.FAILED) {
                    DuaCard(config = config, isDark = isDark)
                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    // FAILED: encouragement box instead
                    EncouragementBox(config = config, isDark = isDark)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── 6. CTA Buttons ─────────────────────────────────────────────
                ActionButtons(
                    config        = config,
                    status        = resultData.status,
                    onActionClick = onActionClick,
                    onBackHome    = onBackHome
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Config Builder — per status
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun rememberConfig(resultData: PaymentResultData, message: String): PaymentResultConfig {
    val isDark = isSystemInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    return when (resultData.status) {

        PaymentStatus.SUCCESS -> PaymentResultConfig(
            accentPrimary   = GoldPrimary,
            accentLight     = GoldLight,
            accentDeep      = GoldDeep,
            statusIcon      = Icons.Default.CheckCircle,
            headerGradient  = Brush.linearGradient(listOf(GoldDeep, GoldPrimary, GoldLight)),
            title           = "Pembayaran Berhasil",
            subtitle        = message.ifEmpty {
                "Transaksi Anda telah dikonfirmasi dan tercatat dalam sistem Al-Hasanah Media."
            },
            actionLabel     = "Lihat Riwayat",
            actionGradient  = Brush.horizontalGradient(listOf(GoldDeep, GoldPrimary, GoldLight)),
            actionTextColor = Color(0xFF12100A),
            // Islamic content — Jazakallah
            arabicText      = "جَزَاكَ اللَّهُ خَيْرًا",
            arabicTranslation = "Semoga Allah membalasmu dengan kebaikan",
            arabicSource    = "— Doa syukur atas kebaikan",
            duaText         = "اللَّهُمَّ تَقَبَّلْ مِنَّا إِنَّكَ أَنتَ السَّمِيعُ الْعَلِيمُ",
            duaTranslation  = "\"Ya Allah, terimalah (amal) dari kami. Sesungguhnya Engkau Maha Mendengar lagi Maha Mengetahui.\""
        )

        PaymentStatus.PENDING -> PaymentResultConfig(
            accentPrimary   = AmberPrimary,
            accentLight     = AmberLight,
            accentDeep      = AmberDeep,
            statusIcon      = Icons.Default.Schedule,
            headerGradient  = Brush.linearGradient(listOf(AmberDeep, AmberPrimary, AmberLight)),
            title           = "Menunggu Pembayaran",
            subtitle        = message.ifEmpty {
                "Selesaikan pembayaran Anda sebelum batas waktu habis. Kami akan memperbarui status otomatis."
            },
            actionLabel     = "Cek Status Pembayaran",
            actionGradient  = Brush.horizontalGradient(listOf(AmberDeep, AmberPrimary)),
            actionTextColor = Color(0xFF1A0E00),
            // Islamic content — Tawakkul
            arabicText      = "وَعَلَى اللَّهِ فَتَوَكَّلُوا",
            arabicTranslation = "\"Dan hanya kepada Allah hendaknya kamu bertawakal\"",
            arabicSource    = "— QS. Al-Ma'idah: 23",
            duaText         = "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ",
            duaTranslation  = "\"Cukuplah Allah sebagai penolong kami, dan Dia sebaik-baik pelindung.\""
        )

        PaymentStatus.FAILED -> PaymentResultConfig(
            accentPrimary   = ErrorPrimary,
            accentLight     = ErrorLight,
            accentDeep      = Color(0xFF7B0000),
            statusIcon      = Icons.Default.ErrorOutline,
            headerGradient  = Brush.linearGradient(listOf(Color(0xFF7B0000), ErrorPrimary, ErrorLight)),
            title           = "Pembayaran Gagal",
            subtitle        = message.ifEmpty {
                "Transaksi tidak dapat diproses. Silakan periksa koneksi atau metode pembayaran Anda."
            },
            actionLabel     = "Coba Lagi",
            actionGradient  = Brush.horizontalGradient(listOf(Color(0xFF7B0000), ErrorPrimary)),
            actionTextColor = Color.White,
            // Islamic content — Jangan putus asa
            arabicText      = "لَا تَيْأَسُوا مِن رَّوْحِ اللَّهِ",
            arabicTranslation = "\"Janganlah kamu berputus asa dari rahmat Allah\"",
            arabicSource    = "— QS. Yusuf: 87",
            duaText         = "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            duaTranslation  = "\"Sesungguhnya sesudah kesulitan itu ada kemudahan.\"\n— QS. Al-Insyirah: 6"
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Background — Islamic Star Pattern
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PaymentResultBackground(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "payBg")
    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(140_000, easing = LinearEasing)),
        label         = "payBgRot"
    )
    val starColor = accentColor.copy(alpha = 0.028f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 92.dp.toPx()
        val starR   = 13.dp.toPx()
        val cols    = (size.width  / spacing).toInt() + 2
        val rows    = (size.height / spacing).toInt() + 2

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
                    drawPath(path, starColor)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StatusOrb — Animated rings, per-status icon & gradient
// SUCCESS  : 3 gold ripple rings + gold icon orb + shimmer sweep
// PENDING  : pulsing amber single ring + rotating arc
// FAILED   : static error arc + no pulse (calm, not alarming)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatusOrb(config: PaymentResultConfig, status: PaymentStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusOrb")

    val ring1 by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label         = "ring1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue  = 0.33f,
        targetValue   = 1.33f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label         = "ring2"
    )
    val ring3 by infiniteTransition.animateFloat(
        initialValue  = 0.66f,
        targetValue   = 1.66f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label         = "ring3"
    )

    val pendingRotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label         = "pendingRot"
    )

    val shimmerX by infiniteTransition.animateFloat(
        initialValue  = -120f,
        targetValue   = 120f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label         = "shimX"
    )

    val breatheScale by infiniteTransition.animateFloat(
        initialValue  = 0.97f,
        targetValue   = 1.03f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Box(
        modifier         = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // ── Ripple rings (SUCCESS only) ────────────────────────────────────
        if (status == PaymentStatus.SUCCESS) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                listOf(ring1, ring2, ring3).forEach { prog ->
                    val p     = prog % 1f
                    val r     = (size.minDimension / 2f) * p
                    val alpha = (1f - p).coerceIn(0f, 1f) * 0.50f
                    drawCircle(
                        color  = config.accentPrimary.copy(alpha = alpha),
                        radius = r,
                        style  = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }

        // ── Rotating arc (PENDING only) ────────────────────────────────────
        if (status == PaymentStatus.PENDING) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                rotate(pendingRotation) {
                    drawArc(
                        brush      = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                config.accentPrimary.copy(alpha = 0.75f),
                                Color.Transparent
                            ),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter  = false,
                        style      = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // ── FAILED: Static broken arc ──────────────────────────────────────
        if (status == PaymentStatus.FAILED) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color      = config.accentPrimary.copy(alpha = 0.25f),
                    startAngle = -220f,
                    sweepAngle = 260f,
                    useCenter  = false,
                    style      = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // ── Outer diffuse glow ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    Brush.radialGradient(
                        listOf(config.accentPrimary.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        // ── Icon orb — gradient fill ───────────────────────────────────────
        Box(
            modifier = Modifier
                .size(if (status == PaymentStatus.PENDING) 76.dp else 80.dp)
                .scale(if (status == PaymentStatus.PENDING) breatheScale else 1f)
                .clip(CircleShape)
                .background(config.headerGradient)
                .drawBehind {
                    if (status == PaymentStatus.SUCCESS) {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.25f),
                                    Color.Transparent
                                ),
                                start = Offset(shimmerX, 0f),
                                end   = Offset(shimmerX + 80f, size.height)
                            )
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = config.statusIcon,
                contentDescription = null,
                tint               = when (status) {
                    PaymentStatus.SUCCESS -> Color(0xFF12100A)
                    PaymentStatus.PENDING -> Color(0xFF1A0E00)
                    PaymentStatus.FAILED  -> Color.White
                },
                modifier           = Modifier.size(38.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ArabicContentBlock — Dark mushaf-style card (always dark — same as HeroCard)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArabicContentBlock(config: PaymentResultConfig, isDark: Boolean) {
    val bgBrush = if (isDark)
        Brush.verticalGradient(listOf(Color(0xFF0E0C08), Color(0xFF181410)))
    else
        Brush.verticalGradient(listOf(Color(0xFF12100A), Color(0xFF1E1A0F)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bgBrush)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        config.accentPrimary.copy(alpha = 0.55f),
                        config.accentLight.copy(alpha = 0.18f),
                        config.accentPrimary.copy(alpha = 0.55f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .drawBehind {
                // Top gold line
                drawLine(
                    brush       = GoldGradientBrush,
                    start       = Offset(40f, 0f),
                    end         = Offset(size.width - 40f, 0f),
                    strokeWidth = 2.dp.toPx()
                )
                // Radial center glow
                drawCircle(
                    brush  = Brush.radialGradient(
                        colors = listOf(
                            config.accentPrimary.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.52f
                    ),
                    radius = size.width * 0.52f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arabic text — large Amiri font
            Text(
                text       = config.arabicText,
                fontFamily = AmiriFontFamily,
                fontSize   = 26.sp,
                color      = config.accentLight,
                textAlign  = TextAlign.Center,
                lineHeight = 38.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ornamental divider
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(0.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, config.accentPrimary.copy(alpha = 0.50f))
                            )
                        )
                )
                Text(
                    "  ✦  ",
                    color    = config.accentPrimary.copy(alpha = 0.55f),
                    fontSize = 8.sp
                )
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(0.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(config.accentPrimary.copy(alpha = 0.50f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Translation
            Text(
                text      = config.arabicTranslation,
                fontSize  = 11.sp,
                color     = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                lineHeight = 17.sp
            )

            // Source
            if (config.arabicSource.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text      = config.arabicSource,
                    fontSize  = 9.sp,
                    color     = config.accentPrimary.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TransactionInfoCard — Theme-adaptive, accent-colored border
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TransactionInfoCard(
    resultData: PaymentResultData,
    config: PaymentResultConfig,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        config.accentPrimary.copy(alpha = 0.35f),
                        config.accentPrimary.copy(alpha = 0.10f),
                        config.accentPrimary.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                // Accent top strip
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, config.accentPrimary.copy(alpha = 0.65f), Color.Transparent)
                    ),
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )
            }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(14.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(config.accentPrimary, config.accentPrimary.copy(alpha = 0.25f))
                            ),
                            RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text  = "INFORMASI TRANSAKSI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color         = config.accentPrimary,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        fontSize      = 8.sp
                    )
                )
            }

            HorizontalDivider(
                color     = config.accentPrimary.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )

            // Order ID row
            InfoDetailRow(
                label    = "Order ID",
                value    = resultData.orderId,
                icon     = Icons.Outlined.Receipt,
                isDark   = isDark,
                accent   = config.accentPrimary
            )

            // Transaction ID row (if available)
            if (resultData.transactionId.isNotEmpty()) {
                InfoDetailRow(
                    label    = "Transaction ID",
                    value    = resultData.transactionId,
                    icon     = Icons.Outlined.Tag,
                    isDark   = isDark,
                    accent   = config.accentPrimary
                )
            }

            // Status row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Outlined.Info,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text  = "Status",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = config.accentPrimary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector        = config.statusIcon,
                            contentDescription = null,
                            tint               = config.accentPrimary,
                            modifier           = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text  = when (resultData.status) {
                                PaymentStatus.SUCCESS -> "BERHASIL"
                                PaymentStatus.PENDING -> "MENUNGGU"
                                PaymentStatus.FAILED  -> "GAGAL"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color         = config.accentPrimary,
                                fontWeight    = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                fontSize      = 8.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoDetailRow(
    label: String,
    value: String,
    icon: ImageVector,
    isDark: Boolean,
    accent: Color
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Row(
            modifier          = Modifier.weight(0.38f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text  = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Text(
            text      = value,
            style     = MaterialTheme.typography.bodySmall.copy(
                color      = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.End,
            modifier  = Modifier.weight(0.62f),
            maxLines  = 2
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DuaCard — Displayed on SUCCESS & PENDING
// Light frosted glass with accent border
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DuaCard(config: PaymentResultConfig, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        config.accentPrimary.copy(alpha = if (isDark) 0.10f else 0.06f),
                        config.accentPrimary.copy(alpha = if (isDark) 0.06f else 0.03f)
                    )
                )
            )
            .border(
                0.8.dp,
                Brush.horizontalGradient(
                    listOf(
                        config.accentPrimary.copy(alpha = 0.35f),
                        config.accentPrimary.copy(alpha = 0.10f),
                        config.accentPrimary.copy(alpha = 0.35f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dua label
            Text(
                text          = "DOA",
                style         = MaterialTheme.typography.labelSmall.copy(
                    color         = config.accentPrimary,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontSize      = 8.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Arabic dua text
            Text(
                text       = config.duaText,
                fontFamily = AmiriFontFamily,
                fontSize   = 18.sp,
                color      = if (isDark) config.accentLight else config.accentDeep,
                textAlign  = TextAlign.Center,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Translation
            Text(
                text      = config.duaTranslation,
                style     = MaterialTheme.typography.bodySmall.copy(
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle  = FontStyle.Italic,
                    lineHeight = 18.sp,
                    fontSize   = 11.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EncouragementBox — FAILED status: calm, hopeful message
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EncouragementBox(config: PaymentResultConfig, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Arabic (large)
            Text(
                text       = config.arabicText,
                fontFamily = AmiriFontFamily,
                fontSize   = 20.sp,
                color      = config.accentLight,
                textAlign  = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text      = config.duaTranslation,
                style     = MaterialTheme.typography.bodySmall.copy(
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle  = FontStyle.Italic,
                    lineHeight = 18.sp,
                    fontSize   = 11.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tips row
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(config.accentPrimary.copy(alpha = 0.08f))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint               = config.accentPrimary,
                    modifier           = Modifier.size(14.dp).padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text  = "Pastikan koneksi internet stabil dan saldo atau metode pembayaran Anda mencukupi, lalu coba kembali.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        fontSize   = 10.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ActionButtons — Gradient primary CTA + outlined secondary
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActionButtons(
    config: PaymentResultConfig,
    status: PaymentStatus,
    onActionClick: () -> Unit,
    onBackHome: () -> Unit
) {
    val primarySource = remember { MutableInteractionSource() }
    val primaryPressed by primarySource.collectIsPressedAsState()
    val primaryScale by animateFloatAsState(
        targetValue   = if (primaryPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "primaryBtnScale"
    )

    val secondarySource = remember { MutableInteractionSource() }
    val secondaryPressed by secondarySource.collectIsPressedAsState()
    val secondaryScale by animateFloatAsState(
        targetValue   = if (secondaryPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "secondaryBtnScale"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Primary CTA — gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(primaryScale)
                .clip(RoundedCornerShape(16.dp))
                .background(config.actionGradient)
                .border(
                    0.5.dp,
                    config.accentLight.copy(alpha = 0.30f),
                    RoundedCornerShape(16.dp)
                )
                .clickable(
                    interactionSource = primarySource,
                    indication        = null,
                    onClick           = onActionClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector        = when (status) {
                        PaymentStatus.SUCCESS -> Icons.Default.CheckCircle
                        PaymentStatus.PENDING -> Icons.Default.Refresh
                        PaymentStatus.FAILED  -> Icons.Default.Replay
                    },
                    contentDescription = null,
                    tint               = config.actionTextColor,
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text  = config.actionLabel.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color         = config.actionTextColor,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        // Secondary — outlined
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .scale(secondaryScale)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    RoundedCornerShape(16.dp)
                )
                .clickable(
                    interactionSource = secondarySource,
                    indication        = null,
                    onClick           = onBackHome
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Home,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(16.dp)
                )
                Text(
                    text  = "Kembali ke Beranda",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        // Trust signal
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Outlined.Lock,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier           = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text  = "Transaksi diproses aman via Midtrans",
                style = MaterialTheme.typography.labelSmall.copy(
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
                    fontSize = 9.sp
                )
            )
        }
    }
}
