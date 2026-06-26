package com.alhasanah.alhasanahmedia.ui.home

// ─────────────────────────────────────────────────────────────────────────────
// Imports — Alphabetically Sorted & Deduplicated
// ─────────────────────────────────────────────────────────────────────────────

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.alhasanah.alhasanahmedia.MainViewModel
import com.alhasanah.alhasanahmedia.R
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.ui.admin.AdminWebViewPreloader
import com.alhasanah.alhasanahmedia.ui.auth.AuthViewModel
import com.alhasanah.alhasanahmedia.ui.components.AppGradientBackground
import com.alhasanah.alhasanahmedia.ui.components.ThemeToggleButton
import com.alhasanah.alhasanahmedia.ui.components.appPanelBorderColor
import com.alhasanah.alhasanahmedia.ui.components.appPanelColor
import com.alhasanah.alhasanahmedia.ui.components.appPanelVariantColor
import com.alhasanah.alhasanahmedia.ui.theme.AlhasanahMediaTheme
import com.alhasanah.alhasanahmedia.ui.theme.AmiriFontFamily
import com.alhasanah.alhasanahmedia.util.PrayerTimeInfo
import com.alhasanah.alhasanahmedia.util.formatRupiah
import org.koin.androidx.compose.koinViewModel
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// HomeContent — Root Composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    isLoggedIn: Boolean,
    openDrawer: () -> Unit,
    onNotificationClick: () -> Unit,
    navController: NavController
) {
    val homeViewModel: HomeViewModel = koinViewModel()
    val authViewModel: AuthViewModel = koinViewModel()
    val activeSantriNis by authViewModel.activeSantriNis.collectAsState()
    val currentUserRole by authViewModel.currentUserRole.collectAsState()
    val beritaList      by homeViewModel.beritaState.collectAsState()
    val isLoadingBerita by homeViewModel.isLoadingBerita.collectAsState()
    val prayerState     by homeViewModel.prayerState.collectAsState()
    val ayatOfTheDay    by homeViewModel.ayatOfTheDay.collectAsState()
    val santriSummary   by homeViewModel.santriSummary.collectAsState()
    val mainViewModel: MainViewModel = koinViewModel()
    val themeMode       by mainViewModel.themeMode.collectAsState()
    val isSystemDark    = isSystemInDarkTheme()
    val useDarkTheme    = themeMode ?: isSystemDark
    val context = LocalContext.current

    LaunchedEffect(isLoggedIn, activeSantriNis, currentUserRole) {
        homeViewModel.loadSantriSummary(
            nis = if (isLoggedIn) activeSantriNis else null,
            role = currentUserRole
        )
    }

    LaunchedEffect(Unit) {
        AdminWebViewPreloader.preload(context)
    }

    // ── Shared state for action bar & header ───────────────────────────
    val primary       = MaterialTheme.colorScheme.primary
    val secondary     = MaterialTheme.colorScheme.secondary
    val actionTint    = if (useDarkTheme) Color(0xFFE4BD62) else primary

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        AppGradientBackground(isDark = useDarkTheme)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                HomeHeader(
                    prayerInfo         = prayerState,
                    isDark             = useDarkTheme
                )
            }

            ayatOfTheDay?.let { surah ->
                item {
                    AyatOfTheDayCard(surah, isDark = useDarkTheme) {
                        navController.navigate(Screen.SurahDetail.createRoute(surah.nomor))
                    }
                }
            }

            if (santriSummary.shouldShow) {
                item {
                    SantriSummaryCard(
                        state = santriSummary,
                        onToggleExpand = {
                            homeViewModel.setSantriSummaryExpanded(!santriSummary.isExpanded)
                        },
                        onOpenKeuangan = {
                            activeSantriNis?.let { navController.navigate(Screen.Keuangan.createRoute(it)) }
                        },
                        onOpenHafalan = {
                            activeSantriNis?.let { navController.navigate(Screen.Hafalan.createRoute(it)) }
                        },
                        onOpenAbsensi = {
                            activeSantriNis?.let { navController.navigate(Screen.Absensi.createRoute(it)) }
                        },
                        onOpenPerizinan = {
                            activeSantriNis?.let { navController.navigate(Screen.Perizinan.createRoute(it)) }
                        },
                        onOpenPelanggaran = {
                            activeSantriNis?.let { navController.navigate(Screen.Pelanggaran.createRoute(it)) }
                        },
                        onOpenKesehatan = {
                            activeSantriNis?.let { navController.navigate(Screen.Kesehatan.createRoute(it)) }
                        },
                        isDark = useDarkTheme
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PremiumFeatureSection(
                        title    = "FITUR & ALAT DIGITAL",
                        subtitle = "Akses cepat alat islami",
                        features = listOf(
                            FeatureItem("Al-Quran",      Icons.Default.Book),
                            FeatureItem("Hadist", Icons.AutoMirrored.Filled.MenuBook),
                            FeatureItem("Doa & Dzikir", Icons.Default.Spa),
                            FeatureItem("Kitab Kuning", Icons.AutoMirrored.Filled.LibraryBooks),
                            FeatureItem("Tuntunan Ibadah", Icons.Default.Mosque),
                            FeatureItem("Kalender Islam", Icons.Default.CalendarMonth),
                            FeatureItem("Tanya AI", Icons.Default.AutoAwesome),
                            FeatureItem("Donasi Infaq", Icons.Default.VolunteerActivism),
                            FeatureItem("Jadwal Sholat", Icons.Default.Schedule),
                            FeatureItem("Falaq Ephemeris", Icons.Default.Dataset),
                            FeatureItem("Cuaca", Icons.Default.Cloud),
                            FeatureItem("Kiblat",         Icons.Default.Explore)
                        ),
                        isDark = useDarkTheme,
                        onFeatureClick = { featureName ->
                            if (featureName.contains("Tanya")) {
                                navController.navigate(Screen.RagChat.route)
                            } else if (featureName.contains("Al-Quran")) {
                                navController.navigate(Screen.Quran.route)
                            } else if (featureName.contains("Hadist")) {
                                navController.navigate(Screen.Hadith.route)
                            } else if (featureName.contains("Doa")) {
                                navController.navigate(Screen.Devotion.route)
                            } else if (featureName.contains("Kitab")) {
                                navController.navigate(Screen.KitabKuning.route)
                            } else if (featureName.contains("Tuntunan")) {
                                navController.navigate(Screen.IbadahGuide.route)
                            } else if (featureName.contains("Kalender")) {
                                navController.navigate(Screen.IslamicCalendar.route)
                            } else if (featureName.contains("Donasi")) {
                                navController.navigate(Screen.Donasi.createRoute(activeSantriNis ?: ""))
                            } else if (featureName.contains("Jadwal")) {
                                navController.navigate(Screen.PrayerSchedule.route)
                            } else if (featureName.contains("Falaq")) {
                                navController.navigate(Screen.FalakEphemeris.route)
                            } else if (featureName.contains("Cuaca")) {
                                navController.navigate(Screen.Weather.route)
                            } else if (featureName.contains("Kiblat")) {
                                navController.navigate(Screen.Qibla.route)
                            }
                        }
                    )
                    PremiumFeatureSection(
                        title    = "SISTEM INFORMASI PESANTREN",
                        subtitle = "Data & informasi pesantren",
                        features = listOf(
                            FeatureItem("Admin Panel", Icons.Default.AdminPanelSettings),
                            FeatureItem("Forum Alumni", Icons.Default.Forum),
                            FeatureItem("Prestasi", Icons.Default.EmojiEvents),
                            FeatureItem("Kegiatan", Icons.Default.Info)
                        ),
                        isDark = useDarkTheme,
                        onFeatureClick = { featureName ->
                            if (featureName.contains("Admin")) {
                                navController.navigate(Screen.AdminPanel.createRoute())
                            } else if (featureName.contains("Forum")) {
                                navController.navigate(Screen.AlumniForum.createRoute())
                            } else if (featureName.contains("Prestasi")) {
                                navController.navigate(Screen.Prestasi.route)
                            }
                        }
                    )
                }
            }

            item {
                BeritaSection(
                    beritaList  = beritaList,
                    isLoading   = isLoadingBerita,
                    onBeritaClick = { slug ->
                        navController.navigate(Screen.BeritaDetail.createRoute(slug))
                    }
                )
            }
        }

        // ── Sticky Action Bar (selalu terlihat saat scroll) ─────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = openDrawer) {
                Icon(
                    imageVector       = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint              = actionTint,
                    modifier          = Modifier.size(24.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoggedIn) {
                    Box {
                        IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                            Icon(
                                imageVector       = Icons.Default.Notifications,
                                contentDescription = "Notifikasi",
                                tint              = actionTint
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-10).dp, y = 10.dp)
                                .background(secondary.copy(alpha = 0.82f), CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.background.copy(alpha = 0.9f), CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                ThemeToggleButton(
                    isDark   = useDarkTheme,
                    onToggle = { mainViewModel.toggleTheme(isSystemDark) },
                    tint     = actionTint
                )
            }
        }
    }
}
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeGeometricPattern(isDark: Boolean = isSystemInDarkTheme()) {
    val infiniteTransition = rememberInfiniteTransition(label = "bgPattern")

    // Slow ambient rotation — feels alive without being distracting
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120_000, easing = LinearEasing)
        ),
        label = "patternRotation"
    )

    val primaryColor = if (isDark) Color.White else MaterialTheme.colorScheme.primary
    val starAlpha = if (isDark) 0.018f else 0.020f
    val dotAlpha = if (isDark) 0.012f else 0.014f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing    = 88.dp.toPx()
        val starRadius = 20.dp.toPx()

        val cols = ((size.width  / spacing).toInt() + 2)
        val rows = ((size.height / spacing).toInt() + 2)

        for (col in -1..cols) {
            for (row in -1..rows) {
                val staggerOffset = if (col % 2 == 0) spacing / 2f else 0f
                val center = Offset(
                    x = col * spacing,
                    y = row * spacing + staggerOffset
                )
                // Alternating rotation for visual richness
                val localRotation = if ((col + row) % 2 == 0) rotation else -rotation

                rotate(degrees = localRotation, pivot = center) {
                    drawIslamicStar(
                        center = center,
                        radius = starRadius,
                        color  = primaryColor.copy(alpha = starAlpha)
                    )
                }

                // Subtle node dots at intersections
                drawCircle(
                    color  = primaryColor.copy(alpha = dotAlpha),
                    radius = 2.dp.toPx(),
                    center = center
                )
            }
        }
    }
}

