package com.alhasanah.alhasanahmedia.ui.santri

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alhasanah.alhasanahmedia.R
import com.alhasanah.alhasanahmedia.data.model.SantriModel
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Screen root — LOGIKA IDENTIK, hanya TopBar & layout container yang diupgrade
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SantriDetailScreen(
    santriNis : String,
    navController: NavController,
    viewModel : SantriDetailViewModel = koinViewModel { parametersOf(santriNis) }
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ProfileAmbientBackground()

        when (val state = uiState) {
            is SantriDetailUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color    = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
            is SantriDetailUiState.Success -> {
                SantriDetailContent(
                    santri = state.santri,
                    onBack = { navController.navigateUp() }
                )
            }
            is SantriDetailUiState.Error -> {
                Text(
                    text     = state.message,
                    color    = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun ProfileAmbientBackground() {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()
    val patternColor = primary.copy(alpha = if (isDark) 0.030f else 0.024f)
    val infiniteTransition = rememberInfiniteTransition(label = "profilePattern")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(180_000, easing = LinearEasing)),
        label = "profilePatternRotation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(primary.copy(alpha = if (isDark) 0.08f else 0.05f), Color.Transparent),
                center = Offset(size.width / 2f, 96.dp.toPx()),
                radius = size.width * 0.72f
            ),
            center = Offset(size.width / 2f, 96.dp.toPx()),
            radius = size.width * 0.72f
        )

        val spacing = 96.dp.toPx()
        val starR = 14.dp.toPx()
        val cols = (size.width / spacing).toInt() + 2
        val rows = (size.height / spacing).toInt() + 2

        for (col in -1..cols) {
            for (row in -1..rows) {
                val stagger = if (col % 2 == 0) spacing / 2f else 0f
                val center = Offset(col * spacing, row * spacing + stagger)
                val localRot = if ((col + row) % 2 == 0) rotation else -rotation

                rotate(degrees = localRot, pivot = center) {
                    val path = Path()
                    val inner = starR * 0.55f
                    for (i in 0 until 16) {
                        val r = if (i % 2 == 0) starR else inner
                        val angle = (i * PI / 8 - PI / 2).toFloat()
                        val px = center.x + r * cos(angle.toDouble()).toFloat()
                        val py = center.y + r * sin(angle.toDouble()).toFloat()
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    drawPath(path, patternColor)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  CONTENT — layout dengan hero area berградient di atas
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SantriDetailContent(
    santri: SantriModel,
    onBack: () -> Unit
) {
    val isDark = isAppInDarkTheme()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileHeroHeader(santri = santri, onBack = onBack, isDark = isDark)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(18.dp))

                ProfileQuickSummary(santri = santri)

                Spacer(Modifier.height(18.dp))

                // ── Section: Identitas ────────────────────────────────────────────
                InfoCard(
                    title   = "Identitas Santri",
                    icon    = Icons.Default.Person
                ) {
                InfoField(label = "NIS", value = santri.id)
                InfoField(label = "NISN", value = santri.nisn)
                InfoField(label = "NIK", value = santri.nik)
                InfoField(label = "No. KK", value = santri.noKk)
                InfoField(label = "Nama Lengkap", value = santri.namaLengkap)
                InfoField(label = "Tempat Lahir", value = santri.tempatLahir)
                InfoField(label = "Tanggal Lahir", value = santri.tanggalLahir)
                InfoField(label = "Jenis Kelamin", value = santri.jenisKelamin)
                InfoField(label = "Agama", value = santri.agama.toEmisLabel(RELIGION_LABELS))
                InfoField(label = "Kewarganegaraan", value = santri.kewarganegaraan.toEmisLabel(CITIZENSHIP_LABELS), isLast = true)
                }

                Spacer(Modifier.height(16.dp))

                // ── Section: Akademik ─────────────────────────────────────────────
                InfoCard(
                    title  = "Akademik & Asrama",
                    icon   = Icons.Default.School
                ) {
                InfoField(label = "Kelas", value = santri.kelas)
                InfoField(label = "Jurusan", value = santri.jurusan)
                InfoField(label = "Pembimbing", value = santri.pembimbing)
                InfoField(label = "Status Santri", value = santri.statusSantri)
                InfoField(label = "Status Mukim", value = santri.statusMukim)
                InfoField(label = "Status SPP", value = santri.statusSpp)
                InfoField(label = "Tahun Masuk", value = santri.tahunMasuk?.toString())
                InfoField(label = "Tanggal Masuk", value = santri.tanggalMasuk)
                InfoField(label = "Tahun Lulus/Keluar", value = santri.tahunLulusKeluar?.toString())
                InfoField(label = "Tanggal Lulus/Keluar", value = santri.tanggalLulusKeluar)
                InfoField(label = "Alasan Keluar", value = santri.alasanKeluar.toEmisLabel(EXIT_REASON_LABELS), multiline = true)
                InfoField(label = "Anak Ke", value = santri.anakKe)
                InfoField(label = "Hafalan Kitab", value = santri.hafalanKitab)
                InfoField(label = "Total Hafalan", value = santri.totalHafalan, isLast = true)
                }

                Spacer(Modifier.height(16.dp))

                InfoCard(
                    title = "Alamat & Domisili",
                    icon = Icons.Default.Home
                ) {
                InfoField(label = "Alamat Lengkap", value = santri.alamatLengkap, multiline = true)
                InfoField(label = "RT/RW", value = joinNonBlank(santri.rt, santri.rw, separator = " / "))
                InfoField(label = "Desa/Kelurahan", value = santri.desaKelurahan)
                InfoField(label = "Kecamatan ID", value = santri.kecamatanId)
                InfoField(label = "Kabupaten/Kota", value = santri.kabupatenKota)
                InfoField(label = "Provinsi", value = santri.provinsi)
                InfoField(label = "Kode Pos", value = santri.kodePos)
                InfoField(label = "Jarak Rumah", value = santri.jarakRumahKm?.let { "$it km" }, isLast = true)
                }

            Spacer(Modifier.height(16.dp))

            InfoCard(
                title = "Data Ayah",
                icon = Icons.Default.Man
            ) {
                InfoField(label = "Nama Ayah", value = santri.namaAyah)
                InfoField(label = "NIK Ayah", value = santri.nikAyah)
                InfoField(label = "Status Ayah", value = santri.statusAyah.toEmisLabel(PARENT_STATUS_LABELS))
                InfoField(label = "Pendidikan Ayah", value = santri.pendidikanAyah.toEmisLabel(EDUCATION_LABELS))
                InfoField(label = "Pekerjaan Ayah", value = santri.pekerjaanAyah.toEmisLabel(JOB_LABELS))
                InfoField(label = "Penghasilan Ayah", value = santri.penghasilanAyah.toEmisLabel(INCOME_LABELS), isLast = true)
            }

            Spacer(Modifier.height(16.dp))

            InfoCard(
                title = "Data Ibu",
                icon = Icons.Default.Woman
            ) {
                InfoField(label = "Nama Ibu", value = santri.namaIbu)
                InfoField(label = "NIK Ibu", value = santri.nikIbu)
                InfoField(label = "Status Ibu", value = santri.statusIbu.toEmisLabel(PARENT_STATUS_LABELS))
                InfoField(label = "Pendidikan Ibu", value = santri.pendidikanIbu.toEmisLabel(EDUCATION_LABELS))
                InfoField(label = "Pekerjaan Ibu", value = santri.pekerjaanIbu.toEmisLabel(JOB_LABELS))
                InfoField(label = "Penghasilan Ibu", value = santri.penghasilanIbu.toEmisLabel(INCOME_LABELS), isLast = true)
            }

            Spacer(Modifier.height(16.dp))

            InfoCard(
                title = "Data Wali",
                icon = Icons.Default.SupervisorAccount
            ) {
                InfoField(label = "Nama Wali", value = santri.namaWali)
                InfoField(label = "NIK Wali", value = santri.nikWali)
                InfoField(label = "Hubungan Wali", value = santri.hubunganWali)
                InfoField(label = "Kontak Wali", value = santri.noKontakWali)
                InfoField(label = "Pendidikan Wali", value = santri.pendidikanWali.toEmisLabel(EDUCATION_LABELS))
                InfoField(label = "Pekerjaan Wali", value = santri.pekerjaanWali.toEmisLabel(JOB_LABELS))
                InfoField(label = "Penghasilan Wali", value = santri.penghasilanWali.toEmisLabel(INCOME_LABELS), isLast = true)
            }

            Spacer(Modifier.height(16.dp))

            InfoCard(
                title = "Bantuan & EMIS",
                icon = Icons.AutoMirrored.Filled.Assignment
            ) {
                InfoField(label = "NSP", value = santri.nsp)
                InfoField(label = "Penerima PIP", value = santri.penerimaPip?.toYaTidak())
                InfoField(label = "No. KIP", value = santri.noKip)
                InfoField(label = "Penerima Beasiswa", value = santri.penerimaBeasiswa?.toYaTidak())
                InfoField(label = "Jenis Beasiswa", value = santri.jenisBeasiswa)
                InfoField(label = "Kebutuhan Khusus", value = santri.kebutuhanKhusus.toEmisLabel(SPECIAL_NEEDS_LABELS))
                InfoField(label = "EMIS Extra", value = santri.emisExtra?.toDisplayText(), isLast = true, multiline = true)
            }

            Spacer(Modifier.height(16.dp))

            InfoCard(
                title = "Lokasi & Geocode",
                icon = Icons.Default.LocationOn
            ) {
                InfoField(label = "Latitude", value = santri.latitude?.toString())
                InfoField(label = "Longitude", value = santri.longitude?.toString())
                InfoField(label = "Status Geocode", value = santri.geocodeStatus)
                InfoField(label = "Provider Geocode", value = santri.geocodeProvider)
                InfoField(label = "Confidence", value = santri.geocodeConfidence?.toString())
                InfoField(label = "Terakhir Geocode", value = santri.geocodedAt, isLast = true)
            }

            Spacer(Modifier.height(16.dp))

            InfoCard(
                title = "Metadata Sistem",
                icon = Icons.Default.Info
            ) {
                InfoField(label = "Dibuat", value = santri.createdAt)
                InfoField(label = "Diperbarui", value = santri.updatedAt, isLast = true)
            }

            Spacer(Modifier.height(128.dp))
            }
        }
    }
}

private fun Boolean.toYaTidak(): String = if (this) "Ya" else "Tidak"

private fun String?.toEmisLabel(labels: Map<String, String>): String? {
    val value = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return labels[value]
        ?: labels[value.uppercase()]
        ?: labels[value.padStart(2, '0')]
        ?: value
}

private val PARENT_STATUS_LABELS = mapOf(
    "HIDUP" to "Masih Hidup",
    "MENINGGAL" to "Sudah Meninggal",
    "TANPA KETERANGAN" to "Tanpa Keterangan / Tidak Diketahui"
)

private val EDUCATION_LABELS = mapOf(
    "01" to "Tidak Sekolah",
    "02" to "Putus SD",
    "03" to "SD / Sederajat",
    "04" to "SMP / Sederajat",
    "05" to "SMA / Sederajat",
    "06" to "D1",
    "07" to "D2",
    "08" to "D3",
    "09" to "D4 / S1",
    "10" to "S2",
    "11" to "S3"
)

private val JOB_LABELS = mapOf(
    "01" to "Tidak Bekerja",
    "02" to "Nelayan",
    "03" to "Petani",
    "04" to "Peternak",
    "05" to "PNS / TNI / POLRI",
    "06" to "Karyawan Swasta",
    "07" to "Pedagang Kecil",
    "08" to "Pedagang Besar",
    "09" to "Wiraswasta",
    "10" to "Wirausaha",
    "11" to "Buruh",
    "12" to "Pensiunan",
    "99" to "Lain-lain"
)

private val INCOME_LABELS = mapOf(
    "1" to "Kurang dari Rp 500.000",
    "2" to "Rp 500.000 - Rp 999.999",
    "3" to "Rp 1.000.000 - Rp 1.999.999",
    "4" to "Rp 2.000.000 - Rp 4.999.999",
    "5" to "Rp 5.000.000 - Rp 20.000.000",
    "6" to "Lebih dari Rp 20.000.000"
)

private val RELIGION_LABELS = mapOf(
    "01" to "Islam",
    "02" to "Kristen / Protestan",
    "03" to "Katholik",
    "04" to "Hindu",
    "05" to "Budha",
    "06" to "Khong Hu Chu",
    "99" to "Lainnya",
    "ISLAM" to "Islam"
)

private val CITIZENSHIP_LABELS = mapOf(
    "WNI" to "Warga Negara Indonesia",
    "WNA" to "Warga Negara Asing"
)

private val SPECIAL_NEEDS_LABELS = mapOf(
    "01" to "Tidak Berkebutuhan Khusus",
    "02" to "Netra",
    "03" to "Rungu",
    "04" to "Grahita Ringan",
    "05" to "Grahita Sedang",
    "06" to "Daksa Ringan",
    "07" to "Daksa Sedang",
    "08" to "Laras",
    "09" to "Wicara",
    "10" to "Tuna Ganda",
    "11" to "Hiperaktif",
    "12" to "Cerdas Istimewa",
    "13" to "Bakat Istimewa",
    "14" to "Kesulitan Belajar",
    "15" to "Narkoba",
    "16" to "Indigo",
    "17" to "Down Syndrome",
    "18" to "Autis"
)

private val EXIT_REASON_LABELS = mapOf(
    "1" to "Lulus",
    "2" to "Mutasi Keluar",
    "3" to "Dikeluarkan",
    "4" to "Mengundurkan Diri",
    "5" to "Putus Sekolah / Berhenti",
    "6" to "Wafat / Meninggal Dunia",
    "7" to "Hilang",
    "8" to "Lainnya"
)

private fun joinNonBlank(
    first: String?,
    second: String?,
    separator: String
): String? {
    val values = listOf(first, second).filter { !it.isNullOrBlank() }
    return values.takeIf { it.isNotEmpty() }?.joinToString(separator)
}

private fun JsonObject.toDisplayText(): String? {
    if (isEmpty()) return null
    return entries.joinToString(separator = "\n") { (key, value) ->
        "$key: ${value.toDisplayText()}"
    }
}

private fun JsonElement.toDisplayText(): String = when (this) {
    JsonNull -> "—"
    is JsonPrimitive -> content
    is JsonObject -> toDisplayText().orEmpty()
    else -> toString()
}

@Composable
private fun ProfileHeroHeader(
    santri: SantriModel,
    onBack: () -> Unit,
    isDark: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val titleColor = if (isDark) primary.copy(alpha = 0.92f) else Color(0xFF8B6914)
    val bodyColor = if (isDark) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        AppPageHeaderBackground(isDark = isDark, modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.10f else 0.56f))
                        .border(1.dp, primary.copy(alpha = 0.38f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(46.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = if (isDark) Color.White.copy(0.88f) else Color(0xFF2B2418),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Box(modifier = Modifier.size(46.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PROFIL SANTRI",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp,
                        color = titleColor
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Data identitas, akademik, keluarga, dan EMIS",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = bodyColor,
                        fontWeight = FontWeight.Normal
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))
                ProfileHeroDivider(isDark = isDark)
                Spacer(modifier = Modifier.height(22.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ProfileHero(santri = santri)
                }
            }
        }
    }
}

@Composable
private fun ProfileHeroDivider(isDark: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val lineColor = if (isDark) Color.White.copy(0.07f) else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier.width(170.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, lineColor))))
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.size(5.dp).background(primary.copy(0.60f), RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Box(modifier = Modifier.size(8.dp).background(primary.copy(0.82f), CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Box(modifier = Modifier.size(5.dp).background(primary.copy(0.60f), RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(lineColor, Color.Transparent))))
    }
}

