package com.alhasanah.alhasanahmedia.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.R
import com.alhasanah.alhasanahmedia.navigation.Screen
import kotlinx.coroutines.delay

// ─── Brand Color System ───────────────────────────────────────────────────────
private val BrandGold       = Color(0xFFE18D19)
private val BrandGoldLight  = Color(0xFFF5C458)
private val BrandGoldDim    = Color(0xFFB8711A)
private val SurfaceDeep     = Color(0xFF06060D)
private val SurfaceWarm     = Color(0xFF130E04)

// ─────────────────────────────────────────────────────────────────────────────
// Public entry point — navigates to Home after the reveal sequence completes
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SplashScreen(navController: NavController) {

    // Sequential reveal flags
    var logoVisible    by remember { mutableStateOf(false) }
    var textVisible    by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }

    // ── Logo: spring scale + fade ─────────────────────────────────────────────
    val logoAlpha by animateFloatAsState(
        targetValue  = if (logoVisible) 1f else 0f,
        animationSpec = tween(700, easing = LinearOutSlowInEasing),
        label        = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue  = if (logoVisible) 1f else 0.68f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    // ── Gold divider line expands from centre ─────────────────────────────────
    val lineProgress by animateFloatAsState(
        targetValue  = if (textVisible) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label        = "lineProgress"
    )

    // ── "PONDOK PESANTREN" — fade + slide-up ─────────────────────────────────
    val subtitleAlpha by animateFloatAsState(
        targetValue  = if (textVisible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 100, easing = LinearOutSlowInEasing),
        label        = "subtitleAlpha"
    )
    val subtitleSlide by animateFloatAsState(
        targetValue  = if (textVisible) 0f else 14f,
        animationSpec = tween(500, delayMillis = 100, easing = FastOutSlowInEasing),
        label        = "subtitleSlide"
    )

    // ── "AL-HASANAH" — fade + slide-up (offset 260 ms) ───────────────────────
    val titleAlpha by animateFloatAsState(
        targetValue  = if (textVisible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 260, easing = LinearOutSlowInEasing),
        label        = "titleAlpha"
    )
    val titleSlide by animateFloatAsState(
        targetValue  = if (textVisible) 0f else 20f,
        animationSpec = tween(600, delayMillis = 260, easing = FastOutSlowInEasing),
        label        = "titleSlide"
    )

    // ── Bottom tagline ────────────────────────────────────────────────────────
    val taglineAlpha by animateFloatAsState(
        targetValue  = if (taglineVisible) 1f else 0f,
        animationSpec = tween(700, easing = LinearOutSlowInEasing),
        label        = "taglineAlpha"
    )

    // ── Infinite ambients ─────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.28f, targetValue = 0.80f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    val ornamentRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing)),
        label = "ornamentRot"
    )

    // ── Orchestrated timing sequence ──────────────────────────────────────────
    LaunchedEffect(Unit) {
        delay(180)
        logoVisible = true
        delay(660)
        textVisible = true
        delay(420)
        taglineVisible = true
        delay(1740)                         // Total on-screen ≈ 3.0 s
        runCatching {
            navController.navigate(Screen.Home.route) {
                launchSingleTop = true
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    SplashContent(
        logoAlpha        = logoAlpha,
        logoScale        = logoScale,
        glowPulse        = glowPulse,
        ornamentRotation = ornamentRotation,
        lineProgress     = lineProgress,
        subtitleAlpha    = subtitleAlpha,
        subtitleSlide    = subtitleSlide,
        titleAlpha       = titleAlpha,
        titleSlide       = titleSlide,
        taglineAlpha     = taglineAlpha,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Pure stateless composable — easy to preview in Android Studio
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SplashContent(
    logoAlpha        : Float,
    logoScale        : Float,
    glowPulse        : Float,
    ornamentRotation : Float,
    lineProgress     : Float,
    subtitleAlpha    : Float,
    subtitleSlide    : Float,
    titleAlpha       : Float,
    titleSlide       : Float,
    taglineAlpha     : Float,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0.00f to SurfaceWarm,
                    0.50f to Color(0xFF0E0B07),
                    1.00f to SurfaceDeep,
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // ── 1. Layered ambient glow (3 concentric halos) ──────────────────────
        Canvas(
            modifier = Modifier
                .size(340.dp)
                .offset(y = (-36).dp)
                .alpha(logoAlpha)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            data class GlowLayer(val radiusFraction: Float, val color: Color, val alphaScale: Float)
            listOf(
                GlowLayer(0.50f, BrandGold,      glowPulse * 0.16f),
                GlowLayer(0.32f, BrandGold,      glowPulse * 0.28f),
                GlowLayer(0.18f, BrandGoldLight, glowPulse * 0.42f),
            ).forEach { layer ->
                val r = size.minDimension * layer.radiusFraction
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            layer.color.copy(alpha = layer.alphaScale),
                            Color.Transparent
                        ),
                        center = center,
                        radius = r
                    ),
                    radius = r,
                    center = center
                )
            }
        }

        // ── 2. Islamic-inspired geometric ornament (dual ring) ────────────────
        Canvas(
            modifier = Modifier
                .size(238.dp)
                .offset(y = (-36).dp)
                .alpha(logoAlpha * 0.24f)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = size.minDimension / 2f - 2.dp.toPx()
            val thinStroke  = Stroke(0.7.dp.toPx())
            val hairStroke  = Stroke(0.5.dp.toPx())

            // Outer ring + 8 radial lines + accent dots
            rotate(ornamentRotation, center) {
                drawCircle(color = BrandGold, radius = outerR, center = center, style = thinStroke)

                repeat(8) { i ->
                    val angle = Math.toRadians(i * 45.0)
                    val cos   = Math.cos(angle).toFloat()
                    val sin   = Math.sin(angle).toFloat()
                    drawLine(
                        color       = BrandGold,
                        start       = Offset(center.x + outerR * 0.50f * cos, center.y + outerR * 0.50f * sin),
                        end         = Offset(center.x + outerR * cos,          center.y + outerR * sin),
                        strokeWidth = 0.7.dp.toPx()
                    )
                    drawCircle(
                        color  = BrandGoldLight,
                        radius = 2.2.dp.toPx(),
                        center = Offset(center.x + outerR * cos, center.y + outerR * sin)
                    )
                }
            }

            // Inner counter-rotating ring
            rotate(-ornamentRotation * 0.38f, center) {
                drawCircle(
                    color  = BrandGoldDim,
                    radius = outerR * 0.66f,
                    center = center,
                    style  = hairStroke
                )
                // 12-point inner tick marks
                repeat(12) { i ->
                    val angle = Math.toRadians(i * 30.0)
                    val cos   = Math.cos(angle).toFloat()
                    val sin   = Math.sin(angle).toFloat()
                    val r     = outerR * 0.66f
                    drawLine(
                        color       = BrandGoldDim,
                        start       = Offset(center.x + r * 0.88f * cos, center.y + r * 0.88f * sin),
                        end         = Offset(center.x + r * cos,          center.y + r * sin),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
            }
        }

        // ── 3. Main content column ────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier            = Modifier.fillMaxSize()
        ) {
            Spacer(Modifier.weight(1f))

            // Logo mark
            Image(
                painter           = painterResource(id = R.drawable.logo),
                contentDescription = "Logo Al-Hasanah",
                modifier          = Modifier
                    .size(128.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha)
            )

            Spacer(Modifier.height(36.dp))

            // ── Gold shimmer divider ──────────────────────────────────────────
            Canvas(
                Modifier
                    .width(180.dp)
                    .height(1.2.dp)
            ) {
                val w         = size.width
                val lineWidth = w * lineProgress
                val startX    = (w - lineWidth) / 2f
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BrandGoldDim,
                            BrandGoldLight,
                            BrandGold,
                            BrandGoldLight,
                            BrandGoldDim,
                            Color.Transparent
                        )
                    ),
                    start       = Offset(startX, 0f),
                    end         = Offset(startX + lineWidth, 0f),
                    strokeWidth = size.height
                )
            }

            Spacer(Modifier.height(20.dp))

            // "PONDOK PESANTREN"
            Text(
                text      = "PONDOK PESANTREN",
                style     = MaterialTheme.typography.labelMedium.copy(
                    fontWeight    = FontWeight.Light,
                    letterSpacing = 5.sp,
                    color         = Color.White.copy(alpha = 0.52f),
                    fontSize      = 10.sp
                ),
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .alpha(subtitleAlpha)
                    .offset(y = subtitleSlide.dp)
            )

            Spacer(Modifier.height(4.dp))

            // "AL-HASANAH"
            Text(
                text      = "AL-HASANAH",
                style     = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 5.sp,
                    color         = BrandGold,
                    fontSize      = 30.sp
                ),
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleSlide.dp)
            )

            Spacer(Modifier.weight(1f))

            // ── Bottom badge ──────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier
                    .padding(bottom = 52.dp)
                    .alpha(taglineAlpha)
            ) {
                // Decorative triple-dot ornament
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Canvas(Modifier.size(3.dp)) { drawCircle(BrandGoldDim) }
                    Canvas(Modifier.size(4.5.dp)) { drawCircle(BrandGold) }
                    Canvas(Modifier.size(3.dp)) { drawCircle(BrandGoldDim) }
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    text  = "Media & Informasi Pesantren",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color         = Color.White.copy(alpha = 0.28f),
                        letterSpacing = 1.2.sp,
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.Light
                    )
                )
            }
        }
    }
}
