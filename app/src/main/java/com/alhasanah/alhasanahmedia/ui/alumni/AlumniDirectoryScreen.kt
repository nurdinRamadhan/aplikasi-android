package com.alhasanah.alhasanahmedia.ui.alumni

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.alhasanah.alhasanahmedia.data.model.AlumniDirectoryItem
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import org.koin.androidx.compose.koinViewModel

// ─── Avatar gradient palette — deterministic by name ─────────────────────────
private val DirAvatarPalettes = listOf(
    listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
    listOf(Color(0xFF2D2D2D), Color(0xFF525252)),
    listOf(Color(0xFF1B4332), Color(0xFF2D6A4F)),
    listOf(Color(0xFF0D1B2A), Color(0xFF1B4965)),
    listOf(Color(0xFF3D0000), Color(0xFF6B0000)),
    listOf(Color(0xFF1A0533), Color(0xFF3D1165)),
    listOf(Color(0xFF0F3460), Color(0xFF16213E)),
    listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF))
)

private fun dirAvatarPalette(name: String): List<Color> =
    DirAvatarPalettes[Math.abs(name.hashCode()) % DirAvatarPalettes.size]

private fun dirInitialsOf(name: String): String =
    name.trim().split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

// ─── Screen Root ──────────────────────────────────────────────────────────────

@Composable
fun AlumniDirectoryScreen(
    navController: NavController,
    viewModel: AlumniDirectoryViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDirectory() }

    AlumniPremiumTheme {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Custom Top Bar ──
            DirectoryTopBar(
                onBack    = { navController.popBackStack() },
                onRefresh = viewModel::loadDirectory
            )

            // ── Content ──
            Box(modifier = Modifier.fillMaxSize()) {
                when (val current = state) {
                    AlumniDirectoryUiState.Loading -> DirCenterState(
                        title       = "Memuat direktori...",
                        showLoading = true
                    )
                    AlumniDirectoryUiState.LoginRequired -> DirCenterState(
                        title       = "Masuk untuk melanjutkan",
                        description = "Direktori alumni hanya tersedia untuk alumni terverifikasi.",
                        actionText  = "Masuk",
                        onAction    = { navController.navigate(Screen.Login.route) }
                    )
                    is AlumniDirectoryUiState.Error -> DirCenterState(
                        title       = "Gagal memuat direktori",
                        description = current.message,
                        actionText  = "Coba Lagi",
                        onAction    = viewModel::loadDirectory
                    )
                    is AlumniDirectoryUiState.Ready -> DirectoryContent(
                        state          = current,
                        onSearchChange = viewModel::setSearchQuery,
                        onYearSelected = viewModel::setSelectedYear,
                        onProvinceSelected = viewModel::setSelectedProvince,
                        onOpenProfile  = { item ->
                            navController.navigate(
                                Screen.AlumniProfileDetail.createRoute(item.alumni.id)
                            )
                        }
                    )
                }
            }
        }
    }
    }
}

// ─── Custom Top Bar ───────────────────────────────────────────────────────────

@Composable
private fun DirectoryTopBar(onBack: () -> Unit, onRefresh: () -> Unit) {
    AppPageHeader(
        title = "DIREKTORI ALUMNI",
        subtitle = "Cari dan hubungi alumni",
        isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
        onBack = onBack,
        size = AppPageHeaderSize.Compact,
        rightAction = {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onRefresh),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "Muat ulang",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

// ─── Directory Content ────────────────────────────────────────────────────────

@Composable
private fun DirectoryContent(
    state: AlumniDirectoryUiState.Ready,
    onSearchChange: (String) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onProvinceSelected: (String?) -> Unit,
    onOpenProfile: (AlumniDirectoryItem) -> Unit
) {
    val years = remember(state.alumni) {
        state.alumni.map { it.alumni.tahunLulus }.distinct().sortedDescending()
    }
    val provinces = remember(state.alumni) {
        state.alumni.mapNotNull { it.alumni.provinceName }.distinct().sorted()
    }
    val filtered = remember(state.alumni, state.searchQuery, state.selectedYear, state.selectedProvince) {
        val query = state.searchQuery.trim().lowercase()
        state.alumni.filter { item ->
            val alumni = item.alumni
            val matchesYear = state.selectedYear == null || alumni.tahunLulus == state.selectedYear
            val matchesProvince = state.selectedProvince == null || alumni.provinceName == state.selectedProvince
            val haystack = listOfNotNull(
                alumni.fullName,
                alumni.tahunLulus.toString(),
                alumni.profesiSekarang.takeIf { alumni.showProfession },
                alumni.instansiKerja.takeIf { alumni.showProfession },
                alumni.alamatDomisili.takeIf { alumni.showLocation },
                alumni.provinceName.takeIf { alumni.showLocation },
                alumni.regencyName.takeIf { alumni.showLocation },
                alumni.districtName.takeIf { alumni.showLocation },
                alumni.villageName.takeIf { alumni.showLocation }
            ).joinToString(" ").lowercase()
            matchesYear && matchesProvince && (query.isBlank() || haystack.contains(query))
        }
    }

    LazyColumn(
        modifier        = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
        contentPadding  = PaddingValues(bottom = 32.dp)
    ) {
        // ── Search + Filter Header ──
        item {
            DirectorySearchHeader(
                query          = state.searchQuery,
                onQueryChange  = onSearchChange,
                years          = years,
                selectedYear   = state.selectedYear,
                onYearSelected = onYearSelected,
                provinces = provinces,
                selectedProvince = state.selectedProvince,
                onProvinceSelected = onProvinceSelected,
                totalCount     = state.alumni.size,
                filteredCount  = filtered.size
            )
        }

        // ── Results ──
        if (filtered.isEmpty()) {
            item {
                DirCenterState(
                    title       = "Tidak ada hasil",
                    description = "Coba kata kunci atau filter angkatan lain."
                )
            }
        } else {
            items(filtered, key = { it.alumni.id }) { item ->
                DirectoryCard(item = item, onClick = { onOpenProfile(item) })
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 82.dp)
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                )
            }
        }
    }
}

