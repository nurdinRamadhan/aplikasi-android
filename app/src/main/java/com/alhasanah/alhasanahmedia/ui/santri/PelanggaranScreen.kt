package com.alhasanah.alhasanahmedia.ui.santri

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.PelanggaranSantri
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import com.alhasanah.alhasanahmedia.util.IslamicEmerald
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import com.alhasanah.alhasanahmedia.util.WarningAmber
import com.alhasanah.alhasanahmedia.util.WarningCrimson

// ─────────────────────────────────────────────────────────────────────────────
// Internal helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Menentukan warna berdasarkan total poin akumulatif */
@Composable
private fun severityColor(poin: Int) = when {
    poin >= 50 -> WarningCrimson
    poin >= 20 -> WarningAmber
    else       -> MaterialTheme.colorScheme.primary
}

/** Label status berdasarkan total poin */
private fun statusLabel(poin: Int) = when {
    poin >= 50 -> "Perlu Perhatian Serius"
    poin >= 20 -> "Perlu Bimbingan"
    else       -> "Kedisiplinan Baik"
}

/** Label tingkat keparahan per item pelanggaran */
private fun severityLabel(poin: Int) = when {
    poin >= 20 -> "BERAT"
    poin >= 10 -> "SEDANG"
    else       -> "RINGAN"
}

private enum class DisciplineFilter(val label: String) {
    SEMUA("Semua"),
    RINGAN("Ringan"),
    SEDANG("Sedang"),
    BERAT("Berat")
}

