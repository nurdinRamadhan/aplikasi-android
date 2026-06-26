package com.alhasanah.alhasanahmedia.ui.falak

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Event
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import com.alhasanah.alhasanahmedia.domain.falak.ButirPerhitunganFalak
import com.alhasanah.alhasanahmedia.domain.falak.HasilGerhanaBulanEphemeris
import com.alhasanah.alhasanahmedia.domain.falak.JenisGerhanaBulan
import com.alhasanah.alhasanahmedia.domain.falak.VisualGerhanaBulanMapper
import com.alhasanah.alhasanahmedia.domain.falak.WaktuFalak
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong
import org.koin.androidx.compose.koinViewModel
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerhanaBulanScreen(
    navController: NavController,
    viewModel: GerhanaBulanViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppPageHeader(
                title = "GERHANA BULAN",
                subtitle = "Hisab gerhana dari data ephemeris resmi",
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
                ParameterGerhanaBulanCard(
                    state = state,
                    onAcuanChange = viewModel::pilihAcuan,
                    onModeTanggalChange = viewModel::ubahModeTanggal,
                    onBulanHijriahChange = viewModel::ubahBulanHijriah,
                    onTanggalChange = viewModel::ubahTanggalKemungkinan,
                    onZonaChange = viewModel::ubahZona,
                    onRentangManualChange = viewModel::ubahRentangPencarianManual,
                    onHitung = viewModel::hitung,
                )
            }

            state.error?.let { message ->
                item { AlertGerhanaCard(message) }
            }

            if (state.loading) {
                item { LoadingGerhanaCard("Menyiapkan data ephemeris dan menghitung gerhana bulan") }
            }

            state.hasil?.let { hasil ->
                item { VisualGerhanaBulanCard(VisualGerhanaBulanMapper().map(hasil)) }
                item { KesimpulanGerhanaBulanCard(hasil) }
                item { RingkasanGerhanaBulanCard(hasil) }
                item { SectionTitleGerhana("Rincian Perhitungan", "Setiap butir memuat rumus, substitusi, hasil, dan sumber data ephemeris.") }
                items(hasil.butirPerhitungan) { butir ->
                    ButirGerhanaAccordion(butir)
                }
            }
        }
    }
}

@Composable
private fun ParameterGerhanaBulanCard(
    state: GerhanaBulanUiState,
    onAcuanChange: (AcuanGerhanaBulanKemenag) -> Unit,
    onModeTanggalChange: (ModeTanggalGerhanaBulan) -> Unit,
    onBulanHijriahChange: (String) -> Unit,
    onTanggalChange: (String) -> Unit,
    onZonaChange: (String) -> Unit,
    onRentangManualChange: (Int) -> Unit,
    onHitung: () -> Unit,
) {
    val acuanTerpilih = remember(state.bulanHijriah, state.tanggalKemungkinan) {
        GerhanaBulanViewModel.acuanKemenag2026.firstOrNull {
            it.bulanHijriah == state.bulanHijriah && it.tanggalKemungkinanMasehi == state.tanggalKemungkinan
        }
    }
    DataCardGerhana {
        SectionTitleGerhana(
            "Parameter Gerhana Bulan",
            "Tanggal kemungkinan adalah tanggal sekitar purnama. Mode manual mencari FIB terbesar dalam rentang yang dipilih."
        )
        AcuanGerhanaBulanPicker(selected = acuanTerpilih, onSelect = onAcuanChange)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = state.modeTanggal == ModeTanggalGerhanaBulan.ACUAN_KEMENAG,
                onClick = { onModeTanggalChange(ModeTanggalGerhanaBulan.ACUAN_KEMENAG) },
                label = { Text("Acuan Kemenag") },
            )
            FilterChip(
                selected = state.modeTanggal == ModeTanggalGerhanaBulan.INPUT_MANUAL,
                onClick = { onModeTanggalChange(ModeTanggalGerhanaBulan.INPUT_MANUAL) },
                label = { Text("Input Manual") },
            )
        }
        OutlinedTextField(
            value = state.bulanHijriah,
            onValueChange = onBulanHijriahChange,
            label = { Text("Keterangan Bulan Hijriah") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.tanggalKemungkinan,
            onValueChange = onTanggalChange,
            label = { Text("Tanggal Kemungkinan Gerhana") },
            placeholder = { Text("2026-03-04") },
            singleLine = true,
            enabled = state.modeTanggal == ModeTanggalGerhanaBulan.INPUT_MANUAL,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            if (state.modeTanggal == ModeTanggalGerhanaBulan.INPUT_MANUAL) {
                "Masukkan tanggal Masehi format yyyy-MM-dd. Aplikasi mencari FIB terbesar dari H-${state.rentangPencarianManualHari} sampai H+${state.rentangPencarianManualHari}, lalu menyiapkan data tambahan untuk interpolasi."
            } else {
                "Tanggal dikunci dari acuan Kemenag. Pilih Input Manual untuk mencoba tanggal lain."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.modeTanggal == ModeTanggalGerhanaBulan.INPUT_MANUAL) {
            RentangManualGerhanaPicker(
                selected = state.rentangPencarianManualHari,
                onSelect = onRentangManualChange,
            )
        }
        ZonaGerhanaPicker(selected = state.zona, onSelect = onZonaChange)
        Text(
            "Tahap ini menghitung kontak global gerhana dari ephemeris. Visibilitas lokal di markaz, seperti tinggi Bulan saat kontak, disiapkan untuk tahap lanjutan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onHitung,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Calculate, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text("Hitung Gerhana Bulan")
        }
    }
}

