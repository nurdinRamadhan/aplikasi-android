package com.alhasanah.alhasanahmedia.ui.ibadah

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.ibadah.IbadahChapter
import com.alhasanah.alhasanahmedia.data.model.ibadah.IbadahGuide
import com.alhasanah.alhasanahmedia.data.model.ibadah.IbadahPrayer
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
private val GoldGradient = Brush.linearGradient(listOf(GoldDeep, GoldPrimary, GoldLight, GoldPrimary, GoldDeep))

@Composable
fun IbadahGuideScreen(
    navController: NavController,
    viewModel: IbadahGuideViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val selectedGuide = state.selectedGuide

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppGradientBackground(isDark = isAppInDarkTheme())
        LazyColumn(
            contentPadding = PaddingValues(bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                IbadahHeader(
                    title = if (selectedGuide == null) "TUNTUNAN IBADAH" else selectedGuide.title.uppercase(),
                    subtitle = if (selectedGuide == null) "Shalat, haji, umrah, qurban, dan bersuci" else selectedGuide.category,
                    query = state.query,
                    showSearch = selectedGuide == null,
                    onQueryChange = viewModel::updateQuery,
                    onBack = {
                        if (selectedGuide != null) viewModel.clearSelectedGuide() else navController.popBackStack()
                    }
                )
            }

            if (selectedGuide == null) {
                item {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.categories) { category ->
                            CategoryChip(
                                label = category,
                                selected = category == state.selectedCategory,
                                onClick = { viewModel.selectCategory(category) }
                            )
                        }
                    }
                }

                when {
                    state.isLoading -> item { LoadingCard() }
                    state.error != null -> item { EmptyCard(state.error.orEmpty()) }
                    state.visibleGuides.isEmpty() -> item { EmptyCard("Tuntunan tidak ditemukan.") }
                    else -> {
                        items(state.visibleGuides, key = { it.id }) { guide ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                GuideCard(guide = guide, onClick = { viewModel.selectGuide(guide) })
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        GuideSummaryCard(guide = selectedGuide)
                    }
                }
                items(selectedGuide.chapters, key = { it.id }) { chapter ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ChapterCard(chapter)
                    }
                }
            }
        }
    }
}

@Composable
private fun IbadahBackground() {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()
    val infiniteTransition = rememberInfiniteTransition(label = "ibadahBg")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(170_000, easing = LinearEasing)),
        label = "ibadahBgRot"
    )
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isDark) listOf(Color(0xFF0B1519), Color(0xFF101C20), MaterialTheme.colorScheme.background)
                    else listOf(WarmIvory, WarmParchment, MaterialTheme.colorScheme.background)
                )
            )
    ) {
        val spacing = 92.dp.toPx()
        val starR = 14.dp.toPx()
        val color = primary.copy(alpha = if (isDark) 0.024f else 0.018f)
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
private fun IbadahHeader(
    title: String,
    subtitle: String,
    query: String,
    showSearch: Boolean,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
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
                HeaderBackButton(onBack = onBack, isDark = isDark)
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(title, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.2.sp, color = if (isDark) GoldLight else GoldDeep)
                    Text(subtitle, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color.White.copy(alpha = 0.72f) else WarmInk.copy(alpha = 0.64f))
                }
            }
            HeroCard()
            AnimatedVisibility(visible = showSearch) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    SearchBar(query = query, onQueryChange = onQueryChange, isDark = isDark, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun HeaderBackButton(onBack: () -> Unit, isDark: Boolean) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.10f else 0.62f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f), CircleShape)
            .clickable { onBack() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Kembali",
            tint = if (isDark) Color.White.copy(alpha = 0.86f) else WarmInk,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun HeroCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "ibadahHero")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -600f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "ibadahHeroShimmer"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF151B18), Color(0xFF20251E))))
            .border(1.dp, Brush.linearGradient(listOf(GoldPrimary.copy(alpha = 0.55f), GoldLight.copy(alpha = 0.18f), GoldPrimary.copy(alpha = 0.55f))), RoundedCornerShape(18.dp))
            .drawBehind {
                drawLine(GoldGradient, Offset(36f, 0f), Offset(size.width - 36f, 0f), 1.dp.toPx())
                drawLine(GoldGradient, Offset(48f, size.height), Offset(size.width - 48f, size.height), 1.dp.toPx())
                drawCircle(
                    Brush.radialGradient(listOf(GoldPrimary.copy(alpha = 0.14f), Color.Transparent), Offset(size.width / 2f, size.height / 2f), size.width * 0.55f),
                    size.width * 0.55f,
                    Offset(size.width / 2f, size.height / 2f)
                )
                drawRect(
                    Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.05f), Color.Transparent), Offset(shimmerX, 0f), Offset(shimmerX + 200f, size.height))
                )
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("فِقْهُ الْعِبَادَاتِ", fontFamily = AmiriFontFamily, fontSize = 28.sp, color = GoldLight, textAlign = TextAlign.Center, lineHeight = 34.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DividerGlow(reverse = false)
                Text("  ✦  ", color = GoldPrimary.copy(alpha = 0.65f), fontSize = 9.sp)
                DividerGlow(reverse = true)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroPill("SHALAT")
                HeroPill("HAJI")
                HeroPill("QURBAN")
            }
        }
    }
}

