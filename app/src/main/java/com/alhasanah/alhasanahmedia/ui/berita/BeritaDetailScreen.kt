package com.alhasanah.alhasanahmedia.ui.berita

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alhasanah.alhasanahmedia.data.model.Berita
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import com.alhasanah.alhasanahmedia.util.formatStringDate
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import org.koin.androidx.compose.koinViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Luxury color tokens — hanya dipakai di file ini
// Tidak mengubah MaterialTheme global
// ─────────────────────────────────────────────────────────────────────────────
private val LuxuryGold      = Color(0xFFD4A853)   // Warm editorial gold
private val LuxuryGoldLight = Color(0xFFECC96B)   // Highlight
private val LuxuryGoldDim   = Color(0xFF9A7535)   // Subtle trace

// Surface — halaman berita bukan app chrome, pakai warm tone
private val DarkPaper       = Color(0xFF0F0D0A)   // Warm near-black, bukan abu Material
private val LightPaper      = Color(0xFFFDF9F3)   // Warm cream, bukan putih murni

// ─────────────────────────────────────────────────────────────────────────────
// ██  SCREEN ROOT — LOGIKA IDENTIK
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeritaDetailScreen(
    slug     : String,
    onBack   : () -> Unit,
    viewModel: BeritaDetailViewModel = koinViewModel()
) {
    val berita    by viewModel.beritaState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDark    = isAppInDarkTheme()

    LaunchedEffect(slug) {
        viewModel.getBerita(slug)
    }

    // Latar belakang halaman: warm paper, bukan background sistem
    val pageBg = if (isDark) Color.Transparent else LightPaper

    Scaffold(
        containerColor = pageBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isDark) {
                AppSolidBackground(isDark = true)
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.align(Alignment.Center),
                    color       = LuxuryGold,
                    strokeWidth = 2.dp
                )
            } else {
                berita?.let {
                    BeritaDetailContent(
                        berita = it,
                        isDark = isDark,
                        pageBg = pageBg,
                        onBack = onBack
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  CONTENT
//
// Sebelumnya:
//   • LazyColumn dengan contentPadding 16dp di semua sisi → gambar ter-crop margin
//   • Urutan: badge → judul → ringkasan → tanggal → gambar → konten
//   • Gambar tidak imersif — hanya 240dp, rounded all sides, border kotak
//
// Sekarang:
//   • Hero image FULL-BLEED di atas (tanpa horizontal padding, tanpa rounded top)
//   • Gradient scrim 3 zona di atas hero: top dark (readability TopBar) +
//     bottom dark (overlay badge + date) → transisi natural ke body
//   • Badge kategori + tanggal mengambang di atas hero — tidak makan ruang body
//   • Padding body 24dp horisontal — lebih lega untuk membaca
//   • Gold ornamental divider antara ringkasan dan konten
//   • Footer elegan dengan ornamen emas closing
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BeritaDetailContent(
    berita: Berita,
    isDark: Boolean,
    pageBg: Color,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        // ZERO top/horizontal padding — hero image harus full-bleed
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── 1. Cinematic hero image ───────────────────────────────────────────
        item {
            HeroImageSection(berita = berita, isDark = isDark, onBack = onBack)
        }

        // ── 2. Judul + ringkasan editorial ────────────────────────────────────
        item {
            ArticleHeader(berita = berita, isDark = isDark)
        }

        // ── 3. Gold divider ornamental ────────────────────────────────────────
        item {
            EditorialDivider()
        }

        // ── 4. Body konten ────────────────────────────────────────────────────
        item {
            berita.konten?.let {
                ArticleBody(konten = it, isDark = isDark)
            }
        }

        // ── 5. Footer closing ─────────────────────────────────────────────────
        item {
            ArticleFooter(isDark = isDark)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  HERO IMAGE SECTION
//
// Sebelumnya:
//   • Tinggi 240dp — terlalu pendek untuk entry point visual
//   • Rounded corners semua sisi — memotong imersivitas
//   • Border 1dp — menambah batas yang tidak perlu
//   • Scrim hanya 60dp hitam tipis di bawah
//
// Sekarang:
//   • Tinggi 300dp full-bleed (NO horizontal padding dari LazyColumn)
//   • Rounded hanya di bawah (bottomStart/End 28dp) — atas rata tepi layar
//   • 3-zone scrim:
//     Top → 80dp: gelap transparan (agar TopBar pill tetap readable)
//     Middle: clear (foto tampil murni)
//     Bottom → 160dp: gelap dalam (area overlay badge + date)
//   • Badge kategori + tanggal mengambang di dalam hero (zona bawah)
//   • Jika tidak ada gambar: placeholder gradient emas hangat + ornamen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroImageSection(
    berita: Berita,
    isDark: Boolean,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
    ) {
        if (berita.thumbnailUrl != null) {
            // ── Foto ──────────────────────────────────────────────────────────
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(berita.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Foto berita",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
        } else {
            // ── Placeholder jika tidak ada foto ───────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDark) {
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1C1408), Color(0xFF0A0806))
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFF7E6), Color(0xFFF3E2BC))
                            )
                        }
                    )
            ) {
                // Ornamen lingkaran emas samar di tengah
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        brush  = Brush.radialGradient(
                            colors = listOf(LuxuryGold.copy(alpha = 0.12f), Color.Transparent),
                            center = center,
                            radius = size.minDimension * 0.5f
                        ),
                        radius = size.minDimension * 0.5f
                    )
                }
            }
        }

        // ── 3-zone cinematic scrim ────────────────────────────────────────────
        // Zone A: top vignette (TopBar readability)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )

        // Zone B: bottom scrim (badge + date overlay area)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        // ── Badge kategori + tanggal di atas scrim bawah ─────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArticleHeroActionButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                onClick = onBack
            )
            ArticleHeroActionButton(
                icon = Icons.Filled.Share,
                contentDescription = "Bagikan",
                onClick = { /* TODO: Share */ }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Kategori pill — emas dengan border
            Surface(
                shape = RoundedCornerShape(50),
                color = LuxuryGold.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = LuxuryGold.copy(alpha = 0.65f)
                )
            ) {
                Text(
                    text     = berita.kategori.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style    = MaterialTheme.typography.labelSmall.copy(
                        color         = LuxuryGoldLight,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 9.sp,
                        letterSpacing = 1.5.sp
                    )
                )
            }

            // Tanggal — teks putih muted tipis
            Text(
                text  = formatStringDate(berita.tanggalPublish),
                style = MaterialTheme.typography.labelSmall.copy(
                    color      = Color.White.copy(alpha = 0.58f),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}

@Composable
private fun ArticleHeroActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.32f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  ARTICLE HEADER
//
// Sebelumnya:
//   • Judul & ringkasan dalam satu item tanpa breathing room
//   • Ringkasan diperlakukan sama dengan body (bodyMedium, warna muted biasa)
//   • Tidak ada kesan editorial premium
//
// Sekarang:
//   • Judul: 26sp, lineHeight 36sp, Bold (bukan ExtraBold — lebih hangat)
//   • Ringkasan: styled sebagai "editorial lead" — border left emas 2dp,
//     italic, slightly larger, gold-tinted — seperti pull quote majalah premium
//   • Reading time estimate di bawah judul (computed dari panjang konten)
//   • Padding 24dp horizontal
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ArticleHeader(berita: Berita, isDark: Boolean) {
    val readingMinutes = ((berita.konten?.length ?: 0) / 1000).coerceAtLeast(1)

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 28.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Judul ─────────────────────────────────────────────────────────────
        Text(
            text  = berita.judul,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color      = if (isDark) Color.White else Color(0xFF1A1208),
                lineHeight = 36.sp,
                fontSize   = 24.sp
            )
        )

        // ── Reading time ──────────────────────────────────────────────────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Dot emas kecil
            Canvas(Modifier.size(4.dp)) {
                drawCircle(LuxuryGoldDim)
            }
            Text(
                text  = "± $readingMinutes menit membaca",
                style = MaterialTheme.typography.labelSmall.copy(
                    color      = LuxuryGoldDim,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // ── Ringkasan — editorial lead style ─────────────────────────────────
        berita.ringkasan?.let { lead ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left border emas vertikal (aksen editorial)
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(LuxuryGold, LuxuryGoldDim.copy(alpha = 0.4f))
                            ),
                            RoundedCornerShape(2.dp)
                        )
                        .align(Alignment.CenterVertically)
                )

                Text(
                    text  = lead,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color      = if (isDark)
                            Color.White.copy(alpha = 0.70f)
                        else
                            Color(0xFF3D2E14).copy(alpha = 0.75f),
                        fontStyle  = FontStyle.Italic,
                        lineHeight = 26.sp,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  EDITORIAL GOLD DIVIDER
//
// Ornamen pemisah antara header artikel dan body konten.
// Menggantikan Spacer polos. Memberikan jeda visual bertone editorial majalah.
// Terinspirasi: The New Yorker, Architectural Digest, TIME Magazine digital
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EditorialDivider() {
    // Pulse subtle untuk ornamen tengah
    val infiniteTransition = rememberInfiniteTransition(label = "divider_pulse")
    val centerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue  = 0.90f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "divAlpha"
    )

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hairline gradient kiri
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
        ) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        LuxuryGoldDim.copy(alpha = 0.30f),
                        LuxuryGold.copy(alpha = 0.55f),
                        LuxuryGold.copy(alpha = 0.70f),
                        Color.Transparent,
                        Color.Transparent                   // gap untuk ornamen tengah
                    )
                ),
                start       = Offset(0f, 0f),
                end         = Offset(size.width, 0f),
                strokeWidth = size.height
            )
        }

        // Ornamen tengah: tiga berlian kecil
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(3.dp)) {
                drawCircle(LuxuryGoldDim.copy(alpha = centerAlpha * 0.6f))
            }
            Canvas(Modifier.size(5.dp)) {
                drawCircle(LuxuryGold.copy(alpha = centerAlpha))
            }
            Canvas(Modifier.size(3.dp)) {
                drawCircle(LuxuryGoldDim.copy(alpha = centerAlpha * 0.6f))
            }
        }

        // Hairline gradient kanan (mirrored)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
        ) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        LuxuryGold.copy(alpha = 0.70f),
                        LuxuryGold.copy(alpha = 0.55f),
                        LuxuryGoldDim.copy(alpha = 0.30f),
                        Color.Transparent
                    )
                ),
                start       = Offset(0f, 0f),
                end         = Offset(size.width, 0f),
                strokeWidth = size.height
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  ARTICLE BODY
//
// Sebelumnya:
//   • `TextAlign.Justify` — masalah terbesar. Justify membuat word-spacing
//     tidak konsisten, gap aneh antara kata, tampilan seperti koran lama.
//     Semua premium news app (Apple News, Medium, Substack) pakai START/LEFT.
//   • lineHeight 28sp — terlalu rapat untuk konten panjang
//   • Tidak ada padding yang proporsional
//
// Sekarang:
//   • TextAlign.Start — konsisten, nyaman, modern
//   • lineHeight 30sp — breathing room yang cukup
//   • fontSize 16sp — optimal untuk reading comfort di mobile
//   • Warna: warm tone (sedikit warm tint, bukan hitam murni)
//   • Horizontal padding 24dp — konsisten dengan header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ArticleBody(konten: String, isDark: Boolean) {
    Text(
        text      = konten,
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        style     = MaterialTheme.typography.bodyLarge.copy(
            color      = if (isDark)
                Color.White.copy(alpha = 0.82f)            // Warm readable di dark
            else
                Color(0xFF2A1F0E).copy(alpha = 0.82f),     // Warm dark sepia di light
            lineHeight = 30.sp,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Normal,
            textAlign  = TextAlign.Start                   // Bukan Justify
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  ARTICLE FOOTER
//
// Memberikan "closing" yang terasa selesai, bukan terpotong.
// Konsisten dengan ornamen brand emas di SplashScreen dan Drawer.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ArticleFooter(isDark: Boolean) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hairline emas full-width
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
        ) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        LuxuryGoldDim.copy(alpha = 0.20f),
                        LuxuryGold.copy(alpha = 0.50f),
                        LuxuryGoldDim.copy(alpha = 0.20f),
                        Color.Transparent
                    )
                ),
                start       = Offset(0f, 0f),
                end         = Offset(size.width, 0f),
                strokeWidth = size.height
            )
        }

        Spacer(Modifier.height(4.dp))

        // Ornamen triple-dot (brand signature — konsisten di seluruh app)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(3.dp))   { drawCircle(LuxuryGoldDim.copy(alpha = 0.40f)) }
            Canvas(Modifier.size(5.dp))   { drawCircle(LuxuryGold.copy(alpha = 0.65f)) }
            Canvas(Modifier.size(3.dp))   { drawCircle(LuxuryGoldDim.copy(alpha = 0.40f)) }
        }

        Text(
            text      = "AL-HASANAH MEDIA",
            style     = MaterialTheme.typography.labelSmall.copy(
                color         = LuxuryGoldDim.copy(alpha = 0.50f),
                letterSpacing = 2.sp,
                fontSize      = 8.5.sp,
                fontWeight    = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stub — masih dipanggil jika ada referensi lain di codebase
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BeritaBackgroundPattern() {
    // No-op — diganti dengan warm paper background di level Scaffold
}
