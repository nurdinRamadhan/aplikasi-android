package com.alhasanah.alhasanahmedia.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alhasanah.alhasanahmedia.util.WarningAmber
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// DrawScope helper — Islamic 8-pointed star polygon
// Outer points at 45° intervals, inner points midway between.
// ─────────────────────────────────────────────────────────────────────────────
private fun DrawScope.drawIslamicStar(
    center        : Offset,
    outerRadius   : Float,
    innerRadius   : Float,
    fillColor     : Color,
    strokeColor   : Color,
    strokeWidthPx : Float
) {
    val path = Path()
    repeat(8) { i ->
        val outerAngle = (i * PI / 4.0 - PI / 2.0).toFloat()
        val innerAngle = (outerAngle + PI / 8.0).toFloat()
        val ox = center.x + outerRadius * cos(outerAngle)
        val oy = center.y + outerRadius * sin(outerAngle)
        val ix = center.x + innerRadius * cos(innerAngle)
        val iy = center.y + innerRadius * sin(innerAngle)
        if (i == 0) path.moveTo(ox, oy) else path.lineTo(ox, oy)
        path.lineTo(ix, iy)
    }
    path.close()
    drawPath(path, color = fillColor)
    drawPath(path, color = strokeColor, style = Stroke(strokeWidthPx, cap = StrokeCap.Round))
}

// ─────────────────────────────────────────────────────────────────────────────
// IslamicOrnamentCanvas
//
// Menggunakan dua animasi infinite:
//   • Float ±3dp — efek "bernapas", lambat dan elegan (bukan bounce toy-like)
//   • Rotasi luar 360°/20s — sangat pelan, seperti kompas; hanya terlihat
//     jika diperhatikan — kesan ultra-premium
//
// Elemen yang digambar (semua via Canvas, tanpa emoji):
//   1. Outer rotating dash ring (16 garis pendek)
//   2. Static thin ring
//   3. Main 8-pointed Islamic star (primary tint)
//   4. Secondary gold star, rotated 22.5°
//   5. Center medallion (dua lingkaran konsentris)
//   6. 8 accent dots di titik luar bintang
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IslamicOrnamentCanvas(
    primaryColor : Color,
    goldColor    : Color,
    modifier     : Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "ornament")

    val floatOffset by infinite.animateFloat(
        initialValue = -3f,
        targetValue  = 3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    val outerRot by infinite.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRot"
    )

    Canvas(modifier = modifier.offset(y = floatOffset.dp)) {
        val cx     = size.width  / 2f
        val cy     = size.height / 2f
        val maxR   = size.minDimension / 2f
        val center = Offset(cx, cy)

        // 1. Rotating outer dash ring
        rotate(degrees = outerRot, pivot = center) {
            repeat(16) { i ->
                val angle = (i * 2.0 * PI / 16).toFloat()
                drawLine(
                    color       = goldColor.copy(alpha = 0.22f),
                    start       = Offset(cx + maxR * 0.85f * cos(angle), cy + maxR * 0.85f * sin(angle)),
                    end         = Offset(cx + maxR * 0.96f * cos(angle), cy + maxR * 0.96f * sin(angle)),
                    strokeWidth = 1.1.dp.toPx(),
                    cap         = StrokeCap.Round
                )
            }
        }

        // 2. Static thin ring
        drawCircle(
            color  = primaryColor.copy(alpha = 0.14f),
            center = center,
            radius = maxR * 0.80f,
            style  = Stroke(width = 0.8.dp.toPx())
        )

        // 3. Main 8-pointed star — primary tint
        drawIslamicStar(
            center        = center,
            outerRadius   = maxR * 0.60f,
            innerRadius   = maxR * 0.27f,
            fillColor     = primaryColor.copy(alpha = 0.09f),
            strokeColor   = primaryColor.copy(alpha = 0.26f),
            strokeWidthPx = 1.0.dp.toPx()
        )

        // 4. Secondary gold star, rotated 22.5° (classic Islamic layering)
        rotate(degrees = 22.5f, pivot = center) {
            drawIslamicStar(
                center        = center,
                outerRadius   = maxR * 0.43f,
                innerRadius   = maxR * 0.20f,
                fillColor     = goldColor.copy(alpha = 0.09f),
                strokeColor   = goldColor.copy(alpha = 0.22f),
                strokeWidthPx = 0.8.dp.toPx()
            )
        }

        // 5. Center medallion (dua lingkaran konsentris)
        drawCircle(
            color  = primaryColor.copy(alpha = 0.22f),
            center = center,
            radius = maxR * 0.12f
        )
        drawCircle(
            color  = goldColor.copy(alpha = 0.18f),
            center = center,
            radius = maxR * 0.06f
        )

        // 6. Accent dots di 8 titik bintang luar
        repeat(8) { i ->
            val angle = (i * PI / 4.0 - PI / 2.0).toFloat()
            drawCircle(
                color  = (if (i % 2 == 0) goldColor else primaryColor).copy(alpha = 0.40f),
                center = Offset(cx + maxR * 0.60f * cos(angle), cy + maxR * 0.60f * sin(angle)),
                radius = 2.3.dp.toPx()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OrnamentDivider — separates ornament zone from text content
// Motif: ── • ◉ • ──  (gradien fade ke tengah)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OrnamentDivider(primaryColor: Color, isDark: Boolean) {
    val lineColor = primaryColor.copy(alpha = if (isDark) 0.18f else 0.13f)
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(0.8.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, lineColor)))
        )
        Spacer(Modifier.width(8.dp))
        Canvas(Modifier.size(5.dp))  { drawCircle(primaryColor.copy(alpha = 0.32f)) }
        Spacer(Modifier.width(5.dp))
        Canvas(Modifier.size(7.5.dp)) { drawCircle(primaryColor.copy(alpha = 0.50f)) }
        Spacer(Modifier.width(5.dp))
        Canvas(Modifier.size(5.dp))  { drawCircle(primaryColor.copy(alpha = 0.32f)) }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .weight(1f)
                .height(0.8.dp)
                .background(Brush.horizontalGradient(listOf(lineColor, Color.Transparent)))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  ComingSoonDialog  — world-class, premium, Islamic identity
