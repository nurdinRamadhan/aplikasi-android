package com.alhasanah.alhasanahmedia.ui.santri

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.HafalanKitab
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════════════
//  KITAB MASTER DATA
// ═══════════════════════════════════════════════════════════════════════════════

private val KITAB_BAIT    = setOf("imrity", "nadzmul maqshud", "alfiyah", "uqudul juman", "sulam munawraq")
private val KITAB_HALAMAN = setOf("jurumiah")

/** Total bait kanonik per kitab nazham — sumber kebenaran untuk progress bar */
private val KITAB_MAX_BAIT = mapOf(
    "alfiyah"         to 1002,
    "imrity"          to 254,
    "nadzmul maqshud" to 113,
    "uqudul juman"    to 1006,
    "sulam munawraq"  to 144
)

/** Urutan tampil di dashboard (besar → kecil bait, Jurumiah paling akhir) */
private val KITAB_DISPLAY_ORDER = listOf(
    "alfiyah", "uqudul juman", "imrity", "sulam munawraq", "nadzmul maqshud", "jurumiah"
)

// ═══════════════════════════════════════════════════════════════════════════════
//  WARNA — minimal: Gold untuk Mumtaz, MaterialTheme untuk selebihnya
// ═══════════════════════════════════════════════════════════════════════════════

private val Gold300 = Color(0xFFE8C97A)
private val Gold500 = Color(0xFFC9A84C)
private val Gold700 = Color(0xFF8A6F2E)

// ═══════════════════════════════════════════════════════════════════════════════
//  DATA CLASS — dashboard progress per kitab
// ═══════════════════════════════════════════════════════════════════════════════

private data class KitabProgressEntry(
    val key         : String,
    val displayName : String,
    val icon        : ImageVector,
    val achieved    : Int,           // bait_akhir tertinggi tercatat (atau jumlah setoran untuk Jurumiah)
    val total       : Int?,          // total kanonik; null = Jurumiah (tidak ada total tetap)
    val progress    : Float,         // 0f–1f; 0f untuk Jurumiah
    val isJurumiah  : Boolean = false
)

// ═══════════════════════════════════════════════════════════════════════════════
//  SMART LOGIC — dipertahankan dari versi asli + penambahan dashboard logic
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Menentukan label progress (BAIT/HALAMAN), nilai awal, dan nilai akhir.
 * Menggunakan partial-match agar robust terhadap variasi penulisan nama kitab.
 */
private fun resolveProgressInfo(hafalan: HafalanKitab): Triple<String, String, String> {
    val key            = hafalan.nama_kitab.lowercase().trim()
    val isKitabBait    = KITAB_BAIT.any    { key.contains(it) }
    val isKitabHalaman = KITAB_HALAMAN.any { key.contains(it) }

    return when {
        isKitabHalaman && hafalan.halaman_awal != null -> Triple(
            "HALAMAN", hafalan.halaman_awal.toString(), hafalan.halaman_akhir?.toString() ?: "–"
        )
        isKitabBait && hafalan.bait_awal != null -> Triple(
            "BAIT", hafalan.bait_awal.toString(), hafalan.bait_akhir?.toString() ?: "–"
        )
        hafalan.bait_awal != null -> Triple(
            "BAIT", hafalan.bait_awal.toString(), hafalan.bait_akhir?.toString() ?: "–"
        )
        hafalan.halaman_awal != null -> Triple(
            "HALAMAN", hafalan.halaman_awal.toString(), hafalan.halaman_akhir?.toString() ?: "–"
        )
        else -> Triple("CAPAIAN", "–", "–")
    }
}

/**
 * Format tampil progress yang cerdas:
 *   • bait tunggal (awal == akhir) → "15" bukan "15 – 15"
 *   • akhir tidak ada              → "ab bait 1"
 *   • normal                       → "1 – 50"
 */
private fun formatProgressDisplay(label: String, awal: String, akhir: String): String {
    if (awal == "–")    return "–"
    if (akhir == "–")   return "ab ${label.lowercase()} $awal"
    if (awal == akhir)  return awal
    return "$awal – $akhir"
}

