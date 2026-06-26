package com.alhasanah.alhasanahmedia.ui.weather

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.weather.BmkgRegion
import com.alhasanah.alhasanahmedia.data.model.weather.CurrentWeather
import com.alhasanah.alhasanahmedia.data.model.weather.DailyForecastItem
import com.alhasanah.alhasanahmedia.data.model.weather.HourlyForecastItem
import com.alhasanah.alhasanahmedia.data.model.weather.WeatherAlertItem
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val GoldLight = Color(0xFFE8C55A)
private val GoldDeep = Color(0xFFAA7C1F)
private val WarmIvory = Color(0xFFFFFCF7)
private val WarmParchment = Color(0xFFFBF3E6)
private val WarmInk = Color(0xFF2A2318)
private val DarkInk = Color(0xFF0B1519)

@Composable
fun WeatherScreen(
    navController: NavController,
    viewModel: WeatherViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refreshWeather()
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
        AppSolidBackground(isDark = isAppInDarkTheme())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WeatherHeader(
                    onBack = { navController.popBackStack() },
                    onRefresh = viewModel::refreshWeather
                )
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    WeatherHeroCard(
                        state = state,
                        onChangeRegion = viewModel::showRegionPicker
                    )
                }
            }

            if (state.isRegionPickerVisible) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        WeatherRegionPicker(
                            query = state.regionQuery,
                            results = state.regionResults,
                            onQueryChange = viewModel::onRegionQueryChange,
                            onSelectRegion = viewModel::selectRegion,
                            onDismiss = viewModel::hideRegionPicker
                        )
                    }
                }
            }

            if (state.current != null) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        WeatherMetricGrid(current = state.current!!)
                    }
                }
            }

            if (state.alerts.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle("Peringatan Dini")
                    }
                }
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        WeatherAlertSection(items = state.alerts)
                    }
                }
            }

            if (state.hourly.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle("Per 3 Jam")
                    }
                }
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        HourlyForecastRow(items = state.hourly)
                    }
                }
            }

            if (state.daily.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle("3 Hari")
                    }
                }
                items(state.daily.size) { index ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DailyForecastCard(item = state.daily[index])
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
            }

            state.errorMessage?.let {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        WeatherNoticeCard(message = it)
                    }
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    BmkgAttributionCard(adm4 = state.adm4)
                }
            }
        }
    }
}

@Composable
private fun WeatherBackground() {
    val isDark = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "weatherBackground")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "weatherBackgroundDrift"
    )
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

        val cloudAlpha = if (isDark) 0.07f else 0.11f
        val cloudTint = if (isDark) Color.White.copy(alpha = cloudAlpha) else primary.copy(alpha = cloudAlpha)
        val driftPx = size.width * drift
        repeat(3) { index ->
            val baseX = ((index * size.width * 0.42f) + driftPx) % (size.width + 220.dp.toPx()) - 120.dp.toPx()
            val y = 132.dp.toPx() + index * 96.dp.toPx()
            drawWeatherCloud(
                center = Offset(baseX, y),
                scale = 1.1f + index * 0.18f,
                color = cloudTint
            )
        }
    }
}

