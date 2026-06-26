package com.alhasanah.alhasanahmedia.ui.santri

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhasanah.alhasanahmedia.data.model.AbsensiHarianItem
import com.alhasanah.alhasanahmedia.data.model.RingkasanAbsensiMingguan
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Brand palette ────────────────────────────────────────────────────────────
private val GoldPrimary = Color(0xFFC9A84C)
private val GoldDark    = Color(0xFF8B6E23)
private val HadirColor  = Color(0xFF16A34A)
private val AlphaColor  = Color(0xFFDC2626)
private val SakitColor  = Color(0xFFD97706)
private val IzinColor    = Color(0xFF2563EB)
private val SekolahColor = Color(0xFF7C3AED)
private val PulangColor  = Color(0xFF0891B2)
private val LainColor    = Color(0xFF6B7280)

// ─── Day label map ─────────────────────────────────────────────────────────────
private val DAY_LABEL = mapOf(
    DayOfWeek.MONDAY    to "Sen",
    DayOfWeek.TUESDAY   to "Sel",
    DayOfWeek.WEDNESDAY to "Rab",
    DayOfWeek.THURSDAY  to "Kam",
    DayOfWeek.FRIDAY    to "Jum",
    DayOfWeek.SATURDAY  to "Sab",
    DayOfWeek.SUNDAY    to "Ahd",
)
private val ORDERED_DOWS = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
)

