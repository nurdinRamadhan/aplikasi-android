package com.alhasanah.alhasanahmedia.ui.absensilengkap.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alhasanah.alhasanahmedia.data.model.QuickFilter
import com.alhasanah.alhasanahmedia.data.model.ViewMode
import com.alhasanah.alhasanahmedia.util.AlhasanahGold

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    currentFilter: QuickFilter,
    selectedSourceFilters: Set<String>,
    selectedStatusFilters: Set<String>,
    viewMode: ViewMode,
    onQuickFilterToggle: (QuickFilter) -> Unit,
    onSourceFilterToggle: (String, Boolean) -> Unit,
    onStatusFilterToggle: (String, Boolean) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onApplyFilters: () -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    // Local state for temporary filter selections
    var tempQuickFilter by remember { mutableStateOf(currentFilter) }
    var tempSourceFilters by remember { mutableStateOf(selectedSourceFilters) }
    var tempStatusFilters by remember { mutableStateOf(selectedStatusFilters) }
    var tempViewMode by remember { mutableStateOf(viewMode) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    tempSourceFilters = emptySet()
                    tempStatusFilters = emptySet()
                    tempViewMode = ViewMode.HARIAN
                    onResetFilters()
                }) {
                    Text(
                        text = "Reset",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Periode
            Text(
                text = "Periode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChipItem(
                    label = "Hari Ini",
                    isSelected = tempQuickFilter == QuickFilter.HARI_INI,
                    onClick = { tempQuickFilter = QuickFilter.HARI_INI }
                )
                FilterChipItem(
                    label = "Kemarin",
                    isSelected = tempQuickFilter == QuickFilter.KEMARIN,
                    onClick = { tempQuickFilter = QuickFilter.KEMARIN }
                )
                FilterChipItem(
                    label = "7 Hari",
                    isSelected = tempQuickFilter == QuickFilter._7_HARI,
                    onClick = { tempQuickFilter = QuickFilter._7_HARI }
                )
                FilterChipItem(
                    label = "30 Hari",
                    isSelected = tempQuickFilter == QuickFilter._30_HARI,
                    onClick = { tempQuickFilter = QuickFilter._30_HARI }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Jenis Absensi",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChipItem(
                    label = "Semua",
                    isSelected = tempSourceFilters.isEmpty(),
                    onClick = { tempSourceFilters = emptySet() }
                )
                FilterChipItem(
                    label = "Tahfidz",
                    isSelected = tempSourceFilters.contains("tahfidz"),
                    onClick = {
                        tempSourceFilters = if (tempSourceFilters.contains("tahfidz")) {
                            tempSourceFilters - "tahfidz"
                        } else {
                            tempSourceFilters + "tahfidz"
                        }
                    }
                )
                FilterChipItem(
                    label = "Mingguan",
                    isSelected = tempSourceFilters.contains("mingguan"),
                    onClick = {
                        tempSourceFilters = if (tempSourceFilters.contains("mingguan")) {
                            tempSourceFilters - "mingguan"
                        } else {
                            tempSourceFilters + "mingguan"
                        }
                    }
                )
                FilterChipItem(
                    label = "Ngaji Kitab",
                    isSelected = tempSourceFilters.contains("ngaji"),
                    onClick = {
                        tempSourceFilters = if (tempSourceFilters.contains("ngaji")) {
                            tempSourceFilters - "ngaji"
                        } else {
                            tempSourceFilters + "ngaji"
                        }
                    }
                )
                FilterChipItem(
                    label = "Sholat Hifdzi",
                    isSelected = tempSourceFilters.contains("sholat_hifdzi"),
                    onClick = {
                        tempSourceFilters = if (tempSourceFilters.contains("sholat_hifdzi")) {
                            tempSourceFilters - "sholat_hifdzi"
                        } else {
                            tempSourceFilters + "sholat_hifdzi"
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Kehadiran
            Text(
                text = "Status Kehadiran",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChipItem(
                    label = "Semua",
                    isSelected = tempStatusFilters.isEmpty(),
                    onClick = { tempStatusFilters = emptySet() }
                )
                FilterChipItem(
                    label = "Hadir",
                    isSelected = tempStatusFilters.contains("HADIR"),
                    onClick = {
                        tempStatusFilters = if (tempStatusFilters.contains("HADIR")) {
                            tempStatusFilters - "HADIR"
                        } else {
                            tempStatusFilters + "HADIR"
                        }
                    }
                )
                FilterChipItem(
                    label = "Izin",
                    isSelected = tempStatusFilters.contains("IZIN"),
                    onClick = {
                        tempStatusFilters = if (tempStatusFilters.contains("IZIN")) {
                            tempStatusFilters - "IZIN"
                        } else {
                            tempStatusFilters + "IZIN"
                        }
                    }
                )
                FilterChipItem(
                    label = "Sakit",
                    isSelected = tempStatusFilters.contains("SAKIT"),
                    onClick = {
                        tempStatusFilters = if (tempStatusFilters.contains("SAKIT")) {
                            tempStatusFilters - "SAKIT"
                        } else {
                            tempStatusFilters + "SAKIT"
                        }
                    }
                )
                FilterChipItem(
                    label = "Alpha",
                    isSelected = tempStatusFilters.contains("ALPHA"),
                    onClick = {
                        tempStatusFilters = if (tempStatusFilters.contains("ALPHA")) {
                            tempStatusFilters - "ALPHA"
                        } else {
                            tempStatusFilters + "ALPHA"
                        }
                    }
                )
                FilterChipItem(
                    label = "Sekolah",
                    isSelected = tempStatusFilters.contains("SEKOLAH"),
                    onClick = {
                        tempStatusFilters = if (tempStatusFilters.contains("SEKOLAH")) {
                            tempStatusFilters - "SEKOLAH"
                        } else {
                            tempStatusFilters + "SEKOLAH"
                        }
                    }
                )
                FilterChipItem(
                    label = "Pulang",
                    isSelected = tempStatusFilters.contains("PULANG"),
                    onClick = {
                        tempStatusFilters = if (tempStatusFilters.contains("PULANG")) {
                            tempStatusFilters - "PULANG"
                        } else {
                            tempStatusFilters + "PULANG"
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Tampilan
            Text(
                text = "Mode Tampilan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeTampilanChip(
                    label = "Harian",
                    description = "Lihat ringkasan kehadiran per hari",
                    isSelected = tempViewMode == ViewMode.HARIAN,
                    onClick = { tempViewMode = ViewMode.HARIAN },
                    modifier = Modifier.weight(1f)
                )
                ModeTampilanChip(
                    label = "Semua Kegiatan",
                    description = "Lihat semua kegiatan dalam satu daftar",
                    isSelected = tempViewMode == ViewMode.SEMUA_KEGIATAN,
                    onClick = { tempViewMode = ViewMode.SEMUA_KEGIATAN },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Terapkan Filter button
            Button(
                onClick = {
                    // Apply quick filter
                    onQuickFilterToggle(tempQuickFilter)

                    // Apply source filters
                    tempSourceFilters.forEach { source ->
                        if (source !in selectedSourceFilters) {
                            onSourceFilterToggle(source, true)
                        }
                    }
                    selectedSourceFilters.forEach { source ->
                        if (source !in tempSourceFilters) {
                            onSourceFilterToggle(source, false)
                        }
                    }

                    // Apply status filters
                    tempStatusFilters.forEach { status ->
                        if (status !in selectedStatusFilters) {
                            onStatusFilterToggle(status, true)
                        }
                    }
                    selectedStatusFilters.forEach { status ->
                        if (status !in tempStatusFilters) {
                            onStatusFilterToggle(status, false)
                        }
                    }

                    onViewModeChange(tempViewMode)
                    onApplyFilters()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlhasanahGold
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Terapkan Filter",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AlhasanahGold.copy(alpha = 0.15f),
            selectedLabelColor = AlhasanahGold
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = AlhasanahGold,
            enabled = true,
            selected = isSelected
        )
    )
}

@Composable
private fun ModeTampilanChip(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AlhasanahGold.copy(alpha = 0.15f),
            selectedLabelColor = AlhasanahGold
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = AlhasanahGold,
            enabled = true,
            selected = isSelected
        )
    )
}
