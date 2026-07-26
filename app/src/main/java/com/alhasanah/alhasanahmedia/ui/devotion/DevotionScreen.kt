package com.alhasanah.alhasanahmedia.ui.devotion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.devotion.DevotionItem
import com.alhasanah.alhasanahmedia.data.model.devotion.KitabBook
import com.alhasanah.alhasanahmedia.data.model.devotion.KitabChapter
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
private val GoldShimmer = Color(0xFFFAF0C0)
private val WarmIvory = Color(0xFFFFFCF7)
private val WarmParchment = Color(0xFFFBF3E6)
private val WarmInk = Color(0xFF2A2318)
private val DarkInk = Color(0xFF0B1519)

private val GoldGradient = Brush.linearGradient(
    colors = listOf(GoldDeep, GoldPrimary, GoldLight, GoldPrimary, GoldDeep)
)

@Composable
fun DevotionScreen(
    navController: NavController,
    initialTab: DevotionTab = DevotionTab.DOA,
    tabs: List<DevotionTab> = listOf(DevotionTab.DOA, DevotionTab.DZIKIR),
    title: String = "DOA & DZIKIR",
    subtitle: String = "Bacaan harian dan dzikir pilihan",
    viewModel: DevotionViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(initialTab) {
        viewModel.selectTab(initialTab)
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppGradientBackground(isDark = isAppInDarkTheme())
        LazyColumn(
            contentPadding = PaddingValues(bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                DevotionGradientHeader(
                    title = title,
                    subtitle = subtitle,
                    selectedTab = state.selectedTab,
                    tabs = tabs,
                    searchQuery = state.query,
                    searchPlaceholder = if (state.selectedTab == DevotionTab.KITAB) "Cari kitab, pengarang, atau bab" else "Cari judul atau terjemah",
                    onQueryChange = viewModel::updateQuery,
                    onTabSelected = viewModel::selectTab,
                    onBack = {
                        if (state.selectedBook != null) viewModel.clearSelectedBook() else navController.popBackStack()
                    }
                )
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    StatusStrip(
                        isLoading = state.isLoading || state.isChapterLoading,
                        isFromCache = state.isFromCache,
                        notice = state.notice,
                        error = state.error
                    )
                }
            }
            when (state.selectedTab) {
                DevotionTab.DOA, DevotionTab.DZIKIR -> {
                    item {
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.devotionCategories) { category ->
                                CategoryChip(
                                    label = category,
                                    selected = category == state.selectedCategory,
                                    onClick = { viewModel.selectCategory(category) }
                                )
                            }
                        }
                    }
                    if (state.isLoading && state.library.devotions.isEmpty()) {
                        item { LoadingCard() }
                    } else if (state.visibleDevotions.isEmpty()) {
                        item { EmptyCard("Belum ada bacaan untuk filter ini.") }
                    } else {
                        items(state.visibleDevotions, key = { it.id }) { item ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                DevotionCard(item)
                            }
                        }
                    }
                }
                DevotionTab.KITAB -> {
                    val selectedBook = state.selectedBook
                    if (selectedBook == null) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                KitabIntro(
                                    count = state.library.kitabBooks.size,
                                    categories = state.library.kitabCategories
                                )
                            }
                        }
                        if (state.isLoading && state.library.kitabBooks.isEmpty()) {
                            item { LoadingCard() }
                        } else if (state.visibleBooks.isEmpty()) {
                            item { EmptyCard("Daftar kitab belum tersedia. Pastikan API key sudah diatur dan buka saat online sekali.") }
                        } else {
                            items(state.visibleBooks, key = { it.slug }) { book ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    KitabBookCard(book = book, onClick = { viewModel.selectBook(book) })
                                }
                            }
                        }
                    } else {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SelectedBookHeader(book = selectedBook, onBack = viewModel::clearSelectedBook)
                            }
                        }
                        if (state.isChapterLoading && state.chapters.isEmpty()) {
                            item { LoadingCard() }
                        } else if (state.visibleChapters.isEmpty()) {
                            item { EmptyCard("Bab kitab belum tersedia untuk mode offline.") }
                        } else {
                            items(state.visibleChapters, key = { it.id }) { chapter ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    KitabChapterCard(
                                        chapter = chapter,
                                        expanded = state.selectedChapter?.id == chapter.id,
                                        selectedChapter = state.selectedChapter,
                                        onClick = { viewModel.selectChapter(chapter) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KitabKuningScreen(
    navController: NavController,
    viewModel: DevotionViewModel = koinViewModel()
) {
    DevotionScreen(
        navController = navController,
        initialTab = DevotionTab.KITAB,
        tabs = listOf(DevotionTab.KITAB),
        title = "KITAB KUNING",
        subtitle = "Referensi belajar santri offline-first",
        viewModel = viewModel
    )
}

@Composable
private fun DevotionBackground() {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()
    val infiniteTransition = rememberInfiniteTransition(label = "devotionBg")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(160_000, easing = LinearEasing)),
        label = "devotionBgRot"
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
        val color = primary.copy(alpha = if (isDark) 0.024f else 0.018f)
        val spacing = 92.dp.toPx()
        val starR = 14.dp.toPx()
        repeat((size.width / spacing).toInt() + 2) { col ->
            repeat((size.height / spacing).toInt() + 2) { row ->
                val center = Offset(col * spacing, row * spacing + if (col % 2 == 0) spacing / 2 else 0f)
                rotate(degrees = if ((col + row) % 2 == 0) rotation else -rotation, pivot = center) {
                    val path = Path()
                    val inner = starR * 0.55f
                    repeat(16) { i ->
                        val r = if (i % 2 == 0) starR else inner
                        val angle = (i * PI / 8 - PI / 2).toFloat()
                        val x = center.x + r * cos(angle.toDouble()).toFloat()
                        val y = center.y + r * sin(angle.toDouble()).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    drawPath(path, color)
                }
            }
        }
    }
}

@Composable
private fun DevotionGradientHeader(
    title: String,
    subtitle: String,
    selectedTab: DevotionTab,
    tabs: List<DevotionTab>,
    searchQuery: String,
    searchPlaceholder: String,
    onQueryChange: (String) -> Unit,
    onTabSelected: (DevotionTab) -> Unit,
    onBack: () -> Unit
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
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .border(1.dp, primary.copy(alpha = 0.34f), CircleShape)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onBack() }
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = if (isDark) Color.White.copy(alpha = 0.86f) else WarmInk,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(title, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.2.sp, color = titleColor)
                    Text(subtitle, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = subtitleColor)
                }
            }

            DevotionHeroCard(selectedTab = selectedTab, isDark = isDark)

            Spacer(modifier = Modifier.height(10.dp))

            DevotionSearchBar(
                query = searchQuery,
                onQueryChange = onQueryChange,
                placeholder = searchPlaceholder,
                isDark = isDark,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (tabs.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                DevotionTabRow(
                    tabs = tabs,
                    selected = selectedTab,
                    onTabSelected = onTabSelected,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun DevotionHeroCard(selectedTab: DevotionTab, isDark: Boolean) {
    val bgBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF111E23), Color(0xFF0D171B)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF151B18), Color(0xFF20251E)))
    }
    val infiniteTransition = rememberInfiniteTransition(label = "devotionHero")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -600f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "devotionHeroShimmer"
    )
    val arabicTitle = when (selectedTab) {
        DevotionTab.KITAB -> "الْكُتُبُ التُّرَاثِيَّةُ"
        else -> "الدُّعَاءُ وَالذِّكْرُ"
    }
    val pills = when (selectedTab) {
        DevotionTab.KITAB -> listOf("KITAB KLASIK", "BAB", "OFFLINE")
        else -> listOf("DOA", "DZIKIR", "FAWAID")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgBrush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(GoldPrimary.copy(alpha = 0.55f), GoldLight.copy(alpha = 0.18f), GoldPrimary.copy(alpha = 0.55f))),
                shape = RoundedCornerShape(18.dp)
            )
            .drawBehind {
                drawLine(brush = GoldGradient, start = Offset(36f, 0f), end = Offset(size.width - 36f, 0f), strokeWidth = 1.dp.toPx())
                drawLine(brush = GoldGradient, start = Offset(48f, size.height), end = Offset(size.width - 48f, size.height), strokeWidth = 1.dp.toPx())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GoldPrimary.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.55f
                    ),
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = arabicTitle,
                fontFamily = AmiriFontFamily,
                fontSize = 28.sp,
                color = GoldLight,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(0.5.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, GoldPrimary.copy(alpha = 0.55f))))
                )
                Text(text = "  ✦  ", color = GoldPrimary.copy(alpha = 0.65f), fontSize = 9.sp)
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(0.5.dp)
                        .background(Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.55f), Color.Transparent)))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                pills.forEach { HeroInfoPill(it) }
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
        Text(text = label, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = GoldLight.copy(alpha = 0.78f))
    }
}

