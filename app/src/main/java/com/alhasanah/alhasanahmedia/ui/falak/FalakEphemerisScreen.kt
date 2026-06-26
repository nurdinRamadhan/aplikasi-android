package com.alhasanah.alhasanahmedia.ui.falak

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarian
import com.alhasanah.alhasanahmedia.data.model.falak.FalakHilalTable
import com.alhasanah.alhasanahmedia.data.model.falak.FalakIndeksItem
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.androidx.compose.koinViewModel

private val monthNames = listOf(
    "01" to "Januari",
    "02" to "Februari",
    "03" to "Maret",
    "04" to "April",
    "05" to "Mei",
    "06" to "Juni",
    "07" to "Juli",
    "08" to "Agustus",
    "09" to "September",
    "10" to "Oktober",
    "11" to "November",
    "12" to "Desember",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FalakEphemerisScreen(
    navController: NavController,
    viewModel: FalakEphemerisViewModel = koinViewModel(),
    onOpenHisabHilal: () -> Unit = {},
    onOpenGerhanaBulan: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(1) }
    val tabs = listOf("Paket", "Harian", "Hilal", "Hisab", "Visual", "Indeks")

    Scaffold(
        topBar = {
            AppPageHeader(
                title = "FALAQ EPHEMERIS",
                subtitle = state.data?.paket?.judul ?: "Data ephemeris resmi Kemenag",
                isDark = isAppInDarkTheme(),
                onBack = { navController.popBackStack() },
                size = AppPageHeaderSize.Compact,
                rightAction = {
                    IconButton(onClick = viewModel::sinkronkan, enabled = !state.syncing) {
                        if (state.syncing) {
                            CircularProgressIndicator(modifier = Modifier.padding(12.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Sinkronkan")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label, maxLines = 1) }
                    )
                }
            }

            when {
                state.loading -> LoadingContent()
                state.error != null && state.data == null -> ErrorContent(message = state.error.orEmpty(), onRetry = viewModel::sinkronkan)
                else -> when (selectedTab) {
                    0 -> PaketTab(state)
                    1 -> HarianTab(state = state, onTanggalChange = viewModel::pilihTanggal)
                    2 -> HilalTab(
                        state = state,
                        onSelect = viewModel::pilihHilal,
                        onLokasiQueryChange = viewModel::ubahLokasiQuery
                    )
                    3 -> HisabTab(
                        state = state,
                        onOpenHisabHilal = onOpenHisabHilal,
                        onOpenGerhanaBulan = onOpenGerhanaBulan,
                    )
                    4 -> VisualTab(
                        state = state,
                        onOpenHisabHilal = onOpenHisabHilal,
                        onOpenGerhanaBulan = onOpenGerhanaBulan,
                    )
                    5 -> IndeksTab(
                        state = state,
                        onQueryChange = viewModel::ubahQuery,
                        onTipeChange = viewModel::ubahFilterTipe,
                        onOpenItem = { item ->
                            item.tanggalData?.let {
                                viewModel.pilihTanggal(it)
                                selectedTab = 1
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text("Memuat dan memeriksa cache Ephemeris")
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Sinkronkan Ulang") }
    }
}

@Composable
private fun PaketTab(state: FalakEphemerisUiState) {
    val data = state.data ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                icon = Icons.Outlined.Dataset,
                title = data.paket.judul,
                subtitle = "Sumber ${data.paket.sumberResmi}, versi ${data.paket.versi}."
            )
        }
        item {
            InfoGrid(
                listOf(
                    "Status" to data.paket.status,
                    "Rentang" to "${data.paket.tanggalMulai} s.d. ${data.paket.tanggalSelesai}",
                    "Hari" to data.paket.jumlahHariEphemeris.toString(),
                    "Tabel Hilal" to data.paket.jumlahTabelHilal.toString(),
                    "Indeks" to data.paket.jumlahBarisIndeks.toString(),
                    "Zona Data" to data.paket.zonaWaktuData,
                    "Manifest" to data.paket.pathManifestStorage.orEmpty(),
                )
            )
        }
        items(data.manifest.berkas) { berkas ->
            DataCard {
                Text(berkas.namaTampil, fontWeight = FontWeight.Bold)
                Text(berkas.pathStorage, style = MaterialTheme.typography.bodySmall)
                Text(
                    "${berkas.jenisBerkas} - ${berkas.jumlahRecord} record - ${formatBytes(berkas.ukuranBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("SHA-256: ${berkas.sha256}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun HisabTab(
    state: FalakEphemerisUiState,
    onOpenHisabHilal: () -> Unit,
    onOpenGerhanaBulan: () -> Unit,
) {
    val data = state.data
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                icon = Icons.Filled.Calculate,
                title = "Hisab Ephemeris Awal Bulan",
                subtitle = "Perhitungan awal bulan berbasis data ephemeris resmi."
            )
        }
        item {
            DataCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                        Icon(
                            Icons.Filled.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hisab Awal Ephemeris Awal Bulan", fontWeight = FontWeight.Black)
                        Text(
                            "Hitung ijtima, ghurub, posisi hilal, kriteria, dan kesimpulan dengan data ephemeris resmi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                InfoGrid(
                    listOf(
                        "Sumber data" to (data?.paket?.judul ?: "Ephemeris aktif"),
                        "Zona data" to formatZonaData(data?.paket?.zonaWaktuData),
                        "Status" to (data?.paket?.status ?: "memuat"),
                    )
                )
                Button(onClick = onOpenHisabHilal, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Calculate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Hisab Hilal")
                }
            }
        }
        item {
            DataCard {
                Text("Perhitungan Klasik", fontWeight = FontWeight.Bold)
                Text(
                    "Ruang ini disiapkan untuk metode klasik seperti tabel pesantren atau metode lain setelah modul ephemeris selesai.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            DataCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)) {
                        Icon(
                            Icons.Filled.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hisab Gerhana Bulan Ephemeris", fontWeight = FontWeight.Black)
                        Text(
                            "Hitung FIB terbesar, istiqbal, lintang Bulan, bayangan inti Bumi, jenis gerhana, waktu kontak, dan magnitude.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(onClick = onOpenGerhanaBulan, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Calculate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Gerhana Bulan")
                }
            }
        }
    }
}

@Composable
private fun VisualTab(
    state: FalakEphemerisUiState,
    onOpenHisabHilal: () -> Unit,
    onOpenGerhanaBulan: () -> Unit,
) {
    val data = state.data
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                icon = Icons.Outlined.Visibility,
                title = "Visualisasi Falak",
                subtitle = "Visual 2D membaca hasil hisab ephemeris. Rumus utama tetap berada di kalkulator dan rincian perhitungan."
            )
        }
        item {
            DataCard {
                Text("Visual Hilal", fontWeight = FontWeight.Black)
                Text(
                    "Tampilkan ufuk, posisi Matahari, posisi hilal, garis elongasi, iluminasi, dan status kriteria setelah Hisab Hilal dihitung.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                InfoGrid(
                    listOf(
                        "Sumber utama" to (data?.paket?.judul ?: "Ephemeris aktif"),
                        "Model visual" to "Compose Canvas 2D",
                        "Status" to "Siap dihitung dari hasil Hisab Hilal",
                    )
                )
                Button(onClick = onOpenHisabHilal, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Visual Hilal")
                }
            }
        }
        item {
            DataCard {
                Text("Visual Gerhana Bulan", fontWeight = FontWeight.Black)
                Text(
                    "Tampilkan penumbra, umbra, lintasan Bulan, titik kontak, magnitude, dan pembanding Meeus setelah Gerhana Bulan dihitung.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                InfoGrid(
                    listOf(
                        "Sumber utama" to (data?.paket?.judul ?: "Ephemeris aktif"),
                        "Pembanding" to "Jean Meeus",
                        "Model visual" to "Compose Canvas 2D",
                    )
                )
                Button(onClick = onOpenGerhanaBulan, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Visual Gerhana Bulan")
                }
            }
        }
        item {
            DataCard {
                Text("Deteksi dan Visual Lintas Tahun", fontWeight = FontWeight.Bold)
                Text(
                    "Tahap berikutnya: daftar kandidat gerhana 2-10 tahun dari Meeus, lalu hitung ulang dengan Kemenag jika paket resmi tersedia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HarianTab(state: FalakEphemerisUiState, onTanggalChange: (String) -> Unit) {
    val data = state.data ?: return
    val datesByMonth = remember(data.ephemerisHarian) {
        data.ephemerisHarian
            .map { it.date }
            .groupBy { it.substring(5, 7) }
    }
    val selectedMonth = state.tanggalDipilih.substring(5, 7)
    val selectedMonthName = monthNames.firstOrNull { it.first == selectedMonth }?.second ?: selectedMonth
    val datesInMonth = datesByMonth[selectedMonth].orEmpty()
    var monthMenuOpen by remember { mutableIntStateOf(0) }
    var dateMenuOpen by remember { mutableIntStateOf(0) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(Icons.Outlined.CalendarMonth, "Data Harian Matahari dan Bulan", "Tabel lengkap per jam GMT/UT dari PDF Kemenag, termasuk nilai asli dan hasil konversi.")
        }
        item {
            DataCard {
                Text("Pilih Data Harian", fontWeight = FontWeight.Bold)
                Text(
                    "Gunakan pilihan bulan dan tanggal agar data yang dibuka selalu sesuai daftar tanggal dalam paket Kemenag.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Bulan", style = MaterialTheme.typography.labelMedium)
                            Button(onClick = { monthMenuOpen = 1 }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedMonthName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        DropdownMenu(expanded = monthMenuOpen == 1, onDismissRequest = { monthMenuOpen = 0 }) {
                            monthNames.forEach { (month, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        monthMenuOpen = 0
                                        datesByMonth[month]?.firstOrNull()?.let(onTanggalChange)
                                    }
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Tanggal", style = MaterialTheme.typography.labelMedium)
                            Button(onClick = { dateMenuOpen = 1 }, modifier = Modifier.fillMaxWidth()) {
                                Text(state.tanggalDipilih.substring(8, 10), maxLines = 1)
                            }
                        }
                        DropdownMenu(expanded = dateMenuOpen == 1, onDismissRequest = { dateMenuOpen = 0 }) {
                            datesInMonth.forEach { date ->
                                DropdownMenuItem(
                                    text = { Text("${date.substring(8, 10)} $selectedMonthName") },
                                    onClick = {
                                        dateMenuOpen = 0
                                        onTanggalChange(date)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        state.ephemerisDipilih?.let { block ->
            item { EphemerisBlock(block) }
        } ?: item {
            Text("Tanggal tidak ditemukan pada paket ini.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun EphemerisBlock(block: FalakEphemerisHarian) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DataCard {
            Text("Tanggal ${block.date}", fontWeight = FontWeight.Black)
            Text(
                "Data ditampilkan mengikuti susunan tabel Ephemeris Kemenag. Geser tabel ke samping untuk melihat semua kolom.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DataCard {
            SectionHeader(Icons.Outlined.TableRows, "Data Matahari", "Bujur, lintang, asensio rekta, deklinasi, jarak, semi diameter, obliquity, dan perata waktu.")
            EphemerisSunTable(block.hourlyTable.sun)
        }
        DataCard {
            SectionHeader(Icons.Outlined.TableRows, "Data Bulan", "Bujur, lintang, asensio rekta, deklinasi, paralaks, semi diameter, sudut cahaya, dan fraksi iluminasi.")
            EphemerisMoonTable(block.hourlyTable.moon)
        }
    }
}

@Composable
private fun EphemerisSunTable(rows: List<JsonObject>) {
    ScrollTable(
        headers = listOf(
            "Jam GMT/UT\nHour\nالساعة" to 112,
            "Bujur Ekliptika Tampak\nApparent Ecliptic Longitude\nالطول الكسوفي الظاهري" to 210,
            "Lintang Ekliptika Tampak\nApparent Ecliptic Latitude\nالعرض الكسوفي الظاهري" to 210,
            "Asensio Rekta Tampak\nApparent Right Ascension\nالمطلع المستقيم الظاهري" to 210,
            "Deklinasi Tampak\nApparent Declination\nالميل الظاهري" to 190,
            "Jarak Geosentris Sejati\nTrue Geocentric Distance\nالبعد المركزي الأرضي الحقيقي" to 220,
            "Semi Diameter\nSemi Diameter\nنصف القطر الظاهري" to 170,
            "Kemiringan Ekliptika Sejati\nTrue Obliquity\nميل فلك البروج الحقيقي" to 210,
            "Perata Waktu\nEquation of Time\nمعادلة الزمن" to 180,
        ),
        rows = rows.map { row ->
            listOf(
                row.text("hour_label"),
                row.raw("apparent_ecliptic_longitude"),
                row.raw("apparent_ecliptic_latitude"),
                row.raw("apparent_right_ascension"),
                row.raw("apparent_declination"),
                row.text("true_geocentric_distance_au"),
                row.raw("semi_diameter"),
                row.raw("true_obliquity"),
                row.raw("equation_of_time"),
            )
        }
    )
}

@Composable
private fun EphemerisMoonTable(rows: List<JsonObject>) {
    ScrollTable(
        headers = listOf(
            "Jam GMT/UT\nHour\nالساعة" to 112,
            "Bujur Bulan Tampak\nApparent Longitude\nالطول الظاهري للقمر" to 200,
            "Lintang Bulan Tampak\nApparent Latitude\nالعرض الظاهري للقمر" to 200,
            "Asensio Rekta Tampak\nApparent Right Ascension\nالمطلع المستقيم الظاهري" to 210,
            "Deklinasi Tampak\nApparent Declination\nالميل الظاهري" to 190,
            "Paralaks Horizontal\nHorizontal Parallax\nاختلاف المنظر الأفقي" to 200,
            "Semi Diameter\nSemi Diameter\nنصف القطر الظاهري" to 170,
            "Sudut Cahaya\nAngle Bright Limb\nزاوية الطرف المضيء" to 190,
            "Fraksi Iluminasi\nFraction Illumination\nنسبة الإضاءة" to 190,
        ),
        rows = rows.map { row ->
            listOf(
                row.text("hour_label"),
                row.raw("apparent_longitude"),
                row.raw("apparent_latitude"),
                row.raw("apparent_right_ascension"),
                row.raw("apparent_declination"),
                row.raw("horizontal_parallax"),
                row.raw("semi_diameter"),
                row.raw("angle_bright_limb"),
                "${row.text("fraction_illumination_percent")}%",
            )
        }
    )
}

@Composable
private fun HilalTab(
    state: FalakEphemerisUiState,
    onSelect: (Int) -> Unit,
    onLokasiQueryChange: (String) -> Unit,
) {
    val data = state.data ?: return
    val table = state.hilalDipilih
    val locations = remember(table) {
        table?.rows.orEmpty()
            .mapNotNull { it.text("location").takeIf(String::isNotBlank) }
            .distinct()
    }
    var locationMenuOpen by remember { mutableIntStateOf(0) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionHeader(Icons.Outlined.LocationOn, "Data Hilal Lokasi", "Pilih bulan Hijriah dan daerah yang tersedia pada tabel Kemenag. Tabel asli tetap ditampilkan lengkap sesuai sumber data.") }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.hilalLokasi.forEachIndexed { index, table ->
                    FilterChip(
                        selected = index == state.hilalIndexDipilih,
                        onClick = { onSelect(index) },
                        label = { Text(table.hijriMonthRaw ?: "Tabel ${index + 1}") }
                    )
                }
            }
        }
        item {
            DataCard {
                Text("Pilih Daerah", fontWeight = FontWeight.Bold)
                Text(
                    "Daftar ini hanya berisi daerah yang tersedia di PDF Kemenag untuk tabel hilal terpilih.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        Button(onClick = { locationMenuOpen = 1 }, modifier = Modifier.fillMaxWidth()) {
                            Text(state.lokasiQuery.ifBlank { "Semua daerah" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(expanded = locationMenuOpen == 1, onDismissRequest = { locationMenuOpen = 0 }) {
                            DropdownMenuItem(
                                text = { Text("Semua daerah") },
                                onClick = {
                                    locationMenuOpen = 0
                                    onLokasiQueryChange("")
                                }
                            )
                            locations.forEach { location ->
                                DropdownMenuItem(
                                    text = { Text(location) },
                                    onClick = {
                                        locationMenuOpen = 0
                                        onLokasiQueryChange(location)
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.lokasiQuery,
                        onValueChange = onLokasiQueryChange,
                        label = { Text("Filter cepat") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        state.hilalDipilih?.let { table ->
            item { HilalTableCard(table, state.lokasiQuery) }
        }
    }
}

@Composable
private fun HilalTableCard(table: FalakHilalTable, lokasiQuery: String) {
    val query = lokasiQuery.trim().lowercase()
    val rows = if (query.isBlank()) table.rows else table.rows.filter { it.text("location").lowercase().contains(query) }
    DataCard {
        Text(table.hijriMonthRaw ?: "Tabel Hilal", fontWeight = FontWeight.Black)
        Text(table.eventDateRaw.orEmpty())
        Text("Ijtimak: ${table.ijtimaRaw.orEmpty()}")
        Text("Menampilkan ${rows.size} dari ${table.rows.size} daerah.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        ScrollTable(
            headers = listOf(
                "No" to 48,
                "Daerah" to 150,
                "Matahari Terbenam" to 150,
                "Bulan Terbenam" to 140,
                "Azimut Matahari" to 136,
                "Azimut Bulan" to 128,
                "Tinggi Bulan" to 128,
                "Elongasi" to 112,
                "FI" to 72,
            ),
            rows = rows.map { row ->
                listOf(
                    row.text("no"),
                    row.text("location"),
                    row.time("sunset"),
                    row.time("moonset"),
                    row.angle("sun_azimuth"),
                    row.angle("moon_azimuth"),
                    row.angle("moon_altitude"),
                    row.angle("elongation"),
                    row.text("fraction_illumination"),
                )
            }
        )
    }
}

@Composable
private fun IndeksTab(
    state: FalakEphemerisUiState,
    onQueryChange: (String) -> Unit,
    onTipeChange: (String?) -> Unit,
    onOpenItem: (FalakIndeksItem) -> Unit,
) {
    val filters = listOf(null to "Semua", "tanggal" to "Tanggal", "jam_ut" to "Jam GMT/UT", "lokasi" to "Lokasi", "bulan_hijriah" to "Hijriah")
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionHeader(Icons.Outlined.Search, "Indeks Pencarian", "Cari tanggal, jam GMT/UT, lokasi hilal, atau bulan Hijriah.") }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("Cari data ephemeris") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { (value, label) ->
                    FilterChip(
                        selected = state.tipeFilter == value,
                        onClick = { onTipeChange(value) },
                        label = { Text(label) }
                    )
                }
            }
        }
        items(state.hasilIndeks) { item ->
            DataCard(onClick = { onOpenItem(item) }) {
                Text(item.judul, fontWeight = FontWeight.Bold)
                Text(item.ringkasan.orEmpty(), style = MaterialTheme.typography.bodySmall)
                Text(
                    listOfNotNull(item.tipeIndeks, item.tanggalData, item.jamUt?.let { "$it GMT/UT" }, item.namaLokasi).joinToString(" - "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                item.pathJsonPointer?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DataCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val modifier = Modifier.fillMaxWidth()
    val shape = RoundedCornerShape(8.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    if (onClick == null) {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
        }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation, onClick = onClick) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
        }
    }
}

@Composable
private fun InfoGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, modifier = Modifier.weight(0.38f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, modifier = Modifier.weight(0.62f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ScrollTable(headers: List<Pair<String, Int>>, rows: List<List<String>>) {
    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Row {
            headers.forEach { (title, width) ->
                TableCell(title, width, header = true)
            }
        }
        rows.forEach { row ->
            Row {
                headers.forEachIndexed { index, (_, width) ->
                    TableCell(row.getOrElse(index) { "-" }, width, header = false, muted = index == 0)
                }
            }
        }
    }
}

@Composable
private fun TableCell(text: String, width: Int, header: Boolean = false, muted: Boolean = false) {
    Surface(
        modifier = Modifier
            .width(width.dp)
            .padding(end = 1.dp, bottom = 1.dp),
        color = when {
            header -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            muted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
        },
        shape = RoundedCornerShape(3.dp)
    ) {
        SelectionContainer {
            Text(
                text = text.ifBlank { "-" },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                fontWeight = if (header || muted) FontWeight.Bold else FontWeight.Normal,
                maxLines = if (header) 4 else 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun JsonObject.text(key: String): String =
    this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

private fun JsonObject.raw(key: String): String =
    runCatching { this[key]?.jsonObject?.get("raw")?.jsonPrimitive?.contentOrNull }.getOrNull().orEmpty()

private fun JsonObject.time(key: String): String {
    val obj = runCatching { this[key]?.jsonObject }.getOrNull() ?: return "-"
    val time = obj["time"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val zone = obj["timezone"]?.jsonPrimitive?.contentOrNull.orEmpty()
    return listOf(time, zone).filter { it.isNotBlank() }.joinToString(" ")
}

private fun JsonObject.angle(key: String): String {
    val obj = runCatching { this[key]?.jsonObject }.getOrNull() ?: return "-"
    val degree = obj["degree"]?.jsonPrimitive?.doubleOrNull
    val minute = obj["minute"]?.jsonPrimitive?.doubleOrNull
    val decimal = obj["decimal_degree"]?.jsonPrimitive?.doubleOrNull
    val main = if (degree != null && minute != null) {
        "%s° %.2f'".format(degree.toInt(), minute)
    } else {
        "-"
    }
    return if (decimal != null) "$main (${"%.4f".format(decimal)}°)" else main
}


private fun formatBytes(value: Long): String {
    if (value < 1024) return "$value B"
    val kb = value / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    return "%.1f MB".format(kb / 1024.0)
}

private fun formatZonaData(value: String?): String =
    if (value.equals("UT", ignoreCase = true)) "GMT/UT" else value ?: "GMT/UT"
