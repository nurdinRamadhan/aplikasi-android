package com.alhasanah.alhasanahmedia.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.alhasanah.alhasanahmedia.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppPageHeaderBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopCenter
) {
    val fallbackColor = if (isDark) Color(0xFF1F2326) else Color(0xFFFFFEFC)
    val imageRes = if (isDark) R.drawable.dark_halaman else R.drawable.light_halaman

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fallbackColor)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = alignment
        )
    }
}

enum class AppPageHeaderSize(val height: Dp) {
    Compact(156.dp),
    Standard(184.dp),
    Comfortable(220.dp),
    Large(320.dp)
}

@Composable
fun AppPageHeaderActionButton(
    isDark: Boolean,
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (isDark) Color.White.copy(alpha = 0.88f) else Color(0xFF2B2418)
    val border = if (isDark) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
    val container = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.58f)

    Surface(
        onClick = onClick,
        modifier = modifier.size(46.dp),
        shape = CircleShape,
        color = container,
        border = BorderStroke(1.dp, border)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun AppPageHeader(
    title: String,
    subtitle: String? = null,
    isDark: Boolean,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    size: AppPageHeaderSize = AppPageHeaderSize.Standard,
    titleTopPadding: Dp = 42.dp,
    rightAction: (@Composable BoxScope.() -> Unit)? = null,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val titleColor = if (isDark) Color.White.copy(alpha = 0.92f) else MaterialTheme.colorScheme.primary
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(size.height)
    ) {
        AppPageHeaderBackground(isDark = isDark, modifier = Modifier.matchParentSize())

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp)
        ) {
            if (onBack != null) {
                AppPageHeaderActionButton(
                    isDark = isDark,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            if (rightAction != null) {
                Box(modifier = Modifier.align(Alignment.TopEnd), content = rightAction)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = titleTopPadding, start = 56.dp, end = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = titleColor,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp
                    ),
                    textAlign = TextAlign.Center
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = subtitleColor,
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (content != null) {
            Box(modifier = Modifier.matchParentSize(), content = content)
        }
    }
}
