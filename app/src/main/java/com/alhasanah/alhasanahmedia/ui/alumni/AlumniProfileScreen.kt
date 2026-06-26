package com.alhasanah.alhasanahmedia.ui.alumni

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.alhasanah.alhasanahmedia.MainViewModel
import com.alhasanah.alhasanahmedia.data.model.AlumniDataDto
import com.alhasanah.alhasanahmedia.data.model.AlumniFollowUser
import com.alhasanah.alhasanahmedia.data.model.ForumCommentItem
import com.alhasanah.alhasanahmedia.data.model.ForumThreadItem
import com.alhasanah.alhasanahmedia.data.model.UpdateAlumniProfileDto
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import org.koin.androidx.compose.koinViewModel

// ────────────────────────────────────────────────────────────────
// Internal Helpers
// ────────────────────────────────────────────────────────────────

private fun alumniInitialsOf(name: String): String =
    name.trim().split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

private fun alumniProfileLocationLabel(alumni: AlumniDataDto): String {
    return listOfNotNull(alumni.regencyName, alumni.provinceName)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")
        ?: alumni.alamatDomisili.orEmpty()
}

private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}jt"
    n >= 10_000    -> "${n / 1_000}rb"
    n >= 1_000     -> String.format("%.1frb", n / 1_000f).trimEnd('0').trimEnd('.')
    else           -> n.toString()
}

private fun formatRelativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            .also { it.timeZone = java.util.TimeZone.getTimeZone("UTC") }
        val date = sdf.parse(iso.take(19)) ?: return "baru saja"
        val diff  = java.util.Date().time - date.time
        val mins  = diff / 60_000
        val hours = diff / 3_600_000
        val days  = diff / 86_400_000
        when {
            mins  < 1  -> "baru saja"
            mins  < 60 -> "${mins}m"
            hours < 24 -> "${hours}j"
            days  < 7  -> "${days}h"
            else       -> java.text.SimpleDateFormat("d MMM", java.util.Locale("id")).format(date)
        }
    } catch (_: Exception) { "" }
}

// ── Tab enum ──
private enum class ProfileTab(val label: String) {
    POSTS("Postingan"), REPLIES("Balasan"), MEDIA("Media")
}

