package com.alhasanah.alhasanahmedia.ui.santri

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.PublicPrestasiSantri
import com.alhasanah.alhasanahmedia.ui.components.AppGradientBackground
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderBackground
import com.alhasanah.alhasanahmedia.util.formatStringDate
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val PrestasiCategories = listOf(
    "TAHFIDZ",
    "KITAB",
    "KHATAM",
    "AKADEMIK",
    "LOMBA",
    "AKHLAK",
    "OLAHRAGA",
    "SENI",
    "UMUM",
    "LAINNYA"
)

private data class PrestasiStudentSummary(
    val nama: String,
    val kelas: String,
    val jurusan: String,
    val total: Int,
    val totalPoin: Int,
    val latestDate: String?,
    val latestTitle: String
) {
    val key: String = "$nama|$kelas|$jurusan"
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PrestasiScreen(
    navController: NavController,
    viewModel: PrestasiViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedStudentKey by remember { mutableStateOf<String?>(null) }
    var searchDraft by remember(state.searchQuery) { mutableStateOf(state.searchQuery) }

    BackHandler(enabled = selectedStudentKey != null || state.selectedCategory != null) {
        if (selectedStudentKey != null) {
            selectedStudentKey = null
        } else {
            viewModel.selectCategory(null)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppGradientBackground(isDark = isAppInDarkTheme())

        when {
            state.loading && state.items.isEmpty() -> {
                PrestasiLoadingContent(
                    onBack = { navController.popBackStack() }
                )
            }
            state.error != null -> {
                PrestasiErrorState(
                    message = state.error.orEmpty(),
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                val pullRefreshState = rememberPullRefreshState(
                    refreshing = state.refreshing,
                    onRefresh = { viewModel.refresh() }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
                ) {
                    PrestasiContent(
                        items = state.items,
                        categoryCounts = state.categoryCounts,
                        selectedCategory = state.selectedCategory,
                        selectedStudentKey = selectedStudentKey,
                        searchDraft = searchDraft,
                        hasMore = state.hasMore,
                        isLoading = state.loading,
                        onCategorySelected = {
                            selectedStudentKey = null
                            viewModel.selectCategory(it)
                        },
                        onSearchChange = { searchDraft = it },
                        onSearchSubmit = {
                            selectedStudentKey = null
                            viewModel.search(searchDraft)
                        },
                        onStudentSelected = { selectedStudentKey = it },
                        onClearStudent = { selectedStudentKey = null },
                        onLoadMore = { viewModel.loadMore() },
                        onBack = {
                            if (selectedStudentKey != null) {
                                selectedStudentKey = null
                            } else if (state.selectedCategory != null) {
                                viewModel.selectCategory(null)
                            } else {
                                navController.popBackStack()
                            }
                        }
                    )
                    PullRefreshIndicator(
                        refreshing = state.refreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PrestasiAmbientBackground() {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()
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
    }
}

@Composable
private fun PrestasiContent(
    items: List<PublicPrestasiSantri>,
    categoryCounts: Map<String, Long>,
    selectedCategory: String?,
    selectedStudentKey: String?,
    searchDraft: String,
    hasMore: Boolean,
    isLoading: Boolean,
    onCategorySelected: (String?) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onStudentSelected: (String) -> Unit,
    onClearStudent: () -> Unit,
    onLoadMore: () -> Unit,
    onBack: () -> Unit
) {
    val students = remember(items) { items.toStudentSummaries() }
    val selectedStudent = students.firstOrNull { it.key == selectedStudentKey }
    val detailItems = selectedStudent?.let { student ->
        items.filter { it.studentKey() == student.key }
    }.orEmpty()
    val totalPrestasi = selectedCategory?.let { categoryCounts[it] } ?: categoryCounts.values.sum()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PrestasiHeader(
                onBack = onBack,
                totalSantri = students.size,
                totalPrestasi = totalPrestasi.toInt()
            )
        }

        if (items.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    PrestasiSearchBox(
                        value = searchDraft,
                        onValueChange = onSearchChange,
                        onSearch = onSearchSubmit
                    )
                }
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    PrestasiCategorySelector(
                        categories = PrestasiCategories,
                        categoryCounts = categoryCounts,
                        selectedCategory = selectedCategory,
                        totalCount = items.size,
                        onCategorySelected = onCategorySelected
                    )
                }
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    PrestasiEmptyState(
                        title = if (selectedCategory != null || searchDraft.isNotBlank()) {
                            "Data Tidak Ditemukan"
                        } else {
                            "Belum Ada Prestasi"
                        },
                        message = if (selectedCategory != null || searchDraft.isNotBlank()) {
                            "Coba ubah kategori atau kata pencarian."
                        } else {
                            "Catatan prestasi santri belum tersedia."
                        }
                    )
                }
            }
        } else if (selectedStudent != null) {
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    PrestasiStudentDetailHeader(
                        student = selectedStudent,
                        selectedCategory = selectedCategory,
                        onClearStudent = onClearStudent
                    )
                }
            }
            items(detailItems, key = { it.prestasi_id }) { prestasi ->
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    PrestasiCard(prestasi = prestasi)
                }
            }
        } else {
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    PrestasiSearchBox(
                        value = searchDraft,
                        onValueChange = onSearchChange,
                        onSearch = onSearchSubmit
                    )
                }
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    PrestasiCategorySelector(
                        categories = PrestasiCategories,
                        categoryCounts = categoryCounts,
                        selectedCategory = selectedCategory,
                        totalCount = items.size,
                        onCategorySelected = onCategorySelected
                    )
                }
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = if (selectedCategory == null) {
                            "Santri Berprestasi"
                        } else {
                            "Santri Berprestasi - $selectedCategory"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
            items(students, key = { it.key }) { student ->
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    PrestasiStudentCard(
                        student = student,
                        onClick = { onStudentSelected(student.key) }
                    )
                }
            }
            if (hasMore) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Button(
                            onClick = onLoadMore,
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isLoading) "Memuat..." else "Tampilkan Lebih Banyak")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrestasiHeader(
    onBack: () -> Unit,
    totalSantri: Int,
    totalPrestasi: Int
) {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()
    val titleColor = if (isDark) primary.copy(alpha = 0.92f) else Color(0xFF8B6914)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .statusBarsPadding()
    ) {
        AppPageHeaderBackground(
            isDark = isDark,
            modifier = Modifier.matchParentSize()
        )

        Box(
            modifier = Modifier
                .size(54.dp)
                .padding(top = 16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.10f else 0.56f))
                .border(1.dp, primary.copy(alpha = 0.38f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(54.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF2B2418),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(top = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(82.dp),
                shape = CircleShape,
                color = primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, primary.copy(alpha = 0.35f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "PRESTASI SANTRI",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                    color = titleColor
                )
            )
            Text(
                text = "$totalSantri santri - $totalPrestasi catatan prestasi",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrestasiCategorySelector(
    categories: List<String>,
    categoryCounts: Map<String, Long>,
    selectedCategory: String?,
    totalCount: Int,
    onCategorySelected: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Kategori Prestasi",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrestasiSelectableChip(
                label = "Semua",
                count = totalCount.toLong(),
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
            categories.forEach { category ->
                PrestasiSelectableChip(
                    label = category,
                    count = categoryCounts[category],
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

@Composable
private fun PrestasiSelectableChip(
    label: String,
    count: Long?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            primary
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.72f else 0.94f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Color.Transparent else primary.copy(alpha = if (isDark) 0.32f else 0.28f)
        )
    ) {
        Text(
            text = count?.let { "$label ($it)" } ?: label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PrestasiSearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            placeholder = { Text("Cari nama, kelas, jurusan, prestasi") },
            shape = RoundedCornerShape(14.dp)
        )
        Button(
            onClick = onSearch,
            modifier = Modifier.height(56.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Text("Cari")
        }
    }
}

@Composable
private fun PrestasiLoadingContent(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PrestasiHeader(
                onBack = onBack,
                totalSantri = 0,
                totalPrestasi = 0
            )
        }
        items(4) {
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                PrestasiSkeletonCard()
            }
        }
    }
}

@Composable
private fun PrestasiSkeletonCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.46f)
                    .height(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                content = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                content = {}
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(14.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                content = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier
                        .width(92.dp)
                        .height(24.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    content = {}
                )
                Surface(
                    modifier = Modifier
                        .width(112.dp)
                        .height(24.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    content = {}
                )
            }
        }
    }
}

@Composable
private fun PrestasiErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MilitaryTech,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(42.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        )
        Button(onClick = onRetry) {
            Text("Coba Lagi")
        }
    }
}