@Composable
private fun RentangManualGerhanaPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Rentang pencarian FIB", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(2, 3, 5).forEach { hari ->
                FilterChip(
                    selected = selected == hari,
                    onClick = { onSelect(hari) },
                    label = { Text("H±$hari") },
                )
            }
        }
    }
}

@Composable
private fun AcuanGerhanaBulanPicker(
    selected: AcuanGerhanaBulanKemenag?,
    onSelect: (AcuanGerhanaBulanKemenag) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(
            value = selected?.nama ?: "Pilih acuan gerhana",
            onValueChange = {},
            readOnly = true,
            label = { Text("Acuan Kemenag 2026") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.ExpandMore, contentDescription = "Pilih acuan")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GerhanaBulanViewModel.acuanKemenag2026.forEach { acuan ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(acuan.nama, fontWeight = FontWeight.Bold)
                            Text("${acuan.tanggalKemungkinanMasehi} • ${acuan.jenis}", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(acuan)
                    }
                )
            }
        }
    }
}

@Composable
private fun ZonaGerhanaPicker(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("WIB", "WITA", "WIT").forEach { zona ->
            FilterChip(
                selected = selected == zona,
                onClick = { onSelect(zona) },
                label = { Text(zona) },
            )
        }
    }
}

@Composable
private fun KesimpulanGerhanaBulanCard(hasil: HasilGerhanaBulanEphemeris) {
    DataCardGerhana {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                Icon(
                    Icons.Outlined.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(hasil.klasifikasi.keterangan, fontWeight = FontWeight.Black)
                Text(
                    hasil.kesimpulan.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        InfoGridGerhana(
            listOf(
                "Jenis" to labelJenis(hasil.kesimpulan.jenis),
                "Tengah" to hasil.kesimpulan.tengahGerhanaLokal?.let(::formatWaktu).orDash(),
                "Magnitude umbra" to hasil.magnitude.magnitudeUmbra?.let { formatAngka(it) }.orDash(),
            )
        )
    }
}

@Composable
private fun RingkasanGerhanaBulanCard(hasil: HasilGerhanaBulanEphemeris) {
    DataCardGerhana {
        SectionTitleGerhana(
            "Waktu Kontak",
            "Waktu ditampilkan sesuai zona yang dipilih. Tengah gerhana mengikuti uraian rumus Kemenag."
        )
        InfoGridGerhana(
            listOf(
                "Mulai Gerhana" to hasil.waktuKontak.mulaiGerhanaLokal?.let(::formatWaktu).orDash(),
                "Mulai Total" to hasil.waktuKontak.mulaiTotalLokal?.let(::formatWaktu).orDash(),
                "Tengah Gerhana" to formatWaktu(hasil.waktuKontak.tengahGerhanaLokal),
                "Selesai Total" to hasil.waktuKontak.selesaiTotalLokal?.let(::formatWaktu).orDash(),
                "Selesai Gerhana" to hasil.waktuKontak.selesaiGerhanaLokal?.let(::formatWaktu).orDash(),
                "Istiqbal GMT/UT" to formatWaktu(hasil.saatIstiqbal.waktuUt),
                "FIB terbesar" to "${formatAngka(hasil.dataIstiqbal.fibTerbesarPersen.nilai)}% pada jam ${hasil.dataIstiqbal.jamFibUt} GMT/UT",
            )
        )
        hasil.pembandingMeeus?.let { pembanding ->
            SectionTitleGerhana(
                "Pembanding Meeus",
                "Estimasi ini dipakai sebagai pembanding awal; keputusan utama tetap dari ephemeris resmi."
            )
            InfoGridGerhana(
                listOf(
                    "Purnama terdekat" to "${formatTanggal(pembanding.tanggalPurnamaTerdekatUt)} ${formatJam(pembanding.jamPurnamaTerdekatUt)} GMT/UT",
                    "Selisih dari input" to "${formatAngka(pembanding.selisihHariDariInput)} hari",
                    "Argumen lintang" to formatDerajat(pembanding.argumenLintangBulanDerajat),
                    "Jarak simpul" to formatDerajat(pembanding.jarakKeSimpulDerajat),
                    "Indikasi" to if (pembanding.memungkinkanGerhana) "Dekat simpul" else "Jauh dari simpul",
                    "Catatan" to pembanding.catatan,
                )
            )
        }
    }
}

@Composable
private fun ButirGerhanaAccordion(butir: ButirPerhitunganFalak) {
    var expanded by remember { mutableStateOf(false) }
    DataCardGerhana {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${butir.nomor}. ${butir.judul}", fontWeight = FontWeight.Bold)
                Text(
                    butir.hasil,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            }
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailLineGerhana("Rumus", butir.rumus)
                    DetailLineGerhana("Substitusi", butir.substitusi)
                    DetailLineGerhana("Hasil", butir.hasil)
                    butir.catatan?.let { DetailLineGerhana("Catatan", it) }
                    if (butir.sumber.isNotEmpty()) {
                        DetailLineGerhana(
                            "Sumber",
                            butir.sumber.joinToString("\n") {
                                "${it.tanggal} ${it.jamUt} GMT/UT • ${it.namaTabel} • ${it.namaKolom}: ${it.raw.orEmpty()}"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataCardGerhana(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionTitleGerhana(title: String, subtitle: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, fontWeight = FontWeight.Black)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoGridGerhana(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
                SelectionContainer {
                    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.58f))
                }
            }
        }
    }
}

@Composable
private fun DetailLineGerhana(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun AlertGerhanaCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(8.dp)) {
        Text(message, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun LoadingGerhanaCard(message: String) {
    DataCardGerhana {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
            Text(message)
        }
    }
}

private fun labelJenis(jenis: JenisGerhanaBulan): String = when (jenis) {
    JenisGerhanaBulan.TidakTerjadi -> "Tidak terjadi"
    JenisGerhanaBulan.PenumbraSebagian -> "Penumbra sebagian"
    JenisGerhanaBulan.PenumbraTotal -> "Penumbra total"
    JenisGerhanaBulan.Sebagian -> "Sebagian"
    JenisGerhanaBulan.Total -> "Total"
}

private fun formatWaktu(waktu: WaktuFalak): String =
    "${formatTanggal(waktu.tanggal)} ${formatJam(waktu.jamDesimal)} ${waktu.zona}"

private fun formatTanggal(tanggal: LocalDate): String =
    tanggal.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID")))

private fun formatJam(value: Double): String {
    val totalSeconds = (value * 3600.0).roundToLong()
    val hour = totalSeconds / 3600
    val minute = (totalSeconds % 3600) / 60
    val second = totalSeconds % 60
    return "%02d:%02d:%02d".format(Locale.US, hour, minute, second)
}

private fun formatAngka(value: Double): String =
    "%.6f".format(Locale.US, value).trimEnd('0').trimEnd('.')

private fun formatDerajat(value: Double): String {
    val sign = if (value < 0.0) "-" else ""
    val totalCentiseconds = (kotlin.math.abs(value) * 3600.0 * 100.0).roundToLong()
    val degree = totalCentiseconds / 360000
    val minute = (totalCentiseconds % 360000) / 6000
    val second = (totalCentiseconds % 6000) / 100.0
    return "$sign${degree}° %02d' %05.2f\"".format(Locale.US, minute, second)
}

private fun String?.orDash(): String = this ?: "-"
