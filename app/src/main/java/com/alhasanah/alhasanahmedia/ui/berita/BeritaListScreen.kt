package com.alhasanah.alhasanahmedia.ui.berita

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.alhasanah.alhasanahmedia.data.model.BeritaModel
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import com.alhasanah.alhasanahmedia.ui.home.HomeViewModel
import com.alhasanah.alhasanahmedia.util.formatStringDate
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun BeritaListScreen(
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel()
) {
    val beritaList by viewModel.beritaState.collectAsState()
    val isLoading by viewModel.isLoadingBerita.collectAsState()
    val isDark = isAppInDarkTheme()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppSolidBackground(isDark = isDark)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 20.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                BeritaListTopAction(onBack = { navController.popBackStack() })
            }

            if (isLoading && beritaList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                items(beritaList, key = { it.slug }) { berita ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        BeritaListCard(
                            berita = berita,
                            isDark = isDark,
                            onClick = {
                                navController.navigate(Screen.BeritaDetail.createRoute(berita.slug))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BeritaListTopAction(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun BeritaListCard(
    berita: BeritaModel,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.58f else 0.97f))
            .border(1.dp, primary.copy(alpha = if (isDark) 0.34f else 0.28f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 104.dp, height = 88.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = berita.thumbnailUrl,
                contentDescription = berita.judul,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.14f))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = berita.kategori?.uppercase() ?: "BERITA",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = primary,
                    fontWeight = FontWeight.Black
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = berita.judul,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            berita.ringkasan?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = primary.copy(alpha = 0.78f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatStringDate(berita.tanggalPublish),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
