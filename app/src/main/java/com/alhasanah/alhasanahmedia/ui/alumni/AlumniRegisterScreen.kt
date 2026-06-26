package com.alhasanah.alhasanahmedia.ui.alumni

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.IndonesiaRegionItem
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import org.koin.androidx.compose.koinViewModel

// ─── Design Tokens ────────────────────────────────────────────────────────────
private val ColorError        = Color(0xFFE53935)
private val ColorSuccess      = Color(0xFF2E7D32)

// ─── Screen ───────────────────────────────────────────────────────────────────
@Composable
fun AlumniRegisterScreen(
    navController: NavController,
    viewModel: AlumniRegisterViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val regionState by viewModel.regionState.collectAsState()

    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var fullName    by remember { mutableStateOf("") }
    var tahunLulus  by remember { mutableStateOf("") }
    var noWa        by remember { mutableStateOf("") }
    var profesi     by remember { mutableStateOf("") }
    var instansi    by remember { mutableStateOf("") }
    var domisili    by remember { mutableStateOf("") }
    var addressDetail by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var selectedProvince by remember { mutableStateOf<IndonesiaRegionItem?>(null) }
    var selectedRegency by remember { mutableStateOf<IndonesiaRegionItem?>(null) }
    var selectedDistrict by remember { mutableStateOf<IndonesiaRegionItem?>(null) }
    var selectedVillage by remember { mutableStateOf<IndonesiaRegionItem?>(null) }

    var passwordVisible by remember { mutableStateOf(false) }
    var showSuccess     by remember { mutableStateOf(false) }
    var showError       by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state) {
        when (val s = state) {
            is AlumniRegisterState.Success -> showSuccess = true
            is AlumniRegisterState.Error   -> showError = s.message
            else                           -> Unit
        }
    }

    // ── Root ──
    AlumniPremiumTheme {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            AppPageHeader(
                title = "DAFTAR AKUN ALUMNI",
                subtitle = "Pondok Pesantren Al-Hasanah",
                isDark = androidx.compose.foundation.isSystemInDarkTheme(),
                onBack = { navController.popBackStack() },
                size = AppPageHeaderSize.Compact
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Form Sections ──
            FormSection(title = "Informasi Akun") {
                ThreadsTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Nama Lengkap",
                    hint = "Masukkan nama lengkap Anda",
                    imeAction = ImeAction.Next
                )
                ThreadsTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Alamat Email",
                    hint = "contoh@email.com",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
                ThreadsTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Kata Sandi",
                    hint = "Minimal 8 karakter",
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingContent = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }

            FormDivider()

            FormSection(title = "Data Kelulusan") {
                ThreadsTextField(
                    value = tahunLulus,
                    onValueChange = { tahunLulus = it.filter(Char::isDigit).take(4) },
                    label = "Tahun Lulus",
                    hint = "contoh: 2018",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
                ThreadsTextField(
                    value = noWa,
                    onValueChange = { noWa = it },
                    label = "Nomor WhatsApp",
                    hint = "08xxxxxxxxxx",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )
            }

            FormDivider()

            FormSection(title = "Profil Profesional") {
                ThreadsTextField(
                    value = profesi,
                    onValueChange = { profesi = it },
                    label = "Profesi Saat Ini",
                    hint = "Guru, Wirausaha, Mahasiswa...",
                    imeAction = ImeAction.Next
                )
                ThreadsTextField(
                    value = instansi,
                    onValueChange = { instansi = it },
                    label = "Instansi / Tempat Kerja",
                    hint = "Nama sekolah, perusahaan, dll.",
                    imeAction = ImeAction.Next
                )
            }

            FormDivider()

            FormSection(title = "Domisili") {
                RegionSelectField(
                    label = "Provinsi",
                    value = selectedProvince?.name.orEmpty(),
                    hint = if (regionState.isLoading && regionState.provinces.isEmpty()) "Memuat provinsi..." else "Pilih provinsi",
                    items = regionState.provinces,
                    enabled = regionState.provinces.isNotEmpty(),
                    onSelected = {
                        selectedProvince = it
                        selectedRegency = null
                        selectedDistrict = null
                        selectedVillage = null
                        viewModel.loadRegencies(it.code)
                    }
                )
                RegionSelectField(
                    label = "Kabupaten / Kota",
                    value = selectedRegency?.name.orEmpty(),
                    hint = if (selectedProvince == null) "Pilih provinsi dahulu" else "Pilih kabupaten/kota",
                    items = regionState.regencies,
                    enabled = selectedProvince != null && regionState.regencies.isNotEmpty(),
                    onSelected = {
                        selectedRegency = it
                        selectedDistrict = null
                        selectedVillage = null
                        viewModel.loadDistricts(it.code)
                    }
                )
                RegionSelectField(
                    label = "Kecamatan",
                    value = selectedDistrict?.name.orEmpty(),
                    hint = if (selectedRegency == null) "Pilih kabupaten/kota dahulu" else "Pilih kecamatan",
                    items = regionState.districts,
                    enabled = selectedRegency != null && regionState.districts.isNotEmpty(),
                    onSelected = {
                        selectedDistrict = it
                        selectedVillage = null
                        viewModel.loadVillages(it.code)
                    }
                )
                RegionSelectField(
                    label = "Desa / Kelurahan",
                    value = selectedVillage?.name.orEmpty(),
                    hint = if (selectedDistrict == null) "Pilih kecamatan dahulu" else "Pilih desa/kelurahan",
                    items = regionState.villages,
                    enabled = selectedDistrict != null && regionState.villages.isNotEmpty(),
                    onSelected = { selectedVillage = it }
                )
                ThreadsTextField(
                    value = addressDetail,
                    onValueChange = { addressDetail = it },
                    label = "Alamat Detail",
                    hint = "Nama jalan, RT/RW, patokan rumah",
                    imeAction = ImeAction.Next,
                    singleLine = false,
                    minLines = 2
                )
                ThreadsTextField(
                    value = postalCode,
                    onValueChange = { postalCode = it.filter(Char::isDigit).take(5) },
                    label = "Kode Pos",
                    hint = "Opsional",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
                ThreadsTextField(
                    value = domisili,
                    onValueChange = { domisili = it },
                    label = "Domisili Manual",
                    hint = "Opsional jika data wilayah belum tersedia",
                    imeAction = ImeAction.Done
                )
                regionState.message?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Info note ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "Akun akan aktif setelah diverifikasi oleh admin. Kami akan menghubungi Anda melalui WhatsApp yang terdaftar.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Submit Button ──
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                val isLoading = state is AlumniRegisterState.Loading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isLoading) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                        .clickable(
                            enabled = !isLoading,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            viewModel.register(
                                email = email,
                                password = password,
                                fullName = fullName,
                                tahunLulus = tahunLulus,
                                noWa = noWa,
                                profesiSekarang = profesi,
                                instansiKerja = instansi,
                                alamatDomisili = domisili,
                                province = selectedProvince,
                                regency = selectedRegency,
                                district = selectedDistrict,
                                village = selectedVillage,
                                postalCode = postalCode,
                                addressDetail = addressDetail
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isLoading,
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                        },
                        label = "btn"
                    ) { loading ->
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                strokeCap = StrokeCap.Round
                            )
                        } else {
                            Text(
                                text = "Kirim Pendaftaran",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.sp
                            )
                        }
                    }
                }
            }

            // ── Login link ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sudah punya akun? ",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Masuk",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { navController.popBackStack() }
                )
            }
        }
    }

    // ── Success Dialog ──
    if (showSuccess) {
        val msg = (state as? AlumniRegisterState.Success)?.message ?: ""
        ThreadsDialog(
            onDismiss = {
                showSuccess = false
                navController.popBackStack()
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = ColorSuccess,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Pendaftaran Terkirim",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = msg.ifBlank { "Pendaftaran Anda sedang diproses. Kami akan segera menghubungi Anda." },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            showSuccess = false
                            navController.popBackStack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Selesai", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }

    // ── Error Dialog ──
    showError?.let { errMsg ->
        ThreadsDialog(onDismiss = {
            showError = null
            viewModel.resetError()
        }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = ColorError,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Pendaftaran Gagal",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = errMsg,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .clickable {
                            showError = null
                            viewModel.resetError()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Coba Lagi", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
    }
}

// ─── Components ───────────────────────────────────────────────────────────────

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        content()
    }
}

@Composable
private fun FormDivider() {
    Spacer(modifier = Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun RegionSelectField(
    label: String,
    value: String,
    hint: String,
    items: List<IndonesiaRegionItem>,
    enabled: Boolean,
    onSelected: (IndonesiaRegionItem) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.3.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                .clickable(
                    enabled = enabled,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { showPicker = true }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifBlank { hint },
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = if (value.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
            )
            Icon(
                Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showPicker) {
        RegionPickerDialog(
            title = label,
            items = items,
            onDismiss = { showPicker = false },
            onSelected = {
                showPicker = false
                onSelected(it)
            }
        )
    }
}

@Composable
private fun RegionPickerDialog(
    title: String,
    items: List<IndonesiaRegionItem>,
    onDismiss: () -> Unit,
    onSelected: (IndonesiaRegionItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(items, query) {
        val clean = query.trim().lowercase()
        if (clean.isBlank()) items else items.filter {
            it.name.lowercase().contains(clean) || it.code.contains(clean)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Pilih $title",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (query.isBlank()) {
                                Text("Cari $title", color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
                            }
                            inner()
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(filtered, key = { it.code }) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelected(item) }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Text(item.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Text(item.code, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(200, easing = EaseOutCubic),
        label = "border"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 2.dp)
    ) {
        // Label
        AnimatedVisibility(
            visible = isFocused || value.isNotEmpty(),
            enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { it / 2 },
            exit = fadeOut(tween(100))
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.3.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Field row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                ),
                singleLine = singleLine,
                minLines = minLines,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = visualTransformation,
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = if (isFocused) "" else if (value.isEmpty()) label else "",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.outline,
                                lineHeight = 22.sp
                            )
                        }
                        if (value.isEmpty() && !isFocused) {
                            // Placeholder shown inline when not focused
                        } else if (value.isEmpty() && isFocused) {
                            Text(
                                text = hint,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.outline,
                                lineHeight = 22.sp
                            )
                        }
                        inner()
                    }
                }
            )
            trailingContent?.invoke()
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    color = lerpColor(
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.colorScheme.onSurface,
                        borderAlpha
                    )
                )
        )

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun ThreadsDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* consume */ },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(modifier = Modifier.padding(28.dp)) {
                    content()
                }
            }
        }
    }
}

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red * (1f - f) + stop.red * f,
        green = start.green * (1f - f) + stop.green * f,
        blue = start.blue * (1f - f) + stop.blue * f,
        alpha = start.alpha * (1f - f) + stop.alpha * f
    )
}