@Composable
private fun DevotionSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val fieldBg = if (isDark) Color(0xFF101D21).copy(alpha = 0.58f) else Color.White.copy(alpha = 0.66f)
    val borderColor = if (isDark) GoldPrimary.copy(alpha = 0.35f) else GoldPrimary.copy(alpha = 0.24f)
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(text = placeholder, color = hintColor, fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isDark) GoldLight.copy(alpha = 0.78f) else GoldDeep.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
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
private fun DevotionTabRow(
    tabs: List<DevotionTab>,
    selected: DevotionTab,
    onTabSelected: (DevotionTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val pillBg = if (isDark) Color(0xFF101D21).copy(alpha = 0.62f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
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
        tabs.forEach { tab ->
            val isSelected = tab == selected
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) GoldPrimary else Color.Transparent,
                animationSpec = spring(),
                label = "devotion_tab_bg_${tab.name}"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF12100A) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                label = "devotion_tab_text_${tab.name}"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50.dp))
                    .background(bgColor)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTabSelected(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                    letterSpacing = 1.8.sp,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun IconCircle(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(GoldPrimary.copy(alpha = 0.12f))
            .border(1.dp, GoldPrimary.copy(alpha = 0.28f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = GoldPrimary)
    }
}

@Composable
private fun StatusStrip(isLoading: Boolean, isFromCache: Boolean, notice: String?, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedVisibility(visible = isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        val message = error ?: notice ?: if (isFromCache) "Mode offline. Data dari cache lokal." else null
        AnimatedVisibility(visible = message != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (error != null) Icons.Default.CloudOff else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = message.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(50.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.48f else 0.18f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DevotionCard(item: DevotionItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(item.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Pill(item.category)
            }
            if (item.arabic.isNotBlank()) {
                Text(item.arabic, modifier = Modifier.fillMaxWidth(), fontFamily = AmiriFontFamily, fontSize = 25.sp, lineHeight = 38.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface)
            }
            if (item.latin.isNotBlank()) Text(item.latin, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            if (item.translation.isNotBlank()) Text(item.translation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.fawaid.isNotBlank()) Text(item.fawaid, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val footer = listOf(item.note, item.source).filter { it.isNotBlank() }.joinToString(" · ")
            if (footer.isNotBlank()) Text(footer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun KitabIntro(count: Int, categories: List<String>) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$count kitab kuning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                text = categories.take(6).joinToString(" · ").ifBlank { "Pilih kitab saat online sekali agar daftar bab tersimpan untuk offline." },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KitabBookCard(book: KitabBook, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircle(Icons.Default.Book, "Buka kitab", onClick)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (book.author.isNotBlank()) Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (book.category.isNotBlank()) Pill(book.category)
                    if (book.totalChapters > 0) Text("${book.totalChapters} bab", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (book.description.isNotBlank()) {
                    Text(book.description, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SelectedBookHeader(book: KitabBook, onBack: () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircle(Icons.AutoMirrored.Filled.ArrowBack, "Daftar kitab", onBack)
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(book.author.ifBlank { book.category }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun KitabChapterCard(
    chapter: KitabChapter,
    expanded: Boolean,
    selectedChapter: KitabChapter?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Text(chapter.number.toString(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text(chapter.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            AnimatedVisibility(visible = expanded) {
                val detail = selectedChapter ?: chapter
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (detail.arabic.isNotBlank()) Text(detail.arabic, modifier = Modifier.fillMaxWidth(), fontFamily = AmiriFontFamily, fontSize = 24.sp, lineHeight = 36.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface)
                    if (detail.content.isNotBlank()) Text(detail.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (detail.translation.isNotBlank()) Text(detail.translation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (detail.latin.isNotBlank()) Text(detail.latin, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String) {
    Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LoadingCard() {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Spa, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
