package com.alhasanah.alhasanahmedia.ui.laporanmasalah

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhasanah.alhasanahmedia.data.model.LaporanMasalah
import com.alhasanah.alhasanahmedia.data.model.LaporanStatus
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme

private val LuxuryGold = Color(0xFFD4A853)
private val LuxuryGoldLight = Color(0xFFECC96B)
private val StatusOpen = Color(0xFF3B82F6)
private val StatusInProgress = Color(0xFFF59E0B)
private val StatusFixed = Color(0xFF10B981)
private val StatusRejected = Color(0xFFEF4444)

private data class LaporanColors(
    val background: Color,
    val card: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
)

private val DarkColors = LaporanColors(
    background = Color(0xFF0F0D0A),
    card = Color(0xFF1E1A14),
    surfaceVariant = Color(0xFF232019),
    textPrimary = Color(0xFFF5F0E8),
    textSecondary = Color(0xFFBDB5A8),
    textTertiary = Color(0xFF8A8078),
    border = Color(0xFF3A352E),
)

private val LightColors = LaporanColors(
    background = Color(0xFFFDF9F3),
    card = Color.White,
    surfaceVariant = Color(0xFFF5F0E8),
    textPrimary = Color(0xFF1A1208),
    textSecondary = Color(0xFF6B5A48),
    textTertiary = Color(0xFF9A7535),
    border = Color(0xFFE8DCC8),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanDetailScreen(laporan: LaporanMasalah, onBack: () -> Unit) {
    val isDark = isAppInDarkTheme()
    val colors = if (isDark) DarkColors else LightColors

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Detail Laporan", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isDark) AppSolidBackground(isDark = true)
            LaporanDetailContent(isDark = isDark, colors = colors, laporan = laporan)
        }
    }
}

@Composable
private fun LaporanDetailContent(isDark: Boolean, colors: LaporanColors, laporan: LaporanMasalah) {
    val statusColor = when (laporan.status) {
        "OPEN" -> StatusOpen; "IN_PROGRESS" -> StatusInProgress; "FIXED" -> StatusFixed; "REJECTED" -> StatusRejected; else -> Color.Gray
    }
    val statusLabel = when (laporan.status) {
        "OPEN" -> LaporanStatus.OPEN.label; "IN_PROGRESS" -> LaporanStatus.IN_PROGRESS.label; "FIXED" -> LaporanStatus.FIXED.label; "REJECTED" -> LaporanStatus.REJECTED.label; "NEED_INFO" -> LaporanStatus.NEED_INFO.label; else -> laporan.status
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Status + ID
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(alpha = if (isDark) 0.2f else 0.1f)) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                        Text(statusLabel, style = MaterialTheme.typography.labelMedium.copy(color = statusColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp))
                    }
                }
                Text("ID: #${laporan.id?.take(8)?.uppercase() ?: "UNKNOWN"}", style = MaterialTheme.typography.labelSmall.copy(color = colors.textTertiary, fontSize = 12.sp))
            }
        }

        // Kategori badge
        item {
            Surface(modifier = Modifier.padding(horizontal = 20.dp), shape = RoundedCornerShape(8.dp), color = if (isDark) Color(0xFF2A2318) else Color(0xFFFFF7E6)) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(when (laporan.kategori) { "BUG" -> Icons.Outlined.BugReport; "FITUR" -> Icons.Outlined.Lightbulb; "PERTANYAAN" -> Icons.Outlined.HelpOutline; else -> Icons.Outlined.Chat }, null, tint = if (isDark) LuxuryGoldLight else Color(0xFF8B6914), modifier = Modifier.size(14.dp))
                    Text(laporan.kategori, style = MaterialTheme.typography.labelSmall.copy(color = if (isDark) LuxuryGoldLight else Color(0xFF8B6914), fontWeight = FontWeight.SemiBold, fontSize = 11.sp))
                }
            }
        }

        // Judul + Deskripsi
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = colors.card), elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(laporan.judul, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 20.sp, lineHeight = 28.sp))
                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)
                    Text(laporan.deskripsi, style = MaterialTheme.typography.bodyLarge.copy(color = colors.textSecondary, lineHeight = 26.sp, fontSize = 15.sp))
                }
            }
        }

        // Info Device
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = colors.card), elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (laporan.createdAt != null) InfoRow(isDark, colors, Icons.Outlined.CalendarToday, formatTimestamp(laporan.createdAt))
                    if (laporan.deviceBrand != null || laporan.deviceModel != null) InfoRow(isDark, colors, Icons.Outlined.PhoneAndroid, listOfNotNull(laporan.deviceBrand, laporan.deviceModel).joinToString(" "))
                    if (laporan.androidVersion != null) InfoRow(isDark, colors, Icons.Outlined.Android, "Android ${laporan.androidVersion}${laporan.deviceSdk?.let { " (API $it)" } ?: ""}")
                    if (laporan.appVersion != null) InfoRow(isDark, colors, Icons.Outlined.Info, "Al-Hasanah Media v${laporan.appVersion}")
                }
            }
        }

        // Riwayat Status
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = colors.card), elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Riwayat Status", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 16.sp))
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusTimeline(isDark = isDark, colors = colors, currentStatus = laporan.status ?: "OPEN", createdAt = laporan.createdAt)
                }
            }
        }

        // Quote
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF2A2318) else Color(0xFFFFF7E6))) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.FormatQuote, null, tint = LuxuryGold, modifier = Modifier.size(24.dp))
                    Text("Terima kasih telah membantu kami menjadi lebih baik. Laporan Anda sangat berarti bagi kami.", style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF4A3728), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, lineHeight = 22.sp, fontSize = 14.sp))
                }
            }
        }

        // Catatan Admin
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = colors.card), elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Catatan (Admin)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 16.sp))
                    if (laporan.adminNote != null) {
                        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = if (isDark) Color(0xFF0D1A12) else Color(0xFFF0FDF4)) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.Comment, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                Text(laporan.adminNote, style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary, lineHeight = 22.sp, fontSize = 14.sp))
                            }
                        }
                    } else {
                        Text("Catatan akan muncul di sini jika ada update dari admin.", style = MaterialTheme.typography.bodyMedium.copy(color = colors.textTertiary.copy(alpha = 0.5f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 14.sp))
                    }
                }
            }
        }

    }
}

