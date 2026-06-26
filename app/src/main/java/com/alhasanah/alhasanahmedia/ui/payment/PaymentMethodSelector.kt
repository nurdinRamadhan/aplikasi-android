package com.alhasanah.alhasanahmedia.ui.payment

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import com.alhasanah.alhasanahmedia.data.model.CorePaymentMethod

// ─────────────────────────────────────────────────────────────
// 🎨  BRAND TOKENS — identik dengan ekosistem Wallet
// ─────────────────────────────────────────────────────────────
private val GoldPrimary = Color(0xFFD4A017)
private val GoldLight   = Color(0xFFF0C040)
private val GoldDark    = Color(0xFF9A7A00)
private val GoldGlow    = Color(0x33D4A017)
private val DarkCard    = Color(0xFF1A1A2E)
private val DarkCardBorder = Color(0xFF2A2A45)
private val BlueAccent  = Color(0xFF3B82F6)
private val PurpleAccent= Color(0xFF8B5CF6)
private val GreenAccent = Color(0xFF22C55E)
private val AmberAccent = Color(0xFFF59E0B)

private val GoldGradient = Brush.linearGradient(listOf(GoldDark, GoldPrimary, GoldLight))

// ─────────────────────────────────────────────────────────────
// 📌  METHOD VISUAL META
//     Maps CorePaymentMethod code → icon, accent color, category
// ─────────────────────────────────────────────────────────────
private data class MethodMeta(
    val icon    : ImageVector,
    val color   : Color,
    val category: String
)

private fun methodMeta(code: String): MethodMeta = when (code.lowercase()) {
    "bca_va", "bni_va", "bri_va",
    "mandiri_bill", "permata_va",
    "other_va"      -> MethodMeta(Icons.Outlined.AccountBalance, BlueAccent,   "Transfer Bank")
    "qris"          -> MethodMeta(Icons.Outlined.QrCode2,        PurpleAccent, "QRIS")
    "gopay"         -> MethodMeta(Icons.Outlined.QrCode2,        GreenAccent,  "E-Wallet")
    "shopeepay"     -> MethodMeta(Icons.Outlined.QrCode2,        Color(0xFFEE4D2D), "E-Wallet")
    "dana"          -> MethodMeta(Icons.Outlined.QrCode2,        BlueAccent,   "E-Wallet")
    "ovo"           -> MethodMeta(Icons.Outlined.QrCode2,        PurpleAccent, "E-Wallet")
    "alfamart",
    "indomaret"     -> MethodMeta(Icons.Outlined.Store,          AmberAccent,  "Minimarket")
    "credit_card"   -> MethodMeta(Icons.Outlined.CreditCard,     GoldPrimary,  "Kartu Kredit")
    else            -> MethodMeta(Icons.Outlined.Payments,       GoldPrimary,  "Lainnya")
}