// Exported so other screens can reuse the same pattern
fun DrawScope.drawIslamicStar(center: Offset, radius: Float, color: Color) {
    val path        = Path()
    val sides       = 8
    val innerRadius = radius * 0.60f

    for (i in 0 until sides * 2) {
        val r     = if (i % 2 == 0) radius else innerRadius
        val angle = (i * Math.PI / sides - Math.PI / 2).toFloat()
        val x     = center.x + r * cos(angle.toDouble()).toFloat()
        val y     = center.y + r * sin(angle.toDouble()).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

private fun homePanelColor(isDark: Boolean): Color =
    appPanelColor(isDark)

private fun homePanelVariantColor(isDark: Boolean): Color =
    appPanelVariantColor(isDark)

private fun homePanelBorderColor(isDark: Boolean): Color =
    appPanelBorderColor(isDark)

// ─────────────────────────────────────────────────────────────────────────────
// HomeHeader
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    prayerInfo: PrayerTimeInfo?,
    isDark: Boolean
) {
    val primary       = MaterialTheme.colorScheme.primary
    val headerImage   = if (isDark) R.drawable.dark_header else R.drawable.light_header
    val actionTint    = if (isDark) Color(0xFFE4BD62) else primary
    val mutedContent  = if (isDark) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val imageHeightDp = (screenWidthDp * (1361f / 1156f)).dp
    val maxHeightDp   = 480.dp
    val isCapped      = imageHeightDp > maxHeightDp
    val boxHeightDp   = if (isCapped) maxHeightDp else imageHeightDp

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── Header image ──────────────────────────────────────────────────
        Image(
            painter       = painterResource(id = headerImage),
            contentDescription = "Header Al-Hasanah Media",
            modifier      = Modifier
                .fillMaxWidth()
                .height(boxHeightDp),
            contentScale  = if (isCapped) ContentScale.Crop else ContentScale.FillBounds,
            alignment     = Alignment.TopCenter
        )

        // ── Location + Prayer Card ────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector       = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint              = actionTint.copy(alpha = 0.78f),
                    modifier          = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text  = prayerInfo?.locationName ?: "Mendeteksi Lokasi...",
                    color = mutedContent,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            NextPrayerTimeCard(prayerInfo = prayerInfo, isDark = isDark)
        }
    }
}

