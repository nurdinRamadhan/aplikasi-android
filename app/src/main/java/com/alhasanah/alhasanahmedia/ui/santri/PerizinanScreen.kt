package com.alhasanah.alhasanahmedia.ui.santri

// ─────────────────────────────────────────────────────────────────────────────
// Imports
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.PerizinanSantri
import com.alhasanah.alhasanahmedia.ui.components.AppGradientBackground
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import com.alhasanah.alhasanahmedia.util.StatusApproved
import com.alhasanah.alhasanahmedia.util.StatusPending
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Filter Enum
// ─────────────────────────────────────────────────────────────────────────────

private enum class IzinFilter(val label: String) {
    SEMUA("Semua"),
    DISETUJUI("Disetujui"),
    DIPROSES("Diproses"),
    DITOLAK("Ditolak"),
    KEMBALI("Kembali")
}

private fun PerizinanSantri.matchesFilter(filter: IzinFilter): Boolean = when (filter) {
    IzinFilter.SEMUA      -> true
    IzinFilter.DISETUJUI  -> status?.lowercase().let { it == "disetujui" || it == "approved" }
    IzinFilter.DIPROSES   -> status?.lowercase().let { it == "diproses" || it == "pending" }
    IzinFilter.DITOLAK    -> status?.lowercase().let { it == "ditolak" || it == "rejected" }
    IzinFilter.KEMBALI    -> !tanggal_kembali.isNullOrBlank()
}

// ─────────────────────────────────────────────────────────────────────────────
// Status Style Helper
// ─────────────────────────────────────────────────────────────────────────────

private data class StatusStyle(
    val label: String,
    val color: Color,
    val icon: ImageVector
)

@Composable
private fun statusStyle(status: String): StatusStyle = when (status.lowercase()) {
    "disetujui", "approved" -> StatusStyle("DISETUJUI", StatusApproved,                    Icons.Default.CheckCircle)
    "diproses", "pending"   -> StatusStyle("DIPROSES",  StatusPending,                     Icons.Default.Schedule)
    "ditolak", "rejected"   -> StatusStyle("DITOLAK",   MaterialTheme.colorScheme.error,   Icons.Default.Cancel)
    else                    -> StatusStyle(status.uppercase(), MaterialTheme.colorScheme.outline, Icons.Default.Info)
}

