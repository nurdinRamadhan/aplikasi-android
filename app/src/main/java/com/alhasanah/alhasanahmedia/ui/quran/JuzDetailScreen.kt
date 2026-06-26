package com.alhasanah.alhasanahmedia.ui.quran

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.quran.Ayah
import com.alhasanah.alhasanahmedia.data.model.quran.JuzDetail
import com.alhasanah.alhasanahmedia.ui.components.AppGradientBackground
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.ui.theme.AmiriFontFamily
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// ─────────────────────────────────────────────────────────────────────────────
// ██  LUXURY COLOR PALETTE — tidak berubah dari aslinya
// ─────────────────────────────────────────────────────────────────────────────
private val IvoryWhite    = Color(0xFFFFFBF0)
private val PureWhite     = Color(0xFFFFFFFF)
private val GoldPrimary   = Color(0xFFD4A017)
private val GoldLight     = Color(0xFFE8C55A)
private val GoldDeep      = Color(0xFFAA7C1F)
private val GoldMuted     = Color(0xFFF5E6A3)
private val GoldShimmer   = Color(0xFFFAF0C0)
private val CharcoalDeep  = Color(0xFF1A1A1A)
private val CharcoalMid   = Color(0xFF222222)
private val TextPrimary   = Color(0xFF1C1C1E)
private val TextSecondary = Color(0xFF6B6B6B)
private val DividerGold   = Color(0xFFE8C55A).copy(alpha = 0.25f)
private val CardSurface   = Color(0xFFFFFDF5)
private val PlayingTint   = Color(0xFFFFF8E1)

// ── NEW: surface tokens untuk dark mode ──────────────────────────────────────
// Warm dark surfaces — bukan Material grey generic
private val DarkBackground  = Color(0xFF0C0A06)   // Warm near-black
private val DarkCardSurface = Color(0xFF1C1810)   // Warm dark card
private val DarkCardPlaying = Color(0xFF231A06)   // Amber-tinted saat playing
private val DarkSurahHeader = Color(0xFF201C12)   // Sedikit lebih terang dari background

private val GoldGradient = Brush.linearGradient(
    colors = listOf(GoldDeep, GoldPrimary, GoldLight, GoldPrimary, GoldDeep)
)
private val HeaderGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF111111), Color(0xFF1F1F1F))
)
private val GoldRadialGlow = Brush.radialGradient(
    colors = listOf(GoldPrimary.copy(alpha = 0.16f), Color.Transparent),
    radius = 260f
)
private val IvoryGradient = Brush.verticalGradient(
    colors = listOf(IvoryWhite, PureWhite)
)

