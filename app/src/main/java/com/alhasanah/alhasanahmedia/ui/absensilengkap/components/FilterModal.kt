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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alhasanah.alhasanahmedia.data.model.QuickFilter
import com.alhasanah.alhasanahmedia.util.AlhasanahGold

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterModal(
    currentFilter: QuickFilter,
    kegiatanList: List<String>,
    selectedKegiatan: Set<String>,
    onFilterSelected: (QuickFilter) -> Unit,
    onKegiatanToggle: (String, Boolean) -> Unit,
    onSelectAllKegiatan: (Boolean) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedQuickFilter by remember { mutableStateOf(currentFilter) }
    val selectedKegiatanState = remember { mutableStateListOf<String>().apply { addAll(selectedKegiatan) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter & Pilih Periode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Filters - Pilih Periode
            SectionHeader(title = "Pilih Periode")

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickFilterChip(
                    label = "Hari Ini",
                    isSelected = selectedQuickFilter == QuickFilter.HARI_INI,
                    onClick = {
                        selectedQuickFilter = QuickFilter.HARI_INI
                        onFilterSelected(QuickFilter.HARI_INI)
                    }
                )
                QuickFilterChip(
                    label = "Kemarin",
                    isSelected = selectedQuickFilter == QuickFilter.KEMARIN,
                    onClick = {
                        selectedQuickFilter = QuickFilter.KEMARIN
                        onFilterSelected(QuickFilter.KEMARIN)
                    }
                )
                QuickFilterChip(
                    label = "7 Hari Terakhir",
                    isSelected = selectedQuickFilter == QuickFilter._7_HARI,
                    onClick = {
                        selectedQuickFilter = QuickFilter._7_HARI
                        onFilterSelected(QuickFilter._7_HARI)
                    }
                )
                QuickFilterChip(
                    label = "30 Hari Terakhir",
                    isSelected = selectedQuickFilter == QuickFilter._30_HARI,
                    onClick = {
                        selectedQuickFilter = QuickFilter._30_HARI
                        onFilterSelected(QuickFilter._30_HARI)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Kegiatan Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "Filter Jenis Aktivitas")

                TextButton(
                    onClick = { onSelectAllKegiatan(selectedKegiatanState.size != kegiatanList.size) }
                ) {
                    Text(
                        text = if (selectedKegiatanState.size == kegiatanList.size) "Batalkan Semua" else "Pilih Semua",
                        color = AlhasanahGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            kegiatanList.forEach { kegiatan ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = kegiatan in selectedKegiatanState,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                selectedKegiatanState.add(kegiatan)
                            } else {
                                selectedKegiatanState.remove(kegiatan)
                            }
                            onKegiatanToggle(kegiatan, isChecked)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AlhasanahGold
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = kegiatan,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Apply & Close Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Reset button
                OutlinedButton(
                    onClick = {
                        selectedQuickFilter = QuickFilter._7_HARI
                        onFilterSelected(QuickFilter._7_HARI)
                        onSelectAllKegiatan(false)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Reset",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Apply button
                Button(
                    onClick = {
                        onApply()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlhasanahGold
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Terapkan",
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(text = label)
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AlhasanahGold,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(),
        shape = RoundedCornerShape(12.dp)
    ) {
        content()
    }
}
