package com.alhasanah.alhasanahmedia.ui.alumni

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.AlumniDirectoryItem
import com.alhasanah.alhasanahmedia.data.model.ChatConversationItem
import com.alhasanah.alhasanahmedia.data.model.ChatDetail
import com.alhasanah.alhasanahmedia.data.model.ChatMessageItem
import com.alhasanah.alhasanahmedia.data.model.ChatPresenceDto
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import org.koin.androidx.compose.koinViewModel
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlumniChatScreen(
    navController: NavController,
    initialConversationId: String? = null,
    initialTargetUserId: String? = null,
    viewModel: AlumniChatViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val ready = state as? AlumniChatUiState.Ready
    var showNewChat by remember { mutableStateOf(false) }
    var showKeyBackup by remember { mutableStateOf(false) }
    var openedInitialConversationId by remember(initialConversationId) { mutableStateOf<String?>(null) }
    var openedInitialTargetUserId by remember(initialTargetUserId) { mutableStateOf<String?>(null) }

    LaunchedEffect(initialConversationId) {
        if (!initialConversationId.isNullOrBlank() && openedInitialConversationId != initialConversationId) {
            openedInitialConversationId = initialConversationId
            viewModel.openInitialConversation(initialConversationId)
        }
    }

    LaunchedEffect(initialTargetUserId) {
        if (!initialTargetUserId.isNullOrBlank() && openedInitialTargetUserId != initialTargetUserId) {
            openedInitialTargetUserId = initialTargetUserId
            viewModel.openInitialDirectChat(initialTargetUserId)
        }
    }

    AlumniPremiumTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0.dp)
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (val current = state) {
                    AlumniChatUiState.Loading -> ChatCenterState("Memuat chat...", true)
                    AlumniChatUiState.LoginRequired -> ChatCenterState("Masuk untuk membuka chat alumni")
                    is AlumniChatUiState.Locked -> ChatCenterState(current.message)
                    is AlumniChatUiState.Error -> ChatCenterState(current.message)
                    is AlumniChatUiState.Ready -> ChatList(
                        state = current,
                        onOpen = { viewModel.openConversation(it.conversation.id) },
                        onNewChat = { showNewChat = true },
                        onBackupKey = { showKeyBackup = true },
                        onClearMessage = viewModel::clearMessage
                    )
                }

                ready?.selectedDetail?.let { detail ->
                    ChatRoom(
                        detail = detail,
                        typingNames = ready.typingNames,
                        presenceByUserId = ready.presenceByUserId,
                        isSending = ready.isSending,
                        isLoadingOlder = ready.isLoadingOlder,
                        hasMoreMessages = ready.hasMoreMessages,
                        onBack = viewModel::closeConversation,
                        onSend = viewModel::sendMessage,
                        onLoadOlder = viewModel::loadOlderMessages,
                        onTypingChanged = viewModel::onTypingChanged,
                        onArchive = viewModel::archiveSelectedConversation,
                        onToggleMute = viewModel::toggleMuteSelectedConversation,
                        onBlockUser = viewModel::blockCurrentChatUser,
                        onDeleteMessage = viewModel::deleteMessage,
                        onReportMessage = { id -> viewModel.reportMessage(id, "other", null) },
                        onRetryOutboxMessage = viewModel::retryOutboxMessage,
                        failedDraft = ready.failedDraft,
                        onRetryFailed = viewModel::retryFailedMessage,
                        onDismissFailed = viewModel::clearFailedDraft
                    )
                }
            }
        }

        if (showNewChat && ready != null) {
            NewChatSheet(
                directory = ready.directory,
                onDismiss = { showNewChat = false },
                onSelect = {
                    showNewChat = false
                    viewModel.startDirectChat(it.alumni.id)
                }
            )
        }

        if (showKeyBackup && ready != null) {
            KeyBackupDialog(
                isSaving = ready.isBackingUpKey || ready.isRestoringKey,
                onDismiss = { showKeyBackup = false },
                onSave = { passphrase ->
                    viewModel.createEncryptedKeyBackup(passphrase)
                    showKeyBackup = false
                },
                onRestore = { passphrase ->
                    viewModel.restoreEncryptedKeyBackup(passphrase)
                    showKeyBackup = false
                },
                onRevoke = {
                    viewModel.revokeCurrentDeviceKey()
                    showKeyBackup = false
                }
            )
        }
    }
}