// ─────────────────────────────────────────────────────────────────────────────
// ██  MAIN SCREEN — LOGIKA IDENTIK
//     Perubahan: `isDark` diteruskan ke seluruh composable tree
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuzDetailScreen(
    nomor       : Int,
    navController: NavController,
    viewModel   : JuzDetailViewModel = koinViewModel { parametersOf(nomor) }
) {
    val uiState            by viewModel.uiState.collectAsState()
    val tafsirState        by viewModel.tafsirState.collectAsState()
    val currentPlayingAyat by viewModel.currentPlayingAyat.collectAsState()
    val isDark              = isAppInDarkTheme()

    var selectedAyatForTafsir by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppGradientBackground(isDark = isDark)
        Column(modifier = Modifier.fillMaxSize()) {

            LuxuryJuzTopBar(
                nomor  = nomor,
                isDark = isDark,
                onBack = { navController.popBackStack() }
            )

            when (val state = uiState) {
                is QuranUiState.Loading -> LuxuryJuzLoading(nomor, isDark)
                is QuranUiState.Error   -> NoInternetScreen(
                    message = state.message,
                    onRetry = { viewModel.fetchJuzDetail() }
                )
                is QuranUiState.Success -> {
                    val bookmarks by viewModel.bookmarks.collectAsState()
                    JuzContent(
                        juz                = state.data,
                        currentPlayingAyat = currentPlayingAyat,
                        bookmarks          = bookmarks,
                        isDark             = isDark,           // ← diteruskan
                        onPlayAudio        = { ayah ->
                            val url = ayah.audioUrl ?: ""
                            viewModel.playAudio(url, ayah.surahNumber, ayah.ayahNumber)
                        },
                        onPlayFullJuz   = { viewModel.playFullJuz() },
                        onTafsirClick   = { surahNo, ayatNo ->
                            selectedAyatForTafsir = surahNo to ayatNo
                            viewModel.fetchTafsir(surahNo, ayatNo)
                        },
                        onBookmarkClick = { surahNo, ayatNo ->
                            viewModel.toggleBookmark(surahNo, ayatNo)
                        }
                    )
                }
            }
        }
    }

    selectedAyatForTafsir?.let { (_, ayatNo) ->
        TafsirBottomSheet(
            ayatNo   = ayatNo,
            state    = tafsirState,
            onDismiss = {
                selectedAyatForTafsir = null
                viewModel.clearTafsir()
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  TOP BAR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LuxuryJuzTopBar(nomor: Int, isDark: Boolean, onBack: () -> Unit) {
    val titleColor = if (isDark) PureWhite else MaterialTheme.colorScheme.onSurface
    val backTint = if (isDark) GoldLight else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        AppPageHeaderBackground(isDark = isDark, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .border(1.dp, GoldPrimary.copy(alpha = 0.40f), CircleShape)
                .clickable { onBack() }
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint               = backTint,
                modifier           = Modifier.size(20.dp)
            )
        }

        Column(
            modifier            = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text          = "Juz $nomor",
                fontSize      = 16.sp,
                fontWeight    = FontWeight.Bold,
                color         = titleColor,
                letterSpacing = 0.5.sp
            )
            Text(
                text          = "الْجُزْء",
                fontFamily    = AmiriFontFamily,
                fontSize      = 13.sp,
                color         = GoldPrimary.copy(alpha = 0.70f),
                letterSpacing = 1.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  JUZ CONTENT
//     Perubahan: terima `isDark`, teruskan ke semua child
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun JuzContent(
    juz               : JuzDetail,
    currentPlayingAyat: String?,
    bookmarks         : Set<String>,
    isDark            : Boolean,
    onPlayAudio       : (Ayah) -> Unit,
    onPlayFullJuz     : () -> Unit,
    onTafsirClick     : (Int, Int) -> Unit,
    onBookmarkClick   : (Int, Int) -> Unit
) {
    val groupedAyat = remember(juz.ayat) {
        juz.ayat.groupBy { it.surahNumber }
    }

    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            JuzHeaderCard(
                juz           = juz,
                isDark        = isDark,
                isPlayingFull = currentPlayingAyat == "streaming",
                onPlayFullClick = onPlayFullJuz
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        groupedAyat.forEach { (surahNo, ayahs) ->
            item {
                LuxurySurahSectionHeader(
                    surahName   = ayahs.firstOrNull()?.surahInfo?.nameLatin ?: "Surah $surahNo",
                    surahArabic = ayahs.firstOrNull()?.surahInfo?.name ?: "",
                    surahNo     = surahNo,
                    // ── FIX 5: Teruskan jumlah ayat ─────────────────────────
                    ayatCount   = ayahs.size,
                    isDark      = isDark
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(ayahs) { ayah ->
                val isPlaying = currentPlayingAyat == "${ayah.surahNumber}:${ayah.ayahNumber}"
                JuzAyahItem(
                    ayah            = ayah,
                    isPlaying       = isPlaying,
                    isDark          = isDark,
                    isBookmarked    = bookmarks.contains("${ayah.surahNumber}:${ayah.ayahNumber}"),
                    onPlayClick     = { onPlayAudio(ayah) },
                    onTafsirClick   = { onTafsirClick(ayah.surahNumber, ayah.ayahNumber) },
                    onBookmarkClick = { onBookmarkClick(ayah.surahNumber, ayah.ayahNumber) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                // ── Surah end divider — triple-dot gold (lebih elegan dari 1dp line) ──
                Row(
                    modifier             = Modifier.fillMaxWidth(),
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Canvas(Modifier.size(3.dp))   { drawCircle(GoldDeep.copy(alpha = 0.30f)) }
                    Spacer(Modifier.width(8.dp))
                    Canvas(Modifier.size(5.dp))   { drawCircle(GoldPrimary.copy(alpha = 0.50f)) }
                    Spacer(Modifier.width(8.dp))
                    Canvas(Modifier.size(3.dp))   { drawCircle(GoldDeep.copy(alpha = 0.30f)) }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  JUZ HEADER CARD
//     Perubahan: terima `isDark` untuk elemen statistik baru
//     Background tetap gelap di kedua mode — ini design language yang tepat
//     untuk "chapter cover" pada mushaf
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun JuzHeaderCard(
    juz            : JuzDetail,
    isDark         : Boolean,
    isPlayingFull  : Boolean,
    onPlayFullClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(HeaderGradient, RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        GoldPrimary.copy(alpha = 0.55f),
                        GoldLight.copy(alpha = 0.20f),
                        GoldPrimary.copy(alpha = 0.55f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .drawBehind {
                drawCircle(
                    brush  = GoldRadialGlow,
                    radius = size.width * 0.65f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
                drawLine(
                    brush       = GoldGradient,
                    start       = Offset(60f, 0f),
                    end         = Offset(size.width - 60f, 0f),
                    strokeWidth = 2.5f
                )
                for (i in 0..3) {
                    val r = 18f + i * 16f
                    drawCircle(
                        color  = GoldPrimary.copy(alpha = 0.055f - i * 0.01f),
                        radius = r,
                        center = Offset(r + 14f, size.height - r - 14f),
                        style  = Stroke(width = 1f)
                    )
                }
                for (i in 0..3) {
                    val r = 18f + i * 16f
                    drawCircle(
                        color  = GoldPrimary.copy(alpha = 0.055f - i * 0.01f),
                        radius = r,
                        center = Offset(size.width - r - 14f, r + 14f),
                        style  = Stroke(width = 1f)
                    )
                }
            }
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Juz number display
            Row(
                verticalAlignment     = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text          = juz.juz.toString(),
                    fontSize      = 52.sp,
                    fontWeight    = FontWeight.Black,
                    color         = GoldLight,
                    letterSpacing = (-2).sp,
                    lineHeight    = 60.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text          = "الْجُزْء",
                fontFamily    = AmiriFontFamily,
                fontSize      = 20.sp,
                color         = GoldPrimary.copy(alpha = 0.70f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier          = Modifier.fillMaxWidth(0.72f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = GoldPrimary.copy(alpha = 0.30f))
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .size(5.dp)
                        .background(GoldPrimary.copy(alpha = 0.55f), CircleShape)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = GoldPrimary.copy(alpha = 0.30f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text       = juz.startSurahNama,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = PureWhite.copy(alpha = 0.85f),
                textAlign  = TextAlign.Center
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.width(28.dp).height(1.dp).background(GoldPrimary.copy(alpha = 0.4f)))
                Text(
                    text          = "  hingga  ",
                    fontSize      = 10.sp,
                    color         = GoldPrimary.copy(alpha = 0.55f),
                    fontStyle     = FontStyle.Italic,
                    letterSpacing = 1.sp
                )
                Box(modifier = Modifier.width(28.dp).height(1.dp).background(GoldPrimary.copy(alpha = 0.4f)))
            }
            Text(
                text       = juz.endSurahNama,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = PureWhite.copy(alpha = 0.85f),
                textAlign  = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── NEW: Stats row — total ayat ──────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Canvas(Modifier.size(3.dp)) { drawCircle(GoldDeep.copy(alpha = 0.50f)) }
                Text(
                    text          = "${juz.ayat.size} Ayat",
                    fontSize      = 10.sp,
                    color         = GoldPrimary.copy(alpha = 0.60f),
                    letterSpacing = 0.8.sp,
                    fontWeight    = FontWeight.Medium
                )
                Canvas(Modifier.size(3.dp)) { drawCircle(GoldDeep.copy(alpha = 0.50f)) }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Play Full Juz button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        if (isPlayingFull) SolidColor(GoldPrimary)
                        else Brush.horizontalGradient(listOf(GoldDeep, GoldPrimary, GoldLight))
                    )
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onPlayFullClick() }
                    .padding(horizontal = 30.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector        = if (isPlayingFull) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint               = CharcoalDeep,
                        modifier           = Modifier.size(18.dp)
                    )
                    Text(
                        text          = if (isPlayingFull) "Berhenti" else "Putar Audio Juz",
                        fontSize      = 12.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = CharcoalDeep,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  SURAH SECTION HEADER
//     Perubahan:
//       • Terima `isDark` dan `ayatCount`
//       • FIX 5: Chip jumlah ayat di kanan (konteks yang sangat berguna)
//       • Background tetap gelap — konsisten sebagai "chapter marker"
//         yang berfungsi sebagai pembatas visual yang kuat di kedua mode
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LuxurySurahSectionHeader(
    surahName  : String,
    surahArabic: String,
    surahNo    : Int,
    ayatCount  : Int,
    isDark     : Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(CharcoalDeep, CharcoalMid, CharcoalDeep)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        GoldPrimary.copy(alpha = 0.55f),
                        GoldPrimary.copy(alpha = 0.20f),
                        GoldPrimary.copy(alpha = 0.55f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .drawBehind {
                drawRoundRect(
                    brush       = Brush.verticalGradient(listOf(GoldLight, GoldPrimary, GoldLight)),
                    topLeft     = Offset(0f, size.height * 0.2f),
                    size        = Size(3.5f, size.height * 0.6f),
                    cornerRadius = CornerRadius(4f)
                )
            }
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Row(
            modifier             = Modifier.fillMaxWidth(),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Kiri: nomor + nama latin
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GoldPrimary.copy(alpha = 0.15f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text          = surahNo.toString().padStart(3, '0'),
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = GoldLight,
                        letterSpacing = 1.sp
                    )
                }
                Column {
                    Text(
                        text          = surahName,
                        fontSize      = 13.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = PureWhite.copy(alpha = 0.92f),
                        letterSpacing = 0.3.sp
                    )
                    // ── FIX 5: Jumlah ayat ─────────────────────────────────────
                    Text(
                        text          = "$ayatCount ayat",
                        fontSize      = 9.sp,
                        color         = GoldPrimary.copy(alpha = 0.55f),
                        fontWeight    = FontWeight.Normal,
                        letterSpacing = 0.4.sp
                    )
                }
            }

            // Kanan: nama Arab
            if (surahArabic.isNotEmpty()) {
                Text(
                    text       = surahArabic,
                    fontFamily = AmiriFontFamily,
                    fontSize   = 18.sp,
                    color      = GoldLight.copy(alpha = 0.85f),
                    textAlign  = TextAlign.End
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  JUZ AYAH ITEM
//
// Sebelumnya:
//   1. Card selalu PureWhite/PlayingTint — putih terang di dark mode → silau
//   2. Left accent strip HANYA muncul saat playing → scroll terlihat monoton
//   3. Arabic color hanya dua state: TextPrimary / GoldDeep
//      Di dark mode TextPrimary = #1C1C1E → hampir tidak terbaca di atas DarkCardSurface
//   4. Translation: 12sp → terlalu kecil untuk teks panjang
//   5. Translation color: TextSecondary = #6B6B6B → terlalu gelap di dark mode
//
// Sekarang:
//   1. Card color: 4 state (playing/idle × dark/light)
//   2. Left accent strip SELALU ada — tebal dan emas saat playing,
//      tipis dan sangat muted saat idle (3dp → 2dp, low alpha)
//      Ini menciptakan ritme visual yang konsisten saat scroll
//   3. Arabic color adaptif per mode
//   4. Translation: 12sp → 14sp, lineHeight 20 → 24sp
//   5. Translation color adaptif per mode
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun JuzAyahItem(
    ayah           : Ayah,
    isPlaying      : Boolean,
    isDark         : Boolean,
    isBookmarked   : Boolean,
    onPlayClick    : () -> Unit,
    onTafsirClick  : () -> Unit,
    onBookmarkClick: () -> Unit
) {
    // ── FIX 1: Card color adaptif ─────────────────────────────────────────────
    val cardColor by animateColorAsState(
        targetValue = when {
            isPlaying && isDark -> DarkCardPlaying
            isPlaying           -> PlayingTint
            isDark              -> DarkCardSurface
            else                -> PureWhite
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label         = "card_color"
    )
    val borderAlpha by animateFloatAsState(
        targetValue   = if (isPlaying) 0.60f else if (isDark) 0.14f else 0.18f,
        label         = "border_alpha"
    )
    // ── FIX 3: Arabic color adaptif ──────────────────────────────────────────
    val arabicColor by animateColorAsState(
        targetValue = when {
            isPlaying           -> GoldDeep
            isDark              -> Color.White.copy(alpha = 0.88f)
            else                -> TextPrimary
        },
        label = "arabic_color"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardColor)
            .border(
                width = if (isPlaying) 1.5.dp else 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        GoldPrimary.copy(alpha = borderAlpha),
                        GoldPrimary.copy(alpha = borderAlpha * 0.35f),
                        GoldPrimary.copy(alpha = borderAlpha)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .drawBehind {
                // ── FIX 2: Left accent strip SELALU tampil ────────────────────
                // Playing → gradient emas penuh (3dp)
                // Idle    → gold sangat muted (2dp) — ada tapi sangat soft
                if (isPlaying) {
                    drawRoundRect(
                        brush        = Brush.verticalGradient(listOf(GoldLight, GoldPrimary, GoldLight)),
                        topLeft      = Offset(0f, size.height * 0.15f),
                        size         = Size(4f, size.height * 0.70f),
                        cornerRadius = CornerRadius(4f)
                    )
                } else {
                    drawRoundRect(
                        color        = GoldDeep.copy(alpha = if (isDark) 0.18f else 0.12f),
                        topLeft      = Offset(0f, size.height * 0.25f),
                        size         = Size(2.5f, size.height * 0.50f),
                        cornerRadius = CornerRadius(4f)
                    )
                }
            }
    ) {
        Column(modifier = Modifier.padding(15.dp)) {

            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment    = Alignment.CenterVertically
            ) {
                JuzAyatNumberBadge(number = ayah.ayahNumber, isPlaying = isPlaying)

                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    JuzActionIcon(
                        icon        = Icons.Default.MenuBook,
                        description = "Tafsir",
                        onClick     = onTafsirClick,
                        tint        = GoldDeep,
                        isDark      = isDark
                    )
                    JuzActionIcon(
                        icon        = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        description = "Bookmark",
                        onClick     = onBookmarkClick,
                        tint        = if (isBookmarked) GoldPrimary else GoldDeep.copy(alpha = 0.45f),
                        filled      = isBookmarked,
                        isDark      = isDark
                    )
                    JuzPlayButton(isPlaying = isPlaying, onClick = onPlayClick)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Arabic text
            Text(
                text       = ayah.arab,
                fontFamily = AmiriFontFamily,
                fontSize   = 26.sp,
                lineHeight = 50.sp,
                textAlign  = TextAlign.End,
                color      = arabicColor,
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color    = DividerGold,
                modifier = Modifier.fillMaxWidth(0.38f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── FIX 4 & 5: Translation size + color adaptif ──────────────────
            // Sebelum: 12sp, TextSecondary (#6B6B6B) di semua mode
            // Sekarang: 14sp, lineHeight 24sp, warm muted adaptif per mode
            Text(
                text       = ayah.translation,
                fontSize   = 14.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
                color      = if (isDark)
                    Color.White.copy(alpha = 0.52f)
                else
                    TextSecondary,
                modifier   = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  AYAT NUMBER BADGE (Diamond style)
//
// FIX: Counter-rotation angka di dalam diamond
// Sebelumnya: Box di-rotate 45°, tapi Text tidak di-counter-rotate
//   → angka miring di dalam diamond (tidak kentara tapi tidak presisi)
// Sekarang: Text di-counter-rotate dengan graphicsLayer { rotationZ = -45f }
//   → angka selalu tegak lurus di dalam diamond
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun JuzAyatNumberBadge(number: Int, isPlaying: Boolean) {
    Box(
        modifier         = Modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer { rotationZ = 45f }
                .clip(RoundedCornerShape(5.dp))
                .border(
                    width = 1.5.dp,
                    brush = if (isPlaying) GoldGradient
                    else Brush.linearGradient(
                        listOf(GoldPrimary.copy(0.40f), GoldLight.copy(0.40f))
                    ),
                    shape = RoundedCornerShape(5.dp)
                )
                .background(
                    color = if (isPlaying) GoldShimmer.copy(0.75f) else GoldShimmer.copy(0.28f),
                    shape = RoundedCornerShape(5.dp)
                )
        )
        // ── FIX: Counter-rotate teks agar selalu tegak ────────────────────────
        Text(
            text       = number.toString(),
            fontSize   = if (number >= 100) 8.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            color      = if (isPlaying) GoldDeep else GoldDeep.copy(alpha = 0.65f),
            modifier   = Modifier.graphicsLayer { rotationZ = -45f }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  ACTION ICON
//     Perubahan: terima `isDark` untuk background saat filled di dark mode
//     Sebelum: GoldShimmer.copy(0.50f) → hampir invisible di dark card
//     Sekarang: GoldPrimary.copy(0.22f) di dark mode → lebih visible
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun JuzActionIcon(
    icon       : androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick    : () -> Unit,
    tint       : Color,
    isDark     : Boolean,
    filled     : Boolean = false
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                when {
                    filled && isDark -> GoldPrimary.copy(alpha = 0.22f)
                    filled           -> GoldShimmer.copy(alpha = 0.50f)
                    else             -> Color.Transparent
                }
            )
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = description,
            tint               = tint,
            modifier           = Modifier.size(18.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  PLAY BUTTON — tidak berubah, sudah premium
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun JuzPlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                if (isPlaying) GoldPrimary
                else GoldShimmer.copy(alpha = 0.55f)
            )
            .border(
                width = 1.dp,
                color = if (isPlaying) GoldDeep else GoldPrimary.copy(alpha = 0.28f),
                shape = CircleShape
            )
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Stop" else "Play",
            tint               = if (isPlaying) CharcoalDeep else GoldDeep,
            modifier           = Modifier.size(17.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  LOADING
//
// Sebelumnya:
//   • CircularProgressIndicator Material standar dengan warna gold
//   • Terlihat identik dengan loading screen app biasa
//
// Sekarang:
//   • Tiga cincin konsentris berdenyut dengan offset timing berbeda
//     (seperti gelombang suara Al-Quran mengalun keluar)
//   • Radius tumbuh dan menyusut dengan fase berbeda — efek organik
//   • InfiniteTransition — tidak ada layout jump
//   • Teks Arab + nomor juz di tengah
//   • Adaptif dark mode (teks muted lebih terang di dark)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LuxuryJuzLoading(nomor: Int, isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    // Ring 1 — terluar, paling lambat
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue  = 0.06f, targetValue = 0.20f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "r1a"
    )
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue  = 0.75f, targetValue = 1.00f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "r1s"
    )

    // Ring 2 — tengah, delay 600ms
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue  = 0.12f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1600, delayMillis = 300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "r2a"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue  = 0.80f, targetValue = 1.00f,
        animationSpec = infiniteRepeatable(tween(1600, delayMillis = 300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "r2s"
    )

    // Ring 3 — terdalam, paling cepat
    val ring3Alpha by infiniteTransition.animateFloat(
        initialValue  = 0.20f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1200, delayMillis = 600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "r3a"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Concentric pulse rings
            Box(
                modifier         = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Ring 1 — terluar
                Canvas(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer { scaleX = ring1Scale; scaleY = ring1Scale }
                ) {
                    drawCircle(
                        color  = GoldPrimary.copy(alpha = ring1Alpha),
                        radius = size.minDimension / 2f,
                        style  = Stroke(width = 1.dp.toPx())
                    )
                }
                // Ring 2 — tengah
                Canvas(
                    modifier = Modifier
                        .size(84.dp)
                        .graphicsLayer { scaleX = ring2Scale; scaleY = ring2Scale }
                ) {
                    drawCircle(
                        brush  = Brush.radialGradient(
                            colors = listOf(GoldPrimary.copy(alpha = ring2Alpha), Color.Transparent)
                        ),
                        radius = size.minDimension / 2f
                    )
                    drawCircle(
                        color  = GoldLight.copy(alpha = ring2Alpha * 0.8f),
                        radius = size.minDimension / 2f,
                        style  = Stroke(width = 1.5.dp.toPx())
                    )
                }
                // Ring 3 — terdalam + titik tengah
                Canvas(modifier = Modifier.size(50.dp)) {
                    drawCircle(
                        brush  = Brush.radialGradient(
                            colors = listOf(GoldPrimary.copy(alpha = ring3Alpha * 0.6f), Color.Transparent)
                        ),
                        radius = size.minDimension / 2f
                    )
                    drawCircle(
                        color  = GoldPrimary.copy(alpha = ring3Alpha),
                        radius = 5.dp.toPx()
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text          = "Memuat Juz $nomor",
                    fontSize      = 13.sp,
                    color         = if (isDark) Color.White.copy(0.45f) else TextSecondary,
                    letterSpacing = 0.5.sp,
                    fontWeight    = FontWeight.Light
                )
                Text(
                    text       = "الْجُزْء",
                    fontFamily = AmiriFontFamily,
                    fontSize   = 22.sp,
                    color      = GoldPrimary.copy(alpha = 0.60f)
                )
            }
        }
    }
}
