package com.alhasanah.alhasanahmedia.ui.laporanmasalah

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhasanah.alhasanahmedia.data.model.LaporanKategori
import com.alhasanah.alhasanahmedia.data.model.LaporanMasalah
import com.alhasanah.alhasanahmedia.data.model.LaporanPrioritas
import com.alhasanah.alhasanahmedia.data.model.LaporanStatus
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme

private val LuxuryGold = Color(0xFFD4A853)
private val LuxuryGoldLight = Color(0xFFECC96B)
private val LuxuryGoldDim = Color(0xFF9A7535)
private val StatusOpen = Color(0xFF3B82F6)
private val StatusInProgress = Color(0xFFF59E0B)
private val StatusFixed = Color(0xFF10B981)
private val StatusRejected = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanMasalahScreen(onBack: () -> Unit, onLaporanClick: (String) -> Unit, viewModel: LaporanMasalahViewModel) {
    val isDark = isAppInDarkTheme()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is LaporanMasalahUiState.Success) { kotlinx.coroutines.delay(2000); viewModel.resetUiState() }
    }

    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isDark) AppSolidBackground(isDark = true)
            LaporanMasalahContent(isDark, onBack, onLaporanClick, viewModel, uiState)
        }
    }
}

@Composable
private fun LaporanMasalahContent(isDark: Boolean, onBack: () -> Unit, onLaporanClick: (String) -> Unit, viewModel: LaporanMasalahViewModel, uiState: LaporanMasalahUiState) {
    val judul by viewModel.selectedJudul.collectAsState()
    val deskripsi by viewModel.selectedDeskripsi.collectAsState()
    val kategori by viewModel.selectedKategori.collectAsState()
    val prioritas by viewModel.selectedPrioritas.collectAsState()
    val laporans by viewModel.laporans.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
        item { LaporanHero(isDark, onBack) }
        item { LaporanFormCard(isDark, judul, deskripsi, kategori, prioritas, viewModel::updateJudul, viewModel::updateDeskripsi, viewModel::updateKategori, viewModel::updatePrioritas, viewModel::submitLaporan, uiState) }
        item { LaporanHistoryHeader(isDark, laporans.size) }
        if (laporans.isEmpty()) item { EmptyLaporanState(isDark) } else items(laporans) { l -> LaporanListItem(isDark, l) { l.id?.let { onLaporanClick(it) } } }
    }
}