private fun DrawScope.drawProfileStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val inner = radius * 0.55f
    for (i in 0 until 16) {
        val r = if (i % 2 == 0) radius else inner
        val angle = (i * PI / 8 - PI / 2).toFloat()
        val px = center.x + r * cos(angle.toDouble()).toFloat()
        val py = center.y + r * sin(angle.toDouble()).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color)
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  PROFILE HERO
//
// Sebelumnya (ProfileOrb):
//   • Pulsing SIZE blur box → menyebabkan layout jump yang kasar
//   • LinearEasing → terasa robotik
//   • Foto 100dp terlalu kecil
//   • Tidak ada nama / NIS / badge di bawah foto — user tidak tahu profil siapa
//
// Sekarang (ProfileHero):
//   • Pulse pada ALPHA bukan size → smooth, tidak ada layout jump
//   • EaseInOutCubic → organik seperti pernapasan
//   • Foto 120dp + dual ring: glow luar (radial) + border emas tajam
//   • Name, NIS, dan pill badge "Aktif" tampil di bawah foto
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ProfileHero(santri: SantriModel) {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val isDark    = isAppInDarkTheme()

    // Pulse alpha — bukan size (tidak ada layout jump)
    val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue  = 0.38f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Foto dengan dual-ring system ──────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier.size(148.dp)
        ) {
            // Ring 1 — radial glow luar (alpha berdenyut)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension / 2f
                    ),
                    radius = size.minDimension / 2f
                )
            }

            // Ring 2 — sweep gradient arc (dekoratif, tidak berputar)
            Canvas(modifier = Modifier.size(134.dp)) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.0f),
                            primary.copy(alpha = 0.7f),
                            secondary.copy(alpha = 0.9f),
                            primary.copy(alpha = 0.7f),
                            primary.copy(alpha = 0.0f)
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter  = false,
                    style      = Stroke(width = 1.8.dp.toPx())
                )
            }

            // Foto profil
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(santri.fotoUrl)
                    .crossfade(true)
                    .build(),
                placeholder        = painterResource(id = R.drawable.ic_user_placeholder),
                contentDescription = "Foto ${santri.namaLengkap}",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(118.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.sweepGradient(listOf(primary, secondary, primary)),
                        shape = CircleShape
                    )
            )
        }

        // ── Nama + NIS + Badge ────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text  = santri.namaLengkap,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight    = FontWeight.Bold,
                    color         = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.2.sp
                ),
                textAlign = TextAlign.Center,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis
            )

            Text(
                text  = "NIS: ${santri.id}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    letterSpacing = 0.8.sp
                )
            )

            Spacer(Modifier.height(4.dp))

            // Pill badge "Aktif"
            Surface(
                shape = RoundedCornerShape(50),
                color = santriStatusColor(santri.statusSantri).copy(alpha = if (isDark) 0.16f else 0.10f)
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(santriStatusColor(santri.statusSantri), CircleShape)
                    )
                    Text(
                        text  = santri.statusSantri?.takeIf { it.isNotBlank() } ?: "Santri Aktif",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color      = santriStatusColor(santri.statusSantri),
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileQuickSummary(santri: SantriModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileSummaryPill(
                icon = Icons.Default.School,
                label = "Kelas",
                value = santri.kelas,
                modifier = Modifier.weight(1f)
            )
            ProfileSummaryPill(
                icon = Icons.Default.Home,
                label = "Mukim",
                value = santri.statusMukim,
                modifier = Modifier.weight(1f)
            )
        }
        ProfileSummaryPill(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            label = "Tahfidz",
            value = santri.totalHafalan,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProfileSummaryPill(
    icon: ImageVector,
    label: String,
    value: String?,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()
    Surface(
        modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.64f else 0.86f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.52f else 0.70f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = value?.takeIf { it.isNotBlank() } ?: "—",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun santriStatusColor(status: String?): Color {
    val normalized = status?.trim()?.lowercase().orEmpty()
    return when {
        normalized.contains("aktif") -> Color(0xFF22C55E)
        normalized.contains("lulus") -> Color(0xFF0277BD)
        normalized.contains("keluar") || normalized.contains("non") -> Color(0xFFBA1A1A)
        else -> Color(0xFF22C55E)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  INFO CARD
//
// Sebelumnya (GlassCard):
//   • Icon mengambang langsung di samping teks — tidak berkarakter
//   • Title alpha 0.6f — terlihat seperti placeholder/disabled
//   • Tidak ada pemisah visual antara header dan konten
//   • Semua card identik — tidak ada hierarki
//
// Sekarang (InfoCard):
//   • Icon dalam rounded-square container tinted (konsisten dengan drawer)
//   • Bar aksen emas vertikal di kiri title (konsisten dengan DrawerSectionLabel)
//   • Hairline divider pemisah header–konten
//   • Title contrast lebih tinggi (alpha 0.85f)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InfoCard(
    title  : String,
    icon   : ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    val primary       = MaterialTheme.colorScheme.primary
    val isDark        = isAppInDarkTheme()
    val borderColor   = primary.copy(alpha = if (isDark) 0.38f else 0.24f)
    val containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.66f else 0.88f)

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(20.dp)),
        colors    = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDark) 0.dp else 1.dp
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box {
            Canvas(modifier = Modifier.size(76.dp).align(Alignment.TopEnd)) {
                drawProfileStar(
                    center = Offset(size.width * 0.88f, -size.height * 0.10f),
                    radius = size.width * 0.58f,
                    color = primary.copy(alpha = if (isDark) 0.040f else 0.035f)
                )
            }
            Column {
                // ── Card header ───────────────────────────────────────────────
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(9.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector       = icon,
                            contentDescription = null,
                            tint              = primary,
                            modifier          = Modifier.size(17.dp)
                        )
                    }

                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .background(
                                    Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.25f))),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Text(
                            text  = title.uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight    = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                            )
                        )
                    }
                }

                HorizontalDivider(
                    color     = borderColor,
                    thickness = 1.dp
                )

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    content  = content
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  INFO FIELD  (stacked — bukan horizontal SpaceBetween)
//
// Sebelumnya (InfoRow):
//   • Label kiri ↔ Value kanan dalam satu baris (SpaceBetween)
//   • Untuk nilai panjang (alamat, nama) → teks terpotong atau wrap aneh
//   • Semua row sama persis — tidak ada hirarki
//   • Tidak ada pemisah antar row
//
// Sekarang (InfoField):
//   • Stacked: label kecil muted di atas → value besar tegas di bawah
//   • Seperti form banking premium (Jenius, BCA, Flip, GoPay)
//   • Hairline divider tipis antara setiap field
//   • Parameter isLast untuk tidak menampilkan divider di field terakhir
//   • Multiline support untuk nilai seperti alamat
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InfoField(
    label    : String,
    value    : String?,
    isLast   : Boolean = false,
    multiline: Boolean = false
) {
    val divColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp)
    ) {
        // Label — kecil, muted, uppercase
        Text(
            text  = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                fontSize      = 9.5.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        )

        Spacer(Modifier.height(4.dp))

        // Value — tegas, readable
        Text(
            text     = value?.takeIf { it.isNotBlank() } ?: "—",
            style    = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (value.isNullOrBlank()) FontWeight.Normal else FontWeight.SemiBold,
                color      = if (value.isNullOrBlank())
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                else
                    MaterialTheme.colorScheme.onSurface,
                fontSize   = 14.5.sp
            ),
            maxLines  = if (multiline) Int.MAX_VALUE else 2,
            overflow  = TextOverflow.Ellipsis
        )
    }

    // Divider antara field — dihilangkan di field terakhir
    if (!isLast) {
        HorizontalDivider(
            color     = divColor,
            thickness = 0.8.dp
        )
    }
}
