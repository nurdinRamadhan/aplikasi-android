package com.alhasanah.alhasanahmedia.ui.absensilengkap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alhasanah.alhasanahmedia.util.AlhasanahGold
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    attendanceData: Map<LocalDate, List<String>>,
    onDayClick: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val greenColor = Color(0xFF16A34A)
    val yellowColor = Color(0xFFF59E0B)
    val redColor = Color(0xFFEF4444)
    val grayColor = Color(0xFF6B7280)

    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)

    // Start from Monday
    val startDayOfWeek = firstDayOfMonth.dayOfWeek
    val daysToSubtract = if (startDayOfWeek == DayOfWeek.MONDAY) 0 else 
        startDayOfWeek.value - 1

    val calendarDays = mutableListOf<LocalDate?>()

    // Add empty days for alignment
    repeat(daysToSubtract) {
        calendarDays.add(null)
    }

    // Add actual days
    for (day in 1..daysInMonth) {
        calendarDays.add(yearMonth.atDay(day))
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
                .padding(16.dp)
        ) {
            // Month header with navigation arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onMonthChange(yearMonth.minusMonths(1)) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Bulan Sebelumnya",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale("id"))} ${yearMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { onMonthChange(yearMonth.plusMonths(1)) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Bulan Selanjutnya",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day of week headers - sesuai referensi (Sen-Sin)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Ahd").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            val rows = calendarDays.chunked(7)
            rows.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    week.forEach { date ->
                        if (date != null) {
                            DayCell(
                                date = date,
                                statuses = attendanceData[date],
                                onClick = { onDayClick(date) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Legend - sesuai referensi
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = greenColor, label = "Hadir")
                Spacer(modifier = Modifier.width(12.dp))
                LegendDot(color = yellowColor, label = "Izin")
                Spacer(modifier = Modifier.width(12.dp))
                LegendDot(color = redColor, label = "Sakit")
                Spacer(modifier = Modifier.width(12.dp))
                LegendDot(color = grayColor, label = "Alpha")
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    statuses: List<String>?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greenColor = Color(0xFF16A34A)
    val isToday = date == LocalDate.now()
    val hasData = !statuses.isNullOrEmpty()

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isToday -> greenColor
                    hasData -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isToday -> Color.White
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // Status dots
            if (hasData) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    val dominantStatus = getDominantStatusForDay(statuses!!)
                    Dot(color = getStatusColor(dominantStatus), size = 4.dp)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun Dot(
    color: Color,
    size: androidx.compose.ui.unit.Dp = 6.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape)
    )
}

private fun getDominantStatusForDay(statuses: List<String>): String {
    val count = statuses.groupingBy { it }.eachCount()
    return count.maxByOrNull { it.value }?.key ?: "HADIR"
}

private fun getStatusColor(status: String): Color {
    return when (status.uppercase()) {
        "HADIR" -> Color(0xFF16A34A)
        "IZIN" -> Color(0xFFF59E0B)
        "SAKIT" -> Color(0xFFEF4444)
        "ALFA", "GHAIB" -> Color(0xFF6B7280)
        else -> Color(0xFF6B7280)
    }
}