package com.alhasanah.alhasanahmedia.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alhasanah.alhasanahmedia.data.model.Announcement
import com.alhasanah.alhasanahmedia.util.WarningAmber
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// AnnouncementDialog — overlay dialog untuk pengumuman
//
// Mengikuti pola UI/UX ComingSoonDialog:
//   • 6-stage orchestrated stagger animation
//   • Same color scheme (primary, WarningAmber, onSurface)
//   • Same card shape (RoundedCornerShape(28.dp))
//   • Same button gradient (primary → WarningAmber)
//   • OrnamentDivider reuse
//
// Fitur:
//   • Single announcement: tampilkan langsung
//   • Multiple announcements: horizontal scroll dengan page indicators
//   • Dynamic content dari Announcement model
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnnouncementDialog(
    announcements: List<Announcement>,
    onDismiss: () -> Unit
) {
    if (announcements.isEmpty()) return

    val primary = MaterialTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()

    // ── 6-stage orchestration (same as ComingSoonDialog) ──────────────────────
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
        targetValue = if (s0) 1f else 0.88f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "dialogScale"
    )

    // Ornament zone: simple fade
    val ornamentAlpha by animateFloatAsState(
        targetValue = if (s1) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "ornamentAlpha"
    )

    // Pengumuman label: fade + slide up
    val labelAlpha by animateFloatAsState(
        targetValue = if (s2) 1f else 0f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "labelAlpha"
    )
    val labelOffset by animateFloatAsState(
        targetValue = if (s2) 0f else 20f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "labelOffset"
    )

    // Title
    val titleAlpha by animateFloatAsState(
        targetValue = if (s3) 1f else 0f,
        animationSpec = tween(340, easing = FastOutSlowInEasing),
        label = "titleAlpha"
    )
    val titleOffset by animateFloatAsState(
        targetValue = if (s3) 0f else 16f,
        animationSpec = tween(340, easing = FastOutSlowInEasing),
        label = "titleOffset"
    )

    // Description
    val descAlpha by animateFloatAsState(
        targetValue = if (s4) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "descAlpha"
    )
    val descOffset by animateFloatAsState(
        targetValue = if (s4) 0f else 14f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "descOffset"
    )

    // Button
    val btnAlpha by animateFloatAsState(
        targetValue = if (s5) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "btnAlpha"
    )
    val btnOffset by animateFloatAsState(
        targetValue = if (s5) 0f else 12f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "btnOffset"
    )

    // Horizontal scroll state for multiple announcements
    val scrollState = rememberScrollState()
    var currentPage by remember { mutableIntStateOf(0) }

    // Track scroll position to update currentPage
    LaunchedEffect(scrollState.value) {
        if (announcements.size > 1) {
            val pageWidth = 280 // approximate width of each page
            currentPage = (scrollState.value + pageWidth / 2) / pageWidth
                .coerceIn(0, announcements.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .scale(dialogScale),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // ── ORNAMENT ZONE ─────────────────────────────────────────
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
                            // Islamic geometric ornament (reuse from ComingSoonDialog style)
                            IslamicAnnouncementOrnament(
                                primaryColor = primary,
                                goldColor = WarningAmber,
                                modifier = Modifier.size(96.dp)
                            )

                            // Arabic: الحسنة — "the good / benefit"
                            Text(
                                text = "الحسنة",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber,
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier
                                    .alpha(labelAlpha)
                                    .offset(y = labelOffset.dp)
                            )
                        }
                    }

                    // ── TEXT CONTENT ──────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Simple divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            primary.copy(alpha = 0.18f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Spacer(Modifier.height(18.dp))

                        // Horizontal scroll for multiple announcements
                        if (announcements.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState)
                            ) {
                                announcements.forEach { announcement ->
                                    AnnouncementContent(
                                        announcement = announcement,
                                        titleAlpha = titleAlpha,
                                        titleOffset = titleOffset,
                                        descAlpha = descAlpha,
                                        descOffset = descOffset,
                                        modifier = Modifier.width(260.dp)
                                    )
                                }
                            }

                            // Page indicators
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(announcements.size) { index ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 3.dp)
                                            .size(if (index == currentPage) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (index == currentPage) primary
                                                else primary.copy(alpha = 0.3f)
                                            )
                                    )
                                }
                            }
                        } else {
                            // Single announcement — no scroll needed
                            AnnouncementContent(
                                announcement = announcements.first(),
                                titleAlpha = titleAlpha,
                                titleOffset = titleOffset,
                                descAlpha = descAlpha,
                                descOffset = descOffset
                            )
                        }

                        Spacer(Modifier.height(22.dp))

                        // ── CTA Button: gradient emerald → gold ──────────────
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
                                text = "Baik, Saya Mengerti",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
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

// ─────────────────────────────────────────────────────────────────────────────
// AnnouncementContent — single announcement content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AnnouncementContent(
    announcement: Announcement,
    titleAlpha: Float,
    titleOffset: Float,
    descAlpha: Float,
    descOffset: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Feature title
        Text(
            text = announcement.title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier
                .alpha(titleAlpha)
                .offset(y = titleOffset.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Body text
        Text(
            text = announcement.body,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier
                .alpha(descAlpha)
                .offset(y = descOffset.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IslamicAnnouncementOrnament — simplified Islamic ornament for announcements
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IslamicAnnouncementOrnament(
    primaryColor: Color,
    goldColor: Color,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "ornament")

    val floatOffset by infinite.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(
        modifier = modifier.offset(y = floatOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.08f))
        )

        // Inner circle
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(goldColor.copy(alpha = 0.10f))
        )

        // Center dot
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.18f))
        )
    }
}
