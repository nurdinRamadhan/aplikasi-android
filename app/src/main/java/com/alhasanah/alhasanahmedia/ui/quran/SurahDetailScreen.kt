package com.alhasanah.alhasanahmedia.ui.quran

// ─────────────────────────────────────────────────────────────────────────────
// Imports
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.quran.Ayah
import com.alhasanah.alhasanahmedia.data.model.quran.QuranQori
import com.alhasanah.alhasanahmedia.data.model.quran.SurahDetail
import com.alhasanah.alhasanahmedia.data.model.quran.TafsirItem
import com.alhasanah.alhasanahmedia.ui.components.AppGradientBackground
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.ui.theme.AmiriFontFamily
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Gold Brand Palette — Fixed (tidak berubah antara light/dark)
// Hanya background/surface/text yang beradaptasi ke tema
// ─────────────────────────────────────────────────────────────────────────────

private val GoldPrimary  = Color(0xFFD4A017)   // Core brand gold
private val GoldLight    = Color(0xFFE8C55A)   // Lighter gold (on dark bg)
private val GoldDeep     = Color(0xFFAA7C1F)   // Deep gold (on light bg)
private val GoldShimmer  = Color(0xFFFAF0C0)   // Very light gold tint (light mode)
private val GoldMuted    = Color(0xFFF5E6A3)   // Muted gold tint

// Gradients — Gold brand identity (unchanged)
private val GoldGradient = Brush.linearGradient(
    colors = listOf(GoldDeep, GoldPrimary, GoldLight, GoldPrimary, GoldDeep)
)

// Header Card — intentionally dark both light & dark mode (luxury mushaf aesthetic)
// Light:  warm dark (#12100A) — not cold black
// Adapted per composable using isSystemInDarkTheme()
private val HeaderGradientLight = Brush.verticalGradient(
    colors = listOf(Color(0xFF12100A), Color(0xFF1E1A0F))
)
private val HeaderGradientDark = Brush.verticalGradient(
    colors = listOf(Color(0xFF0E0C08), Color(0xFF181410))
)
private val GoldRadialGlow = Brush.radialGradient(
    colors = listOf(GoldPrimary.copy(alpha = 0.18f), Color.Transparent),
    radius = 220f
)