@Composable
private fun LaporanHero(isDark: Boolean, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(if (isDark) Color(0xFF1C1408) else Color(0xFFFFF7E6), if (isDark) Color(0xFF0F0D0A) else Color(0xFFF3E2BC))))) {
            Canvas(Modifier.fillMaxSize()) { drawCircle(Brush.radialGradient(listOf(LuxuryGold.copy(alpha = 0.10f), Color.Transparent), center, size.minDimension * 0.5f), size.minDimension * 0.5f) }
        }
        Box(modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, if (isDark) Color(0xFF0F0D0A) else Color(0xFFFDF9F3)))))
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = if (isDark) 0.3f else 0.15f)).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(80.dp).shadow(20.dp, CircleShape).clip(CircleShape).background(if (isDark) Color(0xFF1E1A14) else Color.White.copy(alpha = 0.9f)).border(2.dp, Brush.sweepGradient(listOf(LuxuryGold.copy(alpha = 0.6f), LuxuryGoldLight.copy(alpha = 0.3f), LuxuryGold.copy(alpha = 0.6f))), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.BugReport, null, tint = if (isDark) LuxuryGoldLight else Color(0xFF8B6914), modifier = Modifier.size(40.dp))
            }
            Text("Bantuan & Masukan", style = MaterialTheme.typography.headlineMedium.copy(color = if (isDark) LuxuryGoldLight else Color(0xFF4A3728), fontWeight = FontWeight.Bold, fontSize = 26.sp), textAlign = TextAlign.Center)
            Text("Kami siap membantu dan mendengarkan Anda", style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF6B5A48).copy(alpha = 0.8f), fontSize = 14.sp), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LaporanFormCard(isDark: Boolean, judul: String, deskripsi: String, kategori: LaporanKategori, prioritas: LaporanPrioritas, onJudulChange: (String) -> Unit, onDeskripsiChange: (String) -> Unit, onKategoriChange: (LaporanKategori) -> Unit, onPrioritasChange: (LaporanPrioritas) -> Unit, onSubmit: () -> Unit, uiState: LaporanMasalahUiState) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E1A14) else Color.White), elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(40.dp).background(if (isDark) Color(0xFF232019) else Color(0xFFFFF7E6), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.EditNote, null, tint = if (isDark) LuxuryGoldLight else Color(0xFF8B6914), modifier = Modifier.size(22.dp)) }
                Column { Text("Kirim Laporan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF5F0E8) else Color(0xFF1A1208), fontSize = 18.sp)); Text("Laporkan bug, sampaikan masukan atau usulan fitur", style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFF8A8078) else Color(0xFF6B5A48).copy(alpha = 0.7f), fontSize = 12.sp)) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Judul Laporan", style = MaterialTheme.typography.labelLarge.copy(color = if (isDark) Color(0xFFF5F0E8) else Color(0xFF1A1208), fontWeight = FontWeight.SemiBold, fontSize = 14.sp))
                OutlinedTextField(value = judul, onValueChange = { if (it.length <= 100) onJudulChange(it) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Contoh: Absensi tidak muncul", color = if (isDark) Color(0xFF8A8078).copy(alpha = 0.5f) else Color(0xFF9A7535).copy(alpha = 0.4f)) }, trailingIcon = { Icon(Icons.Outlined.Edit, null, tint = if (isDark) Color(0xFF8A8078).copy(alpha = 0.5f) else Color(0xFF9A7535).copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) }, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxuryGold, unfocusedBorderColor = if (isDark) Color(0xFF3A352E) else Color(0xFFE8DCC8), focusedContainerColor = if (isDark) Color(0xFF1E1A14) else Color(0xFFFFFDF8), unfocusedContainerColor = if (isDark) Color(0xFF1E1A14) else Color(0xFFFFFDF8), focusedTextColor = if (isDark) Color(0xFFF5F0E8) else Color(0xFF1A1208), unfocusedTextColor = if (isDark) Color(0xFFF5F0E8) else Color(0xFF1A1208), cursorColor = LuxuryGold), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), supportingText = { Text("${judul.length}/100", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(color = if (isDark) Color(0xFF8A8078).copy(alpha = 0.5f) else Color(0xFF9A7535).copy(alpha = 0.5f), fontSize = 11.sp)) })
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Deskripsi Masalah", style = MaterialTheme.typography.labelLarge.copy(color = if (isDark) Color(0xFFF5F0E8) else Color(0xFF1A1208), fontWeight = FontWeight.SemiBold, fontSize = 14.sp))
                OutlinedTextField(value = deskripsi, onValueChange = { if (it.length <= 1000) onDeskripsiChange(it) }, modifier = Modifier.fillMaxWidth().height(140.dp), placeholder = { Text("Jelaskan masalah yang Anda alami secara lengkap dan jelas...", color = if (isDark) Color(0xFF8A8078).copy(alpha = 0.5f) else Color(0xFF9A7535).copy(alpha = 0.4f)) }, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxuryGold, unfocusedBorderColor = if (isDark) Color(0xFF3A352E) else Color(0xFFE8DCC8), focusedContainerColor = if (isDark) Color(0xFF1E1A14) else Color(0xFFFFFDF8), unfocusedContainerColor = if (isDark) Color(0xFF1E1A14) else Color(0xFFFFFDF8), focusedTextColor = if (isDark) Color(0xFFF5F0E8) else Color(0xFF1A1208), unfocusedTextColor = if (isDark) Color(0xFFF5F0E8) else Color(0xFF1A1208), cursorColor = LuxuryGold), maxLines = 6, supportingText = { Text("${deskripsi.length}/1000", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(color = if (isDark) Color(0xFF8A8078).copy(alpha = 0.5f) else Color(0xFF9A7535).copy(alpha = 0.5f), fontSize = 11.sp)) })
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text("Kategori", style = MaterialTheme.typography.labelLarge.copy(color = if (isDark) Color(0xFFF5F0E8) else Color(0xFF1A1208), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)); Icon(Icons.Outlined.Info, null, tint = if (isDark) Color(0xFF8A8078).copy(alpha = 0.4f) else Color(0xFF9A7535).copy(alpha = 0.4f), modifier = Modifier.size(16.dp)) }
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KategoriChip(isDark, Icons.Outlined.BugReport, "Bug", kategori == LaporanKategori.BUG) { onKategoriChange(LaporanKategori.BUG) }
                    KategoriChip(isDark, Icons.Outlined.Lightbulb, "Usulan Fitur", kategori == LaporanKategori.FITUR) { onKategoriChange(LaporanKategori.FITUR) }
                    KategoriChip(isDark, Icons.Outlined.HelpOutline, "Pertanyaan", kategori == LaporanKategori.PERTANYAAN) { onKategoriChange(LaporanKategori.PERTANYAAN) }
                    KategoriChip(isDark, Icons.Outlined.Chat, "Masukan", kategori == LaporanKategori.MASUKAN) { onKategoriChange(LaporanKategori.MASUKAN) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Prioritas", style = MaterialTheme.typography.labelLarge.copy(color = if (isDark) Color(0xFFF5F0E8) else Color(0xFF1A1208), fontWeight = FontWeight.SemiBold, fontSize = 14.sp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrioritasChip(isDark, Icons.Outlined.ArrowDownward, "Rendah", Color(0xFF10B981), prioritas == LaporanPrioritas.LOW) { onPrioritasChange(LaporanPrioritas.LOW) }
                    PrioritasChip(isDark, Icons.Outlined.Remove, "Sedang", Color(0xFFF59E0B), prioritas == LaporanPrioritas.MEDIUM) { onPrioritasChange(LaporanPrioritas.MEDIUM) }
                    PrioritasChip(isDark, Icons.Outlined.ArrowUpward, "Tinggi", Color(0xFFEF4444), prioritas == LaporanPrioritas.HIGH) { onPrioritasChange(LaporanPrioritas.HIGH) }
                    PrioritasChip(isDark, Icons.Outlined.Warning, "Mendesak", Color(0xFFDC2626), prioritas == LaporanPrioritas.URGENT) { onPrioritasChange(LaporanPrioritas.URGENT) }
                }
            }
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = if (isDark) Color(0xFF1E1A14) else Color(0xFFFFF7E6)) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Shield, null, tint = if (isDark) LuxuryGoldLight else Color(0xFF8B6914), modifier = Modifier.size(20.dp))
                    Column { Text("Informasi perangkat akan dikirim otomatis untuk membantu kami mempercepat proses penanganan.", style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF4A3728), fontSize = 12.sp, lineHeight = 18.sp)); Text("Lihat detail", style = MaterialTheme.typography.labelSmall.copy(color = if (isDark) LuxuryGoldLight else Color(0xFF8B6914), fontWeight = FontWeight.SemiBold, fontSize = 12.sp), modifier = Modifier.clickable { }) }
                }
            }
            Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White), enabled = uiState !is LaporanMasalahUiState.Loading, contentPadding = PaddingValues(0.dp)) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFFD4A853), Color(0xFFB8922F))), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    if (uiState is LaporanMasalahUiState.Loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp) else Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Send, null, modifier = Modifier.size(20.dp)); Text("Kirim Laporan", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { HorizontalDivider(modifier = Modifier.weight(1f), color = if (isDark) Color(0xFF3A352E) else Color(0xFFE8DCC8)); Text("atau", style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFF8A8078).copy(alpha = 0.6f) else Color(0xFF9A7535).copy(alpha = 0.6f), fontSize = 12.sp)); HorizontalDivider(modifier = Modifier.weight(1f), color = if (isDark) Color(0xFF3A352E) else Color(0xFFE8DCC8)) }
            OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF4A3728)), border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(if (isDark) Color(0xFF3A352E) else Color(0xFFE8DCC8), if (isDark) Color(0xFF3A352E).copy(alpha = 0.7f) else Color(0xFFD4C4A8))))) { Icon(Icons.Outlined.Save, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Simpan sebagai Draft", fontWeight = FontWeight.Medium, fontSize = 14.sp) }
        }
    }
    AnimatedVisibility(visible = uiState is LaporanMasalahUiState.Success || uiState is LaporanMasalahUiState.Error, modifier = Modifier.padding(horizontal = 20.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = when (uiState) { is LaporanMasalahUiState.Success -> Color(0xFF10B981).copy(alpha = 0.1f); is LaporanMasalahUiState.Error -> Color(0xFFEF4444).copy(alpha = 0.1f); else -> Color.Transparent })) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = when (uiState) { is LaporanMasalahUiState.Success -> Icons.Outlined.CheckCircle; is LaporanMasalahUiState.Error -> Icons.Outlined.Error; else -> Icons.Outlined.Info }, contentDescription = null, tint = when (uiState) { is LaporanMasalahUiState.Success -> Color(0xFF10B981); is LaporanMasalahUiState.Error -> Color(0xFFEF4444); else -> Color.Gray }, modifier = Modifier.size(24.dp))
                Text(text = when (uiState) { is LaporanMasalahUiState.Success -> "Laporan berhasil dikirim!"; is LaporanMasalahUiState.Error -> uiState.message; else -> "" }, style = MaterialTheme.typography.bodyMedium.copy(color = when (uiState) { is LaporanMasalahUiState.Success -> Color(0xFF10B981); is LaporanMasalahUiState.Error -> Color(0xFFEF4444); else -> Color.Gray }, fontWeight = FontWeight.Medium))
            }
        }
    }
}

