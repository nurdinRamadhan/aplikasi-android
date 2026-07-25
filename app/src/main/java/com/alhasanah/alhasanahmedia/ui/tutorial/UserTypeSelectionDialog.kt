package com.alhasanah.alhasanahmedia.ui.tutorial

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alhasanah.alhasanahmedia.util.IslamicEmerald
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════════════
//  UserTypeSelectionDialog — Dialog pemilihan tipe user (tidak bisa di-cancel)
//  Muncul sekali setelah splash screen untuk menentukan jalur tutorial
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun UserTypeSelectionDialog(
    onWaliSantriSelected: () -> Unit,
    onGuestSelected: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()

    // ── 6-stage orchestrated animation (sama seperti AnnouncementDialog) ──
    var s0 by remember { mutableStateOf(false) }
    var s1 by remember { mutableStateOf(false) }
    var s2 by remember { mutableStateOf(false) }
    var s3 by remember { mutableStateOf(false) }
    var s4 by remember { mutableStateOf(false) }
    var s5 by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        s0 = true
        delay(60); s1 = true
        delay(140); s2 = true
        delay(140); s3 = true
        delay(115); s4 = true
        delay(105); s5 = true
    }

    val dialogScale by animateFloatAsState(
        targetValue = if (s0) 1f else 0.88f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "dialogScale"
    )

    val ornamentAlpha by animateFloatAsState(
        targetValue = if (s1) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "ornamentAlpha"
    )

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

    // Hover effect state
    var hoveredOption by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { /* Tidak bisa di-cancel */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
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

                    // ── ORNAMENT ZONE ────────────────────────────────────
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
                            // Islamic geometric ornament
                            TutorialOrnament(
                                primaryColor = primary,
                                goldColor = IslamicEmerald,
                                modifier = Modifier.size(96.dp)
                            )

                            Text(
                                text = "الحسنة",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = IslamicEmerald,
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier
                                    .alpha(labelAlpha)
                                    .offset(y = labelOffset.dp)
                            )
                        }
                    }

                    // ── TEXT CONTENT ──────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Divider
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

                        // Title
                        Text(
                            text = "Selamat Datang",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .alpha(titleAlpha)
                                .offset(y = titleOffset.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        // Description
                        Text(
                            text = "Siapa Anda? Pilih peran untuk mendapatkan pengalaman yang sesuai.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .alpha(descAlpha)
                                .offset(y = descOffset.dp)
                        )

                        Spacer(Modifier.height(22.dp))

                        // ── OPTION CARDS ─────────────────────────────────
                        UserTypeOption(
                            icon = Icons.Outlined.FamilyRestroom,
                            title = "Saya Wali Santri",
                            subtitle = "Akses profil, absensi, hafalan & keuangan santri",
                            isHovered = hoveredOption == "wali_santri",
                            isDark = isDark,
                            alpha = btnAlpha,
                            offset = btnOffset,
                            onClick = { onWaliSantriSelected() },
                            onHover = { hoveredOption = "wali_santri" }
                        )

                        Spacer(Modifier.height(12.dp))

                        UserTypeOption(
                            icon = Icons.Outlined.Person,
                            title = "Saya Tamu",
                            subtitle = "Jelajahi fitur publik aplikasi",
                            isHovered = hoveredOption == "guest",
                            isDark = isDark,
                            alpha = btnAlpha,
                            offset = btnOffset,
                            onClick = { onGuestSelected() },
                            onHover = { hoveredOption = "guest" }
                        )

                        Spacer(Modifier.height(26.dp))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  UserTypeOption — Card pilihan user dengan hover effect
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun UserTypeOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isHovered: Boolean,
    isDark: Boolean,
    alpha: Float,
    offset: Float,
    onClick: () -> Unit,
    onHover: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderColor = when {
        isHovered -> primary
        isDark -> primary.copy(alpha = 0.20f)
        else -> primary.copy(alpha = 0.25f)
    }
    val backgroundAlpha = if (isHovered) 0.12f else 0.04f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .offset(y = offset.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isHovered) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .background(primary.copy(alpha = backgroundAlpha))
            .clickable {
                onHover()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) IslamicEmerald.copy(alpha = 0.18f)
                        else IslamicEmerald.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = IslamicEmerald,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                    lineHeight = 16.sp
                )
            }

            // Arrow
            Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = primary.copy(alpha = if (isHovered) 1f else 0.40f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  TutorialOrnament — Islamic geometric ornament untuk dialog
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun TutorialOrnament(
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
                .background(goldColor.copy(alpha = 0.12f))
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
