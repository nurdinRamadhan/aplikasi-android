package com.alhasanah.alhasanahmedia.ui.santri

// ─────────────────────────────────────────────────────────────────────────────
// Imports
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.HafalanTahfidz
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.util.formatDateOnly
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Predikat System — Grade → Visual mapping
// ─────────────────────────────────────────────────────────────────────────────

private data class PredikatStyle(
    val label: String,
    val color: Color,
    val rank: Int      // 1 = highest
)

private enum class HafalanDateFilter(val label: String) {
    ALL("Semua"),
    THIS_MONTH("Bulan Ini"),
    LAST_30_DAYS("30 Hari"),
    LAST_90_DAYS("90 Hari"),
    THIS_YEAR("Tahun Ini"),
    MUMTAZ("Mumtaz"),
    JAYYID("Jayyid"),
    KURANG("Bimbingan")
}

private fun HafalanTahfidz.localDateOrNull(): LocalDate? = runCatching {
    tanggal?.take(10)?.let(LocalDate::parse)
}.getOrNull()

private fun HafalanTahfidz.matchesFilter(filter: HafalanDateFilter): Boolean {
    val normalizedPredikat = predikat?.trim()?.lowercase()
    if (filter == HafalanDateFilter.MUMTAZ) return normalizedPredikat == "mumtaz"
    if (filter == HafalanDateFilter.JAYYID) return normalizedPredikat == "jayyid"
    if (filter == HafalanDateFilter.KURANG) return normalizedPredikat == "kurang"

    val date = localDateOrNull() ?: return filter == HafalanDateFilter.ALL
    val now = LocalDate.now()
    return when (filter) {
        HafalanDateFilter.ALL -> true
        HafalanDateFilter.THIS_MONTH -> YearMonth.from(date) == YearMonth.from(now)
        HafalanDateFilter.LAST_30_DAYS -> !date.isBefore(now.minusDays(30))
        HafalanDateFilter.LAST_90_DAYS -> !date.isBefore(now.minusDays(90))
        HafalanDateFilter.THIS_YEAR -> date.year == now.year
        HafalanDateFilter.MUMTAZ,
        HafalanDateFilter.JAYYID,
        HafalanDateFilter.KURANG -> false
    }
}

private fun List<HafalanTahfidz>.latestFirst(): List<HafalanTahfidz> =
    sortedWith(
        compareByDescending<HafalanTahfidz> { it.localDateOrNull() ?: LocalDate.MIN }
            .thenByDescending { it.id }
    )

private fun monthTitle(date: LocalDate?): String {
    if (date == null) return "Tanggal tidak diketahui"
    return YearMonth.from(date)
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id")))
}

