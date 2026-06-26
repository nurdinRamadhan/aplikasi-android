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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
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
import com.alhasanah.alhasanahmedia.data.model.quran.SurahListItem
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.ui.components.AppGradientBackground
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.ui.theme.AmiriFontFamily
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Gold Brand Palette — Fixed (sama dengan SurahDetailScreen)
// ─────────────────────────────────────────────────────────────────────────────

private val GoldPrimary  = Color(0xFFD4A017)
private val GoldLight    = Color(0xFFE8C55A)
private val GoldDeep     = Color(0xFFAA7C1F)
private val GoldShimmer  = Color(0xFFFAF0C0)
private val GoldMuted    = Color(0xFFF5E6A3)
private val WarmIvory    = Color(0xFFFFFCF7)
private val WarmParchment = Color(0xFFFBF3E6)
private val WarmInk      = Color(0xFF2A2318)
private val DarkInk      = Color(0xFF0B1519)

private val GoldGradient = Brush.linearGradient(
    colors = listOf(GoldDeep, GoldPrimary, GoldLight, GoldPrimary, GoldDeep)
)

// ─────────────────────────────────────────────────────────────────────────────
// QuranScreen — Root  (logika ViewModel tidak berubah)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    navController: NavController,
    viewModel: QuranViewModel = koinViewModel()
) {
    val surahState     by viewModel.filteredSurahList.collectAsState(initial = QuranUiState.Loading)
    val searchQuery    by viewModel.searchQuery.collectAsState()
    val bookmarks      by viewModel.bookmarks.collectAsState()
    val bookmarkedAyat by viewModel.bookmarkedAyatDetails.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Surah", "Juz", "Bookmark")

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppGradientBackground(isDark = isAppInDarkTheme())

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Unified Gradient Header Block ─────────────────────────────
            // TopBar + Hero Card + SearchBar + TabRow — satu blok yang kohesif
            QuranGradientHeader(
                searchQuery   = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                selectedTab   = selectedTab,
                tabs          = tabs,
                onTabSelected = { selectedTab = it },
                onBack        = { navController.popBackStack() }
            )

            // ── Content ───────────────────────────────────────────────────
            when (selectedTab) {
                0 -> SurahListContent(
                    state       = surahState,
                    navController = navController,
                    onRetry     = { viewModel.fetchSurahList() }
                )
                1 -> JuzListContent(navController)
                2 -> BookmarkListContent(
                    bookmarks        = bookmarks,
                    details          = bookmarkedAyat,
                    onNavigateToSurah = { surahNo ->
                        navController.navigate(Screen.SurahDetail.createRoute(surahNo))
                    },
                    onRemoveBookmark = { s, a -> viewModel.removeBookmark(s, a) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Background — Islamic Star Pattern (identical to SurahDetailScreen)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuranListBackground() {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()
    val infiniteTransition = rememberInfiniteTransition(label = "quranListBg")
    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(160_000, easing = LinearEasing)),
        label         = "quranListBgRot"
    )
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isDark) {
                        listOf(Color(0xFF0B1519), Color(0xFF101C20), MaterialTheme.colorScheme.background)
                    } else {
                        listOf(WarmIvory, WarmParchment, MaterialTheme.colorScheme.background)
                    }
                )
            )
    ) {
        val spacing = 92.dp.toPx()
        val starR   = 14.dp.toPx()
        val cols    = (size.width  / spacing).toInt() + 2
        val rows    = (size.height / spacing).toInt() + 2
        val c       = primary.copy(alpha = if (isDark) 0.024f else 0.018f)
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
// QuranGradientHeader — TopBar + Hero + SearchBar + TabRow
// Unified single gold gradient block — konsisten dengan semua screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuranGradientHeader(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    val isDark  = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val titleColor = if (isDark) GoldLight else GoldDeep
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.72f) else WarmInk.copy(alpha = 0.64f)
    val buttonBg = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.10f else 0.62f)

    val headerBrush = if (isDark) {
        Brush.verticalGradient(
            0.0f to DarkInk,
            0.62f to Color(0xFF101D21),
            1.0f  to MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
        )
    } else {
        Brush.verticalGradient(
            0.0f  to WarmIvory,
            0.62f to WarmParchment,
            1.0f  to MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        AppPageHeaderBackground(isDark = isDark, modifier = Modifier.matchParentSize())
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Row 1: Back + Title ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .border(
                            width = 1.dp,
                            color = primary.copy(alpha = 0.34f),
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
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint               = if (isDark) Color.White.copy(alpha = 0.86f) else WarmInk,
                        modifier           = Modifier.size(20.dp)
                    )
                }

                // Center title
                Column(
                    modifier            = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text          = "AL-QUR'AN DIGITAL",
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 2.2.sp,
                        color         = titleColor
                    )
                    Text(
                        text       = "Surah, Juz, dan bookmark",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color      = subtitleColor
                    )
                }
            }

            // ── Row 2: Hero Card — Islamic book cover aesthetic ───────────
            QuranHeroCard(isDark = isDark)

            Spacer(modifier = Modifier.height(10.dp))

            // ── Row 3: Search Bar ─────────────────────────────────────────
            QuranSearchBar(
                query        = searchQuery,
                onQueryChange = onQueryChange,
                isDark       = isDark,
                modifier     = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Row 4: Tab Row ────────────────────────────────────────────
            QuranTabRow(
                tabs          = tabs,
                selectedIndex = selectedTab,
                onTabSelected = onTabSelected,
                modifier      = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QuranHeroCard — Premium Islamic book cover card (always dark, like mushaf)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuranHeroCard(isDark: Boolean) {
    val bgBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF111E23), Color(0xFF0D171B)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF151B18), Color(0xFF20251E)))
    }

    // Shimmer sweep animation
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
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgBrush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        GoldPrimary.copy(alpha = 0.55f),
                        GoldLight.copy(alpha = 0.18f),
                        GoldPrimary.copy(alpha = 0.55f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .drawBehind {
                // Gold top accent line
                drawLine(
                    brush       = GoldGradient,
                    start       = Offset(36f, 0f),
                    end         = Offset(size.width - 36f, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                // Gold bottom accent line
                drawLine(
                    brush       = GoldGradient,
                    start       = Offset(48f, size.height),
                    end         = Offset(size.width - 48f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                // Radial center glow
                drawCircle(
                    brush  = Brush.radialGradient(
                        colors = listOf(GoldPrimary.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
                // Concentric ring ornament — top right corner
                for (i in 0..2) {
                    val r     = 16f + i * 14f
                    val alpha = (0.08f - i * 0.02f).coerceAtLeast(0f)
                    drawArc(
                        color      = GoldPrimary.copy(alpha = alpha),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter  = false,
                        topLeft    = Offset(size.width - r * 2 - 12f, 10f),
                        size       = Size(r * 2, r * 2),
                        style      = Stroke(width = 0.8f)
                    )
                }
                // Sweeping shimmer
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerX, 0f),
                        end   = Offset(shimmerX + 200f, size.height)
                    )
                )
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Arabic title
            Text(
                text       = "الْقُرْآنُ الْكَرِيمُ",
                fontFamily = AmiriFontFamily,
                fontSize   = 28.sp,
                color      = GoldLight,
                textAlign  = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Ornamental divider
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(0.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, GoldPrimary.copy(alpha = 0.55f))
                            )
                        )
                )
                Text(
                    text     = "  ✦  ",
                    color    = GoldPrimary.copy(alpha = 0.65f),
                    fontSize = 9.sp
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(0.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(GoldPrimary.copy(alpha = 0.55f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                HeroInfoPill("114 SURAH")
                HeroInfoPill("30 JUZ")
                HeroInfoPill("6.236 AYAT")
            }
        }
    }
}

@Composable
private fun HeroInfoPill(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .border(0.5.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
            .background(GoldPrimary.copy(alpha = 0.09f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text          = label,
            fontSize      = 7.sp,
            fontWeight    = FontWeight.Black,
            letterSpacing = 1.5.sp,
            color         = GoldLight.copy(alpha = 0.78f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QuranSearchBar — Adaptive per tema
// Light: field putih/krem transparan di atas gold header
// Dark : field gelap transparan
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuranSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val fieldBg     = if (isDark) Color(0xFF101D21).copy(alpha = 0.58f)
                      else Color.White.copy(alpha = 0.66f)
    val borderColor = if (isDark) GoldPrimary.copy(alpha = 0.35f)
                      else GoldPrimary.copy(alpha = 0.24f)
    val textColor   = MaterialTheme.colorScheme.onSurface
    val hintColor   = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)

    OutlinedTextField(
        value         = query,
        onValueChange = onQueryChange,
        modifier      = modifier.fillMaxWidth(),
        placeholder   = {
            Text(
                text     = "Cari nama atau nomor surah…",
                color    = hintColor,
                fontSize = 13.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector        = Icons.Default.Search,
                contentDescription = null,
                tint               = if (isDark) GoldLight.copy(alpha = 0.78f) else GoldDeep.copy(alpha = 0.78f),
                modifier           = Modifier.size(18.dp)
            )
        },
        trailingIcon = if (query.isNotEmpty()) ({
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Hapus pencarian",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
                    modifier           = Modifier.size(16.dp)
                )
            }
        }) else null,
        singleLine  = true,
        shape       = RoundedCornerShape(14.dp),
        colors      = OutlinedTextFieldDefaults.colors(
            focusedTextColor        = textColor,
            unfocusedTextColor      = textColor,
            focusedContainerColor   = fieldBg,
            unfocusedContainerColor = fieldBg,
            focusedBorderColor      = GoldPrimary.copy(alpha = 0.65f),
            unfocusedBorderColor    = borderColor,
            cursorColor             = GoldPrimary
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// QuranTabRow — Adaptive background, gold active tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuranTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val pillBg = if (isDark) Color(0xFF101D21).copy(alpha = 0.62f)
                 else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val pillBorder = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.16f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50.dp))
            .background(pillBg)
            .border(1.dp, pillBorder, RoundedCornerShape(50.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        tabs.forEachIndexed { i, label ->
            val isSelected = i == selectedIndex
            val bgColor by animateColorAsState(
                targetValue   = if (isSelected) GoldPrimary else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label         = "tab_bg_$i"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF12100A) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                label       = "tab_text_$i"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50.dp))
                    .background(bgColor)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTabSelected(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text          = label.uppercase(),
                    fontSize      = 10.sp,
                    fontWeight    = if (isSelected) FontWeight.Black else FontWeight.Medium,
                    letterSpacing = 1.8.sp,
                    color         = textColor
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SurahListContent — logika tidak berubah
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SurahListContent(
    state: QuranUiState<List<SurahListItem>>,
    navController: NavController,
    onRetry: () -> Unit
) {
    when (state) {
        is QuranUiState.Loading -> LuxuryLoadingIndicator()
        is QuranUiState.Error   -> NoInternetScreen(message = state.message, onRetry = onRetry)
        is QuranUiState.Success -> {
            if (state.data.isEmpty()) {
                LuxuryEmptyState("Surah tidak ditemukan")
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 10.dp, bottom = 128.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.data) { index, surah ->
                        LuxurySurahItem(
                            surah = surah,
                            index = index,
                            onClick = {
                                navController.navigate(Screen.SurahDetail.createRoute(surah.nomor))
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LuxurySurahItem — Theme-Adaptive + Press Animation
// BEFORE: CardSurface/PureWhite alternating — broken dark mode
// AFTER : MaterialTheme.colorScheme.surface + accent variants
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LuxurySurahItem(surah: SurahListItem, index: Int, onClick: () -> Unit) {
    val isDark = isAppInDarkTheme()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "surahItemScale"
    )

    // Subtle alternating — surface vs surfaceVariant (both theme-adaptive)
    val cardBg = if (index % 2 == 0)
        MaterialTheme.colorScheme.surface
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(
                width = 0.8.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        GoldPrimary.copy(alpha = 0.22f),
                        GoldPrimary.copy(alpha = 0.07f),
                        GoldPrimary.copy(alpha = 0.22f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                // Gold left accent bar — gradient fade
                drawRoundRect(
                    brush        = Brush.verticalGradient(
                        listOf(GoldLight, GoldPrimary, GoldLight)
                    ),
                    topLeft      = Offset(0f, size.height * 0.20f),
                    size         = Size(3.5f, size.height * 0.60f),
                    cornerRadius = CornerRadius(4f)
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication        = null
            ) { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ── Number Badge — Diamond / Octagonal ring ────────────────────
            SurahNumberBadge(number = surah.nomor, isDark = isDark)

            Spacer(modifier = Modifier.width(14.dp))

            // ── Surah Info ─────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text          = surah.nameLatin,
                    fontSize      = 15.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Revelation chip
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = GoldPrimary.copy(alpha = if (isDark) 0.18f else 0.10f)
                    ) {
                        Text(
                            text          = surah.tempatTurun.uppercase(),
                            fontSize      = 7.sp,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color         = if (isDark) GoldLight else GoldDeep,
                            modifier      = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text       = "${surah.jumlahAyat} Ayat",
                        fontSize   = 11.sp,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Arabic Name ────────────────────────────────────────────────
            Text(
                text       = surah.nama,
                fontFamily = AmiriFontFamily,
                fontSize   = 22.sp,
                color      = if (isDark) GoldLight else GoldDeep,
                textAlign  = TextAlign.End
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Chevron
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = GoldPrimary.copy(alpha = 0.45f),
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SurahNumberBadge(number: Int, isDark: Boolean) {
    Box(
        modifier         = Modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring with gold gradient border
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(1.5.dp, GoldGradient, CircleShape)
                .background(
                    if (isDark) GoldPrimary.copy(alpha = 0.14f)
                    else GoldShimmer.copy(alpha = 0.45f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = number.toString(),
                fontSize   = if (number >= 100) 10.sp else 12.sp,
                fontWeight = FontWeight.Black,
                color      = if (isDark) GoldLight else GoldDeep
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// JuzListContent — logika tidak berubah
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun JuzListContent(navController: NavController) {
    val juzStartSurah = mapOf(
        1 to "Al-Fatihah",  2 to "Al-Baqarah",   3 to "Al-Baqarah",
        4 to "Ali 'Imran",  5 to "An-Nisa'",      6 to "An-Nisa'",
        7 to "Al-Ma'idah",  8 to "Al-An'am",      9 to "Al-A'raf",
        10 to "Al-Anfal",   11 to "At-Tawbah",   12 to "Hud",
        13 to "Yusuf",      14 to "Al-Hijr",      15 to "Al-Isra'",
        16 to "Al-Kahf",    17 to "Al-Anbiya'",   18 to "Al-Mu'minun",
        19 to "Al-Furqan",  20 to "An-Naml",      21 to "Al-'Ankabut",
        22 to "Al-Ahzab",   23 to "Ya-Sin",       24 to "Az-Zumar",
        25 to "Fussilat",   26 to "Al-Ahqaf",     27 to "Adh-Dhariyat",
        28 to "Al-Mujadila",29 to "Al-Mulk",      30 to "An-Naba'"
    )
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 10.dp, bottom = 128.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed((1..30).toList()) { index, juz ->
            LuxuryJuzItem(
                juz        = juz,
                startSurah = juzStartSurah[juz] ?: "",
                index      = index,
                onClick    = {
                    navController.navigate(
                        Screen.JuzDetail.createRoute(juz)
                    )
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LuxuryJuzItem — Theme-Adaptive + Press Animation
// BEFORE: CardSurface/PureWhite hardcoded, muted number
// AFTER : surface-adaptive, clear number badge, gold arrow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LuxuryJuzItem(juz: Int, startSurah: String, index: Int, onClick: () -> Unit) {
    val isDark = isAppInDarkTheme()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "juzItemScale"
    )

    val cardBg = if (index % 2 == 0)
        MaterialTheme.colorScheme.surface
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(
                0.8.dp,
                Brush.horizontalGradient(
                    listOf(
                        GoldPrimary.copy(alpha = 0.20f),
                        GoldPrimary.copy(alpha = 0.06f),
                        GoldPrimary.copy(alpha = 0.20f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null
            ) { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ── Juz Number Orb ─────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) GoldPrimary.copy(alpha = 0.14f)
                        else GoldShimmer.copy(alpha = 0.50f),
                        CircleShape
                    )
                    .border(1.5.dp, GoldGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text          = juz.toString().padStart(2, '0'),
                    fontSize      = 15.sp,
                    fontWeight    = FontWeight.Black,
                    color         = if (isDark) GoldLight else GoldDeep,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text          = "JUZ $juz",
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.Black,
                    color         = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp
                )
                if (startSurah.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text       = "Dimulai dari $startSurah",
                            fontSize   = 11.sp,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // Gold arrow chip
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary.copy(alpha = if (isDark) 0.16f else 0.10f))
                    .border(0.5.dp, GoldPrimary.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint               = if (isDark) GoldLight else GoldDeep,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BookmarkListContent — logika tidak berubah
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BookmarkListContent(
    bookmarks: Set<String>,
    details: Map<String, Ayah>,
    onNavigateToSurah: (Int) -> Unit,
    onRemoveBookmark: (Int, Int) -> Unit
) {
    if (bookmarks.isEmpty()) {
        QuranEmptyBookmark()
    } else {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 10.dp, bottom = 128.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(bookmarks.toList().sorted()) { _, id ->
                val ayah    = details[id]
                val parts   = id.split(":")
                val surahNo = parts[0].toIntOrNull() ?: 1
                val ayatNo  = parts[1].toIntOrNull() ?: 1
                LuxuryBookmarkCard(
                    surahNo  = surahNo,
                    ayatNo   = ayatNo,
                    ayah     = ayah,
                    onClick  = { onNavigateToSurah(surahNo) },
                    onDelete = { onRemoveBookmark(surahNo, ayatNo) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QuranEmptyBookmark — Enhanced empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuranEmptyBookmark() {
    val isDark = isAppInDarkTheme()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(GoldPrimary.copy(alpha = if (isDark) 0.16f else 0.10f), Color.Transparent)
                        )
                    )
                    .border(1.dp, GoldGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Bookmark,
                    contentDescription = null,
                    tint               = GoldPrimary,
                    modifier           = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text       = "Belum Ada Ayat Tersimpan",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text      = "Simpan ayat favorit Anda saat membaca\nuntuk memudahkan tilawah berikutnya.",
                fontSize  = 12.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LuxuryBookmarkCard — Theme-Adaptive
// BEFORE: PureWhite bg, TextPrimary/TextSecondary hardcoded
// AFTER : surface-adaptive, all colors from MaterialTheme
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LuxuryBookmarkCard(
    surahNo: Int,
    ayatNo: Int,
    ayah: Ayah?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isAppInDarkTheme()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.978f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "bookmarkCardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        GoldPrimary.copy(alpha = 0.30f),
                        GoldPrimary.copy(alpha = 0.10f),
                        GoldPrimary.copy(alpha = 0.30f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null
            ) { onClick() }
    ) {
        // Gold gradient top strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(GoldGradient)
                .align(Alignment.TopCenter)
        )

        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {

            // ── Header row ────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint               = GoldPrimary,
                        modifier           = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text          = "Surah $surahNo  ·  Ayat $ayatNo",
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = if (isDark) GoldLight else GoldDeep,
                        letterSpacing = 0.5.sp
                    )
                }
                // Delete button — subtle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint               = MaterialTheme.colorScheme.error.copy(alpha = 0.70f),
                        modifier           = Modifier.size(15.dp)
                    )
                }
            }

            ayah?.let {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    color     = GoldPrimary.copy(alpha = 0.18f),
                    thickness = 0.5.dp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Arabic text
                Text(
                    text       = it.arab,
                    fontFamily = AmiriFontFamily,
                    fontSize   = 20.sp,
                    textAlign  = TextAlign.End,
                    modifier   = Modifier.fillMaxWidth(),
                    color      = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 33.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Translation
                Text(
                    text      = it.translation,
                    fontSize  = 12.sp,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis
                )
            } ?: run {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier   = Modifier.fillMaxWidth(),
                    color      = GoldPrimary,
                    trackColor = GoldPrimary.copy(alpha = 0.12f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LuxuryLoadingIndicator — Skeleton Shimmer (same pattern as SurahDetailScreen)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LuxuryLoadingIndicator() {
    val isDark = isAppInDarkTheme()
    val infiniteTransition = rememberInfiniteTransition(label = "quranLoading")
    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label         = "quranLoadRot"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.30f,
        targetValue   = 0.85f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "quranLoadGlow"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.fillMaxWidth()
        ) {
            // Spinner
            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
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
                CircularProgressIndicator(
                    color       = GoldPrimary.copy(alpha = glowAlpha),
                    trackColor  = GoldPrimary.copy(alpha = 0.08f),
                    strokeWidth = 2.dp,
                    modifier    = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text          = "الْقُرْآنُ الْكَرِيمُ",
                fontFamily    = AmiriFontFamily,
                fontSize      = 18.sp,
                color         = GoldPrimary.copy(alpha = 0.55f),
                textAlign     = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text          = "Memuat Al-Qur'an…",
                fontSize      = 12.sp,
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                fontWeight    = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Skeleton cards
            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { i ->
                    QuranSkeletonItem(isDark = isDark, index = i)
                }
            }
        }
    }
}

@Composable
private fun QuranSkeletonItem(isDark: Boolean, index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "skel_$index")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue  = -400f + index * 60f,
        targetValue   = 600f + index * 60f,
        animationSpec = infiniteRepeatable(
            tween(1600 + index * 80, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "skelX_$index"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            (if (isDark) Color.White else GoldPrimary).copy(alpha = 0.07f),
            Color.Transparent
        ),
        start = Offset(shimmerX, 0f),
        end   = Offset(shimmerX + 300f, 80f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, GoldPrimary.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .drawBehind { drawRect(shimmerBrush) }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Number badge placeholder
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary.copy(alpha = if (isDark) 0.10f else 0.06f))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.28f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f))
                )
            }
            // Arabic name placeholder
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(GoldPrimary.copy(alpha = if (isDark) 0.10f else 0.06f))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LuxuryEmptyState — Theme-Adaptive
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LuxuryEmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector        = Icons.Outlined.SearchOff,
                contentDescription = null,
                tint               = GoldPrimary.copy(alpha = 0.40f),
                modifier           = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text       = message,
                fontSize   = 14.sp,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center
            )
        }
    }
}
