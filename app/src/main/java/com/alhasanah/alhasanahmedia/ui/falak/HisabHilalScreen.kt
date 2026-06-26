package com.alhasanah.alhasanahmedia.ui.falak

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import com.alhasanah.alhasanahmedia.domain.falak.ButirPerhitunganFalak
import com.alhasanah.alhasanahmedia.domain.falak.HasilHisabHilalEphemeris
import com.alhasanah.alhasanahmedia.domain.falak.KriteriaAwalBulanFalak
import com.alhasanah.alhasanahmedia.domain.falak.VisualHilalMapper
import com.alhasanah.alhasanahmedia.domain.falak.WaktuFalak
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong
import org.koin.androidx.compose.koinViewModel
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisabHilalScreen(
    navController: NavController,
    viewModel: HisabHilalViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.deteksiMarkaz()
    }

    Scaffold(
        topBar = {
            AppPageHeader(
                title = "HISAB HILAL",
                subtitle = "Perhitungan awal bulan dari data ephemeris resmi",
                isDark = isAppInDarkTheme(),
                onBack = { navController.popBackStack() },
                size = AppPageHeaderSize.Compact
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HisabInputCard(
                    state = state,
                    onBulanHijriahChange = viewModel::ubahBulanHijriah,
                    onAcuanChange = viewModel::pilihAcuanKemenag,
                    onModeTanggalChange = viewModel::ubahModeTanggal,
                    onTanggalChange = viewModel::ubahTanggalSituasi,
                    onMarkazChange = viewModel::ubahMarkaz,
                    onKriteriaChange = viewModel::ubahKriteria,
                    onDetectLocation = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onOpenMap = { input ->
                        openMarkazMap(context = context, input = input)
                    },
                    onHitung = viewModel::hitung
                )
            }

            state.error?.let { message ->
                item {
                    AlertCard(message = message)
                }
            }

            if (state.loading) {
                item {
                    LoadingCard("Menyiapkan data ephemeris dan menghitung hisab hilal")
                }
            }

            state.hasil?.let { hasil ->
                item { VisualHilalCard(VisualHilalMapper().map(hasil)) }
                item { KesimpulanCard(hasil) }
                item { EvaluasiKriteriaCard(hasil, state) }
                item { RingkasanDataCard(hasil) }
                item {
                    SectionTitle("Rincian Perhitungan", "Buka setiap butir untuk melihat rumus, substitusi, hasil, dan sumber data.")
                }
                items(hasil.butirPerhitungan) { butir ->
                    ButirAccordion(butir)
                }
            }
        }
    }
}