// ─────────────────────────────────────────────────────────────
// 🚀  MAIN COMPOSABLE
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentMethodSelector(
    selected   : CorePaymentMethod,
    onSelected : (CorePaymentMethod) -> Unit,
    modifier   : Modifier = Modifier,
    methods    : List<CorePaymentMethod> = CorePaymentMethod.entries,
    embedded   : Boolean = false
) {
    val isDark = isSystemInDarkTheme()

    // Group methods by category for visual grouping
    val grouped = remember(methods) {
        methods.groupBy { methodMeta(it.code).category }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                when {
                    embedded -> SolidColor(Color.Transparent)
                    isDark -> Brush.linearGradient(listOf(Color(0xFF1E1E35), Color(0xFF12121F)))
                    else -> Brush.linearGradient(listOf(Color(0xFFFDF6DC), Color(0xFFFFFBF0)))
                }
            )
            .border(
                if (embedded) 0.dp else 1.dp,
                if (embedded) SolidColor(Color.Transparent) else Brush.linearGradient(listOf(
                    GoldPrimary.copy(if (isDark) .3f else .2f),
                    GoldLight.copy(if (isDark) .15f else .1f),
                    GoldPrimary.copy(if (isDark) .3f else .2f)
                )),
                RoundedCornerShape(22.dp)
            )
    ) {
        // Background glow (dark only)
        if (isDark && !embedded) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-20).dp)
                    .background(
                        Brush.radialGradient(listOf(GoldGlow, Color.Transparent)),
                        CircleShape
                    )
            )
        }

        Column(
            modifier = Modifier.padding(if (embedded) 0.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (embedded) 12.dp else 16.dp)
        ) {
            // ── HEADER ──────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(GoldPrimary.copy(.15f), RoundedCornerShape(11.dp))
                            .border(1.dp, GoldPrimary.copy(.3f), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Payments, null,
                            tint = GoldPrimary, modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Metode Pembayaran",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            "${methods.size} metode tersedia",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) GoldLight.copy(.55f) else GoldDark.copy(.7f)
                            )
                        )
                    }
                }

                // Selected badge
                Surface(
                    shape = CircleShape,
                    color = GoldPrimary.copy(.14f)
                ) {
                    Text(
                        selected.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Gold separator
            Box(
                Modifier.fillMaxWidth().height(1.dp)
                    .background(GoldPrimary.copy(if (isDark) .18f else .12f))
            )

            // ── SELECTED METHOD HELPER TEXT ──────────────────
            AnimatedContent(
                targetState = selected.helper,
                label       = "helperText",
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }
            ) { helper ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = methodMeta(selected.code).color.copy(.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            methodMeta(selected.code).icon, null,
                            tint = methodMeta(selected.code).color,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            helper,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDark)
                                    methodMeta(selected.code).color.copy(.9f)
                                else methodMeta(selected.code).color,
                                lineHeight = 16.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── METHOD GRID (grouped) ────────────────────────
            grouped.entries.forEachIndexed { groupIdx, (category, groupMethods) ->
                if (grouped.size > 1) {
                    // Category label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(3.dp).height(12.dp)
                                .background(GoldGradient, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            category.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight    = FontWeight.Black,
                                color = if (isDark) Color.White.copy(.45f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                if (embedded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        groupMethods.forEach { method ->
                            MethodRowCard(
                                method = method,
                                selected = method == selected,
                                isDark = isDark,
                                onClick = { onSelected(method) }
                            )
                        }
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupMethods.forEach { method ->
                            MethodPill(
                                method   = method,
                                selected = method == selected,
                                isDark   = isDark,
                                onClick  = { onSelected(method) }
                            )
                        }
                    }
                }

                if (groupIdx < grouped.size - 1) {
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(GoldPrimary.copy(if (isDark) .1f else .07f))
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentMethodPickerDialog(
    selected: CorePaymentMethod,
    onSelected: (CorePaymentMethod) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = "Pilih Metode Pembayaran",
    confirmText: String = "Lanjutkan Pembayaran",
    methods: List<CorePaymentMethod> = CorePaymentMethod.entries
) {
    val isDark = isSystemInDarkTheme()
    val dialogColor = if (isDark) DarkCard else MaterialTheme.colorScheme.surface
    val footerBottomPadding = maxOf(
        24.dp,
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 18.dp
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = dialogColor,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(GoldPrimary.copy(.16f), RoundedCornerShape(13.dp))
                                .border(1.dp, GoldPrimary.copy(.28f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Payments,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Geser untuk memilih metode yang tersedia",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDark) Color.White.copy(.58f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(.06f) else MaterialTheme.colorScheme.surfaceVariant.copy(.7f))
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Tutup",
                            tint = if (isDark) Color.White.copy(.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(
                    color = if (isDark) DarkCardBorder else MaterialTheme.colorScheme.outlineVariant.copy(.55f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    PaymentMethodSelector(
                        selected = selected,
                        onSelected = onSelected,
                        methods = methods,
                        embedded = true
                    )
                }
                HorizontalDivider(
                    color = if (isDark) DarkCardBorder else MaterialTheme.colorScheme.outlineVariant.copy(.55f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(dialogColor)
                        .padding(horizontal = 20.dp)
                        .padding(top = 14.dp, bottom = footerBottomPadding),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1.25f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(confirmText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodRowCard(
    method: CorePaymentMethod,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val meta = methodMeta(method.code)
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(
                if (selected) meta.color.copy(if (isDark) .18f else .12f)
                else if (isDark) Color.White.copy(.045f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(.42f)
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) meta.color.copy(.72f)
                        else if (isDark) DarkCardBorder
                        else MaterialTheme.colorScheme.outlineVariant.copy(.6f),
                shape = shape
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(meta.color.copy(if (selected) .22f else .12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(meta.icon, contentDescription = null, tint = meta.color, modifier = Modifier.size(22.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    method.label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = if (selected) meta.color
                                else if (isDark) Color.White.copy(.9f)
                                else MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    method.helper,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) Color.White.copy(.58f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = meta.color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 💊  METHOD PILL — single selectable chip
// ─────────────────────────────────────────────────────────────

@Composable
private fun MethodPill(
    method  : CorePaymentMethod,
    selected: Boolean,
    isDark  : Boolean,
    onClick : () -> Unit
) {
    val meta = methodMeta(method.code)

    // Animated scale on selection
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.97f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pillScale"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    Brush.linearGradient(listOf(
                        meta.color.copy(.18f), meta.color.copy(.10f)
                    ))
                } else {
                    Brush.linearGradient(listOf(
                        if (isDark) Color.White.copy(.05f) else Color.White.copy(.7f),
                        if (isDark) Color.White.copy(.03f) else Color.White.copy(.5f)
                    ))
                }
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                brush = if (selected) {
                    Brush.linearGradient(listOf(meta.color.copy(.7f), meta.color.copy(.4f)))
                } else {
                    Brush.linearGradient(listOf(
                        GoldPrimary.copy(if (isDark) .2f else .15f),
                        GoldPrimary.copy(if (isDark) .1f else .08f)
                    ))
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        if (selected) meta.color.copy(.2f) else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    meta.icon, null,
                    tint = if (selected) meta.color
                           else if (isDark) GoldLight.copy(.6f) else GoldDark.copy(.7f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Label
            Text(
                method.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (selected) meta.color
                            else if (isDark) Color.White.copy(.75f)
                            else MaterialTheme.colorScheme.onSurface.copy(.8f)
                )
            )

            // Check icon when selected
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit  = scaleOut() + fadeOut()
            ) {
                Icon(
                    Icons.Outlined.CheckCircle, null,
                    tint = meta.color,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 🔧  HELPERS
// ─────────────────────────────────────────────────────────────

@Composable
private fun isSystemInDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.05f
