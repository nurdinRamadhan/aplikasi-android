package com.alhasanah.alhasanahmedia.ui.qibla

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import com.alhasanah.alhasanahmedia.util.QiblaCompassAccuracy
import org.koin.androidx.compose.koinViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaScreen(
    navController: NavController,
    viewModel: QiblaViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshQiblaDirection()
    }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppSolidBackground(isDark = isAppInDarkTheme())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QiblaHeader(
                state = state,
                onBack = { navController.popBackStack() },
                onRefresh = { viewModel.refreshQiblaDirection() }
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                QiblaCompassCard(state = state)

                QiblaInfoCards(
                    state = state,
                    onRefresh = { viewModel.refreshQiblaDirection() },
                    onRequestLocation = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun QiblaBackground() {
    val isDark = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isDark) {
                        listOf(Color(0xFF0E0C08), Color(0xFF181410), MaterialTheme.colorScheme.background)
                    } else {
                        listOf(Color(0xFFFFFCF7), Color(0xFFFDF8F0), MaterialTheme.colorScheme.background)
                    }
                )
            )
    ) {
        val spacing = 94.dp.toPx()
        val starRadius = 13.dp.toPx()
        val cols = (size.width / spacing).toInt() + 2
        val rows = (size.height / spacing).toInt() + 2
        val color = primary.copy(alpha = if (isDark) 0.030f else 0.020f)

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
private fun QiblaHeader(
    state: QiblaUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val titleColor = if (isDark) Color(0xFFE8C55A) else Color(0xFFAA7C1F)
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.72f) else Color(0xFF2A2318).copy(alpha = 0.64f)
    val buttonBg = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.12f else 0.68f)
    val headerBrush = if (isDark) {
        Brush.verticalGradient(
            0.0f to Color(0xFF0E0C08),
            0.58f to Color(0xFF181410),
            1.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color(0xFFFFFCF7),
            0.58f to Color(0xFFFBF3E6),
            1.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        AppPageHeaderBackground(isDark = isDark, modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                HeaderIconButton(
                    modifier = Modifier.align(Alignment.CenterStart),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    background = buttonBg,
                    onClick = onBack
                )

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ARAH KIBLAT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.2.sp,
                        color = titleColor
                    )
                    Text(
                        text = "Kompas realtime dari lokasi Anda",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtitleColor
                    )
                }

                HeaderIconButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    icon = Icons.Default.Refresh,
                    contentDescription = "Muat ulang",
                    background = buttonBg,
                    onClick = onRefresh
                )
            }

            QiblaHeroStatus(state = state)
        }
    }
}

