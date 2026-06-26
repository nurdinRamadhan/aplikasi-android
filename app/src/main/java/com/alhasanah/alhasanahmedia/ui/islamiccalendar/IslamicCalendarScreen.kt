package com.alhasanah.alhasanahmedia.ui.islamiccalendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.islamiccalendar.HolidayItem
import com.alhasanah.alhasanahmedia.data.model.islamiccalendar.SunnahFastItem
import com.alhasanah.alhasanahmedia.data.repository.IslamicCalendarBundle
import com.alhasanah.alhasanahmedia.ui.components.AppGradientBackground
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun IslamicCalendarScreen(
    navController: NavController,
    viewModel: IslamicCalendarViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = isAppInDarkTheme()
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppGradientBackground(isDark = isDark)
        Column(modifier = Modifier.fillMaxSize()) {
            CalendarHeader(
                isDark = isDark,
                onBack = { navController.popBackStack() },
                onRefresh = viewModel::refresh
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.notice != null || state.isOfflineData) {
                    item {
                        NoticeCard(state.notice ?: "Data tersimpan ditampilkan sambil menunggu pembaruan.")
                    }
                }
                when {
                    state.isLoading && state.bundle == null -> item {
                        Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    state.errorMessage != null && state.bundle == null -> item {
                        NoticeCard(state.errorMessage ?: "Kalender tidak tersedia")
                    }
                    state.bundle != null -> {
                        item { TodayCalendarCard(state.bundle!!) }
                        item { MiniCalendarGrid(state.bundle!!) }
                        item { SectionTitle("Puasa Sunnah") }
                        items(state.bundle!!.sunnahFasts) { item -> SunnahFastCard(item) }
                        item { SectionTitle("Libur Nasional") }
                        if (state.bundle!!.holidays.isEmpty()) {
                            item { NoticeCard("Data libur nasional belum tersedia dari API. Kalender utama tetap bisa digunakan offline.") }
                        } else {
                            items(state.bundle!!.holidays.take(12)) { item -> HolidayCard(item) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(isDark: Boolean, onBack: () -> Unit, onRefresh: () -> Unit) {
    AppPageHeader(
        title = "KALENDER ISLAM",
        subtitle = "Hijriah, puasa sunnah, dan libur nasional",
        isDark = isDark,
        onBack = onBack,
        size = AppPageHeaderSize.Compact,
        rightAction = {
            HeaderButton(Icons.Default.Refresh, "Muat ulang", onRefresh)
        }
    )
}

@Composable
private fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.14f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CalendarBackground() {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val color = primary.copy(alpha = 0.024f)
        val spacing = 92.dp.toPx()
        val radius = 13.dp.toPx()
        repeat((size.width / spacing).toInt() + 2) { col ->
            repeat((size.height / spacing).toInt() + 2) { row ->
                val center = Offset(col * spacing, row * spacing + if (col % 2 == 0) spacing / 2 else 0f)
                val path = Path()
                repeat(8) { i ->
                    val angle = Math.PI * 2 * i / 8 - Math.PI / 2
                    val x = center.x + kotlin.math.cos(angle).toFloat() * radius
                    val y = center.y + kotlin.math.sin(angle).toFloat() * radius
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color)
            }
        }
    }
}


@Composable
private fun TodayCalendarCard(bundle: IslamicCalendarBundle) {
    val calendar = bundle.calendar
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(calendar.hijr.today, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(calendar.ce.today, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Metode ${calendar.method} • penyesuaian ${calendar.adjustment}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MiniCalendarGrid(bundle: IslamicCalendarBundle) {
    val calendar = bundle.calendar
    val currentDay = calendar.hijr.day.coerceAtLeast(1)
    val startDay = (currentDay - 3).coerceAtLeast(1)
    val days = (startDay until startDay + 7).toList()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(calendar.hijr.monthName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${calendar.hijr.year} H", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.forEach { day ->
                    val selected = day == currentDay
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(74.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.44f else 0.10f))
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(day.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            Text(if (selected) "Hari ini" else "Hijri", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text(
                text = "Kalender Hijriah mengikuti metode ${calendar.method}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
}

@Composable
private fun SunnahFastCard(item: SunnahFastItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (item.activeToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HolidayCard(item: HolidayItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(item.displayDate.ifBlank { "Tanggal mengikuti sumber API" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NoticeCard(message: String) {
    val color = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Text(message, modifier = Modifier.padding(14.dp), color = color, style = MaterialTheme.typography.bodyMedium)
    }
}
