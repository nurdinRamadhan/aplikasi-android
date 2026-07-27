package com.alhasanah.alhasanahmedia.ui.absensilengkap.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhasanah.alhasanahmedia.data.model.HariAbsensi
import com.alhasanah.alhasanahmedia.util.AlhasanahGold
import kotlin.math.roundToInt

@Composable
fun GrafikKehadiranCard(
    dataHari: List<HariAbsensi>,
    modifier: Modifier = Modifier
) {
    val greenColor = Color(0xFF16A34A)
    val yellowColor = Color(0xFFF59E0B)
    val redColor = Color(0xFFEF4444)
    val grayColor = Color(0xFF6B7280)

    // Hitung persentase kehadiran per hari + data detail
    val chartData = remember(dataHari) {
        dataHari.map { hari ->
            val total = hari.kegiatan.size
            val hadir = hari.kegiatan.count { it.status.uppercase() == "HADIR" }
            val izin = hari.kegiatan.count { it.status.uppercase() == "IZIN" }
            val sakit = hari.kegiatan.count { it.status.uppercase() == "SAKIT" }
            val alpha = hari.kegiatan.count { 
                it.status.uppercase() == "ALFA" || it.status.uppercase() == "GHAIB" 
            }
            val persentase = if (total > 0) (hadir.toFloat() / total) * 100f else 0f
            ChartDayData(
                hariSingkat = hari.hariSingkat,
                tanggal = hari.tanggal,
                persentase = persentase,
                hadir = hadir,
                izin = izin,
                sakit = sakit,
                alpha = alpha
            )
        }
    }

    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "chart"
    )

    LaunchedEffect(dataHari) {
        animationPlayed = false
        kotlinx.coroutines.delay(100)
        animationPlayed = true
    }

    // Overall percentage
    val overallPersentase = remember(dataHari) {
        val totalKegiatan = dataHari.sumOf { it.kegiatan.size }
        val totalHadir = dataHari.sumOf { hari ->
            hari.kegiatan.count { it.status.uppercase() == "HADIR" }
        }
        if (totalKegiatan > 0) (totalHadir.toFloat() / totalKegiatan) * 100f else 0f
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grafik Kehadiran",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Badge jumlah hari
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${dataHari.size} Hari",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bar Chart
            if (chartData.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Y-axis labels
                    Column(
                        modifier = Modifier
                            .width(30.dp)
                            .height(150.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        listOf("100", "50", "0").forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Chart area
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(150.dp)
                    ) {
                        val barWidth = size.width / (chartData.size * 1.8f)
                        val spacing = barWidth * 0.8f
                        val maxValue = 100f
                        val chartHeight = size.height - 10.dp.toPx()

                        // Draw horizontal grid lines
                        listOf(0f, 50f, 100f).forEach { gridValue ->
                            val y = chartHeight - (gridValue / maxValue) * chartHeight
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        chartData.forEachIndexed { index, data ->
                            val barHeight = (data.persentase / maxValue) * chartHeight
                            val x = index * (barWidth + spacing) + spacing / 2
                            val y = chartHeight - barHeight

                            // Bar color based on percentage
                            val barColor = when {
                                data.persentase >= 80f -> greenColor
                                data.persentase >= 50f -> yellowColor
                                else -> redColor
                            }

                            // Draw bar with animation
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y + (1f - animatedProgress) * barHeight),
                                size = Size(barWidth, barHeight * animatedProgress),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )

                            // Draw day label below bar
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    textSize = 9.sp.toPx()
                                    color = android.graphics.Color.GRAY
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                                drawText(
                                    data.hariSingkat,
                                    x + barWidth / 2,
                                    chartHeight + 12.dp.toPx(),
                                    paint
                                )
                                // Draw date below day name
                                val dateText = data.tanggal.takeLast(2)
                                drawText(
                                    dateText,
                                    x + barWidth / 2,
                                    chartHeight + 24.dp.toPx(),
                                    paint
                                )
                            }
                        }
                    }

                    // Percentage label on right
                    Column(
                        modifier = Modifier
                            .width(40.dp)
                            .height(150.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${overallPersentase.roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = greenColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legend - sesuai referensi: Hadir, Izin, Sakit, Alpha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = greenColor, label = "Hadir")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = yellowColor, label = "Izin")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = redColor, label = "Sakit")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = grayColor, label = "Alpha")
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private data class ChartDayData(
    val hariSingkat: String,
    val tanggal: String,
    val persentase: Float,
    val hadir: Int,
    val izin: Int,
    val sakit: Int,
    val alpha: Int
)

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}