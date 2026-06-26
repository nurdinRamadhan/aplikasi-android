package com.alhasanah.alhasanahmedia.ui.alumni

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.alhasanah.alhasanahmedia.data.model.AlumniDataDto
import com.alhasanah.alhasanahmedia.data.model.AlumniRecommendationItem
import com.alhasanah.alhasanahmedia.data.model.ForumCommentItem
import com.alhasanah.alhasanahmedia.data.model.ForumThreadDetail
import com.alhasanah.alhasanahmedia.data.model.ForumThreadItem
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

// ─── Semantic Tokens ─────────────────────────────────────────────────────────
private val LoveRed         = Color(0xFFE53935)
private val PinGold         = Color(0xFFD4A017)
private val LockGray        = Color(0xFF9E9E9E)
private val OverlayBlack    = Color(0xFF000000)

// Avatar gradient palette — deterministic by name
private val AvatarPalettes = listOf(
    listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
    listOf(Color(0xFF2D2D2D), Color(0xFF525252)),
    listOf(Color(0xFF1B4332), Color(0xFF2D6A4F)),
    listOf(Color(0xFF0D1B2A), Color(0xFF1B4965)),
    listOf(Color(0xFF3D0000), Color(0xFF6B0000)),
    listOf(Color(0xFF1A0533), Color(0xFF3D1165)),
    listOf(Color(0xFF0F3460), Color(0xFF16213E)),
    listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF))
)

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun relativeTime(value: String): String {
    val createdAt = try { Instant.parse(value) }
    catch (_: DateTimeParseException) { return "" }
    val d = Duration.between(createdAt, Instant.now()).coerceAtLeast(Duration.ZERO)
    return when {
        d.toMinutes() < 1 -> "Baru"
        d.toHours()   < 1 -> "${d.toMinutes()}m"
        d.toDays()    < 1 -> "${d.toHours()}j"
        d.toDays()    < 7 -> "${d.toDays()}h"
        else              -> "${d.toDays() / 7}mg"
    }
}

private fun initialsOf(name: String): String =
    name.trim().split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

private fun avatarPalette(name: String): List<Color> =
    AvatarPalettes[Math.abs(name.hashCode()) % AvatarPalettes.size]

// ─── Screen Root ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumAlumniScreen(
    navController: NavController,
    initialThreadId: String? = null,
    viewModel: ForumAlumniViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var openedInitialThreadId by remember(initialThreadId) { mutableStateOf<String?>(null) }
    val baseColors = MaterialTheme.colorScheme
    val isDarkForum = baseColors.surface.luminance() < 0.5f
    val forumColors = if (isDarkForum) {
        baseColors.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF0B0B0B),
            onSurface = Color(0xFFF5F5F5),
            onSurfaceVariant = Color(0xFFB8B8B8),
            outlineVariant = Color(0xFF1C1C1C)
        )
    } else {
        baseColors
    }

    LaunchedEffect(initialThreadId, uiState) {
        val ready = uiState as? ForumAlumniUiState.Ready
        if (!initialThreadId.isNullOrBlank() && ready != null && openedInitialThreadId != initialThreadId) {
            openedInitialThreadId = initialThreadId
            viewModel.openThread(initialThreadId)
        }
    }

    MaterialTheme(colorScheme = forumColors) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AppSolidBackground(isDark = isDarkForum)
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = uiState !is ForumAlumniUiState.Ready,
                    enter = fadeIn(tween(180)) + slideInVertically(tween(220, easing = EaseOutCubic)) { -it },
                    exit = fadeOut(tween(160)) + slideOutVertically(tween(180)) { -it }
                ) {
                    ForumTopBar(
                        onBack     = { navController.popBackStack() },
                        onDirectory = { navController.navigate(Screen.AlumniDirectory.route) },
                        onProfile  = { navController.navigate(Screen.AlumniProfile.route) }
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        ForumAlumniUiState.Loading -> ForumCenterState(
                            title = "Memeriksa akses...",
                            showLoading = true
                        )
                        ForumAlumniUiState.LoginRequired -> ForumCenterState(
                            title = "Masuk untuk melanjutkan",
                            description = "Forum Alumni hanya untuk alumni Al-Hasanah yang terverifikasi.",
                            actionText = "Masuk",
                            onAction = { navController.navigate(Screen.Login.route) },
                            secondaryActionText = "Daftar",
                            onSecondaryAction = { navController.navigate(Screen.AlumniRegister.route) }
                        )
                        is ForumAlumniUiState.NotAlumni -> ForumCenterState(
                            title = "Akses terbatas",
                            description = "Akun Anda terdaftar sebagai ${state.role.ifBlank { "pengguna" }}. Forum ini khusus alumni Al-Hasanah."
                        )
                        is ForumAlumniUiState.WaitingVerification -> ForumCenterState(
                            title = "Menunggu verifikasi",
                            description = "Akun ${state.name.orEmpty()} sedang ditinjau admin. Forum akan terbuka setelah diverifikasi."
                        )
                        is ForumAlumniUiState.Ready -> AlumniFeed(
                            state = state,
                            onPost = viewModel::createThread,
                            onOpenThread = { viewModel.openThread(it.thread.id) },
                            onToggleLove = viewModel::toggleLove,
                            onRefresh = viewModel::refresh,
                            onOpenAuthor = { item ->
                                navController.navigate(Screen.AlumniProfileDetail.createRoute(item.thread.authorId))
                            },
                            onEditThread   = { item, content -> viewModel.updateThread(item.thread.id, content) },
                            onDeleteThread = { item -> viewModel.deleteThread(item.thread.id) },
                            onModerateThread = { item, status, pinned, locked ->
                                viewModel.moderateThread(item.thread.id, status, pinned, locked)
                            },
                            onReportThread = { item, reason, note -> viewModel.reportThread(item.thread.id, reason, note) },
                            onCloseThread  = viewModel::closeThread,
                            onCreateComment     = viewModel::createComment,
                            onToggleCommentLove = viewModel::toggleCommentLove,
                            onEditComment = { comment, content ->
                                viewModel.updateComment(comment.comment.id, comment.comment.threadId, content)
                            },
                            onDeleteComment = { comment ->
                                viewModel.deleteComment(comment.comment.id, comment.comment.threadId)
                            },
                            onReportComment = { comment, reason, note ->
                                viewModel.reportComment(comment.comment.id, reason, note)
                            },
                            onOpenRecommendedProfile = { alumniId ->
                                navController.navigate(Screen.AlumniProfileDetail.createRoute(alumniId))
                            },
                            onFollowAlumni = viewModel::followAlumni,
                            onClearMessage = viewModel::clearMessage,
                            onBack = { navController.popBackStack() },
                            onDirectory = { navController.navigate(Screen.AlumniDirectory.route) },
                            onProfile = { navController.navigate(Screen.AlumniProfile.route) }
                        )
                        is ForumAlumniUiState.Error -> ForumCenterState(
                            title = "Gagal memuat forum",
                            description = state.message,
                            actionText = "Coba Lagi",
                            onAction = viewModel::refresh
                        )
                    }
                }
            }
        }
    }
}