@Composable
private fun DividerGlow(reverse: Boolean) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(0.5.dp)
            .background(
                Brush.horizontalGradient(
                    if (reverse) listOf(GoldPrimary.copy(alpha = 0.55f), Color.Transparent)
                    else listOf(Color.Transparent, GoldPrimary.copy(alpha = 0.55f))
                )
            )
    )
}

@Composable
private fun HeroPill(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .border(0.5.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
            .background(GoldPrimary.copy(alpha = 0.09f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = GoldLight.copy(alpha = 0.78f))
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, isDark: Boolean, modifier: Modifier = Modifier) {
    val fieldBg = if (isDark) Color(0xFF101D21).copy(alpha = 0.58f) else Color.White.copy(alpha = 0.66f)
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Cari shalat, haji, umrah, qurban...", fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = if (isDark) GoldLight else GoldDeep, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = fieldBg,
            unfocusedContainerColor = fieldBg,
            focusedBorderColor = GoldPrimary.copy(alpha = 0.65f),
            unfocusedBorderColor = GoldPrimary.copy(alpha = 0.24f),
            cursorColor = GoldPrimary
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
    )
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(50.dp),
        color = if (selected) GoldPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = if (selected) 0.48f else 0.18f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) GoldDeep else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GuideCard(guide: IbadahGuide, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(0.8.dp, Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.22f), GoldPrimary.copy(alpha = 0.07f), GoldPrimary.copy(alpha = 0.22f))), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            GuideIcon(guide.icon)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(guide.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(guide.summary, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${guide.chapters.size} bab · ${guide.category}", style = MaterialTheme.typography.labelSmall, color = GoldDeep, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GuideIcon(icon: String) {
    val imageVector: ImageVector = when (icon) {
        "water" -> Icons.Default.LocalDrink
        "gift" -> Icons.Default.VolunteerActivism
        "kaaba", "route" -> Icons.Default.Explore
        "moon" -> Icons.Default.AutoAwesome
        else -> Icons.Default.Mosque
    }
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(GoldPrimary.copy(alpha = 0.12f))
            .border(1.dp, GoldPrimary.copy(alpha = 0.32f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector, contentDescription = null, tint = GoldDeep)
    }
}

@Composable
private fun GuideSummaryCard(guide: IbadahGuide) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), shadowElevation = 2.dp) {
        Column(
            modifier = Modifier
                .border(0.8.dp, Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.28f), GoldPrimary.copy(alpha = 0.08f), GoldPrimary.copy(alpha = 0.28f))), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(guide.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = GoldDeep, lineHeight = 27.sp)
            Text(guide.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                InfoPill("${guide.chapters.size} bab")
                InfoPill("${guide.chapters.sumOf { it.prayers.size }} bacaan")
                InfoPill("offline")
            }
        }
    }
}

@Composable
private fun ChapterCard(chapter: IbadahChapter) {
    val contentCount = chapter.steps.size + chapter.prayers.size + chapter.notes.size
    var expanded by remember(chapter.id) {
        mutableStateOf(contentCount <= 8)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .border(0.8.dp, Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.22f), GoldPrimary.copy(alpha = 0.06f), GoldPrimary.copy(alpha = 0.22f))), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = GoldPrimary.copy(alpha = 0.13f)) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = GoldDeep,
                        modifier = Modifier.padding(7.dp).size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(chapter.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    ChapterMeta(chapter)
                }
            }

            if (!expanded && chapter.description.isNotBlank()) {
                Text(
                    chapter.description,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (chapter.description.isNotBlank()) {
                        Text(chapter.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    chapter.steps.forEachIndexed { index, step ->
                        StepRow(number = index + 1, text = step)
                    }
                    chapter.prayers.forEachIndexed { index, prayer ->
                        PrayerBlock(number = index + 1, prayer = prayer)
                    }
                    chapter.notes.forEach { note ->
                        Surface(shape = RoundedCornerShape(10.dp), color = GoldPrimary.copy(alpha = 0.09f)) {
                            Text(note, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterMeta(chapter: IbadahChapter) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (chapter.steps.isNotEmpty()) InfoPill("${chapter.steps.size} langkah")
        if (chapter.prayers.isNotEmpty()) InfoPill("${chapter.prayers.size} bacaan")
        if (chapter.notes.isNotEmpty()) InfoPill("${chapter.notes.size} catatan")
    }
}

@Composable
private fun InfoPill(label: String) {
    Surface(shape = RoundedCornerShape(50.dp), color = GoldPrimary.copy(alpha = 0.10f)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = GoldDeep,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GoldSeparator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.6.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, GoldPrimary.copy(alpha = 0.42f), Color.Transparent)))
    )
}

@Composable
private fun PrayerBlock(number: Int, prayer: IbadahPrayer) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isAppInDarkTheme()) 0.26f else 0.42f),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = GoldPrimary.copy(alpha = 0.14f)) {
                    Text(number.toString(), modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = GoldDeep, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                Text(prayer.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GoldDeep)
            }
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (prayer.arabic.isNotBlank()) {
                        Text(
                            prayer.arabic,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = AmiriFontFamily,
                            fontSize = 25.sp,
                            lineHeight = 40.sp,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (prayer.latin.isNotBlank()) {
                        GoldSeparator()
                        Text(prayer.latin, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
                    }
                    if (prayer.translation.isNotBlank()) {
                        GoldSeparator()
                        Text(prayer.translation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = GoldPrimary.copy(alpha = 0.14f)) {
            Text(number.toString(), modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = GoldDeep, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LoadingCard() {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldDeep)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