@Composable
private fun HomeHeroOrnament(isDark: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val lineColor = primary.copy(alpha = if (isDark) 0.18f else 0.12f)
    val domeColor = if (isDark) Color(0xFF2A2924).copy(alpha = 0.62f) else Color.White.copy(alpha = 0.72f)
    val domeLine = primary.copy(alpha = if (isDark) 0.34f else 0.26f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp)
    ) {
        val centerX = size.width / 2f
        val archTop = 84.dp.toPx()
        val archWidth = size.width * 1.08f
        val archHeight = 300.dp.toPx()

        repeat(3) { index ->
            val inset = index * 22.dp.toPx()
            drawArc(
                color = lineColor.copy(alpha = lineColor.alpha * (1f - index * 0.18f)),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(centerX - archWidth / 2f + inset, archTop + inset * 0.62f),
                size = androidx.compose.ui.geometry.Size(archWidth - inset * 2f, archHeight - inset),
                style = Stroke(width = (1.2f - index * 0.2f).dp.toPx())
            )
        }

        val domeBaseY = 292.dp.toPx()
        val domeWidth = size.width * 0.58f
        val domeHeight = 112.dp.toPx()
        val domePath = Path().apply {
            moveTo(centerX - domeWidth / 2f, domeBaseY)
            cubicTo(
                centerX - domeWidth * 0.48f, domeBaseY - domeHeight * 0.92f,
                centerX + domeWidth * 0.48f, domeBaseY - domeHeight * 0.92f,
                centerX + domeWidth / 2f, domeBaseY
            )
            close()
        }
        drawPath(domePath, domeColor)
        drawPath(domePath, domeLine, style = Stroke(width = 1.dp.toPx()))

        val finialX = centerX
        val finialTop = domeBaseY - domeHeight - 34.dp.toPx()
        drawLine(
            color = domeLine,
            start = Offset(finialX, domeBaseY - domeHeight * 0.82f),
            end = Offset(finialX, finialTop + 10.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        drawCircle(domeLine, radius = 5.dp.toPx(), center = Offset(finialX, finialTop + 9.dp.toPx()))
        drawCircle(domeLine.copy(alpha = 0.62f), radius = 2.5.dp.toPx(), center = Offset(finialX, finialTop))

        repeat(7) { index ->
            val x = centerX - domeWidth * 0.38f + index * domeWidth * 0.126f
            drawLine(
                color = domeLine.copy(alpha = 0.18f),
                start = Offset(x, domeBaseY - 6.dp.toPx()),
                end = Offset(centerX, domeBaseY - domeHeight * 0.78f),
                strokeWidth = 0.8.dp.toPx()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NextPrayerTimeCard — Premium with Live Pulse & Countdown Chip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NextPrayerTimeCard(prayerInfo: PrayerTimeInfo?, isDark: Boolean) {
    val nextPrayerName = prayerInfo?.name      ?: "Subuh"
    val nextPrayerTime = prayerInfo?.time      ?: "04:35"
    val countdown      = prayerInfo?.countdown ?: "00:00:00"
    val primary        = MaterialTheme.colorScheme.primary
    val cardContent    = if (isDark) Color(0xFFF4F0E7) else MaterialTheme.colorScheme.onSurface
    val mutedContent   = if (isDark) Color(0xFFB7C5C7) else MaterialTheme.colorScheme.onSurfaceVariant

    // Live pulse animation for the status dot
    val pulseTrans  = rememberInfiniteTransition(label = "livePulse")
    val pulseAlpha  by pulseTrans.animateFloat(
        initialValue  = 0.35f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by pulseTrans.animateFloat(
        initialValue  = 0.75f,
        targetValue   = 1.3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(homePanelColor(isDark))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = if (isDark) 0.48f else 0.52f),
                        homePanelBorderColor(isDark),
                        primary.copy(alpha = if (isDark) 0.30f else 0.38f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {

            // ── Left: Label + Prayer Name + Time ───────────────────────────
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Animated live dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .scale(pulseScale)
                            .background(
                                color = primary.copy(alpha = pulseAlpha),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text  = "SHOLAT BERIKUTNYA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight    = FontWeight.Bold,
                            color         = mutedContent
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text  = nextPrayerName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight    = FontWeight.Black,
                        color         = cardContent
                    )
                )
                Text(
                    text  = nextPrayerTime,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = mutedContent
                    )
                )
            }

            // ── Right: Countdown Chip ───────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = "HITUNG MUNDUR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight    = FontWeight.Bold,
                        color         = mutedContent,
                        fontSize      = 8.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.05f)
                            else Color.White.copy(alpha = 0.62f)
                        )
                        .border(
                            width  = 0.5.dp,
                            color  = primary.copy(alpha = 0.46f),
                            shape  = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text  = countdown,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color      = if (isDark) primary.copy(alpha = 0.95f) else cardContent,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Icon(
                    imageVector  = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint         = primary.copy(alpha = 0.75f),
                    modifier     = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AyatOfTheDayCard — Premium with Islamic Ornament & CTA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AyatOfTheDayCard(
    surah: com.alhasanah.alhasanahmedia.data.model.quran.SurahDetail,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val randomAyat        = remember(surah) { surah.ayahs.randomOrNull() }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "ayatCardScale"
    )
    val primary = MaterialTheme.colorScheme.primary

    if (randomAyat == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .scale(pressScale)
            .clickable(
                interactionSource = interactionSource,
                indication        = null
            ) { onClick() },
        colors    = CardDefaults.cardColors(containerColor = homePanelColor(isDark)),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            // ── Decorative corner Islamic star ornament ─────────────────────
            Canvas(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.TopEnd)
            ) {
                drawIslamicStar(
                    center = Offset(size.width * 0.85f, -size.height * 0.1f),
                    radius = size.width * 0.65f,
                    color  = primary.copy(alpha = 0.055f)
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {

                // ── Header Row ──────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Vertical accent bar
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(primary, primary.copy(alpha = 0.25f))
                                    ),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text  = "AYAT HARI INI",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight    = FontWeight.Black,
                                color         = primary
                            )
                        )
                    }

                    // Surah + Ayat chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = primary.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text     = "${surah.nameLatin} : ${randomAyat.ayahNumber}",
                            style    = MaterialTheme.typography.labelSmall.copy(
                                color      = primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.5.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // ── Arabic Text ─────────────────────────────────────────────
                Text(
                    text      = randomAyat.arab,
                    style     = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = AmiriFontFamily,
                        lineHeight = 46.sp,
                        color      = MaterialTheme.colorScheme.onSurface
                    ),
                    textAlign = TextAlign.End,
                    modifier  = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Translation ─────────────────────────────────────────────
                Text(
                    text     = randomAyat.translation,
                    style    = MaterialTheme.typography.bodySmall.copy(
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp,
                        fontStyle  = FontStyle.Italic
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))
                Divider(
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    thickness = 0.5.dp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // ── CTA Row ─────────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = "Baca Selengkapnya",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color         = primary,
                            fontWeight    = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector  = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint         = primary,
                        modifier     = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SantriSummaryCard — Compact Wali Dashboard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SantriSummaryCard(
    state: HomeSantriSummaryUiState,
    onToggleExpand: () -> Unit,
    onOpenKeuangan: () -> Unit,
    onOpenHafalan: () -> Unit,
    onOpenAbsensi: () -> Unit,
    onOpenPerizinan: () -> Unit,
    onOpenPelanggaran: () -> Unit,
    onOpenKesehatan: () -> Unit,
    isDark: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val rotation by animateFloatAsState(
        targetValue = if (state.isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "santriSummaryExpand"
    )

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = homePanelColor(isDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = primary, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RINGKASAN SANTRI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = primary,
                        letterSpacing = 1.4.sp
                    )
                    Text(
                        text = state.santriName.ifBlank { "Memuat data santri" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.santriMeta.isNotBlank()) {
                        Text(
                            text = state.santriMeta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.10f))
                        .clickable { onToggleExpand() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = if (state.isExpanded) "Sembunyikan" else "Tampilkan",
                        tint = primary,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }

            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    color = primary,
                    trackColor = primary.copy(alpha = 0.12f)
                )
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Payments,
                    label = "Tagihan",
                    value = if (state.unpaidCount > 0) "Belum lunas" else "Lunas",
                    accent = if (state.unpaidAmount > 0) MaterialTheme.colorScheme.secondary else primary,
                    onClick = onOpenKeuangan
                )
                SummaryMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.MenuBook,
                    label = "Hafalan",
                    value = state.latestHafalan,
                    accent = primary,
                    onClick = onOpenHafalan
                )
            }

            AnimatedVisibility(
                visible = state.isExpanded,
                enter = fadeIn(tween(180)) + expandVertically(),
                exit = fadeOut(tween(120)) + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryMetricTile(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.CheckCircle,
                        label = "Absensi",
                        value = "Lihat ringkasan absensi mingguan",
                        accent = primary,
                        onClick = onOpenAbsensi
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SummaryMetricTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.EventAvailable,
                            label = "Perizinan",
                            value = state.activePermit,
                            accent = primary,
                            onClick = onOpenPerizinan
                        )
                        SummaryMetricTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.HealthAndSafety,
                            label = "Kesehatan",
                            value = state.healthSummary,
                            accent = primary,
                            onClick = onOpenKesehatan
                        )
                    }
                    SummaryMetricTile(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.GppMaybe,
                        label = "Kedisiplinan",
                        value = state.violationSummary,
                        accent = if (state.violationSummary == "Tidak ada catatan") primary else MaterialTheme.colorScheme.error,
                        onClick = onOpenPelanggaran
                    )

                    if (state.chartItems.isNotEmpty()) {
                        MiniExpenseChart(
                            title = state.chartTitle,
                            total = state.chartItems.sumOf { it.amount },
                            items = state.chartItems
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 86.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.075f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniExpenseChart(
    title: String,
    total: Long,
    items: List<HomeSummaryChartItem>
) {
    val primary = MaterialTheme.colorScheme.primary
    val maxAmount = items.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("Total ${formatRupiah(total)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.BarChart, contentDescription = null, tint = primary)
            }
            items.forEachIndexed { index, item ->
                val fraction = (item.amount.toFloat() / maxAmount.toFloat()).coerceIn(0.08f, 1f)
                val color = listOf(
                    primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.error
                )[index % 4]
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatRupiah(item.amount), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(color.copy(alpha = 0.64f), color)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PremiumFeatureSection
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PremiumFeatureSection(
    title: String,
    subtitle: String,
    features: List<FeatureItem>,
    isDark: Boolean,
    onFeatureClick: (String) -> Unit,
    collapsedVisibleCount: Int = 6
) {
    PremiumDashboardCard(title = title, subtitle = subtitle, isDark = isDark) {
        var expanded by remember { mutableStateOf(false) }
        val canCollapse = features.size > collapsedVisibleCount
        val collapsedRows = features.take(collapsedVisibleCount).chunked(3)
        val expandedRows = features.drop(collapsedVisibleCount).chunked(3)
        val visibleRows = if (canCollapse) collapsedRows else features.chunked(if (features.size > 4) 3 else features.size.coerceAtLeast(1))

        Column(
            modifier = Modifier.animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            visibleRows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    rowItems.forEach { feature ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            PremiumFeatureIcon(
                                name = feature.name,
                                icon = feature.icon,
                                onClick = { onFeatureClick(feature.name) }
                            )
                        }
                    }
                    repeat((3 - rowItems.size).coerceAtLeast(0)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (canCollapse) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(180)) + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        expandedRows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Top
                            ) {
                                rowItems.forEach { feature ->
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        PremiumFeatureIcon(
                                            name = feature.name,
                                            icon = feature.icon,
                                            onClick = { onFeatureClick(feature.name) }
                                        )
                                    }
                                }
                                repeat((3 - rowItems.size).coerceAtLeast(0)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            if (canCollapse) {
                val iconRotation by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "featureExpandIconRotation"
                )
                val buttonColor by animateColorAsState(
                    targetValue = MaterialTheme.colorScheme.primary.copy(alpha = if (expanded) 0.14f else 0.09f),
                    animationSpec = tween(220),
                    label = "featureExpandButtonColor"
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(8.dp),
                    color = buttonColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(iconRotation),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (expanded) "Sembunyikan sebagian" else "Tampilkan semua fitur",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PremiumDashboardCard — Section Container with Accent Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PremiumDashboardCard(
    title: String,
    subtitle: String,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "dashboardCardShimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -320f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dashboardCardShimmerX"
    )

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = homePanelColor(isDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape     = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .drawBehind {
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                primary.copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            start = Offset(shimmerX, 0f),
                            end = Offset(shimmerX + 180f, 0f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(20.dp)
        ) {

            // Section header with accent bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(34.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(primary, primary.copy(alpha = 0.25f))
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text  = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight    = FontWeight.Black,
                            color         = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text  = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
            }

            Divider(
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.5.dp,
                modifier  = Modifier.padding(vertical = 14.dp)
            )

            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PremiumFeatureIcon — Gradient Orb + Press Scale Animation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PremiumFeatureIcon(
    name: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val pressScale        by animateFloatAsState(
        targetValue   = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "featureIconScale"
    )
    val primary = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "featureGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "featureGlowAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .scale(pressScale)
            .clickable(
                interactionSource = interactionSource,
                indication        = null
            ) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Outer diffuse glow ring
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primary.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            // Inner icon container with gradient fill + border
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.18f),
                                primary.copy(alpha = 0.07f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.35f),
                                primary.copy(alpha = 0.08f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector  = icon,
                    contentDescription = name,
                    modifier     = Modifier.size(22.dp),
                    tint         = primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text      = name,
            maxLines  = 2,
            overflow  = TextOverflow.Ellipsis,
            style     = MaterialTheme.typography.labelSmall.copy(
                fontSize      = 10.sp,
                lineHeight    = 13.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Data Model
// ─────────────────────────────────────────────────────────────────────────────

data class FeatureItem(val name: String, val icon: ImageVector)

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Home — Light")
@Composable
fun HomeContentLightPreview() {
    AlhasanahMediaTheme(darkTheme = false) {
        // HomeContent(isLoggedIn = true, openDrawer = {}, onNotificationClick = {}, navController = rememberNavController())
    }
}

@Preview(showBackground = true, name = "Home — Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeContentDarkPreview() {
    AlhasanahMediaTheme(darkTheme = true) {
        // HomeContent(isLoggedIn = true, openDrawer = {}, onNotificationClick = {}, navController = rememberNavController())
    }
}