@Composable
private fun HisabInputCard(
    state: HisabHilalUiState,
    onBulanHijriahChange: (String) -> Unit,
    onAcuanChange: (AcuanAwalBulanKemenag) -> Unit,
    onModeTanggalChange: (ModeTanggalHisab) -> Unit,
    onTanggalChange: (String) -> Unit,
    onMarkazChange: (MarkazInput) -> Unit,
    onKriteriaChange: (KriteriaAwalBulanFalak) -> Unit,
    onDetectLocation: () -> Unit,
    onOpenMap: (MarkazInput) -> Unit,
    onHitung: () -> Unit,
) {
    val acuanTerpilih = remember(state.bulanHijriah) {
        HisabHilalViewModel.acuanKemenag2026.firstOrNull {
            it.bulanHijriah.equals(state.bulanHijriah.trim(), ignoreCase = true)
        }
    }
    DataCard {
        SectionTitle("Parameter Hisab", "Pilih bulan target. Tanggal rukyat adalah tanggal 29 bulan sebelumnya, bukan tanggal awal bulan.")
        AcuanKemenagPicker(
            selected = acuanTerpilih,
            onSelect = onAcuanChange,
        )
        ModeTanggalSelector(selected = state.modeTanggal, onSelect = onModeTanggalChange)
        OutlinedTextField(
            value = state.bulanHijriah,
            onValueChange = onBulanHijriahChange,
            label = { Text("Bulan Target Hijriah") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.tanggalSituasiHilal,
            onValueChange = onTanggalChange,
            label = { Text("Tanggal Rukyat / 29 Bulan Sebelumnya") },
            placeholder = { Text("2026-02-17") },
            singleLine = true,
            enabled = state.modeTanggal == ModeTanggalHisab.INPUT_MANUAL,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )
        acuanTerpilih?.let { acuan ->
            TanggalAcuanInfo(
                acuan = acuan,
                tanggalSaatIni = state.tanggalSituasiHilal,
                onGunakanAcuan = { onTanggalChange(acuan.tanggalRukyatMasehi) },
            )
        }
        MarkazForm(
            input = state.markazInput,
            sumberMarkaz = state.sumberMarkaz,
            onChange = onMarkazChange,
            onDetectLocation = onDetectLocation,
            onOpenMap = onOpenMap,
            detecting = state.detectingLocation,
        )
        KriteriaPicker(selected = state.kriteria, onSelect = onKriteriaChange)
        Button(
            onClick = onHitung,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Calculate, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Hitung Awal Bulan")
        }
    }
}

@Composable
private fun ModeTanggalSelector(
    selected: ModeTanggalHisab,
    onSelect: (ModeTanggalHisab) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Sumber Tanggal Rukyat", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = selected == ModeTanggalHisab.ACUAN_KEMENAG,
                onClick = { onSelect(ModeTanggalHisab.ACUAN_KEMENAG) },
                label = { Text("Acuan Kemenag") },
            )
            FilterChip(
                selected = selected == ModeTanggalHisab.INPUT_MANUAL,
                onClick = { onSelect(ModeTanggalHisab.INPUT_MANUAL) },
                label = { Text("Input Manual") },
            )
        }
        Text(
            text = if (selected == ModeTanggalHisab.ACUAN_KEMENAG) {
                "Tanggal rukyat dikunci dari acuan penentu awal bulan Kemenag."
            } else {
                "Tanggal dapat diubah untuk latihan, koreksi, atau pembandingan. Aplikasi tetap menilai hasil berdasarkan kriteria yang dipilih."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AcuanKemenagPicker(
    selected: AcuanAwalBulanKemenag?,
    onSelect: (AcuanAwalBulanKemenag) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Acuan Awal Bulan Kemenag 2026", fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.bulanHijriah ?: "Pilih dari tabel penentu awal bulan", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                HisabHilalViewModel.acuanKemenag2026.forEach { acuan ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(acuan.bulanHijriah, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Rukyat ${formatTanggalSingkat(acuan.tanggalRukyatMasehi)}; prediksi ${formatTanggalSingkat(acuan.prediksiAwalBulanMasehi)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        onClick = {
                            open = false
                            onSelect(acuan)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TanggalAcuanInfo(
    acuan: AcuanAwalBulanKemenag,
    tanggalSaatIni: String,
    onGunakanAcuan: () -> Unit,
) {
    val sesuaiAcuan = tanggalSaatIni.trim() == acuan.tanggalRukyatMasehi
    val warna = if (sesuaiAcuan) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val teks = if (sesuaiAcuan) {
        "Acuan Kemenag: rukyat ${formatTanggalLengkap(acuan.tanggalRukyatMasehi)}, ijtimak ${acuan.ijtimakWib}, prediksi awal bulan ${formatTanggalLengkap(acuan.prediksiAwalBulanMasehi)}."
    } else {
        "Tanggal rukyat tidak sesuai acuan Kemenag untuk ${acuan.bulanHijriah}. Gunakan ${formatTanggalLengkap(acuan.tanggalRukyatMasehi)}; jika memakai ${formatTanggalLengkapOrRaw(tanggalSaatIni)}, hasil akan dihitung sebagai tanggal rukyat manual."
    }
    Surface(shape = RoundedCornerShape(8.dp), color = warna) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = teks,
                style = MaterialTheme.typography.bodySmall,
                color = if (sesuaiAcuan) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
            )
            if (!sesuaiAcuan) {
                OutlinedButton(onClick = onGunakanAcuan, modifier = Modifier.fillMaxWidth()) {
                    Text("Gunakan Tanggal Rukyat Kemenag")
                }
            }
        }
    }
}