@Composable
private fun HeaderIconButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    background: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(background)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                shape = CircleShape
            )
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
private fun QiblaHeroStatus(state: QiblaUiState) {
    val isDark = isAppInDarkTheme()
    val heroBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF17130D), Color(0xFF211A10)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFF7E8)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(heroBrush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.46f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                    )
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = when {
                        state.isLoading -> "Mengambil lokasi dan arah kiblat"
                        state.isAligned -> "HP menghadap arah kiblat"
                        state.needsCompassCalibration -> "Kalibrasi kompas untuk hasil lebih stabil"
                        !state.sensorAvailable -> "Sensor kompas tidak tersedia"
                        else -> "Putar HP hingga jarum berada di atas"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = state.locationLabel.ifBlank { "Menunggu lokasi user" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QiblaCompassCard(state: QiblaUiState) {
    val animatedRotation by animateFloatAsState(
        targetValue = state.needleRotation,
        animationSpec = tween(durationMillis = 180),
        label = "qiblaNeedleRotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                        shape = CircleShape
                    )
            ) {
                CompassCanvas(
                    needleRotation = animatedRotation,
                    northRotation = -state.deviceHeading,
                    isAligned = state.isAligned
                )

                Text(
                    text = "N",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 18.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (state.isAligned) "Tepat" else "Ka'bah",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 76.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (state.isLoading && state.qiblaData == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CompassCanvas(
    needleRotation: Float,
    northRotation: Float,
    isAligned: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface
    val success = Color(0xFF2E7D32)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f

        drawCircle(
            color = outline.copy(alpha = 0.16f),
            radius = radius * 0.88f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        rotate(degrees = northRotation, pivot = center) {
            for (degree in 0 until 360 step 10) {
                val isMajor = degree % 30 == 0
                val tickLength = if (isMajor) 16.dp.toPx() else 8.dp.toPx()
                val strokeWidth = if (isMajor) 1.7.dp.toPx() else 1.dp.toPx()
                val angle = Math.toRadians((degree - 90).toDouble())
                val outer = Offset(
                    x = center.x + cos(angle).toFloat() * radius * 0.82f,
                    y = center.y + sin(angle).toFloat() * radius * 0.82f
                )
                val inner = Offset(
                    x = center.x + cos(angle).toFloat() * (radius * 0.82f - tickLength),
                    y = center.y + sin(angle).toFloat() * (radius * 0.82f - tickLength)
                )
                drawLine(
                    color = onSurface.copy(alpha = if (isMajor) 0.42f else 0.22f),
                    start = inner,
                    end = outer,
                    strokeWidth = strokeWidth
                )
            }
        }

        rotate(degrees = needleRotation, pivot = center) {
            val needleColor = if (isAligned) success else primary
            drawCircle(
                color = needleColor.copy(alpha = 0.10f),
                radius = radius * 0.70f,
                center = center
            )
            val path = Path().apply {
                moveTo(center.x, center.y - radius * 0.68f)
                lineTo(center.x - radius * 0.085f, center.y + radius * 0.08f)
                lineTo(center.x, center.y + radius * 0.18f)
                lineTo(center.x + radius * 0.085f, center.y + radius * 0.08f)
                close()
            }
            drawPath(path = path, color = needleColor)
            drawLine(
                color = needleColor.copy(alpha = 0.62f),
                start = center,
                end = Offset(center.x, center.y - radius * 0.72f),
                strokeWidth = 3.dp.toPx()
            )
        }

        drawCircle(
            color = surface,
            radius = 22.dp.toPx(),
            center = center
        )
        drawCircle(
            color = if (isAligned) success else primary,
            radius = 9.dp.toPx(),
            center = center
        )
    }
}

@Composable
private fun QiblaInfoCards(
    state: QiblaUiState,
    onRefresh: () -> Unit,
    onRequestLocation: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QiblaMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.MyLocation,
                label = "Arah kiblat",
                value = state.qiblaData?.direction?.let { "${it.formatDegrees()}°" } ?: "--"
            )
            QiblaMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Sensors,
                label = "Kompas HP",
                value = if (state.sensorAvailable) state.compassAccuracy.label else "--"
            )
        }

        LocationNoticeCard(
            state = state,
            onRequestLocation = onRequestLocation,
            onRefresh = onRefresh
        )

        if (state.needsCompassCalibration) {
            CalibrationCard()
        }

        state.errorMessage?.let {
            StatusCard(text = it, isWarning = true)
        }

        if (!state.sensorAvailable) {
            StatusCard(
                text = "Perangkat tidak menyediakan sensor kompas. Derajat kiblat tetap ditampilkan, tetapi jarum tidak dapat mengikuti rotasi HP.",
                isWarning = true
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onRequestLocation,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Lokasi")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onRefresh,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Muat Ulang")
            }
        }
    }
}

@Composable
private fun LocationNoticeCard(
    state: QiblaUiState,
    onRequestLocation: () -> Unit,
    onRefresh: () -> Unit
) {
    val isWarning = state.usingFallbackLocation
    val color = if (isWarning) Color(0xFF8A5A00) else MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = if (isWarning) 0.11f else 0.09f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isWarning) Icons.Default.LocationOn else Icons.Default.Verified,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = if (isWarning) "Lokasi user belum aktif" else "Lokasi user aktif",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = if (isWarning) {
                        "Arah yang tampil masih sementara. Izinkan lokasi dan pastikan GPS aktif agar arah kiblat dihitung dari posisi Anda."
                    } else {
                        val accuracy = state.locationAccuracyMeters
                            ?.let { " Akurasi lokasi sekitar ${it.toInt()} m." }
                            .orEmpty()
                        "Arah kiblat dihitung dari koordinat perangkat Anda.$accuracy"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isWarning) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Izinkan Lokasi",
                            modifier = Modifier.clickable { onRequestLocation() },
                            style = MaterialTheme.typography.labelLarge,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Muat Ulang",
                            modifier = Modifier.clickable { onRefresh() },
                            style = MaterialTheme.typography.labelLarge,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationCard() {
    val color = MaterialTheme.colorScheme.secondary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.10f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.ScreenRotation,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Kalibrasi kompas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "Gerakkan HP membentuk angka 8 beberapa kali, jauhkan dari magnet atau logam, lalu pegang HP mendatar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QiblaMetricCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatusCard(text: String, isWarning: Boolean) {
    val color = if (isWarning) Color(0xFF8A5A00) else MaterialTheme.colorScheme.primary
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

private fun Double.formatDegrees(): String =
    String.format(java.util.Locale.US, "%.1f", this)

private val QiblaCompassAccuracy.label: String
    get() = when (this) {
        QiblaCompassAccuracy.High -> "Akurat"
        QiblaCompassAccuracy.Medium -> "Cukup"
        QiblaCompassAccuracy.Low -> "Rendah"
        QiblaCompassAccuracy.Unreliable -> "Tidak stabil"
        QiblaCompassAccuracy.Unavailable -> "Tidak tersedia"
    }