// ─── Custom Top Bar ───────────────────────────────────────────────────────────

@Composable
private fun ForumTopBar(
    onBack: () -> Unit,
    onDirectory: () -> Unit,
    onProfile: () -> Unit
) {
    AppPageHeader(
        title = "FORUM ALUMNI",
        subtitle = "Diskusi dan kabar alumni",
        isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
        onBack = onBack,
        size = AppPageHeaderSize.Compact,
        rightAction = {
            Row {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onDirectory),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(Icons.Rounded.Groups, contentDescription = "Direktori", tint = MaterialTheme.colorScheme.onSurface)
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onProfile),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(Icons.Rounded.Person, contentDescription = "Profil", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    )
}

// ─── Feed ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlumniFeed(
    state: ForumAlumniUiState.Ready,
    onPost: (String, Uri?) -> Unit,
    onOpenThread: (ForumThreadItem) -> Unit,
    onToggleLove: (ForumThreadItem) -> Unit,
    onRefresh: () -> Unit,
    onOpenAuthor: (ForumThreadItem) -> Unit,
    onEditThread: (ForumThreadItem, String) -> Unit,
    onDeleteThread: (ForumThreadItem) -> Unit,
    onModerateThread: (ForumThreadItem, String?, Boolean?, Boolean?) -> Unit,
    onReportThread: (ForumThreadItem, String, String?) -> Unit,
    onCloseThread: () -> Unit,
    onCreateComment: (String, String) -> Unit,
    onToggleCommentLove: (ForumCommentItem) -> Unit,
    onEditComment: (ForumCommentItem, String) -> Unit,
    onDeleteComment: (ForumCommentItem) -> Unit,
    onReportComment: (ForumCommentItem, String, String?) -> Unit,
    onOpenRecommendedProfile: (String) -> Unit,
    onFollowAlumni: (String) -> Unit,
    onClearMessage: () -> Unit,
    onBack: () -> Unit,
    onDirectory: () -> Unit,
    onProfile: () -> Unit
) {
    var content      by remember { mutableStateOf("") }
    var imageUri     by remember { mutableStateOf<Uri?>(null) }
    var previewUrl   by remember { mutableStateOf<String?>(null) }
    var showComposer by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    // Auto-dismiss transient toast
    LaunchedEffect(state.transientMessage) {
        if (state.transientMessage != null) {
            kotlinx.coroutines.delay(3000)
            onClearMessage()
        }
    }

    // Thread detail overlay
    state.selectedDetail?.let { detail ->
        ThreadDetailScreen(
            detail           = detail,
            currentUserId    = state.access.userId,
            isForumAdmin     = state.access.isForumAdmin,
            isLoading        = state.isDetailLoading,
            actionInProgress = state.actionInProgress,
            onDismiss        = onCloseThread,
            onSendComment    = { text -> onCreateComment(detail.item.thread.id, text) },
            onToggleLove     = { onToggleLove(detail.item) },
            onEditThread     = { text -> onEditThread(detail.item, text) },
            onDeleteThread   = { onDeleteThread(detail.item) },
            onModerateThread = { status, pinned, locked -> onModerateThread(detail.item, status, pinned, locked) },
            onReportThread   = { reason, note -> onReportThread(detail.item, reason, note) },
            onToggleCommentLove = onToggleCommentLove,
            onEditComment    = onEditComment,
            onDeleteComment  = onDeleteComment,
            onImageClick     = { previewUrl = it },
            onReportComment  = onReportComment
        )
    }

    // Image preview
    previewUrl?.let { url ->
        ImagePreviewDialog(url = url, onDismiss = { previewUrl = null })
    }

    if (showComposer) {
        PostComposerDialog(
            content         = content,
            onContentChange = { content = it },
            imageUri        = imageUri,
            onRemoveImage   = { imageUri = null },
            onPickImage     = { imagePicker.launch("image/*") },
            isPosting       = state.isPosting,
            onDismiss       = { showComposer = false },
            onPost          = {
                onPost(content, imageUri)
                content = ""
                imageUri = null
                showComposer = false
            }
        )
    }

    // Toast overlay
    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh    = onRefresh,
            modifier     = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    ForumTopBar(
                        onBack = onBack,
                        onDirectory = onDirectory,
                        onProfile = onProfile
                    )
                }

                // Compose box
                item {
                    ComposeBox(
                        onOpenComposer = { showComposer = true }
                    )
                    FeedDivider()
                }

                if (state.recommendations.isNotEmpty()) {
                    item {
                        FollowRecommendationSection(
                            recommendations = state.recommendations,
                            onOpenProfile = onOpenRecommendedProfile,
                            onFollow = { onFollowAlumni(it.alumni.id) }
                        )
                        FeedDivider()
                    }
                }

                // Empty state
                if (state.threads.isEmpty()) {
                    item {
                        ForumCenterState(
                            title       = "Belum ada kiriman",
                            description = "Jadilah yang pertama menyapa sesama alumni Al-Hasanah."
                        )
                    }
                } else {
                    items(state.threads, key = { it.thread.id }) { item ->
                        ThreadCard(
                            item          = item,
                            currentUserId = state.access.userId,
                            isForumAdmin  = state.access.isForumAdmin,
                            onOpen        = { onOpenThread(item) },
                            onOpenAuthor  = { onOpenAuthor(item) },
                            onToggleLove  = { onToggleLove(item) },
                            onEdit        = { c -> onEditThread(item, c) },
                            onDelete      = { onDeleteThread(item) },
                            onModerate    = { status, pinned, locked -> onModerateThread(item, status, pinned, locked) },
                            onImageClick  = { previewUrl = it },
                            onReport      = { reason, note -> onReportThread(item, reason, note) }
                        )
                        FeedDivider()
                    }
                }
            }
        }

        // Floating toast
        AnimatedVisibility(
            visible = state.transientMessage != null,
            enter   = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it },
            exit    = fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                androidx.compose.material3.Text(
                    text       = state.transientMessage ?: "",
                    color      = Color.White,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun FollowRecommendationSection(
    recommendations: List<AlumniRecommendationItem>,
    onOpenProfile: (String) -> Unit,
    onFollow: (AlumniRecommendationItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Text(
                text = "Alumni yang mungkin Anda kenal",
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            androidx.compose.material3.Text(
                text = "Direkomendasikan",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(recommendations.take(8), key = { it.alumni.id }) { item ->
                FollowRecommendationCard(
                    item = item,
                    onOpenProfile = { onOpenProfile(item.alumni.id) },
                    onFollow = { onFollow(item) }
                )
            }
        }
    }
}

@Composable
private fun FollowRecommendationCard(
    item: AlumniRecommendationItem,
    onOpenProfile: () -> Unit,
    onFollow: () -> Unit
) {
    val followInteraction = remember { MutableInteractionSource() }
    val followPressed by followInteraction.collectIsPressedAsState()
    val followScale by animateFloatAsState(
        targetValue = if (followPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "followScale"
    )
    val followContainer by animateColorAsState(
        targetValue = if (followPressed) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(160),
        label = "followContainer"
    )

    Column(
        modifier = Modifier
            .width(184.dp)
            .heightIn(min = 210.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
            .clickable(onClick = onOpenProfile)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(avatarPalette(item.alumni.fullName))),
            contentAlignment = Alignment.Center
        ) {
            if (!item.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                androidx.compose.material3.Text(
                    text = initialsOf(item.alumni.fullName),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        androidx.compose.material3.Text(
            text = item.alumni.fullName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        androidx.compose.material3.Text(
            text = recommendationSubtitle(item.alumni),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.Text(
            text = item.reason,
            modifier = Modifier.heightIn(min = 34.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .scale(followScale)
                .clip(RoundedCornerShape(10.dp))
                .background(followContainer)
                .clickable(
                    interactionSource = followInteraction,
                    indication = null,
                    onClick = onFollow
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = "Ikuti",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface
            )
        }
    }
}

private fun recommendationSubtitle(alumni: AlumniDataDto): String {
    val work = listOfNotNull(
        alumni.profesiSekarang?.takeIf { it.isNotBlank() },
        alumni.instansiKerja?.takeIf { it.isNotBlank() }
    ).joinToString(" di ")
    return when {
        work.isNotBlank() -> work
        !alumni.regencyName.isNullOrBlank() -> "${alumni.regencyName} · Angkatan ${alumni.tahunLulus}"
        else -> "Angkatan ${alumni.tahunLulus}"
    }
}

// ─── Compose Box ──────────────────────────────────────────────────────────────

@Composable
private fun ComposeBox(
    onOpenComposer: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "composeBoxScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onOpenComposer
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(avatarPalette("Me"))
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                tint     = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.material3.Text(
                text = "Bagikan kabar atau cerita alumni...",
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.Icon(
                Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PostComposerDialog(
    content: String,
    onContentChange: (String) -> Unit,
    imageUri: Uri?,
    onRemoveImage: () -> Unit,
    onPickImage: () -> Unit,
    isPosting: Boolean,
    onDismiss: () -> Unit,
    onPost: () -> Unit
) {
    val hasContent = content.isNotBlank() || imageUri != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220, easing = EaseOutCubic)) { it / 8 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(160)) { it / 8 }
        ) {
            Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text(
                    text = "Batal",
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onDismiss
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                androidx.compose.material3.Text(
                    text = "Buat Postingan",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (hasContent && !isPosting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(
                            enabled = hasContent && !isPosting,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onPost
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            strokeCap = StrokeCap.Round
                        )
                    } else {
                        androidx.compose.material3.Text(
                            text = "Kirim",
                            color = if (hasContent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(avatarPalette("Me"))),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        BasicTextField(
                            value = content,
                            onValueChange = onContentChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 260.dp),
                            textStyle = TextStyle(
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            minLines = 5,
                            maxLines = 12,
                            decorationBox = { inner ->
                                Box {
                                    if (content.isEmpty()) {
                                        androidx.compose.material3.Text(
                                            text = "Apa kabar alumni hari ini?",
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            lineHeight = 24.sp
                                        )
                                    }
                                    inner()
                                }
                            }
                        )

                        imageUri?.let { uri ->
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.62f))
                                        .clickable { onRemoveImage() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Hapus gambar",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onPickImage
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Rounded.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    androidx.compose.material3.Text(
                        "Tambahkan foto",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    androidx.compose.material3.Text(
                        "JPG, PNG, WebP",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
}

// ─── Thread Card ──────────────────────────────────────────────────────────────

@Composable
private fun ThreadCard(
    item: ForumThreadItem,
    currentUserId: String,
    isForumAdmin: Boolean,
    onOpen: () -> Unit,
    onOpenAuthor: () -> Unit,
    onToggleLove: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    onModerate: (String?, Boolean?, Boolean?) -> Unit,
    onImageClick: (String) -> Unit,
    onReport: (String, String?) -> Unit
) {
    val author = item.author
    var showOptions     by remember { mutableStateOf(false) }
    var showEditDialog  by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val isOwner = item.thread.authorId == currentUserId

    if (showEditDialog) {
        EditDialog(
            title       = "Edit Postingan",
            initialText = item.thread.content,
            minLines    = 4,
            onDismiss   = { showEditDialog = false },
            onSave      = { text -> showEditDialog = false; onEdit(text) }
        )
    }
    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title       = "Hapus postingan?",
            description = "Postingan dan semua komentarnya akan dihapus dari forum.",
            onDismiss   = { showDeleteDialog = false },
            onConfirm   = { showDeleteDialog = false; onDelete() }
        )
    }
    if (showReportDialog) {
        ReportBottomDialog(
            onDismiss = { showReportDialog = false },
            onSubmit  = { reason, note -> showReportDialog = false; onReport(reason, note) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onOpen
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // ── Left: avatar + vertical thread line ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                AuthorAvatar(
                    name    = author?.fullName ?: "A",
                    size    = 40,
                    onClick = onOpenAuthor
                )
                // Thread line (shows if there are comments)
                if (item.thread.commentCount > 0) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(1.dp)
                            )
                    )
                }
            }

            // ── Right: content ──
            Column(modifier = Modifier.weight(1f)) {
                // Name row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    androidx.compose.material3.Text(
                        text       = author?.fullName ?: "Alumni Al-Hasanah",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                    // Badges
                    if (item.thread.isPinned) {
                        androidx.compose.material3.Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = "Dipin",
                            tint     = PinGold,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    if (item.thread.isLocked) {
                        androidx.compose.material3.Icon(
                            Icons.Rounded.Lock,
                            contentDescription = "Dikunci",
                            tint     = LockGray,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    if (item.thread.status != "published") {
                        StatusPill(item.thread.status)
                    }

                    // Time
                    androidx.compose.material3.Text(
                        text     = relativeTime(item.thread.createdAt),
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 3-dot menu
                    Box {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { showOptions = true },
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Rounded.MoreHoriz,
                                contentDescription = "Opsi",
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        ThreadOptionsMenu(
                            expanded  = showOptions,
                            isOwner   = isOwner,
                            isForumAdmin = isForumAdmin,
                            isLocked  = item.thread.isLocked,
                            isPinned  = item.thread.isPinned,
                            status    = item.thread.status,
                            onDismiss = { showOptions = false },
                            onEdit    = { showOptions = false; showEditDialog = true },
                            onDelete  = { showOptions = false; showDeleteDialog = true },
                            onModerateStatus = { status -> showOptions = false; onModerate(status, null, null) },
                            onTogglePin = { showOptions = false; onModerate(null, !item.thread.isPinned, null) },
                            onToggleLock = { showOptions = false; onModerate(null, null, !item.thread.isLocked) },
                            onReport  = { showOptions = false; showReportDialog = true }
                        )
                    }
                }

                // Subtitle (profesi · angkatan)
                val subtitle = listOfNotNull(
                    author?.profesiSekarang,
                    author?.tahunLulus?.let { "Angkatan $it" }
                ).joinToString(" · ").ifBlank { "Alumni terverifikasi" }

                androidx.compose.material3.Text(
                    text     = subtitle,
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                if (item.thread.repostOfThreadId != null) {
                    androidx.compose.material3.Text(
                        text = "Posting ulang",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // Content text
                androidx.compose.material3.Text(
                    text       = item.thread.content,
                    fontSize   = 15.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )

                // Attachment image
                item.attachments.firstOrNull()?.let { att ->
                    Spacer(Modifier.height(10.dp))
                    AsyncImage(
                        model              = att.signedUrl,
                        contentDescription = att.attachment.altText,
                        modifier           = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(att.signedUrl) },
                        contentScale       = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Action bar
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    LoveButton(
                        lovedByMe = item.lovedByMe,
                        count     = item.thread.reactionCount,
                        onClick   = onToggleLove
                    )
                    CommentChip(count = item.thread.commentCount, onClick = onOpen)
                }
            }
        }
    }
}

// ─── Thread Detail Screen (Threads-style full-screen) ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadDetailScreen(
    detail: ForumThreadDetail,
    currentUserId: String,
    isForumAdmin: Boolean,
    isLoading: Boolean,
    actionInProgress: Boolean,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit,
    onToggleLove: () -> Unit,
    onEditThread: (String) -> Unit,
    onDeleteThread: () -> Unit,
    onModerateThread: (String?, Boolean?, Boolean?) -> Unit,
    onReportThread: (String, String?) -> Unit,
    onToggleCommentLove: (ForumCommentItem) -> Unit,
    onEditComment: (ForumCommentItem, String) -> Unit,
    onDeleteComment: (ForumCommentItem) -> Unit,
    onImageClick: (String) -> Unit,
    onReportComment: (ForumCommentItem, String, String?) -> Unit
) {
    var comment          by remember(detail.item.thread.id) { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    val listState        = rememberLazyListState()
    val scope            = rememberCoroutineScope()
    val focusRequester   = remember { FocusRequester() }
    val commentCount     = detail.comments.size

    if (showReportDialog) {
        ReportBottomDialog(
            onDismiss = { showReportDialog = false },
            onSubmit  = { reason, note -> showReportDialog = false; onReportThread(reason, note) }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Detail Top Bar ──
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = onDismiss
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Tutup",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        androidx.compose.material3.Text(
                            text       = "Diskusi",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { showReportDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Rounded.Report,
                                contentDescription = "Laporkan",
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    )
                }

                // ── Scrollable content: post + comments ──
                LazyColumn(
                    state    = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Post header (pinned at top)
                    item {
                        ThreadDetailHeader(
                            item          = detail.item,
                            currentUserId = currentUserId,
                            isForumAdmin  = isForumAdmin,
                            onToggleLove  = onToggleLove,
                            onEditThread  = onEditThread,
                            onDeleteThread = onDeleteThread,
                            onModerateThread = onModerateThread,
                            onReportThread = { showReportDialog = true },
                            onImageClick  = onImageClick
                        )
                    }

                    // "Balasan" label
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Text(
                                text       = "Balasan",
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            if (commentCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    androidx.compose.material3.Text(
                                        text       = "$commentCount",
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Loading / empty / comments
                    when {
                        isLoading -> item {
                            Box(
                                modifier         = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(24.dp),
                                    color       = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp,
                                    strokeCap   = StrokeCap.Round
                                )
                            }
                        }
                        detail.comments.isEmpty() -> item {
                            Box(
                                modifier         = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Text(
                                    text      = "Jadilah yang pertama berkomentar",
                                    fontSize  = 14.sp,
                                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else -> items(detail.comments, key = { it.comment.id }) { commentItem ->
                            CommentRow(
                                item          = commentItem,
                                currentUserId = currentUserId,
                                onToggleLove  = { onToggleCommentLove(commentItem) },
                                onEdit        = { text -> onEditComment(commentItem, text) },
                                onDelete      = { onDeleteComment(commentItem) },
                                onReport      = { reason, note -> onReportComment(commentItem, reason, note) }
                            )
                            // Indented divider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 64.dp)
                                    .height(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                            )
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }

                // ── Fixed Comment Input Bar ──
                CommentInputBar(
                    comment          = comment,
                    onCommentChange  = { comment = it },
                    actionInProgress = actionInProgress,
                    focusRequester   = focusRequester,
                    onSend           = {
                        if (comment.isNotBlank() && !actionInProgress) {
                            val text = comment
                            comment = ""
                            onSendComment(text)
                            scope.launch {
                                listState.animateScrollToItem((commentCount + 2).coerceAtLeast(0))
                            }
                        }
                    }
                )
            }
        }
    }
}

// ─── Thread Detail Header (expanded post view) ────────────────────────────────

@Composable
private fun ThreadDetailHeader(
    item: ForumThreadItem,
    currentUserId: String,
    isForumAdmin: Boolean,
    onToggleLove: () -> Unit,
    onEditThread: (String) -> Unit,
    onDeleteThread: () -> Unit,
    onModerateThread: (String?, Boolean?, Boolean?) -> Unit,
    onReportThread: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val author = item.author
    var showEditDialog   by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOptions      by remember { mutableStateOf(false) }
    val isOwner = item.thread.authorId == currentUserId

    if (showEditDialog) {
        EditDialog(
            title       = "Edit Postingan",
            initialText = item.thread.content,
            minLines    = 4,
            onDismiss   = { showEditDialog = false },
            onSave      = { text -> showEditDialog = false; onEditThread(text) }
        )
    }
    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title       = "Hapus postingan?",
            description = "Postingan dan semua komentarnya akan dihapus.",
            onDismiss   = { showDeleteDialog = false },
            onConfirm   = { showDeleteDialog = false; onDeleteThread() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Author row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AuthorAvatar(name = author?.fullName ?: "A", size = 44)

            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    text       = author?.fullName ?: "Alumni Al-Hasanah",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                val subtitle = listOfNotNull(
                    author?.profesiSekarang,
                    author?.tahunLulus?.let { "Angkatan $it" }
                ).joinToString(" · ").ifBlank { "Alumni terverifikasi" }
                androidx.compose.material3.Text(
                    text     = subtitle,
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            androidx.compose.material3.Text(
                text     = relativeTime(item.thread.createdAt),
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if ((isOwner && !item.thread.isLocked) || isForumAdmin) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showOptions = true },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Rounded.MoreHoriz,
                            contentDescription = "Opsi",
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    ThreadOptionsMenu(
                        expanded = showOptions,
                        isOwner = isOwner,
                        isForumAdmin = isForumAdmin,
                        isLocked = item.thread.isLocked,
                        isPinned = item.thread.isPinned,
                        status = item.thread.status,
                        onDismiss = { showOptions = false },
                        onEdit = { showOptions = false; showEditDialog = true },
                        onDelete = { showOptions = false; showDeleteDialog = true },
                        onModerateStatus = { status -> showOptions = false; onModerateThread(status, null, null) },
                        onTogglePin = { showOptions = false; onModerateThread(null, !item.thread.isPinned, null) },
                        onToggleLock = { showOptions = false; onModerateThread(null, null, !item.thread.isLocked) },
                        onReport = { showOptions = false; onReportThread() }
                    )
                }
            }
        }

        // Content
        if (item.thread.repostOfThreadId != null) {
            androidx.compose.material3.Text(
                text = "Posting ulang",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        androidx.compose.material3.Text(
            text       = item.thread.content,
            fontSize   = 15.sp,
            color      = MaterialTheme.colorScheme.onSurface,
            lineHeight = 23.sp
        )

        // Image
        item.attachments.firstOrNull()?.let { att ->
            AsyncImage(
                model              = att.signedUrl,
                contentDescription = att.attachment.altText,
                modifier           = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onImageClick(att.signedUrl) },
                contentScale       = ContentScale.Crop
            )
        }

        // Actions
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LoveButton(
                lovedByMe = item.lovedByMe,
                count     = item.thread.reactionCount,
                onClick   = onToggleLove
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
                if (item.thread.commentCount > 0) {
                    androidx.compose.material3.Text(
                        text     = "${item.thread.commentCount}",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Full-width divider after header
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

// ─── Comment Input Bar ────────────────────────────────────────────────────────

@Composable
private fun CommentInputBar(
    comment: String,
    onCommentChange: (String) -> Unit,
    actionInProgress: Boolean,
    focusRequester: FocusRequester,
    onSend: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // My avatar
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(avatarPalette("Me"))),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Input pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value           = comment,
                    onValueChange   = onCommentChange,
                    modifier        = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle       = TextStyle(
                        fontSize  = 14.sp,
                        color     = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    ),
                    cursorBrush     = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines        = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    decorationBox   = { inner ->
                        Box {
                            if (comment.isEmpty()) {
                                androidx.compose.material3.Text(
                                    text     = "Tambahkan komentar...",
                                    fontSize = 14.sp,
                                    color    = MaterialTheme.colorScheme.outline
                                )
                            }
                            inner()
                        }
                    }
                )
            }

            // Send button
            val canSend = comment.isNotBlank() && !actionInProgress
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .clickable(
                        enabled = canSend,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onSend
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (actionInProgress) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp,
                        strokeCap   = StrokeCap.Round
                    )
                } else {
                    androidx.compose.material3.Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Kirim",
                        tint     = if (canSend) Color.White else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─── Comment Row ──────────────────────────────────────────────────────────────

@Composable
private fun CommentRow(
    item: ForumCommentItem,
    currentUserId: String,
    onToggleLove: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    onReport: (String, String?) -> Unit
) {
    var showOptions      by remember { mutableStateOf(false) }
    var showEditDialog   by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val isOwner = item.comment.authorId == currentUserId

    if (showEditDialog) {
        EditDialog(
            title       = "Edit Komentar",
            initialText = item.comment.content,
            minLines    = 2,
            onDismiss   = { showEditDialog = false },
            onSave      = { text -> showEditDialog = false; onEdit(text) }
        )
    }
    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title       = "Hapus komentar?",
            description = "Komentar akan dihapus dari diskusi.",
            onDismiss   = { showDeleteDialog = false },
            onConfirm   = { showDeleteDialog = false; onDelete() }
        )
    }
    if (showReportDialog) {
        ReportBottomDialog(
            onDismiss = { showReportDialog = false },
            onSubmit  = { reason, note -> showReportDialog = false; onReport(reason, note) }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        AuthorAvatar(name = item.author?.fullName ?: "A", size = 34)

        Column(modifier = Modifier.weight(1f)) {
            // Name + time + options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                androidx.compose.material3.Text(
                    text       = item.author?.fullName ?: "Alumni Al-Hasanah",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false)
                )
                androidx.compose.material3.Text(
                    text     = relativeTime(item.comment.createdAt),
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showOptions = true },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Rounded.MoreHoriz,
                            contentDescription = "Opsi komentar",
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    DropdownMenu(
                        expanded         = showOptions,
                        onDismissRequest = { showOptions = false }
                    ) {
                        if (isOwner) {
                            DropdownMenuItem(
                                text        = { androidx.compose.material3.Text("Edit komentar", fontSize = 13.sp) },
                                leadingIcon = {
                                    androidx.compose.material3.Icon(
                                        Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp)
                                    )
                                },
                                onClick = { showOptions = false; showEditDialog = true }
                            )
                            DropdownMenuItem(
                                text        = {
                                    androidx.compose.material3.Text(
                                        "Hapus komentar", fontSize = 13.sp, color = LoveRed
                                    )
                                },
                                leadingIcon = {
                                    androidx.compose.material3.Icon(
                                        Icons.Rounded.Delete, contentDescription = null,
                                        tint = LoveRed, modifier = Modifier.size(15.dp)
                                    )
                                },
                                onClick = { showOptions = false; showDeleteDialog = true }
                            )
                        }
                        DropdownMenuItem(
                            text        = { androidx.compose.material3.Text("Laporkan", fontSize = 13.sp) },
                            leadingIcon = {
                                androidx.compose.material3.Icon(
                                    Icons.Rounded.Report, contentDescription = null, modifier = Modifier.size(15.dp)
                                )
                            },
                            onClick = { showOptions = false; showReportDialog = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Comment text
            androidx.compose.material3.Text(
                text       = item.comment.content,
                fontSize   = 14.sp,
                color      = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(8.dp))

            // Love
            LoveButton(
                lovedByMe = item.lovedByMe,
                count     = item.comment.reactionCount,
                onClick   = onToggleLove
            )
        }
    }
}

// ─── Small Reusable Components ────────────────────────────────────────────────

@Composable
private fun AuthorAvatar(name: String, size: Int, onClick: () -> Unit = {}) {
    val initials  = initialsOf(name).ifEmpty { "A" }
    val palette   = avatarPalette(name)
    val textSize  = if (size >= 40) 14.sp else 11.sp

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(palette))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text       = initials,
            fontSize   = textSize,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )
    }
}

@Composable
private fun LoveButton(lovedByMe: Boolean, count: Int, onClick: () -> Unit) {
    val heartColor by animateColorAsState(
        targetValue   = if (lovedByMe) LoveRed else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label         = "heartColor"
    )
    val heartScale by animateFloatAsState(
        targetValue   = if (lovedByMe) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "heartScale"
    )
    Row(
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick
        ),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector        = if (lovedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (lovedByMe) "Batalkan" else "Suka",
            tint               = heartColor,
            modifier           = Modifier
                .size(18.dp)
                .scale(heartScale)
        )
        if (count > 0) {
            androidx.compose.material3.Text(
                text     = "$count",
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CommentChip(count: Int, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "commentChipScale"
    )
    Row(
        modifier = Modifier
            .scale(scale)
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick
            ),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        androidx.compose.material3.Icon(
            Icons.Rounded.ChatBubbleOutline,
            contentDescription = "Komentar",
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
        if (count > 0) {
            androidx.compose.material3.Text(
                text     = "$count",
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        androidx.compose.material3.Text(
            text     = text,
            fontSize = 10.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeedDivider() {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isDark) 1.25.dp else 1.dp)
            .background(
                if (isDark) {
                    Color.White.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.78f)
                }
            )
    )
}

@Composable
private fun ThreadOptionsMenu(
    expanded: Boolean,
    isOwner: Boolean,
    isForumAdmin: Boolean,
    isLocked: Boolean,
    isPinned: Boolean,
    status: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onModerateStatus: (String) -> Unit,
    onTogglePin: () -> Unit,
    onToggleLock: () -> Unit,
    onReport: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (isOwner && !isLocked) {
            DropdownMenuItem(
                text        = { androidx.compose.material3.Text("Edit postingan", fontSize = 14.sp) },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(17.dp)
                    )
                },
                onClick = onEdit
            )
            DropdownMenuItem(
                text        = {
                    androidx.compose.material3.Text(
                        "Hapus postingan", fontSize = 14.sp, color = LoveRed
                    )
                },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        Icons.Rounded.Delete, contentDescription = null,
                        tint = LoveRed, modifier = Modifier.size(17.dp)
                    )
                },
                onClick = onDelete
            )
        }
        if (isForumAdmin) {
            DropdownMenuItem(
                text = {
                    androidx.compose.material3.Text(
                        if (status == "published") "Sembunyikan dari forum" else "Pulihkan publikasi",
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        Icons.Rounded.Report, contentDescription = null, modifier = Modifier.size(17.dp)
                    )
                },
                onClick = { onModerateStatus(if (status == "published") "hidden" else "published") }
            )
            DropdownMenuItem(
                text = {
                    androidx.compose.material3.Text(
                        if (isLocked) "Buka komentar" else "Kunci komentar",
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(17.dp)
                    )
                },
                onClick = onToggleLock
            )
            DropdownMenuItem(
                text = {
                    androidx.compose.material3.Text(
                        if (isPinned) "Lepas pin" else "Pin postingan",
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(17.dp)
                    )
                },
                onClick = onTogglePin
            )
        }
        DropdownMenuItem(
            text        = { androidx.compose.material3.Text("Laporkan", fontSize = 14.sp) },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    Icons.Rounded.Report, contentDescription = null, modifier = Modifier.size(17.dp)
                )
            },
            onClick = onReport
        )
    }
}

// ─── Forum Center State (Loading / Empty / Error) ─────────────────────────────

@Composable
private fun ForumCenterState(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    showLoading: Boolean = false,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    val hasActions = actionText != null && onAction != null

    Column(
        modifier             = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment  = Alignment.CenterHorizontally,
        verticalArrangement  = Arrangement.Center
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
        if (hasActions) {
            Box(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Rounded.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    androidx.compose.material3.Text(
                        text = title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.3).sp
                    )
                    if (!description.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { onAction() }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = actionText,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (secondaryActionText != null && onSecondaryAction != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
                                    .clickable { onSecondaryAction() }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Text(
                                    text = secondaryActionText,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            return@Column
        }
        androidx.compose.material3.Text(
            text       = title,
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface,
            textAlign  = TextAlign.Center,
            letterSpacing = (-0.2).sp
        )
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.Text(
                text      = description,
                fontSize  = 13.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary action
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onAction() }
                        .padding(horizontal = 24.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text       = actionText,
                        color      = Color.White,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // Secondary action
                if (secondaryActionText != null && onSecondaryAction != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .clickable { onSecondaryAction() }
                            .padding(horizontal = 24.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Text(
                            text       = secondaryActionText,
                            color      = MaterialTheme.colorScheme.onSurface,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ─── Dialogs ──────────────────────────────────────────────────────────────────

@Composable
private fun EditDialog(
    title: String,
    initialText: String,
    minLines: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text(
                    text = "Batal",
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onDismiss
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                androidx.compose.material3.Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = text.isNotBlank()) { onSave(text) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    androidx.compose.material3.Text(
                        "Simpan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (text.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    minLines = minLines.coerceAtLeast(6),
                    maxLines = 18,
                    decorationBox = { inner ->
                        Box {
                            if (text.isBlank()) {
                                androidx.compose.material3.Text(
                                    "Tulis pembaruan...",
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 24.sp
                                )
                            }
                            inner()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OverlayBlack.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.Text(
                        text       = title,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    androidx.compose.material3.Text(
                        text      = description,
                        fontSize  = 13.sp,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .clickable { onDismiss() }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            androidx.compose.material3.Text(
                                "Batal", fontSize = 13.sp,
                                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(LoveRed)
                                .clickable { onConfirm() }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            androidx.compose.material3.Text(
                                "Hapus", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportBottomDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String?) -> Unit
) {
    var note by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OverlayBlack.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    androidx.compose.material3.Text(
                        text       = "Laporkan Konten",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    androidx.compose.material3.Text(
                        text      = "Laporan akan ditinjau oleh admin forum.",
                        fontSize  = 13.sp,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(14.dp)
                    ) {
                        BasicTextField(
                            value         = note,
                            onValueChange = { note = it },
                            modifier      = Modifier.fillMaxWidth(),
                            textStyle     = TextStyle(
                                fontSize   = 14.sp,
                                color      = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 21.sp
                            ),
                            cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                            minLines      = 2,
                            maxLines      = 5,
                            decorationBox = { inner ->
                                Box {
                                    if (note.isEmpty()) {
                                        androidx.compose.material3.Text(
                                            text     = "Catatan opsional...",
                                            fontSize = 14.sp,
                                            color    = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .clickable { onDismiss() }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            androidx.compose.material3.Text(
                                "Batal", fontSize = 13.sp,
                                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { onSubmit("lainnya", note.ifBlank { null }) }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            androidx.compose.material3.Text(
                                "Kirim Laporan", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewDialog(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model              = url,
                contentDescription = "Pratinjau gambar",
                modifier           = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale       = ContentScale.Fit
            )
            // Close pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onDismiss() }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Tutup",
                    tint     = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