@Composable
private fun ChatList(
    state: AlumniChatUiState.Ready,
    onOpen: (ChatConversationItem) -> Unit,
    onNewChat: () -> Unit,
    onBackupKey: () -> Unit,
    onClearMessage: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredConversations = remember(state.conversations, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isBlank()) state.conversations else state.conversations.filter { item ->
            listOfNotNull(
                item.otherParticipant?.fullName,
                item.otherParticipant?.profesiSekarang,
                item.conversation.lastMessagePreview
            ).joinToString(" ").lowercase().contains(query)
        }
    }

    LaunchedEffect(state.transientMessage) {
        if (state.transientMessage != null) {
            kotlinx.coroutines.delay(2600)
            onClearMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ChatListTopBar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            onBackupKey = onBackupKey,
            onNewChat = onNewChat
        )

        if (state.conversations.isEmpty()) {
            ChatEmptyState(onNewChat = onNewChat)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(filteredConversations, key = { it.conversation.id }) { item ->
                    ConversationRow(item = item, onOpen = { onOpen(item) })
                }
            }
        }
    }

    AnimatedVisibility(visible = state.transientMessage != null) {
        Surface(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.inverseSurface
        ) {
            Text(
                text = state.transientMessage.orEmpty(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ChatListTopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onBackupKey: () -> Unit,
    onNewChat: () -> Unit
) {
    AppPageHeader(
        title = "CHAT ALUMNI",
        subtitle = "Percakapan alumni realtime",
        isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
        size = AppPageHeaderSize.Compact,
        rightAction = {
            Row {
                ChatIconButton(icon = Icons.Rounded.Lock, contentDescription = "Backup kunci", onClick = onBackupKey)
                Spacer(Modifier.width(8.dp))
                ChatIconButton(icon = Icons.Rounded.AddComment, contentDescription = "Chat baru", onClick = onNewChat)
            }
        }
    )
    AnimatedVisibility(visible = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                singleLine = true,
                decorationBox = { inner ->
                    if (searchQuery.isBlank()) {
                        Text("Cari percakapan", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                    inner()
                }
            )
        }
    }
}

@Composable
private fun KeyBackupDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onRestore: (String) -> Unit,
    onRevoke: () -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    val isValid = passphrase.length >= 12
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup kunci E2EE") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Gunakan passphrase untuk backup atau pemulihan kunci chat perangkat ini.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving,
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text("Passphrase") },
                    supportingText = { Text("Minimal 12 karakter") }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = isValid && !isSaving,
                onClick = { onSave(passphrase) }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Simpan")
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = !isSaving,
                    onClick = onRevoke
                ) {
                    Text("Cabut", color = MaterialTheme.colorScheme.error)
                }
                TextButton(
                    enabled = isValid && !isSaving,
                    onClick = { onRestore(passphrase) }
                ) {
                    Text("Pulihkan")
                }
                TextButton(enabled = !isSaving, onClick = onDismiss) {
                    Text("Batal")
                }
            }
        }
    )
}

@Composable
private fun ConversationRow(item: ChatConversationItem, onOpen: () -> Unit) {
    val person = item.otherParticipant ?: item.participants.firstOrNull()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChatAvatar(name = person?.fullName ?: "A", size = 54)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    person?.fullName ?: "Alumni Al-Hasanah",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    relativeTime(item.conversation.lastMessageAt ?: item.conversation.updatedAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.conversation.lastMessagePreview ?: "Mulai percakapan alumni",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    UnreadBadge(item.unreadCount)
                }
            }
        }
    }
}