/** Ikon kontekstual per kitab */
private fun resolveKitabIcon(namaKitab: String): ImageVector {
    val key = namaKitab.lowercase().trim()
    return when {
        key.contains("alfiyah")  -> Icons.Default.FormatAlignJustify
        key.contains("imrity")   -> Icons.AutoMirrored.Filled.MenuBook
        key.contains("nadzmul")  -> Icons.Default.Star
        key.contains("uqudul")   -> Icons.Default.AutoStories
        key.contains("sulam")    -> Icons.AutoMirrored.Filled.LibraryBooks
        key.contains("jurumiah") -> Icons.AutoMirrored.Filled.Article
        else                     -> Icons.AutoMirrored.Filled.MenuBook
    }
}

/**
 * Menghitung progres capaian per kitab dari seluruh riwayat setoran.
 * Untuk nazham bait: ambil nilai bait_akhir tertinggi yang pernah tercatat.
 * Untuk Jurumiah (nasar/bab): hitung jumlah setoran.
 */
private fun buildKitabEntries(hafalanList: List<HafalanKitab>): List<KitabProgressEntry> {
    val maxBait       = mutableMapOf<String, Int>()
    var jurumiahCount = 0

    hafalanList.forEach { h ->
        val name = h.nama_kitab.lowercase().trim()
        when {
            name.contains("jurumiah") -> jurumiahCount++
            else -> KITAB_MAX_BAIT.keys.firstOrNull { name.contains(it) }?.let { key ->
                val akhir = h.bait_akhir ?: return@let
                maxBait[key] = maxOf(maxBait.getOrDefault(key, 0), akhir)
            }
        }
    }

    val baitEntries = KITAB_DISPLAY_ORDER
        .filter { it != "jurumiah" }
        .map { key ->
            val total    = KITAB_MAX_BAIT[key] ?: 1
            val achieved = maxBait.getOrDefault(key, 0)
            val name     = key.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar(Char::uppercase)
            }
            KitabProgressEntry(
                key         = key,
                displayName = name,
                icon        = resolveKitabIcon(key),
                achieved    = achieved,
                total       = total,
                progress    = (achieved / total.toFloat()).coerceIn(0f, 1f)
            )
        }

    val jurumiah = KitabProgressEntry(
        key         = "jurumiah",
        displayName = "Jurumiah",
        icon        = resolveKitabIcon("jurumiah"),
        achieved    = jurumiahCount,
        total       = null,
        progress    = 0f,
        isJurumiah  = true
    )

    return baitEntries + jurumiah
}

// ═══════════════════════════════════════════════════════════════════════════════
//  PREDIKAT HELPERS — diselaraskan dengan HafalanScreen
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Warna aksen predikat, selaras dengan skema warna HafalanScreen:
 *   Mumtaz       → Gold500 (konsisten)
 *   Jayyid Jiddan → Emerald (unik untuk kitab, tidak ada di tahfidz)
 *   Jayyid       → Biru 0277BD (sama dengan HafalanScreen)
 *   Kurang       → Merah BA1A1A (sama dengan HafalanScreen)
 */
private fun kitabPredikatAccent(predikat: String?, primary: Color): Color =
    when (predikat?.lowercase()?.trim()) {
        "mumtaz"        -> Gold500
        "jayyid jiddan" -> Color(0xFF2E7D52)
        "jayyid"        -> Color(0xFF0277BD)
        "kurang"        -> Color(0xFFBA1A1A)
        else            -> primary
    }

private fun isMumtaz(predikat: String?) = predikat?.lowercase()?.trim() == "mumtaz"

// ═══════════════════════════════════════════════════════════════════════════════
//  FILTER + EXTENSIONS
// ═══════════════════════════════════════════════════════════════════════════════

private enum class KitabDateFilter(val label: String) {
    ALL("Semua"),
    LAST_30_DAYS("30 Hari"),
    LAST_90_DAYS("90 Hari"),
    THIS_YEAR("Tahun Ini")
}

