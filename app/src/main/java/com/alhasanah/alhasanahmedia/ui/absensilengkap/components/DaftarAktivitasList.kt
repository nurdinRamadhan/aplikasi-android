package com.alhasanah.alhasanahmedia.ui.absensilengkap.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alhasanah.alhasanahmedia.data.model.HariAbsensi
import com.alhasanah.alhasanahmedia.data.model.KegiatanHarian

@Composable
fun DaftarAktivitasList(
    perHari: List<HariAbsensi>,
    getStatusColor: (String) -> Long,
    getStatusLabel: (String) -> String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        perHari.forEach { hari ->
            HariAccordion(
                hari = hari,
                getStatusColor = getStatusColor,
                getStatusLabel = getStatusLabel
            )
        }
    }
}

@Composable
private fun HariAccordion(
    hari: HariAbsensi,
    getStatusColor: (String) -> Long,
    getStatusLabel: (String) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "chevron"
    )

    // Hitung ringkasan per hari
    val summary = remember(hari.kegiatan) {
        mutableMapOf(
            "HADIR" to 0,
            "IZIN" to 0,
            "SAKIT" to 0,
            "ALPHA" to 0
        ).also { map ->
            hari.kegiatan.forEach { kegiatan ->
                when (kegiatan.status.uppercase()) {
                    "HADIR" -> map["HADIR"] = (map["HADIR"] ?: 0) + 1
                    "IZIN" -> map["IZIN"] = (map["IZIN"] ?: 0) + 1
                    "SAKIT" -> map["SAKIT"] = (map["SAKIT"] ?: 0) + 1
                    "ALFA", "GHAIB" -> map["ALPHA"] = (map["ALPHA"] ?: 0) + 1
                }
            }
        }
    }

    // Hitung persentase kehadiran
    val percentage = remember(hari.kegiatan) {
        if (hari.kegiatan.isEmpty()) 0.0
        else {
            val hadirCount = hari.kegiatan.count { it.status.uppercase() == "HADIR" }
            (hadirCount.toDouble() / hari.kegiatan.size) * 100.0
        }
    }

    // Warna badge berdasarkan persentase
    val badgeColor = when {
        percentage >= 80 -> Color(0xFF16A34A)  // Hijau
        percentage >= 60 -> Color(0xFFF59E0B)  // Kuning
        else -> Color(0xFFEF4444)              // Merah
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp)
        ) {
            // Header: Tanggal + Badge Persentase + Expand Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${hari.hari}, ${hari.tanggalDisplay}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Ringkasan dots: ● X ○ X ● X ● X
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusDot(
                            color = Color(0xFF16A34A),
                            count = summary["HADIR"] ?: 0
                        )
                        StatusDot(
                            color = Color(0xFFF59E0B),
                            count = summary["IZIN"] ?: 0
                        )
                        StatusDot(
                            color = Color(0xFFEF4444),
                            count = summary["SAKIT"] ?: 0
                        )
                        StatusDot(
                            color = Color(0xFF6B7280),
                            count = summary["ALPHA"] ?: 0
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge persentase
                    Box(
                        modifier = Modifier
                            .background(
                                color = badgeColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${String.format("%.0f", percentage)}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    // Expand icon
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Tutup" else "Buka",
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = rotation },
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Expanded content: Kegiatan per hari
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Group by sesi (untuk tahfidz) atau tampilkan semua
                    val groupedBySesi = remember(hari.kegiatan) {
                        groupBySesi(hari.kegiatan)
                    }

                    groupedBySesi.forEach { (sesi, kegiatanList) ->
                        // Label sesi (Pagi, Siang, Sore, Malam)
                        if (sesi != "Lainnya") {
                            Text(
                                text = sesi,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        kegiatanList.forEach { kegiatan ->
                            KegiatanItem(
                                kegiatan = kegiatan,
                                getStatusColor = getStatusColor,
                                getStatusLabel = getStatusLabel
                            )
                        }

                        if (sesi != groupedBySesi.keys.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(
    color: androidx.compose.ui.graphics.Color,
    count: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun KegiatanItem(
    kegiatan: KegiatanHarian,
    getStatusColor: (String) -> Long,
    getStatusLabel: (String) -> String
) {
    val statusColor = Color(getStatusColor(kegiatan.status))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon kegiatan berdasarkan sumber
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getKegiatanIcon(kegiatan.sumber),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column {
                Text(
                    text = kegiatan.nama,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                // Hanya tampilkan sesi untuk Ziyadah (Pagi) dan Murojaah (Siang)
                if (kegiatan.kategori == "ZIYADAH" || kegiatan.kategori == "MUROJAAH") {
                    val sesiLabel = if (kegiatan.kategori == "ZIYADAH") "Pagi" else "Siang"
                    Text(
                        text = sesiLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Status badge
        Box(
            modifier = Modifier
                .background(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = getStatusLabel(kegiatan.status),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}

private fun getKegiatanIcon(sumber: String): String {
    return when (sumber) {
        "tahfidz" -> "📖"
        "mingguan" -> "📅"
        "ngaji" -> "🕌"
        "sholat_hifdzi" -> "🌙"
        else -> "📋"
    }
}

private fun groupBySesi(kegiatanList: List<KegiatanHarian>): Map<String, List<KegiatanHarian>> {
    val result = mutableMapOf<String, MutableList<KegiatanHarian>>()

    kegiatanList.forEach { kegiatan ->
        // Hanya Ziyadah = Pagi, Murojaah = Siang
        // Kegiatan lain (Mingguan, Ngaji, Sholat Hifdzi) tanpa sesi
        val sesiKey = when {
            kegiatan.kategori == "ZIYADAH" -> "Pagi"
            kegiatan.kategori == "MUROJAAH" -> "Siang"
            else -> "Lainnya"
        }

        if (!result.containsKey(sesiKey)) {
            result[sesiKey] = mutableListOf()
        }
        result[sesiKey]?.add(kegiatan)
    }

    // Sort: Pagi first, then Siang, then others
    val sortedKeys = result.keys.sortedWith(
        compareBy<String> {
            when (it.lowercase()) {
                "pagi" -> 0
                "siang" -> 1
                "sore" -> 2
                "malam" -> 3
                else -> 4
            }
        }
    )

    return sortedKeys.associateWith { result[it] ?: emptyList() }
}
