package com.alhasanah.alhasanahmedia.ui.prayer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerLocation
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerScheduleEntry
import com.alhasanah.alhasanahmedia.ui.components.AppGradientBackground
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.util.PrayerReminderMode
import com.alhasanah.alhasanahmedia.util.PrayerReminderSettings
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val GoldLight = Color(0xFFE8C55A)
private val GoldDeep = Color(0xFFAA7C1F)
private val WarmIvory = Color(0xFFFFFCF7)
private val WarmParchment = Color(0xFFFBF3E6)
private val WarmInk = Color(0xFF2A2318)
private val DarkInk = Color(0xFF0B1519)

@Composable
fun PrayerScheduleScreen(
    navController: NavController,
    viewModel: PrayerScheduleViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val reminderSettings by viewModel.reminderSettings.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.detectLocationAndLoad()
    }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppGradientBackground(isDark = isAppInDarkTheme())

        Column(modifier = Modifier.fillMaxSize()) {
            PrayerHeader(
                searchQuery = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onBack = { navController.popBackStack() },
                onRefresh = viewModel::detectLocationAndLoad
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.detectedLocationLabel?.let {
                    item {
                        LocationHintCard(
                            label = it,
                            selected = state.selectedLocation?.lokasi,
                            onRefreshLocation = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                        )
                    }
                }

                if (state.cacheNotice != null || state.isOfflineData) {
                    item {
                        PrayerNoticeCard(
                            message = state.cacheNotice ?: "Data tersimpan ditampilkan sambil menunggu pembaruan."
                        )
                    }
                }

                state.todaySchedule?.let { schedule ->
                    item {
                        PrayerTodayCard(
                            location = state.scheduleData?.kabko.orEmpty(),
                            province = state.scheduleData?.prov.orEmpty(),
                            schedule = schedule
                        )
                    }
                    item {
                        PrayerReminderSettingsCard(
                            settings = reminderSettings,
                            onEnabledChange = viewModel::setReminderEnabled,
                            onModeChange = viewModel::setReminderMode,
                            onOffsetChange = viewModel::setReminderOffset,
                            onTogglePrayer = viewModel::toggleReminderPrayer
                        )
                    }
                }

                if (state.locations.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pilih Kab/Kota",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(state.locations, key = { it.id }) { location ->
                        LocationResultCard(
                            location = location,
                            selected = location.id == state.selectedLocation?.id,
                            onClick = { viewModel.selectLocation(location) }
                        )
                    }
                }

                if (state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                state.errorMessage?.let {
                    item {
                        PrayerNoticeCard(message = it)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerBackground() {
    val isDark = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isDark) listOf(DarkInk, Color(0xFF101C20), MaterialTheme.colorScheme.background)
                    else listOf(WarmIvory, WarmParchment, MaterialTheme.colorScheme.background)
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
private fun PrayerHeader(
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
                HeaderIconButton(Modifier.align(Alignment.CenterStart), Icons.AutoMirrored.Filled.ArrowBack, buttonBg, "Kembali", onBack)
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("KALENDER & JADWAL SHOLAT", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp, color = titleColor)
                    Text("Kemenag/MyQuran, lokasi, dan pengingat", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = subtitleColor)
                }
                HeaderIconButton(Modifier.align(Alignment.CenterEnd), Icons.Default.Refresh, buttonBg, "Muat ulang", onRefresh)
            }

            PrayerHeroCard()
            Spacer(Modifier.height(10.dp))
            PrayerSearchBar(searchQuery, onQueryChange, isDark, Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun HeaderIconButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "prayerHeaderIconScale"
    )
    Box(
        modifier = modifier
            .scale(pressScale)
            .size(42.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f), CircleShape)
            .clickable(indication = null, interactionSource = interactionSource) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PrayerHeroCard() {
    val primary = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "prayerHeroMotion")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "prayerHeroShimmerX"
    )
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "prayerHeroIconPulse"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF151B18), Color(0xFF20251E))))
            .border(1.dp, primary.copy(alpha = 0.42f), RoundedCornerShape(8.dp))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.13f), Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 0.50f),
                        radius = size.width * 0.42f
                    ),
                    radius = size.width * 0.42f,
                    center = Offset(size.width * 0.18f, size.height * 0.50f)
                )
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.055f), Color.Transparent),
                        start = Offset(shimmerX, 0f),
                        end = Offset(shimmerX + 190f, size.height)
                    )
                )
            }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .scale(iconScale),
                shape = CircleShape,
                color = primary.copy(alpha = 0.16f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldLight, modifier = Modifier.size(27.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Hari Ini", style = MaterialTheme.typography.titleMedium, color = GoldLight, fontWeight = FontWeight.Bold)
                Text("Timeline sholat, terbit, dhuha, dan pengingat", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.68f))
            }
        }
    }
}