private fun HafalanKitab.localDateOrNull(): LocalDate? = runCatching {
    LocalDate.parse(tanggal.take(10))
}.getOrNull()

private fun HafalanKitab.matchesFilter(filter: KitabDateFilter): Boolean {
    val date = localDateOrNull() ?: return filter == KitabDateFilter.ALL
    val now  = LocalDate.now()
    return when (filter) {
        KitabDateFilter.ALL          -> true
        KitabDateFilter.LAST_30_DAYS -> !date.isBefore(now.minusDays(30))
        KitabDateFilter.LAST_90_DAYS -> !date.isBefore(now.minusDays(90))
        KitabDateFilter.THIS_YEAR    -> date.year == now.year
    }
}

private fun kitabMonthTitle(date: LocalDate?): String {
    if (date == null) return "Tanggal tidak diketahui"
    return YearMonth.from(date)
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id")))
}

private fun formatDate(tanggal: String): String = runCatching {
    LocalDate.parse(tanggal.take(10))
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id")))
}.getOrDefault(tanggal.take(10))

// ═══════════════════════════════════════════════════════════════════════════════
//  SCREEN ROOT
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun HafalanKitabScreen(
    navController : NavController,
    viewModel     : SantriActivityViewModel,
    santriNis     : String
) {
    val hafalanList by viewModel.hafalanKitabState.collectAsState()
    val santri      by viewModel.santriState.collectAsState()       // sama dengan HafalanScreen
    val isLoading   by viewModel.isLoading.collectAsState()
    var activeFilter   by remember { mutableStateOf(KitabDateFilter.ALL) }
    var showAllHistory by remember { mutableStateOf(false) }

    LaunchedEffect(santriNis) { viewModel.loadAllData(santriNis) }

    val kitabEntries = remember(hafalanList) { buildKitabEntries(hafalanList) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background motif islami — reuse dari HafalanScreen (public, paket sama)
        HafalanIslamicBackground()

        when {
            isLoading && hafalanList.isEmpty() -> {
                KitabLoadingState()
            }
            !isLoading && hafalanList.isEmpty() -> {
                KitabEmptyState()
            }
            else -> {
                val filteredHistory = remember(hafalanList, activeFilter) {
                    hafalanList
                        .filter { it.matchesFilter(activeFilter) }
                        .sortedByDescending { it.localDateOrNull() ?: LocalDate.MIN }
                }
                val visibleHistory = remember(filteredHistory, showAllHistory) {
                    if (showAllHistory || filteredHistory.size <= 3) filteredHistory
                    else filteredHistory.take(3)
                }
                val groupedHistory = remember(visibleHistory) {
                    visibleHistory.groupBy { kitabMonthTitle(it.localDateOrNull()) }
                }

                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {

                    // ── 1. Header ─────────────────────────────────────────────
                    item(key = "header") {
                        HafalanKitabHeader(
                            santriName      = santri?.namaLengkap ?: "",
                            totalSetoran    = hafalanList.size,
                            totalKitabAktif = kitabEntries.count { it.achieved > 0 },
                            onBack          = { navController.popBackStack() }
                        )
                    }

                    // ── 2. Dashboard Progres Per Kitab ────────────────────────
                    item(key = "dashboard") {
                        Spacer(modifier = Modifier.height(16.dp))
                        KitabProgressDashboard(kitabEntries = kitabEntries)
                    }

                    // ── 3. Section header + filter chips ─────────────────────
                    item(key = "history_header") {
                        KitabHistoryHeader(
                            totalCount    = filteredHistory.size,
                            activeFilter  = activeFilter,
                            onFilterChange = {
                                activeFilter   = it
                                showAllHistory = false
                            }
                        )
                    }

                    // ── 4. Riwayat setoran (grouped by month) ─────────────────
                    if (filteredHistory.isEmpty()) {
                        item(key = "empty_filter") {
                            KitabHistoryEmpty(activeFilter = activeFilter)
                        }
                    } else {
                        groupedHistory.forEach { (month, itemsInMonth) ->
                            item(key = "month_$month") {
                                KitabMonthLabel(month = month)
                            }
                            items(
                                items = itemsInMonth,
                                key   = { "${it.id}_${it.tanggal}_${it.nama_kitab}" }
                            ) { hafalan ->
                                HafalanKitabCard(
                                    hafalan  = hafalan,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp)
                                )
                            }
                        }

                        if (filteredHistory.size > 3) {
                            item(key = "toggle") {
                                KitabHistoryToggleButton(
                                    expanded    = showAllHistory,
                                    hiddenCount = (filteredHistory.size - 3).coerceAtLeast(0),
                                    onClick     = { showAllHistory = !showAllHistory }
                                )
                            }
                        }
                    }

                    item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HEADER — struktur identik dengan HafalanHeader di HafalanScreen
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HafalanKitabHeader(
    santriName      : String,
    totalSetoran    : Int,
    totalKitabAktif : Int,
    onBack          : () -> Unit
) {
    val isDark     = isAppInDarkTheme()
    val primary    = MaterialTheme.colorScheme.primary
    // Warna judul diselaraskan: gold-tone di light (sama dengan HafalanScreen)
    val titleColor = if (isDark) primary.copy(alpha = 0.92f) else Color(0xFF8B6914)
    val bodyColor  = if (isDark) Color.White.copy(alpha = 0.78f)
                     else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Background identik dengan HafalanScreen
        AppPageHeaderBackground(isDark = isDark, modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Back button row ────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = if (isDark) 0.10f else 0.56f
                            )
                        )
                        .border(1.dp, primary.copy(alpha = 0.38f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(46.dp)) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint               = if (isDark) Color.White.copy(0.88f) else Color(0xFF2B2418),
                            modifier           = Modifier.size(22.dp)
                        )
                    }
                }
                // Balance spacer — konsisten dengan HafalanHeader
                Box(modifier = Modifier.size(46.dp))
            }

            // ── Konten tengah ─────────────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Judul
                Text(
                    text  = "HAFALAN KITAB",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color         = titleColor
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = "Rekam Jejak Hafalan Kitab Kuning",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color      = bodyColor,
                        fontWeight = FontWeight.Normal
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Divider ornamental — identik dengan HafalanHeroDivider
                KitabHeroDivider(isDark = isDark)

                // Nama santri
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

                Spacer(modifier = Modifier.height(16.dp))

                // Stat chips — menggantikan ArcGauge (ringkasan cepat di header)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KitabStatChip(
                        value = "$totalSetoran",
                        label = "Total Setoran",
                        icon  = Icons.Outlined.MenuBook
                    )
                    KitabStatChip(
                        value = "$totalKitabAktif / 6",
                        label = "Kitab Aktif",
                        icon  = Icons.Outlined.AutoStories
                    )
                }
            }
        }
    }
}