@Composable
private fun KategoriChip(isDark: Boolean, icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) (if (isDark) Color(0xFF2A2318) else Color(0xFFFFF7E6)) else (if (isDark) Color(0xFF1E1A14) else Color(0xFFF5F0E8))
    val border = if (isSelected) Color(0xFFD4A853) else (if (isDark) Color(0xFF3A352E) else Color(0xFFE8DCC8))
    val text = if (isSelected) (if (isDark) LuxuryGoldLight else Color(0xFF8B6914)) else (if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF6B5A48).copy(alpha = 0.7f))
    Surface(modifier = Modifier.clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), color = bg, border = androidx.compose.foundation.BorderStroke(1.dp, border)) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = text, modifier = Modifier.size(16.dp)); Text(label, style = MaterialTheme.typography.labelMedium.copy(color = text, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, fontSize = 13.sp)) }
    }
}

@Composable
private fun PrioritasChip(isDark: Boolean, icon: ImageVector, label: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) color.copy(alpha = if (isDark) 0.2f else 0.1f) else (if (isDark) Color(0xFF1E1A14) else Color(0xFFF5F0E8))
    val border = if (isSelected) color.copy(alpha = 0.5f) else (if (isDark) Color(0xFF3A352E) else Color(0xFFE8DCC8))
    val text = if (isSelected) color else (if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF6B5A48).copy(alpha = 0.7f))
    Surface(modifier = Modifier.clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), color = bg, border = androidx.compose.foundation.BorderStroke(1.dp, border)) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = text, modifier = Modifier.size(16.dp)); Text(label, style = MaterialTheme.typography.labelMedium.copy(color = text, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, fontSize = 13.sp)) }
    }
}

