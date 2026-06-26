package com.alhasanah.alhasanahmedia.ui.santri

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.KesehatanSantri
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ═══════════════════════════════════════════════════════════════
//  COLOR TOKENS
//
//  PRINSIP DESAIN:
//  • Medical semantic colors (rose, teal) → DIPERTAHANKAN
//    Karena semantically correct: rose = keluhan, teal = tindakan
//  • Structural chrome (background, TopBar, cards) → DISELARASKAN
//    ke warm system yang konsisten dengan seluruh app
//
//  MASALAH ASLI:
//  DarkBase = #0C1F18 (hijau tua) — seharusnya warm near-black
//  DarkCard  = #122518 (hijau gelap) — seharusnya warm dark card
// ═══════════════════════════════════════════════════════════════

// ── Medical semantic — TIDAK BERUBAH ─────────────────────────────────────────
private val MedRose         = Color(0xFFD14B4B)
private val MedRoseSoft     = Color(0xFFFFF3F3)
private val MedRoseMid      = Color(0xFFFFE0E0)
private val MedRoseBorder   = Color(0xFFF2C0C0)

private val TreatTeal       = Color(0xFF0D8A7A)
private val TreatTealSoft   = Color(0xFFEAF5F3)
private val TreatTealBorder = Color(0xFFB0DDD8)

private val EmeraldRich     = Color(0xFF1B5E3B)

// ── Gold — konsisten dengan seluruh app ──────────────────────────────────────
private val IslamicGold      = Color(0xFFC9A84C)
private val IslamicGoldLight = Color(0xFFE8C97A)
private val IslamicGoldDark  = Color(0xFF8A6F2E)

// ── Neutral ───────────────────────────────────────────────────────────────────
private val ParchmentBase    = Color(0xFFF8F4EC)   // light background (warm cream)
private val ParchmentCard    = Color(0xFFFFFFFF)
private val ParchmentBorder  = Color(0xFFE4DDD2)
private val SageNeutral      = Color(0xFF8FA69A)

// ── Warm dark system (FIX: bukan green-tinted) ───────────────────────────────
// Konsisten dengan BeritaDetailScreen, JuzDetailScreen, HafalanKitabScreen
private val DarkBase         = Color(0xFF0D0B08)   // warm near-black
private val DarkCard         = Color(0xFF1A1710)   // warm dark card
private val DarkBorder       = Color(0xFF312C1E)   // warm dark border
private val DarkSubtle       = Color(0xFF252011)   // warm dark subtle bg

// ─── TopBar gradients — dark mode warm, light mode emerald ───────────────────
private val TopBarDark  = Brush.linearGradient(listOf(Color(0xFF110F08), Color(0xFF1C1A10)))
private val TopBarLight = Brush.linearGradient(listOf(EmeraldRich, Color(0xFF1F6B42)))

// ─── Summary card — dark mode warm charcoal, light mode emerald ──────────────
private val SummaryGradientDark  = Brush.linearGradient(
    listOf(Color(0xFF1C1810), Color(0xFF231F14), Color(0xFF1A1710))
)
private val SummaryGradientLight = Brush.linearGradient(
    listOf(EmeraldRich, Color(0xFF236B46), Color(0xFF1A5535))
)

private enum class HealthFilter(val label: String) {
    SEMUA("Semua"),
    TERBARU("30 hari"),
    KELUHAN("Keluhan"),
    CATATAN("Catatan")
}

private fun KesehatanSantri.matchesFilter(filter: HealthFilter): Boolean {
    return when (filter) {
        HealthFilter.SEMUA -> true
        HealthFilter.TERBARU -> tanggal?.let { raw ->
            runCatching {
                LocalDate.parse(raw.take(10)).isAfter(LocalDate.now().minusDays(31))
            }.getOrDefault(false)
        } == true
        HealthFilter.KELUHAN -> !keluhan.isNullOrBlank()
        HealthFilter.CATATAN -> !catatan.isNullOrBlank()
    }
}