// ── Divider ornamental (versi kitab dari HafalanHeroDivider) ─────────────────

@Composable
private fun KitabHeroDivider(isDark: Boolean) {
    val primary   = MaterialTheme.colorScheme.primary
    val lineColor = if (isDark) Color.White.copy(0.07f) else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier          = Modifier.width(170.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, lineColor)))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.size(5.dp).background(primary.copy(0.60f), RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Box(modifier = Modifier.size(8.dp).background(primary.copy(0.82f), CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Box(modifier = Modifier.size(5.dp).background(primary.copy(0.60f), RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(lineColor, Color.Transparent)))
        )
    }
}

// ── Stat chip di header ───────────────────────────────────────────────────────

@Composable
private fun KitabStatChip(value: String, label: String, icon: ImageVector) {
    val isDark  = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.56f else 0.96f)
            )
            .border(
                1.dp,
                primary.copy(alpha = if (isDark) 0.30f else 0.20f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = primary,
                modifier           = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Column {
                Text(
                    text  = value,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  DASHBOARD PROGRES PER KITAB — fitur utama baru
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun KitabProgressDashboard(kitabEntries: List<KitabProgressEntry>) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section header — identik dengan header "RIWAYAT SETORAN" di HafalanScreen
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
                    text  = "CAPAIAN PER KITAB",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        color      = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }

        // Grid 2 kolom — 6 kitab = 3 baris
        kitabEntries.chunked(2).forEach { pair ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { entry ->
                    KitabProgressCard(entry = entry, modifier = Modifier.weight(1f))
                }
                // Jika baris ganjil (seharusnya tidak terjadi untuk 6 kitab)
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  CARD PROGRES KITAB — individual
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun KitabProgressCard(
    entry   : KitabProgressEntry,
    modifier: Modifier = Modifier
) {
    val isDark  = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary

    val animatedProgress by animateFloatAsState(
        targetValue   = entry.progress,
        animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
        label         = "kitabProg_${entry.key}"
    )

    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(16.dp),
        color           = MaterialTheme.colorScheme.surface,
        border          = BorderStroke(1.dp, primary.copy(alpha = if (isDark) 0.20f else 0.12f)),
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Icon + nama kitab
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = entry.icon,
                    contentDescription = null,
                    tint               = primary,
                    modifier           = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text     = entry.displayName,
                    style    = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        fontSize   = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (entry.isJurumiah) {
                // Jurumiah: tidak ada total bait tetap — tampilkan jumlah setoran
                Text(
                    text  = if (entry.achieved == 0) "–" else "${entry.achieved}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        color      = MaterialTheme.colorScheme.onSurface,
                        fontSize   = 24.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = if (entry.achieved == 0) "Belum ada setoran" else "setoran tercatat",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = primary.copy(alpha = if (isDark) 0.12f else 0.08f)
                ) {
                    Text(
                        text     = "Nasar · Bab",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style    = MaterialTheme.typography.labelSmall.copy(
                            color      = primary,
                            fontSize   = 8.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            } else {
                // Nazham dengan total bait tetap — progress bar animasi
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(primary.copy(alpha = if (isDark) 0.14f else 0.09f))
                ) {
                    if (animatedProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(primary.copy(alpha = 0.65f), primary)
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = if (entry.achieved == 0) "Belum ada"
                                else "${entry.achieved} / ${entry.total}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    )
                    Surface(shape = CircleShape, color = primary.copy(alpha = if (isDark) 0.12f else 0.10f)) {
                        Text(
                            text     = if (entry.achieved == 0) "0%"
                                       else "${(entry.progress * 100).toInt()}%",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style    = MaterialTheme.typography.labelSmall.copy(
                                color      = primary,
                                fontWeight = FontWeight.Black,
                                fontSize   = 9.sp
                            )
                        )
                    }
                }

                Text(
                    text  = "dari ${entry.total} bait",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        fontSize = 8.sp
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HISTORY HEADER — filter chips + section label
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun KitabHistoryHeader(
    totalCount    : Int,
    activeFilter  : KitabDateFilter,
    onFilterChange: (KitabDateFilter) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Label + badge count — identik dengan HafalanScreen
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
                    text  = "RIWAYAT SETORAN",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        color      = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
            Surface(shape = CircleShape, color = primary.copy(alpha = 0.10f)) {
                Text(
                    text     = "$totalCount setoran",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style    = MaterialTheme.typography.labelSmall.copy(
                        color      = primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Filter chips — horizontalScroll (sama dengan HafalanScreen, bukan weight=1f)
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KitabDateFilter.entries.forEach { filter ->
                val selected = filter == activeFilter
                Surface(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .widthIn(min = 80.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { onFilterChange(filter) },
                    shape  = CircleShape,
                    color  = if (selected) primary
                             else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    border = if (selected) null
                             else BorderStroke(
                                 1.dp,
                                 MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                             )
                ) {
                    Text(
                        text      = filter.label,
                        textAlign = TextAlign.Center,
                        maxLines  = 1,
                        style     = MaterialTheme.typography.labelSmall.copy(
                            color      = if (selected) MaterialTheme.colorScheme.onPrimary
                                         else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 10.sp
                        ),
                        modifier  = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HAFALAN KITAB CARD — compact, konsisten dengan HafalanSetoranCard
//
//  Perubahan utama dari versi lama:
//    • Icon circle 46dp → accent bar kiri 4dp (seperti HafalanSetoranCard)
//    • GoldOrnamentalDivider dalam card → dihapus (menghemat ~24dp per card)
//    • InfoChip dua baris → satu baris meta yang ringkas
//    • CatatanBlock → KitabCatatanNote (expandable, default collapsed 2 baris)
//    • Tidak ada isDark prop-drilling; gunakan isAppInDarkTheme() lokal
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HafalanKitabCard(
    hafalan : HafalanKitab,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    val (progressLabel, progressAwal, progressAkhir) = resolveProgressInfo(hafalan)
    val progressDisplay = formatProgressDisplay(progressLabel, progressAwal, progressAkhir)

    val accent = kitabPredikatAccent(hafalan.predikat, primary)
    val isGold = isMumtaz(hafalan.predikat)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.977f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "kitabCardScale"
    )

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = {}   // TODO: navigasi ke detail
            ),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape     = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            // ── Accent bar kiri (konsisten dengan HafalanSetoranCard) ──────────
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.30f))),
                        RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                    )
            )

            // ── Konten card ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                // Baris atas: nama kitab + predikat badge
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text     = hafalan.nama_kitab.uppercase(),
                            style    = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight    = FontWeight.ExtraBold,
                                color         = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.3.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Meta: bab · bait/halaman
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!hafalan.bab_materi.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Bookmark, null,
                                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text     = hafalan.bab_materi,
                                        style    = MaterialTheme.typography.labelSmall.copy(
                                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 90.dp)
                                    )
                                }
                                Text(
                                    text  = "·",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.40f)
                                    )
                                )
                            }
                            // Progress bait/halaman
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Tag, null,
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text  = "$progressLabel $progressDisplay",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Kanan: predikat badge + status pill
                    Column(horizontalAlignment = Alignment.End) {
                        if (!hafalan.predikat.isNullOrBlank()) {
                            KitabPredikatBadge(
                                predikat = hafalan.predikat,
                                accent   = accent,
                                isGold   = isGold
                            )
                        }
                        if (!hafalan.status.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            KitabStatusPill(status = hafalan.status)
                        }
                    }
                }

                // Tanggal
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CalendarToday, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text  = formatDate(hafalan.tanggal),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.65f),
                            fontSize = 10.sp
                        )
                    )
                }

                // Catatan (collapsed default, expandable on tap)
                if (!hafalan.catatan.isNullOrBlank()) {
                    KitabCatatanNote(catatan = hafalan.catatan)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  KOMPONEN PENDUKUNG CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun KitabPredikatBadge(predikat: String, accent: Color, isGold: Boolean) {
    val isDark = isAppInDarkTheme()
    Surface(
        shape  = RoundedCornerShape(8.dp),
        color  = if (isGold) Gold500.copy(alpha = 0.15f)
                 else accent.copy(alpha = if (isDark) 0.15f else 0.10f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isGold) Gold500.copy(0.55f)
                    else accent.copy(if (isDark) 0.40f else 0.28f)
        )
    ) {
        Text(
            text     = predikat.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style    = MaterialTheme.typography.labelSmall.copy(
                color         = if (isGold) Gold700 else accent,
                fontWeight    = FontWeight.Black,
                letterSpacing = 0.5.sp,
                fontSize      = 8.sp
            )
        )
    }
}

@Composable
private fun KitabStatusPill(status: String) {
    val isDark = isAppInDarkTheme()
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val bg: Color
    val fg: Color
    when (status.lowercase().trim()) {
        "lulus"        -> { bg = Color(0xFF2E7D52).copy(if (isDark) 0.18f else 0.12f); fg = Color(0xFF4CAF78) }
        "mengulang"    -> { bg = Color(0xFFB45309).copy(if (isDark) 0.18f else 0.12f); fg = Color(0xFFFBBF24) }
        "dalam proses" -> { bg = Color(0xFF1D4ED8).copy(if (isDark) 0.18f else 0.10f); fg = Color(0xFF60A5FA) }
        else           -> { bg = onSurfaceVariant.copy(0.10f); fg = onSurfaceVariant }
    }

    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            text     = status,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style    = MaterialTheme.typography.labelSmall.copy(
                color      = fg,
                fontSize   = 8.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

/** Catatan singkat expandable — collapsed default (2 baris), expand on tap */
@Composable
private fun KitabCatatanNote(catatan: String) {
    val isDark   = isAppInDarkTheme()
    val primary  = MaterialTheme.colorScheme.primary
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) { expanded = !expanded },
        shape  = RoundedCornerShape(10.dp),
        color  = primary.copy(alpha = if (isDark) 0.10f else 0.075f),
        border = BorderStroke(1.dp, primary.copy(alpha = if (isDark) 0.28f else 0.22f))
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Outlined.Notes, null,
                tint     = primary,
                modifier = Modifier.size(13.dp).padding(top = 1.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text     = catatan.trim(),
                style    = MaterialTheme.typography.labelSmall.copy(
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontSize   = 11.sp,
                    lineHeight = 16.sp,
                    fontStyle  = FontStyle.Italic
                ),
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  KOMPONEN LIST PENDUKUNG
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun KitabMonthLabel(month: String) {
    Text(
        text     = month.uppercase(),
        style    = MaterialTheme.typography.labelSmall.copy(
            color      = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
            fontWeight = FontWeight.Black,
            fontSize   = 10.sp
        ),
        modifier = Modifier.padding(start = 26.dp, end = 24.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun KitabHistoryEmpty(activeFilter: KitabDateFilter) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape  = RoundedCornerShape(18.dp),
        color  = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.45f))
    ) {
        Text(
            text      = "Tidak ada setoran kitab pada filter ${activeFilter.label.lowercase()}.",
            style     = MaterialTheme.typography.bodySmall.copy(
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(18.dp)
        )
    }
}

@Composable
private fun KitabHistoryToggleButton(
    expanded   : Boolean,
    hiddenCount: Int,
    onClick    : () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            ),
        shape  = RoundedCornerShape(16.dp),
        color  = Color.Transparent,
        border = BorderStroke(1.dp, primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier              = Modifier.padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = if (expanded) Icons.Outlined.VisibilityOff
                                     else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Sembunyikan" else "Lihat semua",
                tint               = primary,
                modifier           = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text  = if (expanded) "SEMBUNYIKAN RIWAYAT"
                        else "LIHAT SEMUA RIWAYAT (+$hiddenCount)",
                style = MaterialTheme.typography.labelMedium.copy(
                    color      = primary,
                    fontWeight = FontWeight.Black
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  LOADING + EMPTY STATES
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun KitabLoadingState() {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color       = Gold500,
                trackColor  = Gold500.copy(alpha = 0.15f),
                strokeWidth = 3.dp,
                modifier    = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text  = "Memuat data hafalan…",
                style = MaterialTheme.typography.bodySmall.copy(
                    color         = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "بِسْمِ اللَّهِ",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = primary.copy(alpha = 0.40f)
                )
            )
        }
    }
}

@Composable
private fun KitabEmptyState() {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 40.dp)
        ) {
            // Icon circle (dipertahankan dari PremiumEmptyState)
            Box(
                modifier         = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Gold500.copy(0.08f), Color.Transparent)))
                        .border(1.dp, Gold500.copy(0.18f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Gold500.copy(0.12f), Color.Transparent)))
                        .border(1.dp, Gold500.copy(0.30f), CircleShape)
                )
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint               = Gold500.copy(0.50f),
                    modifier           = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text  = "Belum Ada Catatan",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    color         = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text      = "Riwayat setoran kitab belum\ntersedia untuk santri ini",
                style     = MaterialTheme.typography.bodySmall.copy(
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(1.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Gold500.copy(0.35f))))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(4.dp).background(Gold500.copy(0.45f), RoundedCornerShape(1.dp)))
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(1.dp)
                        .background(Brush.horizontalGradient(listOf(Gold500.copy(0.35f), Color.Transparent)))
                )
            }
        }
    }
}