// ─────────────────────────────────────────────────────────────────────────────
// PerizinanScreen — Root
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PerizinanScreen(
    navController: NavController,
    viewModel: SantriActivityViewModel,
    santriNis: String
) {
    val perizinanList by viewModel.perizinanState.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val isDark        = isAppInDarkTheme()

    var activeFilter by remember { mutableStateOf(IzinFilter.SEMUA) }
    var showAllHistory by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = santriNis) {
        viewModel.loadAllData(santriNis)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppGradientBackground(isDark = isDark)
        PerizinanIslamicBackground()

        if (isLoading && perizinanList.isEmpty()) {
            PermitSkeletonLoading()
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                // ── 1. Gradient Header (Full Width) ───────────────────────
                item {
                    PermitGradientHeader(
                        onBack        = { navController.popBackStack() },
                        perizinanList = perizinanList,
                        activeFilter  = activeFilter,
                        onFilterChange = {
                            activeFilter = it
                            showAllHistory = false
                        }
                    )
                }

                // ── 2. Section Label + Count ──────────────────────────────
                item {
                    val filteredCount = perizinanList.count { it.matchesFilter(activeFilter) }
                    PermitSectionLabel(
                        text  = "RIWAYAT IZIN SANTRI",
                        count = filteredCount,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                    )
                }

                // ── 3. List / Empty ───────────────────────────────────────
                val filtered = perizinanList.filter { it.matchesFilter(activeFilter) }

                if (filtered.isEmpty()) {
                    item { PermitEmptyState(filter = activeFilter) }
                } else {
                    val visible = if (showAllHistory || filtered.size <= 5) filtered else filtered.take(5)
                    itemsIndexed(
                        items = visible,
                        key   = { i, it -> "${it.id ?: i}_${it.tanggal}_${it.jenis_izin}_${it.status}" }
                    ) { i, perizinan ->
                        PermitGlassCard(
                            perizinan = perizinan,
                            modifier  = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )
                    }
                    if (filtered.size > 5) {
                        item {
                            PermitHistoryToggleButton(
                                expanded = showAllHistory,
                                hiddenCount = (filtered.size - 5).coerceAtLeast(0),
                                onClick = { showAllHistory = !showAllHistory }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Background — Islamic Geometric Dots (Consistent with Tahfidz)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PerizinanIslamicBackground() {
    val color   = MaterialTheme.colorScheme.primary.copy(alpha = 0.035f)
    val infiniteTransition = rememberInfiniteTransition(label = "perizinanBgRot")
    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(180_000, easing = LinearEasing)),
        label         = "perizinanBgRotVal"
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
// PermitGradientHeader — Refactored to eliminate side gutters
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermitGradientHeader(
    onBack: () -> Unit,
    perizinanList: List<PerizinanSantri>,
    activeFilter: IzinFilter,
    onFilterChange: (IzinFilter) -> Unit
) {
    val isDark  = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val titleColor = if (isDark) primary.copy(0.92f) else Color(0xFF8B6914)
    val subtitleColor = if (isDark) Color.White.copy(0.76f) else MaterialTheme.colorScheme.onSurfaceVariant

    // Stats computation
    val totalCount     = perizinanList.size
    val disetujuiCount = perizinanList.count { it.status?.lowercase().let { s -> s == "disetujui" || s == "approved" } }
    val diprosesCount  = perizinanList.count { it.status?.lowercase().let { s -> s == "diproses" || s == "pending" } }
    val ditolakCount   = perizinanList.count { it.status?.lowercase().let { s -> s == "ditolak" || s == "rejected" } }
    val kembaliCount   = perizinanList.count { !it.tanggal_kembali.isNullOrBlank() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
    ) {
        AppPageHeaderBackground(isDark = isDark, modifier = Modifier.matchParentSize())

        Column(
            modifier            = Modifier.fillMaxWidth()
        ) {
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
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint               = if (isDark) Color.White.copy(0.86f) else Color(0xFF2B2418),
                            modifier           = Modifier.size(22.dp)
                        )
                    }
                }
                Box(modifier = Modifier.size(46.dp))
            }

            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text          = "PERIZINAN SANTRI",
                    style         = MaterialTheme.typography.titleLarge.copy(
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 1.4.sp,
                        color         = titleColor
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = "Monitoring Keluar Masuk Santri",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color      = subtitleColor,
                        fontWeight = FontWeight.Normal
                    )
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                PermitHeroDivider(isDark = isDark)
                Spacer(modifier = Modifier.height(18.dp))

                // ── Stats Summary Card (Glass) ─────────────────────────────
                PermitStatsCard(
                    total     = totalCount,
                    disetujui = disetujuiCount,
                    diproses  = diprosesCount,
                    ditolak   = ditolakCount,
                    isDark    = isDark
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Filter Row ─────────────────────────────────────────────
                PermitFilterChipRow(
                    activeFilter   = activeFilter,
                    onFilterChange = onFilterChange,
                    totalCount     = totalCount,
                    disetujuiCount = disetujuiCount,
                    diprosesCount  = diprosesCount,
                    ditolakCount   = ditolakCount,
                    kembaliCount   = kembaliCount
                )
            }
        }
    }
}

@Composable
private fun PermitHeroDivider(isDark: Boolean) {
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
// PermitStatsCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermitStatsCard(
    total: Int,
    disetujui: Int,
    diproses: Int,
    ditolak: Int,
    isDark: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.58f else 0.84f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.50f),
                        primary.copy(alpha = 0.12f),
                        primary.copy(alpha = 0.34f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                text  = "RINGKASAN STATUS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color         = primary,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                PermitStatMetric(label = "TOTAL", value = total.toString(), color = contentColor, labelColor = mutedColor)
                Box(modifier = Modifier.width(0.5.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)))
                PermitStatMetric(label = "DISETUJUI", value = disetujui.toString(), color = StatusApproved, labelColor = mutedColor)
                Box(modifier = Modifier.width(0.5.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)))
                PermitStatMetric(label = "DIPROSES", value = diproses.toString(), color = StatusPending, labelColor = mutedColor)
                Box(modifier = Modifier.width(0.5.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)))
                PermitStatMetric(label = "DITOLAK", value = ditolak.toString(), color = MaterialTheme.colorScheme.error, labelColor = mutedColor)
            }
        }
    }
}