@Composable
private fun LaporanHistoryHeader(isDark: Boolean, count: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Riwayat Laporan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF5F0E8) else Color(0xFF1A1208), fontSize = 18.sp))
        Row(modifier = Modifier.clickable { }, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { Text("Lihat semua", style = MaterialTheme.typography.labelMedium.copy(color = if (isDark) LuxuryGoldLight else Color(0xFF8B6914), fontWeight = FontWeight.Medium, fontSize = 13.sp)); Icon(Icons.Outlined.ChevronRight, null, tint = if (isDark) LuxuryGoldLight else Color(0xFF8B6914), modifier = Modifier.size(18.dp)) }
    }
}

@Composable
private fun EmptyLaporanState(isDark: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Inbox, null, tint = if (isDark) Color(0xFF8A8078).copy(alpha = 0.3f) else Color(0xFF9A7535).copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
            Text("Belum ada laporan", style = MaterialTheme.typography.bodyLarge.copy(color = if (isDark) Color(0xFFBDB5A8).copy(alpha = 0.5f) else Color(0xFF6B5A48).copy(alpha = 0.5f), fontWeight = FontWeight.Medium))
            Text("Kirim laporan pertama Anda menggunakan form di atas", style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFF8A8078).copy(alpha = 0.4f) else Color(0xFF9A7535).copy(alpha = 0.4f), textAlign = TextAlign.Center))
        }
    }
}