// ══════════════════════════════════════════════════════════════════════════════
//  MAIN SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AbsensiScreen(
    santriNis: String,
    viewModel: AbsensiViewModel,
    onBack: () -> Unit,
) {
    val uiState       by viewModel.uiState.collectAsState()
    val isDark        = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()
    val weekStart     = viewModel.getCurrentWeekStart()
    val displayPeriod = viewModel.getWeekStartForDisplay(weekStart)

    val tabTitles  = listOf("Semua", "Tahfidz", "Mingguan", "Ngaji", "Sholat Hifdzi")
    val pagerState = rememberPagerState { tabTitles.size }

    LaunchedEffect(santriNis) {
        viewModel.loadWeeklySummary(santriNis, weekStart)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                // ── Preserved ornament header ──────────────────────────────
                AppPageHeader(
                    title    = "RINGKASAN ABSENSI",
                    subtitle = displayPeriod,
                    isDark   = isDark,
                    onBack   = onBack,
                    size     = AppPageHeaderSize.Compact,
                )

                // ── Gold-accented tab row ──────────────────────────────────
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding      = 0.dp,
                    containerColor   = MaterialTheme.colorScheme.surface,
                    contentColor     = GoldPrimary,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            Box(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                    .height(2.5.dp)
                                    .padding(horizontal = 18.dp)
                                    .background(
                                        GoldPrimary,
                                        RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                                    ),
                            )
                        }
                    },
                    divider = {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        )
                    },
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        val selected = pagerState.currentPage == index
                        Tab(
                            selected = selected,
                            onClick  = {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Text(
                                    text       = title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize   = 13.sp,
                                    color      = if (selected) GoldPrimary
                                                 else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val state = uiState) {
                is AbsensiUiState.Loading -> SkeletonLoadingState()

                is AbsensiUiState.Error -> {
                    Column(
                        modifier             = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment  = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier            = Modifier
                                .size(64.dp)
                                .background(AlphaColor.copy(alpha = 0.1f), CircleShape),
                            contentAlignment    = Alignment.Center,
                        ) {
                            Text("✕", fontSize = 28.sp, color = AlphaColor)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text      = state.message,
                            style     = MaterialTheme.typography.bodyLarge,
                            color     = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text("Coba Lagi", color = GoldPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                is AbsensiUiState.Success -> {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        when (page) {
                            0 -> AbsensiMingguanAll(summary = state.summary, viewModel = viewModel)
                            1 -> AbsensiByKegiatan(summary = state.summary, viewModel = viewModel,
                                    filterKegiatan = setOf("ZIYADAH", "MUROJAAH"))
                            2 -> AbsensiByKegiatan(summary = state.summary, viewModel = viewModel,
                                    filterKegiatan = setOf("ISTIGHOSAH", "NGAOS", "TILAWAH",
                                        "TAWASUL", "MUHADHOROH", "MHQ", "HAFALAN"))
                            3 -> AbsensiByKegiatan(summary = state.summary, viewModel = viewModel,
                                    filterKegiatan = setOf("NGAJI"))
                            4 -> AbsensiByKegiatan(summary = state.summary, viewModel = viewModel,
                                    filterKegiatan = setOf("SHOLAT HIFDZI"))
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  TAB CONTENT
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AbsensiMingguanAll(
    summary: RingkasanAbsensiMingguan,
    viewModel: AbsensiViewModel,
) {
    if (summary.data.isEmpty()) { EmptyStateAbsensi(); return }

    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { AbsensiRingkasanCard(items = summary.data) }
        item { WeeklyTrendCard(items = summary.data) }
        items(
            summary.data.groupBy { it.tanggal }.toList().sortedBy { it.first },
        ) { (tanggal, items) ->
            AbsensiHarianCard(
                hari     = items.first().hari,
                tanggal  = tanggal,
                items    = items,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun AbsensiByKegiatan(
    summary: RingkasanAbsensiMingguan,
    viewModel: AbsensiViewModel,
    filterKegiatan: Set<String>,
) {
    val filtered = summary.data.filter { item ->
        filterKegiatan.any { item.kegiatan.uppercase().contains(it) }
    }

    if (filtered.isEmpty()) {
        EmptyStateAbsensi("Tidak ada data kegiatan untuk kategori ini.")
        return
    }

    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { AbsensiRingkasanCard(items = filtered) }
        item { WeeklyTrendCard(items = filtered) }
        items(
            filtered.groupBy { it.tanggal }.toList().sortedBy { it.first },
        ) { (tanggal, items) ->
            AbsensiHarianCard(
                hari     = items.first().hari,
                tanggal  = tanggal,
                items    = items,
                viewModel = viewModel,
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  RINGKASAN CARD  (donut + stat bars)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AbsensiRingkasanCard(items: List<AbsensiHarianItem>) {
    val hadir   = items.count { it.status.uppercase() == "HADIR" }
    val alpha   = items.count { it.status.uppercase() == "ALPHA" }
    val sakit   = items.count { it.status.uppercase() == "SAKIT" }
    val izin    = items.count { it.status.uppercase() == "IZIN"  }
    val sekolah = items.count { it.status.uppercase() == "SEKOLAH" }
    val pulang  = items.count { it.status.uppercase() == "PULANG" }
    val lainnya = items.size - (hadir + alpha + sakit + izin + sekolah + pulang)
    val total   = items.size

    ElevatedCard(
        modifier   = Modifier.fillMaxWidth(),
        elevation  = CardDefaults.cardElevation(2.dp),
        colors     = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surface),
        shape      = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .drawBehind {
                    // Subtle gold glow in top-right corner
                    drawCircle(
                        brush  = Brush.radialGradient(
                            colors    = listOf(GoldPrimary.copy(alpha = 0.07f), Color.Transparent),
                            center    = Offset(size.width, 0f),
                            radius    = size.width * 0.65f,
                        ),
                    )
                },
        ) {
            // ── Card header ──────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .background(GoldPrimary.copy(alpha = 0.13f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("◈", fontSize = 17.sp, color = GoldPrimary)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Ringkasan Kehadiran",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "$total kegiatan tercatat",
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Donut + stat bars ────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Animated donut
                Box(
                    modifier         = Modifier.size(115.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AttendanceDonut(
                        hadir   = hadir, alpha = alpha, sakit = sakit,
                        izin    = izin, sekolah = sekolah, pulang = pulang,
                        lainnya = lainnya,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(Modifier.width(18.dp))

                // Animated stat bars
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    StatBar("Hadir",   hadir,   total, HadirColor)
                    StatBar("Alpha",   alpha,   total, AlphaColor)
                    StatBar("Sakit",   sakit,   total, SakitColor)
                    StatBar("Izin",    izin,    total, IzinColor)
                    StatBar("Sekolah", sekolah, total, SekolahColor)
                    StatBar("Pulang",  pulang,  total, PulangColor)
                    if (lainnya > 0) StatBar("Lainnya", lainnya, total, LainColor)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))

            // ── Bottom summary strip ─────────────────────────────────────
            Row(
                modifier                = Modifier.fillMaxWidth(),
                horizontalArrangement   = Arrangement.SpaceAround,
            ) {
                BottomKpi(
                    label = "Total",
                    value = "$total",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BottomKpi(
                    label = "Kehadiran",
                    value = if (total > 0) "${(hadir * 100 / total)}%" else "0%",
                    color = HadirColor,
                )
                BottomKpi(
                    label = "Tidak Hadir",
                    value = "${total - hadir}",
                    color = AlphaColor,
                )
            }
        }
    }
}

// ── Animated donut chart ─────────────────────────────────────────────────────

@Composable
private fun AttendanceDonut(
    hadir: Int, alpha: Int, sakit: Int, izin: Int,
    sekolah: Int, pulang: Int, lainnya: Int,
    modifier: Modifier = Modifier,
) {
    val total    = (hadir + alpha + sakit + izin + sekolah + pulang + lainnya).coerceAtLeast(1)
    val hadirPct = hadir.toFloat() / total

    var trigger by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue     = if (trigger) 1f else 0f,
        animationSpec   = tween(1100, easing = FastOutSlowInEasing),
        label           = "donut_progress",
    )
    LaunchedEffect(Unit) { trigger = true }

    val segments = listOf(
        hadir   to HadirColor,
        alpha   to AlphaColor,
        sakit   to SakitColor,
        izin    to IzinColor,
        sekolah to SekolahColor,
        pulang  to PulangColor,
        lainnya to LainColor,
    ).filter { it.first > 0 }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = size.minDimension * 0.145f
            val radius  = (size.minDimension - strokeW) / 2f
            val tl      = Offset(center.x - radius, center.y - radius)
            val arcSz   = Size(radius * 2f, radius * 2f)

            // Background track
            drawArc(
                color       = Color.Gray.copy(alpha = 0.1f),
                startAngle  = 0f, sweepAngle = 360f,
                useCenter   = false, topLeft = tl, size = arcSz,
                style       = Stroke(strokeW, cap = StrokeCap.Round),
            )
            // Animated segments — all grow simultaneously from -90°
            var currentAngle = -90f
            segments.forEach { (count, color) ->
                val sweep = (count.toFloat() / total) * 360f * progress
                if (sweep > 0f) {
                    drawArc(
                        color       = color,
                        startAngle  = currentAngle,
                        sweepAngle  = sweep,
                        useCenter   = false, topLeft = tl, size = arcSz,
                        style       = Stroke(strokeW, cap = StrokeCap.Butt),
                    )
                }
                currentAngle += sweep
            }
        }

        // Animated percentage in centre
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = "${(hadirPct * 100f * progress).toInt()}%",
                fontSize   = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = HadirColor,
                lineHeight = 20.sp,
            )
            Text(
                text       = "Hadir",
                fontSize   = 10.sp,
                color      = LainColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ── Animated horizontal stat bar ─────────────────────────────────────────────

@Composable
private fun StatBar(label: String, count: Int, total: Int, color: Color) {
    val pct   = if (total > 0) count.toFloat() / total else 0f
    var shown by remember { mutableStateOf(false) }
    val fill  by animateFloatAsState(
        targetValue   = if (shown) pct else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label         = "bar_$label",
    )
    LaunchedEffect(Unit) { shown = true }
    val isDark = isSystemInDarkTheme()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            text     = label,
            fontSize = 12.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(46.dp),
        )
        // Bar track
        Box(
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (isDark) 0.12f else 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .height(5.dp)
                    .background(color, CircleShape),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text       = "$count",
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold,
            color      = color,
            modifier   = Modifier.width(22.dp),
            textAlign  = TextAlign.End,
        )
    }
}

// ── Bottom KPI item ───────────────────────────────────────────────────────────

@Composable
private fun BottomKpi(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = color)
        Text(
            label, fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  WEEKLY TREND BAR CHART
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WeeklyTrendCard(items: List<AbsensiHarianItem>) {
    // Build per-day hadir/total pairs
    val dayStats: Map<DayOfWeek, Pair<Int, Int>> = items
        .groupBy { runCatching { LocalDate.parse(it.tanggal).dayOfWeek }.getOrNull() }
        .filterKeys { it != null }
        .mapKeys { it.key!! }
        .mapValues { (_, dayItems) ->
            dayItems.count { it.status.uppercase() == "HADIR" } to dayItems.size
        }

    if (dayStats.isEmpty()) return

    var revealBars by remember { mutableStateOf(false) }
    val barReveal  by animateFloatAsState(
        targetValue   = if (revealBars) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label         = "bar_reveal",
    )
    LaunchedEffect(Unit) { revealBars = true }
    val isDark = isSystemInDarkTheme()

    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp),
        colors    = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surface),
        shape     = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header ────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier
                        .size(34.dp)
                        .background(GoldPrimary.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    // Mini sparkbar icon drawn with Canvas
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val bw = size.width / 5f
                        listOf(0.55f, 1f, 0.38f, 0.78f, 0.62f).forEachIndexed { i, h ->
                            drawRoundRect(
                                color        = GoldPrimary,
                                topLeft      = Offset(i * bw * 1.22f, size.height * (1f - h)),
                                size         = Size(bw * 0.85f, size.height * h),
                                cornerRadius = CornerRadius(2.dp.toPx()),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Tren Kehadiran Harian",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Persentase kehadiran per hari",
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Bar columns ───────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment     = Alignment.Bottom,
            ) {
                ORDERED_DOWS.forEach { dow ->
                    val (hadir, total) = dayStats[dow] ?: (0 to 0)
                    val rate      = if (total > 0) hadir.toFloat() / total else 0f
                    val hasData   = total > 0
                    val isGood    = rate >= 0.8f
                    val barColor  = if (isGood) HadirColor else GoldPrimary
                    val maxBarH   = 64.dp

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.weight(1f),
                    ) {
                        // Percent label above bar
                        Text(
                            text       = if (hasData) "${(rate * 100).toInt()}%" else "",
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = barColor,
                            textAlign  = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))

                        // Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                        ) {
                            // Track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(maxBarH)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isDark) Color.White.copy(alpha = 0.05f)
                                        else Color.Black.copy(alpha = 0.04f),
                                    ),
                            )
                            // Value bar (animated height)
                            val animH = maxBarH * rate.coerceIn(0f, 1f) * barReveal
                            if (hasData && animH > 0.dp) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(animH)
                                        .align(Alignment.BottomStart)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(barColor.copy(alpha = 0.75f), barColor),
                                            ),
                                        ),
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        // Day label
                        Text(
                            text       = DAY_LABEL[dow] ?: "",
                            fontSize   = 11.sp,
                            color      = if (hasData)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            fontWeight = if (hasData) FontWeight.Medium else FontWeight.Normal,
                            textAlign  = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Legend ────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(HadirColor, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "≥ 80% Hadir", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(GoldPrimary, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "< 80% Hadir", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  DAILY CARD  (redesigned header + rows)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AbsensiHarianCard(
    hari: String,
    tanggal: String,
    items: List<AbsensiHarianItem>,
    viewModel: AbsensiViewModel,
) {
    val parsedDate  = runCatching { LocalDate.parse(tanggal) }.getOrNull()
    val dayNumber   = parsedDate?.dayOfMonth?.toString() ?: "--"
    val monthAbbr   = parsedDate
        ?.format(DateTimeFormatter.ofPattern("MMM", Locale("id")))
        ?.uppercase() ?: ""
    val displayDate = parsedDate
        ?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id")))
        ?: tanggal
    val hadirCount  = items.count { it.status.uppercase() == "HADIR" }
    val allHadir    = hadirCount == items.size

    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp),
        colors    = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surface),
        shape     = RoundedCornerShape(16.dp),
    ) {
        Column {
            // ── Card header ───────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Gold gradient day-number box
                Box(
                    modifier         = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(GoldPrimary, GoldDark))),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = dayNumber,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 17.sp,
                            color      = Color.White,
                            lineHeight = 17.sp,
                        )
                        if (monthAbbr.isNotBlank()) {
                            Text(
                                text       = monthAbbr,
                                fontSize   = 8.sp,
                                color      = Color.White.copy(alpha = 0.85f),
                                lineHeight = 8.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = hari.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 14.sp,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text     = displayDate,
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Hadir/total pill badge
                Row(
                    modifier          = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (allHadir) HadirColor.copy(alpha = 0.12f)
                            else GoldPrimary.copy(alpha = 0.1f),
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (allHadir) HadirColor else GoldPrimary,
                                CircleShape,
                            ),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text       = "$hadirCount/${items.size}",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (allHadir) HadirColor else GoldPrimary,
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            )

            // ── Attendance rows ──────────────────────────────────────────
            Column(
                modifier            = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items.forEach { item ->
                    AbsensiTableRow(item = item, viewModel = viewModel)
                }
            }
        }
    }
}

// ── Attendance row with left-border accent ────────────────────────────────────

@Composable
private fun AbsensiTableRow(
    item: AbsensiHarianItem,
    viewModel: AbsensiViewModel,
) {
    val statusColor = Color(viewModel.getStatusColor(item.status))
    val isDark      = isSystemInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            // Tinted background
            .background(statusColor.copy(alpha = if (isDark) 0.09f else 0.05f))
            // Solid left border accent drawn behind content
            .drawBehind {
                drawRect(
                    color    = statusColor,
                    topLeft  = Offset.Zero,
                    size     = Size(4.dp.toPx(), size.height),
                )
            }
            .padding(start = 14.dp, top = 10.dp, end = 11.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = item.kegiatan,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.sesi.isNotBlank() && item.sesi != "-") {
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = item.sesi,
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Status badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(statusColor.copy(alpha = if (isDark) 0.22f else 0.13f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text           = viewModel.getStatusLabel(item.status),
                fontWeight     = FontWeight.ExtraBold,
                fontSize       = 10.sp,
                color          = statusColor,
                letterSpacing  = 0.5.sp,
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SKELETON LOADING
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SkeletonLoadingState() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val tx by transition.animateFloat(
        initialValue  = -600f,
        targetValue   = 1200f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label         = "shimmer_tx",
    )
    val shimmer = Brush.linearGradient(
        colors = listOf(
            Color.Gray.copy(alpha = 0.07f),
            Color.Gray.copy(alpha = 0.18f),
            Color.Gray.copy(alpha = 0.07f),
        ),
        start = Offset(tx, 0f),
        end   = Offset(tx + 400f, 120f),
    )

    @Composable
    fun SkeletonBox(height: Int, cornerDp: Int = 16) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(RoundedCornerShape(cornerDp.dp))
                .background(shimmer),
        )
    }

    LazyColumn(
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SkeletonBox(170, 20) }   // Ringkasan card
        item { SkeletonBox(150, 16) }   // Trend chart
        items(3) { SkeletonBox(115, 16) } // Daily cards
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  EMPTY STATE
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun EmptyStateAbsensi(text: String = "Belum ada data absensi untuk periode ini.") {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Illustrated calendar icon using Canvas
            Box(
                modifier         = Modifier
                    .size(88.dp)
                    .background(GoldPrimary.copy(alpha = 0.09f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(44.dp)) {
                    val r  = CornerRadius(4.dp.toPx())
                    val sw = 2.dp.toPx()
                    // Calendar outline
                    drawRoundRect(color = GoldPrimary.copy(alpha = 0.55f), cornerRadius = r,
                        style = Stroke(sw))
                    // Top bar (header of calendar)
                    drawRoundRect(
                        color        = GoldPrimary.copy(alpha = 0.35f),
                        topLeft      = Offset(0f, 0f),
                        size         = Size(size.width, size.height * 0.28f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                    // Grid dots
                    val dotR    = 2.dp.toPx()
                    val startX  = size.width  * 0.18f
                    val startY  = size.height * 0.46f
                    val spacing = size.width  * 0.22f
                    repeat(2) { row ->
                        repeat(3) { col ->
                            drawCircle(
                                color  = GoldPrimary.copy(alpha = 0.45f),
                                radius = dotR,
                                center = Offset(startX + col * spacing, startY + row * spacing),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Tidak Ada Data",
                fontWeight = FontWeight.Bold,
                fontSize   = 17.sp,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = text,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
        }
    }
}
