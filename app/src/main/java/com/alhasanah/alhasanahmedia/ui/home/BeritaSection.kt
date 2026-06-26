package com.alhasanah.alhasanahmedia.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.* 
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alhasanah.alhasanahmedia.data.model.BeritaModel
import com.alhasanah.alhasanahmedia.util.GlassWhite
import com.alhasanah.alhasanahmedia.util.formatStringDate // Add this import

@Composable
fun BeritaSection(
    beritaList: List<BeritaModel>,
    isLoading: Boolean,
    onBeritaClick: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    
    if (isLoading && beritaList.isEmpty()) {
        BeritaShimmer(isDark)
    } else if (beritaList.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INFORMASI DAN KAJIAN",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Lihat semua",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(beritaList) { berita ->
                    BeritaGlassCard(berita = berita, isDark = isDark, onClick = { onBeritaClick(berita.slug) })
                }
            }
        }
    }
}

@Composable
fun BeritaGlassCard(berita: BeritaModel, isDark: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceAlpha = if (isDark) 0.56f else 0.92f

    Card(
        modifier = Modifier
            .width(320.dp)
            .clickable(onClick = onClick)
            .border(1.dp, primary.copy(alpha = if (isDark) 0.42f else 0.30f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = surfaceAlpha)
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .height(178.dp)
                    .fillMaxWidth()
            ) {
                AsyncImage(
                    model = berita.thumbnailUrl,
                    contentDescription = "Thumbnail Berita",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.10f)
                                )
                            )
                        )
                )
                
                berita.kategori?.let {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = primary.copy(alpha = 0.92f)
                    ) {
                        Text(
                            text = it.uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = berita.judul,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                berita.ringkasan?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = primary.copy(alpha = 0.82f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatStringDate(berita.tanggalPublish),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun BeritaShimmer(isDark: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .width(150.dp)
                .height(14.dp)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(3) { 
                ShimmerBeritaCard(isDark)
            }
        }
    }
}

@Composable
private fun ShimmerBeritaCard(isDark: Boolean) {
    Card(
        modifier = Modifier.width(320.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) GlassWhite else Color.White)
    ) {
        Column {
            Box(modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
                .shimmerEffect()) 
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp)
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .shimmerEffect()
                )
            }
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition") // Added label
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "translateAnim" // Added label
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
    background(brush)
}