@Composable
private fun InfoRow(isDark: Boolean, colors: LaporanColors, icon: ImageVector, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = colors.textTertiary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary, fontSize = 14.sp))
    }
}

@Composable
private fun StatusTimeline(isDark: Boolean, colors: LaporanColors, currentStatus: String, createdAt: String?) {
    val allStatuses = listOf(
        Triple("OPEN", "Baru", "Laporan sedang menunggu peninjauan"),
        Triple("IN_PROGRESS", "Sedang Ditinjau", "Laporan sedang ditinjau oleh tim kami"),
        Triple("PROCESSING", "Dalam Pengerjaan", "Laporan sedang dalam proses perbaikan"),
        Triple("FIXED", "Selesai", "Masalah telah diperbaiki")
    )
    val statusOrder = mapOf("OPEN" to 0, "IN_PROGRESS" to 1, "PROCESSING" to 2, "FIXED" to 3, "REJECTED" to 3)
    val currentIndex = statusOrder[currentStatus] ?: 0

    Column(modifier = Modifier.fillMaxWidth()) {
        allStatuses.forEachIndexed { index, (statusKey, title, description) ->
            val isCompleted = index < currentIndex
            val isCurrent = index == currentIndex
            val dotColor = when { isCompleted -> Color(0xFF10B981); isCurrent -> StatusOpen; else -> Color(0xFFD1D5DB) }
            val textColor = when { isCompleted || isCurrent -> colors.textPrimary; else -> colors.textTertiary.copy(alpha = 0.5f) }
            val descColor = when { isCompleted || isCurrent -> colors.textSecondary; else -> colors.textTertiary.copy(alpha = 0.4f) }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp)) {
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(dotColor).then(if (isCurrent) Modifier.border(3.dp, dotColor.copy(alpha = 0.3f), CircleShape) else Modifier))
                    if (index < allStatuses.size - 1) {
                        Box(modifier = Modifier.width(2.dp).height(40.dp).background(if (isCompleted) Color(0xFF10B981).copy(alpha = 0.5f) else colors.border.copy(alpha = 0.5f)))
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(bottom = if (index < allStatuses.size - 1) 8.dp else 0.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall.copy(color = textColor, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold, fontSize = 14.sp))
                    Text(description, style = MaterialTheme.typography.bodySmall.copy(color = descColor, fontSize = 12.sp, lineHeight = 16.sp))
                    Text(if (isCompleted || isCurrent) (createdAt?.let { formatTimestamp(it) } ?: "-") else "-", style = MaterialTheme.typography.labelSmall.copy(color = colors.textTertiary.copy(alpha = 0.5f), fontSize = 11.sp))
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val input = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val output = java.text.SimpleDateFormat("dd MMM yyyy · HH:mm", java.util.Locale("id", "ID"))
        val date = input.parse(timestamp.take(19))
        date?.let { output.format(it) } ?: timestamp
    } catch (e: Exception) {
        timestamp.take(16).replace("T", " ")
    }
}
