package com.alhasanah.alhasanahmedia.util

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════
//  AL-HASANAH MEDIA — Brand Color Palette
//  Brand Identity : Putih Bersih & Kuning Emas (White & Royal Gold)
//  Philosophy     : Warm, Timeless, Premium Islamic Aesthetic
// ═══════════════════════════════════════════════════════════════════════════

// ── Core Gold Family ────────────────────────────────────────────────────────
// Digunakan sebagai primary brand color di seluruh aplikasi

/** Royal Gold — warna utama brand, digunakan di light & dark primary */
val AlhasanahGold       = Color(0xFFC5A44C)

/** Luminous Gold — lebih cerah, optimal sebagai primary di atas dark background */
val AlhasanahGoldBright = Color(0xFFD4AF37)

/** Pale Gold — untuk primaryContainer / surface tint di light mode */
val AlhasanahGoldPale   = Color(0xFFF5E6B8)

/** Deep Gold — untuk primaryContainer di dark mode & secondary di light */
val AlhasanahGoldDeep   = Color(0xFF8B6914)

/** Warm Amber — secondary accent, hangat & elegan (pengganti neon yellow) */
val AlhasanahAmber      = Color(0xFFE09820)


// ── White Family ─────────────────────────────────────────────────────────────
// Digunakan sebagai background & surface

/** Pure White — surface card / dialog */
val AlhasanahWhite      = Color(0xFFFFFFFF)

/** Warm Cream — background utama light mode, terasa hangat & premium */
val AlhasanahCream      = Color(0xFFFDF8F0)

/** Gold-Tinted Surface — surfaceVariant light mode */
val AlhasanahGoldTint   = Color(0xFFF5EDD8)

/** Off-White Text — onBackground di dark mode, tidak "hard" seperti pure white */
val AlhasanahOffWhite   = Color(0xFFEDE8DF)


// ── Dark Background Family ────────────────────────────────────────────────────
// Charcoal / dark slate — menyatu dengan aset header dark mode.

/** Dark slate base — background dark mode utama */
val WarmNearBlack       = Color(0xFF1F2326)

/** Dark slate surface — surface card di dark mode */
val WarmDarkSurface     = Color(0xFF23282B)

/** Dark slate variant — surfaceVariant di dark mode */
val WarmDarkVariant     = Color(0xFF2B3135)

/** Legacy alias — dipertahankan untuk kompatibilitas kode lain */
val CyberDark           = WarmNearBlack


// ── Semantic / Status Colors ───────────────────────────────────────────────────

/** Approved / Success */
val StatusApproved        = Color(0xFF2E7D32)
val StatusApprovedLight   = Color(0xFFB8F5BA)

/** Pending / Warning */
val StatusPending         = Color(0xFFF9A825)
val StatusPendingLight    = Color(0xFFFFECB3)

/** Error / Danger */
val WarningCrimson        = Color(0xFFBA1A1A)
val WarningCrimsonLight   = Color(0xFFFFDAD6)
val HealthRose            = Color(0xFFFF5252) // backward compat

/** Info */
val StatusInfo            = Color(0xFF0277BD)


// ── Glassmorphism Utilities ────────────────────────────────────────────────────

/** Semi-transparent white overlay — untuk glass card effect */
val GlassWhite            = Color(0x1AFFFFFF)

/** Semi-transparent dark overlay — untuk scrim/dimming */
val GlassDark             = Color(0x4D000000)


// ── Santri Activity Chip Colors ────────────────────────────────────────────────

val MumtazChipColor       = Color(0xFF1B5E20)
val JayyidChipColor       = Color(0xFF0D47A1)
val StatusApprovedChipColor = Color(0xFF2E7D32)
val StatusPendingChipColor  = Color(0xFFF9A825)


// ── Islamic Accent ─────────────────────────────────────────────────────────────
// Untuk elemen dekoratif — gunakan sparingly

val IslamicEmerald        = Color(0xFF00897B)
val LightGrayBackground   = Color(0xFFF0F4F8)


// ═══════════════════════════════════════════════════════════════════════════
//  DEPRECATED — Akan dihapus di versi mendatang
//  Gunakan padanan baru di atas
// ═══════════════════════════════════════════════════════════════════════════

@Deprecated("Terlalu neon untuk brand premium. Gunakan AlhasanahAmber.", ReplaceWith("AlhasanahAmber"))
val AlhasanahYellow     = Color(0xFFFFD600)

@Deprecated("Gunakan AlhasanahGoldBright.", ReplaceWith("AlhasanahGoldBright"))
val AlhasanahGoldLight  = AlhasanahGoldBright

@Deprecated("Gunakan WarmNearBlack untuk konsistensi brand.", ReplaceWith("WarmNearBlack"))
val GoldGlow            = Color(0xFFFFD600)

@Deprecated("Gunakan StatusInfo.", ReplaceWith("StatusInfo"))
val FinanceBlue         = Color(0xFF00B0FF)
val FinanceGold         = Color(0xFFFFD600)

@Deprecated("Gunakan IslamicEmerald.", ReplaceWith("IslamicEmerald"))
val FuturisticTeal      = Color(0xFF00E5FF)
val MedicalTeal         = Color(0xFF00BFA5)

@Deprecated("Gunakan WarningCrimson.", ReplaceWith("WarningCrimson"))
val WarningAmber        = Color(0xFFFFD740)
