package com.alhasanah.alhasanahmedia.ui.absensilengkap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alhasanah.alhasanahmedia.ui.absensilengkap.components.CalendarGrid
import com.alhasanah.alhasanahmedia.ui.absensilengkap.components.DaftarAktivitasList
import com.alhasanah.alhasanahmedia.ui.absensilengkap.components.DateRangeSelector
import com.alhasanah.alhasanahmedia.ui.absensilengkap.components.FilterBottomSheet
import com.alhasanah.alhasanahmedia.ui.absensilengkap.components.GrafikKehadiranCard
import com.alhasanah.alhasanahmedia.ui.absensilengkap.components.ProfileHeaderCard
import com.alhasanah.alhasanahmedia.ui.absensilengkap.components.RingkasanCards
import com.alhasanah.alhasanahmedia.data.model.ViewMode
import com.alhasanah.alhasanahmedia.util.AlhasanahGold
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsensiLengkapScreen(
    santriNis: String,
    viewModel: AbsensiLengkapViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentStartDate by viewModel.currentStartDate.collectAsState()
    val currentEndDate by viewModel.currentEndDate.collectAsState()
    val currentFilter by viewModel.quickFilter.collectAsState()
    val selectedKegiatan by viewModel.selectedKegiatanFilters.collectAsState()
    val selectedStatusFilters by viewModel.selectedStatusFilters.collectAsState()
    val selectedSourceFilters by viewModel.selectedSourceFilters.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }

    // Load data on first composition
    LaunchedEffect(santriNis) {
        viewModel.loadAbsensi(santriNis)
    }

    // Auto-sync calendar month when date range changes
    LaunchedEffect(currentStartDate, currentEndDate) {
        currentYearMonth = YearMonth.from(currentStartDate)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Absensi Santri",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (selectedSourceFilters.isNotEmpty() || selectedStatusFilters.isNotEmpty()) {
                                AlhasanahGold
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is AbsensiLengkapUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AlhasanahGold)
                }
            }

            is AbsensiLengkapUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Gagal memuat data",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            is AbsensiLengkapUiState.Success -> {
                val data = state.data

                // Build attendance data map, filtered by actual API date range
                val apiStartDate = try { LocalDate.parse(data.startDate) } catch (_: Exception) { currentStartDate }
                val apiEndDate = try { LocalDate.parse(data.endDate) } catch (_: Exception) { currentEndDate }

                val attendanceData = remember(data.perHari, apiStartDate, apiEndDate) {
                    data.perHari.associate { hari ->
                        val date = LocalDate.parse(hari.tanggal)
                        date to hari.kegiatan.map { it.status }
                    }.filterKeys { date ->
                        !date.isBefore(apiStartDate) && !date.isAfter(apiEndDate)
                    }
                }

                // Hitung jumlah hari
                val totalDays = remember(apiStartDate, apiEndDate) {
                    java.time.temporal.ChronoUnit.DAYS.between(apiStartDate, apiEndDate).toInt() + 1
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ProfileHeaderCard(
                            nama = data.santriNama,
                            kelas = data.kelas,
                            nis = data.santriNis,
                            persentase = data.ringkasan.persentase,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        DateRangeSelector(
                            dateRange = viewModel.getDisplayDateRange(),
                            onPreviousClick = { viewModel.navigateWeek(isNext = false) },
                            onNextClick = { viewModel.navigateWeek(isNext = true) },
                            onCalendarClick = { showCalendar = !showCalendar },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        RingkasanCards(
                            ringkasan = data.ringkasan,
                            totalDays = totalDays,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        GrafikKehadiranCard(
                            dataHari = data.perHari,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Calendar View (toggle)
                    if (showCalendar) {
                        item {
                            CalendarGrid(
                                yearMonth = currentYearMonth,
                                attendanceData = attendanceData,
                                onDayClick = { date ->
                                    // Navigate date range to show this date
                                },
                                onMonthChange = { newMonth ->
                                    currentYearMonth = newMonth
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Mode Tampilan Toggle
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ModeTampilanButton(
                                label = "Harian",
                                isSelected = viewMode == ViewMode.HARIAN,
                                onClick = { viewModel.setViewMode(ViewMode.HARIAN) },
                                modifier = Modifier.weight(1f)
                            )
                            ModeTampilanButton(
                                label = "Semua Kegiatan",
                                isSelected = viewMode == ViewMode.SEMUA_KEGIATAN,
                                onClick = { viewModel.setViewMode(ViewMode.SEMUA_KEGIATAN) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Daftar Aktivitas
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            // Filter chips for quick access
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedSourceFilters.isEmpty(),
                                    onClick = { showFilterSheet = true },
                                    label = {
                                        Text(
                                            text = "Jenis: Semua",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AlhasanahGold.copy(alpha = 0.15f),
                                        selectedLabelColor = AlhasanahGold
                                    )
                                )
                                FilterChip(
                                    selected = selectedStatusFilters.isEmpty(),
                                    onClick = { showFilterSheet = true },
                                    label = {
                                        Text(
                                            text = "Status: Semua",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AlhasanahGold.copy(alpha = 0.15f),
                                        selectedLabelColor = AlhasanahGold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Daftar Aktivitas
                            when (viewMode) {
                                ViewMode.HARIAN -> {
                                    DaftarAktivitasList(
                                        perHari = data.perHari,
                                        getStatusColor = { status -> viewModel.getStatusColor(status) },
                                        getStatusLabel = { status -> viewModel.getStatusLabel(status) }
                                    )
                                }
                                ViewMode.SEMUA_KEGIATAN -> {
                                    // Flat list semua kegiatan
                                    SemuaKegiatanList(
                                        perHari = data.perHari,
                                        getStatusColor = { status -> viewModel.getStatusColor(status) },
                                        getStatusLabel = { status -> viewModel.getStatusLabel(status) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // Filter Bottom Sheet
        if (showFilterSheet) {
            FilterBottomSheet(
                currentFilter = currentFilter,
                selectedSourceFilters = selectedSourceFilters,
                selectedStatusFilters = selectedStatusFilters,
                viewMode = viewMode,
                onQuickFilterToggle = { filter ->
                    viewModel.setQuickFilter(filter)
                },
                onSourceFilterToggle = { source, isSelected ->
                    viewModel.setSourceFilter(source, isSelected)
                },
                onStatusFilterToggle = { status, isSelected ->
                    viewModel.setStatusFilter(status, isSelected)
                },
                onViewModeChange = { mode ->
                    viewModel.setViewMode(mode)
                },
                onApplyFilters = {
                    viewModel.applyFilters()
                },
                onResetFilters = {
                    viewModel.resetAllFilters()
                },
                onDismiss = {
                    showFilterSheet = false
                }
            )
        }
    }
}

@Composable
private fun ModeTampilanButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
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

@Composable
private fun SemuaKegiatanList(
    perHari: List<com.alhasanah.alhasanahmedia.data.model.HariAbsensi>,
    getStatusColor: (String) -> Long,
    getStatusLabel: (String) -> String
) {
    // Flatten all activities into a single list sorted by date
    val allActivities = remember(perHari) {
        perHari.flatMap { hari ->
            hari.kegiatan.map { kegiatan ->
                Triple(hari.tanggalDisplay, hari.hari, kegiatan)
            }
        }.sortedBy { it.first }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allActivities.forEach { ( tanggalDisplay, hari, kegiatan) ->
            val statusColor = androidx.compose.ui.graphics.Color(getStatusColor(kegiatan.status))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(40.dp)
                    ) {
                        Text(
                            text = tanggalDisplay.split(" ")[0],
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tanggalDisplay.split(" ").getOrElse(1) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Column {
                        Text(
                            text = kegiatan.nama,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (kegiatan.sesi.isNotEmpty()) {
                            Text(
                                text = kegiatan.sesi,
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
    }
}
