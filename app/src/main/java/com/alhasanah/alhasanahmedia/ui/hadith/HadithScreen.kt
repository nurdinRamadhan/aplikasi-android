package com.alhasanah.alhasanahmedia.ui.hadith

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithItem
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithSearchItem
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.ui.components.AppGradientBackground
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.ui.theme.AmiriFontFamily
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val GoldPrimary = Color(0xFFD4A017)
private val GoldLight = Color(0xFFE8C55A)
private val GoldDeep = Color(0xFFAA7C1F)
private val WarmIvory = Color(0xFFFFFCF7)
private val WarmParchment = Color(0xFFFBF3E6)
private val WarmInk = Color(0xFF2A2318)
private val DarkInk = Color(0xFF0B1519)

@Composable
fun HadithScreen(
    navController: NavController,
    viewModel: HadithViewModel = koinViewModel()
) {
    val state by viewModel.listState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppGradientBackground(isDark = isAppInDarkTheme())

        Column(modifier = Modifier.fillMaxSize()) {
            HadithHeader(
                searchQuery = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onBack = { navController.popBackStack() },
                onRefresh = {
                    if (state.isSearchMode) viewModel.searchHadith(page = 1)
                    else state.selectedGroup?.let(viewModel::openRangeGroup)
                }
            )

            HadithListContent(
                state = state,
                onRetry = {
                    if (state.isSearchMode) viewModel.searchHadith()
                    else state.selectedGroup?.let(viewModel::openRangeGroup)
                },
                onOpenDetail = { id -> navController.navigate(Screen.HadithDetail.createRoute(id)) },
                onOpenGroup = viewModel::openRangeGroup,
                onOpenTopic = viewModel::openTopic,
                onBackToGroups = viewModel::backToGroups,
                onPage = viewModel::loadPage
            )
        }
    }
}

@Composable
fun HadithDetailScreen(
    id: Int,
    navController: NavController,
    viewModel: HadithViewModel = koinViewModel()
) {
    val state by viewModel.detailState.collectAsState()

    LaunchedEffect(id) {
        viewModel.loadDetail(id)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppGradientBackground(isDark = isAppInDarkTheme())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            HadithDetailHeader(
                title = state.hadith?.id?.let { "HADIS #$it" } ?: "DETAIL HADIS",
                onBack = { navController.popBackStack() }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    state.isLoading && state.hadith == null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    state.errorMessage != null && state.hadith == null -> {
                        HadithErrorCard(message = state.errorMessage ?: "Hadis tidak ditemukan") {
                            viewModel.loadDetail(id)
                        }
                    }
                    state.hadith != null -> {
                        HadithDetailContent(
                            hadith = state.hadith!!,
                            isLoading = state.isLoading,
                            isOfflineData = state.isOfflineData,
                            cacheNotice = state.cacheNotice,
                            errorMessage = state.errorMessage,
                            onPrevious = viewModel::loadPrevious,
                            onNext = viewModel::loadNext
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HadithBackground() {
    val isDark = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isDark) {
                        listOf(DarkInk, Color(0xFF101C20), MaterialTheme.colorScheme.background)
                    } else {
                        listOf(WarmIvory, WarmParchment, MaterialTheme.colorScheme.background)
                    }
                )
            )
    ) {
        val spacing = 92.dp.toPx()
        val starRadius = 14.dp.toPx()
        val cols = (size.width / spacing).toInt() + 2
        val rows = (size.height / spacing).toInt() + 2
        val color = primary.copy(alpha = if (isDark) 0.024f else 0.018f)

        for (col in -1..cols) {
            for (row in -1..rows) {
                val stagger = if (col % 2 == 0) spacing / 2f else 0f
                val center = Offset(col * spacing, row * spacing + stagger)
                val path = Path()
                val inner = starRadius * 0.55f
                for (i in 0 until 16) {
                    val radius = if (i % 2 == 0) starRadius else inner
                    val angle = (i * PI / 8 - PI / 2).toFloat()
                    val x = center.x + radius * cos(angle.toDouble()).toFloat()
                    val y = center.y + radius * sin(angle.toDouble()).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color)
            }
        }
    }
}

@Composable
private fun HadithHeader(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val titleColor = if (isDark) GoldLight else GoldDeep
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.72f) else WarmInk.copy(alpha = 0.64f)
    val buttonBg = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.10f else 0.62f)
    val headerBrush = if (isDark) {
        Brush.verticalGradient(0.0f to DarkInk, 0.62f to Color(0xFF101D21), 1.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
    } else {
        Brush.verticalGradient(0.0f to WarmIvory, 0.62f to WarmParchment, 1.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        AppPageHeaderBackground(isDark = isDark, modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                CircleIconButton(
                    modifier = Modifier.align(Alignment.CenterStart),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    background = buttonBg,
                    contentDescription = "Kembali",
                    onClick = onBack
                )

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ENSIKLOPEDIA HADIS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.0.sp,
                        color = titleColor
                    )
                    Text(
                        text = "Cari, baca, dan telusuri hadis",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtitleColor
                    )
                }

                CircleIconButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    icon = Icons.Default.Refresh,
                    background = buttonBg,
                    contentDescription = "Muat ulang",
                    onClick = onRefresh
                )
            }

            HadithHeroCard(isDark = isDark)

            Spacer(modifier = Modifier.height(10.dp))

            HadithSearchBar(
                query = searchQuery,
                onQueryChange = onQueryChange,
                isDark = isDark,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun HadithDetailHeader(
    title: String,
    onBack: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val buttonBg = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.10f else 0.62f)
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        AppPageHeaderBackground(isDark = isDark, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            CircleIconButton(
                modifier = Modifier.align(Alignment.CenterStart),
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                background = buttonBg,
                contentDescription = "Kembali",
                onClick = onBack
            )
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.0.sp,
                color = if (isDark) GoldLight else GoldDeep
            )
        }
    }
}