@Composable
private fun WeatherHeader(
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val titleColor = if (isDark) GoldLight else GoldDeep
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.72f) else WarmInk.copy(alpha = 0.64f)
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
            HeaderIconButton(Modifier.align(Alignment.CenterStart), Icons.AutoMirrored.Filled.ArrowBack, buttonBg, "Kembali", onBack)
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PRAKIRAAN CUACA", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.0.sp, color = titleColor)
                Text("Data resmi BMKG", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = subtitleColor)
            }
            HeaderIconButton(Modifier.align(Alignment.CenterEnd), Icons.Default.Refresh, buttonBg, "Muat ulang", onRefresh)
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
    Box(
        modifier = modifier
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
private fun WeatherHeroCard(
    state: WeatherUiState,
    onChangeRegion: () -> Unit
) {
    val current = state.current
    val primary = MaterialTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()
    val infiniteTransition = rememberInfiniteTransition(label = "weatherHero")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "weatherHeroShimmer"
    )
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "weatherHeroGlow"
    )
    val accent = weatherAccentColor(current?.weatherCode, isDark)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawCircle(
                    color = accent.copy(alpha = glow),
                    radius = size.width * 0.42f,
                    center = Offset(size.width * 0.88f, size.height * 0.08f)
                )
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(Color.Transparent, accent.copy(alpha = 0.44f), Color.Transparent),
                        start = Offset(shimmerX, 0f),
                        end = Offset(shimmerX + 180f, 0f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF111B1E).copy(alpha = 0.98f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = if (isDark) 0.11f else 0.08f),
                            Color.Transparent,
                            primary.copy(alpha = if (isDark) 0.10f else 0.06f)
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = if (isDark) 0.12f else 0.09f))
                        .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    WeatherConditionVisual(
                        weatherCode = current?.weatherCode,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = current?.temperature?.let { "${it.roundToInt()}°" } ?: "--°",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = current?.description ?: weatherLabel(current?.weatherCode),
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = state.locationLabel.ifBlank { "Menunggu lokasi" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (state.isSavedRegion) "Wilayah cuaca tersimpan" else "Wilayah otomatis dari GPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f)
                    )
                }
                Surface(
                    modifier = Modifier.clickable { onChangeRegion() },
                    shape = RoundedCornerShape(50.dp),
                    color = primary.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primary.copy(alpha = 0.28f))
                ) {
                    Text(
                        text = "Ubah",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.usingFallbackLocation) {
                WeatherNoticeCard("Lokasi user belum tersedia. Cuaca sementara memakai koordinat cadangan.")
            }
        }
    }
}

@Composable
private fun WeatherConditionVisual(
    weatherCode: Int?,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val accent = weatherAccentColor(weatherCode, isDark)
    val infiniteTransition = rememberInfiniteTransition(label = "weatherCondition")
    val cloudShift by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "weatherConditionCloud"
    )
    val sunPulse by infiniteTransition.animateFloat(
        initialValue = 0.84f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "weatherConditionSun"
    )
    val rainDrop by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "weatherConditionRain"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width * 0.52f, size.height * 0.44f)
        val sunColor = Color(0xFFFFC64D)
        val cloudColor = if (isDark) Color(0xFFEAF6FF).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.98f)
        val shadowColor = if (isDark) Color(0xFFB8CBD4).copy(alpha = 0.24f) else Color(0xFF9CB1BE).copy(alpha = 0.18f)

        drawCircle(
            color = accent.copy(alpha = if (isDark) 0.16f else 0.12f),
            radius = size.minDimension * 0.42f,
            center = center
        )

        if (!isRainy(weatherCode) || isStormy(weatherCode)) {
            drawSun(
                center = Offset(size.width * 0.36f, size.height * 0.34f),
                radius = size.minDimension * 0.13f * sunPulse,
                color = sunColor.copy(alpha = if (isDark) 0.86f else 0.94f)
            )
        }

        drawWeatherCloud(
            center = Offset(center.x + cloudShift.dp.toPx(), center.y + 6.dp.toPx()),
            scale = size.minDimension / 132.dp.toPx(),
            color = shadowColor
        )
        drawWeatherCloud(
            center = Offset(center.x + cloudShift.dp.toPx(), center.y),
            scale = size.minDimension / 132.dp.toPx(),
            color = cloudColor
        )

        if (isRainy(weatherCode)) {
            repeat(5) { index ->
                val startX = center.x - 32.dp.toPx() + index * 16.dp.toPx()
                val dropOffset = ((rainDrop + index * 0.18f) % 1f) * 26.dp.toPx()
                drawLine(
                    color = Color(0xFF57A7FF).copy(alpha = 0.80f),
                    start = Offset(startX, center.y + 34.dp.toPx() + dropOffset),
                    end = Offset(startX - 5.dp.toPx(), center.y + 48.dp.toPx() + dropOffset),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        if (isStormy(weatherCode)) {
            val bolt = Path().apply {
                moveTo(center.x + 8.dp.toPx(), center.y + 26.dp.toPx())
                lineTo(center.x - 3.dp.toPx(), center.y + 54.dp.toPx())
                lineTo(center.x + 10.dp.toPx(), center.y + 50.dp.toPx())
                lineTo(center.x - 5.dp.toPx(), center.y + 82.dp.toPx())
                lineTo(center.x + 24.dp.toPx(), center.y + 42.dp.toPx())
                lineTo(center.x + 10.dp.toPx(), center.y + 46.dp.toPx())
                close()
            }
            drawPath(bolt, Color(0xFFFFD95A))
        }
    }
}

@Composable
private fun WeatherRegionPicker(
    query: String,
    results: List<BmkgRegion>,
    onQueryChange: (String) -> Unit,
    onSelectRegion: (BmkgRegion) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Pilih Wilayah BMKG", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Pilihan disimpan otomatis", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "Tutup",
                    modifier = Modifier.clickable { onDismiss() },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Cari desa, kecamatan, kota, atau kode adm4") },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.66f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            results.take(8).forEach { region ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectRegion(region) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(region.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("${region.province} • ${region.adm4}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (results.isEmpty()) {
                WeatherNoticeCard("Wilayah belum ada di katalog lokal. Masukkan kode adm4 ke katalog agar bisa dipilih.")
            }
        }
    }
}