//
// Perubahan fundamental dari versi sebelumnya:
//
//   أَسْفًا  →  قريباً
//     Sebelumnya: ekspresi penyesalan (semantik salah untuk "coming soon")
//     Sekarang: "segera" / "akan datang" — tepat secara semantik dan konteks
//
//   Emoji bounce (0.8→1.1f tween 600ms) → Islamic ornament Canvas + float anim
//     Sebelumnya: terasa toy-like, tidak mencerminkan identitas Islamic
//     Sekarang: ornamen geometris Islamic yang digambar via Canvas dengan
//     float ±3dp (2700ms) + outer ring rotation (20000ms) — breathing, elegan
//
//   Semua elemen fade bersamaan → 6-stage orchestrated stagger
//     Sebelumnya: tidak ada urutan, efeknya flat
//     Sekarang: dialog scale-in → ornament zone → qariiban → title →
//               description → button — setiap stage tertunda 120-160ms
//
//   Solid primary button → Gradient button (emerald → gold)
//     Sebelumnya: tidak mencerminkan gold palette yang ada di app
//     Sekarang: horizontal gradient primary→WarningAmber dengan rounded 14dp
//
//   Tidak ada Islamic identity → OrnamentDivider + Islamic star canvas
//     Sekarang: seluruh dialog mencerminkan estetika pesantren secara visual
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ComingSoonDialog(
    title    : String,
    onDismiss: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val isDark  = isAppInDarkTheme()

    // ── 6-stage orchestration ─────────────────────────────────────────────────
    // s0: dialog card spring-in (immediate)
    // s1: ornament zone fade (60ms)
    // s2: قريباً slide+fade (200ms)
    // s3: title slide+fade (340ms)
    // s4: description slide+fade (450ms)
    // s5: button slide+fade (560ms)
    var s0 by remember { mutableStateOf(false) }
    var s1 by remember { mutableStateOf(false) }
    var s2 by remember { mutableStateOf(false) }
    var s3 by remember { mutableStateOf(false) }
    var s4 by remember { mutableStateOf(false) }
    var s5 by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        s0 = true
        delay(60);  s1 = true
        delay(140); s2 = true
        delay(140); s3 = true
        delay(115); s4 = true
        delay(105); s5 = true
    }

    // Dialog card: spring scale-in from 0.88f
    val dialogScale by animateFloatAsState(
        targetValue   = if (s0) 1f else 0.88f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "dialogScale"
    )

    // Ornament zone: simple fade (no offset — it's the hero section)
    val ornamentAlpha by animateFloatAsState(
        targetValue   = if (s1) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label         = "ornamentAlpha"
    )

    // قريباً: fade + slide up from 20dp below
    val arabicAlpha  by animateFloatAsState(
        targetValue   = if (s2) 1f else 0f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label         = "arabicAlpha"
    )
    val arabicOffset by animateFloatAsState(
        targetValue   = if (s2) 0f else 20f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label         = "arabicOffset"
    )

    // Title
    val titleAlpha  by animateFloatAsState(
        targetValue   = if (s3) 1f else 0f,
        animationSpec = tween(340, easing = FastOutSlowInEasing),
        label         = "titleAlpha"
    )
    val titleOffset by animateFloatAsState(
        targetValue   = if (s3) 0f else 16f,
        animationSpec = tween(340, easing = FastOutSlowInEasing),
        label         = "titleOffset"
    )

    // Description
    val descAlpha  by animateFloatAsState(
        targetValue   = if (s4) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label         = "descAlpha"
    )
    val descOffset by animateFloatAsState(
        targetValue   = if (s4) 0f else 14f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label         = "descOffset"
    )

    // Button
    val btnAlpha  by animateFloatAsState(
        targetValue   = if (s5) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label         = "btnAlpha"
    )
    val btnOffset by animateFloatAsState(
        targetValue   = if (s5) 0f else 12f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label         = "btnOffset"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .scale(dialogScale),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(28.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // ── ORNAMENT ZONE ─────────────────────────────────────────
                    // Subtle vertical gradient tint (primary) + Islamic ornament
                    // + Arabic قريباً text in gold
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        primary.copy(alpha = if (isDark) 0.11f else 0.06f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .alpha(ornamentAlpha)
                            .padding(top = 30.dp, bottom = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Islamic geometric ornament (Canvas)
                            IslamicOrnamentCanvas(
                                primaryColor = primary,
                                goldColor    = WarningAmber,
                                modifier     = Modifier.size(96.dp)
                            )

                            // Arabic: قريباً — "soon / will arrive"
                            // Semantik benar untuk coming soon.
                            // FontWeight.Bold + WarningAmber → terkesan khidmat, bukan apologi.
                            Text(
                                text          = "قريباً",
                                fontSize      = 30.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = WarningAmber,
                                textAlign     = TextAlign.Center,
                                letterSpacing = 0.5.sp,
                                modifier      = Modifier
                                    .alpha(arabicAlpha)
                                    .offset(y = arabicOffset.dp)
                            )
                        }
                    }

                    // ── TEXT CONTENT ──────────────────────────────────────────
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Islamic ornament divider — separates hero from content
                        OrnamentDivider(primaryColor = primary, isDark = isDark)

                        Spacer(Modifier.height(18.dp))

                        // Feature title (from caller)
                        Text(
                            text       = title,
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface,
                            textAlign  = TextAlign.Center,
                            lineHeight = 24.sp,
                            modifier   = Modifier
                                .alpha(titleAlpha)
                                .offset(y = titleOffset.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        // Supporting description
                        Text(
                            text       = "Fitur ini sedang disiapkan dengan sepenuh hati" +
                                         "\ndan akan segera hadir untuk Anda.",
                            fontSize   = 13.sp,
                            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                            textAlign  = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier   = Modifier
                                .alpha(descAlpha)
                                .offset(y = descOffset.dp)
                        )

                        Spacer(Modifier.height(22.dp))

                        // ── CTA Button: gradient emerald → gold ──────────────
                        // Menggantikan solid primary button yang tidak mencerminkan
                        // gold palette yang ada di app.
                        // Gradient dari primary (IslamicEmerald) ke WarningAmber (gold).
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .alpha(btnAlpha)
                                .offset(y = btnOffset.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            primary,
                                            WarningAmber.copy(alpha = 0.90f)
                                        )
                                    )
                                )
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text          = "Insya Allah, Siap",
                                color         = Color.White,
                                fontWeight    = FontWeight.Bold,
                                fontSize      = 14.sp,
                                letterSpacing = 0.3.sp
                            )
                        }

                        Spacer(Modifier.height(26.dp))
                    }
                }
            }
        }
    }
}