@Composable
private fun CircleIconButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f), CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun HadithHeroCard(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "hadithHero")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -600f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing)),
        label = "hadithHeroShimmer"
    )
    val bgBrush = if (isDark) Brush.verticalGradient(listOf(Color(0xFF111E23), Color(0xFF0D171B)))
    else Brush.verticalGradient(listOf(Color(0xFF151B18), Color(0xFF20251E)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgBrush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(GoldPrimary.copy(alpha = 0.55f), GoldLight.copy(alpha = 0.18f), GoldPrimary.copy(alpha = 0.55f))),
                shape = RoundedCornerShape(8.dp)
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(listOf(GoldPrimary.copy(alpha = 0.14f), Color.Transparent)),
                    radius = size.width * 0.55f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.05f), Color.Transparent),
                        start = Offset(shimmerX, 0f),
                        end = Offset(shimmerX + 200f, size.height)
                    )
                )
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = "الْحَدِيثُ النَّبَوِيُّ",
                fontFamily = AmiriFontFamily,
                fontSize = 27.sp,
                lineHeight = 33.sp,
                color = GoldLight,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroInfoPill("TERJEMAH")
                HeroInfoPill("TAKHRIJ")
                HeroInfoPill("HIKMAH")
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
            text = label,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp,
            color = GoldLight.copy(alpha = 0.78f)
        )
    }
}

@Composable
private fun HadithSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val fieldBg = if (isDark) Color(0xFF101D21).copy(alpha = 0.58f) else Color.White.copy(alpha = 0.66f)
    val borderColor = if (isDark) GoldPrimary.copy(alpha = 0.35f) else GoldPrimary.copy(alpha = 0.24f)
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Cari hadis, minimal 4 karakter...",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                fontSize = 13.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isDark) GoldLight.copy(alpha = 0.78f) else GoldDeep.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = if (query.isNotEmpty()) ({
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hapus pencarian",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }) else null,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = fieldBg,
            unfocusedContainerColor = fieldBg,
            focusedBorderColor = GoldPrimary.copy(alpha = 0.65f),
            unfocusedBorderColor = borderColor,
            cursorColor = GoldPrimary
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
    )
}

