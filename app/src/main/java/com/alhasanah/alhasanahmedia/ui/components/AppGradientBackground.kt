package com.alhasanah.alhasanahmedia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.alhasanah.alhasanahmedia.R

@Composable
fun AppGradientBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val brush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF1B1E21),
                Color(0xFF1F2326),
                Color(0xFF23282B)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFFFEFC),
                Color(0xFFF8F4EC)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
    )
}

@Composable
fun AppSolidBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (isDark) {
        Image(
            painter = painterResource(id = R.drawable.solid),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    } else {
        AppGradientBackground(isDark = false, modifier = modifier)
    }
}

fun appPanelColor(isDark: Boolean): Color =
    if (isDark) Color(0xE623282B) else Color(0xF7FFFFFF)

fun appPanelVariantColor(isDark: Boolean): Color =
    if (isDark) Color(0xCC2B3135) else Color(0x6BF5E6B8)

fun appPanelBorderColor(isDark: Boolean): Color =
    if (isDark) Color(0x995E6A6F) else Color(0xB8DFCDA0)
