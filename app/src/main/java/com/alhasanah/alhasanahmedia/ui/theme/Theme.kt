package com.alhasanah.alhasanahmedia.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.alhasanah.alhasanahmedia.util.AlhasanahAmber
import com.alhasanah.alhasanahmedia.util.AlhasanahCream
import com.alhasanah.alhasanahmedia.util.AlhasanahGold
import com.alhasanah.alhasanahmedia.util.AlhasanahGoldBright
import com.alhasanah.alhasanahmedia.util.AlhasanahGoldDeep
import com.alhasanah.alhasanahmedia.util.AlhasanahGoldPale
import com.alhasanah.alhasanahmedia.util.AlhasanahGoldTint
import com.alhasanah.alhasanahmedia.util.AlhasanahOffWhite
import com.alhasanah.alhasanahmedia.util.AlhasanahWhite
import com.alhasanah.alhasanahmedia.util.WarmDarkSurface
import com.alhasanah.alhasanahmedia.util.WarmDarkVariant
import com.alhasanah.alhasanahmedia.util.WarmNearBlack

// ═══════════════════════════════════════════════════════════════════════════
//  AL-HASANAH MEDIA — Theme
//  Brand  : Putih Bersih & Kuning Emas
//  Design : Warm, Premium, Timeless Islamic Aesthetic
//
//  CATATAN KONTRAS (WCAG AA):
//  • onPrimary menggunakan warna gelap, BUKAN putih.
//    Gold #C5A44C vs White hanya ~2.4:1 — gagal standar aksesibilitas.
//    Gold #C5A44C vs #1A0E00 mencapai ~9.2:1 — excellent.
//  • Hal yang sama berlaku untuk onSecondary.
// ═══════════════════════════════════════════════════════════════════════════

// ── Shared "on gold" text color (used in both schemes) ──────────────────────
// Ratio: Gold #C5A44C vs #1A0E00 ≈ 9.2:1  ✅ WCAG AAA
private val OnGold = Color(0xFF1A0E00) // Warm near-black — dark text on any gold surface

// ─────────────────────────────────────────────────────────────────────────────
//  DARK COLOR SCHEME
//  Background: Warm near-black (tidak biru/hijau — selaras dengan gold)
//  Primary   : AlhasanahGoldBright (lebih luminous di atas gelap)
//  Secondary : AlhasanahAmber (warm amber, bukan neon yellow)
// ─────────────────────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(

    // ── Primary — Royal Gold (luminous untuk dark background) ────────────
    primary             = AlhasanahGoldBright,          // #D4AF37 — Metallic Gold
    onPrimary           = OnGold,                        // Dark warm on gold ✅
    primaryContainer    = Color(0xFF3A2B0E),             // Deep gold container
    onPrimaryContainer  = Color(0xFFFFE09A),             // Warm cream text on container

    // ── Secondary — Warm Amber (accent / highlight) ───────────────────────
    // Menggantikan neon yellow yang terlalu mencolok & murahan
    secondary           = AlhasanahAmber,               // #E09820 — Warm Amber
    onSecondary         = OnGold,                        // Dark on amber ✅
    secondaryContainer  = Color(0xFF332713),             // Deep amber container
    onSecondaryContainer = Color(0xFFFFDC99),            // Light warm text on container

    // ── Tertiary — Deeper Gold (variasi ketiga) ───────────────────────────
    tertiary            = AlhasanahGold,                // #C5A44C — Royal Gold
    onTertiary          = OnGold,
    tertiaryContainer   = Color(0xFF332813),
    onTertiaryContainer = Color(0xFFFFD580),

    // ── Background & Surface — CHARCOAL / DARK SLATE ─────────────────────
    background          = WarmNearBlack,                // #1F2326 — Charcoal slate
    onBackground        = AlhasanahOffWhite,            // #EDE8DF — Soft warm white

    surface             = WarmDarkSurface,              // #23282B — Dark slate card
    onSurface           = AlhasanahOffWhite,            // #EDE8DF
    surfaceVariant      = WarmDarkVariant,              // #2B3135 — Dark slate variant
    onSurfaceVariant    = Color(0xFFC0C8CA),            // Muted blue-grey

    // ── Borders & Dividers ────────────────────────────────────────────────
    outline             = Color(0xFF5E6A6F),            // Slate outline
    outlineVariant      = Color(0xFF3A4247),            // Subtle slate divider

    // ── Error ─────────────────────────────────────────────────────────────
    error               = Color(0xFFFF8A80),            // Softer red for dark bg
    onError             = Color(0xFF1A0000),
    errorContainer      = Color(0xFF4A0000),
    onErrorContainer    = Color(0xFFFFB4AB),

    // ── Inverse / Scrim ────────────────────────────────────────────────────
    scrim               = Color(0xFF000000),
    inverseSurface      = AlhasanahOffWhite,
    inverseOnSurface    = Color(0xFF1A1510),
    inversePrimary      = AlhasanahGoldDeep,            // Deep gold on light
)

