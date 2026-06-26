package com.alhasanah.alhasanahmedia.ui.quran

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import com.alhasanah.alhasanahmedia.data.model.quran.TafsirItem

@Composable
fun AyahNumberFrame(
    number: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier = modifier.size(42.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2.2f
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw 8-point star (Rub el Hizb)
            val path = Path()
            val sides = 8
            val innerRadius = radius * 0.82f
            
            for (i in 0 until sides * 2) {
                val r = if (i % 2 == 0) radius else innerRadius
                val angle = (i * Math.PI / sides).toFloat()
                val x = center.x + r * Math.cos(angle.toDouble()).toFloat()
                val y = center.y + r * Math.sin(angle.toDouble()).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            
            drawPath(
                path = path,
                color = color.copy(alpha = 0.1f),
                style = androidx.compose.ui.graphics.drawscope.Fill
            )
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        Text(
            text = number,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirBottomSheet(
    ayatNo: Int,
    state: QuranUiState<TafsirItem>?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = "Tafsir Ayat $ayatNo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (val st = state) {
                is QuranUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is QuranUiState.Error -> {
                    Text(st.message, color = MaterialTheme.colorScheme.error)
                }
                is QuranUiState.Success -> {
                    val tafsir = st.data.tafsir.find { it.ayat == ayatNo }?.teks
                        ?: "Tafsir tidak ditemukan untuk ayat ini."
                    
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item {
                            Text(
                                text = tafsir,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Justify,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QoriSelectionSheet(
    selectedQori: String,
    onQoriSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val qoris = listOf(
        "1" to "Mishary Rasyid Al-Afasy",
        "2" to "Abu Bakar Al-Shatri",
        "3" to "Abdurrahman As-Sudais",
        "4" to "Abdullah Al-Matroud",
        "5" to "Maher Al-Muaiqly"
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Pilih Qori",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            qoris.forEach { (id, name) ->
                ListItem(
                    headlineContent = { Text(name) },
                    modifier = Modifier.clickable { onQoriSelected(id) },
                    trailingContent = {
                        if (selectedQori == id) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }
    }
}