@Composable
private fun MarkazForm(
    input: MarkazInput,
    sumberMarkaz: String?,
    onChange: (MarkazInput) -> Unit,
    onDetectLocation: () -> Unit,
    onOpenMap: (MarkazInput) -> Unit,
    detecting: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Markaz", fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = input.nama,
            onValueChange = { onChange(input.copy(nama = it)) },
            label = { Text("Nama Markaz") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                label = "Lintang",
                value = input.lintang,
                onValueChange = { onChange(input.copy(lintang = it)) },
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "Bujur",
                value = input.bujur,
                onValueChange = { onChange(input.copy(bujur = it)) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                label = "Elevasi (m)",
                value = input.elevasi,
                onValueChange = { onChange(input.copy(elevasi = it)) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = input.zona,
                onValueChange = { onChange(input.copy(zona = it.uppercase(Locale.US))) },
                label = { Text("Zona") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onDetectLocation,
                enabled = !detecting,
                modifier = Modifier.weight(1f)
            ) {
                if (detecting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(18.dp)
                            .height(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Filled.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text("GPS")
            }
            OutlinedButton(
                onClick = { onOpenMap(input) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Map, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Buka Map")
            }
        }
        Text(
            "Gunakan GPS untuk mengisi koordinat otomatis. Tombol Map membuka titik markaz pada aplikasi peta agar posisi dapat diperiksa, lalu lintang, bujur, dan elevasi tetap bisa dikoreksi manual.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        sumberMarkaz?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun KriteriaPicker(
    selected: KriteriaAwalBulanFalak,
    onSelect: (KriteriaAwalBulanFalak) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val options = listOf(
        KriteriaAwalBulanFalak.KemenagMabimsTerbaru,
        KriteriaAwalBulanFalak.HisabWujudulHilal,
        KriteriaAwalBulanFalak.TanpaKriteria,
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Kriteria", fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected.nama, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(option.nama, fontWeight = FontWeight.SemiBold)
                                Text(kriteriaLabel(option), style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        onClick = {
                            open = false
                            onSelect(option)
                        }
                    )
                }
            }
        }
        Text(kriteriaLabel(selected), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KesimpulanCard(hasil: HasilHisabHilalEphemeris) {
    val kesimpulan = hasil.kesimpulan
    DataCard(
        colors = CardDefaults.cardColors(
            containerColor = if (kesimpulan.memenuhiKriteria) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Assessment, contentDescription = null)
            Text("Kesimpulan", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        }
        Text(kesimpulan.statusPrakiraan, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(kesimpulan.catatan, style = MaterialTheme.typography.bodySmall)
        InfoRows(
            listOf(
                "Tanggal prakiraan" to formatTanggalLengkap(kesimpulan.tanggalPrakiraanAwalBulanMasehi),
                "Kriteria" to kesimpulan.kriteriaAwalBulan.nama,
                "Tinggi Hilal Mar'i Tepi Atas" to formatDerajat(kesimpulan.tinggiHilalMariDerajat),
                "Elongasi Geosentrik" to formatDerajat(kesimpulan.elongasiHilalDerajat),
                "Lama Hilal" to formatJamDurasi(kesimpulan.lamaHilalJam),
            )
        )
    }
}

@Composable
private fun EvaluasiKriteriaCard(hasil: HasilHisabHilalEphemeris, state: HisabHilalUiState) {
    val kesimpulan = hasil.kesimpulan
    val acuan = HisabHilalViewModel.acuanKemenag2026.firstOrNull {
        it.bulanHijriah.equals(state.bulanHijriah.trim(), ignoreCase = true)
    }
    DataCard {
        SectionTitle("Evaluasi Kriteria", "Pemeriksaan ini menjelaskan mengapa hasil memenuhi atau belum memenuhi kriteria.")
        InfoRows(
            listOfNotNull(
                "Mode tanggal" to if (state.modeTanggal == ModeTanggalHisab.ACUAN_KEMENAG) "Acuan Kemenag" else "Input manual",
                "Tanggal rukyat dihitung" to formatTanggalLengkap(kesimpulan.tanggalSituasiHilalMasehi),
                acuan?.let { "Tanggal rukyat acuan" to formatTanggalLengkap(it.tanggalRukyatMasehi) },
                acuan?.let { "Prediksi acuan Kemenag" to formatTanggalLengkap(it.prediksiAwalBulanMasehi) },
                "Ijtimak sebelum/saat ghurub" to statusMemenuhi(kesimpulan.ijtimaSebelumGhurub),
                "Tinggi Hilal Mar'i Tepi Atas" to "${formatDerajat(kesimpulan.tinggiHilalMariDerajat)} (${statusMemenuhi(kesimpulan.tinggiHilalMemenuhi)})",
                "Elongasi Geosentrik" to "${formatDerajat(kesimpulan.elongasiHilalDerajat)} (${statusMemenuhi(kesimpulan.elongasiMemenuhi)})",
                "Status kriteria" to if (kesimpulan.memenuhiKriteria) "Memenuhi ${kesimpulan.kriteriaAwalBulan.nama}" else "Belum memenuhi ${kesimpulan.kriteriaAwalBulan.nama}",
            )
        )
        acuan?.let {
            if (state.modeTanggal == ModeTanggalHisab.INPUT_MANUAL && state.tanggalSituasiHilal.trim() != it.tanggalRukyatMasehi) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        text = "Catatan: tanggal manual berbeda dari tabel Kemenag untuk ${it.bulanHijriah}. Ini sah untuk belajar dan koreksi, tetapi kesimpulan tidak boleh dibaca sebagai prediksi resmi Kemenag.",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun RingkasanDataCard(hasil: HasilHisabHilalEphemeris) {
    DataCard {
        SectionTitle("Ringkasan Hasil", "Nilai ini berasal dari rangkaian butir perhitungan, bukan input manual.")
        InfoRows(
            listOf(
                "Ijtimak" to formatWaktu(hasil.kesimpulan.ijtima),
                "Ghurub" to formatWaktu(hasil.kesimpulan.ghurub),
                "Terbenam Hilal" to formatWaktu(hasil.kesimpulan.terbenamHilal),
                "Azimut Matahari" to formatDerajat(hasil.kesimpulan.azimutMatahariDerajat),
                "Azimut Hilal" to formatDerajat(hasil.kesimpulan.azimutHilalDerajat),
                "Posisi Hilal" to "${formatDerajat(hasil.kesimpulan.posisiHilalDerajat)}; ${hasil.kesimpulan.posisiHilalDariMatahari}",
                "Keadaan Hilal" to hasil.kesimpulan.keadaanHilal,
                "Illuminasi Bulan / FIB" to "${formatAngka(hasil.kesimpulan.fractionIlluminationPersen)}%",
                "Nurul Hilal" to "${formatAngka(hasil.kesimpulan.cahayaHilalJari)} jari",
                "Arah Terbenam Hilal" to formatDerajat(hasil.kesimpulan.arahTerbenamHilalDerajat),
            )
        )
    }
}

@Composable
private fun ButirAccordion(butir: ButirPerhitunganFalak) {
    var expanded by remember(butir.nomor) { mutableStateOf(false) }
    DataCard(onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                Text(
                    butir.nomor.toString().padStart(2, '0'),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(butir.judul, fontWeight = FontWeight.Bold)
                Text(butir.hasil, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            DetailBlock("Rumus", butir.rumus)
            DetailBlock("Substitusi", butir.substitusi, monospace = true)
            DetailBlock("Hasil", butir.hasil, highlight = true)
            butir.catatan?.let { DetailBlock("Catatan", it) }
            if (butir.sumber.isNotEmpty()) {
                SumberEphemerisBlock(butir)
            }
        }
    }
}

@Composable
private fun DetailBlock(label: String, value: String, monospace: Boolean = false, highlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            SelectionContainer {
                Text(
                    value,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                    fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun SumberEphemerisBlock(butir: ButirPerhitunganFalak) {
    DetailBlock(
        label = "Sumber Ephemeris",
        value = butir.sumber.joinToString("\n") {
            "${it.tanggal} ${it.jamUt} GMT/UT | ${it.namaTabel} | ${it.namaKolom} | ${it.raw.orEmpty()}"
        },
        monospace = true,
    )
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
            Icon(Icons.Outlined.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DataCard(
    onClick: (() -> Unit)? = null,
    colors: androidx.compose.material3.CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    if (onClick == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    } else {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
private fun InfoRows(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(label, modifier = Modifier.weight(0.42f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(value, modifier = Modifier.weight(0.58f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AlertCard(message: String) {
    DataCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun LoadingCard(message: String) {
    DataCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(strokeWidth = 2.dp)
            Text(message)
        }
    }
}

private fun kriteriaLabel(kriteria: KriteriaAwalBulanFalak): String {
    val parts = mutableListOf<String>()
    if (kriteria.memakaiSyaratIjtimaSebelumGhurub) parts += "ijtimak sebelum/saat ghurub"
    kriteria.tinggiHilalMinimumDerajat?.let { parts += "tinggi mar'i tepi atas >= ${formatDerajat(it)}" }
    kriteria.elongasiMinimumDerajat?.let { parts += "elongasi geosentrik >= ${formatDerajat(it)}" }
    return parts.ifEmpty { listOf("tanpa batas visibilitas") }.joinToString(", ")
}

private fun formatWaktu(waktu: WaktuFalak): String =
    "${formatTanggalSingkat(waktu.tanggal)} ${formatJam(waktu.jamDesimal)} ${waktu.zona}"

private fun formatJam(value: Double): String {
    val totalSeconds = (value * 3600.0).toLong()
    val hour = totalSeconds / 3600
    val minute = (totalSeconds % 3600) / 60
    val second = totalSeconds % 60
    return "%02d:%02d:%02d".format(Locale.US, hour, minute, second)
}

private fun formatJamDurasi(value: Double): String {
    val sign = if (value < 0) "-" else ""
    val totalSeconds = kotlin.math.abs(value * 3600.0).toLong()
    val hour = totalSeconds / 3600
    val minute = (totalSeconds % 3600) / 60
    val second = totalSeconds % 60
    return "$sign%02d:%02d:%02d".format(Locale.US, hour, minute, second)
}

private fun formatDerajat(value: Double): String {
    val sign = if (value < 0.0) "-" else ""
    val totalCentiseconds = (kotlin.math.abs(value) * 3600.0 * 100.0).roundToLong()
    val degree = totalCentiseconds / 360000
    val minute = (totalCentiseconds % 360000) / 6000
    val second = (totalCentiseconds % 6000) / 100.0
    return "$sign${degree}° %02d' %05.2f\"".format(Locale.US, minute, second)
}

private fun formatAngka(value: Double): String = "%.6f".format(Locale.US, value).trimEnd('0').trimEnd('.')

private fun statusMemenuhi(value: Boolean?): String = when (value) {
    true -> "memenuhi"
    false -> "belum memenuhi"
    null -> "tidak dipakai"
}

private fun formatTanggalSingkat(tanggal: LocalDate): String =
    tanggal.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID")))

private fun formatTanggalSingkat(tanggal: String): String =
    formatTanggalSingkat(LocalDate.parse(tanggal))

private fun formatTanggalLengkap(tanggal: LocalDate): String =
    tanggal.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID")))

private fun formatTanggalLengkap(tanggal: String): String =
    formatTanggalLengkap(LocalDate.parse(tanggal))

private fun formatTanggalLengkapOrRaw(tanggal: String): String =
    runCatching { formatTanggalLengkap(tanggal) }.getOrElse { tanggal }

private fun openMarkazMap(context: android.content.Context, input: MarkazInput) {
    val lintang = input.lintang.toDoubleOrNull() ?: return
    val bujur = input.bujur.toDoubleOrNull() ?: return
    val label = Uri.encode(input.nama.ifBlank { "Markaz" })
    val uri = Uri.parse("geo:$lintang,$bujur?q=$lintang,$bujur($label)")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    runCatching { context.startActivity(intent) }
}