// ─────────────────────────────────────────────────────────────────────────────
//  LIGHT COLOR SCHEME
//  Background: Warm cream (bukan pure white — lebih premium & hangat)
//  Primary   : AlhasanahGold — onPrimary GELAP untuk kontras optimal
//  Surface   : Pure white agar card terlihat mengapung di atas cream
// ─────────────────────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(

    // ── Primary — Royal Gold ──────────────────────────────────────────────
    primary             = AlhasanahGold,                // #C5A44C — Royal Gold
    onPrimary           = OnGold,                        // #1A0E00 — FIXED ✅ (dulu Color.White = gagal WCAG)
    primaryContainer    = AlhasanahGoldPale,            // #F5E6B8 — Pale gold container
    onPrimaryContainer  = Color(0xFF3D2A00),            // Deep gold on pale container

    // ── Secondary — Deep Gold (variasi lebih dalam) ────────────────────────
    secondary           = AlhasanahGoldDeep,            // #8B6914 — Deep Gold
    onSecondary         = AlhasanahWhite,               // White on deep gold (ratio ~5.1:1 ✅)
    secondaryContainer  = Color(0xFFFFE0A0),            // Light amber container
    onSecondaryContainer = Color(0xFF1A0E00),           // Dark on light container

    // ── Tertiary — Warm Amber Accent ──────────────────────────────────────
    tertiary            = Color(0xFF8B5E00),            // Darker amber-bronze
    onTertiary          = AlhasanahWhite,
    tertiaryContainer   = Color(0xFFFFDDB0),
    onTertiaryContainer = Color(0xFF1A0E00),

    // ── Background & Surface ──────────────────────────────────────────────
    // Cream background membuat card putih terlihat "mengapung" — efek premium
    background          = AlhasanahCream,               // #FDF8F0 — Warm cream
    onBackground        = Color(0xFF1A1208),            // Warm dark brown text

    surface             = AlhasanahWhite,               // #FFFFFF — Pure white card
    onSurface           = Color(0xFF1A1208),            // Warm dark text
    surfaceVariant      = AlhasanahGoldTint,            // #F5EDD8 — Gold-tinted bg
    onSurfaceVariant    = Color(0xFF5C4A2A),            // Warm brown on tinted surface

    // ── Borders & Dividers ────────────────────────────────────────────────
    outline             = Color(0xFF9C7D45),            // Warm gold outline
    outlineVariant      = Color(0xFFDFCDA0),            // Subtle gold divider

    // ── Error ─────────────────────────────────────────────────────────────
    error               = Color(0xFFBA1A1A),
    onError             = AlhasanahWhite,
    errorContainer      = Color(0xFFFFDAD6),
    onErrorContainer    = Color(0xFF410002),

    // ── Inverse / Scrim ────────────────────────────────────────────────────
    scrim               = Color(0xFF000000),
    inverseSurface      = Color(0xFF1A1208),
    inverseOnSurface    = Color(0xFFF8F0E0),
    inversePrimary      = AlhasanahGoldBright,          // Bright gold on dark inverse
)

// ─────────────────────────────────────────────────────────────────────────────
//  AlhasanahMediaTheme — Root Theme Composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AlhasanahMediaTheme(
    darkTheme: Boolean    = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // false = pertahankan brand identity Al-Hasanah
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Edge-to-edge: biarkan konten di balik status bar (modern approach)
            // Header HomeContent sudah memiliki gradient yang menutupi area ini.
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()

            // Icon status bar: putih di dark mode, gelap di light mode
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  PANDUAN PENGGUNAAN WARNA (Quick Reference)
// ─────────────────────────────────────────────────────────────────────────────
//
//  MaterialTheme.colorScheme.primary         → Royal Gold (tombol, icon aktif)
//  MaterialTheme.colorScheme.onPrimary       → Teks di atas tombol emas (gelap)
//  MaterialTheme.colorScheme.primaryContainer → Latar badge/chip gold pale
//  MaterialTheme.colorScheme.secondary       → Amber accent (notifikasi, badge)
//  MaterialTheme.colorScheme.background      → Latar halaman (cream / near-black)
//  MaterialTheme.colorScheme.surface         → Latar kartu (putih / dark warm)
//  MaterialTheme.colorScheme.surfaceVariant  → Latar section / input field
//  MaterialTheme.colorScheme.outlineVariant  → Divider tipis
//  MaterialTheme.colorScheme.outline         → Border / divider normal
//  MaterialTheme.colorScheme.onSurfaceVariant→ Teks subtitle / placeholder