// ═══════════════════════════════════════════════════════════════
//  SCREEN ROOT
//
//  FIX #1: isDark = isSystemInDarkTheme() (bukan luminance)
//  FIX #2: Scaffold containerColor → warm parchment system
//  FIX #3: MedicalCrossPattern → WarmAmbientBackground
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KesehatanScreen(
    navController: NavController,
    viewModel    : SantriActivityViewModel,
    santriNis    : String
) {
    val kesehatanList by viewModel.kesehatanState.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val isDark = isAppInDarkTheme()
    var showAllHistory by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(HealthFilter.SEMUA) }
    val filteredKesehatan = remember(kesehatanList, activeFilter) {
        kesehatanList.filter { it.matchesFilter(activeFilter) }
    }

    LaunchedEffect(key1 = santriNis) {
        viewModel.loadAllData(santriNis)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppSolidBackground(isDark = isDark)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HealthTopBar(
                    onBack = { navController.popBackStack() },
                    totalItems = kesehatanList.size,
                    isDark = isDark
                )
            }

            when {
                isLoading && kesehatanList.isEmpty() -> {
                    item { HealthLoadingState(isDark = isDark) }
                }
                else -> {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            HealthSummaryCard(latest = kesehatanList.firstOrNull(), isDark = isDark)
                        }
                    }

                    if (kesehatanList.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                HealthFilterRow(
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
                            HealthSectionHeader(isDark = isDark)
                        }
                    }

                        if (filteredKesehatan.isEmpty()) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    HealthEmptyState(isDark = isDark)
                                }
                            }
                        } else {
                            val visibleKesehatan = if (showAllHistory || filteredKesehatan.size <= 5) {
                                filteredKesehatan
                            } else {
                                filteredKesehatan.take(5)
                            }
                            items(visibleKesehatan) { kesehatan ->
                                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    MedicalScanCard(kesehatan = kesehatan, isDark = isDark)
                                }
                            }
                            if (filteredKesehatan.size > 5) {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                        HealthShowMoreButton(
                                            expanded = showAllHistory,
                                            hiddenCount = (filteredKesehatan.size - 5).coerceAtLeast(0),
                                            isDark = isDark,
                                            onClick = { showAllHistory = !showAllHistory }
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(40.dp)) }
                    }
                }
            }
    }
}

@Composable
private fun HealthFilterRow(
    activeFilter: HealthFilter,
    onFilterChange: (HealthFilter) -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HealthFilter.entries.forEach { filter ->
            HealthFilterChip(
                label = filter.label,
                selected = filter == activeFilter,
                isDark = isDark,
                onClick = { onFilterChange(filter) }
            )
        }
    }
}