@Composable
private fun ChatRoom(
    detail: ChatDetail,
    typingNames: List<String>,
    presenceByUserId: Map<String, ChatPresenceDto>,
    isSending: Boolean,
    isLoadingOlder: Boolean,
    hasMoreMessages: Boolean,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onLoadOlder: () -> Unit,
    onTypingChanged: (String) -> Unit,
    onArchive: () -> Unit,
    onToggleMute: () -> Unit,
    onBlockUser: () -> Unit,
    onDeleteMessage: (String) -> Unit,
    onReportMessage: (String) -> Unit,
    onRetryOutboxMessage: (String) -> Unit,
    failedDraft: String?,
    onRetryFailed: () -> Unit,
    onDismissFailed: () -> Unit
) {
    var text by remember(detail.conversation.conversation.id) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val other = detail.conversation.otherParticipant

    LaunchedEffect(detail.messages.size, typingNames, isLoadingOlder) {
        val target = detail.messages.lastIndex.coerceAtLeast(0)
        if (detail.messages.isNotEmpty() && !isLoadingOlder) listState.animateScrollToItem(target)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatRoomTopBar(
                title = other?.fullName ?: "Chat Alumni",
                subtitle = chatSubtitle(typingNames, other?.id?.let { presenceByUserId[it] }),
                isMuted = detail.conversation.myParticipant?.mutedUntil != null,
                onBack = onBack,
                onArchive = onArchive,
                onToggleMute = onToggleMute,
                onBlockUser = onBlockUser
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasMoreMessages) {
                    item {
                        LoadOlderMessagesRow(
                            isLoading = isLoadingOlder,
                            onClick = onLoadOlder
                        )
                    }
                }
                items(detail.messages, key = { it.message.id }) { message ->
                    MessageBubble(
                        item = message,
                        otherLastReadAt = detail.conversation.otherLastReadAt,
                        onDelete = { onDeleteMessage(message.message.id) },
                        onReport = { onReportMessage(message.message.id) },
                        onRetryOutbox = { onRetryOutboxMessage(message.message.id) }
                    )
                }
                if (typingNames.isNotEmpty()) {
                    item { TypingBubble() }
                }
            }

            failedDraft?.let {
                FailedMessageBar(
                    text = it,
                    onRetry = onRetryFailed,
                    onDismiss = onDismissFailed
                )
            }

            ChatInputBar(
                text = text,
                isSending = isSending,
                onTextChange = {
                    text = it
                    onTypingChanged(it)
                },
                onSend = {
                    val clean = text.trim()
                    if (clean.isNotBlank()) {
                        text = ""
                        onSend(clean)
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatRoomTopBar(
    title: String,
    subtitle: String,
    isMuted: Boolean,
    onBack: () -> Unit,
    onArchive: () -> Unit,
    onToggleMute: () -> Unit,
    onBlockUser: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    AppPageHeader(
        title = title.uppercase(),
        subtitle = subtitle,
        isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
        onBack = onBack,
        size = AppPageHeaderSize.Compact,
        rightAction = {
            Box {
                ChatIconButton(icon = Icons.Rounded.MoreVert, contentDescription = "Opsi chat", onClick = { showMenu = true })
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(if (isMuted) "Aktifkan notifikasi" else "Bisukan chat") },
                    leadingIcon = { Icon(Icons.Rounded.NotificationsOff, contentDescription = null) },
                    onClick = { showMenu = false; onToggleMute() }
                )
                DropdownMenuItem(
                    text = { Text("Arsipkan chat") },
                    leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                    onClick = { showMenu = false; onArchive() }
                )
                DropdownMenuItem(
                    text = { Text("Blokir alumni", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.PersonOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = { showMenu = false; onBlockUser() }
                )
            }
        }
        }
    )
}

@Composable
private fun MessageBubble(
    item: ChatMessageItem,
    otherLastReadAt: String?,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onRetryOutbox: () -> Unit
) {
    val isLocal = item.message.id.startsWith("local-")
    val isFailed = item.deliveryState == "failed"
    val isRead = item.isMine && otherLastReadAt != null && isAtOrAfter(otherLastReadAt, item.message.createdAt)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (item.isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.78f),
            horizontalAlignment = if (item.isMine) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (item.isMine) 20.dp else 6.dp,
                    bottomEnd = if (item.isMine) 6.dp else 20.dp
                ),
                color = if (item.isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
            Text(
                    text = item.displayContent(),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = if (item.isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )
            }
            Text(
                buildString {
                    append(relativeTime(item.message.createdAt))
                    if (item.isMine) {
                        append(
                            when {
                                isFailed -> " · belum terkirim"
                                isLocal -> " · mengirim"
                                isRead -> " · dibaca"
                                else -> " · terkirim"
                            }
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 10.sp,
                color = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.isMine && isFailed) {
                Text(
                    "Coba lagi",
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onRetryOutbox)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (item.isMine && !isLocal) {
                Text(
                    "Hapus",
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (!item.isMine) {
                Text(
                    "Laporkan",
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onReport)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun ChatMessageItem.displayContent(): String =
    when {
        decryptedContent != null -> decryptedContent
        message.encryptionScheme != "legacy_plaintext" && decryptError -> "Pesan tidak dapat didekripsi di perangkat ini."
        message.encryptionScheme != "legacy_plaintext" -> "Pesan terenkripsi"
        else -> message.content ?: "Pesan lama tidak tersedia."
    }

@Composable
private fun LoadOlderMessagesRow(isLoading: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        ) {
            Row(
                modifier = Modifier
                    .clickable(enabled = !isLoading, onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(
                    if (isLoading) "Memuat..." else "Muat pesan lama",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FailedMessageBar(text: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Pesan gagal: $text",
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Text(
            "Coba lagi",
            modifier = Modifier.clickable(onClick = onRetry).padding(horizontal = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Icon(
            Icons.Rounded.Close,
            contentDescription = "Tutup",
            modifier = Modifier.size(18.dp).clickable(onClick = onDismiss),
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun TypingBubble() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            "mengetik...",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                ),
                maxLines = 5,
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text("Tulis pesan...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                    }
                    inner()
                }
            )
        }
        Surface(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .clickable(enabled = text.isNotBlank() && !isSending, onClick = onSend),
            color = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Kirim",
                        tint = if (text.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatSheet(
    directory: List<AlumniDirectoryItem>,
    onDismiss: () -> Unit,
    onSelect: (AlumniDirectoryItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mulai chat", modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            ChatIconButton(icon = Icons.Rounded.Close, contentDescription = "Tutup", onClick = onDismiss)
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
            items(directory, key = { it.alumni.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ChatAvatar(item.alumni.fullName, 46)
                    Column {
                        Text(item.alumni.fullName, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(item.alumni.profesiSekarang, "Angkatan ${item.alumni.tahunLulus}").joinToString(" · "),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatEmptyState(onNewChat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(modifier = Modifier.size(74.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Belum ada percakapan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Mulai percakapan personal dengan alumni lain secara realtime.",
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(18.dp))
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
            Text(
                "Chat baru",
                modifier = Modifier
                    .clickable(onClick = onNewChat)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChatCenterState(message: String, loading: Boolean = false) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChatIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ChatAvatar(name: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF16A34A)))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A",
            color = Color.White,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(count.coerceAtMost(99).toString(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun relativeTime(raw: String): String {
    return runCatching {
        val then = Instant.parse(raw)
        val minutes = Duration.between(then, Instant.now()).toMinutes().coerceAtLeast(0)
        when {
            minutes < 1 -> "baru"
            minutes < 60 -> "${minutes}m"
            minutes < 1440 -> "${minutes / 60}j"
            else -> "${minutes / 1440}h"
        }
    }.getOrDefault("")
}

private fun chatSubtitle(typingNames: List<String>, presence: ChatPresenceDto?): String {
    if (typingNames.isNotEmpty()) return "${typingNames.first()} sedang mengetik..."
    if (presence?.isOnline == true && !isPresenceStale(presence.updatedAt ?: presence.lastSeenAt)) return "online"
    val lastSeen = presence?.lastSeenAt?.let { relativeTime(it) }.orEmpty()
    return if (lastSeen.isBlank()) "terakhir dilihat belum tersedia" else "terakhir dilihat $lastSeen"
}

private fun isPresenceStale(raw: String): Boolean {
    return runCatching {
        Duration.between(Instant.parse(raw), Instant.now()).seconds > 70
    }.getOrDefault(true)
}

private fun isAtOrAfter(left: String, right: String): Boolean {
    return runCatching {
        !Instant.parse(left).isBefore(Instant.parse(right))
    }.getOrDefault(false)
}