private fun PelanggaranSantri.matchesFilter(filter: DisciplineFilter): Boolean {
    val poin = poin ?: 0
    return when (filter) {
        DisciplineFilter.SEMUA  -> true
        DisciplineFilter.RINGAN -> poin < 10
        DisciplineFilter.SEDANG -> poin in 10..19
        DisciplineFilter.BERAT  -> poin >= 20
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  SCREEN ROOT — LOGIKA IDENTIK
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PelanggaranScreen(
    navController: NavController,
    viewModel    : SantriActivityViewModel,
    santriNis    : String
) {
    val pelanggaranList by viewModel.pelanggaranState.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val totalPoin        = pelanggaranList.sumOf { it.poin ?: 0 }
    val isDark           = isAppInDarkTheme()
    var showAllHistory by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(DisciplineFilter.SEMUA) }
    val filteredPelanggaran = remember(pelanggaranList, activeFilter) {
        pelanggaranList.filter { it.matchesFilter(activeFilter) }
    }

    LaunchedEffect(key1 = santriNis) {
        viewModel.loadAllData(santriNis)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DisciplineAmbientBackground(isDark = isDark)

        if (isLoading && pelanggaranList.isEmpty()) {
            CircularProgressIndicator(
                modifier    = Modifier.align(Alignment.Center),
                color       = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    DisciplineHeroHeader(
                        onBack = { navController.popBackStack() },
                        isDark = isDark
                    )
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        DisciplineSummaryHeader(totalPoin = totalPoin, isDark = isDark)
                    }
                }

                if (pelanggaranList.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            DisciplineFilterRow(
                                activeFilter = activeFilter,
                                onFilterChange = {
                                    activeFilter = it
                                    showAllHistory = false
                                },
                                isDark = isDark
                            )
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Column {
                            Spacer(Modifier.height(14.dp))
                            ViolationSectionLabel()
                        }
                    }
                }

                if (filteredPelanggaran.isEmpty()) {
                    item { EmptyStateMessage() }
                } else {
                    val visiblePelanggaran = if (showAllHistory || filteredPelanggaran.size <= 5) {
                        filteredPelanggaran
                    } else {
                        filteredPelanggaran.take(5)
                    }
                    items(visiblePelanggaran) { pelanggaran ->
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            PelanggaranNeonCard(pelanggaran = pelanggaran, isDark = isDark)
                        }
                    }
                    if (filteredPelanggaran.size > 5) {
                        item {
                            ActivityShowMoreButton(
                                expanded = showAllHistory,
                                hiddenCount = (filteredPelanggaran.size - 5).coerceAtLeast(0),
                                onClick = { showAllHistory = !showAllHistory }
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun DisciplineFilterRow(
    activeFilter: DisciplineFilter,
    onFilterChange: (DisciplineFilter) -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DisciplineFilter.entries.forEach { filter ->
            DisciplineFilterChip(
                label = filter.label,
                selected = filter == activeFilter,
                isDark = isDark,
                onClick = { onFilterChange(filter) }
            )
        }
    }
}

@Composable
private fun DisciplineFilterChip(
    label: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.10f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.34f else 0.78f)
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = background,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) color.copy(alpha = 0.46f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                color = color.copy(alpha = if (selected) 0.95f else 0.62f),
                fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun DisciplineAmbientBackground(isDark: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(primary.copy(alpha = if (isDark) 0.08f else 0.05f), Color.Transparent),
                center = Offset(size.width / 2f, 80.dp.toPx()),
                radius = size.width * 0.70f
            ),
            center = Offset(size.width / 2f, 80.dp.toPx()),
            radius = size.width * 0.70f
        )
    }
}

@Composable
private fun DisciplineHeroHeader(
    onBack: () -> Unit,
    isDark: Boolean
) {
    AppPageHeader(
        title = "CATATAN KEDISIPLINAN",
        isDark = isDark,
        onBack = onBack,
        size = AppPageHeaderSize.Comfortable,
        titleTopPadding = 58.dp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  DISCIPLINE SUMMARY HEADER
//
// Sebelumnya:
//   • Blur Box 80dp untuk glow → layout jump, tidak reliable di semua device
//   • Angka poin mengambang tanpa konteks visual yang kuat
//   • Progress bar tanpa threshold marker → wali tidak tahu batas aman/bahaya
//   • Desain: widget dashboard biasa, tidak ada karakter
//
// Sekarang:
//   • Arc gauge (Canvas) dengan StrokeCap.Round — seperti speedometer presisi
//   • Poin besar di tengah arc dengan label kecil di bawahnya
//   • Tiga threshold marker di progress bar (0 · 20 · 50 · 100)
//   • Gradient background card berdasarkan severity (sangat subtle)
//   • Pill badge status dengan warna semantis
//   • Left accent strip matching severity color (konsisten dengan card system)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DisciplineSummaryHeader(totalPoin: Int, isDark: Boolean) {
    val accentColor   = severityColor(totalPoin)
    val statusText    = statusLabel(totalPoin)
    val progressValue = (totalPoin / 100f).coerceIn(0f, 1f)
    val progressAngle = progressValue * 270f

    // ── Animated values ─────────────────────────────────────────────────
    val animatableSweep = remember { Animatable(0f) }
    val animatablePoin  = remember { Animatable(0f) }
    val animatableProgress = remember { Animatable(0f) }

    LaunchedEffect(totalPoin) {
        coroutineScope {
            launch {
                animatableSweep.animateTo(
                    targetValue   = progressAngle,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
                )
            }
            launch {
                animatablePoin.animateTo(
                    targetValue   = totalPoin.toFloat(),
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                )
            }
            launch {
                animatableProgress.animateTo(
                    targetValue   = progressValue,
                    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    val currentSweep     by animatableSweep.asState()
    val currentPoin      = animatablePoin.value.toInt()
    val currentProgress  by animatableProgress.asState()

    val borderColor    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.40f)
    val cardBackground = MaterialTheme.colorScheme.surface

    // Subtle pulse on arc alpha
    val infiniteTransition = rememberInfiniteTransition(label = "arc_pulse")
    val arcAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue  = 1.00f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arcAlpha"
    )

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(20.dp)),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isDark) 0.08f else 0.05f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier             = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment    = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(
                            accentColor,
                            RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                        )
                )

                Row(
                    modifier             = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ── Arc gauge (animated sweep) ────────────────────────────
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier.size(90.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokePx    = 7.dp.toPx()
                            val strokeStyle = Stroke(strokePx, cap = StrokeCap.Round)
                            val trackColor  = accentColor.copy(alpha = 0.12f)

                            drawArc(
                                color      = trackColor,
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter  = false,
                                style      = strokeStyle
                            )
                            if (currentSweep > 0f) {
                                drawArc(
                                    color      = accentColor.copy(alpha = arcAlpha),
                                    startAngle = 135f,
                                    sweepAngle = currentSweep,
                                    useCenter  = false,
                                    style      = strokeStyle
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text  = currentPoin.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color      = accentColor,
                                    fontSize   = 28.sp
                                )
                            )
                            Text(
                                text  = "POIN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color         = accentColor.copy(alpha = 0.65f),
                                    letterSpacing = 1.2.sp,
                                    fontSize      = 8.sp,
                                    fontWeight    = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // ── Info kanan ────────────────────────────────────────────
                    Column(
                        modifier            = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text  = "SKOR KEDISIPLINAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                letterSpacing = 1.2.sp,
                                fontSize      = 9.sp,
                                fontWeight    = FontWeight.Medium
                            )
                        )

                        Text(
                            text  = statusText,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color      = accentColor
                            )
                        )

                        // ── Progress bar (animated) ─────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress      = currentProgress,
                                modifier      = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape),
                                color         = accentColor,
                                trackColor    = accentColor.copy(alpha = 0.12f)
                            )
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("0", "20", "50", "100").forEach { label ->
                                    Text(
                                        text  = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                            fontSize = 8.sp
                                        )
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accentColor.copy(alpha = if (isDark) 0.15f else 0.10f)
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(accentColor, CircleShape)
                                )
                                Text(
                                    text  = statusText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color      = accentColor,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize   = 9.sp
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
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  SECTION LABEL — konsisten dengan DrawerSectionLabel & SantriDetailScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ViolationSectionLabel() {
    Row(
        modifier          = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(12.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.70f),
                    RoundedCornerShape(2.dp)
                )
        )
        Text(
            text  = "RIWAYAT PELANGGARAN",
            style = MaterialTheme.typography.labelSmall.copy(
                color         = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f),
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp,
                fontSize      = 9.5.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  VIOLATION CARD  (tetap bernama PelanggaranNeonCard agar kompatibel)
//
// Sebelumnya:
//   • Circle icon Warning sebagai severity indicator — generic Material, tidak berkesan
//   • Semua card identik secara visual — tidak ada perbedaan berat/sedang/ringan
//   • Layout Row tunggal: icon | info | poin — sangat padat dan tidak bernapas
//   • Tanggal di paling bawah dengan alpha rendah — sulit dibaca
//   • Nama pelanggaran ALL CAPS ExtraBold — terlalu agresif secara tipografi
//   • Tidak ada visual hierarchy yang jelas
//
// Sekarang:
//   • LEFT SEVERITY STRIP: bar vertikal tebal (5dp) berwarna severity —
//     wali langsung tahu bobot pelanggaran dari warna di tepi kiri card
//   • TOP ROW: tanggal pill (kanan) + severity badge pill (kiri) — konteks segera terlihat
//   • Nama pelanggaran: SemiBold bukan ExtraBold — tegas tapi tidak teriak
//   • Poin badge: pill dengan background tinted, bukan angka mentah
//   • Hukuman & catatan: dengan label muted di atas, value tegas di bawah
//     (stacked field, konsisten dengan InfoField di SantriDetailScreen)
//   • Hairline divider memisahkan zona info utama dari detail hukuman
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PelanggaranNeonCard(pelanggaran: PelanggaranSantri, isDark: Boolean) {
    val poin        = pelanggaran.poin ?: 0
    val accentColor = when {
        poin >= 20 -> WarningCrimson
        poin >= 10 -> WarningAmber
        else       -> MaterialTheme.colorScheme.primary
    }
    val svLabel     = severityLabel(poin)
    val hasDetail   = !pelanggaran.hukuman.isNullOrBlank() || !pelanggaran.catatan.isNullOrBlank()

    // ── Expand/Collapse state ───────────────────────────────────────────
    var expanded by remember { mutableStateOf(false) }

    // ── Press animation ─────────────────────────────────────────────────
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "cardScale"
    )

    // ── Chevron rotation ────────────────────────────────────────────────
    val chevronRotation by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "chevron"
    )

    val borderColor    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.40f)
    val cardBackground = MaterialTheme.colorScheme.surface
    val divColor       = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = hasDetail
            ) { if (hasDetail) expanded = !expanded }
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .animateContentSize(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

            // ── Left severity strip ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        accentColor,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )

            // ── Content ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Baris atas: severity badge + tanggal ──────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accentColor.copy(alpha = if (isDark) 0.18f else 0.10f)
                    ) {
                        Text(
                            text     = svLabel,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            style    = MaterialTheme.typography.labelSmall.copy(
                                color         = accentColor,
                                fontWeight    = FontWeight.Bold,
                                fontSize      = 9.sp,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    ) {
                        Text(
                            text     = pelanggaran.tanggal ?: "—",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            style    = MaterialTheme.typography.labelSmall.copy(
                                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // ── Nama pelanggaran + poin badge ─────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top
                ) {
                    Text(
                        text     = pelanggaran.jenis_pelanggaran ?: "Pelanggaran",
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        style    = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = if (isDark) 0.18f else 0.10f)
                    ) {
                        Column(
                            modifier            = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text  = "+$poin",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color      = accentColor,
                                    fontSize   = 18.sp
                                )
                            )
                            Text(
                                text  = "poin",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color    = accentColor.copy(alpha = 0.65f),
                                    fontSize = 8.sp
                                )
                            )
                        }
                    }
                }

                // ── Chevron indicator (jika ada detail) ───────────────────────
                if (hasDetail) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector       = Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Sembunyikan detail" else "Tampilkan detail",
                            tint              = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
                            modifier          = Modifier
                                .size(18.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }

                // ── Detail section (expandable) ───────────────────────────────
                AnimatedVisibility(
                    visible = expanded && hasDetail,
                    enter  = fadeIn(tween(200)) + expandVertically(tween(250)),
                    exit   = fadeOut(tween(150)) + shrinkVertically(tween(200))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        HorizontalDivider(color = divColor, thickness = 0.8.dp)

                        // Hukuman
                        if (!pelanggaran.hukuman.isNullOrBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text  = "HUKUMAN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                        fontSize      = 8.5.sp,
                                        letterSpacing = 0.8.sp,
                                        fontWeight    = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text  = pelanggaran.hukuman,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        // Catatan
                        if (!pelanggaran.catatan.isNullOrBlank()) {
                            Row(
                                verticalAlignment     = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector       = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier          = Modifier
                                        .size(12.dp)
                                        .padding(top = 2.dp),
                                    tint              = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                )
                                Text(
                                    text  = pelanggaran.catatan,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                                        fontStyle  = FontStyle.Italic,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  EMPTY STATE
//
// Sebelumnya:
//   • Icons.Default.Info — semantik salah untuk konteks "tidak ada pelanggaran"
//   • Teks UPPERCASE tanpa breathing room
//   • Tidak ada nuansa islami / konteks pesantren
//
// Sekarang:
//   • Icons.Default.CheckCircle dengan warna IslamicEmerald — bermakna positif
//   • Container card dengan border emerald subtle
//   • Kutipan karakter pesantren yang hangat dan relevan
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun EmptyStateMessage() {
    val isDark = isAppInDarkTheme()

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(
                width  = 1.dp,
                color  = IslamicEmerald.copy(alpha = if (isDark) 0.25f else 0.18f),
                shape  = RoundedCornerShape(20.dp)
            ),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = IslamicEmerald.copy(alpha = if (isDark) 0.07f else 0.04f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon dalam lingkaran emerald
            Box(
                modifier         = Modifier
                    .size(64.dp)
                    .background(IslamicEmerald.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector       = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint              = IslamicEmerald,
                    modifier          = Modifier.size(32.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text      = "Tidak Ada Catatan Pelanggaran",
                    style     = MaterialTheme.typography.titleSmall.copy(
                        color      = IslamicEmerald,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text      = "Alhamdulillah, santri senantiasa\nmenjaga akhlak dan kedisiplinannya.",
                    style     = MaterialTheme.typography.bodySmall.copy(
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Ornamen bawah
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Canvas(Modifier.size(3.dp)) { drawCircle(IslamicEmerald.copy(alpha = 0.35f)) }
                Canvas(Modifier.size(4.5.dp)) { drawCircle(IslamicEmerald.copy(alpha = 0.55f)) }
                Canvas(Modifier.size(3.dp)) { drawCircle(IslamicEmerald.copy(alpha = 0.35f)) }
            }
        }
    }
}

@Composable
private fun ActivityShowMoreButton(
    expanded: Boolean,
    hiddenCount: Int,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    ) {
        Text(
            text = if (expanded) "Sembunyikan riwayat" else "Lihat $hiddenCount catatan lainnya",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}