// ─── Search + Filter Header ───────────────────────────────────────────────────

@Composable
private fun DirectorySearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    years: List<Int>,
    selectedYear: Int?,
    onYearSelected: (Int?) -> Unit,
    provinces: List<String>,
    selectedProvince: String?,
    onProvinceSelected: (String?) -> Unit,
    totalCount: Int,
    filteredCount: Int
) {
    var isSearchFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Count row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 18.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text          = "$filteredCount Alumni",
                    fontSize      = 22.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text     = "dari $totalCount alumni terverifikasi",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.material3.Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint     = if (isSearchFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            BasicTextField(
                value           = query,
                onValueChange   = onQueryChange,
                modifier        = Modifier
                    .weight(1f)
                    .onFocusChanged { isSearchFocused = it.isFocused },
                textStyle       = TextStyle(
                    fontSize  = 14.sp,
                    color     = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                ),
                cursorBrush     = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine      = true,
                decorationBox   = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text     = "Cari nama, profesi, kota, angkatan...",
                                fontSize = 14.sp,
                                color    = MaterialTheme.colorScheme.outline
                            )
                        }
                        inner()
                    }
                }
            )
            // Clear button
            AnimatedVisibility(visible = query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Year filter chips
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                YearChip(
                    label      = "Semua",
                    isSelected = selectedYear == null,
                    onClick    = { onYearSelected(null) }
                )
            }
            items(years) { year ->
                YearChip(
                    label      = year.toString(),
                    isSelected = selectedYear == year,
                    onClick    = { onYearSelected(year) }
                )
            }
        }

        if (provinces.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    YearChip(
                        label = "Semua lokasi",
                        isSelected = selectedProvince == null,
                        onClick = { onProvinceSelected(null) }
                    )
                }
                items(provinces) { province ->
                    YearChip(
                        label = province,
                        isSelected = selectedProvince == province,
                        onClick = { onProvinceSelected(province) }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Divider before list
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        )
    }
}

@Composable
private fun YearChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor    = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val textColor  = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val textWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 13.sp,
            color      = textColor,
            fontWeight = textWeight
        )
    }
}

// ─── Directory Card ───────────────────────────────────────────────────────────

@Composable
private fun DirectoryCard(item: AlumniDirectoryItem, onClick: () -> Unit) {
    val alumni   = item.alumni
    val initials = dirInitialsOf(alumni.fullName).ifEmpty { "A" }
    val palette  = dirAvatarPalette(alumni.fullName)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(palette)),
            contentAlignment = Alignment.Center
        ) {
            if (item.avatarUrl.isNullOrBlank()) {
                Text(
                    text       = initials,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            } else {
                AsyncImage(
                    model              = item.avatarUrl,
                    contentDescription = "Foto ${alumni.fullName}",
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
            }
        }

        // Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Name row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text       = alumni.fullName,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false)
                )
                // Year badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text       = alumni.tahunLulus.toString(),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Profesi
            if (alumni.showProfession && !alumni.profesiSekarang.isNullOrBlank()) {
                DirMetaRow(
                    icon  = Icons.Rounded.Work,
                    label = alumni.profesiSekarang
                )
            }
            // Instansi
            if (alumni.showProfession && !alumni.instansiKerja.isNullOrBlank()) {
                DirMetaRow(
                    icon  = Icons.Rounded.Business,
                    label = alumni.instansiKerja
                )
            }
            // Domisili
            val location = directoryLocationLabel(alumni)
            if (alumni.showLocation && location.isNotBlank()) {
                DirMetaRow(
                    icon  = Icons.Rounded.LocationOn,
                    label = location
                )
            }
        }

        // Chevron hint
        Text(
            text     = "›",
            fontSize = 20.sp,
            color    = MaterialTheme.colorScheme.outlineVariant,
            fontWeight = FontWeight.Light
        )
    }
}

private fun directoryLocationLabel(alumni: com.alhasanah.alhasanahmedia.data.model.AlumniDataDto): String {
    return listOfNotNull(alumni.regencyName, alumni.provinceName)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")
        ?: alumni.alamatDomisili.orEmpty()
}

@Composable
private fun DirMetaRow(icon: ImageVector, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.outline,
            modifier           = Modifier.size(13.dp)
        )
        Text(
            text     = label,
            fontSize = 12.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Center State ─────────────────────────────────────────────────────────────

@Composable
private fun DirCenterState(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    showLoading: Boolean = false,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier            = modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(28.dp),
                color       = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                strokeCap   = StrokeCap.Round
            )
            Spacer(Modifier.height(20.dp))
        }
        Text(
            text          = title,
            fontSize      = 16.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = MaterialTheme.colorScheme.onSurface,
            textAlign     = TextAlign.Center,
            letterSpacing = (-0.2).sp
        )
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text       = description,
                fontSize   = 13.sp,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center,
                lineHeight = 19.sp
            )
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onAction
                    )
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = actionText,
                    color      = Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