@Composable
private fun predikatStyle(predikat: String?): PredikatStyle {
    val primary = MaterialTheme.colorScheme.primary
    return when (predikat?.trim()?.lowercase()) {
        "mumtaz" -> PredikatStyle("MUMTAZ", primary,              1) // Gold — Terbaik
        "jayyid" -> PredikatStyle("JAYYID", Color(0xFF0277BD),    2) // Blue — Baik
        "kurang" -> PredikatStyle("KURANG", Color(0xFFBA1A1A),    3) // Red  — Perlu ditingkatkan
        else     -> PredikatStyle(
            predikat?.uppercase() ?: "—",
            MaterialTheme.colorScheme.onSurfaceVariant,
            3
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HafalanScreen — Root
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HafalanScreen(
    navController: NavController,
    viewModel: SantriActivityViewModel,
    santriNis: String
) {
    val hafalanList by viewModel.hafalanState.collectAsState()
    val santri      by viewModel.santriState.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    var activeFilter by remember { mutableStateOf(HafalanDateFilter.ALL) }
    var showAllHistory by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = santriNis) {
        viewModel.loadAllData(santriNis)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subtle Islamic-pattern background
        HafalanIslamicBackground()

        if (isLoading && hafalanList.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color    = MaterialTheme.colorScheme.primary
            )
        } else {
            val sortedHafalan = remember(hafalanList) {
                hafalanList.latestFirst()
            }
            val filteredHistory = remember(sortedHafalan, activeFilter) {
                sortedHafalan.filter { it.matchesFilter(activeFilter) }
            }
            val visibleHistory = remember(filteredHistory, showAllHistory) {
                if (showAllHistory || filteredHistory.size <= 3) filteredHistory else filteredHistory.take(3)
            }
            val groupedHistory = remember(visibleHistory) {
                visibleHistory.groupBy { monthTitle(it.localDateOrNull()) }
            }

            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                // ── 1. Gradient Header ────────────────────────────────────
                item {
                    HafalanHeader(
                        santriName   = santri?.namaLengkap ?: "",
                        totalHafalan = santri?.totalHafalan ?: "0 Juz",
                        onBack       = { navController.popBackStack() }
                    )
                }

                // ── 2. Last Setoran Card ──────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    sortedHafalan.firstOrNull()?.let { last ->
                        LastSetoranCard(
                            hafalan  = last,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }

                // ── 3. Riwayat Section Label ──────────────────────────────
                if (hafalanList.isNotEmpty()) {
                    item {
                        HafalanHistoryHeader(
                            totalCount = filteredHistory.size,
                            activeFilter = activeFilter,
                            onFilterChange = {
                                activeFilter = it
                                showAllHistory = false
                            }
                        )
                    }

                    // ── 4. Setoran History Cards ──────────────────────────
                    if (filteredHistory.isEmpty()) {
                        item { HafalanHistoryEmpty(activeFilter = activeFilter) }
                    } else {
                        groupedHistory.forEach { (month, itemsInMonth) ->
                            item { HafalanMonthLabel(month = month) }
                            items(itemsInMonth, key = { "${it.id}_${it.tanggal}_${it.ayat_awal}" }) { hafalan ->
                                HafalanSetoranCard(
                                    hafalan  = hafalan,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp)
                                )
                            }
                        }
                        if (filteredHistory.size > 3) {
                            item {
                                HistoryToggleButton(
                                    expanded = showAllHistory,
                                    hiddenCount = (filteredHistory.size - 3).coerceAtLeast(0),
                                    onClick = { showAllHistory = !showAllHistory }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Background — Islamic Geometric Dots (Halus & Tidak Mengganggu)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HafalanIslamicBackground() {
    val color   = MaterialTheme.colorScheme.primary.copy(alpha = 0.035f)
    val infiniteTransition = rememberInfiniteTransition(label = "bgRot")
    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(180_000, easing = LinearEasing)),
        label         = "bgRotVal"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 96.dp.toPx()
        val cols    = (size.width  / spacing).toInt() + 2
        val rows    = (size.height / spacing).toInt() + 2
        val starR   = 14.dp.toPx()

        for (col in -1..cols) {
            for (row in -1..rows) {
                val stagger = if (col % 2 == 0) spacing / 2f else 0f
                val center  = Offset(col * spacing, row * spacing + stagger)
                val localRot = if ((col + row) % 2 == 0) rotation else -rotation

                rotate(degrees = localRot, pivot = center) {
                    // 8-pointed Islamic star
                    val path   = Path()
                    val inner  = starR * 0.55f
                    val sides  = 8
                    for (i in 0 until sides * 2) {
                        val r     = if (i % 2 == 0) starR else inner
                        val angle = (i * PI / sides - PI / 2).toFloat()
                        val px    = center.x + r * cos(angle.toDouble()).toFloat()
                        val py    = center.y + r * sin(angle.toDouble()).toFloat()
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    drawPath(path, color)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HafalanHeader — Gradient with Arc Gauge inside Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HafalanHeader(
    santriName: String,
    totalHafalan: String,
    onBack: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val titleColor = if (isDark) primary.copy(alpha = 0.92f) else Color(0xFF8B6914)
    val bodyColor = if (isDark) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(398.dp)
    ) {
        AppPageHeaderBackground(isDark = isDark, modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Top Action Row ────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.10f else 0.56f))
                        .border(1.dp, primary.copy(alpha = 0.38f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(46.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = if (isDark) Color.White.copy(0.88f) else Color(0xFF2B2418),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Box(modifier = Modifier.size(46.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text  = "PROGRES TAHFIDZ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color         = titleColor
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text  = "Rekam Jejak Hafalan Al-Qur'an",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color      = bodyColor,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HafalanHeroDivider(isDark = isDark)

                if (santriName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(0.5.dp)
                                .background(primary.copy(alpha = 0.35f))
                        )
                        Text(
                            text  = "  ✦  ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color    = primary.copy(alpha = 0.75f),
                                fontSize = 8.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(0.5.dp)
                                .background(primary.copy(alpha = 0.35f))
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text  = santriName.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color         = bodyColor,
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        )
                    )
                }
            }

            // ── Arc Gauge Card (glass) ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.56f else 0.97f))
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                primary.copy(alpha = if (isDark) 0.50f else 0.62f),
                                primary.copy(alpha = if (isDark) 0.12f else 0.28f),
                                primary.copy(alpha = if (isDark) 0.38f else 0.50f)
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(vertical = 14.dp)
            ) {
                HafalanArcGauge(
                    totalHafalan = totalHafalan,
                    primary      = primary,
                    secondary    = primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun HafalanHeroDivider(isDark: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val lineColor = if (isDark) Color.White.copy(0.07f) else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier.width(170.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, lineColor))))
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.size(5.dp).background(primary.copy(0.60f), RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Box(modifier = Modifier.size(8.dp).background(primary.copy(0.82f), CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Box(modifier = Modifier.size(5.dp).background(primary.copy(0.60f), RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(lineColor, Color.Transparent))))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HafalanArcGauge — Redesigned Arc with Juz Ticks & Contextual Labels
// Logika identik dengan aslinya, hanya visual yang ditingkatkan
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HafalanArcGauge(
    totalHafalan: String,
    primary: Color,
    secondary: Color
) {
    val juzValue = totalHafalan.filter { it.isDigit() }.toFloatOrNull() ?: 0f
    val progress = (juzValue / 30f).coerceIn(0f, 1f)
    val isDark = isAppInDarkTheme()

    val animatedProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(2000, easing = FastOutSlowInEasing),
        label         = "hafalanProgress"
    )

    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.45f else 0.72f)

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(142.dp)) {
                val arcInset   = 14.dp.toPx()
                val strokeW    = 11.dp.toPx()
                val tickRadius = size.minDimension / 2f

                // ── Track arc (background) ─────────────────────────────────
                drawArc(
                    color      = trackColor,
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter  = false,
                    style      = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // ── Progress arc ───────────────────────────────────────────
                if (animatedProgress > 0f) {
                    drawArc(
                        brush      = Brush.sweepGradient(
                            colors = listOf(
                                secondary,
                                primary.copy(green = (primary.green + 0.05f).coerceAtMost(1f)),
                                secondary
                            ),
                            center = center
                        ),
                        startAngle = 140f,
                        sweepAngle = 260f * animatedProgress,
                        useCenter  = false,
                        style      = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // ── Juz tick marks (30 ticks = 30 Juz) ────────────────────
                val outerR = tickRadius - arcInset - strokeW / 2f - 6.dp.toPx()
                val innerR = outerR - 8.dp.toPx()
                for (i in 0..30) {
                    val angle    = (140f + 260f * (i / 30f))
                    val angleRad = (angle * PI / 180f).toFloat()
                    val tickActive = (i / 30f) <= animatedProgress
                    val isMajor  = i % 5 == 0

                    val tickOuter = if (isMajor) outerR + 4.dp.toPx() else outerR
                    val tickInner = if (isMajor) innerR - 2.dp.toPx() else innerR

                    drawLine(
                        color       = if (tickActive) secondary.copy(alpha = 0.85f)
                                      else muted.copy(alpha = if (isDark) 0.22f else 0.34f),
                        start       = Offset(
                            tickOuter * cos(angleRad) + center.x,
                            tickOuter * sin(angleRad) + center.y
                        ),
                        end         = Offset(
                            tickInner * cos(angleRad) + center.x,
                            tickInner * sin(angleRad) + center.y
                        ),
                        strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.dp.toPx()
                    )
                }

                // ── Progress dot (tip indicator) ───────────────────────────
                if (animatedProgress > 0.01f) {
                    val tipAngle = (140f + 260f * animatedProgress) * PI.toFloat() / 180f
                    val tipR     = tickRadius - arcInset - strokeW / 2f
                    val tipX     = tipR * cos(tipAngle) + center.x
                    val tipY     = tipR * sin(tipAngle) + center.y
                    drawCircle(
                        color  = Color.White,
                        radius = 5.dp.toPx(),
                        center = Offset(tipX, tipY)
                    )
                    drawCircle(
                        color  = secondary,
                        radius = 3.dp.toPx(),
                        center = Offset(tipX, tipY)
                    )
                }
            }

            // ── Center Text ───────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = totalHafalan,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        color      = onSurface,
                        fontSize   = 30.sp
                    )
                )
                Text(
                    text  = "dari 30 Juz",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color         = muted,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        // ── Bottom Row: Start / End labels + percentage ─────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = "Juz 1",
                style = MaterialTheme.typography.labelSmall.copy(
                    color  = muted,
                    fontSize = 10.sp
                )
            )
            // Percentage chip in center
            Surface(
                shape = CircleShape,
                color = primary.copy(alpha = if (isDark) 0.12f else 0.18f)
            ) {
                Text(
                    text     = "${(progress * 100).toInt()}% Tercapai",
                    style    = MaterialTheme.typography.labelSmall.copy(
                        color      = primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
            Text(
                text  = "Juz 30",
                style = MaterialTheme.typography.labelSmall.copy(
                    color  = muted,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun HafalanHistoryHeader(
    totalCount: Int,
    activeFilter: HafalanDateFilter,
    onFilterChange: (HafalanDateFilter) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .background(
                            Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.25f))),
                            RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "RIWAYAT SETORAN",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
            Surface(
                shape = CircleShape,
                color = primary.copy(alpha = 0.10f)
            ) {
                Text(
                    text = "$totalCount setoran",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = primary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HafalanDateFilter.entries.forEach { filter ->
                val selected = filter == activeFilter
                Surface(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .widthIn(min = 86.dp)
                        .clickable { onFilterChange(filter) },
                    shape = CircleShape,
                    color = if (selected) primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    border = if (selected) null else androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HafalanMonthLabel(month: String) {
    Text(
        text = month.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
            fontWeight = FontWeight.Black
        ),
        modifier = Modifier.padding(start = 26.dp, end = 24.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun HafalanHistoryEmpty(activeFilter: HafalanDateFilter) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Text(
            text = "Tidak ada setoran pada filter ${activeFilter.label.lowercase()}.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@Composable
private fun HistoryToggleButton(
    expanded: Boolean,
    hiddenCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (expanded) Icons.Outlined.VisibilityOff else Icons.Outlined.Article,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (expanded) "SEMBUNYIKAN RIWAYAT" else "LIHAT SEMUA RIWAYAT (+$hiddenCount)",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LastSetoranCard — Highlight Card untuk Setoran Terakhir
// Menggantikan HafalanSummaryOrb yang terlalu minim informasi
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LastSetoranCard(
    hafalan: HafalanTahfidz,
    modifier: Modifier = Modifier
) {
    val style   = predikatStyle(hafalan.predikat)
    val primary = MaterialTheme.colorScheme.primary
    val ayatCount = ((hafalan.ayat_akhir ?: 0) - (hafalan.ayat_awal ?: 0) + 1)
        .coerceAtLeast(0)

    Card(
        modifier  = modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape     = RoundedCornerShape(20.dp)
    ) {
        Box {
            // Islamic ornament — subtle corner star
            Canvas(modifier = Modifier.size(80.dp).align(Alignment.TopEnd)) {
                val path   = Path()
                val center = Offset(size.width * 0.88f, -size.height * 0.08f)
                val r      = size.width * 0.60f
                val inner  = r * 0.55f
                for (i in 0 until 16) {
                    val rad  = if (i % 2 == 0) r else inner
                    val ang  = (i * PI / 8 - PI / 2).toFloat()
                    val px   = center.x + rad * cos(ang.toDouble()).toFloat()
                    val py   = center.y + rad * sin(ang.toDouble()).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, primary.copy(alpha = 0.045f))
            }

            Column(modifier = Modifier.padding(20.dp)) {

                // ── Header ────────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(
                                    Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.25f))),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text  = "SETORAN TERAKHIR",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight    = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                color         = primary
                            )
                        )
                    }

                    // Predikat badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = style.color.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text     = style.label,
                            style    = MaterialTheme.typography.labelSmall.copy(
                                color         = style.color,
                                fontWeight    = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                fontSize      = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.5.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // ── Surat Name (Hero Text) ────────────────────────────────
                Text(
                    text     = (hafalan.surat ?: "—").uppercase(),
                    style    = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight    = FontWeight.Black,
                        color         = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Metadata Row ──────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetaChip(
                        icon  = Icons.Outlined.MenuBook,
                        label = "Juz ${hafalan.juz ?: "—"}"
                    )
                    MetaChip(
                        icon  = Icons.Outlined.AutoStories,
                        label = "Ayat ${hafalan.ayat_awal ?: "—"} – ${hafalan.ayat_akhir ?: "—"}"
                    )
                    MetaChip(
                        icon  = Icons.Outlined.Tag,
                        label = "$ayatCount Ayat"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    thickness = 0.5.dp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // ── Footer: Tanggal ───────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text  = "Setoran: ${formatDateOnly(hafalan.tanggal)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HafalanInfoPanel(
                    label = "Detail Hafalan",
                    value = hafalan.detail_hafalan,
                    icon = Icons.Outlined.MenuBook,
                    modifier = Modifier.padding(top = 10.dp)
                )

                HafalanInfoPanel(
                    label = "Penyimak",
                    value = hafalan.penyimak,
                    icon = Icons.Outlined.Person,
                    modifier = Modifier.padding(top = 6.dp)
                )

                HafalanCatatanPanel(
                    catatan = hafalan.catatan,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HafalanSetoranCard — History Card
// Menggantikan HafalanCyberCard: Hapus segmented progress bar palsu,
// tambah info lengkap, tambah press animation, accent bar per predikat
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HafalanSetoranCard(
    hafalan: HafalanTahfidz,
    modifier: Modifier = Modifier
) {
    val style     = predikatStyle(hafalan.predikat)
    val ayatCount = ((hafalan.ayat_akhir ?: 0) - (hafalan.ayat_awal ?: 0) + 1)
        .coerceAtLeast(0)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.977f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "setoranCardScale"
    )

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = {}     // detail sheet bisa ditambahkan di sini
            ),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape     = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            // ── Left: Predikat Accent Bar ──────────────────────────────────
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(style.color, style.color.copy(alpha = 0.30f))
                        ),
                        RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                    )
            )

            // ── Right: Content ─────────────────────────────────────────────
            Column(
                modifier              = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // ── Left Column: Surat + Meta ──────────────────────────
                    Column(modifier = Modifier.weight(1f)) {
                        // Surat name
                        Text(
                            text     = (hafalan.surat ?: "—").uppercase(),
                            style    = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight    = FontWeight.ExtraBold,
                                color         = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.3.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Meta — Juz & Ayat range
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.MenuBook,
                                    contentDescription = null,
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text  = "Juz ${hafalan.juz ?: "—"}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            Text(
                                text  = "·",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.AutoStories,
                                    contentDescription = null,
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text  = "Ayat ${hafalan.ayat_awal ?: "—"}–${hafalan.ayat_akhir ?: "—"}  ($ayatCount ayat)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        // Tanggal
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text  = formatDateOnly(hafalan.tanggal),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    fontWeight = FontWeight.Normal,
                                    fontSize   = 10.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // ── Right: Predikat Badge + Rank Indicator ─────────────
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = style.color.copy(alpha = 0.10f)
                        ) {
                            Column(
                                modifier            = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text  = style.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color         = style.color,
                                        fontWeight    = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        fontSize      = 8.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Rank stars — 3 level sesuai sistem pesantren
                        val starCount = when (style.rank) {
                            1    -> 3   // Mumtaz  → ★★★
                            2    -> 2   // Jayyid  → ★★
                            3    -> 1   // Kurang  → ★
                            else -> 0
                        }
                        if (starCount > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                repeat(starCount) {
                                    Text(
                                        text  = "★",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color    = style.color.copy(alpha = 0.70f),
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }

                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HafalanInfoPanel(
                    label = "Detail Hafalan",
                    value = hafalan.detail_hafalan,
                    icon = Icons.Outlined.MenuBook,
                    modifier = Modifier.padding(top = 8.dp)
                )

                HafalanInfoPanel(
                    label = "Penyimak",
                    value = hafalan.penyimak,
                    icon = Icons.Outlined.Person,
                    modifier = Modifier.padding(top = 4.dp)
                )

                HafalanCatatanPanel(
                    catatan = hafalan.catatan,
                    compact = true,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun HafalanCatatanPanel(
    catatan: String?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val note = catatan?.trim()?.takeIf { it.isNotBlank() } ?: return
    var expanded by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
        color = primary.copy(alpha = if (isAppInDarkTheme()) 0.10f else 0.075f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            primary.copy(alpha = if (isAppInDarkTheme()) 0.28f else 0.22f)
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 12.dp else 14.dp,
                vertical = if (compact) 10.dp else 12.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notes,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Catatan Ustadz",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = primary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.45.sp,
                            fontSize = if (compact) 9.sp else 10.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp,
                    fontSize = if (compact) 11.sp else 12.sp
                ),
                maxLines = if (expanded) Int.MAX_VALUE else if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HafalanInfoPanel — Detail Hafalan / Penyimak (simple, no expand)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HafalanInfoPanel(
    label: String,
    value: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val text = value?.trim()?.takeIf { it.isNotBlank() } ?: return
    val primary = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = primary.copy(alpha = if (isAppInDarkTheme()) 0.08f else 0.06f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            primary.copy(alpha = if (isAppInDarkTheme()) 0.18f else 0.14f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.4.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        fontSize = 11.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MetaChip — Reusable small icon+label chip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector  = icon,
            contentDescription = null,
            tint         = MaterialTheme.colorScheme.primary,
            modifier     = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 10.sp
            )
        )
    }
}