@Composable
private fun WeatherMetricGrid(current: CurrentWeather) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        WeatherMetricCell(Icons.Default.WaterDrop, "Kelembapan", current.humidity?.let { "$it%" } ?: "--", Modifier.weight(1f))
        WeatherMetricCell(Icons.Default.Air, "Angin", current.windSpeed?.let { "${it.roundToInt()} km/j" } ?: "--", Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        WeatherMetricCell(Icons.Default.Umbrella, "Presipitasi", current.precipitation?.let { "$it mm" } ?: "--", Modifier.weight(1f))
        WeatherMetricCell(Icons.Default.WbSunny, "Terasa", current.apparentTemperature?.let { "${it.roundToInt()}°" } ?: "--", Modifier.weight(1f))
    }
}

@Composable
private fun WeatherMetricCell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BmkgWeatherIcon(
    imageUrl: String?,
    weatherCode: Int?,
    modifier: Modifier = Modifier
) {
    Icon(
        weatherIcon(weatherCode),
        contentDescription = null,
        tint = weatherAccentColor(weatherCode, isAppInDarkTheme()),
        modifier = modifier
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun HourlyForecastRow(items: List<HourlyForecastItem>) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items.forEachIndexed { index, item ->
            Box(
                modifier = Modifier
                    .width(104.dp)
                    .height(154.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val centerY = 48.dp.toPx()
                    if (index > 0) {
                        drawLine(
                            color = primary.copy(alpha = 0.34f),
                            start = Offset(0f, centerY),
                            end = Offset(size.width / 2f - 18.dp.toPx(), centerY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                    if (index < items.lastIndex) {
                        drawLine(
                            color = primary.copy(alpha = 0.34f),
                            start = Offset(size.width / 2f + 18.dp.toPx(), centerY),
                            end = Offset(size.width, centerY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                    drawCircle(
                        color = primary.copy(alpha = 0.18f),
                        radius = 23.dp.toPx(),
                        center = Offset(size.width / 2f, centerY)
                    )
                    drawCircle(
                        color = primary.copy(alpha = 0.88f),
                        radius = 4.dp.toPx(),
                        center = Offset(size.width / 2f, centerY)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatHour(item.time),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                            .border(1.dp, primary.copy(alpha = 0.28f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        BmkgWeatherIcon(item.imageUrl, item.weatherCode, Modifier.size(28.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.temperature?.let { "${it.roundToInt()}°" } ?: "--", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            item.description ?: weatherLabel(item.weatherCode),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(item.precipitationProbability?.let { "${it / 100.0} mm" } ?: "--", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyForecastCard(item: DailyForecastItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BmkgWeatherIcon(item.imageUrl, item.weatherCode, Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(formatDate(item.date), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(item.description ?: weatherLabel(item.weatherCode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "${item.temperatureMin?.roundToInt() ?: "--"}° / ${item.temperatureMax?.roundToInt() ?: "--"}°",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WeatherNoticeCard(message: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color(0xFF8A5A00).copy(alpha = 0.10f)) {
        Text(message, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8A5A00))
    }
}

@Composable
private fun WeatherAlertSection(items: List<WeatherAlertItem>) {
    var expanded by remember { mutableStateOf(false) }
    val visibleItems = if (expanded) items else items.take(1)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF8A1F11).copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            visibleItems.forEach { item ->
                WeatherAlertContent(item = item, expanded = expanded)
            }
            if (items.size > 1 || visibleItems.any { it.description.length > 140 }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF8A1F11).copy(alpha = 0.08f)
                ) {
                    Text(
                        text = if (expanded) "Sembunyikan" else "Tampilkan semua",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF8A1F11),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherAlertContent(item: WeatherAlertItem, expanded: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF8A1F11))
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = item.description.ifBlank { "Detail peringatan tersedia di kanal BMKG." },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 3
            )
        }
        if (item.pubDate.isNotBlank()) {
            Text(item.pubDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun BmkgAttributionCard(adm4: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Sumber data: BMKG", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(
                text = "Badan Meteorologi, Klimatologi, dan Geofisika. Prakiraan cuaca berbasis kode wilayah adm4${adm4.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun weatherIcon(code: Int?) =
    when (code) {
        0, 1 -> Icons.Default.WbSunny
        2, 3, 4, 5, 45, 48 -> Icons.Default.Cloud
        60, 61, 63, 65, 80, 81, 82, 95, 96, 97 -> Icons.Default.Umbrella
        else -> Icons.Default.Cloud
    }

private fun weatherAccentColor(code: Int?, isDark: Boolean): Color =
    when {
        isStormy(code) -> if (isDark) Color(0xFFFFD95A) else Color(0xFF7A5A00)
        isRainy(code) -> if (isDark) Color(0xFF78C7FF) else Color(0xFF1269A8)
        code in setOf(2, 3, 4, 5, 45, 48) -> if (isDark) Color(0xFFBFD8E6) else Color(0xFF537387)
        else -> if (isDark) Color(0xFFFFD87A) else GoldDeep
    }

private fun isRainy(code: Int?): Boolean =
    code in setOf(60, 61, 63, 65, 80, 81, 82, 95, 96, 97)

private fun isStormy(code: Int?): Boolean =
    code in setOf(95, 96, 97)

private fun DrawScope.drawSun(
    center: Offset,
    radius: Float,
    color: Color
) {
    repeat(12) { index ->
        val angle = (index * PI / 6).toFloat()
        val start = Offset(
            x = center.x + cos(angle.toDouble()).toFloat() * radius * 1.45f,
            y = center.y + sin(angle.toDouble()).toFloat() * radius * 1.45f
        )
        val end = Offset(
            x = center.x + cos(angle.toDouble()).toFloat() * radius * 2.08f,
            y = center.y + sin(angle.toDouble()).toFloat() * radius * 2.08f
        )
        drawLine(
            color = color.copy(alpha = 0.42f),
            start = start,
            end = end,
            strokeWidth = 2.dp.toPx()
        )
    }
    drawCircle(color = color.copy(alpha = 0.16f), radius = radius * 1.9f, center = center)
    drawCircle(color = color, radius = radius, center = center)
}

private fun DrawScope.drawWeatherCloud(
    center: Offset,
    scale: Float,
    color: Color
) {
    val s = scale.coerceAtLeast(0.42f)
    drawCircle(color = color, radius = 22.dp.toPx() * s, center = Offset(center.x - 22.dp.toPx() * s, center.y + 4.dp.toPx() * s))
    drawCircle(color = color, radius = 30.dp.toPx() * s, center = Offset(center.x + 2.dp.toPx() * s, center.y - 9.dp.toPx() * s))
    drawCircle(color = color, radius = 21.dp.toPx() * s, center = Offset(center.x + 31.dp.toPx() * s, center.y + 4.dp.toPx() * s))
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - 45.dp.toPx() * s, center.y + 2.dp.toPx() * s),
        size = Size(92.dp.toPx() * s, 26.dp.toPx() * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx() * s, 20.dp.toPx() * s)
    )
}

private fun weatherLabel(code: Int?): String =
    when (code) {
        0, 1 -> "Cerah"
        2 -> "Berawan sebagian"
        3 -> "Berawan"
        4, 5 -> "Udara kabur"
        45, 48 -> "Berkabut"
        60, 61 -> "Hujan ringan"
        63 -> "Hujan sedang"
        65 -> "Hujan lebat"
        80, 81, 82 -> "Hujan lokal"
        95, 96, 97 -> "Hujan petir"
        else -> "Cuaca tersedia"
    }

private fun formatHour(value: String): String =
    value.substringAfter("T", value).take(5).ifBlank { "--:--" }

private fun formatDate(value: String): String =
    value.ifBlank { "-" }
