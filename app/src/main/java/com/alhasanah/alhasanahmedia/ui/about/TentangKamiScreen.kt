package com.alhasanah.alhasanahmedia.ui.about

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhasanah.alhasanahmedia.R
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme

// ── Luxury color tokens (konsisten dengan BeritaDetailScreen) ──────────────
private val LuxuryGold      = Color(0xFFD4A853)
private val LuxuryGoldLight = Color(0xFFECC96B)
private val LuxuryGoldDim   = Color(0xFF9A7535)

private val DarkPaper  = Color(0xFF0F0D0A)
private val LightPaper = Color(0xFFFDF9F3)

// ── Contact data ───────────────────────────────────────────────────────────
private const val DEV_EMAIL     = "nurdincrs123@gmail.com"
private const val DEV_WHATSAPP  = "0882000979741"
private const val WA_MESSAGE    = "Halo, saya ingin bertanya tentang aplikasi Al-Hasanah Media."

// ─────────────────────────────────────────────────────────────────────────────
// ██  SCREEN ROOT
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TentangKamiScreen(
    onBack: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val pageBg = if (isDark) Color.Transparent else LightPaper

    Scaffold(
        containerColor = pageBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isDark) {
                AppSolidBackground(isDark = true)
            }
            TentangKamiContent(isDark = isDark, pageBg = pageBg, onBack = onBack)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  CONTENT
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TentangKamiContent(
    isDark: Boolean,
    pageBg: Color,
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── 1. Hero section ─────────────────────────────────────────────────
        item {
            TentangKamiHero(isDark = isDark, onBack = onBack)
        }

        // ── 2. Editorial divider ────────────────────────────────────────────
        item {
            EditorialDivider()
        }

        // ── 3. Judul "Tentang Kami" ────────────────────────────────────────
        item {
            TentangKamiTitle(isDark = isDark)
        }

        // ── 4. Body text (5 paragraf) ──────────────────────────────────────
        item {
            TentangKamiBody(isDark = isDark)
        }

        // ── 5. Divider sebelum contact ──────────────────────────────────────
        item {
            EditorialDivider()
        }

        // ── 6. Contact Developer ────────────────────────────────────────────
        item {
            ContactDeveloper(
                isDark = isDark,
                onEmailClick = {
                    uriHandler.openUri("mailto:$DEV_EMAIL?subject=Tentang%20Aplikasi%20Al-Hasanah%20Media")
                },
                onWhatsAppClick = {
                    val encoded = java.net.URLEncoder.encode(WA_MESSAGE, "UTF-8")
                    uriHandler.openUri("https://wa.me/62${DEV_WHATSAPP.drop(1)}?text=$encoded")
                }
            )
        }

        // ── 7. Footer ───────────────────────────────────────────────────────
        item {
            AboutFooter(isDark = isDark)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  HERO SECTION
// Logo + "PONPES AL-HASANAH" dengan gradient emas hangat
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TentangKamiHero(
    isDark: Boolean,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
    ) {
        // ── Gradient background ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1C1408),
                                Color(0xFF0F0D0A)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFF7E6),
                                Color(0xFFF3E2BC)
                            )
                        )
                    }
                )
        ) {
            // Radial glow emas di tengah
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            LuxuryGold.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.5f
                    ),
                    radius = size.minDimension * 0.5f
                )
            }
        }

        // ── 3-zone cinematic scrim ──────────────────────────────────────────
        // Top scrim — back button readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom scrim — transisi ke body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (isDark) Color(0xFF0F0D0A) else LightPaper
                        )
                    )
                )
        )

        // ── Back button ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            HeroActionButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                onClick = onBack
            )
        }

        // ── Logo + Title ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo with glow
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = CircleShape,
                        ambientColor = LuxuryGold.copy(alpha = 0.25f),
                        spotColor = LuxuryGold.copy(alpha = 0.15f)
                    )
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color(0xFF1C1408).copy(alpha = 0.8f)
                        else Color.White.copy(alpha = 0.9f)
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                LuxuryGold.copy(alpha = 0.6f),
                                LuxuryGoldLight.copy(alpha = 0.3f),
                                LuxuryGold.copy(alpha = 0.6f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo Al-Hasanah",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }

            // "PONPES AL-HASANAH"
            Text(
                text  = "PONPES AL-HASANAH",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color         = if (isDark) LuxuryGoldLight else Color(0xFF6B4F1D),
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    fontSize      = 20.sp
                ),
                textAlign = TextAlign.Center
            )

            // Subtitle tagline
            Text(
                text  = "Membangun Layanan Pesantren yang Lebih Baik",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color      = if (isDark)
                        Color.White.copy(alpha = 0.50f)
                    else
                        Color(0xFF6B4F1D).copy(alpha = 0.55f),
                    fontWeight = FontWeight.Normal,
                    fontSize   = 12.sp,
                    fontStyle  = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Hero action button (back) ──────────────────────────────────────────────
@Composable
private fun HeroActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.32f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector       = icon,
            contentDescription = contentDescription,
            tint              = Color.White.copy(alpha = 0.92f),
            modifier          = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  EDITORIAL DIVIDER (konsisten dengan BeritaDetailScreen)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EditorialDivider() {
    val infiniteTransition = rememberInfiniteTransition(label = "divider_pulse")
    val centerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue  = 0.90f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "divAlpha"
    )

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
        ) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        LuxuryGoldDim.copy(alpha = 0.30f),
                        LuxuryGold.copy(alpha = 0.55f),
                        LuxuryGold.copy(alpha = 0.70f),
                        Color.Transparent
                    )
                ),
                start       = Offset(0f, 0f),
                end         = Offset(size.width, 0f),
                strokeWidth = size.height
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(3.dp)) {
                drawCircle(LuxuryGoldDim.copy(alpha = centerAlpha * 0.6f))
            }
            Canvas(Modifier.size(5.dp)) {
                drawCircle(LuxuryGold.copy(alpha = centerAlpha))
            }
            Canvas(Modifier.size(3.dp)) {
                drawCircle(LuxuryGoldDim.copy(alpha = centerAlpha * 0.6f))
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
        ) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        LuxuryGold.copy(alpha = 0.70f),
                        LuxuryGold.copy(alpha = 0.55f),
                        LuxuryGoldDim.copy(alpha = 0.30f),
                        Color.Transparent
                    )
                ),
                start       = Offset(0f, 0f),
                end         = Offset(size.width, 0f),
                strokeWidth = size.height
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  TITLE SECTION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TentangKamiTitle(isDark: Boolean) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text  = "Tentang Kami",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color      = if (isDark) Color.White else Color(0xFF1A1208),
                lineHeight = 36.sp,
                fontSize   = 24.sp
            )
        )

        // Gold accent bar di bawah judul
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(3.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(LuxuryGold, LuxuryGoldDim.copy(alpha = 0.4f))
                    ),
                    RoundedCornerShape(2.dp)
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  BODY TEXT — 5 paragraf persis seperti yang diberikan user
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TentangKamiBody(isDark: Boolean) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val paragraphs = listOf(
            "Aplikasi ini dikembangkan sebagai bagian dari ikhtiar untuk menghadirkan layanan pesantren yang lebih modern, tertata, dan berorientasi pada kemudahan akses bagi wali santri maupun publik. Dalam setiap prosesnya, kami menjunjung tinggi nilai amanah, ketelitian, dan kebermanfaatan sebagai landasan utama dalam membangun sistem yang benar-benar relevan dengan kebutuhan pesantren.",

            "Pengembangan sistem ini dilakukan melalui proses yang cermat dan bertahap, dengan melibatkan unsur yang bersinggungan langsung dengan kebutuhan operasional di lingkungan tahfidz. Kolaborasi ini menjadi bagian penting dalam memastikan bahwa setiap fitur yang dihadirkan tidak hanya baik secara teknis, tetapi juga selaras dengan alur kerja, budaya, dan karakter layanan pesantren.",

            "Kami memahami bahwa pesantren bukan sekadar lembaga pendidikan, melainkan ruang pembinaan yang memiliki nilai, tradisi, dan tanggung jawab besar terhadap para santri serta keluarganya. Karena itu, sistem ini dirancang untuk mendukung pelayanan yang lebih transparan, memperkuat komunikasi, serta membantu penyajian informasi yang lebih jelas, cepat, dan dapat dipertanggungjawabkan.",

            "Fokus kami bukan hanya pada pembangunan aplikasi, melainkan pada penciptaan pengalaman layanan yang lebih tenang dan meyakinkan bagi wali santri. Melalui sistem ini, kami berharap proses komunikasi, pemantauan, dan pengelolaan data dapat berjalan lebih rapi, efisien, dan bermanfaat dalam jangka panjang.",

            "Kami berkomitmen untuk terus menjaga kualitas pengembangan, melakukan penyempurnaan secara berkelanjutan, serta memastikan bahwa setiap langkah yang diambil tetap berpihak pada kemaslahatan pesantren dan seluruh pihak yang menjadi bagian dari amanah ini."
        )

        paragraphs.forEach { text ->
            Text(
                text      = text,
                modifier  = Modifier.fillMaxWidth(),
                style     = MaterialTheme.typography.bodyLarge.copy(
                    color      = if (isDark)
                        Color.White.copy(alpha = 0.82f)
                    else
                        Color(0xFF2A1F0E).copy(alpha = 0.82f),
                    lineHeight = 30.sp,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign  = TextAlign.Start
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  BANTUAN & DUKUNGAN — Prominent cards with shadow/glow contrast
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ContactDeveloper(
    isDark: Boolean,
    onEmailClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section label
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier             = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(11.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        RoundedCornerShape(2.dp)
                    )
            )
            Text(
                text  = "BANTUAN & DUKUNGAN",
                style = MaterialTheme.typography.labelSmall.copy(
                    color         = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f),
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontSize      = 9.5.sp
                )
            )
        }

        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Email card ──────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = if (isDark) 0.dp else 8.dp,
                        shape     = RoundedCornerShape(18.dp),
                        ambientColor = if (isDark) Color.Transparent else Color(0xFF000000).copy(alpha = 0.08f),
                        spotColor    = if (isDark) Color.Transparent else Color(0xFF000000).copy(alpha = 0.06f)
                    )
                    .clickable(onClick = onEmailClick),
                shape    = RoundedCornerShape(18.dp),
                color    = if (isDark)
                    Color(0xFF1A150C)
                else
                    Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isDark)
                        LuxuryGold.copy(alpha = 0.25f)
                    else
                        Color(0xFFE8DCC8).copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon with glow ring
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (isDark)
                                    LuxuryGold.copy(alpha = 0.12f)
                                else
                                    Color(0xFFFFF7E6),
                                shape = CircleShape
                            )
                            .then(
                                if (isDark) {
                                    Modifier.shadow(
                                        elevation = 16.dp,
                                        shape     = CircleShape,
                                        ambientColor = LuxuryGold.copy(alpha = 0.30f),
                                        spotColor    = LuxuryGold.copy(alpha = 0.15f)
                                    )
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector       = Icons.Filled.Email,
                            contentDescription = null,
                            tint              = if (isDark) LuxuryGold else Color(0xFF9A7535),
                            modifier          = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text  = "Email Kami",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color   = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFFA09480),
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text  = "nurdincrs123@gmail.com",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color      = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF2A1F0E),
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 12.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── WhatsApp card ───────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = if (isDark) 0.dp else 8.dp,
                        shape     = RoundedCornerShape(18.dp),
                        ambientColor = if (isDark) Color.Transparent else Color(0xFF000000).copy(alpha = 0.08f),
                        spotColor    = if (isDark) Color.Transparent else Color(0xFF000000).copy(alpha = 0.06f)
                    )
                    .clickable(onClick = onWhatsAppClick),
                shape    = RoundedCornerShape(18.dp),
                color    = if (isDark)
                    Color(0xFF0D1A12)
                else
                    Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isDark)
                        Color(0xFF25D366).copy(alpha = 0.30f)
                    else
                        Color(0xFFE8DCC8).copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon with green glow ring (dark) or warm bg (light)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (isDark)
                                    Color(0xFF25D366).copy(alpha = 0.12f)
                                else
                                    Color(0xFFE8F5E9),
                                shape = CircleShape
                            )
                            .then(
                                if (isDark) {
                                    Modifier.shadow(
                                        elevation = 16.dp,
                                        shape     = CircleShape,
                                        ambientColor = Color(0xFF25D366).copy(alpha = 0.30f),
                                        spotColor    = Color(0xFF25D366).copy(alpha = 0.15f)
                                    )
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector       = Icons.Filled.Phone,
                            contentDescription = null,
                            tint              = if (isDark) Color(0xFF25D366) else Color(0xFF1B7A3D),
                            modifier          = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text  = "WhatsApp",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color   = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFFA09480),
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text  = "0882 0009 79741",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color      = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF2A1F0E),
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 12.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  FOOTER — konsisten dengan BeritaDetailScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AboutFooter(isDark: Boolean) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
        ) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        LuxuryGoldDim.copy(alpha = 0.20f),
                        LuxuryGold.copy(alpha = 0.50f),
                        LuxuryGoldDim.copy(alpha = 0.20f),
                        Color.Transparent
                    )
                ),
                start       = Offset(0f, 0f),
                end         = Offset(size.width, 0f),
                strokeWidth = size.height
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(3.dp)) { drawCircle(LuxuryGoldDim.copy(alpha = 0.40f)) }
            Canvas(Modifier.size(5.dp)) { drawCircle(LuxuryGold.copy(alpha = 0.65f)) }
            Canvas(Modifier.size(3.dp)) { drawCircle(LuxuryGoldDim.copy(alpha = 0.40f)) }
        }

        Text(
            text      = "AL-HASANAH MEDIA",
            style     = MaterialTheme.typography.labelSmall.copy(
                color         = LuxuryGoldDim.copy(alpha = 0.50f),
                letterSpacing = 2.sp,
                fontSize      = 8.5.sp,
                fontWeight    = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
    }
}
