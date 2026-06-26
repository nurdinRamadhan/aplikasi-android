package com.alhasanah.alhasanahmedia.ui.quran

// ─────────────────────────────────────────────────────────────────────────────
// Imports
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// NoInternetScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NoInternetScreen(
    message: String = "Koneksi internet tidak tersedia",
    onRetry: () -> Unit
) {
    val isDark   = isSystemInDarkTheme()
    val primary  = MaterialTheme.colorScheme.primary
    val bg       = MaterialTheme.colorScheme.background

    // ── Ripple wave animation — 3 staggered rings ──────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "noInternet")

    // Each wave has a different delay offset baked into initialValue
    val wave1 by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label         = "wave1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue  = 0.33f,
        targetValue   = 1.33f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label         = "wave2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue  = 0.66f,
        targetValue   = 1.66f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label         = "wave3"
    )

    // Slow background pattern rotation
    val bgRotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(120_000, easing = LinearEasing)),
        label         = "bgRot"
    )

    // Icon subtle breathe
    val iconBreath by infiniteTransition.animateFloat(
        initialValue  = 0.94f,
        targetValue   = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "iconBreath"
    )

    // Retry button press
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val btnScale          by animateFloatAsState(
        targetValue   = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "btnScale"
    )

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {

        // ── Layer 1: Islamic Geometric Background ───────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 90.dp.toPx()
            val starR   = 15.dp.toPx()
            val cols    = (size.width  / spacing).toInt() + 2
            val rows    = (size.height / spacing).toInt() + 2
            val c       = primary.copy(alpha = 0.038f)

            for (col in -1..cols) {
                for (row in -1..rows) {
                    val stagger = if (col % 2 == 0) spacing / 2f else 0f
                    val center  = Offset(col * spacing, row * spacing + stagger)
                    val localR  = if ((col + row) % 2 == 0) bgRotation else -bgRotation
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

        // ── Layer 2: Radial Vignette Glow (center focus) ───────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = if (isDark) 0.07f else 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension * 0.65f
                )
            )
        }

        // ── Layer 3: Content Card ───────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Signal Wave Orb ─────────────────────────────────────────────
            Box(
                modifier         = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Animated ripple rings
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxRadius   = size.minDimension / 2f
                    val strokeWidth = 1.2.dp.toPx()

                    listOf(wave1, wave2, wave3).forEach { progress ->
                        val clampedProgress = progress % 1f
                        val radius  = maxRadius * clampedProgress
                        val alpha   = (1f - clampedProgress).coerceIn(0f, 1f) * 0.45f
                        drawCircle(
                            color  = primary.copy(alpha = alpha),
                            radius = radius,
                            center = center,
                            style  = Stroke(width = strokeWidth)
                        )
                    }
                }

                // Static inner glow ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primary.copy(alpha = if (isDark) 0.16f else 0.10f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Glass icon container with sweep border
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .scale(iconBreath)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primary.copy(alpha = if (isDark) 0.18f else 0.11f),
                                    primary.copy(alpha = if (isDark) 0.08f else 0.04f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    primary.copy(alpha = 0.0f),
                                    primary.copy(alpha = 0.55f),
                                    primary.copy(alpha = 0.15f),
                                    primary.copy(alpha = 0.55f),
                                    primary.copy(alpha = 0.0f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.WifiOff,
                        contentDescription = null,
                        tint               = primary,
                        modifier           = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Main Message Card ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.25f),
                                primary.copy(alpha = 0.05f),
                                primary.copy(alpha = 0.18f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(28.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {

                    // Section label
                    Text(
                        text  = "KONEKSI TERPUTUS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color         = primary,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize      = 9.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ornamental divider
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Text(
                            text  = "  ✦  ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color    = primary.copy(alpha = 0.45f),
                                fontSize = 8.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Headline
                    Text(
                        text      = "Ops! Koneksi Terputus",
                        style     = MaterialTheme.typography.titleLarge.copy(
                            fontWeight    = FontWeight.Black,
                            color         = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.3.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sub message
                    Text(
                        text      = "Mohon periksa sambungan internet Anda agar dapat terus mengakses data terbaru dari Pesantren.",
                        style     = MaterialTheme.typography.bodySmall.copy(
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // ── Retry Button ────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .scale(btnScale)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        primary,
                                        primary.copy(
                                            red   = (primary.red   + 0.06f).coerceAtMost(1f),
                                            green = (primary.green + 0.06f).coerceAtMost(1f)
                                        )
                                    )
                                )
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication        = null,
                                onClick           = onRetry
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.onPrimary,
                                modifier           = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text  = "COBA LAGI",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color         = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight    = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trust signal below button
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Wifi,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier           = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text  = "Aplikasi akan otomatis terhubung kembali",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                                fontWeight = FontWeight.Normal,
                                fontSize   = 9.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OfflineFeatureChip — Small card showing what works without internet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OfflineFeatureChip(
    icon: ImageVector,
    label: String,
    note: String,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = primary,
                modifier           = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Column {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color      = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 10.sp
                    )
                )
                Text(
                    text  = note,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Normal,
                        fontSize   = 8.sp
                    )
                )
            }
        }
    }
}