// ════════════════════════════════════════════════════════════════
//  AlumniProfileScreen  — Threads-style main profile view
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlumniProfileScreen(
    navController: NavController,
    alumniId    : String? = null,
    viewModel   : AlumniProfileViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val followListState by viewModel.followListState.collectAsState()
    var selectedTab by remember { mutableStateOf(ProfileTab.POSTS) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(alumniId) { viewModel.loadProfile(alumniId) }

    val ready = state as? AlumniProfileUiState.Ready
    val isOwn = ready?.profile?.isOwnProfile == true

    AlumniPremiumTheme {
    previewImageUrl?.let { url ->
        AlumniProfileImagePreview(url = url, onDismiss = { previewImageUrl = null })
    }

    if (followListState.visible) {
        AlumniFollowListDialog(
            state = followListState,
            viewerId = ready?.viewerId,
            onDismiss = viewModel::closeFollowList,
            onTabSelected = viewModel::selectFollowListTab,
            onOpenProfile = { targetId ->
                viewModel.closeFollowList()
                navController.navigate(Screen.AlumniProfileDetail.createRoute(targetId)) {
                    launchSingleTop = true
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppPageHeader(
                title = "PROFIL ALUMNI",
                subtitle = ready?.profile?.alumni?.fullName ?: "Pondok Pesantren Al-Hasanah",
                isDark = isSystemInDarkTheme(),
                onBack = { navController.popBackStack() },
                size = AppPageHeaderSize.Compact,
                rightAction = {
                    if (isOwn) {
                        IconButton(onClick = { navController.navigate(Screen.AlumniSettings.route) }) {
                            Icon(Icons.Default.Settings, "Pengaturan", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            AlumniProfileUiState.Loading -> AlumniCenterState(
                modifier = Modifier.padding(padding), title = "Memuat profil...", showLoading = true
            )
            AlumniProfileUiState.LoginRequired -> AlumniCenterState(
                modifier = Modifier.padding(padding),
                title = "Masuk untuk melanjutkan",
                description = "Profil alumni hanya tersedia untuk pengguna yang sudah masuk."
            )
            is AlumniProfileUiState.Error -> AlumniCenterState(
                modifier = Modifier.padding(padding),
                title = "Gagal memuat profil", description = s.message,
                actionText = "Coba Lagi", onAction = { viewModel.loadProfile(alumniId) }
            )
            is AlumniProfileUiState.Ready -> {
                // Auto-dismiss toast
                LaunchedEffect(s.message) {
                    if (s.message != null) {
                        kotlinx.coroutines.delay(3_000)
                        viewModel.clearMessage()
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // ── Toast banner ──
                    s.message?.let { msg ->
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.inverseSurface)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(msg, fontSize = 13.sp, color = MaterialTheme.colorScheme.inverseOnSurface)
                            }
                        }
                    }

                    // ── Profile header ──
                    item {
                        ProfileHero(
                            alumni        = s.profile.alumni,
                            avatarUrl     = s.profile.avatarUrl,
                            postCount     = s.profile.postCount,
                            commentCount  = s.profile.commentCount,
                            reactionCount = s.profile.reactionCount,
                            followerCount = s.profile.followStats.followerCount,
                            followingCount = s.profile.followStats.followingCount,
                            followedByMe  = s.profile.followStats.followedByMe,
                            isSaving      = s.isSaving,
                            isOwn         = isOwn,
                            onEditProfile = { navController.navigate(Screen.AlumniProfileEdit.route) },
                            onToggleFollow = viewModel::toggleFollow,
                            onOpenFollowList = viewModel::loadFollowList,
                            onMessage = {
                                navController.navigate(Screen.AlumniChat.createDirectRoute(s.profile.alumni.id)) {
                                    launchSingleTop = true
                                }
                            },
                            onShareProfile = {
                                shareAlumniProfile(
                                    context = context,
                                    alumni = s.profile.alumni
                                )
                            }
                        )
                    }

                    // ── Tab bar ──
                    item {
                        ProfileTabBar(
                            selectedTab    = selectedTab,
                            onTabSelected  = { selectedTab = it }
                        )
                    }

                    // ── Tab content ──
                    when (selectedTab) {

                        ProfileTab.POSTS -> {
                            if (s.profile.posts.isEmpty()) {
                                item { EmptyTabState(Icons.Default.Forum, "Belum ada postingan") }
                            } else {
                                items(s.profile.posts, key = { it.thread.id }) { post ->
                                    val isLast = post == s.profile.posts.last()
                                    ThreadsPostItem(
                                        post          = post,
                                        alumniName    = s.profile.alumni.fullName,
                                        alumniAvatar  = s.profile.avatarUrl,
                                        isLast        = isLast,
                                        onRepost      = { viewModel.repostThread(post.thread.id, post.thread.content) },
                                        onShare       = {
                                            shareAlumniText(
                                                context = context,
                                                text = "${s.profile.alumni.fullName} di Forum Alumni Al-Hasanah:\n\n${post.thread.content}"
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        ProfileTab.REPLIES -> {
                            if (s.profile.replies.isEmpty()) {
                                item { EmptyTabState(Icons.Default.ChatBubbleOutline, "Belum ada balasan") }
                            } else {
                                items(s.profile.replies, key = { it.comment.id }) { reply ->
                                    ProfileReplyItem(
                                        reply = reply,
                                        alumniName = s.profile.alumni.fullName,
                                        alumniAvatar = s.profile.avatarUrl,
                                        onOpenForum = { navController.navigate(Screen.AlumniForum.createRoute()) }
                                    )
                                }
                            }
                        }

                        ProfileTab.MEDIA -> {
                            val mediaPosts = s.profile.posts.filter { it.attachments.isNotEmpty() }
                            if (mediaPosts.isEmpty()) {
                                item { EmptyTabState(Icons.Default.Photo, "Belum ada media") }
                            } else {
                                items(mediaPosts.chunked(3), key = { it.first().thread.id }) { chunk ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        chunk.forEach { post ->
                                            post.attachments.firstOrNull()?.let { att ->
                                                AsyncImage(
                                                    model              = att.signedUrl,
                                                    contentDescription = null,
                                                    modifier           = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .clickable { previewImageUrl = att.signedUrl },
                                                    contentScale       = ContentScale.Crop
                                                )
                                            }
                                        }
                                        repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

private val AlumniDataDto.isVerified: Boolean
    get() = true

private fun shareAlumniText(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan"))
}

private fun shareAlumniProfile(context: android.content.Context, alumni: AlumniDataDto) {
    val appLink = "alhasanahmedia://alumni/profile/${alumni.id}"
    val webLink = "https://alhasanah.media/alumni/profile/${alumni.id}"
    shareAlumniText(
        context = context,
        text = buildString {
            append("Profil Alumni Al-Hasanah\n")
            append(alumni.fullName)
            append(" · Angkatan ")
            append(alumni.tahunLulus)
            alumni.bio?.takeIf { it.isNotBlank() }?.let {
                append("\n\n")
                append(it)
            }
            append("\n\nBuka di aplikasi:\n")
            append(appLink)
            append("\n\nLink web:\n")
            append(webLink)
        }
    )
}

@Composable
private fun AlumniFollowListDialog(
    state: FollowListUiState,
    viewerId: String?,
    onDismiss: () -> Unit,
    onTabSelected: (FollowListTab) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Kembali", modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "Koneksi Alumni",
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth()) {
                    FollowListTabButton(
                        label = "Pengikut",
                        count = state.followers.size,
                        selected = state.selectedTab == FollowListTab.FOLLOWERS,
                        onClick = { onTabSelected(FollowListTab.FOLLOWERS) },
                        modifier = Modifier.weight(1f)
                    )
                    FollowListTabButton(
                        label = "Mengikuti",
                        count = state.following.size,
                        selected = state.selectedTab == FollowListTab.FOLLOWING,
                        onClick = { onTabSelected(FollowListTab.FOLLOWING) },
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                when {
                    state.isLoading -> AlumniCenterState(
                        modifier = Modifier.weight(1f),
                        title = "Memuat koneksi...",
                        showLoading = true
                    )
                    state.errorMessage != null -> AlumniCenterState(
                        modifier = Modifier.weight(1f),
                        title = "Gagal memuat",
                        description = state.errorMessage
                    )
                    else -> {
                        val items = if (state.selectedTab == FollowListTab.FOLLOWERS) {
                            state.followers
                        } else {
                            state.following
                        }
                        if (items.isEmpty()) {
                            AlumniCenterState(
                                modifier = Modifier.weight(1f),
                                title = if (state.selectedTab == FollowListTab.FOLLOWERS) "Belum ada pengikut" else "Belum mengikuti alumni",
                                description = "Daftar koneksi alumni akan muncul di sini."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(items, key = { it.alumni.id }) { item ->
                                    AlumniFollowUserRow(
                                        item = item,
                                        isMe = item.alumni.id == viewerId,
                                        onClick = { onOpenProfile(item.alumni.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowListTabButton(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$label ${formatCount(count)}",
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent)
        )
    }
}

@Composable
private fun AlumniFollowUserRow(
    item: AlumniFollowUser,
    isMe: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileAvatar(url = item.avatarUrl, name = item.alumni.fullName, size = 46)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.alumni.fullName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isMe) {
                    Text(
                        text = "  Anda",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            val subtitle = listOfNotNull(
                "Angkatan ${item.alumni.tahunLulus}",
                item.alumni.profesiSekarang?.takeIf { it.isNotBlank() },
                item.alumni.regencyName?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            Text(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!isMe) {
            Text(
                text = if (item.followedByMe) "Mengikuti" else "Lihat",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  ProfileHero  — Threads-style: name+avatar row → bio → info → stats → buttons
// ════════════════════════════════════════════════════════════════

@Composable
private fun ProfileHero(
    alumni       : AlumniDataDto,
    avatarUrl    : String?,
    postCount    : Int,
    commentCount : Int,
    reactionCount: Int,
    followerCount: Int,
    followingCount: Int,
    followedByMe : Boolean,
    isSaving     : Boolean,
    isOwn        : Boolean,
    onEditProfile: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenFollowList: (FollowListTab) -> Unit,
    onMessage    : () -> Unit,
    onShareProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // ── Name / Handle / Avatar row (exactly like Threads) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text          = alumni.fullName,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 22.sp,
                    letterSpacing = (-0.5).sp,
                    lineHeight    = 27.sp,
                    color         = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text  = "@${alumni.fullName.lowercase().replace(" ", ".")}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (alumni.isVerified == true) {
                        Box(
                            modifier         = Modifier
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check, "Terverifikasi",
                                tint     = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            ) {
                ProfileAvatar(url = avatarUrl, name = alumni.fullName, size = 76)
            }
        }

        // ── Bio ──
        if (!alumni.bio.isNullOrBlank()) {
            Text(
                text       = alumni.bio,
                fontSize   = 14.sp,
                lineHeight = 20.sp,
                color      = MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(10.dp))
        }

        // ── Info rows (profession · location · graduation) ──
        val infoItems = buildList<Pair<ImageVector, String>> {
            if (alumni.showProfession && !alumni.profesiSekarang.isNullOrBlank())
                add(Icons.Default.Work to alumni.profesiSekarang!!)
            if (alumni.showProfession && !alumni.instansiKerja.isNullOrBlank())
                add(Icons.Default.Business to alumni.instansiKerja!!)
            val location = alumniProfileLocationLabel(alumni)
            if (alumni.showLocation && location.isNotBlank())
                add(Icons.Default.LocationOn to location)
            add(Icons.Default.School to "Angkatan ${alumni.tahunLulus}")
        }
        if (infoItems.isNotEmpty()) {
            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                infoItems.forEach { (icon, label) ->
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Stats rows ──
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(formatCount(reactionCount), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("suka", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" · ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatCount(postCount), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("postingan", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" · ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatCount(commentCount), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("komentar", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    formatCount(followerCount),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onOpenFollowList(FollowListTab.FOLLOWERS) }
                )
                Text(
                    "pengikut",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onOpenFollowList(FollowListTab.FOLLOWERS) }
                )
                Text(" · ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatCount(followingCount),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onOpenFollowList(FollowListTab.FOLLOWING) }
                )
                Text(
                    "mengikuti",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onOpenFollowList(FollowListTab.FOLLOWING) }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Action buttons ──
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isOwn) {
                OutlinedButton(
                    onClick         = onEditProfile,
                    modifier        = Modifier.weight(1f).height(36.dp),
                    shape           = RoundedCornerShape(10.dp),
                    border          = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    contentPadding  = PaddingValues(0.dp)
                ) { Text("Edit Profil", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                OutlinedButton(
                    onClick        = onShareProfile,
                    modifier       = Modifier.weight(1f).height(36.dp),
                    shape          = RoundedCornerShape(10.dp),
                    border         = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("Bagikan Profil", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            } else {
                val followLabel = if (followedByMe) "Mengikuti" else "Ikuti"
                val followColors = if (followedByMe) {
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                }
                Button(
                    onClick        = onToggleFollow,
                    enabled        = !isSaving,
                    modifier       = Modifier.weight(1f).height(36.dp),
                    shape          = RoundedCornerShape(10.dp),
                    colors         = followColors,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(followLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick        = onMessage,
                    modifier       = Modifier.weight(1f).height(36.dp),
                    shape          = RoundedCornerShape(10.dp),
                    border         = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("Pesan", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ── Tab bar ──
@Composable
private fun ProfileTabBar(
    selectedTab  : ProfileTab,
    onTabSelected: (ProfileTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            ProfileTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { onTabSelected(tab) }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = tab.label,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize   = 14.sp,
                        color      = if (selected) MaterialTheme.colorScheme.onSurface
                                     else          MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Animated underline indicator
        Row(modifier = Modifier.fillMaxWidth()) {
            ProfileTab.entries.forEach { tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            if (tab == selectedTab) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  ThreadsPostItem  — Threads-exact post layout with connector line
// ════════════════════════════════════════════════════════════════

@Composable
private fun ThreadsPostItem(
    post        : ForumThreadItem,
    alumniName  : String,
    alumniAvatar: String?,
    isLast      : Boolean,
    onRepost    : () -> Unit,
    onShare     : () -> Unit
) {
    var liked by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue  = if (liked) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label        = "heart_scale"
    )
    val heartTint by animateColorAsState(
        targetValue = if (liked) MaterialTheme.colorScheme.error
                      else       MaterialTheme.colorScheme.onSurfaceVariant,
        label       = "heart_tint"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .height(IntrinsicSize.Min)             // key for connector line
    ) {
        // ── Left column: avatar + vertical thread connector ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier
                .width(40.dp)
                .fillMaxHeight()                   // match height of right column
        ) {
            // Avatar
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                ProfileAvatar(url = alumniAvatar, name = alumniName, size = 40)
            }
            // Thread connector line (all posts except last)
            if (!isLast) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .weight(1f)                // fills remaining vertical space
                        .background(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        Spacer(Modifier.width(10.dp))

        // ── Right column: meta + content + actions ──
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 14.dp else 6.dp)
        ) {
            // Author row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = alumniName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // relative time placeholder — use post.thread.createdAt if present in model
                    Text("baru saja", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(
                        Icons.Default.MoreHoriz, "Opsi",
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Post content
            Text(
                text     = post.thread.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            // Attached image
            post.attachments.firstOrNull()?.let { att ->
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model              = att.signedUrl,
                    contentDescription = att.attachment.altText,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale       = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Action row (Heart · Comment · Repost · Share) ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Like
                PostActionButton(
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Icon(
                        imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Suka",
                        tint     = heartTint,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer(scaleX = heartScale, scaleY = heartScale)
                    )
                    if (post.thread.reactionCount + (if (liked) 1 else 0) > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            formatCount(post.thread.reactionCount + if (liked) 1 else 0),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Comment
                PostActionButton(modifier = Modifier.padding(end = 12.dp)) {
                    Icon(Icons.Default.ChatBubbleOutline, "Komentar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    if (post.thread.commentCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(formatCount(post.thread.commentCount), fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // Repost
                PostActionButton(modifier = Modifier.padding(end = 12.dp), onClick = onRepost) {
                    Icon(Icons.Default.Repeat, "Repost",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                // Share
                PostActionButton(onClick = onShare) {
                    Icon(Icons.Default.Share, "Bagikan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                // Activate like on tap
                LaunchedEffect(Unit) { /* placeholder for real like toggle */ }
            }
        }
    }

    // Divider only after last post to separate from next section
    if (isLast) HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

    // Handle like click — placed via the button above
    // (In production: trigger via onLike callback to ViewModel)
}

@Composable
private fun ProfileReplyItem(
    reply: ForumCommentItem,
    alumniName: String,
    alumniAvatar: String?,
    onOpenForum: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onOpenForum
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        ProfileAvatar(url = alumniAvatar, name = alumniName, size = 38)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    alumniName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    formatRelativeTime(reply.comment.createdAt),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                reply.comment.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (reply.comment.reactionCount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        formatCount(reply.comment.reactionCount),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun AlumniProfileImagePreview(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.94f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Preview media alumni",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun PostActionButton(
    modifier: Modifier = Modifier,
    onClick : () -> Unit = {},
    content : @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
            .padding(top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
private fun EmptyTabState(icon: ImageVector, message: String) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(36.dp))
        Text(message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ════════════════════════════════════════════════════════════════
//  AlumniProfileEditScreen
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlumniProfileEditScreen(
    navController: NavController,
    viewModel    : AlumniProfileViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadProfile() }

    AlumniPremiumTheme {
    Scaffold(
        topBar = {
            AppPageHeader(
                title = "EDIT PROFIL",
                subtitle = "Perbarui data alumni",
                isDark = isSystemInDarkTheme(),
                onBack = { navController.popBackStack() },
                size = AppPageHeaderSize.Compact
            )
        }
    ) { padding ->
        when (val s = state) {
            AlumniProfileUiState.Loading -> AlumniCenterState(Modifier.padding(padding), "Memuat...", showLoading = true)
            AlumniProfileUiState.LoginRequired -> AlumniCenterState(Modifier.padding(padding), "Silakan masuk terlebih dahulu.")
            is AlumniProfileUiState.Error -> AlumniCenterState(Modifier.padding(padding), "Gagal memuat profil", s.message)
            is AlumniProfileUiState.Ready -> {
                LaunchedEffect(s.message) {
                    if (s.message == "Profil alumni diperbarui.") {
                        kotlinx.coroutines.delay(900)
                        navController.popBackStack()
                    }
                }
                AlumniProfileForm(
                    modifier = Modifier.padding(padding),
                    state    = s,
                    onSave   = viewModel::saveProfile,
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
    }
}

@Composable
private fun AlumniProfileForm(
    modifier: Modifier = Modifier,
    state   : AlumniProfileUiState.Ready,
    onSave  : (UpdateAlumniProfileDto, Uri?) -> Unit,
    onCancel: () -> Unit
) {
    val alumni      = state.profile.alumni
    var fullName    by remember(alumni.id) { mutableStateOf(alumni.fullName) }
    var noWa        by remember(alumni.id) { mutableStateOf(alumni.noWa.orEmpty()) }
    var profession  by remember(alumni.id) { mutableStateOf(alumni.profesiSekarang.orEmpty()) }
    var institution by remember(alumni.id) { mutableStateOf(alumni.instansiKerja.orEmpty()) }
    var location    by remember(alumni.id) { mutableStateOf(alumniProfileLocationLabel(alumni)) }
    var bio         by remember(alumni.id) { mutableStateOf(alumni.bio.orEmpty()) }
    var avatarUri   by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        avatarUri = uri
    }

    LazyColumn(
        modifier       = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        state.message?.let { message ->
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // ── Avatar picker ──
        item {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier         = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { imagePicker.launch("image/*") }
                    ) {
                        val displayUrl = avatarUri?.toString() ?: state.profile.avatarUrl
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!displayUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model              = displayUrl,
                                    contentDescription = "Foto profil",
                                    modifier           = Modifier.fillMaxSize(),
                                    contentScale       = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    alumniInitialsOf(alumni.fullName).ifEmpty { "A" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 28.sp,
                                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        // Camera badge
                        Box(
                            modifier         = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null,
                                tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text("Ganti foto profil", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── Informasi Dasar ──
        item {
            EditFormSection("INFORMASI DASAR") {
                InlineFormField(Icons.Default.Person, "Nama Lengkap", fullName, { fullName = it }, required = true)
                InlineFormField(Icons.Default.Edit, "Bio", bio,
                    onValueChange = { if (it.length <= 300) bio = it },
                    multiline     = true,
                    trailingInfo  = "${bio.length}/300"
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── Pekerjaan ──
        item {
            EditFormSection("PEKERJAAN") {
                InlineFormField(Icons.Default.Work, "Profesi", profession, { profession = it })
                InlineFormField(Icons.Default.Business, "Instansi / Perusahaan", institution, { institution = it })
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── Kontak ──
        item {
            EditFormSection("KONTAK & LOKASI") {
                InlineFormField(Icons.Default.LocationOn, "Kota Domisili", location, { location = it })
                InlineFormField(Icons.Default.Phone, "Nomor WhatsApp", noWa, { noWa = it })
            }
        }

        item { Spacer(Modifier.height(28.dp)) }

        // ── Save button ──
        item {
            Button(
                onClick = {
                    onSave(
                        UpdateAlumniProfileDto(
                            fullName       = fullName,
                            bio            = bio.takeIf { it.isNotBlank() },
                            profesiSekarang = profession.takeIf { it.isNotBlank() },
                            instansiKerja   = institution.takeIf { it.isNotBlank() },
                            alamatDomisili  = location.takeIf { it.isNotBlank() },
                            provinceCode = alumni.provinceCode,
                            provinceName = alumni.provinceName,
                            regencyCode = alumni.regencyCode,
                            regencyName = alumni.regencyName,
                            districtCode = alumni.districtCode,
                            districtName = alumni.districtName,
                            villageCode = alumni.villageCode,
                            villageName = alumni.villageName,
                            postalCode = alumni.postalCode,
                            addressDetail = alumni.addressDetail,
                            noWa            = noWa.takeIf { it.isNotBlank() },
                            avatarStoragePath = alumni.avatarStoragePath,
                            showWhatsapp = alumni.showWhatsapp,
                            showProfession = alumni.showProfession,
                            showLocation = alumni.showLocation,
                            forumNotifyReplies = alumni.forumNotifyReplies,
                            forumNotifyReactions = alumni.forumNotifyReactions
                        ),
                        avatarUri
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape  = RoundedCornerShape(12.dp),
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.surface
                    )
                } else {
                    Text("Simpan Perubahan", fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.surface)
                }
            }
        }
    }
}

@Composable
private fun EditFormSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text          = title,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.6.sp,
            modifier      = Modifier.padding(start = 16.dp, bottom = 4.dp)
        )
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
            Column { content() }
        }
    }
}

@Composable
private fun InlineFormField(
    icon         : ImageVector,
    label        : String,
    value        : String,
    onValueChange: (String) -> Unit,
    required     : Boolean = false,
    multiline    : Boolean = false,
    trailingInfo : String? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp)) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Text(
                if (required) "$label *" else label,
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (trailingInfo != null) {
                Spacer(Modifier.weight(1f))
                Text(trailingInfo, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier.fillMaxWidth(),
            textStyle     = TextStyle(
                fontSize   = 14.sp,
                lineHeight = 20.sp,
                color      = MaterialTheme.colorScheme.onSurface
            ),
            minLines      = if (multiline) 3 else 1,
            maxLines      = if (multiline) 8 else 1,
            cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            "Tambahkan $label...",
                            fontSize   = 14.sp,
                            lineHeight = 20.sp,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }
                    inner()
                }
            }
        )
    }
    HorizontalDivider(
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant,
        modifier  = Modifier.padding(start = 38.dp)
    )
}

// ════════════════════════════════════════════════════════════════
//  AlumniSettingsScreen  — iOS-inspired grouped settings
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlumniSettingsScreen(
    navController: NavController,
    viewModel    : AlumniProfileViewModel = koinViewModel(),
    mainViewModel: MainViewModel = koinViewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadProfile() }
    val state  by viewModel.uiState.collectAsState()
    val themeMode by mainViewModel.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme = themeMode ?: isSystemDark
    val ready   = state as? AlumniProfileUiState.Ready
    val alumni  = ready?.profile?.alumni

    var showProfession by remember(alumni) { mutableStateOf(alumni?.showProfession ?: true) }
    var showLocation   by remember(alumni) { mutableStateOf(alumni?.showLocation   ?: true) }
    var showWhatsapp   by remember(alumni) { mutableStateOf(alumni?.showWhatsapp   ?: true) }
    var notifForum     by remember(alumni) { mutableStateOf(alumni?.forumNotifyReplies ?: true) }
    var notifMention   by remember { mutableStateOf(true) }
    var notifLike      by remember(alumni) { mutableStateOf(alumni?.forumNotifyReactions ?: true) }

    fun saveCurrentSettings(
        profession: Boolean = showProfession,
        location: Boolean = showLocation,
        whatsapp: Boolean = showWhatsapp,
        forum: Boolean = notifForum,
        like: Boolean = notifLike
    ) {
        viewModel.saveSettings(
            showProfession = profession,
            showLocation = location,
            showWhatsapp = whatsapp,
            forumNotifyReplies = forum,
            forumNotifyReactions = like
        )
    }

    AlumniPremiumTheme {
    Scaffold(
        topBar = {
            AppPageHeader(
                title = "PENGATURAN",
                subtitle = alumni?.fullName ?: "Akun alumni",
                isDark = useDarkTheme,
                onBack = { navController.popBackStack() },
                size = AppPageHeaderSize.Compact
            )
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // ── Account info card ──
            alumni?.let { a ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape    = RoundedCornerShape(14.dp),
                        color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier              = Modifier.padding(14.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProfileAvatar(url = ready?.profile?.avatarUrl, name = a.fullName, size = 46)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(a.fullName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Angkatan ${a.tahunLulus}", fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (a.isVerified == true) {
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                                    Text("✓ Terverifikasi", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── AKUN ──
            item { SettingsHeader("AKUN") }
            item {
                SettingsCard {
                    SettingsNavRow(Icons.Default.Edit, "Edit Profil", "Ubah nama, bio, dan foto profil") {
                        navController.navigate(Screen.AlumniProfileEdit.route)
                    }
                    SettingsNavRow(Icons.Default.Lock, "Kata Sandi", "Ubah kata sandi akun Anda", isLast = true) {}
                }
            }

            // ── PRIVASI ──
            item { SettingsHeader("PRIVASI") }
            item {
                SettingsCard {
                    SettingsToggle(Icons.Default.Work, "Tampilkan Pekerjaan",
                        "Profesi dan instansi terlihat oleh sesama alumni", showProfession) {
                        showProfession = it
                        saveCurrentSettings(profession = it)
                    }
                    SettingsToggle(Icons.Default.LocationOn, "Tampilkan Lokasi",
                        "Kota domisili terlihat di profil Anda", showLocation) {
                        showLocation = it
                        saveCurrentSettings(location = it)
                    }
                    SettingsToggle(Icons.Default.Phone, "Tampilkan WhatsApp",
                        "Nomor WhatsApp terlihat oleh alumni lain", showWhatsapp, isLast = true) {
                        showWhatsapp = it
                        saveCurrentSettings(whatsapp = it)
                    }
                }
            }

            // ── TAMPILAN ──
            item { SettingsHeader("TAMPILAN") }
            item {
                SettingsCard {
                    SettingsToggle(Icons.Default.Settings, "Mode Gelap",
                        "Gunakan tema gelap di aplikasi", useDarkTheme, isLast = true) {
                        mainViewModel.toggleTheme(isSystemDark)
                    }
                }
            }

            // ── NOTIFIKASI ──
            item { SettingsHeader("NOTIFIKASI") }
            item {
                SettingsCard {
                    SettingsToggle(Icons.Default.Forum, "Postingan Forum",
                        "Notifikasi postingan baru di forum", notifForum) {
                        notifForum = it
                        saveCurrentSettings(forum = it)
                    }
                    SettingsToggle(Icons.Default.AlternateEmail, "Sebutan (@mention)",
                        "Saat seseorang menyebut Anda di postingan", notifMention) { notifMention = it }
                    SettingsToggle(Icons.Default.Favorite, "Suka & Reaksi",
                        "Saat seseorang menyukai postingan Anda", notifLike, isLast = true) {
                        notifLike = it
                        saveCurrentSettings(like = it)
                    }
                }
            }

            // ── TENTANG ──
            item { SettingsHeader("TENTANG") }
            item {
                SettingsCard {
                    SettingsNavRow(Icons.Default.Info, "Tentang Aplikasi",
                        "Forum Alumni Pondok Pesantren Al-Hasanah") {
                        navController.navigate(Screen.AlumniInfo.createRoute("about"))
                    }
                    SettingsNavRow(Icons.Default.PrivacyTip, "Kebijakan Privasi",
                        "Baca kebijakan privasi kami") {
                        navController.navigate(Screen.AlumniInfo.createRoute("privacy"))
                    }
                    SettingsNavRow(Icons.Default.HelpOutline, "Bantuan & Dukungan",
                        "Hubungi tim kami jika ada masalah", isLast = true) {
                        navController.navigate(Screen.AlumniInfo.createRoute("support"))
                    }
                }
            }

            // ── KELUAR ──
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape    = RoundedCornerShape(14.dp),
                    color    = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier              = Modifier
                            .clickable { /* logout action */ }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text("Keluar dari Akun", fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item {
                Text(
                    "Forum Alumni Al-Hasanah · v1.0.0",
                    fontSize  = 11.sp,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlumniInfoScreen(
    navController: NavController,
    page: String
) {
    val content = remember(page) {
        when (page) {
            "privacy" -> AlumniStaticPage(
                title = "Kebijakan Privasi",
                subtitle = "Kebijakan privasi Forum Alumni Al-Hasanah",
                sections = listOf(
                    "Data profil alumni" to "Aplikasi menyimpan data yang Anda berikan untuk kebutuhan direktori dan forum alumni, seperti nama, angkatan, bio, profesi, domisili, dan kontak yang Anda izinkan untuk tampil.",
                    "Konten forum" to "Postingan, komentar, reaksi, lampiran gambar, dan laporan moderasi digunakan untuk menjaga forum tetap relevan dan aman bagi komunitas alumni.",
                    "Kontrol privasi" to "Anda dapat mengatur visibilitas pekerjaan, lokasi, dan WhatsApp dari halaman Pengaturan Alumni.",
                    "Keamanan" to "Akses forum dibatasi untuk akun alumni yang terverifikasi. Konten yang dilaporkan dapat ditinjau oleh admin."
                )
            )
            "support" -> AlumniStaticPage(
                title = "Bantuan & Dukungan",
                subtitle = "Pusat bantuan awal untuk penggunaan Forum Alumni",
                sections = listOf(
                    "Akun alumni" to "Jika data alumni Anda belum terverifikasi, hubungi admin pondok atau tunggu proses peninjauan dari panel admin.",
                    "Masalah posting" to "Pastikan koneksi internet stabil. Untuk gambar, gunakan JPG, PNG, atau WebP dengan ukuran maksimal 5 MB.",
                    "Moderasi" to "Gunakan fitur laporkan pada postingan atau komentar yang tidak sesuai. Admin akan meninjau laporan tersebut.",
                    "Kontak dukungan" to "Template ini dapat disambungkan ke WhatsApp, email, atau halaman bantuan resmi pada tahap berikutnya."
                )
            )
            else -> AlumniStaticPage(
                title = "Tentang Aplikasi",
                subtitle = "Forum Alumni Pondok Pesantren Al-Hasanah",
                sections = listOf(
                    "Tujuan" to "Forum Alumni dibuat sebagai ruang silaturahmi, berbagi kabar, dan memperkuat jejaring alumni Al-Hasanah.",
                    "Fitur utama" to "Alumni dapat membuat postingan, mengunggah gambar, memberi reaksi, berdiskusi melalui komentar, melihat direktori alumni, dan mengatur profil pribadi.",
                    "Verifikasi" to "Akses forum diberikan kepada alumni yang sudah terverifikasi agar komunitas tetap tertata.",
                    "Versi" to "Template halaman informasi v1.0.0."
                )
            )
        }
    }

    AlumniPremiumTheme {
    Scaffold(
        topBar = {
            AppPageHeader(
                title = content.title.uppercase(),
                subtitle = "Informasi alumni",
                isDark = isSystemInDarkTheme(),
                onBack = { navController.popBackStack() },
                size = AppPageHeaderSize.Compact
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    content.subtitle,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
            }
            items(content.sections, key = { it.first }) { (title, body) ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            body,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    }
}

private data class AlumniStaticPage(
    val title: String,
    val subtitle: String,
    val sections: List<Pair<String, String>>
)

// ── Settings helpers ──

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text          = title,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.7.sp,
        modifier      = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape    = RoundedCornerShape(14.dp),
        color    = MaterialTheme.colorScheme.surface
    ) { Column { content() } }
}

@Composable
private fun SettingsNavRow(
    icon    : ImageVector,
    title   : String,
    subtitle: String,
    isLast  : Boolean = false,
    onClick : () -> Unit
) {
    Column {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(17.dp))
        }
        if (!isLast) HorizontalDivider(
            modifier  = Modifier.padding(start = 60.dp),
            thickness = 0.5.dp,
            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SettingsToggle(
    icon          : ImageVector,
    title         : String,
    subtitle      : String,
    checked       : Boolean,
    isLast        : Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(34.dp)
                    .background(
                        if (checked) MaterialTheme.colorScheme.primaryContainer
                        else         MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint     = if (checked) MaterialTheme.colorScheme.primary
                               else         MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        if (!isLast) HorizontalDivider(
            modifier  = Modifier.padding(start = 60.dp),
            thickness = 0.5.dp,
            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

// ════════════════════════════════════════════════════════════════
//  Shared Composables
// ════════════════════════════════════════════════════════════════

/**
 * Renders an avatar: photo if URL is present, else initials fallback.
 * Exposed as `internal` so ForumScreen can reuse it without duplication.
 */
@Composable
internal fun ProfileAvatar(url: String?, name: String, size: Int) {
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model              = url,
            contentDescription = "Foto profil $name",
            modifier           = Modifier.size(size.dp).clip(CircleShape),
            contentScale       = ContentScale.Crop
        )
    } else {
        Box(
            modifier         = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = alumniInitialsOf(name).ifEmpty { "A" },
                fontWeight = FontWeight.Bold,
                fontSize   = (size / 3.2f).sp,
                color      = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** Full-screen loading / error / login-required state */
@Composable
private fun AlumniCenterState(
    modifier   : Modifier = Modifier,
    title      : String,
    description: String?  = null,
    showLoading: Boolean  = false,
    actionText : String?  = null,
    onAction   : (() -> Unit)? = null
) {
    Column(
        modifier            = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showLoading) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            Spacer(Modifier.height(16.dp))
        }
        Text(title, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, fontSize = 14.sp)
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(description, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(10.dp)) {
                Text(actionText, fontSize = 13.sp)
            }
        }
    }
}