@Composable
private fun HadithListContent(
    state: HadithListUiState,
    onRetry: () -> Unit,
    onOpenDetail: (Int) -> Unit,
    onOpenGroup: (HadithRangeGroup) -> Unit,
    onOpenTopic: (HadithTopicShortcut) -> Unit,
    onBackToGroups: () -> Unit,
    onPage: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!state.isLoading && (state.cacheNotice != null || state.isOfflineData)) {
            item {
                StatusNotice(state.cacheNotice ?: "Data tersimpan ditampilkan sambil menunggu pembaruan.")
            }
        }

        if (!state.hasSelectedGroup && !state.isSearchMode) {
            item {
                HadithLibraryIntro()
            }
            item {
                TopicShortcutRow(
                    topics = state.topicShortcuts,
                    onOpenTopic = onOpenTopic
                )
            }
            items(state.rangeGroups, key = { it.startId }) { group ->
                HadithRangeGroupCard(group = group, onClick = { onOpenGroup(group) })
            }
        } else if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (state.errorMessage != null) {
            item {
                HadithErrorCard(message = state.errorMessage, onRetry = onRetry)
            }
        } else if (state.isSearchMode) {
            item {
                HadithListToolbar(
                    title = "Hasil pencarian: ${state.searchQuery}",
                    subtitle = "${state.paging.totalData} hadis ditemukan",
                    onBackToGroups = onBackToGroups
                )
            }
            if (state.searchItems.isEmpty()) {
                item { EmptyHadithCard("Tidak ada hasil untuk kata kunci tersebut.") }
            } else {
                items(state.searchItems, key = { it.id }) { item ->
                    HadithSearchResultCard(item = item, onClick = { onOpenDetail(item.id) })
                }
            }
        } else {
            state.selectedGroup?.let { group ->
                item {
                    HadithListToolbar(
                        title = group.title,
                        subtitle = "Menampilkan halaman ${state.paging.current} dari Ensiklopedia Hadis",
                        onBackToGroups = onBackToGroups
                    )
                }
            }
            items(state.items, key = { it.id }) { item ->
                HadithCard(item = item, onClick = { onOpenDetail(item.id) })
            }
        }

        if (!state.isLoading && state.errorMessage == null && state.paging.totalPages > 1) {
            item {
                HadithPagination(
                    current = state.paging.current,
                    total = state.paging.totalPages,
                    hasPrev = state.paging.hasPrev,
                    hasNext = state.paging.hasNext,
                    onPage = onPage
                )
            }
        }
    }
}

@Composable
private fun HadithLibraryIntro() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.ViewModule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Rak Hadis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Pilih kelompok nomor atau topik terlebih dahulu. Data disusun dari Ensiklopedia Hadis API Muslim dan tetap mendukung cache offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopicShortcutRow(
    topics: List<HadithTopicShortcut>,
    onOpenTopic: (HadithTopicShortcut) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Topik cepat", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(topics, key = { it.keyword }) { topic ->
                Surface(
                    modifier = Modifier.clickable { onOpenTopic(topic) },
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
                ) {
                    Text(
                        text = topic.title,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HadithRangeGroupCard(
    group: HadithRangeGroup,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(group.startId.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(group.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(group.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun HadithListToolbar(
    title: String,
    subtitle: String,
    onBackToGroups: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = onBackToGroups) {
                Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Rak")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HadithCard(
    item: HadithItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HadithCardHeader(id = item.id, grade = item.grade)
            item.text?.arabic?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = AmiriFontFamily,
                    fontSize = 24.sp,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = item.text?.indonesia.orEmpty().ifBlank { "Terjemah tidak tersedia." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            item.takhrij?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HadithSearchResultCard(
    item: HadithSearchItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            HadithCardHeader(id = item.id, grade = null)
            Text(
                text = item.text.orEmpty().ifBlank { item.focus.joinToString(" ") }.ifBlank { "Cuplikan tidak tersedia." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HadithCardHeader(id: Int, grade: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.FormatQuote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = "Hadis #$id",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        grade?.takeIf { it.isNotBlank() }?.let {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HadithPagination(
    current: Int,
    total: Int,
    hasPrev: Boolean,
    hasNext: Boolean,
    onPage: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(enabled = hasPrev, onClick = { onPage((current - 1).coerceAtLeast(1)) }) {
                Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Sebelum")
            }
            Text(
                text = "$current / $total",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(enabled = hasNext, onClick = { onPage(current + 1) }) {
                Text("Lanjut")
                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun HadithDetailContent(
    hadith: HadithItem,
    isLoading: Boolean,
    isOfflineData: Boolean,
    cacheNotice: String?,
    errorMessage: String?,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (cacheNotice != null || isOfflineData) {
            StatusNotice(cacheNotice ?: "Data tersimpan ditampilkan sambil menunggu pembaruan.")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HadithCardHeader(id = hadith.id, grade = hadith.grade)
                hadith.text?.arabic?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = AmiriFontFamily,
                        fontSize = 27.sp,
                        lineHeight = 42.sp,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = hadith.text?.indonesia.orEmpty().ifBlank { "Terjemah tidak tersedia." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 26.sp
                )
                DetailSection("Takhrij", hadith.takhrij)
                DetailSection("Hikmah", hadith.hikmah)
            }
        }

        errorMessage?.let {
            StatusNotice(text = it)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = !isLoading && hadith.prev != null,
                onClick = onPrevious
            ) {
                Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sebelum")
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = !isLoading && hadith.next != null,
                onClick = onNext
            ) {
                Text("Berikutnya")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, value: String?) {
    value?.takeIf { it.isNotBlank() }?.let {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HadithErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            Button(onClick = onRetry) {
                Text("Coba Lagi")
            }
        }
    }
}

@Composable
private fun EmptyHadithCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusNotice(text: String) {
    val color = Color(0xFF8A5A00)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}