// ─────────────────────────────────────────────────────────────────────────────
// SurahDetailScreen — Root  (logika tidak berubah)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahDetailScreen(
    nomor: Int,
    navController: NavController,
    viewModel: SurahDetailViewModel = koinViewModel { parametersOf(nomor) }
) {
    val uiState            by viewModel.uiState.collectAsState()
    val tafsirState        by viewModel.tafsirState.collectAsState()
    val currentPlayingAyat by viewModel.currentPlayingAyat.collectAsState()
    val selectedQori       by viewModel.selectedQori.collectAsState()

    var selectedAyatForTafsir by remember { mutableStateOf<Int?>(null) }

    // ── Theme-adaptive background ───────────────────────────────────────────
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppGradientBackground(isDark = isSystemInDarkTheme())

        when (val state = uiState) {
            is QuranUiState.Loading -> {
                LuxuryDetailLoading()
            }
            is QuranUiState.Error -> {
                NoInternetScreen(
                    message = state.message,
                    onRetry = { viewModel.fetchSurahDetail() }
                )
            }
            is QuranUiState.Success -> {
                val bookmarks by viewModel.bookmarks.collectAsState()
                val availableQori = remember(state.data) { state.data.availableQori() }
                Column(modifier = Modifier.fillMaxSize()) {
                    // ── Gradient TopBar — unified with app design system ─────
                    LuxuryDetailTopBar(
                        surah  = state.data,
                        onBack = { navController.popBackStack() }
                    )
                    // ── Content ──────────────────────────────────────────────
                    SurahContent(
                        surah              = state.data,
                        currentPlayingAyat = currentPlayingAyat,
                        selectedQori       = selectedQori,
                        availableQori      = availableQori,
                        bookmarks          = bookmarks,
                        onPlayAudio        = { ayah -> viewModel.playAyah(ayah) },
                        onPlayFullSurah    = { viewModel.playFullSurah() },
                        onQoriSelected     = { qoriId -> viewModel.selectQori(qoriId) },
                        onTafsirClick      = { ayatNo ->
                            selectedAyatForTafsir = ayatNo
                            viewModel.fetchTafsir(ayatNo)
                        },
                        onBookmarkClick    = { ayatNo -> viewModel.toggleBookmark(ayatNo) }
                    )
                }
            }
        }
    }

    // ── Tafsir Sheet (logika tidak berubah) ────────────────────────────────
    selectedAyatForTafsir?.let { ayatNo ->
        TafsirBottomSheet(
            ayatNo    = ayatNo,
            state     = tafsirState,
            onDismiss = {
                selectedAyatForTafsir = null
                viewModel.clearTafsir()
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Background — Islamic Star Pattern (same language as HomeContent)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuranPageBackground() {
    val primary = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "quranBg")
    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(160_000, easing = LinearEasing)),
        label         = "quranBgRot"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 92.dp.toPx()
        val starR   = 14.dp.toPx()
        val cols    = (size.width  / spacing).toInt() + 2
        val rows    = (size.height / spacing).toInt() + 2
        val c       = primary.copy(alpha = 0.032f)

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
// TopBar — Gradient Brand Header (unified with all app screens)
// BEFORE: Hardcoded CharcoalDeep — broken in light mode, inconsistent
// AFTER : Brand primary gradient — consistent, adaptive
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LuxuryDetailTopBar(surah: SurahDetail, onBack: () -> Unit) {
    val isDark  = isSystemInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary

    val headerBrush = if (isDark) {
        Brush.verticalGradient(
            0.0f to primary.copy(alpha = 0.93f),
            1.0f to primary.copy(alpha = 0.82f)
        )
    } else {
        Brush.verticalGradient(
            0.0f to primary,
            1.0f to primary.copy(alpha = 0.90f)
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        AppPageHeaderBackground(isDark = isDark, modifier = Modifier.matchParentSize())
        // ── Back button ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(
                    width = 1.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.50f),
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.50f),
                            Color.White.copy(alpha = 0.0f)
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onBack() }
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.ArrowBack,
                contentDescription = "Kembali",
                tint               = Color.White,
                modifier           = Modifier.size(20.dp)
            )
        }

        // ── Center info ───────────────────────────────────────────────────
        Column(
            modifier            = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text          = surah.nameLatin,
                fontSize      = 16.sp,
                fontWeight    = FontWeight.Black,
                color         = Color.White,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text          = "${surah.nomor.toString().padStart(3, '0')}  ·  ${surah.jumlahAyat} AYAT  ·  ${surah.tempatTurun.uppercase()}",
                    fontSize      = 8.sp,
                    color         = Color.White.copy(alpha = 0.68f),
                    letterSpacing = 1.8.sp,
                    fontWeight    = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SurahContent — LazyColumn (logika tidak berubah)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SurahContent(
    surah: SurahDetail,
    currentPlayingAyat: Int?,
    selectedQori: QuranQori,
    availableQori: List<QuranQori>,
    bookmarks: Set<String>,
    onPlayAudio: (Ayah) -> Unit,
    onPlayFullSurah: () -> Unit,
    onQoriSelected: (String) -> Unit,
    onTafsirClick: (Int) -> Unit,
    onBookmarkClick: (Int) -> Unit
) {
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start  = 16.dp,
            end    = 16.dp,
            top    = 16.dp,
            bottom = 40.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Hero Header Card ──────────────────────────────────────────────
        item {
            SurahHeaderCard(
                surah            = surah,
                isPlayingFull    = currentPlayingAyat == 0,
                selectedQori     = selectedQori,
                availableQori    = availableQori,
                onQoriSelected   = onQoriSelected,
                onPlayFullClick = onPlayFullSurah
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Bismillah Banner ──────────────────────────────────────────────
        if (surah.nomor != 1 && surah.nomor != 9) {
            item {
                BismillahBanner()
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // ── Ayat Items ────────────────────────────────────────────────────
        items(surah.ayahs) { ayah ->
            AyatItem(
                ayah            = ayah,
                isPlaying       = currentPlayingAyat == ayah.ayahNumber,
                isBookmarked    = bookmarks.contains("${surah.nomor}:${ayah.ayahNumber}"),
                onPlayClick     = { onPlayAudio(ayah) },
                onTafsirClick   = { onTafsirClick(ayah.ayahNumber) },
                onBookmarkClick = { onBookmarkClick(ayah.ayahNumber) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SurahHeaderCard — Intentionally Dark (both light & dark mode)
// Seperti sampul mushaf mewah — selalu premium hitam-emas
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SurahHeaderCard(
    surah: SurahDetail,
    isPlayingFull: Boolean,
    selectedQori: QuranQori,
    availableQori: List<QuranQori>,
    onQoriSelected: (String) -> Unit,
    onPlayFullClick: () -> Unit
) {
    val isDark         = isSystemInDarkTheme()
    val headerGradient = if (isDark) HeaderGradientDark else HeaderGradientLight

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val playBtnScale      by animateFloatAsState(
        targetValue   = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "playBtnScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .shadow(
                elevation    = 20.dp,
                shape        = RoundedCornerShape(24.dp),
                ambientColor = GoldPrimary.copy(alpha = 0.30f),
                spotColor    = GoldPrimary.copy(alpha = 0.35f)
            )
            .background(headerGradient, RoundedCornerShape(24.dp))
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
                // Radial glow center
                drawCircle(
                    brush  = GoldRadialGlow,
                    radius = size.width * 0.6f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
                // Concentric ring ornaments — top-right & bottom-left corners
                for (i in 0..3) {
                    val radius = 20f + i * 18f
                    val alpha  = (0.07f - i * 0.015f).coerceAtLeast(0f)
                    // Top-right
                    drawArc(
                        color      = GoldPrimary.copy(alpha = alpha),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter  = false,
                        topLeft    = Offset(size.width - radius * 2 - 14f, 14f),
                        size       = Size(radius * 2, radius * 2),
                        style      = Stroke(width = 1f)
                    )
                    // Bottom-left
                    drawArc(
                        color      = GoldPrimary.copy(alpha = alpha),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter  = false,
                        topLeft    = Offset(14f, size.height - radius * 2 - 14f),
                        size       = Size(radius * 2, radius * 2),
                        style      = Stroke(width = 1f)
                    )
                }
                // Gold top accent line
                drawLine(
                    brush       = GoldGradient,
                    start       = Offset(60f, 0f),
                    end         = Offset(size.width - 60f, 0f),
                    strokeWidth = 2.5f
                )
                // Gold bottom accent line
                drawLine(
                    brush       = GoldGradient,
                    start       = Offset(60f, size.height),
                    end         = Offset(size.width - 60f, size.height),
                    strokeWidth = 1.5f
                )
            }
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Surah number chip ─────────────────────────────────────────
            Surface(
                shape = CircleShape,
                color = GoldPrimary.copy(alpha = 0.12f)
            ) {
                Text(
                    text     = "${surah.nomor.toString().padStart(3, '0')}",
                    style    = MaterialTheme.typography.labelSmall.copy(
                        color         = GoldLight.copy(alpha = 0.75f),
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 3.sp,
                        fontSize      = 9.sp
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Arabic name ───────────────────────────────────────────────
            Text(
                text       = surah.nama,
                fontFamily = AmiriFontFamily,
                fontSize   = 46.sp,
                color      = GoldLight,
                textAlign  = TextAlign.Center,
                lineHeight = 56.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ── Latin name ────────────────────────────────────────────────
            Text(
                text          = surah.nameLatin.uppercase(),
                fontSize      = 13.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 5.sp,
                color         = Color.White.copy(alpha = 0.90f),
                textAlign     = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(5.dp))

            // ── Meaning ───────────────────────────────────────────────────
            Text(
                text      = "\" ${surah.arti} \"",
                fontSize  = 12.sp,
                fontStyle = FontStyle.Italic,
                color     = GoldPrimary.copy(alpha = 0.72f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ── Ornamental gold divider ───────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(0.80f),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(0.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, GoldPrimary.copy(alpha = 0.45f))
                            )
                        )
                )
                Text(
                    text     = "  ✦  ",
                    color    = GoldPrimary.copy(alpha = 0.60f),
                    fontSize = 9.sp
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(0.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(GoldPrimary.copy(alpha = 0.45f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Info chips row ────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderInfoChip(label = surah.tempatTurun.uppercase())
                HeaderInfoChip(label = "${surah.jumlahAyat} AYAT")
                HeaderInfoChip(label = "JUZ ${surah.nomor}")
            }

            Spacer(modifier = Modifier.height(14.dp))

            QoriSelectorChip(
                selectedQori = selectedQori,
                availableQori = availableQori,
                onQoriSelected = onQoriSelected
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ── Play Full Surah Button ────────────────────────────────────
            Box(
                modifier = Modifier
                    .scale(playBtnScale)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        if (isPlayingFull)
                            Brush.horizontalGradient(listOf(GoldDeep, GoldPrimary))
                        else
                            Brush.horizontalGradient(listOf(GoldDeep, GoldPrimary, GoldLight))
                    )
                    .border(
                        width = 1.dp,
                        color = if (isPlayingFull) GoldLight.copy(alpha = 0.40f)
                                else GoldLight.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(50.dp)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication        = null
                    ) { onPlayFullClick() }
                    .padding(horizontal = 28.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector        = if (isPlayingFull) Icons.Default.Stop
                                            else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint               = Color(0xFF12100A),
                        modifier           = Modifier.size(18.dp)
                    )
                    Text(
                        text          = if (isPlayingFull) "Berhenti" else "Putar Murottal",
                        fontSize      = 12.sp,
                        fontWeight    = FontWeight.Black,
                        color         = Color(0xFF12100A),
                        letterSpacing = 1.sp
                    )
                }
            }

            // ── Playing state label ───────────────────────────────────────
            if (isPlayingFull) {
                Spacer(modifier = Modifier.height(10.dp))
                NowPlayingPulse()
            }
        }
    }
}

@Composable
private fun QoriSelectorChip(
    selectedQori: QuranQori,
    availableQori: List<QuranQori>,
    onQoriSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val choices = if (availableQori.isEmpty()) listOf(selectedQori) else availableQori

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
                .background(GoldPrimary.copy(alpha = 0.10f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 7.dp)
                .widthIn(max = 260.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.VolumeUp,
                contentDescription = null,
                tint = GoldLight.copy(alpha = 0.78f),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = selectedQori.displayName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = GoldLight.copy(alpha = 0.86f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Pilih qori",
                tint = GoldLight.copy(alpha = 0.70f),
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            choices.forEach { qori ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = qori.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        expanded = false
                        onQoriSelected(qori.id)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.VolumeUp,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun HeaderInfoChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
            .background(GoldPrimary.copy(alpha = 0.09f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text          = label,
            fontSize      = 8.sp,
            fontWeight    = FontWeight.Black,
            letterSpacing = 1.5.sp,
            color         = GoldLight.copy(alpha = 0.80f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NowPlayingPulse — Animated "sedang diputar" indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NowPlayingPulse() {
    val infiniteTransition = rememberInfiniteTransition(label = "nowPlaying")
    val bars = (1..4).map { i ->
        infiniteTransition.animateFloat(
            initialValue  = 0.35f,
            targetValue   = 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(400 + i * 80, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$i"
        )
    }
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = Icons.Outlined.VolumeUp,
            contentDescription = null,
            tint               = GoldLight.copy(alpha = 0.65f),
            modifier           = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier.height(16.dp)
        ) {
            bars.forEach { anim ->
                val h by anim
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight(h)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.75f))
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text          = "Sedang Diputar",
            fontSize      = 9.sp,
            color         = GoldLight.copy(alpha = 0.65f),
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BismillahBanner — Enhanced with Canvas ornament (theme-adaptive)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BismillahBanner() {
    val isDark  = isSystemInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary

    // Breathing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "bismillah")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.06f,
        targetValue   = 0.13f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "bismillahGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        GoldPrimary.copy(alpha = if (isDark) 0.10f else 0.06f),
                        GoldPrimary.copy(alpha = if (isDark) 0.18f else 0.12f),
                        GoldPrimary.copy(alpha = if (isDark) 0.10f else 0.06f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        GoldPrimary.copy(alpha = 0.12f),
                        GoldPrimary.copy(alpha = 0.50f),
                        GoldPrimary.copy(alpha = 0.12f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .drawBehind {
                // Radial center glow — breathing effect
                drawCircle(
                    brush  = Brush.radialGradient(
                        colors = listOf(GoldPrimary.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.45f
                    ),
                    radius = size.width * 0.45f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
                // Top & bottom gold accent lines
                drawLine(
                    brush       = Brush.horizontalGradient(
                        listOf(Color.Transparent, GoldPrimary.copy(alpha = 0.45f), Color.Transparent)
                    ),
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    brush       = Brush.horizontalGradient(
                        listOf(Color.Transparent, GoldPrimary.copy(alpha = 0.45f), Color.Transparent)
                    ),
                    start       = Offset(0f, size.height),
                    end         = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 20.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
                fontFamily = AmiriFontFamily,
                fontSize   = 28.sp,
                color      = if (isDark) GoldLight else GoldDeep,
                textAlign  = TextAlign.Center,
                lineHeight = 44.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text      = "Dengan nama Allah Yang Maha Pengasih lagi Maha Penyayang",
                fontSize  = 9.sp,
                color     = if (isDark) GoldLight.copy(alpha = 0.45f)
                            else GoldDeep.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AyatItem — Theme-Adaptive  (FIXED dark mode: was always white card)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AyatItem(
    ayah: Ayah,
    isPlaying: Boolean,
    isBookmarked: Boolean,
    onPlayClick: () -> Unit,
    onTafsirClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    val isDark  = isSystemInDarkTheme()

    // ── Theme-adaptive playing/idle colors ────────────────────────────────
    val idleCardColor    = MaterialTheme.colorScheme.surface
    val playingCardColor = if (isDark) Color(0xFF1F1800) else Color(0xFFFFF8E1)

    val cardColor by animateColorAsState(
        targetValue   = if (isPlaying) playingCardColor else idleCardColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label         = "card_color"
    )
    val borderAlpha by animateFloatAsState(
        targetValue   = if (isPlaying) 0.60f else 0.16f,
        label         = "border_alpha"
    )

    // Arabic text color — readable in both themes
    val arabicColor by animateColorAsState(
        targetValue   = when {
            isPlaying && isDark  -> GoldLight
            isPlaying && !isDark -> GoldDeep
            isDark               -> MaterialTheme.colorScheme.onSurface
            else                 -> Color(0xFF1C1C1E)
        },
        label = "arabic_color"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardColor)
            .border(
                width = if (isPlaying) 1.5.dp else 0.8.dp,
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
                if (isPlaying) {
                    // Left gold stripe pulse for active ayat
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(GoldLight, GoldPrimary, GoldLight)
                        ),
                        topLeft      = Offset(0f, size.height * 0.15f),
                        size         = Size(4f, size.height * 0.70f),
                        cornerRadius = CornerRadius(4f)
                    )
                }
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Action Row ────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                AyatNumberBadge(
                    number    = ayah.ayahNumber,
                    isPlaying = isPlaying,
                    isDark    = isDark
                )
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    LuxuryActionIcon(
                        icon        = Icons.Outlined.MenuBook,
                        description = "Tafsir",
                        onClick     = onTafsirClick,
                        tint        = if (isDark) GoldLight.copy(alpha = 0.70f) else GoldDeep,
                        filled      = false,
                        isDark      = isDark
                    )
                    LuxuryActionIcon(
                        icon        = if (isBookmarked) Icons.Default.Bookmark
                                      else Icons.Outlined.BookmarkBorder,
                        description = "Bookmark",
                        onClick     = onBookmarkClick,
                        tint        = if (isBookmarked) GoldPrimary
                                      else if (isDark) GoldLight.copy(alpha = 0.35f)
                                      else GoldDeep.copy(alpha = 0.45f),
                        filled      = isBookmarked,
                        isDark      = isDark
                    )
                    PlayActionButton(
                        isPlaying = isPlaying,
                        isDark    = isDark,
                        onClick   = onPlayClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Arabic Text ───────────────────────────────────────────────
            Text(
                text       = ayah.arab,
                fontFamily = AmiriFontFamily,
                fontSize   = 28.sp,
                lineHeight = 54.sp,
                textAlign  = TextAlign.End,
                color      = arabicColor,
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Centered ornamental divider ────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(0.50f)
                    .align(Alignment.CenterHorizontally),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(0.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, GoldPrimary.copy(alpha = 0.30f))
                            )
                        )
                )
                Text(
                    text     = "  ✦  ",
                    color    = GoldPrimary.copy(alpha = 0.35f),
                    fontSize = 7.sp
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(0.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(GoldPrimary.copy(alpha = 0.30f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Translation ───────────────────────────────────────────────
            Text(
                text       = ayah.translation,
                fontSize   = 13.sp,
                lineHeight = 22.sp,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier   = Modifier.fillMaxWidth()
            )

            // ── Playing indicator row ─────────────────────────────────────
            if (isPlaying) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    NowPlayingPulse()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AyatNumberBadge — Diamond shape, theme-adaptive
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AyatNumberBadge(number: Int, isPlaying: Boolean, isDark: Boolean) {
    val badgeBg = when {
        isPlaying && isDark  -> GoldPrimary.copy(alpha = 0.25f)
        isPlaying && !isDark -> GoldShimmer.copy(alpha = 0.80f)
        isDark               -> GoldPrimary.copy(alpha = 0.12f)
        else                 -> GoldShimmer.copy(alpha = 0.45f)
    }

    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer { rotationZ = 45f }
                .clip(RoundedCornerShape(7.dp))
                .border(
                    width = 1.5.dp,
                    brush = if (isPlaying) GoldGradient
                            else Brush.linearGradient(
                                listOf(
                                    GoldPrimary.copy(alpha = 0.45f),
                                    GoldLight.copy(alpha = 0.30f)
                                )
                            ),
                    shape = RoundedCornerShape(7.dp)
                )
                .background(badgeBg, RoundedCornerShape(7.dp))
        )
        Text(
            text       = number.toString(),
            fontSize   = if (number >= 100) 9.sp else 11.sp,
            fontWeight = FontWeight.Black,
            color      = if (isPlaying) GoldDeep
                         else if (isDark) GoldLight.copy(alpha = 0.75f)
                         else GoldDeep.copy(alpha = 0.75f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LuxuryActionIcon — Theme-adaptive filled state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LuxuryActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color,
    filled: Boolean = false,
    isDark: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "iconPressScale"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(pressScale)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    filled && isDark  -> GoldPrimary.copy(alpha = 0.18f)
                    filled && !isDark -> GoldShimmer.copy(alpha = 0.60f)
                    else              -> Color.Transparent
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = description,
            tint               = tint,
            modifier           = Modifier.size(19.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PlayActionButton — Theme-adaptive, consistent in both modes
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayActionButton(isPlaying: Boolean, isDark: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "playBtnPressScale"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(pressScale)
            .clip(CircleShape)
            .background(
                if (isPlaying)
                    Brush.linearGradient(listOf(GoldDeep, GoldPrimary))
                else
                    Brush.linearGradient(
                        listOf(
                            GoldPrimary.copy(alpha = if (isDark) 0.22f else 0.18f),
                            GoldPrimary.copy(alpha = if (isDark) 0.12f else 0.08f)
                        )
                    )
            )
            .border(
                width = 1.dp,
                color = if (isPlaying) GoldLight.copy(alpha = 0.45f)
                        else GoldPrimary.copy(alpha = 0.35f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Stop" else "Play",
            tint               = if (isPlaying) Color(0xFF12100A)
                                 else if (isDark) GoldLight else GoldDeep,
            modifier           = Modifier.size(18.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LuxuryDetailLoading — Enhanced shimmer skeleton
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LuxuryDetailLoading() {
    val isDark  = isSystemInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary

    // Rotating spinner ornament
    val infiniteTransition = rememberInfiniteTransition(label = "loadingAnim")
    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label         = "loadRotation"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.30f,
        targetValue   = 0.85f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Spinner with glow ring ─────────────────────────────────────
            Box(
                modifier         = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Rotating outer gold ring
                    rotate(rotation) {
                        drawArc(
                            brush      = Brush.sweepGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    GoldPrimary.copy(alpha = 0.80f),
                                    Color.Transparent
                                ),
                                center = center
                            ),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter  = false,
                            style      = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Static glow
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    GoldPrimary.copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                CircularProgressIndicator(
                    color       = GoldPrimary.copy(alpha = glowAlpha),
                    trackColor  = GoldPrimary.copy(alpha = 0.08f),
                    strokeWidth = 2.dp,
                    modifier    = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bismillah arabic
            Text(
                text       = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
                fontFamily = AmiriFontFamily,
                fontSize   = 20.sp,
                color      = GoldPrimary.copy(alpha = 0.55f),
                textAlign  = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text          = "Memuat Surah…",
                fontSize      = 12.sp,
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight    = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Skeleton shimmer cards ─────────────────────────────────────
            repeat(2) {
                SkeletonCard(isDark = isDark)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SkeletonCard(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue  = -500f,
        targetValue   = 500f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label         = "skeletonX"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            (if (isDark) Color.White else GoldPrimary).copy(alpha = 0.07f),
            Color.Transparent
        ),
        start = Offset(shimmerX, 0f),
        end   = Offset(shimmerX + 300f, 200f)
    )
    val baseBg = if (isDark)
        MaterialTheme.colorScheme.surface
    else
        MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .clip(RoundedCornerShape(14.dp))
            .background(baseBg)
            .border(
                0.5.dp,
                GoldPrimary.copy(alpha = 0.12f),
                RoundedCornerShape(14.dp)
            )
            .drawBehind { drawRect(shimmerBrush) }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Arabic text placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.70f)
                    .height(18.dp)
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(6.dp))
                    .background(GoldPrimary.copy(alpha = if (isDark) 0.12f else 0.07f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(14.dp)
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(6.dp))
                    .background(GoldPrimary.copy(alpha = if (isDark) 0.08f else 0.05f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Translation placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
            )
        }
    }
}
