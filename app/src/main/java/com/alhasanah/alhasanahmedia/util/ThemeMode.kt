package com.alhasanah.alhasanahmedia.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance

@Composable
fun isAppInDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background.luminance() < 0.5f
}