@Composable
private fun PermitStatMetric(label: String, value: String, color: Color, labelColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                color      = color
            )
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color    = labelColor,
                fontSize = 7.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PermitFilterChipRow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermitFilterChipRow(
    activeFilter: IzinFilter,
    onFilterChange: (IzinFilter) -> Unit,
    totalCount: Int,
    disetujuiCount: Int,
    diprosesCount: Int,
    ditolakCount: Int,
    kembaliCount: Int
) {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IzinFilter.entries.forEach { filter ->
            val isActive = filter == activeFilter
            val count = when (filter) {
                IzinFilter.SEMUA -> totalCount
                IzinFilter.DISETUJUI -> disetujuiCount
                IzinFilter.DIPROSES -> diprosesCount
                IzinFilter.DITOLAK -> ditolakCount
                IzinFilter.KEMBALI -> kembaliCount
            }

            val chipColor = when (filter) {
                IzinFilter.DISETUJUI -> StatusApproved
                IzinFilter.DIPROSES  -> StatusPending
                IzinFilter.DITOLAK   -> MaterialTheme.colorScheme.error
                IzinFilter.KEMBALI   -> MaterialTheme.colorScheme.secondary
                else                 -> primary
            }

            Surface(
                shape  = CircleShape,
                color  = if (isActive) {
                    chipColor
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.88f else 0.94f)
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isActive) {
                        chipColor.copy(alpha = 0.72f)
                    } else {
                        primary.copy(alpha = if (isDark) 0.32f else 0.24f)
                    }
                ),
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .widthIn(min = 96.dp)
                    .clickable { onFilterChange(filter) }
            ) {
                Text(
                    text  = "${filter.label} ($count)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color      = if (isActive) {
                            if (filter == IzinFilter.SEMUA) MaterialTheme.colorScheme.onPrimary else Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    ),
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PermitSectionLabel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PermitSectionLabel(
    text: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(
                        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))),
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text  = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color         = MaterialTheme.colorScheme.onBackground
                )
            )
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
            Text(
                text = "$count data",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PermitGlassCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PermitGlassCard(
    perizinan: PerizinanSantri,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.978f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "permitCardScale"
    )

    val isDark      = isAppInDarkTheme()
    val status      = perizinan.status?.lowercase() ?: ""
    val style       = statusStyle(status)
    val accentColor = style.color

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = {}),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.62f else 0.86f)),
        border    = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.30f))), RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
            )

            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = accentColor.copy(alpha = 0.10f)) {
                        Text(
                            text = (perizinan.jenis_izin ?: "IZIN").uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(color = accentColor, fontWeight = FontWeight.Black, fontSize = 9.sp)
                        )
                    }
                    PermitStatusBadge(style = style)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = perizinan.keterangan ?: "Tanpa keterangan",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    PermitDateCell(label = "Berangkat", date = perizinan.tanggal ?: "—", icon = Icons.Outlined.FlightTakeoff)
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(14.dp))
                    PermitDateCell(label = "Kembali", date = perizinan.tanggal_kembali ?: "—", icon = Icons.Outlined.FlightLand, alignEnd = true)
                }
            }
        }
    }
}

@Composable
private fun PermitDateCell(label: String, date: String, icon: ImageVector, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!alignEnd) Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(11.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp))
            if (alignEnd) Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(11.dp).padding(start = 4.dp))
        }
        Text(text = date, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
    }
}

@Composable
private fun PermitStatusBadge(style: StatusStyle) {
    Surface(shape = RoundedCornerShape(20.dp), color = style.color.copy(alpha = 0.10f)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(style.icon, null, tint = style.color, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = style.label, style = MaterialTheme.typography.labelSmall.copy(color = style.color, fontWeight = FontWeight.Black, fontSize = 8.sp))
        }
    }
}

@Composable
private fun PermitEmptyState(filter: IzinFilter) {
    val primary = MaterialTheme.colorScheme.primary
    Column(modifier = Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(80.dp).background(Brush.radialGradient(listOf(primary.copy(0.12f), Color.Transparent)), CircleShape))
            Icon(Icons.AutoMirrored.Outlined.AssignmentReturn, null, tint = primary.copy(0.65f), modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Belum Ada Riwayat Izin", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Text(text = "Data perizinan santri akan tampil di sini.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PermitHistoryToggleButton(expanded: Boolean, hiddenCount: Int, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))) {
        Text(text = if (expanded) "Sembunyikan riwayat" else "Lihat $hiddenCount izin lainnya", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black))
    }
}

@Composable
private fun PermitSkeletonLoading() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))) }
        items(4) { Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))) }
    }
}
