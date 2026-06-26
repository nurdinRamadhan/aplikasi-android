package com.alhasanah.alhasanahmedia.ui.falak

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alhasanah.alhasanahmedia.domain.falak.VisualGerhanaBulan
import com.alhasanah.alhasanahmedia.domain.falak.VisualHilal
import kotlin.math.min

@Composable
fun VisualGerhanaBulanCard(visual: VisualGerhanaBulan, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(visual.judul, fontWeight = FontWeight.Black)
            Text(
                "${visual.sumberData} • ${visual.status}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GerhanaBulanCanvas(visual)
        }
    }
}

@Composable
fun VisualHilalCard(visual: VisualHilal, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(visual.judul, fontWeight = FontWeight.Black)
            Text(
                "${visual.sumberData} • ${visual.status}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HilalCanvas(visual)
        }
    }
}

@Composable
private fun GerhanaBulanCanvas(visual: VisualGerhanaBulan) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.45f)
    ) {
        val scale = min(size.width, size.height)
        fun point(x: Double, y: Double) = Offset((x * size.width).toFloat(), (y * size.height).toFloat())
        fun radius(value: Double) = (value * scale).toFloat()
        val center = point(visual.umbra.pusat.x, visual.umbra.pusat.y)
        drawCircle(
            color = surfaceVariant.copy(alpha = 0.55f),
            radius = radius(visual.penumbra.radius),
            center = center,
        )
        drawCircle(
            color = Color(0xFF2F2A34).copy(alpha = 0.86f),
            radius = radius(visual.umbra.radius),
            center = center,
        )
        drawLine(
            color = outline.copy(alpha = 0.7f),
            start = point(visual.lintasanAwal.x, visual.lintasanAwal.y),
            end = point(visual.lintasanAkhir.x, visual.lintasanAkhir.y),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        visual.kontak.forEach { kontak ->
            val posisi = kontak.posisi ?: return@forEach
            drawCircle(
                color = primary.copy(alpha = 0.85f),
                radius = 3.5.dp.toPx(),
                center = point(posisi.x, posisi.y),
            )
        }
        val moonCenter = point(visual.bulan.pusat.x, visual.bulan.pusat.y)
        drawCircle(
            color = Color(0xFFE8E0D5),
            radius = radius(visual.bulan.radius),
            center = moonCenter,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.24f),
            radius = radius(visual.bulan.radius * 0.52),
            center = moonCenter.copy(x = moonCenter.x - radius(visual.bulan.radius * 0.18)),
        )
        drawCircle(
            color = outline.copy(alpha = 0.58f),
            radius = radius(visual.bulan.radius),
            center = moonCenter,
            style = Stroke(width = 1.4.dp.toPx()),
        )
    }
}

@Composable
private fun HilalCanvas(visual: VisualHilal) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.45f)
    ) {
        fun point(x: Double, y: Double) = Offset((x * size.width).toFloat(), (y * size.height).toFloat())
        val ufuk = (visual.ufukY * size.height).toFloat()
        drawLine(
            color = outline.copy(alpha = 0.7f),
            start = Offset(0f, ufuk),
            end = Offset(size.width, ufuk),
            strokeWidth = 2.dp.toPx(),
        )
        drawCircle(
            color = Color(0xFFFFC857).copy(alpha = 0.92f),
            radius = 22.dp.toPx(),
            center = point(visual.matahari.x, visual.matahari.y),
        )
        val moonCenter = point(visual.bulan.x, visual.bulan.y)
        val moonRadius = 20.dp.toPx()
        drawLine(
            color = primary.copy(alpha = 0.45f),
            start = point(visual.matahari.x, visual.matahari.y),
            end = moonCenter,
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = surfaceVariant.copy(alpha = 0.92f),
            radius = moonRadius,
            center = moonCenter,
        )
        drawCircle(
            color = Color(0xFFECE6D8),
            radius = moonRadius,
            center = moonCenter.copy(x = moonCenter.x + (moonRadius * (0.88f - visual.fractionIlluminationPersen.coerceIn(0.0, 100.0).toFloat() / 130f))),
        )
        drawCircle(
            color = outline.copy(alpha = 0.72f),
            radius = moonRadius,
            center = moonCenter,
            style = Stroke(width = 1.2.dp.toPx()),
        )
        drawCircle(
            color = if (visual.memenuhiKriteria) primary else Color(0xFFB85042),
            radius = 4.dp.toPx(),
            center = moonCenter,
        )
    }
}
