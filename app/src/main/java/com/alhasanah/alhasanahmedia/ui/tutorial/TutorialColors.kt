package com.alhasanah.alhasanahmedia.ui.tutorial

import androidx.compose.ui.graphics.Color
import com.alhasanah.alhasanahmedia.util.AlhasanahGold
import com.alhasanah.alhasanahmedia.util.AlhasanahGoldBright
import com.alhasanah.alhasanahmedia.util.AlhasanahGoldDeep
import com.alhasanah.alhasanahmedia.util.IslamicEmerald

// ═══════════════════════════════════════════════════════════════════════════
//  TUTORIAL THEME — Gold/Emerald untuk IntroShowcase
//  Mengikuti brand identity Al-Hasanah: warm, premium, Islamic aesthetic
// ═══════════════════════════════════════════════════════════════════════════

object TutorialColors {

    // ── Background Dimming ─────────────────────────────────────────────────
    /** Dark mode: warm near-black dengan sentuhan emas */
    val DarkBackground = Color(0xFF1A0E00)

    /** Light mode: deep warm brown */
    val LightBackground = Color(0xFF3D2A00)

    // ── Target Circle ──────────────────────────────────────────────────────
    /** Lingkaran highlight di sekitar target */
    val DarkTargetCircle = AlhasanahGoldBright   // #D4AF37
    val LightTargetCircle = AlhasanahGold         // #C5A44C

    // ── Tooltip Background ─────────────────────────────────────────────────
    val DarkTooltipBackground = Color(0xFF2A1D0A)
    val LightTooltipBackground = Color(0xFFFFF8ED)

    // ── Tooltip Text ───────────────────────────────────────────────────────
    val DarkTooltipText = Color(0xFFF5E6B8)       // Warm cream on dark
    val LightTooltipText = Color(0xFF3D2A00)       // Deep brown on light

    // ── Tooltip Subtitle ───────────────────────────────────────────────────
    val DarkTooltipSubtitle = Color(0xFFB8A070)
    val LightTooltipSubtitle = Color(0xFF8B6914)

    // ── Accent / Highlight ─────────────────────────────────────────────────
    val Emerald = IslamicEmerald                   // #00897B
    val Gold = AlhasanahGold                       // #C5A44C
    val GoldBright = AlhasanahGoldBright           // #D4AF37
    val GoldDeep = AlhasanahGoldDeep               // #8B6914

    // ── Button Gradient ────────────────────────────────────────────────────
    val DarkButtonStart = AlhasanahGoldBright
    val DarkButtonEnd = IslamicEmerald
    val LightButtonStart = AlhasanahGold
    val LightButtonEnd = IslamicEmerald

    // ── Step Number Badge ──────────────────────────────────────────────────
    val DarkBadgeBackground = IslamicEmerald.copy(alpha = 0.25f)
    val LightBadgeBackground = IslamicEmerald.copy(alpha = 0.15f)
    val DarkBadgeText = AlhasanahGoldBright
    val LightBadgeText = IslamicEmerald
}