@Composable
private fun HealthFilterChip(
    label: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val activeColor = if (isDark) IslamicGoldLight else IslamicGoldDark
    val textColor = if (selected) activeColor else if (isDark) Color.White.copy(0.56f) else SageNeutral
    val background = when {
        selected -> activeColor.copy(alpha = if (isDark) 0.16f else 0.10f)
        isDark -> DarkCard.copy(alpha = 0.82f)
        else -> ParchmentCard.copy(alpha = 0.92f)
    }
    val borderColor = if (selected) activeColor.copy(alpha = 0.48f) else if (isDark) DarkBorder else ParchmentBorder

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = background,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            color = textColor
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  TOP BAR
//
//  FIX #4: Struktur hacky (Canvas + offset negatif) → Box bersih
//  FIX #5: Dark mode gradient warm charcoal (bukan hijau gelap)
//  Light mode: emerald — konsisten dengan HafalanKitabScreen
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthTopBar(
    onBack    : () -> Unit,
    totalItems: Int,
    isDark    : Boolean
) {
    AppPageHeader(
        title = "REKAM MEDIS",
        subtitle = if (totalItems > 0) "$totalItems catatan kesehatan" else null,
        isDark = isDark,
        onBack = onBack,
        size = AppPageHeaderSize.Standard,
        titleTopPadding = 58.dp
    )
}

// ═══════════════════════════════════════════════════════════════
//  HEALTH SUMMARY CARD
//
//  FIX #6: Dark mode gradient warm charcoal, bukan bright green
//  Sebelum: #112B1E / #1A3828 / #122218 → hijau mencolok di dark
//  Sekarang: #1C1810 / #231F14 / #1A1710 → warm charcoal
//
//  Identity medis dipertahankan lewat:
//  • Pulsing heart icon tetap MedRose
//  • Gold accent tetap muncul di tanggal dan bottom line
//  Light mode: emerald gradient tetap (cocok di terang)
//
//  FIX #7: Cross pattern di dalam card → dihapus
//  diganti warm radial glow yang sama dengan ambient background
// ═══════════════════════════════════════════════════════════════

@Composable
fun HealthSummaryCard(latest: KesehatanSantri?, isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val heartScale by infiniteTransition.animateFloat(
        initialValue  = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "heartbeat"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.12f, targetValue = 0.28f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "glow"
    )

    val formattedDate = remember(latest?.tanggal) {
        latest?.tanggal?.let { raw ->
            runCatching {
                LocalDate.parse(raw.take(10))
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id")))
            }.getOrDefault(raw.take(10))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) DarkCard.copy(alpha = 0.88f) else ParchmentCard.copy(alpha = 0.86f))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        IslamicGold.copy(alpha = if (isDark) 0.45f else 0.28f),
                        IslamicGold.copy(alpha = if (isDark) 0.10f else 0.08f),
                        IslamicGold.copy(alpha = if (isDark) 0.45f else 0.28f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        // FIX #7: Cross pattern → warm radial ambient
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Rose glow tengah kiri — medical
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(MedRose.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(0f, size.height / 2f),
                    radius = size.height * 0.8f
                ),
                radius = size.height * 0.8f,
                center = Offset(0f, size.height / 2f)
            )
            // Gold glow kanan — luxury brand
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(IslamicGold.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width, size.height / 2f),
                    radius = size.height * 0.8f
                ),
                radius = size.height * 0.8f,
                center = Offset(size.width, size.height / 2f)
            )
        }

        Row(
            modifier          = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing heart — TIDAK BERUBAH (sudah premium)
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(heartScale)
                        .background(MedRose.copy(alpha = glowAlpha), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (isDark) Color.White.copy(0.12f) else IslamicGold.copy(0.08f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                        .border(1.dp, IslamicGold.copy(if (isDark) 0.34f else 0.30f), CircleShape)
                )
                Icon(
                    Icons.Default.Favorite, contentDescription = null,
                    tint = MedRose, modifier = Modifier.size(26.dp).scale(heartScale)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Text(
                    text = "STATUS KESEHATAN", fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                    color = if (isDark) Color.White.copy(0.55f) else IslamicGoldDark.copy(0.78f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    // FIX: ExtraBold → Bold
                    text = if (latest != null) "Tercatat Terakhir" else "Belum Ada Data",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (isDark) Color.White.copy(0.92f) else Color(0xFF2A2117)
                )
                if (formattedDate != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday, contentDescription = null,
                            tint = IslamicGold.copy(0.85f), modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = formattedDate, fontSize = 12.sp,
                            color = IslamicGold.copy(0.95f), fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Gold bottom accent line
        Box(
            modifier = Modifier
                .fillMaxWidth().height(2.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            IslamicGold.copy(0.40f),
                            IslamicGoldLight.copy(0.70f),
                            IslamicGold.copy(0.40f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  SECTION HEADER — terima isDark sebagai parameter
// ═══════════════════════════════════════════════════════════════

@Composable
fun HealthSectionHeader(isDark: Boolean) {
    val lineColor = if (isDark) Color.White.copy(0.07f) else ParchmentBorder

    Row(
        modifier          = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, lineColor)))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.size(5.dp).background(IslamicGold.copy(0.5f), RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "RIWAYAT KESEHATAN", fontSize = 10.sp, fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = if (isDark) Color.White.copy(0.40f) else SageNeutral
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.size(5.dp).background(IslamicGold.copy(0.5f), RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier.weight(1f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(lineColor, Color.Transparent)))
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  MEDICAL SCAN CARD
//
//  FIX #8: Card tidak lagi menggunakan MaterialTheme.colorScheme.surface
//  (yang memberi warna tidak konsisten antar tema).
//  Sekarang menggunakan warm card tokens yang sama dengan seluruh app.
//
//  Medical identity TETAP: rose accent strip, teal tindakan block.
//  Yang berubah hanya structural chrome (background, border).
// ═══════════════════════════════════════════════════════════════

@Composable
fun MedicalScanCard(kesehatan: KesehatanSantri, isDark: Boolean) {
    val cardBg      = if (isDark) DarkCard else ParchmentCard
    val borderColor = if (isDark) DarkBorder else ParchmentBorder

    val formattedDate = remember(kesehatan.tanggal) {
        kesehatan.tanggal?.let { raw ->
            runCatching {
                LocalDate.parse(raw.take(10))
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id")))
            }.getOrDefault(raw.take(10))
        } ?: "–"
    }

    // ── Expand/Collapse state ───────────────────────────────────────
    var expanded by remember { mutableStateOf(false) }

    // ── Press animation ─────────────────────────────────────────────
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "cardScale"
    )

    // ── Chevron rotation ────────────────────────────────────────────
    val chevronRotation by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "chevron"
    )

    val hasDetail = !kesehatan.keluhan.isNullOrBlank() ||
                    !kesehatan.tindakan.isNullOrBlank() ||
                    !kesehatan.catatan.isNullOrBlank()

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .animateContentSize()
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = hasDetail
            ) { if (hasDetail) expanded = !expanded },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        border    = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Row 1: Rose strip + Icon + Date + Chevron ─────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Rose accent dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MedRose, CircleShape)
                )

                // Icon
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = MedRose.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp)
                )

                // Date
                Text(
                    text     = formattedDate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color    = if (isDark) Color.White.copy(0.55f) else SageNeutral,
                    modifier = Modifier.weight(1f)
                )

                // Chevron (jika ada detail)
                if (hasDetail) {
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

            // ── Row 2: Keluhan preview (collapsed) ───────────────────
            if (!expanded && !kesehatan.keluhan.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text     = kesehatan.keluhan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color    = if (isDark) Color.White.copy(0.80f) else Color(0xFF2E1212),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 18.dp)
                )
            }

            // ── Detail section (expandable) ──────────────────────────
            AnimatedVisibility(
                visible = expanded && hasDetail,
                enter  = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit   = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color     = borderColor,
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Keluhan
                    if (!kesehatan.keluhan.isNullOrBlank()) {
                        MedicalInfoBlock(
                            label      = "KELUHAN",
                            value      = kesehatan.keluhan,
                            bgColor    = if (isDark) MedRose.copy(0.08f) else MedRoseSoft,
                            border     = if (isDark) MedRose.copy(0.18f) else MedRoseBorder,
                            labelColor = MedRose.copy(0.80f),
                            valueColor = if (isDark) Color.White.copy(0.85f) else Color(0xFF2E1212),
                            icon       = Icons.Default.ReportProblem
                        )
                    }

                    // Tindakan
                    if (!kesehatan.tindakan.isNullOrBlank()) {
                        MedicalInfoBlock(
                            label      = "TINDAKAN",
                            value      = kesehatan.tindakan,
                            bgColor    = if (isDark) TreatTeal.copy(0.09f) else TreatTealSoft,
                            border     = if (isDark) TreatTeal.copy(0.22f) else TreatTealBorder,
                            labelColor = TreatTeal.copy(0.85f),
                            valueColor = if (isDark) Color.White.copy(0.85f) else Color(0xFF0A2E28),
                            icon       = Icons.Default.Healing
                        )
                    }

                    // Catatan
                    if (!kesehatan.catatan.isNullOrBlank()) {
                        MedicalCatatanBlock(catatan = kesehatan.catatan, isDark = isDark)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  MEDICAL INFO BLOCK — terima isDark eksplisit, bukan luminance
// ═══════════════════════════════════════════════════════════════

@Composable
fun MedicalInfoBlock(
    label     : String,
    value     : String?,
    bgColor   : Color,
    border    : Color,
    labelColor: Color,
    valueColor: Color,
    icon      : androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(10.dp))
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon, contentDescription = null,
            tint = labelColor, modifier = Modifier.size(14.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label, fontSize = 9.sp, fontWeight = FontWeight.Black,
                letterSpacing = 1.sp, color = labelColor
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value ?: "–", fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, color = valueColor, lineHeight = 19.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  CATATAN BLOCK — terima isDark eksplisit
// ═══════════════════════════════════════════════════════════════

@Composable
fun MedicalCatatanBlock(catatan: String, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isDark) DarkSubtle else Color(0xFFF3EFE6),
                RoundedCornerShape(10.dp)
            )
            .border(
                1.dp,
                if (isDark) DarkBorder else ParchmentBorder,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Info, contentDescription = null,
            tint = SageNeutral.copy(0.65f),
            modifier = Modifier.size(14.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = catatan, fontSize = 12.sp, fontStyle = FontStyle.Italic,
            color = if (isDark) Color.White.copy(0.52f) else Color(0xFF4A5E54),
            lineHeight = 18.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  LOADING STATE
// ═══════════════════════════════════════════════════════════════

@Composable
fun HealthLoadingState(isDark: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color       = IslamicGold,
                trackColor  = if (isDark) IslamicGoldDark.copy(0.20f) else IslamicGold.copy(0.12f),
                strokeWidth = 2.dp,
                modifier    = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Memuat rekam medis…", fontSize = 13.sp,
                letterSpacing = 0.5.sp,
                color = if (isDark) Color.White.copy(0.38f) else SageNeutral.copy(0.75f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  EMPTY STATE
// ═══════════════════════════════════════════════════════════════

@Composable
fun HealthEmptyState(isDark: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 40.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.radialGradient(listOf(MedRose.copy(0.10f), Color.Transparent)),
                    CircleShape
                )
                .border(1.dp, MedRose.copy(0.20f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Favorite, contentDescription = null,
                tint = MedRose.copy(0.35f), modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Belum Ada Riwayat", fontWeight = FontWeight.Bold,
            fontSize = 15.sp, letterSpacing = 0.4.sp,
            color = if (isDark) Color.White.copy(0.70f) else Color(0xFF1A2E22)
        )
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = "Catatan medis santri belum\ntersedia saat ini",
            fontSize = 12.sp,
            color = if (isDark) Color.White.copy(0.32f) else SageNeutral,
            textAlign = TextAlign.Center, lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        // Ornamental gold closing mark
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.width(36.dp).height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, IslamicGold.copy(0.3f))))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(modifier = Modifier.size(4.dp).background(IslamicGold.copy(0.40f), RoundedCornerShape(1.dp)))
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier.width(36.dp).height(1.dp)
                    .background(Brush.horizontalGradient(listOf(IslamicGold.copy(0.3f), Color.Transparent)))
            )
        }
    }
}

@Composable
private fun HealthShowMoreButton(
    expanded: Boolean,
    hiddenCount: Int,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val color = if (isDark) IslamicGoldLight else IslamicGoldDark
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.42f))
    ) {
        Text(
            text = if (expanded) "Sembunyikan riwayat" else "Lihat $hiddenCount catatan lainnya",
            color = color,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp
        )
    }
}