@Composable
private fun PrayerSearchBar(query: String, onQueryChange: (String) -> Unit, isDark: Boolean, modifier: Modifier = Modifier) {
    val fieldBg = if (isDark) Color(0xFF101D21).copy(alpha = 0.58f) else Color.White.copy(alpha = 0.66f)
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Cari kota/kabupaten...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f), fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
        trailingIcon = if (query.isNotEmpty()) ({
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
            }
        }) else null,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = fieldBg,
            unfocusedContainerColor = fieldBg,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
    )
}

@Composable
private fun LocationHintCard(label: String, selected: String?, onRefreshLocation: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "locationHintGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.07f,
        targetValue = 0.13f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "locationHintGlowAlpha"
    )
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Lokasi terdeteksi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(selected ?: label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "Perbarui",
                modifier = Modifier.clickable { onRefreshLocation() },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PrayerTodayCard(location: String, province: String, schedule: PrayerScheduleEntry) {
    val primary = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "prayerTodayAccent")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -260f,
        targetValue = 760f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "prayerTodayAccentX"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, primary.copy(alpha = 0.28f), Color.Transparent),
                        start = Offset(shimmerX, 0f),
                        end = Offset(shimmerX + 170f, 0f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(location.ifBlank { "Lokasi terpilih" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${province.ifBlank { "Indonesia" }} • ${schedule.tanggal}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PrayerTimeTimeline(schedule)
        }
    }
}

@Composable
private fun PrayerTimeTimeline(schedule: PrayerScheduleEntry) {
    val times = listOf(
        "Imsak" to schedule.imsak,
        "Fajr / Subuh" to schedule.subuh,
        "Matahari Terbit" to schedule.terbit,
        "Dhuha" to schedule.dhuha,
        "Dzuhur" to schedule.dzuhur,
        "Ashar" to schedule.ashar,
        "Maghrib" to schedule.maghrib,
        "Isya" to schedule.isya
    )
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        times.forEachIndexed { index, item ->
            PrayerTimelineRow(
                name = item.first,
                time = item.second,
                isFirst = index == 0,
                isLast = index == times.lastIndex
            )
        }
    }
}

@Composable
private fun PrayerTimelineRow(name: String, time: String, isFirst: Boolean, isLast: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val isPrimaryPrayer = name in setOf("Fajr / Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(54.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                if (!isFirst) {
                    drawLine(
                        color = primary.copy(alpha = 0.24f),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, size.height / 2f - 6.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                if (!isLast) {
                    drawLine(
                        color = primary.copy(alpha = 0.24f),
                        start = Offset(centerX, size.height / 2f + 6.dp.toPx()),
                        end = Offset(centerX, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                drawCircle(
                    color = if (isPrimaryPrayer) primary else outline,
                    radius = if (isPrimaryPrayer) 5.dp.toPx() else 3.5.dp.toPx(),
                    center = Offset(centerX, size.height / 2f)
                )
            }
        }
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = if (isPrimaryPrayer) primary.copy(alpha = 0.075f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = if (isPrimaryPrayer) FontWeight.Bold else FontWeight.Medium)
                Text(time.ifBlank { "--:--" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PrayerReminderSettingsCard(
    settings: PrayerReminderSettings,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (PrayerReminderMode) -> Unit,
    onOffsetChange: (Int) -> Unit,
    onTogglePrayer: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pengingat Sholat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Pilih notifikasi, getar, dering, atau adzan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = settings.enabled, onCheckedChange = onEnabledChange)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PrayerReminderMode.entries.forEach { mode ->
                    ReminderOptionChip(
                        label = mode.label,
                        selected = settings.mode == mode,
                        onClick = { onModeChange(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ingatkan sebelum waktu", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(0, 5, 10, 15).forEach { minute ->
                        ReminderOptionChip(
                            label = if (minute == 0) "Tepat" else "$minute mnt",
                            selected = settings.minutesBefore == minute,
                            onClick = { onOffsetChange(minute) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Aktif untuk", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya").chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { name ->
                            ReminderOptionChip(
                                label = name,
                                selected = name in settings.enabledPrayers,
                                onClick = { onTogglePrayer(name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Text(
                text = "Mode adzan memakai suara alarm perangkat sampai audio adzan lokal ditambahkan. Pengingat dijadwalkan dari data Kemenag/MyQuran yang tersimpan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReminderOptionChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.42f else 0.10f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun LocationResultCard(location: PrayerLocation, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "locationResultScale"
    )
    val bg by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        label = "locationResultBg"
    )
    val elevation by animateFloatAsState(
        targetValue = if (selected) 4f else 1f,
        animationSpec = tween(220),
        label = "locationResultElevation"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(location.lokasi, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PrayerNoticeCard(message: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color(0xFF8A5A00).copy(alpha = 0.10f)) {
        Text(message, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8A5A00))
    }
}