@Composable
private fun LaporanListItem(isDark: Boolean, laporan: LaporanMasalah, onClick: () -> Unit) {
    val statusColor = when (laporan.status) { "OPEN" -> StatusOpen; "IN_PROGRESS" -> StatusInProgress; "FIXED" -> StatusFixed; "REJECTED" -> StatusRejected; "NEED_INFO" -> Color(0xFF8B5CF6); else -> Color.Gray }
    val statusLabel = when (laporan.status) { "OPEN" -> LaporanStatus.OPEN.label; "IN_PROGRESS" -> LaporanStatus.IN_PROGRESS.label; "FIXED" -> LaporanStatus.FIXED.label; "REJECTED" -> LaporanStatus.REJECTED.label; "NEED_INFO" -> LaporanStatus.NEED_INFO.label; else -> laporan.status }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E1A14) else Color.White), elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = if (isDark) Color(0xFF2A2318) else Color(0xFFFFF7E6)) { Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(when (laporan.kategori) { "BUG" -> Icons.Outlined.BugReport; "FITUR" -> Icons.Outlined.Lightbulb; "PERTANYAAN" -> Icons.Outlined.HelpOutline; else -> Icons.Outlined.Chat }, null, tint = if (isDark) LuxuryGoldLight else Color(0xFF8B6914), modifier = Modifier.size(12.dp)); Text(laporan.kategori, style = MaterialTheme.typography.labelSmall.copy(color = if (isDark) LuxuryGoldLight else Color(0xFF8B6914), fontWeight = FontWeight.SemiBold, fontSize = 10.sp)) } }
                    Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = if (isDark) 0.2f else 0.1f)) { Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor)); Text(statusLabel, style = MaterialTheme.typography.labelSmall.copy(color = statusColor, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)) } }
                }
                Icon(Icons.Outlined.MoreVert, null, tint = if (isDark) Color(0xFF8A8078).copy(alpha = 0.4f) else Color(0xFF9A7535).copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
            }
            Text(laporan.judul, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF5F0E8) else Color(0xFF1A1208), fontSize = 16.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (laporan.deviceBrand != null || laporan.deviceModel != null) { Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.PhoneAndroid, null, tint = if (isDark) Color(0xFF8A8078).copy(alpha = 0.5f) else Color(0xFF9A7535).copy(alpha = 0.5f), modifier = Modifier.size(14.dp)); Text(listOfNotNull(laporan.deviceBrand, laporan.deviceModel).joinToString(" ") + (laporan.androidVersion?.let { " · Android $it" } ?: ""), style = MaterialTheme.typography.labelSmall.copy(color = if (isDark) Color(0xFF8A8078) else Color(0xFF9A7535), fontSize = 11.sp)) } }
            if (laporan.createdAt != null) { Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Schedule, null, tint = if (isDark) Color(0xFF8A8078).copy(alpha = 0.5f) else Color(0xFF9A7535).copy(alpha = 0.5f), modifier = Modifier.size(14.dp)); Text(formatTimestamp(laporan.createdAt), style = MaterialTheme.typography.labelSmall.copy(color = if (isDark) Color(0xFF8A8078) else Color(0xFF9A7535), fontSize = 11.sp)) } }
        }
    }
}

private fun formatTimestamp(timestamp: String): String = try { val input = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()); val output = java.text.SimpleDateFormat("dd MMM yyyy · HH:mm", java.util.Locale("id", "ID")); output.format(input.parse(timestamp.take(19)) ?: timestamp) } catch (e: Exception) { timestamp.take(16).replace("T", " ") }