@Composable
private fun PrestasiStudentCard(
    student: PrestasiStudentSummary,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.nama,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrestasiMeta(Icons.Outlined.Class, "Kelas ${student.kelas}")
                        PrestasiMeta(Icons.Outlined.School, student.jurusan)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = student.latestTitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrestasiChip(Icons.Outlined.Badge, "${student.total} prestasi")
                if (student.totalPoin > 0) {
                    PrestasiChip(Icons.Outlined.Stars, "${student.totalPoin} poin")
                }
                student.latestDate?.let {
                    PrestasiMeta(Icons.Outlined.CalendarToday, formatStringDate(it))
                }
            }
        }
    }
}

@Composable
private fun PrestasiStudentDetailHeader(
    student: PrestasiStudentSummary,
    selectedCategory: String?,
    onClearStudent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.nama,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf("Kelas ${student.kelas}", student.jurusan, selectedCategory)
                            .filterNot { it.isNullOrBlank() }
                            .joinToString(" - "),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    onClick = onClearStudent,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Daftar",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PrestasiCard(prestasi: PublicPrestasiSantri) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box {
            Canvas(modifier = Modifier.size(86.dp).align(Alignment.TopEnd)) {
                val path = Path()
                val center = Offset(size.width * 0.88f, -size.height * 0.08f)
                val r = size.width * 0.58f
                val inner = r * 0.55f
                for (i in 0 until 16) {
                    val rad = if (i % 2 == 0) r else inner
                    val ang = (i * PI / 8 - PI / 2).toFloat()
                    val px = center.x + rad * cos(ang.toDouble()).toFloat()
                    val py = center.y + rad * sin(ang.toDouble()).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, primary.copy(alpha = 0.045f))
            }

            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrestasiChip(Icons.Outlined.Category, prestasi.kategori)
                    prestasi.poin_prestasi?.let {
                        PrestasiChip(Icons.Outlined.Stars, "$it poin")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = prestasi.judul_prestasi,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!prestasi.keterangan.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = prestasi.keterangan,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 19.sp
                        ),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrestasiMeta(Icons.Outlined.CalendarToday, formatStringDate(prestasi.tanggal_prestasi))
                }
            }
        }
    }
}

@Composable
private fun PrestasiChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PrestasiMeta(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PrestasiEmptyState(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.62f),
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

private fun List<PublicPrestasiSantri>.toStudentSummaries(): List<PrestasiStudentSummary> {
    return groupBy { it.studentKey() }
        .map { (_, achievements) ->
            val first = achievements.first()
            PrestasiStudentSummary(
                nama = first.santri_nama,
                kelas = first.santri_kelas.orEmpty().ifBlank { "-" },
                jurusan = first.santri_jurusan.orEmpty().ifBlank { "-" },
                total = achievements.size,
                totalPoin = achievements.sumOf { it.poin_prestasi ?: 0 },
                latestDate = achievements.firstOrNull()?.tanggal_prestasi,
                latestTitle = achievements.firstOrNull()?.judul_prestasi.orEmpty()
            )
        }
        .sortedWith(
            compareByDescending<PrestasiStudentSummary> { it.latestDate.orEmpty() }
                .thenBy { it.nama }
        )
}

private fun PublicPrestasiSantri.studentKey(): String {
    return "${santri_nama}|${santri_kelas.orEmpty()}|${santri_jurusan.orEmpty()}"
}
