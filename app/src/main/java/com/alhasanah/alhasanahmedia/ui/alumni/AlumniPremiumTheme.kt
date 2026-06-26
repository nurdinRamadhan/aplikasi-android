package com.alhasanah.alhasanahmedia.ui.alumni

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun AlumniPremiumTheme(content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val isDark = base.surface.luminance() < 0.5f
    val colors = if (isDark) {
        base.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF0B0B0B),
            onSurface = Color(0xFFF5F5F5),
            onSurfaceVariant = Color(0xFFB8B8B8),
            outlineVariant = Color(0xFF1C1C1C),
            inverseSurface = Color(0xFFEDEDED),
            inverseOnSurface = Color(0xFF111111)
        )
    } else {
        base
    }

    MaterialTheme(colorScheme = colors, content = content)
}
