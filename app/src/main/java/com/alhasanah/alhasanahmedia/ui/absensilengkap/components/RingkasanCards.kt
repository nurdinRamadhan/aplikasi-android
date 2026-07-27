package com.alhasanah.alhasanahmedia.ui.absensilengkap.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhasanah.alhasanahmedia.data.model.RingkasanSummary

private data class StatusEntry(
    val label: String,
    val count: Int,
    val color: Color
)

@Composable
fun RingkasanCards(
    ringkasan: RingkasanSummary,
    totalDays: Int,
    modifier: Modifier = Modifier
) {
    val total = ringkasan.total

    val entries = listOf(
        StatusEntry("Hadir", ringkasan.hadir, Color(0xFF16A34A)),
        StatusEntry("Izin", ringkasan.izin, Color(0xFFF59E0B)),
        StatusEntry("Sakit", ringkasan.sakit, Color(0xFFEF4444)),
        StatusEntry("Alpha", ringkasan.alpha, Color(0xFF6B7280)),
        StatusEntry("Sekolah", ringkasan.sekolah, Color(0xFF8B5CF6)),
        StatusEntry("Pulang", ringkasan.pulang, Color(0xFF0891B2))
    ).filter { it.count > 0 }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ringkasan Kehadiran",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Badge "7 Hari" atau jumlah hari
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$totalDays Hari",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Box summary (Hadir, Izin, Sakit, Alpha)
            // Sekolah digabung ke Izin untuk ringkasan
            val izinTotal = ringkasan.izin + ringkasan.sekolah

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBox(
                    count = ringkasan.hadir,
                    label = "Hadir",
                    percentage = if (total > 0) (ringkasan.hadir.toFloat() / total) * 100f else 0f,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
                StatusBox(
                    count = izinTotal,
                    label = "Izin",
                    percentage = if (total > 0) (izinTotal.toFloat() / total) * 100f else 0f,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                StatusBox(
                    count = ringkasan.sakit,
                    label = "Sakit",
                    percentage = if (total > 0) (ringkasan.sakit.toFloat() / total) * 100f else 0f,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                StatusBox(
                    count = ringkasan.alpha,
                    label = "Alpha",
                    percentage = if (total > 0) (ringkasan.alpha.toFloat() / total) * 100f else 0f,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.weight(1f)
                )
            }

            // Pulang (jika ada)
            if (ringkasan.pulang > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBox(
                        count = ringkasan.pulang,
                        label = "Pulang",
                        percentage = if (total > 0) (ringkasan.pulang.toFloat() / total) * 100f else 0f,
                        color = Color(0xFF0891B2),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(3f))
                }
            }
        }
    }
}

@Composable
private fun StatusBox(
    count: Int,
    label: String,
    percentage: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${String.format("%.1f", percentage)}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
